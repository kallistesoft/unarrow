package ca.kallistesoft.unarrow.core.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleGeneratorTest {
    @Test
    fun `every generated puzzle has the requested arrows and clears in any order of legal taps`() {
        for (seed in 0L until 500L) {
            val size = 4 + (seed % 6).toInt()
            val count = 1 + (seed * 31 % (size * size)).toInt()
            val board = PuzzleGenerator.generate(size, size, count, SeededRandom(seed))
            assertEquals("seed $seed", count, board.arrows.size)
            assertTrue("seed $seed got stuck", clearsWithRandomTaps(board, SeededRandom(seed + 1)))
        }
    }

    @Test
    fun `a full board is still solvable`() {
        val board = PuzzleGenerator.generate(9, 9, 81, SeededRandom(7))
        assertEquals(81, board.arrows.size)
        assertTrue(clearsWithRandomTaps(board, SeededRandom(8)))
    }

    @Test
    fun `the same seed always gives the same puzzle`() {
        val a = PuzzleGenerator.generate(6, 6, 25, SeededRandom(99))
        val b = PuzzleGenerator.generate(6, 6, 25, SeededRandom(99))
        val c = PuzzleGenerator.generate(6, 6, 25, SeededRandom(100))
        assertEquals(a, b)
        assertNotEquals(a, c)
    }

    @Test
    fun `a narrower width makes longer chains`() {
        val chain = PuzzleGenerator.generate(6, 6, 25, SeededRandom(5), width = 1)
        val wide = PuzzleGenerator.generate(6, 6, 25, SeededRandom(5), width = 6)
        assertTrue("${chain.rounds()} vs ${wide.rounds()}", chain.rounds() > wide.rounds())
        assertTrue(chain.freeArrows().size <= wide.freeArrows().size)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `no free arrow at all is refused`() {
        PuzzleGenerator.generate(4, 4, 5, SeededRandom(1), width = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `more arrows than cells is refused`() {
        PuzzleGenerator.generate(2, 2, 5, SeededRandom(1))
    }

    /** Taps a random free arrow until none is left; false if the board ever has arrows but none can leave. */
    private fun clearsWithRandomTaps(start: Board, random: SeededRandom): Boolean {
        var board = start
        while (!board.isCleared) {
            val free = board.freeArrows()
            if (free.isEmpty()) return false
            board = (board.tap(free[random.nextInt(free.size)].id) as TapResult.Exited).board
        }
        return true
    }
}
