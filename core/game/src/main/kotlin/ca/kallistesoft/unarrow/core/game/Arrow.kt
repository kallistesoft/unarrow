package ca.kallistesoft.unarrow.core.game

/** Where an arrow points, and so where it leaves the board. [degrees] is its rotation from RIGHT, clockwise. */
enum class Direction(val dRow: Int, val dCol: Int, val degrees: Float) {
    UP(-1, 0, 270f),
    RIGHT(0, 1, 0f),
    DOWN(1, 0, 90f),
    LEFT(0, -1, 180f),
}

/** One arrow on the board: it fills the cell at [row], [col] and slides out towards [direction] when tapped. */
data class Arrow(val id: Int, val row: Int, val col: Int, val direction: Direction)
