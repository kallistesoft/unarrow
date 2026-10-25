package ca.kallistesoft.unarrow.core.flags

import com.google.android.gms.tasks.Tasks
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigValue
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Firebase Remote Config is faked: [remote] holds the values the server sent, [online] whether it can be reached. */
class FirebaseFeatureFlagsTest {
    private val remote = mutableMapOf<String, Any>()
    private var online = true
    private val listener = slot<ConfigUpdateListener>()
    private val config = mockk<FirebaseRemoteConfig>(relaxed = true) {
        every { getValue(any()) } answers { value(remote[firstArg()]) }
        every { fetchAndActivate() } answers {
            if (online) Tasks.forResult(true) else Tasks.forException(IllegalStateException("offline"))
        }
        every { activate() } returns Tasks.forResult(true)
        every { addOnConfigUpdateListener(capture(listener)) } returns mockk(relaxed = true)
        // Real-time updates are handed over on Remote Config's executor; run them at once.
        every { schedule(any()) } answers { firstArg<Runnable>().run(); Tasks.forResult(null) }
    }

    @Test
    fun `flags keep their defaults until the server sets them`() = runTest {
        val flags = flags()
        assertEquals(FeatureFlag.entries.associateWith { it.defaultValue }, flags.values.value)
        verify { config.setConfigSettingsAsync(match { it.minimumFetchIntervalInSeconds == 60L }) }
    }

    @Test
    fun `values fetched on a previous launch apply at start`() = runTest {
        remote["rewarded_ads"] = false
        assertEquals(false, flags().values.value[FeatureFlag.REWARDED_ADS])
    }

    @Test
    fun `refresh activates new values, and keeps the last ones offline`() = runTest {
        val flags = flags()
        remote["review_prompt"] = false
        flags.refresh()
        assertEquals(false, flags.values.value[FeatureFlag.REVIEW_PROMPT])

        online = false
        flags.refresh()
        assertEquals(false, flags.values.value[FeatureFlag.REVIEW_PROMPT])
    }

    @Test
    fun `real-time updates are activated as they arrive`() = runTest {
        val flags = flags()
        runCurrent()
        remote["ads_enabled"] = false
        listener.captured.onUpdate(mockk())
        runCurrent()
        verify { config.activate() }
        assertEquals(false, flags.values.value[FeatureFlag.ADS_ENABLED])
    }

    @Test
    fun `without real-time updates, refresh still works`() = runTest {
        every { config.addOnConfigUpdateListener(any()) } throws IllegalStateException("no real-time")
        val flags = flags()
        runCurrent()
        remote["rewarded_ads"] = false
        flags.refresh()
        assertEquals(false, flags.values.value[FeatureFlag.REWARDED_ADS])
    }

    @Test
    fun `a number is the server's value, 0 when the server has none, null when never fetched`() = runTest {
        val flags = flags()
        remote["min_version_code"] = 42L
        assertEquals(42L, flags.fetchLong("min_version_code"))

        online = false
        assertEquals("last value fetched", 42L, flags.fetchLong("min_version_code"))
        assertNull(flags.fetchLong("unknown"))

        online = true
        assertEquals(0L, flags.fetchLong("unknown"))
    }

    @Test
    fun `fetching a number also refreshes the flags`() = runTest {
        val flags = flags()
        remote["rewarded_ads"] = false
        flags.fetchLong("min_version_code")
        assertEquals(false, flags.values.value[FeatureFlag.REWARDED_ADS])
    }

    private fun TestScope.flags() = FirebaseFeatureFlags(config, FeatureFlagSettings(60), backgroundScope)

    /** A value from the server, or the static default (false, 0) when it has none. */
    private fun value(remote: Any?): FirebaseRemoteConfigValue = mockk {
        every { source } returns
            if (remote == null) FirebaseRemoteConfig.VALUE_SOURCE_STATIC else FirebaseRemoteConfig.VALUE_SOURCE_REMOTE
        every { asBoolean() } returns (remote as? Boolean ?: false)
        every { asLong() } returns (remote as? Long ?: 0L)
    }
}
