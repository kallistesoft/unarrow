package ca.kallistesoft.unarrow.core.game

/**
 * A solved puzzle's rating: 3 stars for no mistake within [targetSeconds] and no hint, 2 for at most one mistake
 * (whatever the time, or with a hint), 1 for any other win. A mistake is a tap on a blocked arrow.
 */
object Stars {
    const val MAX = 3

    /** Time to read the board, then a few seconds per arrow (10 arrows: 0:45, a 9 × 9 of 70: 3:45). */
    private const val BASE_SECONDS = 15
    private const val SECONDS_PER_ARROW = 3

    /** The time limit for 3 stars on a board of [arrowCount] arrows. */
    fun targetSeconds(arrowCount: Int): Int = BASE_SECONDS + SECONDS_PER_ARROW * arrowCount

    fun of(arrowCount: Int, mistakes: Int, seconds: Int, hintUsed: Boolean): Int = when {
        mistakes == 0 && !hintUsed && seconds <= targetSeconds(arrowCount) -> 3
        mistakes <= 1 -> 2
        else -> 1
    }
}
