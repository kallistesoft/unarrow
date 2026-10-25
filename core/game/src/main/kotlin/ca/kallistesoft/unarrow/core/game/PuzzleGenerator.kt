package ca.kallistesoft.unarrow.core.game

/**
 * Builds solvable puzzles in reverse: arrows are placed one by one, each with a clear path to the edge among the
 * arrows already placed, so taking them off in the opposite order always works. After each placement, the arrows on
 * the board are exactly those left in play at that point of the solution, so the construction controls how many
 * arrows are free all along the game, not only at the start.
 *
 * Taking an arrow off never blocks another, so the difficulty comes from few free arrows at a time and long chains:
 * - each new arrow goes on the path of a free one (blocking it), so the player must find the one arrow that unlocks
 *   it; while [width] or more arrows are free, an arrow that blocks nothing is only placed when no other can be;
 * - an arrow blocks one or two free ones when it can, so each tap unlocks only one or two;
 * - longer paths are much more likely (they get crossed, so blocked, by later arrows), and an arrow pointing straight
 *   off the edge (never blockable) only comes when nothing else fits;
 * - a placement never leaves fewer fillable cells than arrows still to place, so the board gets its count.
 *
 * Each puzzle is built [ATTEMPTS] times from the same [SeededRandom], and the board with the fewest free arrows at the
 * start (then the most rounds) is kept.
 */
object PuzzleGenerator {
    const val ATTEMPTS = 6

    fun generate(rows: Int, cols: Int, count: Int, random: SeededRandom, width: Int = 2): Board {
        require(count in 1..rows * cols) { "Can't place $count arrows on $rows × $cols" }
        require(width >= 1) { "At least one arrow is free: $width" }
        return List(ATTEMPTS) { Builder(rows, cols, random, width).build(count) }
            .minWith(compareBy<Board> { it.freeArrows().size }.thenByDescending { it.rounds() })
    }

    /** A cell and direction an arrow can take now, and what placing it there does. */
    private class Candidate(val cell: Int, val direction: Direction, val length: Int, val blocks: Int, val kills: Int)

    private class Builder(val rows: Int, val cols: Int, val random: SeededRandom, val width: Int) {
        private val cells = rows * cols
        private val grid = arrayOfNulls<Arrow>(cells)
        private val placed = ArrayList<Arrow>()

        /** Per empty cell, a bit per direction whose path to the edge is clear. */
        private val clearDirections = IntArray(cells) { ALL_DIRECTIONS }

        /** The cells of the arrows that can leave now. */
        private val free = HashSet<Int>()

        /** Per cell, the free arrows whose path crosses it: an arrow placed there blocks them. */
        private val crossing = Array(cells) { HashSet<Int>() }

        fun build(count: Int): Board {
            repeat(count) { step -> place(choose(remainingAfter = count - step - 1)) }
            return Board(rows, cols, placed)
        }

        private fun choose(remainingAfter: Int): Candidate {
            val fillable = (0 until cells).count { grid[it] == null && clearDirections[it] != 0 }
            val candidates = ArrayList<Candidate>()
            for (cell in 0 until cells) {
                if (grid[cell] != null || clearDirections[cell] == 0) continue
                val kills = kills(cell)
                for (direction in Direction.entries) {
                    if (clearDirections[cell] and direction.bit == 0) continue
                    candidates += Candidate(cell, direction, path(cell, direction).size, crossing[cell].size, kills)
                }
            }
            val safe = candidates.filter { fillable - 1 - it.kills >= remainingAfter }.ifEmpty { candidates }
            val useful = safe.filter { it.length > 0 || it.blocks >= 2 }.ifEmpty { safe }
            val tier = if (free.size >= width) {
                useful.filter { it.blocks in 1..2 }.ifEmpty { useful.filter { it.blocks >= 3 } }.ifEmpty { useful }
            } else {
                useful
            }
            val fewestKills = tier.minOf { it.kills }
            return random.weighted(tier.filter { it.kills == fewestKills })
        }

        /** Empty cells that would have no clear direction left once [cell] is filled. */
        private fun kills(cell: Int): Int = Direction.entries.sumOf { direction ->
            emptyCellsFrom(cell, direction).count { clearDirections[it] == direction.opposite.bit }
        }

        private fun place(candidate: Candidate) {
            val cell = candidate.cell
            for (blocked in crossing[cell].toList()) {
                free -= blocked
                for (pathCell in path(blocked, grid[blocked]!!.direction)) crossing[pathCell] -= blocked
            }
            val arrow = Arrow(id = placed.size, row = cell / cols, col = cell % cols, direction = candidate.direction)
            grid[cell] = arrow
            placed += arrow
            free += cell
            for (pathCell in path(cell, candidate.direction)) crossing[pathCell] += cell
            // Empty cells in line with the new arrow can't point through it any more.
            for (direction in Direction.entries) {
                for (empty in emptyCellsFrom(cell, direction)) {
                    clearDirections[empty] = clearDirections[empty] and direction.opposite.bit.inv()
                }
            }
            clearDirections[cell] = 0
        }

        /** The cells from [cell] (excluded) to the edge in [direction], nearest first. */
        private fun path(cell: Int, direction: Direction): List<Int> {
            val cellsOnPath = ArrayList<Int>(maxOf(rows, cols))
            var row = cell / cols + direction.dRow
            var col = cell % cols + direction.dCol
            while (row in 0 until rows && col in 0 until cols) {
                cellsOnPath += row * cols + col
                row += direction.dRow
                col += direction.dCol
            }
            return cellsOnPath
        }

        /** The empty cells next to [cell] in [direction], up to the first arrow. */
        private fun emptyCellsFrom(cell: Int, direction: Direction): List<Int> =
            path(cell, direction).takeWhile { grid[it] == null }
    }

    /** Picks a candidate, the longer paths much more likely (weight: (length + 1)²). */
    private fun SeededRandom.weighted(options: List<Candidate>): Candidate {
        fun weight(option: Candidate) = (option.length + 1) * (option.length + 1)
        var pick = nextInt(options.sumOf(::weight))
        return options.first { option ->
            pick -= weight(option)
            pick < 0
        }
    }

    private const val ALL_DIRECTIONS = 0b1111

    private val Direction.bit: Int get() = 1 shl ordinal

    private val Direction.opposite: Direction get() = Direction.entries[(ordinal + 2) % 4]
}
