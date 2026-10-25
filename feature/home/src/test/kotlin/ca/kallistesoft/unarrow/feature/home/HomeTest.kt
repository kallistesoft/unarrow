package ca.kallistesoft.unarrow.feature.home

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.kallistesoft.unarrow.core.billing.Product
import ca.kallistesoft.unarrow.core.data.DailyResult
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.testing.FakeAds
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.FakePurchases
import ca.kallistesoft.unarrow.core.testing.MainDispatcherRule
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import ca.kallistesoft.unarrow.core.testing.collecting
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.performScrollTo
import org.robolectric.annotation.GraphicsMode
import androidx.compose.ui.test.onRoot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK], qualifiers = ScreenshotDevice.LIGHT_EN)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
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
        val vm = HomeViewModel(progress, purchases, FakeAds(), { today })
        collecting(vm.uiState)
        val state = vm.uiState.value
        assertEquals(9, state.level)
        assertEquals(5L, state.dailyNumber)
        assertEquals(3, state.streak)
        assertFalse(state.dailyDone)
        assertEquals("$3.99", state.removeAdsPrice)

        vm.buyRemoveAds()
        assertEquals(listOf(Product.REMOVE_ADS), purchases.bought)

        progress.completeDaily(DailyResult(stars = 3, seconds = 42))
        assertTrue(vm.uiState.value.dailyDone)
        assertEquals(4, vm.uiState.value.streak)
    }

    @Test
    fun `a solved daily can be played again after a rewarded ad, without growing the streak`() = runTest {
        val progress = FakeProgressRepository(GameProgress(dailyStreak = 2, lastDailyDay = today), today = today)
        val ads = FakeAds()
        val vm = HomeViewModel(progress, FakePurchases(), ads, { today })
        collecting(vm.uiState)
        assertTrue(vm.uiState.value.rewardedReady)
        var retries = 0

        vm.retryDaily { retries++ }
        assertEquals(1, retries)

        ads.earn = false
        vm.retryDaily { retries++ }
        assertEquals("an ad closed early doesn't reopen the daily", 1, retries)
        assertEquals(2, ads.rewardedShown)

        progress.completeDaily(DailyResult(stars = 3, seconds = 30))
        assertEquals(2, vm.uiState.value.streak)
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
    fun `a solved daily and bought ads say so, and the daily can't be played again`() {
        var dailyClicks = 0
        compose.setContent {
            UnarrowTheme {
                HomeContent(
                    HomeUiState(loading = false, dailyNumber = 7, dailyDone = true, removeAdsOwned = true),
                    onPlay = {},
                    onDaily = { dailyClicks++ },
                    onSettings = {},
                    onRemoveAds = {},
                )
            }
        }
        compose.onNodeWithText("Solved today. Come back tomorrow!").assertExists()
        compose.onNodeWithText("No ads. Thank you!").assertExists()
        compose.onNodeWithText("Daily puzzle").assertDoesNotExist()
        compose.onNodeWithTag("daily").assertIsNotEnabled()
        compose.onNodeWithText("Daily #7").performClick()
        assertEquals(0, dailyClicks)
        compose.onNodeWithText("Play again (ad)").assertDoesNotExist()
    }

    @Test
    fun `the home screen replays a solved daily after a rewarded ad`() {
        val progress = FakeProgressRepository(GameProgress(dailyStreak = 1, lastDailyDay = today), today = today)
        val vm = HomeViewModel(progress, FakePurchases(), FakeAds(), { today })
        var dailies = 0
        compose.setContent {
            UnarrowTheme { HomeScreen(onPlay = {}, onDaily = { dailies++ }, onSettings = {}, viewModel = vm) }
        }
        compose.onNodeWithText("Play again (ad)").performClick()
        compose.waitForIdle()
        assertEquals(1, dailies)
    }

    @Test
    fun `a solved daily offers a replay for an ad when one is ready`() {
        var retries = 0
        compose.setContent {
            UnarrowTheme {
                HomeContent(
                    HomeUiState(loading = false, dailyDone = true, rewardedReady = true),
                    onPlay = {},
                    onDaily = {},
                    onSettings = {},
                    onRemoveAds = {},
                    onRetryDaily = { retries++ },
                )
            }
        }
        compose.onNodeWithText("Play again (ad)").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `the preview renders`() {
        compose.setContent { HomePreview() }
        compose.onNodeWithText("Play · Level 23").assertExists()
    }

    @Test
    fun `the records add up the progress, and the sound button flips the setting`() = runTest {
        val progress = FakeProgressRepository(
            GameProgress(level = 21, levelStars = mapOf(1 to 3, 2 to 2, 3 to 3), bestDailyStreak = 6),
            today = today,
        )
        val vm = HomeViewModel(progress, FakePurchases(), FakeAds(), { today })
        collecting(vm.uiState)
        val state = vm.uiState.value
        assertEquals(21, state.level)
        assertEquals(8, state.totalStars)
        assertEquals(6, state.bestStreak)
        assertTrue(state.soundOn)
        assertEquals(null, state.dailyResult)

        vm.toggleSound()
        assertFalse(vm.uiState.value.soundOn)
        assertFalse(progress.state.value.soundOn)
        vm.toggleSound()
        assertTrue(progress.state.value.soundOn)

        progress.completeDaily(DailyResult(stars = 3, seconds = 42))
        assertEquals(DailyResult(3, 42), vm.uiState.value.dailyResult)
    }

    @Test
    fun `records, how to play and the footer are shown and spoken`() {
        var sound = 0
        var site = 0
        compose.setContent {
            UnarrowTheme {
                HomeContent(
                    HomeUiState(loading = false, level = 21, totalStars = 58, bestStreak = 9, soundOn = false),
                    onPlay = {},
                    onDaily = {},
                    onSettings = {},
                    onRemoveAds = {},
                    onSound = { sound++ },
                    onWebsite = { site++ },
                )
            }
        }
        compose.onNodeWithContentDescription("Level: 21").assertExists()
        compose.onNodeWithContentDescription("Stars: 58").assertExists()
        compose.onNodeWithContentDescription("Best streak: 9").assertExists()
        compose.onNodeWithContentDescription("Unarrow").assertExists()
        compose.onNodeWithText("A blocked arrow slides to the obstacle and costs a life.").performScrollTo().assertExists()
        compose.onNodeWithText("A locked arrow waits until the key of its colour has left.").assertExists()
        compose.onNodeWithContentDescription("Sound off").performScrollTo().performClick()
        compose.onNodeWithText("kallistesoft.ca").performScrollTo().performClick()
        assertEquals(1 to 1, sound to site)
    }

    @Test
    fun `a solved daily keeps its title in full colour and shows today's result`() {
        compose.setContent {
            UnarrowTheme {
                HomeContent(
                    HomeUiState(loading = false, dailyNumber = 7, dailyDone = true, dailyResult = DailyResult(2, 65)),
                    onPlay = {},
                    onDaily = {},
                    onSettings = {},
                    onRemoveAds = {},
                )
            }
        }
        compose.onNodeWithText("★★☆ 1:05").assertExists()
        // Disabled Material content is drawn at 38 % alpha: the title's darkest pixel would be a light grey.
        compose.mainClock.advanceTimeBy(2_000) // The entrance is over.
        val bounds = compose.onNodeWithTag("daily_title", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val pixels = compose.onRoot().captureToImage().toPixelMap()
        var darkest = 1f
        for (x in bounds.left.toInt() until bounds.right.toInt()) {
            for (y in bounds.top.toInt() until bounds.bottom.toInt()) darkest = minOf(darkest, pixels[x, y].red)
        }
        assertTrue("darkest red $darkest", darkest < 0.25f)
    }

    @Test
    fun `the home screen sends the sound button to the settings`() {
        val progress = FakeProgressRepository(GameProgress(), today = today)
        val vm = HomeViewModel(progress, FakePurchases(), FakeAds(), { today })
        compose.setContent {
            UnarrowTheme { HomeScreen(onPlay = {}, onDaily = {}, onSettings = {}, viewModel = vm) }
        }
        compose.onNodeWithContentDescription("Sound on").performScrollTo().performClick()
        compose.waitForIdle()
        assertFalse(progress.state.value.soundOn)
        compose.onNodeWithText("kallistesoft.ca").performScrollTo().performClick() // Opens the browser (no-op here).
    }
}
