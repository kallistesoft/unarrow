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
) {
    fun dailyDone(today: Long): Boolean = lastDailyDay == today

    /** The streak still counts until the end of the day after the last solved daily. */
    fun currentStreak(today: Long): Int =
        if (lastDailyDay != null && today - lastDailyDay <= 1) dailyStreak else 0

    companion object {
        const val STARTING_HINTS = 3
    }
}
