package ca.kallistesoft.unarrow.core.ads

import android.app.Activity
import ca.kallistesoft.unarrow.core.flags.FeatureFlag
import ca.kallistesoft.unarrow.core.flags.FeatureFlags
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * The ad rules, on top of the SDKs ([consent], [sdk]):
 * - nothing is initialised, loaded or shown until consent allows ads to be requested, nor while `ads_enabled` is off;
 * - interstitials pace themselves ([InterstitialAds]) and stop for good once [adsRemoved] ("Remove ads" bought);
 *   rewarded ads stay available, since the player asks for them, unless `rewarded_ads` is off;
 * - ads are preloaded, so the game never waits for a network request: not loaded means no ad;
 * - at most one interstitial every [CAP_MILLIS] (AdMob's frequency cap is set to the same);
 * - a loaded ad older than [AD_LIFETIME_MILLIS] is dropped (AdMob doesn't serve stale ones) and loaded again.
 *
 * Runs on the main thread ([scope] and callers), as the SDKs require; [clock] is the elapsed real time in ms.
 */
internal class AdsManager(
    private val consent: ConsentGateway,
    private val sdk: AdsSdk,
    private val flags: FeatureFlags,
    private val currentActivity: () -> Activity?,
    private val config: AdsConfig,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
    private val adsRemoved: Flow<Boolean> = flowOf(false),
) : InterstitialAds, RewardedAds, AdConsent {
    private val privacyRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = privacyRequired.asStateFlow()

    private var initialized = false
    private var loading: Job? = null
    private var ad: Interstitial? = null
    private var loadedAt = 0L
    private var lastShown: Long? = null
    private var solvedSinceAd = 0

    private var rewardedLoading: Job? = null
    private var rewarded: Rewarded? = null
    private var rewardedLoadedAt = 0L
    private val rewardedState = MutableStateFlow(false)
    override val rewardedReady: StateFlow<Boolean> = rewardedState.asStateFlow()

    override fun gatherConsent(activity: Activity) {
        if (!config.enabled) return
        scope.launch {
            // Consent given on an earlier launch: ads can start while the information is refreshed.
            startIfAllowed()
            consent.update(activity)
            privacyRequired.value = consent.privacyOptionsRequired()
            startIfAllowed()
        }
    }

    override fun showPrivacyOptions() {
        val activity = currentActivity() ?: return
        scope.launch {
            consent.showPrivacyOptions(activity)
            privacyRequired.value = consent.privacyOptionsRequired()
            startIfAllowed()
        }
    }

    override suspend fun showAfterPuzzle(level: Int) {
        if (level < FIRST_AD_LEVEL || adsRemoved.first()) return
        solvedSinceAd++
        if (solvedSinceAd < INTERSTITIAL_EVERY) return
        if (showInterstitial()) solvedSinceAd = 0
    }

    /** Shows the preloaded interstitial now if the rules allow it; true when one was shown. */
    internal suspend fun showInterstitial(): Boolean {
        if (!allowed()) return false
        val last = lastShown
        if (last != null && clock() - last < CAP_MILLIS) return false
        val activity = currentActivity() ?: return false
        val ready = ad?.takeIf { clock() - loadedAt < AD_LIFETIME_MILLIS }
        ad = null
        if (ready != null) {
            lastShown = clock()
            ready.show(activity)
        }
        preload()
        return ready != null
    }

    override suspend fun showRewarded(): Boolean {
        if (!rewardedAllowed()) return false
        val activity = currentActivity() ?: return false
        val ready = rewarded?.takeIf { clock() - rewardedLoadedAt < AD_LIFETIME_MILLIS }
        rewarded = null
        rewardedState.value = false
        val earned = ready?.show(activity) ?: false
        preloadRewarded()
        return earned
    }

    private suspend fun allowed(): Boolean =
        config.enabled && consent.canRequestAds() && flags.isEnabled(FeatureFlag.ADS_ENABLED).first()

    private suspend fun rewardedAllowed(): Boolean =
        config.rewardedUnitId.isNotEmpty() && allowed() && flags.isEnabled(FeatureFlag.REWARDED_ADS).first()

    private suspend fun startIfAllowed() {
        if (allowed()) {
            if (!adsRemoved.first()) preload()
            if (rewardedAllowed()) preloadRewarded()
        }
    }

    /** Loads the next interstitial, unless one is ready or loading. A failed load leaves none: the next test retries. */
    private fun preload() {
        if (ad != null || loading?.isActive == true) return
        loading = scope.launch {
            initializeOnce()
            ad = sdk.loadInterstitial(config.interstitialUnitId)
            loadedAt = clock()
        }
    }

    /** Loads the next rewarded ad, unless one is ready or loading; [rewardedReady] turns true once it's in. */
    private fun preloadRewarded() {
        if (rewarded != null || rewardedLoading?.isActive == true) return
        rewardedLoading = scope.launch {
            initializeOnce()
            rewarded = sdk.loadRewarded(config.rewardedUnitId)
            rewardedLoadedAt = clock()
            rewardedState.value = rewarded != null
        }
    }

    private suspend fun initializeOnce() {
        if (!initialized) {
            initialized = true
            sdk.initialize()
        }
    }
}

/** At most one interstitial every 3 minutes. */
internal const val CAP_MILLIS = 3 * 60 * 1_000L

/** A loaded interstitial is shown within the hour, or dropped. */
internal const val AD_LIFETIME_MILLIS = 60 * 60 * 1_000L
