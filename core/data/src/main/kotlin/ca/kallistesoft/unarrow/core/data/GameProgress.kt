package ca.kallistesoft.unarrow.core.data

import kotlinx.serialization.Serializable

/** Everything the game remembers. It stays on the device: no account, no server. */
@Serializable
data class GameProgress(
    /** The next level to play (levels before it are solved). */
    val level: Int = 1,
    val dailyStreak: Int = 0,
    val bestDailyStreak: Int = 0,
    /** The last day (epoch day) the daily puzzle was solved. */
    val lastDailyDay: Long? = null,
    val hints: Int = STARTING_HINTS,
    /** Puzzles solved, levels and dailies together (the rating prompt counts them). */
    val wins: Int = 0,
    val lastReviewPromptDay: Long? = null,
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
    /** Last known "Remove ads" ownership, so ads stay off offline; Play Billing stays the source of truth. */
    val removeAdsOwned: Boolean = false,
    /** Earlier versions' best stars per level, read once and moved to [levelResults] ([migrated]). */
    val levelStars: Map<Int, Int> = emptyMap(),
    /** The best result of each solved level (most stars, then fastest), the history's Levels tab. */
    val levelResults: Map<Int, LevelResult> = emptyMap(),
    /** The best result of each solved daily, by epoch day, the history's Daily tab. */
    val dailyResults: Map<Long, DailyResult> = emptyMap(),
    /** The best daily puzzle ever solved: the most stars, then the fastest. */
    val bestDaily: DailyResult? = null,
    /** The best try at the daily of [lastDailyDay] (a replay can improve it). */
    val lastDailyResult: DailyResult? = null,
    /** The tips already shown (a mechanic's name), each shown once. */
    val tipsSeen: Set<String> = emptySet(),
    /** Light or dark look, or the system's (Settings › Theme). */
    val theme: AppTheme = AppTheme.SYSTEM,
) {
    fun dailyDone(today: Long): Boolean = lastDailyDay == today

    /** [today]'s daily result, when it was solved. */
    fun dailyResult(today: Long): DailyResult? = lastDailyResult.takeIf { dailyDone(today) }

    /** Every star earned: the best of each solved level, added up. */
    val totalStars: Int get() = levelResults.values.sumOf { it.stars }

    /** The streak still counts until the end of the day after the last solved daily. */
    fun currentStreak(today: Long): Int =
        if (lastDailyDay != null && today - lastDailyDay <= 1) dailyStreak else 0

    companion object {
        const val STARTING_HINTS = 3
    }
}

/** The look chosen in Settings: the system's light or dark, or one of them always. */
@Serializable
enum class AppTheme { SYSTEM, LIGHT, DARK }

/**
 * A solved level: its stars (1 to 3) and the time it took, in seconds; [seconds] is null for levels solved before
 * times were kept (shown "—").
 */
@Serializable
data class LevelResult(val stars: Int, val seconds: Int? = null) {
    /** More stars win; with as many, the faster time (an unknown time is the slowest); a tie keeps the earlier. */
    fun beats(other: LevelResult?): Boolean = other == null || stars > other.stars ||
        (stars == other.stars && seconds != null && (other.seconds == null || seconds < other.seconds))
}

private fun LevelResult.bestOf(other: LevelResult?): LevelResult = other?.takeUnless { beats(it) } ?: this

private fun DailyResult.bestOf(other: DailyResult?): DailyResult = other?.takeUnless { beats(it) } ?: this

/** A solved daily puzzle: its stars (1 to 3) and the time it took, in seconds. */
@Serializable
data class DailyResult(val stars: Int, val seconds: Int) {
    /** More stars win; with as many, the faster time. */
    fun beats(other: DailyResult?): Boolean =
        other == null || stars > other.stars || (stars == other.stars && seconds < other.seconds)
}

/** [progress] with [level] solved as [result]: the next level opens, the level keeps its best result. */
fun GameProgress.withLevelSolved(level: Int, result: LevelResult): GameProgress = copy(
    level = maxOf(this.level, level + 1),
    wins = wins + 1,
    levelResults = levelResults + (level to result.bestOf(levelResults[level])),
)

/** [level] replayed from the history: only its best result can change, never the current level. */
fun GameProgress.withLevelReplayed(level: Int, result: LevelResult): GameProgress =
    copy(levelResults = levelResults + (level to result.bestOf(levelResults[level])))

/**
 * The daily of [day] replayed (from the history, or today's from home): its best result, the best daily and today's
 * result can improve; the streak and the last solved day never move.
 */
fun GameProgress.withDailyReplayed(day: Long, result: DailyResult): GameProgress = copy(
    dailyResults = dailyResults + (day to result.bestOf(dailyResults[day])),
    bestDaily = result.bestOf(bestDaily),
    lastDailyResult = if (day == lastDailyDay) result.bestOf(lastDailyResult) else lastDailyResult,
)

/**
 * Data saved by earlier versions, moved to the current fields: best stars per level become results with no time, and
 * the last solved daily (the only one known) starts the daily history.
 */
fun GameProgress.migrated(): GameProgress {
    var progress = this
    if (levelStars.isNotEmpty()) {
        val old = levelStars.mapValues { (_, stars) -> LevelResult(stars) }
        progress = progress.copy(levelResults = old + levelResults, levelStars = emptyMap())
    }
    val day = lastDailyDay
    val result = lastDailyResult
    if (day != null && result != null && day !in dailyResults) {
        progress = progress.copy(dailyResults = progress.dailyResults + (day to result))
    }
    return progress
}

/**
 * [progress] with the daily of [day] solved as [result]: the streak grows if the day before was solved too, else
 * starts again at 1; solving the same day again changes nothing but the best result.
 */
fun GameProgress.withDailySolved(day: Long, result: DailyResult): GameProgress {
    if (lastDailyDay == day) return withDailyReplayed(day, result)
    val streak = if (lastDailyDay == day - 1) dailyStreak + 1 else 1
    return copy(
        dailyStreak = streak,
        bestDailyStreak = maxOf(bestDailyStreak, streak),
        lastDailyDay = day,
        wins = wins + 1,
        bestDaily = result.bestOf(bestDaily),
        lastDailyResult = result,
        dailyResults = dailyResults + (day to result.bestOf(dailyResults[day])),
    )
}
