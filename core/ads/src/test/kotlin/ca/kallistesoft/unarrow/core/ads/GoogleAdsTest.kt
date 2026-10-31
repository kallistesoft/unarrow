package ca.kallistesoft.unarrow.core.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentForm
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentInformation.OnConsentInfoUpdateFailureListener
import com.google.android.ump.ConsentInformation.OnConsentInfoUpdateSuccessListener
import com.google.android.ump.ConsentInformation.PrivacyOptionsRequirementStatus
import com.google.android.ump.UserMessagingPlatform
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice

/** The SDK adapters: each SDK callback ends the matching suspend call. The SDKs themselves are mocked. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK])
class GoogleAdsTest {
    private val context: Context = RuntimeEnvironment.getApplication()
    private val activity = mockk<Activity>()

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `consent update ends after the form, or at once on an error`() = runTest {
        mockkStatic(UserMessagingPlatform::class)
        val info = mockk<ConsentInformation>()
        val success = slot<OnConsentInfoUpdateSuccessListener>()
        val failure = slot<OnConsentInfoUpdateFailureListener>()
        every { info.requestConsentInfoUpdate(activity, any(), capture(success), capture(failure)) } returns Unit
        val dismissed = slot<ConsentForm.OnConsentFormDismissedListener>()
        every { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, capture(dismissed)) } returns Unit
        val consent = UmpConsent(info)

        val afterForm = async { consent.update(activity) }
        runCurrent()
        success.captured.onConsentInfoUpdateSuccess()
        assertFalse("waits for the form", afterForm.isCompleted)
        dismissed.captured.onConsentFormDismissed(null)
        afterForm.await()

        val offline = async { consent.update(activity) }
        runCurrent()
        failure.captured.onConsentInfoUpdateFailure(mockk())
        offline.await()
        verify(exactly = 1) { UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity, any()) }
    }

    @Test
    fun `consent state and privacy options come from the consent information`() = runTest {
        mockkStatic(UserMessagingPlatform::class)
        val info = mockk<ConsentInformation> {
            every { canRequestAds() } returns true
            every { privacyOptionsRequirementStatus } returnsMany
                listOf(PrivacyOptionsRequirementStatus.REQUIRED, PrivacyOptionsRequirementStatus.NOT_REQUIRED)
        }
        val dismissed = slot<ConsentForm.OnConsentFormDismissedListener>()
        every { UserMessagingPlatform.showPrivacyOptionsForm(activity, capture(dismissed)) } returns Unit
        val consent = UmpConsent(info)

        assertTrue(consent.canRequestAds())
        assertTrue(consent.privacyOptionsRequired())
        assertFalse(consent.privacyOptionsRequired())

        val form = async { consent.showPrivacyOptions(activity) }
        runCurrent()
        assertFalse(form.isCompleted)
        dismissed.captured.onConsentFormDismissed(null)
        form.await()
    }

    @Test
    fun `the SDK initialises, and an interstitial loads or doesn't`() = runTest {
        mockkStatic(MobileAds::class, InterstitialAd::class)
        val initialized = slot<OnInitializationCompleteListener>()
        every { MobileAds.initialize(context, capture(initialized)) } returns Unit
        val loaded = slot<InterstitialAdLoadCallback>()
        every { InterstitialAd.load(context, "unit-1", any(), capture(loaded)) } returns Unit
        val sdk = GoogleAdsSdk(context, StandardTestDispatcher(testScheduler))

        val init = async { sdk.initialize() }
        runCurrent()
        initialized.captured.onInitializationComplete(mockk())
        init.await()

        val filled = async { sdk.loadInterstitial("unit-1") }
        runCurrent()
        loaded.captured.onAdLoaded(mockk())
        assertNotNull(filled.await())

        val noFill = async { sdk.loadInterstitial("unit-1") }
        runCurrent()
        loaded.captured.onAdFailedToLoad(mockk<LoadAdError>())
        assertNull(noFill.await())
    }

    @Test
    fun `an interstitial returns once closed, or at once if it can't show`() = runTest {
        val callback = slot<FullScreenContentCallback>()
        val ad = mockk<InterstitialAd> {
            every { fullScreenContentCallback = capture(callback) } returns Unit
            every { show(activity) } returns Unit
        }
        val interstitial = GoogleInterstitial(ad)

        val closed = async { interstitial.show(activity) }
        runCurrent()
        verify { ad.show(activity) }
        assertFalse(closed.isCompleted)
        callback.captured.onAdDismissedFullScreenContent()
        closed.await()

        val failed = async { interstitial.show(activity) }
        runCurrent()
        callback.captured.onAdFailedToShowFullScreenContent(mockk<AdError>())
        failed.await()
    }

    @Test
    fun `a rewarded ad loads or doesn't`() = runTest {
        mockkStatic(RewardedAd::class)
        val loaded = slot<RewardedAdLoadCallback>()
        every { RewardedAd.load(context, "unit-2", any<com.google.android.gms.ads.AdRequest>(), capture(loaded)) } returns Unit
        val sdk = GoogleAdsSdk(context, StandardTestDispatcher(testScheduler))

        val filled = async { sdk.loadRewarded("unit-2") }
        runCurrent()
        loaded.captured.onAdLoaded(mockk())
        assertNotNull(filled.await())

        val noFill = async { sdk.loadRewarded("unit-2") }
        runCurrent()
        loaded.captured.onAdFailedToLoad(mockk<LoadAdError>())
        assertNull(noFill.await())
    }

    @Test
    fun `a rewarded ad tells whether the reward was earned`() = runTest {
        val callback = slot<FullScreenContentCallback>()
        val reward = slot<OnUserEarnedRewardListener>()
        val ad = mockk<RewardedAd> {
            every { fullScreenContentCallback = capture(callback) } returns Unit
            every { show(activity, capture(reward)) } returns Unit
        }
        val rewarded = GoogleRewarded(ad)

        val watched = async { rewarded.show(activity) }
        runCurrent()
        reward.captured.onUserEarnedReward(mockk())
        callback.captured.onAdDismissedFullScreenContent()
        assertEquals(true, watched.await())

        val skipped = async { rewarded.show(activity) }
        runCurrent()
        callback.captured.onAdDismissedFullScreenContent()
        assertEquals(false, skipped.await())

        val failed = async { rewarded.show(activity) }
        runCurrent()
        callback.captured.onAdFailedToShowFullScreenContent(mockk<AdError>())
        assertEquals(false, failed.await())
    }
}
