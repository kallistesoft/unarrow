package ca.kallistesoft.unarrow.di

import ca.kallistesoft.unarrow.BuildConfig
import ca.kallistesoft.unarrow.core.ads.AdsConfig
import ca.kallistesoft.unarrow.core.flags.FeatureFlagSettings
import ca.kallistesoft.unarrow.core.update.ForceUpdateSettings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    fun forceUpdateSettings(config: BuildTypeConfig) = ForceUpdateSettings(
        parameter = config.minVersionParameter,
        currentVersionCode = BuildConfig.VERSION_CODE.toLong(),
        storePackage = BuildConfig.STORE_PACKAGE,
    )

    @Provides
    fun featureFlagSettings(config: BuildTypeConfig) =
        FeatureFlagSettings(minimumFetchIntervalSeconds = config.featureFlagFetchIntervalSeconds)

    /**
     * The ad units of the build type (app/build.gradle.kts). Ads are off when the app runs under instrumented tests:
     * an interstitial or the consent form would cover the screens they drive.
     */
    @Provides
    fun adsConfig() = AdsConfig(
        interstitialUnitId = BuildConfig.ADMOB_INTERSTITIAL_ID,
        rewardedUnitId = BuildConfig.ADMOB_REWARDED_ID,
        bannerUnitId = BuildConfig.ADMOB_BANNER_ID,
        enabled = !underInstrumentation(),
    )

    /** AndroidX Test is loaded into the app's process only when a test APK instruments it. */
    private fun underInstrumentation(): Boolean =
        runCatching { Class.forName("androidx.test.platform.app.InstrumentationRegistry") }.isSuccess
}
