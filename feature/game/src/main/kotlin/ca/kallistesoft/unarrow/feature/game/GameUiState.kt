package ca.kallistesoft.unarrow.feature.game

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

/** A tap on an arrow that can't leave: [arrowId] shakes and [blockerId] flashes. [serial] tells two taps apart. */
data class BlockedTap(val arrowId: Int, val blockerId: Int, val serial: Int)

/** One puzzle being played. */
data class GameSession(
    val mode: GameMode,
    /** The level played (LEVELS), or the player's level (DAILY, for ad pacing). */
    val level: Int,
    /** The daily puzzle's number ("Daily #12"), 0 for levels. */
    val dailyNumber: Long,
    val board: Board,
    val startArrowCount: Int,
    val lives: Int = MAX_LIVES,
    val taps: Int = 0,
    /** The arrow a hint points at, until it's gone. */
    val hintId: Int? = null,
    val blocked: BlockedTap? = null,
    /** The last arrows that left, newest last, for the slide-out animation. */
    val exits: List<Arrow> = emptyList(),
    val status: GameStatus = GameStatus.PLAYING,
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
)

const val MAX_LIVES = 3

/** How many recent exits the board animates. */
internal const val EXITS_KEPT = 6
