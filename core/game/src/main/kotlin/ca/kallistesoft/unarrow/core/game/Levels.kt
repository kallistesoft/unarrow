package ca.kallistesoft.unarrow.core.game

/** What a puzzle is made of: its grid, its number of arrows and the seed that places them. */
data class LevelSpec(val rows: Int, val cols: Int, val arrowCount: Int, val seed: Long) {
    fun board(): Board = PuzzleGenerator.generate(rows, cols, arrowCount, SeededRandom(seed))
}

/**
 * The level curve and the daily puzzle. Level 1 is a small, sparse 4 × 4; every [LEVELS_PER_SIZE] levels the grid
 * grows by one, up to [MAX_SIZE], and within each size the board fills up from [MIN_FILL] to [MAX_FILL]. Past the
 * largest size, levels keep cycling through the denser fills.
 */
object Levels {
    const val MIN_SIZE = 4
    const val MAX_SIZE = 9
    const val LEVELS_PER_SIZE = 6
    const val MIN_FILL = 0.55
    const val MAX_FILL = 0.85
    const val DAILY_SIZE = 7
    const val DAILY_FILL = 0.8

    /** The day of the first daily puzzle (2026-11-01), puzzle #1. */
    const val FIRST_DAILY_EPOCH_DAY = 20_758L

    fun spec(level: Int): LevelSpec {
        require(level >= 1) { "Levels start at 1: $level" }
        val index = level - 1
        val size = (MIN_SIZE + index / LEVELS_PER_SIZE).coerceAtMost(MAX_SIZE)
        val step = index % LEVELS_PER_SIZE
        val fill = MIN_FILL + (MAX_FILL - MIN_FILL) * step / (LEVELS_PER_SIZE - 1)
        return LevelSpec(size, size, arrowCount(size, fill), seed = level * LEVEL_PRIME + LEVEL_SALT)
    }

    /** Everyone gets the same puzzle on the same day ([epochDay]: days since 1970-01-01, local date). */
    fun daily(epochDay: Long): LevelSpec =
        LevelSpec(DAILY_SIZE, DAILY_SIZE, arrowCount(DAILY_SIZE, DAILY_FILL), seed = epochDay * DAILY_PRIME + DAILY_SALT)

    /** The number shown and shared for the daily puzzle ("Unarrow Daily #12"). */
    fun dailyNumber(epochDay: Long): Long = epochDay - FIRST_DAILY_EPOCH_DAY + 1

    private fun arrowCount(size: Int, fill: Double) = Math.round(size * size * fill).toInt()

    private const val LEVEL_PRIME = 7_919L
    private const val LEVEL_SALT = 0x5EED_0001L
    private const val DAILY_PRIME = 104_729L
    private const val DAILY_SALT = 0xDA11_0001L
}
