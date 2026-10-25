package ca.kallistesoft.unarrow.core.flags

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureFlagsTest {
    @Test
    fun `without Firebase every flag has its default`() = runTest {
        val flags = DefaultFeatureFlags()
        flags.refresh()
        assertEquals(FeatureFlag.entries.associateWith { it.defaultValue }, flags.values.first())
        assertTrue(flags.isEnabled(FeatureFlag.REWARDED_ADS).first())
    }

    @Test
    fun `tests can switch a flag off`() = runTest {
        val flags = DefaultFeatureFlags(mapOf(FeatureFlag.REVIEW_PROMPT to false))
        assertEquals(listOf(false), flags.isEnabled(FeatureFlag.REVIEW_PROMPT).toList())
        assertTrue(flags.isEnabled(FeatureFlag.ADS_ENABLED).first())
    }

    @Test
    fun `without Firebase nothing is known remotely`() = runTest {
        assertNull(NoRemoteConfigValues.fetchLong("min_version_code"))
    }

    @Test
    fun `settings compare by value`() {
        assertEquals(FeatureFlagSettings(3600), FeatureFlagSettings(3600))
        assertFalse(FeatureFlagSettings(0) == FeatureFlagSettings(3600))
        assertEquals(0L, FeatureFlagSettings(0).minimumFetchIntervalSeconds)
    }
}
