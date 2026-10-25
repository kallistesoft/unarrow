package ca.kallistesoft.unarrow.feature.game

import ca.kallistesoft.unarrow.core.billing.Product
import ca.kallistesoft.unarrow.core.data.GameProgress
import ca.kallistesoft.unarrow.core.game.Board
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.testing.FakeAds
import ca.kallistesoft.unarrow.core.testing.FakeEngagement
import ca.kallistesoft.unarrow.core.testing.FakeProgressRepository
import ca.kallistesoft.unarrow.core.testing.FakePurchases
import ca.kallistesoft.unarrow.core.testing.MainDispatcherRule
import ca.kallistesoft.unarrow.core.testing.collecting
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

    private fun TestScope.viewModel(mode: GameMode = GameMode.LEVELS) =
        GameViewModel(mode, progress, { today }, ads, ads, purchases, engagement, engagement, ads).also {
            collecting(it.uiState)
        }

    private val GameViewModel.session get() = uiState.value.session!!

    /** Taps free arrows until the board is clear. */
    private fun GameViewModel.solve() {
        while (!session.board.isCleared) tap(session.board.freeArrows().first().id)
    }

    private fun Board.blockedArrow() = arrows.first { !canExit(it) }

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
}
