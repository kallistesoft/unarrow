package ca.kallistesoft.unarrow.core.game

/** Where an arrow points, and so where it leaves the board. [degrees] is its rotation from RIGHT, clockwise. */
enum class Direction(val dRow: Int, val dCol: Int, val degrees: Float) {
    UP(-1, 0, 270f),
    RIGHT(0, 1, 0f),
    DOWN(1, 0, 90f),
    LEFT(0, -1, 180f),
    ;

    /** A quarter turn clockwise. */
    val clockwise: Direction get() = entries[(ordinal + 1) % entries.size]

    /** [turns] quarter turns clockwise (negative: anticlockwise). */
    fun turned(turns: Int): Direction = entries[Math.floorMod(ordinal + turns, entries.size)]
}

/**
 * One arrow on the board: it fills the cell at [row], [col] and moves towards [direction] when tapped.
 * - [rotating]: turns a quarter clockwise after every move the player makes (an exit or a slide).
 * - [lock]: locked with that colour (0 until [LOCK_COLOURS]) while the arrow holding the [key] of the same colour is on
 *   the board; a locked arrow doesn't move.
 * - [key]: holds the key of that colour; its locks open once it has left.
 */
data class Arrow(
    val id: Int,
    val row: Int,
    val col: Int,
    val direction: Direction,
    val rotating: Boolean = false,
    val lock: Int? = null,
    val key: Int? = null,
) {
    companion object {
        /** At most this many lock colours on a board. */
        const val LOCK_COLOURS = 3
    }
}
