package ca.kallistesoft.unarrow.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK])
class ThemeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `light and dark themes give the board different colours`() {
        var light: GameColors? = null
        var dark: GameColors? = null
        var lightBackground = Color.Unspecified
        compose.setContent {
            UnarrowTheme(darkTheme = false) {
                light = UnarrowTheme.gameColors
                lightBackground = MaterialTheme.colorScheme.background
            }
            UnarrowTheme(darkTheme = true) { dark = UnarrowTheme.gameColors }
        }
        assertNotEquals(light, dark)
        assertEquals(Color(0xFFF6F4EF), lightBackground)
    }

    @Test
    fun `buttons call back when tapped, and not when disabled`() {
        var taps = 0
        compose.setContent {
            UnarrowTheme {
                Column {
                    PrimaryButton("Primary", onClick = { taps++ })
                    SecondaryButton("Secondary", onClick = { taps++ })
                    TertiaryButton("Tertiary", onClick = { taps++ })
                    PrimaryButton("Off", onClick = { taps++ }, enabled = false)
                }
            }
        }
        compose.onNodeWithText("Primary").performClick()
        compose.onNodeWithText("Secondary").performClick()
        compose.onNodeWithText("Tertiary").performClick()
        compose.onNodeWithText("Off").assertIsNotEnabled()
        assertEquals(3, taps)
    }
}
