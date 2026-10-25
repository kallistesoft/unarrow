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
        assertEquals(9, spec.arrowCount) // 16 × 0.55
    }

    @Test
    fun `the grid grows every six levels and stops at nine`() {
        assertEquals(4, Levels.spec(6).rows)
        assertEquals(5, Levels.spec(7).rows)
        assertEquals(9, Levels.spec(31).rows)
        assertEquals(9, Levels.spec(500).rows)
    }

    @Test
    fun `within a size the board fills up`() {
        assertTrue(Levels.spec(7).arrowCount < Levels.spec(12).arrowCount)
        assertEquals(Math.round(25 * 0.85).toInt(), Levels.spec(12).arrowCount)
    }

    @Test
    fun `every level up to 300 generates its arrows`() {
        for (level in 1..300) {
            val spec = Levels.spec(level)
            assertEquals("level $level", spec.arrowCount, spec.board().arrows.size)
        }
    }

    @Test
    fun `levels never end - past level 36 the grid stays 9 x 9 and fills between 70 and 90 percent`() {
        assertEquals(36, Levels.LAST_GROWING_LEVEL)
        assertEquals(9, Levels.spec(36).rows)
        val cells = 9 * 9
        val fills = (37..2_000).filter { it % Levels.BREATHER_EVERY != 0 }.map { level ->
            val spec = Levels.spec(level)
            assertEquals(9, spec.rows)
            spec.arrowCount
        }
        assertTrue(fills.all { it in Math.round(cells * 0.70).toInt()..Math.round(cells * 0.90).toInt() })
        assertTrue("fills vary", fills.toSet().size > 10)
    }

    @Test
    fun `every tenth level past 36 is a breather`() {
        assertEquals(Math.round(81 * Levels.BREATHER_FILL).toInt(), Levels.spec(40).arrowCount)
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
        for (level in 37..10_000) Levels.spec(level).board()
        assertTrue("10,000 levels in under 20 s", System.nanoTime() - start < 20_000_000_000L)
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
        assertEquals(39, Levels.daily(day).arrowCount) // 49 × 0.8
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
