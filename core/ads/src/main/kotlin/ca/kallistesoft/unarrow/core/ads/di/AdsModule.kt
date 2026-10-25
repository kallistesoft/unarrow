package ca.kallistesoft.unarrow.core.ads.di

import android.content.Context
import android.os.SystemClock
import ca.kallistesoft.unarrow.core.ads.AdConsent
import ca.kallistesoft.unarrow.core.ads.AdsConfig
import ca.kallistesoft.unarrow.core.ads.AdsManager
import ca.kallistesoft.unarrow.core.ads.GoogleAdsSdk
import ca.kallistesoft.unarrow.core.ads.InterstitialAds
import ca.kallistesoft.unarrow.core.ads.RewardedAds
import ca.kallistesoft.unarrow.core.ads.UmpConsent
import ca.kallistesoft.unarrow.core.common.CurrentActivity
import ca.kallistesoft.unarrow.core.data.ProgressRepository
import ca.kallistesoft.unarrow.core.flags.FeatureFlags
import com.google.android.ump.UserMessagingPlatform
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

/**
 * AdMob behind [InterstitialAds], [RewardedAds] and [AdConsent]; the app provides [AdsConfig] (the ad units of its
 * build type). Interstitials stop once "Remove ads" is owned (kept in the progress by the billing module).
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AdsModule {
    @Provides
    @Singleton
    fun adsManager(
        @ApplicationContext context: Context,
        flags: FeatureFlags,
        config: AdsConfig,
        currentActivity: CurrentActivity,
        progress: ProgressRepository,
    ): AdsManager = AdsManager(
        consent = UmpConsent(UserMessagingPlatform.getConsentInformation(context)),
        sdk = GoogleAdsSdk(context),
        flags = flags,
        currentActivity = currentActivity::get,
        config = config,
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        clock = SystemClock::elapsedRealtime,
        adsRemoved = progress.progress.map { it.removeAdsOwned }.distinctUntilChanged(),
    )

    @Provides
    fun interstitialAds(manager: AdsManager): InterstitialAds = manager

    @Provides
    fun rewardedAds(manager: AdsManager): RewardedAds = manager

    @Provides
    fun adConsent(manager: AdsManager): AdConsent = manager
}
