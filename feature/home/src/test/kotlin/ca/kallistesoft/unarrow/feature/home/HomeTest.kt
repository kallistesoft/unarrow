package ca.kallistesoft.unarrow.feature.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.kallistesoft.unarrow.core.billing.Product
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.FakePurchases
import ca.kallistesoft.unarrow.core.testing.MainDispatcherRule
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import ca.kallistesoft.unarrow.core.testing.collecting
import kotlinx.coroutines.test.runTest
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
class HomeTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val today = Levels.FIRST_DAILY_EPOCH_DAY + 4

    @Test
    fun `the home shows the level, today's daily, the streak and the purchase`() = runTest {
        val progress = FakeProgressRepository(
            GameProgress(level = 9, dailyStreak = 3, lastDailyDay = today - 1),
            today = today,
        )
        val purchases = FakePurchases()
        val vm = HomeViewModel(progress, purchases, { today })
        collecting(vm.uiState)
        val state = vm.uiState.value
        assertEquals(9, state.level)
        assertEquals(5L, state.dailyNumber)
        assertEquals(3, state.streak)
        assertFalse(state.dailyDone)
        assertEquals("$3.99", state.removeAdsPrice)

        vm.buyRemoveAds()
        assertEquals(listOf(Product.REMOVE_ADS), purchases.bought)

        progress.completeDaily()
        assertTrue(vm.uiState.value.dailyDone)
        assertEquals(4, vm.uiState.value.streak)
    }

    @Test
    fun `buttons lead to the game, the daily, settings and the purchase`() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            UnarrowTheme {
                HomeContent(
                    HomeUiState(loading = false, level = 4, dailyNumber = 7, streak = 2, removeAdsPrice = "$3.99"),
                    onPlay = { clicks += "play" },
                    onDaily = { clicks += "daily" },
                    onSettings = { clicks += "settings" },
                    onRemoveAds = { clicks += "ads" },
                )
            }
        }
        compose.onNodeWithText("Play · Level 4").performClick()
        compose.onNodeWithText("Daily puzzle").performClick()
        compose.onNodeWithText("Daily #7").performClick()
        compose.onNodeWithContentDescription("Settings").performClick()
        compose.onNodeWithText("Remove ads · $3.99").performClick()
        compose.onNodeWithText("2-day streak").assertExists()
        assertEquals(listOf("play", "daily", "daily", "settings", "ads"), clicks)
    }

    @Test
    fun `a solved daily and bought ads say so`() {
        compose.setContent {
            UnarrowTheme {
                HomeContent(
                    HomeUiState(loading = false, dailyDone = true, removeAdsOwned = true),
                    onPlay = {},
                    onDaily = {},
                    onSettings = {},
                    onRemoveAds = {},
                )
            }
        }
        compose.onNodeWithText("Solved today. Come back tomorrow!").assertExists()
        compose.onNodeWithText("No ads. Thank you!").assertExists()
        compose.onNodeWithText("Daily puzzle").assertDoesNotExist()
    }

    @Test
    fun `the preview renders`() {
        compose.setContent { HomePreview() }
        compose.onNodeWithText("Play · Level 23").assertExists()
    }
}
