package ca.kallistesoft.unarrow.core.game

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the difficulty curve, so it can't silently get easy again: every board has a perfect line (exits only, no life
 * lost), few arrows are free at the start, careless taps trap the player from level 4, and the mechanics appear where
 * the curve says.
 */
class DifficultyTest {
    private class Measure(val level: Int, val board: Board, val difficulty: Difficulty?)

    private companion object {
        // Built once for every test: a few seconds of generation and measuring.
        val levels by lazy { (1..60).map { Levels.spec(it).board().let { b -> Measure(it, b, Difficulty.of(b)) } } }
        val dailies by lazy { (0L until 10L).map { Levels.daily(Levels.FIRST_DAILY_EPOCH_DAY + it).board() } }
    }

    @Test
    fun `every level and daily has a perfect line, with at least one arrow free`() {
        for (measure in levels) assertTrue("level ${measure.level}", measure.difficulty != null)
        for (board in levels.map { it.board } + dailies) {
            assertTrue(board.freeArrows().isNotEmpty())
            assertTrue(Solver().solve(board) is Solution.Found)
        }
    }

    @Test
    fun `from level 4, careless taps trap the player`() {
        val trapped = levels.filter { it.level >= 4 }.count { it.difficulty!!.traps > 0 }
        assertTrue("$trapped of ${levels.size - 3}", trapped >= (levels.size - 3) * 8 / 10)
        assertTrue(dailies.all { (Difficulty.of(it)?.traps ?: 0) > 0 })
    }

    @Test
    fun `few arrows are free at the start`() {
        for (measure in levels.filter { it.level >= 4 }) {
            val share = measure.board.freeArrows().size.toDouble() / measure.board.arrows.size
            assertTrue("level ${measure.level}: $share", share <= 0.25)
        }
    }

    @Test
    fun `locks appear from level 10 and rotating arrows from level 20`() {
        fun Measure.locks() = board.arrows.any { it.lock != null }
        fun Measure.rotating() = board.arrows.any { it.rotating }
        assertTrue(levels.filter { it.level < Levels.FIRST_LOCK_LEVEL }.none { it.locks() || it.rotating() })
        assertTrue(levels.filter { it.level in Levels.FIRST_LOCK_LEVEL until Levels.FIRST_ROTATING_LEVEL }.all { it.locks() })
        assertTrue(levels.filter { it.level < Levels.FIRST_ROTATING_LEVEL }.none { it.rotating() })
        assertTrue(levels.filter { it.level in Levels.FIRST_ROTATING_LEVEL..Levels.LAST_STAGED_LEVEL }.all { it.rotating() })
        // Each lock colour has exactly one key, and the locks of a colour sit with it.
        for (board in levels.map { it.board } + dailies) {
            val keys = board.arrows.mapNotNull { it.key }
            assertTrue(keys.size == keys.toSet().size)
            assertTrue(board.arrows.mapNotNull { it.lock }.all { it in keys })
            assertTrue(keys.all { it < Arrow.LOCK_COLOURS })
        }
    }
}
