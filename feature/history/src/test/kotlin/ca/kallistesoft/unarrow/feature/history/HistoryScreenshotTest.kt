package ca.kallistesoft.unarrow.feature.history

import androidx.compose.ui.test.junit4.createComposeRule
import ca.kallistesoft.unarrow.core.designsystem.PreviewSurface
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

/** Screenshots of the history, light in English and dark in French (see README, "Screenshot tests"). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [ScreenshotDevice.SDK])
class HistoryScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    @Config(qualifiers = LIGHT_EN)
    fun historyLightEn() = compose.captureScreenshot("history_light_en") { HistoryPreview() }

    @Test
    @Config(qualifiers = DARK_FR)
    fun historyDarkFr() = compose.captureScreenshot("history_dark_fr") { HistoryPreview() }

    @Test
    @Config(qualifiers = LIGHT_EN)
    fun emptyLightEn() = compose.captureScreenshot("history_empty_light_en") {
        PreviewSurface { HistoryContent(HistoryUiState(loading = false, rewardedReady = false)) }
    }

    @Test
    @Config(qualifiers = DARK_FR)
    fun emptyDarkFr() = compose.captureScreenshot("history_empty_dark_fr") {
        PreviewSurface { HistoryContent(HistoryUiState(loading = false, tab = HistoryTab.DAILY)) }
    }
}
