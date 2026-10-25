package ca.kallistesoft.unarrow.core.game

/**
 * What a puzzle is made of: its grid, its number of arrows, the seed that places them and its [width], the number of
 * arrows the generator keeps free at a time (fewer: longer chains, harder).
 */
data class LevelSpec(val rows: Int, val cols: Int, val arrowCount: Int, val seed: Long, val width: Int = Levels.WIDTH) {
    fun board(): Board = PuzzleGenerator.generate(rows, cols, arrowCount, SeededRandom(seed), width)
}

/**
 * The level curve and the daily puzzle. Levels never end: each one is generated from its number.
 *
 * The grid grows fast: 4 × 4 for levels 1–3, then 5 × 5, 6 × 6, 7 × 7 and 8 × 8 for [LEVELS_PER_SIZE] levels each, up
 * to [LAST_GROWING_LEVEL]; within each size the board fills up from [MIN_FILL] to [MAX_FILL]. The first
 * [GENTLE_LEVELS] keep more arrows free ([GENTLE_WIDTH]) while the player learns the rule. After that the grid stays
 * at its largest (the most a phone screen taps comfortably) and each level's fill is drawn from its seed between
 * [ENDLESS_MIN_FILL] and [ENDLESS_MAX_FILL], and its width between [WIDTH] and [WIDTH] + 1, so no two levels in a row
 * feel alike; every [BREATHER_EVERY]th level is a lighter one ([BREATHER_FILL], [BREATHER_WIDTH]), so the difficulty
 * comes in waves.
 */
object Levels {
    const val MIN_SIZE = 4
    const val MAX_SIZE = 9
    val LEVELS_PER_SIZE = listOf(3, 4, 5, 6, 7)
    const val LAST_GROWING_LEVEL = 25
    const val MIN_FILL = 0.60
    const val MAX_FILL = 0.88
    const val WIDTH = 2
    const val GENTLE_LEVELS = 2
    const val GENTLE_WIDTH = 3
    const val ENDLESS_MIN_FILL = 0.75
    const val ENDLESS_MAX_FILL = 0.92
    const val BREATHER_EVERY = 10
    const val BREATHER_FILL = 0.65
    const val BREATHER_WIDTH = 4
    const val DAILY_SIZE = 7
    const val DAILY_FILL = 0.85

    /** The day of the first daily puzzle (2026-11-01), puzzle #1. */
    const val FIRST_DAILY_EPOCH_DAY = 20_758L

    fun spec(level: Int): LevelSpec {
        require(level >= 1) { "Levels start at 1: $level" }
        val seed = level * LEVEL_PRIME + LEVEL_SALT
        if (level > LAST_GROWING_LEVEL) return endless(level, seed)
        var first = 1
        var index = 0
        while (level >= first + LEVELS_PER_SIZE[index]) first += LEVELS_PER_SIZE[index++]
        val size = MIN_SIZE + index
        val fill = MIN_FILL + (MAX_FILL - MIN_FILL) * (level - first) / (LEVELS_PER_SIZE[index] - 1)
        val width = if (level <= GENTLE_LEVELS) GENTLE_WIDTH else WIDTH
        return LevelSpec(size, size, arrowCount(size, fill), seed, width)
    }

    /** Everyone gets the same puzzle on the same day ([epochDay]: days since 1970-01-01, local date). */
    fun daily(epochDay: Long): LevelSpec =
        LevelSpec(DAILY_SIZE, DAILY_SIZE, arrowCount(DAILY_SIZE, DAILY_FILL), seed = epochDay * DAILY_PRIME + DAILY_SALT)

    /**
     * The number shown and shared for the daily puzzle ("Unarrow Daily #12"). Days before the first daily (a phone
     * clock set back, testers before the launch) are #1, never 0 or negative.
     */
    fun dailyNumber(epochDay: Long): Long = maxOf(1, epochDay - FIRST_DAILY_EPOCH_DAY + 1)

    /** Past the growing levels: a breather every [BREATHER_EVERY] levels, otherwise a fill and width drawn from the seed. */
    private fun endless(level: Int, seed: Long): LevelSpec {
        if (level % BREATHER_EVERY == 0) {
            return LevelSpec(MAX_SIZE, MAX_SIZE, arrowCount(MAX_SIZE, BREATHER_FILL), seed, BREATHER_WIDTH)
        }
        val random = SeededRandom(seed xor FILL_SALT)
        val fill = ENDLESS_MIN_FILL + (ENDLESS_MAX_FILL - ENDLESS_MIN_FILL) * random.nextInt(ENDLESS_FILL_STEPS + 1) /
            ENDLESS_FILL_STEPS
        return LevelSpec(MAX_SIZE, MAX_SIZE, arrowCount(MAX_SIZE, fill), seed, WIDTH + random.nextInt(2))
    }

    private fun arrowCount(size: Int, fill: Double) = Math.round(size * size * fill).toInt()

    private const val ENDLESS_FILL_STEPS = 20

    // Salts: bump them all when the generator or the curve changes on purpose (version 2: harder puzzles).
    private const val FILL_SALT = 0xF112L
    private const val LEVEL_PRIME = 7_919L
    private const val LEVEL_SALT = 0x5EED_0002L
    private const val DAILY_PRIME = 104_729L
    private const val DAILY_SALT = 0xDA11_0002L
}
