package ca.kallistesoft.unarrow.feature.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.height
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.performClick
import ca.kallistesoft.unarrow.core.designsystem.Sizes
import ca.kallistesoft.unarrow.core.designsystem.UnarrowTheme
import ca.kallistesoft.unarrow.core.game.Arrow
import ca.kallistesoft.unarrow.core.game.Board
import ca.kallistesoft.unarrow.core.game.Direction
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.testing.FakeAds
import ca.kallistesoft.unarrow.core.testing.FakeEngagement
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.FakePurchases
import ca.kallistesoft.unarrow.core.testing.MainDispatcherRule
import ca.kallistesoft.unarrow.core.testing.ScreenshotDevice
import ca.kallistesoft.unarrow.core.testing.setContentWithSystemBars
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ScreenshotDevice.SDK], qualifiers = ScreenshotDevice.LIGHT_EN)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GameScreenTest {
    @get:Rule
    val compose = createComposeRule()

    // ↑ at (0,0) is free, → at (1,0) is free, ← at (1,1) is blocked by the →.
    private val up = Arrow(0, 0, 0, Direction.UP)
    private val right = Arrow(1, 1, 0, Direction.RIGHT)
    private val left = Arrow(2, 1, 1, Direction.LEFT)
    private val down = Arrow(3, 0, 1, Direction.DOWN)
    private val board = Board(2, 2, listOf(up, right, left, down))

    private fun session(
        mode: GameMode = GameMode.LEVELS,
        status: GameStatus = GameStatus.PLAYING,
        lives: Int = MAX_LIVES,
        blocked: BlockedTap? = null,
        exits: List<Arrow> = emptyList(),
    ) = GameSession(mode, 5, 12, board, 4, lives = lives, taps = 6, hintId = up.id, blocked = blocked, exits = exits,
        status = status)

    private fun show(state: GameUiState, actions: GameActions = GameActions()) =
        compose.setContent { UnarrowTheme { GameContent(state, actions) } }

    @Test
    fun `arrows are tapped by their label, and the level and lives show`() {
        val tapped = mutableListOf<Int>()
        show(GameUiState(session(lives = 2), hints = 3), GameActions(onTap = { tapped += it }))
        compose.onNodeWithText("Level 5").assertExists()
        compose.onNodeWithContentDescription("2 lives left").assertExists()
        compose.onNodeWithContentDescription("Arrow pointing up").performClick()
        compose.onNodeWithContentDescription("Arrow pointing left").performClick()
        assertEquals(listOf(up.id, left.id), tapped)
        compose.onNodeWithContentDescription("Puzzle board").assertExists()
    }

    @Test
    fun `the hint button spends a hint, then offers an ad, then the pack`() {
        var hints = 0
        var packs = 0
        val actions = GameActions(onHint = { hints++ }, onBuyHints = { packs++ })
        show(GameUiState(session().copy(hintId = null), hints = 2), actions)
        compose.onNodeWithText("Hint · 2").performClick()
        assertEquals(1, hints)
    }

    @Test
    fun `with no hints a rewarded ad gives one`() {
        var hints = 0
        show(GameUiState(session().copy(hintId = null), hints = 0, rewardedReady = true), GameActions(onHint = { hints++ }))
        compose.onNodeWithText("Hint (ad)").performClick()
        assertEquals(1, hints)
    }

    @Test
    fun `with no hints and no ad the pack is for sale, or nothing`() {
        var packs = 0
        show(GameUiState(session(), hints = 0, hintPackPrice = "$0.99"), GameActions(onBuyHints = { packs++ }))
        compose.onNodeWithText("Get 10 hints · $0.99").performClick()
        assertEquals(1, packs)
    }

    @Test
    fun `no hint to offer at all`() {
        show(GameUiState(session(), hints = 0))
        compose.onNodeWithText("Hint").assertIsNotEnabled()
    }

    @Test
    fun `a solved level offers the next one and a share with the Play link`() {
        var next = 0
        val shared = mutableListOf<String>()
        show(
            GameUiState(session(status = GameStatus.WON, lives = 2)),
            GameActions(onNext = { next++ }, onShare = { text, _ -> shared += text }),
        )
        compose.onNodeWithText("Solved!").assertExists()
        compose.onNodeWithText("6 taps").assertExists()
        compose.onNodeWithText("Next level").performClick()
        compose.onNodeWithText("Share").performClick()
        assertEquals(1, next)
        assertTrue(shared.single().startsWith("I solved Unarrow level 5 ❤️❤️🤍 6 taps"))
        assertTrue(shared.single().endsWith("https://play.google.com/store/apps/details?id=ca.kallistesoft.unarrow"))
    }

    @Test
    fun `a solved daily goes home and shares its number`() {
        var home = 0
        val shared = mutableListOf<String>()
        show(
            GameUiState(session(mode = GameMode.DAILY, status = GameStatus.WON)),
            GameActions(onHome = { home++ }, onShare = { text, _ -> shared += text }),
        )
        compose.onNodeWithText("Daily #12").assertExists()
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Share").performClick()
        assertEquals(1, home)
        assertTrue(shared.single().startsWith("Unarrow Daily #12 ✅ ❤️❤️❤️ 6 taps"))
    }

    @Test
    fun `a lost puzzle offers an extra life for an ad, and a retry`() {
        var life = 0
        var retry = 0
        show(
            GameUiState(session(status = GameStatus.LOST, lives = 0), rewardedReady = true),
            GameActions(onExtraLife = { life++ }, onRestart = { retry++ }),
        )
        compose.onNodeWithText("Out of lives").assertExists()
        compose.onNodeWithText("Watch an ad: +1 life").performClick()
        compose.onNodeWithText("Try again").performClick()
        assertEquals(1 to 1, life to retry)
    }

    @Test
    fun `blocked taps and exits animate, the tutorial shows on the first levels`() {
        val early = session(blocked = BlockedTap(left.id, right.id, 1), exits = listOf(Arrow(9, 0, 0, Direction.LEFT)))
        show(GameUiState(early.copy(level = 1)))
        compose.onRoot().captureToImage() // Draws the board: tiles, hint, blocked flash, exits.
        compose.mainClock.advanceTimeBy(1_000)
        compose.onRoot().captureToImage()
        compose.onNodeWithText("Tap an arrow to slide it out. It only leaves if nothing is in its way.").assertExists()
        compose.onAllNodesWithContentDescription("Arrow pointing", substring = true).fetchSemanticsNodes().let {
            assertEquals(4, it.size)
        }
    }

    @Test
    fun `back and restart call back, and loading shows a spinner`() {
        var back = 0
        show(GameUiState(), GameActions(onBack = { back++ }))
        compose.onNodeWithContentDescription("Back").performClick()
        assertEquals(1, back)
        compose.onNodeWithTag("board").assertDoesNotExist()
    }

    @Test
    fun `the banner sits under the play area only while allowed`() {
        var state by androidx.compose.runtime.mutableStateOf(GameUiState(session(), banner = true))
        compose.setContent {
            UnarrowTheme {
                GameContent(state, GameActions(), banner = { FakeAds().Banner(it.height(Sizes.icon)) })
            }
        }
        val banner = compose.onNodeWithTag("banner").assertExists().getUnclippedBoundsInRoot()
        val restart = compose.onNodeWithText("Restart").getUnclippedBoundsInRoot()
        assertTrue("under the buttons", banner.top >= restart.bottom)

        state = state.copy(banner = false)
        compose.onNodeWithTag("banner").assertDoesNotExist()
    }

    @Test
    fun `the top bar stays under the status bar, the banner right above the navigation bar`() {
        val (statusBar, navigationBar) = with(compose.density) { 100.toDp() to 60.toDp() }
        compose.setContentWithSystemBars(top = 100, bottom = 60) {
            UnarrowTheme {
                GameContent(
                    GameUiState(session(status = GameStatus.WON), banner = true),
                    GameActions(),
                    banner = { FakeAds().Banner(it.height(Sizes.icon)) },
                )
            }
        }
        val root = compose.onRoot().getUnclippedBoundsInRoot()
        assertTrue(compose.onNodeWithContentDescription("Back").getUnclippedBoundsInRoot().top >= statusBar)
        assertTrue(compose.onNodeWithText("Level 5").getUnclippedBoundsInRoot().top >= statusBar)
        val banner = compose.onNodeWithTag("banner").getUnclippedBoundsInRoot()
        assertEquals("the inset applied once", root.bottom - navigationBar, banner.bottom)
        assertTrue("win panel above the banner", compose.onNodeWithTag("result").getUnclippedBoundsInRoot().bottom <= banner.top)
    }

    @Test
    fun `restart starts the puzzle again`() {
        var restarts = 0
        show(GameUiState(session()), GameActions(onRestart = { restarts++ }))
        compose.onNodeWithText("Restart").performClick()
        assertEquals(1, restarts)
    }

    @Test
    fun `the shake dies out`() {
        assertEquals(0f, shakeOffset(0f), 1e-6f)
        assertEquals(0f, shakeOffset(1f), 1e-6f)
        assertTrue(shakeOffset(0.1f) > 0f)
    }

    @Test
    fun `the preview renders`() {
        compose.setContent { GamePreview() }
        compose.onNodeWithText("Level 14").assertExists()
    }

    @Test
    fun `sounds play for exits, blocked taps and wins, and stay quiet when off`() {
        val sounds = GameSounds(org.robolectric.RuntimeEnvironment.getApplication())
        Sound.entries.forEach { sounds.play(it) }
        sounds.release()

        var state by androidx.compose.runtime.mutableStateOf(GameUiState(session(), soundOn = true, hapticsOn = true))
        compose.setContent { UnarrowTheme { GameContent(state, GameActions()) } }
        state = state.copy(session = session(exits = listOf(up)).copy(board = Board(2, 2, listOf(right, left, down))))
        compose.waitForIdle()
        state = state.copy(session = state.session!!.copy(blocked = BlockedTap(left.id, right.id, 1)))
        compose.waitForIdle()
        state = state.copy(session = state.session!!.copy(status = GameStatus.WON), soundOn = false, hapticsOn = false)
        compose.waitForIdle()
        compose.onNodeWithText("Solved!").assertExists()
    }

    @get:Rule
    val main = MainDispatcherRule()

    @Test
    fun `the screen plays a daily with its ViewModel and goes home after the win`() {
        val ads = FakeAds()
        val vm = GameViewModel(
            GameMode.DAILY, FakeProgressRepository(), { Levels.FIRST_DAILY_EPOCH_DAY }, ads, ads, FakePurchases(),
            FakeEngagement(), FakeEngagement(), ads,
        )
        var home = 0
        compose.setContent { UnarrowTheme { GameScreen(GameMode.DAILY, onBack = { home++ }, viewModel = vm) } }
        compose.onNodeWithText("Daily #1").assertExists()
        compose.onNodeWithTag("banner").assertExists()
        while (vm.uiState.value.session!!.board.arrows.isNotEmpty()) {
            vm.tap(vm.uiState.value.session!!.board.freeArrows().first().id)
        }
        compose.onRoot().captureToImage()
        compose.onNodeWithText("Home").performClick()
        compose.waitForIdle()
        assertEquals(1, home)
        assertEquals(listOf(1), ads.interstitialLevels)
    }
}
