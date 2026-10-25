package ca.kallistesoft.unarrow.core.game

/**
 * A puzzle in play: a [rows] × [cols] grid and the arrows still on it. A tapped arrow leaves when no other arrow stands
 * between it and the edge it points to; otherwise it slides up to the first arrow in its way (or doesn't move if it
 * already touches it), which costs a life. After every exit or slide, each rotating arrow turns a quarter clockwise.
 * A locked arrow doesn't move until the arrow with its key has left.
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

    /** [arrow] has a lock whose key is still on the board. */
    fun isLocked(arrow: Arrow): Boolean = arrow.lock != null && arrows.any { it.key == arrow.lock }

    /** [arrow] would leave if tapped now. */
    fun canExit(arrow: Arrow): Boolean = !isLocked(arrow) && blockerOf(arrow) == null

    /** The arrows that can leave now. */
    fun freeArrows(): List<Arrow> = arrows.filter(::canExit)

    /** The board has rotating arrows or locks: the order of the moves matters. */
    val hasMechanics: Boolean get() = arrows.any { it.rotating || it.lock != null }

    /** How many cells [arrow] travels to get off the board. */
    fun exitDistance(arrow: Arrow): Int = when (arrow.direction) {
        Direction.UP -> arrow.row + 1
        Direction.DOWN -> rows - arrow.row
        Direction.LEFT -> arrow.col + 1
        Direction.RIGHT -> cols - arrow.col
    }

    /**
     * How many rounds clear the board when every free arrow is taken off at once, round after round, rotation aside:
     * the length of its longest chain of arrows waiting on each other. -1 when the board can't be cleared that way.
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

    fun tap(id: Int): TapResult {
        val arrow = arrows.firstOrNull { it.id == id } ?: return TapResult.Missed
        if (isLocked(arrow)) return TapResult.Locked(arrow)
        val blocker = blockerOf(arrow) ?: return TapResult.Exited(arrow, copy(arrows = arrows - arrow).rotated())
        val distance = path(arrow).indexOfFirst { (row, col) -> arrowAt(row, col) != null }
        val moved = arrow.copy(
            row = arrow.row + arrow.direction.dRow * distance,
            col = arrow.col + arrow.direction.dCol * distance,
        )
        return TapResult.Slid(arrow, moved, distance, blocker, copy(arrows = arrows.map { if (it == arrow) moved else it }).rotated())
    }

    /** Every rotating arrow a quarter turn clockwise: what happens after each move. */
    private fun rotated(): Board =
        if (arrows.none { it.rotating }) this else copy(arrows = arrows.map { if (it.rotating) it.copy(direction = it.direction.clockwise) else it })

    /** The cells between [arrow] and the edge it points to, nearest first. */
    private fun path(arrow: Arrow): Sequence<Pair<Int, Int>> =
        generateSequence(arrow.row + arrow.direction.dRow to arrow.col + arrow.direction.dCol) { (row, col) ->
            row + arrow.direction.dRow to col + arrow.direction.dCol
        }.takeWhile { (row, col) -> row in 0 until rows && col in 0 until cols }

    private fun cellIndex(row: Int, col: Int) = row * cols + col
}

sealed interface TapResult {
    /** [arrow] left the board; [board] is what remains, rotating arrows turned. */
    data class Exited(val arrow: Arrow, val board: Board) : TapResult

    /**
     * [arrow] was in the way of [blocker]: it slid [distance] cells (0: it already touched it) to become [moved],
     * which costs a life; [board] is the result, rotating arrows turned.
     */
    data class Slid(val arrow: Arrow, val moved: Arrow, val distance: Int, val blocker: Arrow, val board: Board) : TapResult

    /** [arrow] is locked: nothing happens. */
    data class Locked(val arrow: Arrow) : TapResult

    /** No arrow with that id (already gone). */
    data object Missed : TapResult
}
