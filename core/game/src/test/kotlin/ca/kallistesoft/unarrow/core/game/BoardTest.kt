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
        val slid = board.tap(left.id) as TapResult.Slid
        assertEquals("already touching: it doesn't move", 0, slid.distance)
        assertEquals(up, slid.blocker)
        assertEquals(left, slid.moved)
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
    fun `rounds count the waves of free arrows, and -1 when the board is stuck`() {
        assertEquals(2, board.rounds()) // ↑ and → first, then ←
        assertEquals(0, Board(1, 1, emptyList()).rounds())
        val facing = Board(1, 2, listOf(Arrow(0, 0, 0, Direction.RIGHT), Arrow(1, 0, 1, Direction.LEFT)))
        assertEquals(-1, facing.rounds())
    }

    @Test
    fun `a blocked arrow slides up to its blocker`() {
        // → . . ↓   the → slides two cells, next to the ↓.
        val right = Arrow(0, 0, 0, Direction.RIGHT)
        val down = Arrow(1, 0, 3, Direction.DOWN)
        val slid = Board(2, 4, listOf(right, down)).tap(right.id) as TapResult.Slid
        assertEquals(2, slid.distance)
        assertEquals(down, slid.blocker)
        assertEquals(right.copy(col = 2), slid.moved)
        assertEquals(right.copy(col = 2), slid.board.arrowAt(0, 2))
        assertNull(slid.board.arrowAt(0, 0))
    }

    @Test
    fun `rotating arrows turn a quarter after each exit or slide, not after a locked tap`() {
        val spinner = Arrow(0, 1, 1, Direction.UP, rotating = true)
        val free = Arrow(1, 0, 0, Direction.UP)
        val stuck = Arrow(2, 2, 0, Direction.RIGHT)
        val locked = Arrow(3, 2, 2, Direction.DOWN, lock = 0)
        val key = Arrow(4, 0, 2, Direction.RIGHT, key = 0)
        val start = Board(3, 3, listOf(spinner, free, stuck, locked, key))
        assertEquals(TapResult.Locked(locked), start.tap(locked.id))

        val afterExit = (start.tap(free.id) as TapResult.Exited).board
        assertEquals(Direction.RIGHT, afterExit.arrowAt(1, 1)!!.direction)
        val afterSlide = (afterExit.tap(stuck.id) as TapResult.Slid).board
        assertEquals(Direction.DOWN, afterSlide.arrowAt(1, 1)!!.direction)
        assertEquals("plain arrows keep their direction", Direction.RIGHT, afterSlide.arrowAt(2, 1)!!.direction)
        assertEquals(Direction.LEFT, Direction.DOWN.clockwise)
        assertEquals(Direction.LEFT, Direction.UP.turned(-1))
        assertEquals(Direction.UP, Direction.UP.turned(8))
    }

    @Test
    fun `a locked arrow opens once its key has left`() {
        val locked = Arrow(0, 0, 0, Direction.UP, lock = 1)
        val key = Arrow(1, 1, 1, Direction.DOWN, key = 1)
        val other = Arrow(2, 1, 0, Direction.LEFT, lock = 2) // no key on the board: open
        val board = Board(2, 2, listOf(locked, key, other))
        assertTrue(board.isLocked(locked))
        assertFalse(board.canExit(locked))
        assertFalse(board.isLocked(other))
        assertEquals(listOf(key, other), board.freeArrows())
        assertTrue(board.hasMechanics)
        assertFalse(Board(1, 1, listOf(Arrow(0, 0, 0, Direction.UP))).hasMechanics)

        val opened = (board.tap(key.id) as TapResult.Exited).board
        assertFalse(opened.isLocked(locked))
        assertTrue(opened.tap(locked.id) is TapResult.Exited)
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
