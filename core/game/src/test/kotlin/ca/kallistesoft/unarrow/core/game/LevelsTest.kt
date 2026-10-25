package ca.kallistesoft.unarrow.core.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelsTest {
    @Test
    fun `level 1 is a small sparse grid`() {
        val spec = Levels.spec(1)
        assertEquals(4, spec.rows)
        assertEquals(4, spec.cols)
        assertEquals(10, spec.arrowCount) // 16 × 0.6
        assertEquals(Levels.GENTLE_WIDTH, spec.width)
        assertEquals(Levels.WIDTH, Levels.spec(3).width)
    }

    @Test
    fun `the grid grows fast and stops at nine`() {
        assertEquals(listOf(4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9), listOf(3, 4, 7, 8, 12, 13, 18, 19, 25, 26, 500).map {
            Levels.spec(it).rows
        })
    }

    @Test
    fun `within a size the board fills up`() {
        assertTrue(Levels.spec(4).arrowCount < Levels.spec(7).arrowCount)
        assertEquals(Math.round(25 * 0.6).toInt(), Levels.spec(4).arrowCount)
        assertEquals(Math.round(25 * 0.88).toInt(), Levels.spec(7).arrowCount)
    }

    @Test
    fun `every level up to 300 generates its arrows`() {
        for (level in 1..300) {
            val spec = Levels.spec(level)
            assertEquals("level $level", spec.arrowCount, spec.board().arrows.size)
        }
    }

    @Test
    fun `levels never end - past level 25 the grid stays 9 x 9, fills 75 to 92 percent, width 2 or 3`() {
        assertEquals(25, Levels.LAST_GROWING_LEVEL)
        assertEquals(Levels.LAST_GROWING_LEVEL, Levels.LEVELS_PER_SIZE.sum())
        val cells = 9 * 9
        val specs = (26..2_000).filter { it % Levels.BREATHER_EVERY != 0 }.map(Levels::spec)
        assertTrue(specs.all { it.rows == 9 && it.cols == 9 })
        val fills = specs.map { it.arrowCount }
        assertTrue(fills.all { it in Math.round(cells * 0.75).toInt()..Math.round(cells * 0.92).toInt() })
        assertTrue("fills vary", fills.toSet().size > 10)
        assertEquals(setOf(2, 3), specs.map { it.width }.toSet())
    }

    @Test
    fun `every tenth level past 25 is a breather`() {
        assertEquals(Math.round(81 * Levels.BREATHER_FILL).toInt(), Levels.spec(40).arrowCount)
        assertEquals(Levels.BREATHER_WIDTH, Levels.spec(40).width)
        assertEquals(Math.round(81 * Levels.BREATHER_FILL).toInt(), Levels.spec(1_000_000).arrowCount)
    }

    @Test
    fun `far levels generate, solvable, and fast`() {
        val start = System.nanoTime()
        for (level in listOf(37, 99, 1_234, 10_000, 123_456, 9_999_999)) {
            val board = Levels.spec(level).board()
            assertEquals(Levels.spec(level).arrowCount, board.arrows.size)
            var b = board
            while (!b.isCleared) b = (b.tap(b.freeArrows().first().id) as TapResult.Exited).board
        }
        for (level in 26..225) Levels.spec(level).board()
        // About 5 ms a 9 × 9 board on a laptop; CI runners are several times slower.
        assertTrue("200 levels in under 20 s", System.nanoTime() - start < 20_000_000_000L)
    }

    @Test
    fun `levels are stable`() {
        assertEquals(Levels.spec(42).board(), Levels.spec(42).board())
        assertNotEquals(Levels.spec(42).board(), Levels.spec(43).board())
    }

    @Test
    fun `everyone gets the same daily puzzle on the same day and a new one the next day`() {
        val day = Levels.FIRST_DAILY_EPOCH_DAY + 10
        assertEquals(Levels.daily(day).board(), Levels.daily(day).board())
        assertNotEquals(Levels.daily(day).board(), Levels.daily(day + 1).board())
        assertEquals(7, Levels.daily(day).rows)
        assertEquals(42, Levels.daily(day).arrowCount) // 49 × 0.85
    }

    @Test
    fun `daily puzzles are numbered from the first day`() {
        assertEquals(1, Levels.dailyNumber(Levels.FIRST_DAILY_EPOCH_DAY))
        assertEquals(31, Levels.dailyNumber(Levels.FIRST_DAILY_EPOCH_DAY + 30))
    }

    @Test
    fun `days before the first daily are number 1`() {
        assertEquals(1, Levels.dailyNumber(Levels.FIRST_DAILY_EPOCH_DAY - 1))
        assertEquals(1, Levels.dailyNumber(Levels.FIRST_DAILY_EPOCH_DAY - 7)) // 2026-10-25 showed "Daily #-6"
        assertEquals(1, Levels.dailyNumber(0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `there is no level 0`() {
        Levels.spec(0)
    }
}
