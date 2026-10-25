package ca.kallistesoft.unarrow.core.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelsTest {
    @Test
    fun `level 1 is a small sparse grid of plain arrows`() {
        val spec = Levels.spec(1)
        assertEquals(4, spec.rows)
        assertEquals(10, spec.arrowCount) // 16 × 0.6
        assertEquals(Levels.GENTLE_WIDTH, spec.width)
        assertEquals(PuzzleGenerator.Mechanics(), spec.mechanics)
        assertEquals(Levels.WIDTH, Levels.spec(3).width)
    }

    @Test
    fun `mechanics come one at a time - traps from 4, locks from 10, rotating arrows from 20`() {
        fun mechanics(level: Int) = Levels.spec(level).mechanics
        assertEquals(0, mechanics(3).minTraps)
        assertTrue((4..25).all { mechanics(it).minTraps > 0 })
        assertTrue((1 until Levels.FIRST_LOCK_LEVEL).all { mechanics(it).lockColours == 0 })
        assertEquals(1, mechanics(Levels.FIRST_LOCK_LEVEL).lockColours)
        assertTrue((1 until Levels.FIRST_ROTATING_LEVEL).all { mechanics(it).rotating == 0 })
        assertEquals(2, mechanics(Levels.FIRST_ROTATING_LEVEL).rotating)
        assertEquals(listOf(4, 5, 6, 6, 7, 6, 7), listOf(3, 4, 7, 10, 15, 20, 25).map { Levels.spec(it).rows })
    }

    @Test
    fun `within a stage the board fills up`() {
        assertEquals(Math.round(25 * 0.65).toInt(), Levels.spec(4).arrowCount)
        assertEquals(Math.round(25 * 0.85).toInt(), Levels.spec(6).arrowCount)
        assertTrue(Levels.spec(15).arrowCount < Levels.spec(19).arrowCount)
    }

    @Test
    fun `endless levels mix every kind, rotating ones no bigger than 7 x 7`() {
        val specs = (Levels.LAST_STAGED_LEVEL + 1..1_000).filter { it % Levels.BREATHER_EVERY != 0 }.map(Levels::spec)
        assertTrue(specs.any { it.mechanics.rotating > 0 && it.mechanics.lockColours > 0 })
        assertTrue(specs.any { it.mechanics.rotating == 0 && it.mechanics.lockColours == 3 })
        assertTrue(specs.any { it.mechanics == PuzzleGenerator.Mechanics(minTraps = 2) && it.rows == 9 })
        assertTrue(specs.filter { it.mechanics.rotating > 0 }.all { it.rows <= 7 })
        assertEquals(setOf(2, 3), specs.map { it.width }.toSet())
        assertTrue("fills vary", specs.map { it.arrowCount }.toSet().size > 10)
    }

    @Test
    fun `every tenth level past 25 is a plain breather`() {
        for (level in listOf(30, 40, 1_000_000)) {
            val spec = Levels.spec(level)
            assertEquals(Math.round(49 * Levels.BREATHER_FILL).toInt(), spec.arrowCount)
            assertEquals(Levels.BREATHER_WIDTH, spec.width)
            assertEquals(PuzzleGenerator.Mechanics(), spec.mechanics)
        }
    }

    @Test
    fun `far levels generate fast`() {
        val start = System.nanoTime()
        for (level in listOf(99, 1_234, 123_456, 9_999_999)) {
            assertEquals(Levels.spec(level).arrowCount, Levels.spec(level).board().arrows.size)
        }
        for (level in 26..75) Levels.spec(level).board()
        // Up to about 0.25 s for a board on a laptop; CI runners are several times slower.
        assertTrue("54 levels in under 40 s", System.nanoTime() - start < 40_000_000_000L)
    }

    @Test
    fun `levels are stable`() {
        assertEquals(Levels.spec(42).board(), Levels.spec(42).board())
        assertNotEquals(Levels.spec(42).board(), Levels.spec(43).board())
    }

    @Test
    fun `everyone gets the same daily puzzle on the same day, with every mechanic`() {
        val day = Levels.FIRST_DAILY_EPOCH_DAY + 10
        val board = Levels.daily(day).board()
        assertEquals(board, Levels.daily(day).board())
        assertNotEquals(board, Levels.daily(day + 1).board())
        assertEquals(7, Levels.daily(day).rows)
        assertEquals(42, board.arrows.size) // 49 × 0.85
        assertEquals(2, board.arrows.count { it.rotating })
        assertTrue(board.arrows.any { it.lock != null } && board.arrows.any { it.key != null })
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
