package ca.kallistesoft.unarrow.core.ads.di

import android.content.Context
import ca.kallistesoft.unarrow.core.ads.AdsConfig
import ca.kallistesoft.unarrow.core.testing.FakeFeatureFlags
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.FakeRemoteNumbers
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK])
class AdsModuleTest {
    private val context: Context = RuntimeEnvironment.getApplication()

    @Test
    fun `one manager serves every ad and the consent, and nothing is asked before launch`() {
        val manager = AdsModule.adsManager(context, FakeFeatureFlags(), FakeRemoteNumbers(), AdsConfig("unit-1"), { null }, FakeProgressRepository())
        assertSame(manager, AdsModule.interstitialAds(manager))
        assertSame(manager, AdsModule.rewardedAds(manager))
        assertSame(manager, AdsModule.bannerAds(manager))
        assertSame(manager, AdsModule.adConsent(manager))
        assertFalse(manager.privacyOptionsRequired.value)
    }
}
