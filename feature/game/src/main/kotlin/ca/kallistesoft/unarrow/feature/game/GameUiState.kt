package ca.kallistesoft.unarrow.feature.game

import ca.kallistesoft.unarrow.core.data.DailyResult
import ca.kallistesoft.unarrow.core.game.Arrow
import ca.kallistesoft.unarrow.core.game.Board

/** Which puzzles a game screen plays. */
enum class GameMode {
    /** The player's next level, then the ones after it. */
    LEVELS,

    /** Today's puzzle, the same for everyone. */
    DAILY,
}

enum class GameStatus { PLAYING, WON, LOST }

/**
 * A tap that didn't send an arrow out: [arrowId] slid (or, already touching, shook) into [blockerId], which both flash;
 * a locked arrow ([blockerId] null) only shakes. [serial] tells two taps apart.
 */
data class BlockedTap(val arrowId: Int, val blockerId: Int?, val serial: Int, val distance: Int = 0)

/** A one-line tip shown the first time a mechanic appears on a board (remembered in the progress by [name]). */
enum class Tip { LOCKS, ROTATING }

/** One puzzle being played. */
data class GameSession(
    val mode: GameMode,
    /** The level played (LEVELS), or the player's level (DAILY, for ad pacing). */
    val level: Int,
    /** The daily puzzle's number ("Daily #12"), 0 for levels. */
    val dailyNumber: Long,
    val board: Board,
    val startArrowCount: Int,
    /** The board as it started, for Restart. */
    val start: Board = board,
    val lives: Int = MAX_LIVES,
    val taps: Int = 0,
    /** The arrow a hint points at, until it's gone. */
    val hintId: Int? = null,
    val blocked: BlockedTap? = null,
    /** The last arrows that left, newest last, for the slide-out animation. */
    val exits: List<Arrow> = emptyList(),
    val status: GameStatus = GameStatus.PLAYING,
    /** Time spent on the puzzle while it was on screen. */
    val seconds: Int = 0,
    /** Taps on blocked arrows. */
    val mistakes: Int = 0,
    val hintUsed: Boolean = false,
    /** The rating once solved ([Stars]), 0 before. */
    val stars: Int = 0,
    /** The tip for a mechanic seen here for the first time. */
    val tip: Tip? = null,
    /** A hint found no perfect line left from here. */
    val stuck: Boolean = false,
)

data class GameUiState(
    val session: GameSession? = null,
    val hints: Int = 0,
    val rewardedReady: Boolean = false,
    /** Price of a 10-hint pack, null while Play hasn't given one (not for sale now). */
    val hintPackPrice: String? = null,
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
    /** The banner ad shows under the board (ads allowed, "Remove ads" not bought). */
    val banner: Boolean = false,
    /** The most stars ever earned on this level (null: never solved, or a daily). */
    val bestStars: Int? = null,
    /** The best daily ever solved (dailies only). */
    val bestDaily: DailyResult? = null,
)

const val MAX_LIVES = 3

/** How many recent exits the board animates. */
internal const val EXITS_KEPT = 6
