package ca.kallistesoft.unarrow.core.game

/**
 * Builds solvable puzzles in reverse: arrows are placed one by one, each with a clear path to the edge among the
 * arrows already placed, so taking them off in the opposite order always works.
 *
 * Cells are placed from the centre outwards (the ring nearest the edge last). A cell's path towards its nearest edge
 * only crosses outer rings, still empty when it's placed, so every chosen cell gets an arrow and the board always
 * holds exactly the requested count. Among the clear directions, longer paths are more likely: they cross more cells,
 * so more arrows end up depending on each other.
 */
object PuzzleGenerator {
    fun generate(rows: Int, cols: Int, count: Int, random: SeededRandom): Board {
        require(count in 1..rows * cols) { "Can't place $count arrows on $rows × $cols" }
        val cells = (0 until rows * cols).toMutableList().also { random.shuffle(it) }.take(count)
        val ordered = cells.sortedByDescending { ring(it / cols, it % cols, rows, cols) }
        val grid = arrayOfNulls<Arrow>(rows * cols)
        val placed = ArrayList<Arrow>(count)

        for (cell in ordered) {
            val row = cell / cols
            val col = cell % cols
            val options = Direction.entries.mapNotNull { direction ->
                val length = clearLength(grid, rows, cols, row, col, direction)
                if (length < 0) null else direction to length
            }
            val direction = random.weighted(options)
            val arrow = Arrow(id = placed.size, row = row, col = col, direction = direction)
            grid[cell] = arrow
            placed += arrow
        }
        return Board(rows, cols, placed)
    }

    /** How far the cell is from the nearest edge (0 on the border). */
    private fun ring(row: Int, col: Int, rows: Int, cols: Int) = minOf(row, col, rows - 1 - row, cols - 1 - col)

    /** The number of cells to the edge in [direction], or -1 when an arrow is in the way. */
    private fun clearLength(grid: Array<Arrow?>, rows: Int, cols: Int, row: Int, col: Int, direction: Direction): Int {
        var r = row + direction.dRow
        var c = col + direction.dCol
        var length = 0
        while (r in 0 until rows && c in 0 until cols) {
            if (grid[r * cols + c] != null) return -1
            length++
            r += direction.dRow
            c += direction.dCol
        }
        return length
    }

    /** Picks a direction, the longer paths more likely; a border arrow pointing straight out still counts once. */
    private fun SeededRandom.weighted(options: List<Pair<Direction, Int>>): Direction {
        val total = options.sumOf { it.second + 1 }
        var pick = nextInt(total)
        for ((direction, length) in options) {
            pick -= length + 1
            if (pick < 0) return direction
        }
        return options.last().first
    }
}
