package ca.kallistesoft.unarrow.core.ads

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import ca.kallistesoft.unarrow.core.testing.FakeFeatureFlags
import ca.kallistesoft.unarrow.core.testing.FakeRemoteNumbers
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import com.google.android.gms.ads.AdView
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The banner composable on a real AdView (no network in tests): no room until loaded, removed when it leaves. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK])
class BannerTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `the banner takes room once loaded, and is removed when it leaves`() {
        val manager = AdsManager(
            consent = mockk(), sdk = mockk(), flags = FakeFeatureFlags(), numbers = FakeRemoteNumbers(), currentActivity = { null },
            config = AdsConfig("unit-1", bannerUnitId = "banner-1"), scope = CoroutineScope(Dispatchers.Unconfined),
            clock = { 0L },
        )
        var shown by mutableStateOf(true)
        compose.setContent {
            Box { if (shown) manager.Banner(Modifier) }
        }
        val adView = adView()!!
        assertEquals("banner-1", adView.adUnitId)
        assertEquals("no room before an ad", 0f, height())

        compose.runOnUiThread { adView.adListener.onAdLoaded() }
        compose.waitForIdle()
        assertTrue("anchored banner height", height() >= 50f)

        shown = false
        compose.waitForIdle()
        assertNull(adView())
    }

    private fun height(): Float = compose.onNodeWithTag("banner").getUnclippedBoundsInRoot().let { it.bottom - it.top }.value

    private fun adView(): AdView? = compose.activity.window.decorView.find()

    private fun View.find(): AdView? = when (this) {
        is AdView -> this
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { getChildAt(it).find() }
        else -> null
    }
}
