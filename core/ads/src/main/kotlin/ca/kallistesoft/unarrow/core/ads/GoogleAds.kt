package ca.kallistesoft.unarrow.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** The consent SDK, as [AdsManager] needs it. Errors (offline, no form configured) only end the call. */
internal interface ConsentGateway {
    /** Refreshes the consent information, then shows the consent form if the user's region requires it. */
    suspend fun update(activity: Activity)

    fun canRequestAds(): Boolean

    fun privacyOptionsRequired(): Boolean

    suspend fun showPrivacyOptions(activity: Activity)
}

/** The Mobile Ads SDK, as [AdsManager] needs it. */
internal interface AdsSdk {
    suspend fun initialize()

    /** A loaded interstitial, or null when none could be loaded (no fill, offline). */
    suspend fun loadInterstitial(unitId: String): Interstitial?

    /** A loaded rewarded ad, or null when none could be loaded. */
    suspend fun loadRewarded(unitId: String): Rewarded?
}

internal fun interface Interstitial {
    /** Shows the ad full screen and returns when it's closed, or at once if it can't be shown. */
    suspend fun show(activity: Activity)
}

internal fun interface Rewarded {
    /** Shows the ad full screen; true once it's closed if the player earned the reward. */
    suspend fun show(activity: Activity): Boolean
}

/** Google's User Messaging Platform. */
internal class UmpConsent(private val info: ConsentInformation) : ConsentGateway {
    override suspend fun update(activity: Activity) = suspendCancellableCoroutine { done ->
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { done.resume(Unit) } },
            { done.resume(Unit) },
        )
    }

    override fun canRequestAds(): Boolean = info.canRequestAds()

    override fun privacyOptionsRequired(): Boolean =
        info.privacyOptionsRequirementStatus == PrivacyOptionsRequirementStatus.REQUIRED

    override suspend fun showPrivacyOptions(activity: Activity) = suspendCancellableCoroutine { done ->
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { done.resume(Unit) }
    }
}

/** Google Mobile Ads. Initialised off the main thread ([background]), as Google recommends. */
internal class GoogleAdsSdk(
    private val context: Context,
    private val background: CoroutineDispatcher = Dispatchers.IO,
) : AdsSdk {
    override suspend fun initialize() = withContext(background) {
        suspendCancellableCoroutine { done -> MobileAds.initialize(context) { done.resume(Unit) } }
    }

    override suspend fun loadInterstitial(unitId: String): Interstitial? = suspendCancellableCoroutine { done ->
        InterstitialAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) = done.resume(GoogleInterstitial(ad))

                override fun onAdFailedToLoad(error: LoadAdError) = done.resume(null)
            },
        )
    }

    override suspend fun loadRewarded(unitId: String): Rewarded? = suspendCancellableCoroutine { done ->
        RewardedAd.load(
            context,
            unitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) = done.resume(GoogleRewarded(ad))

                override fun onAdFailedToLoad(error: LoadAdError) = done.resume(null)
            },
        )
    }
}

internal class GoogleRewarded(private val ad: RewardedAd) : Rewarded {
    override suspend fun show(activity: Activity): Boolean = suspendCancellableCoroutine { done ->
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = done.resume(earned)

            override fun onAdFailedToShowFullScreenContent(error: AdError) = done.resume(false)
        }
        ad.show(activity) { earned = true }
    }
}

internal class GoogleInterstitial(private val ad: InterstitialAd) : Interstitial {
    override suspend fun show(activity: Activity) = suspendCancellableCoroutine { done ->
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() = done.resume(Unit)

            override fun onAdFailedToShowFullScreenContent(error: AdError) = done.resume(Unit)
        }
        ad.show(activity)
    }
}
