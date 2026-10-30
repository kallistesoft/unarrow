package ca.kallistesoft.unarrow.core.update

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
class ForceUpdateScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = LIGHT_EN)
    fun forceUpdateLightEn() = compose.captureScreenshot("force_update_light_en") { ForceUpdatePreview() }

    @Test
    @Config(qualifiers = DARK_FR)
    fun forceUpdateDarkFr() = compose.captureScreenshot("force_update_dark_fr") { ForceUpdatePreview() }
}
