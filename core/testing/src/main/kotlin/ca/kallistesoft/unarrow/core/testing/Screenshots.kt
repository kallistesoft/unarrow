package ca.kallistesoft.unarrow.core.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage

/**
 * Robolectric devices for screenshot tests (`@Config(qualifiers = …)`): a typical phone in the two preview variants,
 * light in English and dark in French.
 */
object ScreenshotDevice {
    const val LIGHT_EN = "en-w411dp-h891dp-notnight-xhdpi"
    const val DARK_FR = "fr-w411dp-h891dp-night-xhdpi"

    /** SDK the screenshots render with (the newest Robolectric ships). */
    const val SDK = 35
}

/**
 * Renders [content] (usually a preview) in inspection mode, like Android Studio previews, and captures
 * it to `src/test/screenshots/<name>.png`. Capturing only happens in the Roborazzi record/verify tasks; a plain test
 * run still checks that the composable renders.
 */
fun ComposeContentTestRule.captureScreenshot(name: String, content: @Composable () -> Unit) {
    setContent { CompositionLocalProvider(LocalInspectionMode provides true, content = content) }
    onRoot().captureRoboImage("src/test/screenshots/$name.png")
}
