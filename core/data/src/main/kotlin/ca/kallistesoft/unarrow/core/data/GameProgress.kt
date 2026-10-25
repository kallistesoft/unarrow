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
    /** The most stars earned on each solved level (1 to 3). */
    val levelStars: Map<Int, Int> = emptyMap(),
    /** The best daily puzzle ever solved: the most stars, then the fastest. */
    val bestDaily: DailyResult? = null,
    /** The tips already shown (a mechanic's name), each shown once. */
    val tipsSeen: Set<String> = emptySet(),
    /** Light or dark look, or the system's (Settings › Theme). */
    val theme: AppTheme = AppTheme.SYSTEM,
) {
    fun dailyDone(today: Long): Boolean = lastDailyDay == today

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

/** A solved daily puzzle: its stars (1 to 3) and the time it took, in seconds. */
@Serializable
data class DailyResult(val stars: Int, val seconds: Int) {
    /** More stars win; with as many, the faster time. */
    fun beats(other: DailyResult?): Boolean =
        other == null || stars > other.stars || (stars == other.stars && seconds < other.seconds)
}

/** [progress] with [level] solved for [stars]: the next level opens, the level keeps its best stars. */
fun GameProgress.withLevelSolved(level: Int, stars: Int): GameProgress = copy(
    level = maxOf(this.level, level + 1),
    wins = wins + 1,
    levelStars = levelStars + (level to maxOf(stars, levelStars[level] ?: 0)),
)

/**
 * [progress] with the daily of [day] solved as [result]: the streak grows if the day before was solved too, else
 * starts again at 1; solving the same day again changes nothing but the best result.
 */
fun GameProgress.withDailySolved(day: Long, result: DailyResult): GameProgress {
    val best = if (result.beats(bestDaily)) result else bestDaily
    if (lastDailyDay == day) return copy(bestDaily = best)
    val streak = if (lastDailyDay == day - 1) dailyStreak + 1 else 1
    return copy(
        dailyStreak = streak,
        bestDailyStreak = maxOf(bestDailyStreak, streak),
        lastDailyDay = day,
        wins = wins + 1,
        bestDaily = best,
    )
}
