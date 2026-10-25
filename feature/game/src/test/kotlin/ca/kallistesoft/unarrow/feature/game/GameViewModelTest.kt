package ca.kallistesoft.unarrow.feature.game

import ca.kallistesoft.unarrow.core.billing.Product
import ca.kallistesoft.unarrow.core.data.DailyResult
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.game.Board
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.game.Stars
import ca.kallistesoft.unarrow.core.testing.FakeAds
import ca.kallistesoft.unarrow.core.testing.FakeEngagement
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.FakePurchases
import ca.kallistesoft.unarrow.core.testing.MainDispatcherRule
import ca.kallistesoft.unarrow.core.testing.collecting
import ca.kallistesoft.unarrow.core.game.Arrow
import ca.kallistesoft.unarrow.core.game.Direction
import ca.kallistesoft.unarrow.core.game.Solution
import ca.kallistesoft.unarrow.core.game.Solver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GameViewModelTest {
    @get:Rule
    val main = MainDispatcherRule()

    private val today = Levels.FIRST_DAILY_EPOCH_DAY + 9
    private val progress = FakeProgressRepository(GameProgress(level = 7), today = today)
    private val ads = FakeAds()
    private val purchases = FakePurchases()
    private val engagement = FakeEngagement()

    private fun TestScope.viewModel(mode: GameMode = GameMode.LEVELS, puzzles: Puzzles = Puzzles(Dispatchers.Unconfined)) =
        GameViewModel(mode, progress, { today }, puzzles, ads, ads, purchases, engagement, engagement, ads).also {
            collecting(it.uiState)
        }

    private val GameViewModel.session get() = uiState.value.session!!

    /** Plays a perfect line from the current position. */
    private fun GameViewModel.solve() {
        for (move in (Solver().solve(session.board) as Solution.Found).moves) tap(move)
    }

    /** An arrow that slides when tapped (blocked, not locked). */
    private fun Board.blockedArrow() = arrows.first { !canExit(it) && !isLocked(it) }

    // → . ↓   a perfect line exists (↑, ←, ↓, →), but sliding the → onto the ↑'s path leaves none.
    // . ↑ ←
    private val right = Arrow(0, 0, 0, Direction.RIGHT)
    private val trap = Board(
        2, 3,
        listOf(right, Arrow(1, 0, 2, Direction.DOWN), Arrow(2, 1, 2, Direction.LEFT), Arrow(3, 1, 1, Direction.UP)),
    )

    @Test
    fun `the player's level is set up when the screen opens`() = runTest {
        val vm = viewModel()
        assertEquals(7, vm.session.level)
        assertEquals(Levels.spec(7).board(), vm.session.board)
        assertEquals(MAX_LIVES, vm.session.lives)
        assertEquals(GameProgress.STARTING_HINTS, vm.uiState.value.hints)
        assertEquals("$0.99", vm.uiState.value.hintPackPrice)
    }

    @Test
    fun `a free arrow leaves, a blocked one costs a life`() = runTest {
        val vm = viewModel()
        val free = vm.session.board.freeArrows().first()
        vm.tap(free.id)
        assertNull(vm.session.board.arrows.find { it.id == free.id })
        assertEquals(listOf(free), vm.session.exits)

        val stuck = vm.session.board.blockedArrow()
        vm.tap(stuck.id)
        assertEquals(MAX_LIVES - 1, vm.session.lives)
        assertEquals(stuck.id, vm.session.blocked?.arrowId)
        assertEquals(2, vm.session.taps)
        vm.tap(999) // gone already
        assertEquals(2, vm.session.taps)
    }

    @Test
    fun `three blocked taps lose the puzzle, and an ad buys one more life`() = runTest {
        val vm = viewModel()
        repeat(MAX_LIVES) { vm.tap(vm.session.board.blockedArrow().id) }
        assertEquals(GameStatus.LOST, vm.session.status)
        vm.tap(vm.session.board.freeArrows().first().id)
        assertEquals("no taps once lost", MAX_LIVES, vm.session.taps)

        vm.extraLife()
        assertEquals(GameStatus.PLAYING, vm.session.status)
        assertEquals(1, vm.session.lives)
        assertEquals(1, ads.rewardedShown)
    }

    @Test
    fun `an ad skipped gives no life`() = runTest {
        ads.earn = false
        val vm = viewModel()
        repeat(MAX_LIVES) { vm.tap(vm.session.board.blockedArrow().id) }
        vm.extraLife()
        assertEquals(GameStatus.LOST, vm.session.status)
        vm.restart()
        assertEquals(GameStatus.PLAYING, vm.session.status)
        assertEquals(MAX_LIVES, vm.session.lives)
    }

    @Test
    fun `solving a level saves it, asks for a rating and moves on after the interstitial`() = runTest {
        val vm = viewModel()
        vm.solve()
        assertEquals(GameStatus.WON, vm.session.status)
        assertEquals(8, progress.state.value.level)
        assertEquals(1, engagement.reviewAsks)

        vm.next()
        assertEquals(listOf(7), ads.interstitialLevels)
        assertEquals(8, vm.session.level)
        assertEquals(GameStatus.PLAYING, vm.session.status)
        assertTrue(vm.session.exits.isEmpty())
    }

    @Test
    fun `next does nothing before the puzzle is solved`() = runTest {
        val vm = viewModel()
        vm.next()
        assertEquals(7, vm.session.level)
        assertTrue(ads.interstitialLevels.isEmpty())
    }

    @Test
    fun `a hint points at a free arrow and spends one`() = runTest {
        val vm = viewModel()
        vm.hint()
        val hinted = vm.session.hintId
        assertNotNull(hinted)
        assertTrue(vm.session.board.canExit(vm.session.board.arrows.first { it.id == hinted }))
        assertEquals(GameProgress.STARTING_HINTS - 1, progress.state.value.hints)
        vm.hint()
        assertEquals("one hint at a time", GameProgress.STARTING_HINTS - 1, progress.state.value.hints)
        vm.tap(hinted!!)
        assertNull(vm.session.hintId)
    }

    @Test
    fun `with no hints left a hint costs a rewarded ad, or nothing happens`() = runTest {
        progress.state.value = GameProgress(level = 3, hints = 0)
        val vm = viewModel()
        vm.hint()
        assertNotNull(vm.session.hintId)
        assertEquals(1, ads.rewardedShown)

        vm.tap(vm.session.hintId!!)
        ads.earn = false
        vm.hint()
        assertNull(vm.session.hintId)
    }

    @Test
    fun `the hint pack is bought from the game`() = runTest {
        val vm = viewModel()
        vm.buyHints()
        assertEquals(listOf(Product.HINTS_10), purchases.bought)
    }

    @Test
    fun `the daily puzzle is today's, counts the streak, and its ad comes on the way home`() = runTest {
        val vm = viewModel(GameMode.DAILY)
        assertEquals(Levels.daily(today).board(), vm.session.board)
        assertEquals(10L, vm.session.dailyNumber)
        vm.restart()
        assertEquals(Levels.daily(today).board(), vm.session.board)

        vm.solve()
        assertEquals(1, progress.state.value.dailyStreak)
        assertEquals(7, progress.state.value.level)
        vm.next() // levels only
        assertTrue(ads.interstitialLevels.isEmpty())
        vm.leaveAfterWin()
        assertEquals(listOf(7), ads.interstitialLevels)
    }

    @Test
    fun `leaving an unsolved puzzle shows no ad`() = runTest {
        val vm = viewModel()
        vm.leaveAfterWin()
        assertTrue(ads.interstitialLevels.isEmpty())
    }

    @Test
    fun `sharing goes to the share sheet`() = runTest {
        val vm = viewModel()
        vm.share("result", "Share")
        assertEquals(listOf("result"), engagement.shared)
    }

    @Test
    fun `the banner follows the ad rules, and goes once ads are removed`() = runTest {
        val vm = viewModel()
        assertTrue(vm.uiState.value.banner)
        ads.bannerAllowed.value = false
        assertEquals(false, vm.uiState.value.banner)
    }

    @Test
    fun `a clean, quick solve earns three stars and the level keeps its best`() = runTest {
        val vm = viewModel()
        vm.solve()
        assertEquals(3, vm.session.stars)
        assertEquals(3, progress.state.value.levelStars[7])
        vm.restart()
        vm.tap(vm.session.board.blockedArrow().id)
        vm.tap(vm.session.board.blockedArrow().id)
        vm.solve()
        assertEquals("two mistakes", 1, vm.session.stars)
        assertEquals(2, vm.session.mistakes)
        assertEquals("the best stays", 3, progress.state.value.levelStars[7])
        assertEquals(3, vm.uiState.value.bestStars)
        assertNull(vm.uiState.value.bestDaily)
    }

    @Test
    fun `a hint caps the puzzle at two stars`() = runTest {
        val vm = viewModel()
        vm.hint()
        vm.solve()
        assertTrue(vm.session.hintUsed)
        assertEquals(2, vm.session.stars)
    }

    @Test
    fun `the clock runs while the screen is on and the puzzle played, not during an ad`() = runTest(main.testDispatcher) {
        val vm = viewModel()
        val time = main.testDispatcher.scheduler
        vm.startClock()
        vm.startClock() // already running
        time.advanceTimeBy(3_500)
        assertEquals(3, vm.session.seconds)

        vm.stopClock()
        time.advanceTimeBy(10_000)
        assertEquals("paused in the background", 3, vm.session.seconds)

        vm.startClock()
        progress.state.value = progress.state.value.copy(hints = 0)
        ads.rewardedOnScreen = CompletableDeferred()
        vm.hint()
        time.advanceTimeBy(5_000)
        assertEquals("stopped under the ad", 3, vm.session.seconds)
        ads.rewardedOnScreen!!.complete(Unit)
        time.advanceTimeBy(2_000)
        assertEquals(5, vm.session.seconds)

        vm.solve()
        time.advanceTimeBy(5_000)
        assertEquals("stopped once solved", 5, vm.session.seconds)
        assertEquals("too slow for 3 stars? no: a hint", 2, vm.session.stars)
        vm.stopClock()
    }

    @Test
    fun `a slow solve earns two stars, and the daily keeps its best time`() = runTest(main.testDispatcher) {
        val vm = viewModel(GameMode.DAILY)
        vm.startClock()
        main.testDispatcher.scheduler.advanceTimeBy((Stars.targetSeconds(vm.session.startArrowCount) + 1) * 1_000L + 500)
        vm.solve()
        vm.stopClock()
        val result = DailyResult(2, Stars.targetSeconds(vm.session.startArrowCount) + 1)
        assertEquals(result, progress.state.value.bestDaily)
        assertEquals(result, vm.uiState.value.bestDaily)
        assertNull(vm.uiState.value.bestStars)

        vm.restart()
        assertEquals("a restart starts the clock and mistakes again", 0 to 0, vm.session.seconds to vm.session.mistakes)
    }

    @Test
    fun `a slide moves the arrow and costs a life, a locked tap costs nothing`() = runTest {
        val vm = viewModel(GameMode.DAILY)
        val board = vm.session.board
        val locked = board.arrows.first { board.isLocked(it) }
        vm.tap(locked.id)
        assertEquals(MAX_LIVES, vm.session.lives)
        assertEquals(board, vm.session.board)
        assertEquals(locked.id, vm.session.blocked?.arrowId)
        assertNull(vm.session.blocked?.blockerId)
        assertEquals(0, vm.session.mistakes)

        val stuck = board.blockedArrow()
        vm.tap(stuck.id)
        assertEquals(MAX_LIVES - 1, vm.session.lives)
        assertEquals(1, vm.session.mistakes)
        assertTrue(vm.session.board != board)
    }

    @Test
    fun `a hint after a trap says no perfect line is left, for free, and restart sets the board back`() = runTest {
        val vm = viewModel(puzzles = Puzzles(Dispatchers.Unconfined, levelBoard = { trap }))
        vm.tap(right.id) // slides onto the ↑'s path
        vm.hint()
        assertTrue(vm.session.stuck)
        assertNull(vm.session.hintId)
        assertEquals(GameProgress.STARTING_HINTS, progress.state.value.hints)
        vm.hint() // already said
        assertEquals(GameProgress.STARTING_HINTS, progress.state.value.hints)

        vm.restart()
        assertEquals(trap, vm.session.board)
        assertEquals(MAX_LIVES, vm.session.lives)
        assertTrue(!vm.session.stuck)
        vm.hint()
        assertEquals("the ↑ goes first", 3, vm.session.hintId)
    }

    @Test
    fun `tips show once, the first time a mechanic appears`() = runTest {
        val first = viewModel(GameMode.DAILY)
        assertEquals(Tip.LOCKS, first.session.tip)
        assertEquals(setOf("LOCKS"), progress.state.value.tipsSeen)
        val second = viewModel(GameMode.DAILY)
        assertEquals(Tip.ROTATING, second.session.tip)
        assertNull(viewModel(GameMode.DAILY).session.tip)
        assertNull("plain boards have no tip", viewModel().session.tip)
    }
}
