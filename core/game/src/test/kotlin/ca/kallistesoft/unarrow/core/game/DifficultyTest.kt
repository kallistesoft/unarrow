package ca.kallistesoft.unarrow.core.game

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the difficulty curve, so it can't silently get easy again. Taking an arrow off never blocks another, so a
 * puzzle is only as hard as its free arrows are few (the share of arrows that can leave at the start) and its chains
 * long (the rounds needed when every free arrow is taken off at once). Before the harder generator, a 9 × 9 level had
 * 30–40% of its arrows free and cleared in 5–8 rounds.
 */
class DifficultyTest {
    private class Measure(val level: Int, val board: Board) {
        val free = board.freeArrows().size
        val freeShare = free.toDouble() / board.arrows.size
        val rounds = board.rounds()
    }

    private val levels = (1..300).map { Measure(it, Levels.spec(it).board()) }
    private val bigBoards = levels.filter { it.level > Levels.LAST_GROWING_LEVEL && it.level % Levels.BREATHER_EVERY != 0 }

    @Test
    fun `every level and daily is solvable, with at least one arrow free`() {
        val dailies = (0L until 60L).map { Levels.daily(Levels.FIRST_DAILY_EPOCH_DAY + it).board() }
        for (board in levels.map { it.board } + dailies) {
            assertTrue(board.freeArrows().isNotEmpty())
            assertTrue(board.rounds() > 0)
        }
    }

    @Test
    fun `from level 6 at most a quarter of the arrows are free at the start, one in seven on average`() {
        val measured = levels.filter { it.level >= 6 }
        val worst = measured.maxBy { it.freeShare }
        assertTrue("level ${worst.level}: ${worst.freeShare}", worst.freeShare <= 0.25)
        assertTrue(measured.map { it.freeShare }.average() <= 0.16)
    }

    @Test
    fun `big boards keep 10 to 15 percent of their arrows free`() {
        val average = bigBoards.map { it.freeShare }.average()
        assertTrue("$average", average in 0.10..0.16)
        assertTrue(bigBoards.all { it.freeShare <= 0.22 })
    }

    @Test
    fun `chains are long - many rounds to clear a board`() {
        val measured = levels.filter { it.level >= 6 }
        val shortest = measured.minBy { it.rounds }
        assertTrue("level ${shortest.level}: ${shortest.rounds} rounds", shortest.rounds >= 8)
        assertTrue(bigBoards.map { it.rounds }.average() >= 15)
        val dailies = (0L until 30L).map { Levels.daily(Levels.FIRST_DAILY_EPOCH_DAY + it).board() }
        assertTrue(dailies.all { it.rounds() >= 10 && it.freeArrows().size.toDouble() / it.arrows.size <= 0.2 })
    }

    @Test
    fun `the first levels are gentler than the rest`() {
        val gentle = levels.take(Levels.GENTLE_LEVELS).map { it.rounds }.average()
        assertTrue(gentle < levels.filter { it.level >= 6 }.map { it.rounds }.average())
    }
}
