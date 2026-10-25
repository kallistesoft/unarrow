package ca.kallistesoft.unarrow.core.ads

import android.app.Activity
import ca.kallistesoft.unarrow.core.flags.FeatureFlag
import ca.kallistesoft.unarrow.core.testing.FakeFeatureFlags
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The ad rules on fake SDKs: consent gating, the `ads_enabled` flag, preloading, the 3-minute cap and fallbacks. */
@OptIn(ExperimentalCoroutinesApi::class)
class AdsManagerTest {
    private val activity = mockk<Activity>()
    private val flags = FakeFeatureFlags()
    private val consent = FakeConsent()
    private val sdk = FakeSdk()
    private var now = 0L
    private var onScreen: Activity? = activity

    private val adsRemoved = MutableStateFlow(false)

    private fun TestScope.manager(config: AdsConfig = AdsConfig("unit-1")) = AdsManager(
        consent, sdk, flags, { onScreen }, config, backgroundScope, { now }, adsRemoved,
    )

    private fun TestScope.withRewarded(): AdsManager = manager(AdsConfig("unit-1", rewardedUnitId = "reward-1")).also {
        it.gatherConsent(activity)
        runCurrent()
    }

    /** Consent gathered at launch, ads allowed: the SDK starts and an ad is preloaded. */
    private fun TestScope.started(): AdsManager = manager().also {
        it.gatherConsent(activity)
        runCurrent()
    }

    @Test
    fun `nothing starts before consent allows ads`() = runTest {
        consent.canRequest = false
        val ads = manager()
        ads.gatherConsent(activity)
        runCurrent()
        assertEquals(1, consent.updates)
        assertEquals(0, sdk.initialized)
        assertEquals(0, sdk.loads)

        ads.showInterstitial()
        assertEquals("no ad without consent", 0, sdk.shown)
        assertEquals(0, sdk.initialized)
    }

    @Test
    fun `ads turned off in the config ask nothing and show nothing`() = runTest {
        val ads = manager(AdsConfig("unit-1", enabled = false))
        ads.gatherConsent(activity)
        runCurrent()
        ads.showInterstitial()
        runCurrent()
        assertEquals("no consent request", 0, consent.updates)
        assertEquals(0 to 0, sdk.initialized to sdk.loads)
        assertEquals(0, sdk.shown)
    }

    @Test
    fun `consent given in the form starts the SDK and preloads the interstitial`() = runTest {
        consent.canRequest = false
        consent.onUpdate = { consent.canRequest = true }
        manager().gatherConsent(activity)
        runCurrent()
        assertEquals(1, sdk.initialized)
        assertEquals(listOf("unit-1"), sdk.unitIds)
    }

    @Test
    fun `consent from an earlier launch starts ads once, while the information refreshes`() = runTest {
        val update = CompletableDeferred<Unit>()
        consent.waitOnUpdate = update
        manager().gatherConsent(activity)
        runCurrent()
        assertEquals("started before the refresh ends", 1, sdk.initialized)
        update.complete(Unit)
        runCurrent()
        assertEquals("initialised once, one ad loaded", 1 to 1, sdk.initialized to sdk.loads)
    }

    @Test
    fun `the preloaded interstitial shows after a test, then the next one loads`() = runTest {
        val ads = started()
        ads.showInterstitial()
        assertEquals(listOf(activity), sdk.shownOn)
        runCurrent()
        assertEquals(2, sdk.loads)
        assertEquals(1, sdk.initialized)
    }

    @Test
    fun `the results wait while the ad is on screen`() = runTest {
        val ads = started()
        val closed = CompletableDeferred<Unit>()
        sdk.closeAd = closed
        var resultsShown = false
        launch {
            ads.showInterstitial()
            resultsShown = true
        }
        runCurrent()
        assertFalse(resultsShown)
        closed.complete(Unit)
        runCurrent()
        assertTrue(resultsShown)
    }

    @Test
    fun `at most one interstitial every 3 minutes`() = runTest {
        val ads = started()
        ads.showInterstitial()
        runCurrent()
        now += 2 * 60_000
        ads.showInterstitial()
        assertEquals("capped", 1, sdk.shown)
        now += 60_000
        ads.showInterstitial()
        assertEquals(2, sdk.shown)
    }

    @Test
    fun `no ad loaded means the results come at once, and one starts loading`() = runTest {
        sdk.fill = false
        val ads = started()
        assertEquals(1, sdk.loads)
        ads.showInterstitial()
        assertEquals(0, sdk.shown)
        runCurrent()
        assertEquals("loads again for the next test", 2, sdk.loads)

        sdk.fill = true
        ads.showInterstitial()
        runCurrent()
        assertEquals(3, sdk.loads)
        ads.showInterstitial()
        assertEquals(1, sdk.shown)
    }

    @Test
    fun `an ad still loading isn't waited for, nor loaded twice`() = runTest {
        val loaded = CompletableDeferred<Unit>()
        sdk.waitOnLoad = loaded
        val ads = started()
        ads.showInterstitial()
        runCurrent()
        assertEquals(0, sdk.shown)
        assertEquals(1, sdk.loads)
        loaded.complete(Unit)
        runCurrent()
        ads.showInterstitial()
        assertEquals(1, sdk.shown)
    }

    @Test
    fun `an ad loaded over an hour ago is dropped and loaded again`() = runTest {
        val ads = started()
        now += 60 * 60_000
        ads.showInterstitial()
        assertEquals(0, sdk.shown)
        runCurrent()
        assertEquals(2, sdk.loads)
        ads.showInterstitial()
        assertEquals(1, sdk.shown)
    }

    @Test
    fun `with ads_enabled off nothing loads or shows`() = runTest {
        flags.set(FeatureFlag.ADS_ENABLED, false)
        val ads = started()
        assertEquals(0, sdk.initialized)
        ads.showInterstitial()
        assertEquals(0, sdk.shown)

        flags.set(FeatureFlag.ADS_ENABLED, true) // switched back on remotely: the next test loads one
        ads.showInterstitial()
        runCurrent()
        assertEquals(1, sdk.loads)
    }

    @Test
    fun `ads_enabled switched off stops a preloaded ad from showing`() = runTest {
        val ads = started()
        flags.set(FeatureFlag.ADS_ENABLED, false)
        ads.showInterstitial()
        assertEquals(0, sdk.shown)
    }

    @Test
    fun `no activity on screen, no ad, and the loaded one is kept`() = runTest {
        val ads = started()
        onScreen = null
        ads.showInterstitial()
        runCurrent()
        assertEquals(0, sdk.shown)
        assertEquals("not loaded again", 1, sdk.loads)
        onScreen = activity
        ads.showInterstitial()
        assertEquals(1, sdk.shown)
    }

    @Test
    fun `privacy options follow the consent, and the form shows on the current screen`() = runTest {
        consent.privacyRequired = true
        val ads = started()
        assertTrue(ads.privacyOptionsRequired.value)

        consent.onPrivacyOptions = { consent.privacyRequired = false }
        ads.showPrivacyOptions()
        runCurrent()
        assertEquals(listOf(activity), consent.privacyShownOn)
        assertFalse(ads.privacyOptionsRequired.value)
    }

    @Test
    fun `consent given from the privacy options starts ads`() = runTest {
        consent.canRequest = false
        val ads = started()
        consent.onPrivacyOptions = { consent.canRequest = true }
        ads.showPrivacyOptions()
        runCurrent()
        assertEquals(1, sdk.initialized)
    }

    @Test
    fun `privacy options need a screen to show on`() = runTest {
        val ads = started()
        onScreen = null
        ads.showPrivacyOptions()
        runCurrent()
        assertTrue(consent.privacyShownOn.isEmpty())
    }

    @Test
    fun `an interstitial comes every third solved puzzle, from level 5`() = runTest {
        val ads = started()
        repeat(6) { ads.showAfterPuzzle(level = 4) }
        assertEquals("not before level 5", 0, sdk.shown)
        ads.showAfterPuzzle(5)
        ads.showAfterPuzzle(6)
        assertEquals(0, sdk.shown)
        ads.showAfterPuzzle(7)
        assertEquals(1, sdk.shown)
        runCurrent()
        now += CAP_MILLIS
        repeat(2) { ads.showAfterPuzzle(8) }
        assertEquals(1, sdk.shown)
        ads.showAfterPuzzle(8)
        assertEquals(2, sdk.shown)
    }

    @Test
    fun `an interstitial that couldn't show is tried again after the next puzzle`() = runTest {
        val ads = started()
        onScreen = null
        repeat(3) { ads.showAfterPuzzle(10) }
        assertEquals(0, sdk.shown)
        onScreen = activity
        ads.showAfterPuzzle(10)
        assertEquals(1, sdk.shown)
    }

    @Test
    fun `remove ads stops interstitials, and none is loaded`() = runTest {
        adsRemoved.value = true
        val ads = started()
        assertEquals(0, sdk.loads)
        repeat(9) { ads.showAfterPuzzle(20) }
        assertEquals(0, sdk.shown)
    }

    @Test
    fun `a rewarded ad loads with the others, and gives its reward once watched`() = runTest {
        val ads = withRewarded()
        assertTrue(ads.rewardedReady.value)
        assertEquals(listOf("unit-1", "reward-1"), sdk.unitIds)
        assertTrue(ads.showRewarded())
        assertEquals(listOf(activity), sdk.rewardedOn)
        runCurrent()
        assertTrue("the next one loads", ads.rewardedReady.value)
        sdk.earn = false
        assertFalse(ads.showRewarded())
    }

    @Test
    fun `rewarded ads stay available after remove ads`() = runTest {
        adsRemoved.value = true
        val ads = withRewarded()
        assertEquals(listOf("reward-1"), sdk.unitIds)
        assertTrue(ads.showRewarded())
    }

    @Test
    fun `no rewarded ad without a unit, without consent, off remotely or without a screen`() = runTest {
        val noUnit = started()
        assertFalse(noUnit.rewardedReady.value)
        assertFalse(noUnit.showRewarded())

        val ads = withRewarded()
        onScreen = null
        assertFalse(ads.showRewarded())
        onScreen = activity
        flags.set(FeatureFlag.REWARDED_ADS, false)
        assertFalse(ads.showRewarded())
        flags.set(FeatureFlag.REWARDED_ADS, true)
        consent.canRequest = false
        assertFalse(ads.showRewarded())
        assertTrue(sdk.rewardedOn.isEmpty())
    }

    @Test
    fun `no rewarded ad loaded means no reward, and one starts loading`() = runTest {
        sdk.rewardedFill = false
        val ads = withRewarded()
        assertFalse(ads.rewardedReady.value)
        assertFalse(ads.showRewarded())
        sdk.rewardedFill = true
        runCurrent()
        assertTrue(ads.rewardedReady.value)
    }

    @Test
    fun `a rewarded ad loaded over an hour ago is dropped`() = runTest {
        val ads = withRewarded()
        now += AD_LIFETIME_MILLIS
        assertFalse(ads.showRewarded())
        assertTrue(sdk.rewardedOn.isEmpty())
    }

    @Test
    fun `the banner shows once the SDK has started, and follows consent, ads_enabled and remove ads`() = runTest {
        val banner = mutableListOf<Boolean>()
        val ads = manager(AdsConfig("unit-1", bannerUnitId = "banner-1"))
        backgroundScope.launch { ads.bannerAllowed.collect { banner += it } }
        runCurrent()
        assertEquals(listOf(false), banner)

        ads.gatherConsent(activity)
        runCurrent()
        assertEquals(listOf(false, true), banner)

        flags.set(FeatureFlag.ADS_ENABLED, false)
        runCurrent()
        flags.set(FeatureFlag.ADS_ENABLED, true)
        runCurrent()
        assertEquals(listOf(false, true, false, true), banner)

        consent.onPrivacyOptions = { consent.canRequest = false }
        ads.showPrivacyOptions()
        runCurrent()
        assertEquals("consent withdrawn", false, banner.last())

        consent.canRequest = true
        consent.onPrivacyOptions = {}
        ads.showPrivacyOptions()
        runCurrent()
        assertEquals(true, banner.last())
        adsRemoved.value = true
        runCurrent()
        assertEquals("removed for good", false, banner.last())
    }

    @Test
    fun `no banner without a unit, nor with ads turned off in the config`() = runTest {
        val noUnit = started()
        assertFalse(noUnit.bannerAllowed.first())
        val off = manager(AdsConfig("unit-1", bannerUnitId = "banner-1", enabled = false))
        off.gatherConsent(activity)
        runCurrent()
        assertFalse(off.bannerAllowed.first())
    }

    private class FakeConsent : ConsentGateway {
        var canRequest = true
        var privacyRequired = false
        var updates = 0
        var onUpdate: () -> Unit = {}
        var waitOnUpdate: CompletableDeferred<Unit>? = null
        var onPrivacyOptions: () -> Unit = {}
        val privacyShownOn = mutableListOf<Activity>()

        override suspend fun update(activity: Activity) {
            updates++
            waitOnUpdate?.await()
            onUpdate()
        }

        override fun canRequestAds() = canRequest

        override fun privacyOptionsRequired() = privacyRequired

        override suspend fun showPrivacyOptions(activity: Activity) {
            privacyShownOn += activity
            onPrivacyOptions()
        }
    }

    private class FakeSdk : AdsSdk {
        var initialized = 0
        var loads = 0
        val unitIds = mutableListOf<String>()
        var fill = true
        var waitOnLoad: CompletableDeferred<Unit>? = null
        var closeAd: CompletableDeferred<Unit>? = null
        val shownOn = mutableListOf<Activity>()
        val shown get() = shownOn.size
        var rewardedFill = true
        var earn = true
        val rewardedOn = mutableListOf<Activity>()

        override suspend fun loadRewarded(unitId: String): Rewarded? {
            unitIds += unitId
            return if (rewardedFill) {
                Rewarded { activity ->
                    rewardedOn += activity
                    earn
                }
            } else {
                null
            }
        }

        override suspend fun initialize() {
            initialized++
        }

        override suspend fun loadInterstitial(unitId: String): Interstitial? {
            loads++
            unitIds += unitId
            waitOnLoad?.await()
            return if (fill) {
                Interstitial { activity ->
                    shownOn += activity
                    closeAd?.await()
                }
            } else {
                null
            }
        }
    }
}
