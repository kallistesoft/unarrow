package ca.kallistesoft.unarrow.feature.game

import ca.kallistesoft.unarrow.core.game.Arrow
import ca.kallistesoft.unarrow.core.game.Board
import ca.kallistesoft.unarrow.core.game.Direction
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.game.Solver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class PuzzlesTest {
    @Test
    fun `boards are made off the main thread, the same as the curve`() = runTest {
        val puzzles = Puzzles()
        assertEquals(Levels.spec(3).board(), puzzles.level(3))
        assertEquals(Levels.daily(Levels.FIRST_DAILY_EPOCH_DAY).board(), puzzles.daily(Levels.FIRST_DAILY_EPOCH_DAY))
    }

    @Test
    fun `when the search gives up, the hint points at any free arrow, or says stuck`() = runTest {
        val puzzles = Puzzles(Dispatchers.Unconfined, solver = Solver(budget = 0))
        val free = Arrow(0, 0, 0, Direction.UP, rotating = true)
        assertEquals(Hint.Move(free.id), puzzles.hint(Board(2, 2, listOf(free, Arrow(1, 1, 1, Direction.UP)))))
        val facing = Board(1, 2, listOf(Arrow(0, 0, 0, Direction.RIGHT, rotating = true), Arrow(1, 0, 1, Direction.LEFT)))
        assertEquals(Hint.Stuck, puzzles.hint(facing))
    }
}
