package ca.kallistesoft.unarrow.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice.DARK_FR
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice.LIGHT_EN
import ca.kallistesoft.unarrow.core.testing.captureScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screenshots of this module's previews, light in English and dark in French (see README, "Screenshot tests"). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [ScreenshotDevice.SDK])
class HomeScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = LIGHT_EN)
    fun homeLightEn() = compose.captureScreenshot("home_light_en") { HomePreview() }

    @Test
    @Config(qualifiers = DARK_FR)
    fun homeDarkFr() = compose.captureScreenshot("home_dark_fr") { HomePreview() }
}
