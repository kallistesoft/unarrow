package ca.kallistesoft.unarrow.feature.settings

import android.app.Activity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onRoot
import androidx.core.os.LocaleListCompat
import ca.kallistesoft.unarrow.core.ads.AdConsent
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.FakePurchases
import ca.kallistesoft.unarrow.core.testing.MainDispatcherRule
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import ca.kallistesoft.unarrow.core.testing.collecting
import ca.kallistesoft.unarrow.core.testing.setContentWithSystemBars
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
    fun `the top bar stays under the status bar, the last row above the navigation bar`() {
        val (statusBar, navigationBar) = with(compose.density) { 100.toDp() to 60.toDp() }
        compose.setContentWithSystemBars(top = 100, bottom = 60) {
            UnarrowTheme {
                SettingsContent(SettingsUiState(loading = false), SettingsActions(), AppLanguage.SYSTEM, "1.0")
            }
        }
        assertTrue(compose.onNodeWithContentDescription("Back").getUnclippedBoundsInRoot().top >= statusBar)
        assertTrue(compose.onNodeWithText("Settings").getUnclippedBoundsInRoot().top >= statusBar)
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        val version = compose.onNodeWithText("Version 1.0").performScrollTo().getUnclippedBoundsInRoot()
        assertTrue(version.bottom <= root.bottom - navigationBar)
    }

    @Test
    fun `language tags map to the app's languages`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags(""))
        assertEquals(AppLanguage.FRENCH_CANADA, AppLanguage.fromTags("fr-CA"))
        assertEquals(AppLanguage.FRENCH_FRANCE, AppLanguage.fromTags("fr-FR,en"))
        assertEquals(AppLanguage.FRENCH_FRANCE, AppLanguage.fromTags("fr-BE"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTags("en-GB"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTags("de-DE"))
    }

    @Test
    fun `the screen switches the language`() {
        val vm = SettingsViewModel(FakeProgressRepository(), FakePurchases(), FakeConsent())
        compose.setContent { SettingsScreen(onBack = {}, versionName = "1.0", viewModel = vm) }
        AppLanguage.current() // Readable without an AppCompat activity (SYSTEM then).
        compose.onNodeWithText("English").performScrollTo().performClick()
        compose.onNodeWithText("Version 1.0").performScrollTo().assertExists()
        AppLanguage.SYSTEM.select()
    }

    @Test
    fun `the preview renders`() {
        compose.setContent { SettingsPreview() }
        compose.onNodeWithText("Privacy options").assertExists()
    }
}
