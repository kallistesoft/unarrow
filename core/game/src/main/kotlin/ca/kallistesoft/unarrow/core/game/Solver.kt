package ca.kallistesoft.unarrow.core.game

/** The result of looking for a perfect line: exits only, so no life lost. */
sealed interface Solution {
    /** Tap these arrows (ids) in this order. */
    data class Found(val moves: List<Int>) : Solution

    /** No order of exits clears the board: a life must be lost (a slide) to get anywhere. */
    data object None : Solution

    /** The search gave up after [Solver.budget] states; the board may or may not be solvable. */
    data object Unknown : Solution
}

/**
 * Finds a perfect line through a board: an order of exits that clears it. Positions don't change in such a line (only
 * slides move arrows), and every exit turns the rotating arrows, so a state is just the set of arrows left: the time
 * (and so each rotating arrow's direction) is the number of arrows already gone. States found dead are remembered.
 *
 * Without rotating arrows the order doesn't matter (an exit never blocks anything and a key only opens locks), so any
 * free arrow is taken; with them, the search tries each free arrow in turn, rotating ones first, and gives up after
 * [budget] states.
 */
class Solver(val budget: Int = DEFAULT_BUDGET) {
    fun solve(board: Board): Solution = try {
        Search(board, budget).run()
    } catch (_: BudgetSpent) {
        Solution.Unknown
    }

    private class BudgetSpent : RuntimeException()

    /** Arrow sets as two 64-bit masks (up to 128 arrows: a 9 × 9 board has 81 cells). */
    private data class ArrowSet(val low: Long, val high: Long) {
        operator fun contains(index: Int): Boolean =
            if (index < 64) low and (1L shl index) != 0L else high and (1L shl (index - 64)) != 0L

        fun without(index: Int): ArrowSet =
            if (index < 64) copy(low = low and (1L shl index).inv()) else copy(high = high and (1L shl (index - 64)).inv())

        fun meets(other: ArrowSet): Boolean = low and other.low != 0L || high and other.high != 0L

        companion object {
            fun of(indices: Iterable<Int>): ArrowSet =
                indices.fold(ArrowSet(0, 0)) { set, i -> if (i < 64) set.copy(low = set.low or (1L shl i)) else set.copy(high = set.high or (1L shl (i - 64))) }
        }
    }

    private class Search(board: Board, private val budget: Int) {
        private val arrows = board.arrows
        private val count = arrows.size
        private val rotating = ArrowSet.of(arrows.indices.filter { arrows[it].rotating })

        /** Per arrow and direction: the arrows on its path to the edge (positions never change in a perfect line). */
        private val paths: List<List<ArrowSet>> = arrows.map { arrow ->
            Direction.entries.map { direction ->
                ArrowSet.of(cellsTowards(board, arrow, direction).mapNotNull { (row, col) ->
                    board.arrowAt(row, col)?.let(arrows::indexOf)
                })
            }
        }

        /** Per arrow: the index of the arrow holding its lock's key, or -1. */
        private val keyHolder = arrows.map { arrow -> arrow.lock?.let { lock -> arrows.indexOfFirst { it.key == lock } } ?: -1 }
        private val dead = HashSet<ArrowSet>()
        private var visited = 0

        fun run(): Solution {
            val moves = ArrayList<Int>(count)
            return if (clear(ArrowSet.of(arrows.indices), count, moves)) {
                Solution.Found(moves.map { arrows[it].id })
            } else {
                Solution.None
            }
        }

        /** Clears [left] ([size] arrows) from time count - size; on success [moves] holds the exits, in order. */
        private fun clear(left: ArrowSet, size: Int, moves: MutableList<Int>): Boolean {
            if (size == 0) return true
            if (left in dead) return false
            if (++visited > budget) throw BudgetSpent()
            if (!left.meets(rotating)) return clearInAnyOrder(left, size, moves)
            val time = count - size
            val candidates = (0 until count).filter { it in left && canExit(it, left, time) }
                .sortedByDescending { arrows[it].rotating }
            for (index in candidates) {
                moves += index
                if (clear(left.without(index), size - 1, moves)) return true
                moves.removeAt(moves.lastIndex)
            }
            dead += left
            return false
        }

        /** No rotating arrow left: taking any free arrow never hurts, so take them as they come. */
        private fun clearInAnyOrder(start: ArrowSet, size: Int, moves: MutableList<Int>): Boolean {
            var left = start
            var remaining = size
            val added = moves.size
            while (remaining > 0) {
                val next = (0 until count).firstOrNull { it in left && canExit(it, left, 0) }
                if (next == null) {
                    while (moves.size > added) moves.removeAt(moves.lastIndex)
                    dead += start
                    return false
                }
                moves += next
                left = left.without(next)
                remaining--
            }
            return true
        }

        private fun canExit(index: Int, left: ArrowSet, time: Int): Boolean {
            val key = keyHolder[index]
            if (key >= 0 && key in left) return false
            val arrow = arrows[index]
            val direction = if (arrow.rotating) arrow.direction.turned(time) else arrow.direction
            return !paths[index][direction.ordinal].meets(left)
        }
    }

    companion object {
        const val DEFAULT_BUDGET = 200_000

        /** The cells from [arrow] to the edge in [direction], nearest first. */
        internal fun cellsTowards(board: Board, arrow: Arrow, direction: Direction): List<Pair<Int, Int>> =
            generateSequence(arrow.row + direction.dRow to arrow.col + direction.dCol) { (row, col) ->
                row + direction.dRow to col + direction.dCol
            }.takeWhile { (row, col) -> row in 0 until board.rows && col in 0 until board.cols }.toList()
    }
}
