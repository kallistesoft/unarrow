package ca.kallistesoft.unarrow.feature.settings

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.LocaleListCompat
import ca.kallistesoft.unarrow.core.ads.AdConsent
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.FakePurchases
import ca.kallistesoft.unarrow.core.testing.MainDispatcherRule
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import ca.kallistesoft.unarrow.core.testing.collecting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK], qualifiers = ScreenshotDevice.LIGHT_EN)
class SettingsTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    @After
    fun resetLanguage() = AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())

    private class FakeConsent : AdConsent {
        override val privacyOptionsRequired = MutableStateFlow(true)
        var shown = 0

        override fun gatherConsent(activity: Activity) = Unit

        override fun showPrivacyOptions() {
            shown++
        }
    }

    @Test
    fun `settings save sound and vibration, restore purchases and open privacy options`() = runTest {
        val progress = FakeProgressRepository()
        val purchases = FakePurchases()
        val consent = FakeConsent()
        val vm = SettingsViewModel(progress, purchases, consent)
        collecting(vm.uiState)
        assertTrue(vm.uiState.value.privacyOptionsRequired)

        vm.setSound(false)
        vm.setHaptics(false)
        assertEquals(GameProgress(soundOn = false, hapticsOn = false), progress.state.value)
        assertFalse(vm.uiState.value.soundOn)

        vm.restorePurchases()
        assertEquals(1, purchases.refreshes)
        assertTrue(vm.uiState.value.restored)

        vm.showPrivacyOptions()
        assertEquals(1, consent.shown)
    }

    @Test
    fun `rows call back`() {
        val calls = mutableListOf<String>()
        compose.setContent {
            UnarrowTheme {
                SettingsContent(
                    SettingsUiState(loading = false, privacyOptionsRequired = true, restored = true),
                    SettingsActions(
                        onBack = { calls += "back" },
                        onSound = { calls += "sound $it" },
                        onHaptics = { calls += "haptics $it" },
                        onLanguage = { calls += it.name },
                        onRestore = { calls += "restore" },
                        onPrivacy = { calls += "privacy" },
                        onRate = { calls += "rate" },
                    ),
                    language = AppLanguage.SYSTEM,
                    versionName = "1.2.3",
                )
            }
        }
        compose.onNodeWithText("Sound").performClick()
        compose.onNodeWithText("Vibration").performClick()
        compose.onNodeWithText("Français (Canada)").performScrollTo().performClick()
        compose.onNodeWithText("Restore purchases").performScrollTo().performClick()
        compose.onNodeWithText("Purchases restored").assertExists()
        compose.onNodeWithText("Privacy options").performScrollTo().performClick()
        compose.onNodeWithText("Rate Unarrow").performScrollTo().performClick()
        compose.onNodeWithText("Version 1.2.3").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(
            listOf("sound false", "haptics false", "FRENCH_CANADA", "restore", "privacy", "rate", "back"),
            calls,
        )
    }

    @Test
    fun `the chosen language is applied and read back`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.current())
        AppLanguage.FRENCH_FRANCE.select()
        assertEquals(AppLanguage.FRENCH_FRANCE, AppLanguage.current())
        AppLanguage.FRENCH_CANADA.select()
        assertEquals(AppLanguage.FRENCH_CANADA, AppLanguage.current())
        AppLanguage.SYSTEM.select()
        assertEquals(AppLanguage.SYSTEM, AppLanguage.current())
    }

    @Test
    fun `the screen shows the current language`() {
        compose.setContent { SettingsScreen(onBack = {}, versionName = "1.0", viewModel = SettingsViewModel(FakeProgressRepository(), FakePurchases(), FakeConsent())) }
        compose.onNodeWithText("English").performScrollTo().performClick()
        assertEquals(AppLanguage.ENGLISH, AppLanguage.current())
    }

    @Test
    fun `the preview renders`() {
        compose.setContent { SettingsPreview() }
        compose.onNodeWithText("Privacy options").assertExists()
    }
}
