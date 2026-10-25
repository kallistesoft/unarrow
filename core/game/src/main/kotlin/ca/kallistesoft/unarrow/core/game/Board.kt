package ca.kallistesoft.unarrow.core.game

/**
 * A puzzle in play: a [rows] × [cols] grid and the arrows still on it. An arrow can leave when no other arrow stands
 * between it and the edge it points to. Taking an arrow off never blocks another one, so any order of legal taps
 * clears a solvable board: the player can only lose by tapping blocked arrows.
 */
data class Board(val rows: Int, val cols: Int, val arrows: List<Arrow>) {
    private val byCell: Map<Int, Arrow> = arrows.associateBy { cellIndex(it.row, it.col) }

    init {
        require(rows > 0 && cols > 0) { "Empty grid: $rows × $cols" }
        require(arrows.all { it.row in 0 until rows && it.col in 0 until cols }) { "Arrow outside the grid" }
        require(byCell.size == arrows.size) { "Two arrows on the same cell" }
    }

    val isCleared: Boolean get() = arrows.isEmpty()

    fun arrowAt(row: Int, col: Int): Arrow? =
        if (row in 0 until rows && col in 0 until cols) byCell[cellIndex(row, col)] else null

    /** The first arrow in the way of [arrow], or null when its path to the edge is clear. */
    fun blockerOf(arrow: Arrow): Arrow? = path(arrow).firstNotNullOfOrNull { (row, col) -> arrowAt(row, col) }

    fun canExit(arrow: Arrow): Boolean = blockerOf(arrow) == null

    /** The arrows that can leave now: the hints. */
    fun freeArrows(): List<Arrow> = arrows.filter(::canExit)

    /**
     * How many rounds clear the board when every free arrow is taken off at once, round after round: the length of its
     * longest chain of arrows waiting on each other. -1 when the board can't be cleared.
     */
    fun rounds(): Int {
        var board = this
        var rounds = 0
        while (!board.isCleared) {
            val free = board.freeArrows().toSet()
            if (free.isEmpty()) return -1
            board = board.copy(arrows = board.arrows - free)
            rounds++
        }
        return rounds
    }

    /** How many cells [arrow] travels to get off the board. */
    fun exitDistance(arrow: Arrow): Int = when (arrow.direction) {
        Direction.UP -> arrow.row + 1
        Direction.DOWN -> rows - arrow.row
        Direction.LEFT -> arrow.col + 1
        Direction.RIGHT -> cols - arrow.col
    }

    fun tap(id: Int): TapResult {
        val arrow = arrows.firstOrNull { it.id == id } ?: return TapResult.Missed
        val blocker = blockerOf(arrow)
        return if (blocker == null) {
            TapResult.Exited(arrow, copy(arrows = arrows - arrow))
        } else {
            TapResult.Blocked(arrow, blocker)
        }
    }

    /** The cells between [arrow] and the edge it points to, nearest first. */
    private fun path(arrow: Arrow): Sequence<Pair<Int, Int>> =
        generateSequence(arrow.row + arrow.direction.dRow to arrow.col + arrow.direction.dCol) { (row, col) ->
            row + arrow.direction.dRow to col + arrow.direction.dCol
        }.takeWhile { (row, col) -> row in 0 until rows && col in 0 until cols }

    private fun cellIndex(row: Int, col: Int) = row * cols + col
}

sealed interface TapResult {
    /** [arrow] left the board; [board] is what remains. */
    data class Exited(val arrow: Arrow, val board: Board) : TapResult

    /** [arrow] can't leave: [blocker] is in its way. */
    data class Blocked(val arrow: Arrow, val blocker: Arrow) : TapResult

    /** No arrow with that id (already gone). */
    data object Missed : TapResult
}
