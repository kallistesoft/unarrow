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
import org.junit.Assert.assertTrue
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
        assertEquals(Color(0xFFF6F8FC), lightBackground) // Nocturne's bg
    }

    @Test
    fun `the spacing scale grows`() {
        val scale = listOf(Spacing.xs, Spacing.s, Spacing.m, Spacing.l, Spacing.xl, Spacing.xxl)
        assertEquals(scale.sorted(), scale)
        assertTrue(Spacing.screen > Spacing.l)
        assertTrue(Sizes.touchTarget >= Sizes.icon)
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

    @Test
    fun `a status column and the preview surface show their content`() {
        compose.setContent {
            PreviewSurface {
                androidx.compose.material3.Scaffold { padding ->
                    StatusColumn(padding) { androidx.compose.material3.Text("Centred") }
                }
            }
        }
        compose.onNodeWithText("Centred").assertExists()
    }

    @Test
    fun `the studio's look - accent, Manrope headings, cards, chips and pills`() {
        var primary = Color.Unspecified
        var heading: androidx.compose.ui.text.TextStyle? = null
        var cardTaps = 0
        compose.setContent {
            UnarrowTheme(darkTheme = true) {
                primary = MaterialTheme.colorScheme.primary
                heading = MaterialTheme.typography.headlineMedium
                Column {
                    BrandCard(onClick = { cardTaps++ }) { androidx.compose.material3.Text("Card") }
                    BrandCard { androidx.compose.material3.Text("Plain card") }
                    Chip("4-day streak") { androidx.compose.material3.Text("*") }
                    Chip("Best")
                    StatusPill("Restored")
                    androidx.compose.material3.Text("0:42", style = MonoLabel)
                }
            }
        }
        compose.onNodeWithText("Card").performClick()
        assertEquals(1, cardTaps)
        for (text in listOf("Plain card", "4-day streak", "Best", "Restored", "0:42")) compose.onNodeWithText(text).assertExists()
        assertEquals(Color(0xFF2F6BFF), primary)
        assertEquals(Manrope, heading!!.fontFamily)
        assertEquals(androidx.compose.ui.text.font.FontWeight.ExtraBold, heading!!.fontWeight)
        assertEquals(JetBrainsMono, MonoLabel.fontFamily)
    }
}
