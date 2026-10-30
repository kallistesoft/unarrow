package ca.kallistesoft.unarrow.feature.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ca.kallistesoft.unarrow.core.data.DailyResult
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.data.LevelResult
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.testing.FakeAds
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
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
class HistoryTest {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val main = MainDispatcherRule()

    private val first = Levels.FIRST_DAILY_EPOCH_DAY
    private val progress = FakeProgressRepository(
        GameProgress(
            level = 9,
            levelResults = mapOf(2 to LevelResult(3, 41), 8 to LevelResult(1, 300), 5 to LevelResult(2)),
            dailyResults = mapOf(first to DailyResult(2, 95), first + 2 to DailyResult(3, 60)),
        ),
    )

    @Test
    fun `levels come highest first and dailies newest first, with their best results`() = runTest {
        val vm = HistoryViewModel(progress, FakeAds())
        collecting(vm.uiState)
        val state = vm.uiState.value
        assertEquals(listOf(8, 5, 2), state.levels.map { it.level })
        assertEquals(LevelEntry(5, 2, null), state.levels[1])
        assertEquals(listOf(DailyEntry(first + 2, 3, 3, 60), DailyEntry(first, 1, 2, 95)), state.dailies)
        assertTrue(state.rewardedReady)
        vm.selectTab(HistoryTab.DAILY)
        assertEquals(HistoryTab.DAILY, vm.uiState.value.tab)
    }

    @Test
    fun `a replay opens only once the rewarded ad was watched to the end`() = runTest {
        val ads = FakeAds()
        val vm = HistoryViewModel(progress, ads)
        var opened = 0
        vm.replay { opened++ }
        ads.earn = false
        vm.replay { opened++ }
        assertEquals(1, opened)
        assertEquals(2, ads.rewardedShown)
    }

    @Test
    fun `the screen lists both tabs and replays a level or a daily after the ad`() {
        val vm = HistoryViewModel(progress, FakeAds())
        val replays = mutableListOf<String>()
        var back = 0
        compose.setContent {
            UnarrowTheme {
                HistoryScreen(
                    onBack = { back++ },
                    onReplayLevel = { replays += "level $it" },
                    onReplayDaily = { replays += "day $it" },
                    viewModel = vm,
                )
            }
        }
        compose.onNodeWithText("Level 8").assertExists()
        compose.onNodeWithContentDescription("1 star").assertExists()
        compose.onNodeWithContentDescription("Time 5:00").assertExists()
        compose.onNodeWithContentDescription("time unknown").assertExists()
        compose.onAllNodesWithText("Replay (ad)").onFirst().assertIsEnabled().performClick()
        compose.onNodeWithText("Daily").performClick()
        compose.onNodeWithText("Daily #3").assertExists()
        compose.onAllNodesWithText("Replay (ad)").onFirst().performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitForIdle()
        assertEquals(listOf("level 8", "day ${first + 2}"), replays)
        assertEquals(1, back)
    }

    @Test
    fun `without an ad replays are off and say why, and empty tabs say so`() {
        var state by mutableStateOf(
            HistoryUiState(loading = false, levels = listOf(LevelEntry(4, 3, 50)), rewardedReady = false),
        )
        compose.setContent { UnarrowTheme { HistoryContent(state) } }
        compose.onNodeWithText("No ad available right now. Try again in a moment.").assertExists()
        compose.onNodeWithText("Replay (ad)").assertIsNotEnabled()
        state = HistoryUiState(loading = false, tab = HistoryTab.DAILY)
        compose.onNodeWithText("No daily puzzle solved yet.").assertExists()
        assertFalse(compose.onAllNodesWithText("Replay (ad)").fetchSemanticsNodes().isNotEmpty())
    }

    @Test
    fun `an empty levels tab says so, and the preview renders`() {
        compose.setContent { UnarrowTheme { HistoryContent(HistoryUiState(loading = false)) } }
        compose.onNodeWithTag("empty").assertExists()
        compose.onNodeWithText("No level solved yet.").assertExists()
    }
}
