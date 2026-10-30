package ca.kallistesoft.unarrow.core.game

import ca.kallistesoft.unarrow.core.game.PuzzleGenerator.Mechanics

/**
 * What a puzzle is made of: its grid, its number of arrows, the seed that places them, its [width] (the number of
 * arrows the generator keeps free at a time: fewer, longer chains) and its [mechanics] (rotating arrows, locks, traps).
 */
data class LevelSpec(
    val rows: Int,
    val cols: Int,
    val arrowCount: Int,
    val seed: Long,
    val width: Int = Levels.WIDTH,
    val mechanics: Mechanics = Mechanics(),
) {
    fun board(): Board = PuzzleGenerator.generate(rows, cols, arrowCount, SeededRandom(seed), width, mechanics)
}

/**
 * The level curve and the daily puzzle. Levels never end: each one is generated from its number.
 *
 * Mechanics come one at a time ([STAGES]): plain arrows on 4 × 4 for levels 1–3 (the first two keep more arrows free
 * while the player learns), then boards picked for their slide traps from level 4, locks from level 10, rotating
 * arrows from level 20. Within a stage the board fills up. Past [LAST_STAGED_LEVEL] each level draws its kind from its
 * seed ([Kind]): a dense plain 9 × 9, locks on 8 × 8, or rotating arrows (with or without locks) on 7 × 7, the most the
 * solver checks quickly on a phone; every [BREATHER_EVERY]th level is a lighter one, so the difficulty comes in waves.
 * The daily puzzle mixes all three on a 7 × 7.
 */
object Levels {
    const val WIDTH = 2
    const val GENTLE_LEVELS = 2
    const val GENTLE_WIDTH = 3
    const val BREATHER_EVERY = 10
    const val BREATHER_SIZE = 7
    const val BREATHER_FILL = 0.65
    const val BREATHER_WIDTH = 4
    const val DAILY_SIZE = 7
    const val DAILY_FILL = 0.85
    val DAILY_MECHANICS = Mechanics(rotating = 2, lockColours = 2, minTraps = TRAPS)

    /** The first level that gets locks, and the first with rotating arrows. */
    const val FIRST_LOCK_LEVEL = 10
    const val FIRST_ROTATING_LEVEL = 20

    /** The day of the first daily puzzle (2026-11-01), puzzle #1. */
    const val FIRST_DAILY_EPOCH_DAY = 20_758L

    /** A run of levels: from [first], a [size] × [size] grid filling from [fromFill] to [toFill], with [mechanics]. */
    data class Stage(val first: Int, val size: Int, val fromFill: Double, val toFill: Double, val mechanics: Mechanics)

    val STAGES = listOf(
        Stage(1, 4, 0.60, 0.75, Mechanics()),
        Stage(4, 5, 0.65, 0.85, Mechanics(minTraps = TRAPS)),
        Stage(7, 6, 0.70, 0.85, Mechanics(minTraps = TRAPS)),
        Stage(FIRST_LOCK_LEVEL, 6, 0.70, 0.88, Mechanics(lockColours = 1, minTraps = TRAPS)),
        Stage(15, 7, 0.75, 0.88, Mechanics(lockColours = 2, minTraps = TRAPS)),
        Stage(FIRST_ROTATING_LEVEL, 6, 0.75, 0.85, Mechanics(rotating = 2, minTraps = TRAPS)),
        Stage(23, 7, 0.78, 0.88, Mechanics(rotating = 3, lockColours = 1, minTraps = TRAPS)),
    )
    const val LAST_STAGED_LEVEL = 25

    /** What an endless level is made of, drawn from its seed. */
    enum class Kind(val size: Int, val fromFill: Double, val toFill: Double) {
        PLAIN(9, 0.75, 0.92),
        LOCKS(8, 0.75, 0.90),
        ROTATING(7, 0.75, 0.88),
        MIXED(7, 0.75, 0.88),
    }

    fun spec(level: Int): LevelSpec {
        require(level >= 1) { "Levels start at 1: $level" }
        val seed = level * LEVEL_PRIME + LEVEL_SALT
        if (level > LAST_STAGED_LEVEL) return endless(level, seed)
        val index = STAGES.indexOfLast { it.first <= level }
        val stage = STAGES[index]
        val last = STAGES.getOrNull(index + 1)?.first?.minus(1) ?: LAST_STAGED_LEVEL
        val fill = stage.fromFill + (stage.toFill - stage.fromFill) * (level - stage.first) / maxOf(1, last - stage.first)
        val width = if (level <= GENTLE_LEVELS) GENTLE_WIDTH else WIDTH
        return LevelSpec(stage.size, stage.size, arrowCount(stage.size, fill), seed, width, stage.mechanics)
    }

    /** Everyone gets the same puzzle on the same day ([epochDay]: days since 1970-01-01, local date). */
    fun daily(epochDay: Long): LevelSpec = LevelSpec(
        DAILY_SIZE,
        DAILY_SIZE,
        arrowCount(DAILY_SIZE, DAILY_FILL),
        seed = epochDay * DAILY_PRIME + DAILY_SALT,
        mechanics = DAILY_MECHANICS,
    )

    /**
     * The number shown and shared for the daily puzzle ("Unarrow Daily #12"). Days before the first daily (a phone
     * clock set back, testers before the launch) are #1, never 0 or negative.
     */
    fun dailyNumber(epochDay: Long): Long = maxOf(1, epochDay - FIRST_DAILY_EPOCH_DAY + 1)

    /** Past the stages: a breather every [BREATHER_EVERY] levels, otherwise a kind, fill and width drawn from the seed. */
    private fun endless(level: Int, seed: Long): LevelSpec {
        if (level % BREATHER_EVERY == 0) {
            val count = arrowCount(BREATHER_SIZE, BREATHER_FILL)
            return LevelSpec(BREATHER_SIZE, BREATHER_SIZE, count, seed, BREATHER_WIDTH)
        }
        val random = SeededRandom(seed xor FILL_SALT)
        val kind = Kind.entries[random.nextInt(Kind.entries.size)]
        val fill = kind.fromFill + (kind.toFill - kind.fromFill) * random.nextInt(FILL_STEPS + 1) / FILL_STEPS
        val width = WIDTH + random.nextInt(2)
        val mechanics = when (kind) {
            Kind.PLAIN -> Mechanics(minTraps = TRAPS)
            Kind.LOCKS -> Mechanics(lockColours = 2 + random.nextInt(2), minTraps = TRAPS)
            Kind.ROTATING -> Mechanics(rotating = 3, minTraps = TRAPS)
            Kind.MIXED -> Mechanics(rotating = 2, lockColours = 2, minTraps = TRAPS)
        }
        return LevelSpec(kind.size, kind.size, arrowCount(kind.size, fill), seed, width, mechanics)
    }

    private fun arrowCount(size: Int, fill: Double) = Math.round(size * size * fill).toInt()

    /** Traps wanted over the first moves of a board, from level 4. */
    private const val TRAPS = 2
    private const val FILL_STEPS = 20

    // Salts: bump them all when the generator or the curve changes on purpose (version 3: slides, locks, rotation).
    private const val FILL_SALT = 0xF113L
    private const val LEVEL_PRIME = 7_919L
    private const val LEVEL_SALT = 0x5EED_0003L
    private const val DAILY_PRIME = 104_729L
    private const val DAILY_SALT = 0xDA11_0003L
}
