package ca.kallistesoft.unarrow.core.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BoardTest {
    // . . →
    // . ↑ ←      the ↑ at (1,1) and the → at (0,2) are free; the ← at (1,2) is blocked by the ↑.
    private val right = Arrow(0, 0, 2, Direction.RIGHT)
    private val up = Arrow(1, 1, 1, Direction.UP)
    private val left = Arrow(2, 1, 2, Direction.LEFT)
    private val board = Board(2, 3, listOf(right, up, left))

    @Test
    fun `an arrow with another one on its path is blocked by the nearest`() {
        assertEquals(up, board.blockerOf(left))
        assertEquals(TapResult.Blocked(left, up), board.tap(left.id))
    }

    @Test
    fun `an arrow with a clear path leaves the board`() {
        val result = board.tap(up.id) as TapResult.Exited
        assertEquals(up, result.arrow)
        assertEquals(listOf(right, left), result.board.arrows)
        assertTrue(result.board.canExit(left))
    }

    @Test
    fun `arrows with nothing on their path are free`() {
        assertNull(board.blockerOf(up))
        assertNull(board.blockerOf(right))
        assertEquals(listOf(right, up), board.freeArrows())
    }

    @Test
    fun `tapping an arrow that is gone misses`() {
        assertEquals(TapResult.Missed, board.tap(42))
    }

    @Test
    fun `cells are looked up by position`() {
        assertEquals(up, board.arrowAt(1, 1))
        assertNull(board.arrowAt(0, 0))
        assertNull(board.arrowAt(-1, 0))
        assertNull(board.arrowAt(0, 3))
    }

    @Test
    fun `exit distance counts the cells to the edge`() {
        val b = Board(4, 5, emptyList())
        assertEquals(2, b.exitDistance(Arrow(0, 1, 2, Direction.UP)))
        assertEquals(3, b.exitDistance(Arrow(0, 1, 2, Direction.DOWN)))
        assertEquals(3, b.exitDistance(Arrow(0, 1, 2, Direction.LEFT)))
        assertEquals(3, b.exitDistance(Arrow(0, 1, 2, Direction.RIGHT)))
    }

    @Test
    fun `a board is cleared once every arrow is gone`() {
        assertFalse(board.isCleared)
        assertTrue(Board(1, 1, emptyList()).isCleared)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `two arrows can't share a cell`() {
        Board(2, 2, listOf(Arrow(0, 0, 0, Direction.UP), Arrow(1, 0, 0, Direction.LEFT)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `an arrow can't sit outside the grid`() {
        Board(2, 2, listOf(Arrow(0, 2, 0, Direction.UP)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a grid can't be empty`() {
        Board(0, 3, emptyList())
    }
}
