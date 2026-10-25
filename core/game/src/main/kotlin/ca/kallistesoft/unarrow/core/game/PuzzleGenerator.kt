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
 * Then the [Mechanics] are added on top of that line (the order the arrows were placed in, reversed):
 * - a rotating arrow turns a quarter after each move, so it starts turned back by the number of moves before its own
 *   exit and points the right way when its turn comes;
 * - each lock colour gets a key on an arrow that leaves early in the line (one blocked at the start, so it takes work
 *   to free), and its locks go on arrows that leave after it, preferably ones free at the start.
 * The line stays a perfect one (exits only, no life lost): positions never change in it, and each arrow points the
 * same way at its turn.
 *
 * Each puzzle is built [ATTEMPTS] times from the same [SeededRandom]. Plain boards keep the one with the fewest free
 * arrows at the start (then the most rounds); boards that need traps ([Mechanics.minTraps]) or carry mechanics keep
 * the one with enough traps ([Difficulty], measured over the first [TRAP_STEPS] moves; up to [MAX_ROUNDS] rounds of
 * attempts until one has them), then the most exits that trap, the fewest free arrows and the most traps.
 */
object PuzzleGenerator {
    const val ATTEMPTS = 6
    const val TRAP_STEPS = 6

    /** At most this many rounds of [ATTEMPTS] when no board has the traps wanted. */
    const val MAX_ROUNDS = 3

    /** Rotating arrows are drawn among this many best placed ones. */
    private const val ROTATING_CHOICES = 4

    fun generate(
        rows: Int,
        cols: Int,
        count: Int,
        random: SeededRandom,
        width: Int = 2,
        mechanics: Mechanics = Mechanics(),
    ): Board {
        require(count in 1..rows * cols) { "Can't place $count arrows on $rows × $cols" }
        require(width >= 1) { "At least one arrow is free: $width" }
        if (mechanics == Mechanics()) {
            return List(ATTEMPTS) { Builder(rows, cols, random, width).build(count) }
                .minWith(compareBy<Board> { it.freeArrows().size }.thenByDescending { it.rounds() })
        }
        val order = compareBy<Pair<Board, Difficulty?>> { (_, difficulty) -> traps(difficulty) < mechanics.minTraps }
            .thenByDescending { (_, difficulty) -> difficulty?.trapExits ?: 0 }
            .thenBy { (board, _) -> board.freeArrows().size }
            .thenByDescending { (_, difficulty) -> traps(difficulty) }
        var best: Pair<Board, Difficulty?>? = null
        // More rounds of attempts only while none has the traps wanted.
        for (round in 0 until MAX_ROUNDS) {
            val candidates = List(ATTEMPTS) {
                val board = decorate(Builder(rows, cols, random, width).build(count), random, mechanics)
                board to Difficulty.of(board, TRAP_STEPS)
            }
            best = (candidates + listOfNotNull(best)).minWith(order)
            if (traps(best.second) >= mechanics.minTraps) break
        }
        return best!!.first
    }

    private fun traps(difficulty: Difficulty?): Int = difficulty?.traps ?: 0

    /** Adds rotating arrows and locks to [board] without breaking its perfect line (see the class comment). */
    private fun decorate(board: Board, random: SeededRandom, mechanics: Mechanics): Board {
        val count = board.arrows.size
        // The Builder numbers arrows in the order it placed them: the perfect line takes them off in reverse.
        fun turn(arrow: Arrow) = count - 1 - arrow.id
        val inLine = board.arrows.sortedBy(::turn)
        val changed = board.arrows.associateBy { it.id }.toMutableMap()
        val used = HashSet<Int>()
        fun pick(options: List<Arrow>): Arrow = options[random.nextInt(options.size)].also { used += it.id }

        for (colour in 0 until minOf(mechanics.lockColours, Arrow.LOCK_COLOURS)) {
            val keys = inLine.filter { it.id !in used && turn(it) in count / 5 until count / 2 }
                .let { options -> options.filterNot(board::canExit).ifEmpty { options } }
            if (keys.isEmpty()) break
            val key = pick(keys)
            changed[key.id] = key.copy(key = colour)
            val later = inLine.filter { it.id !in used && turn(it) > turn(key) }
            val free = later.filter(board::canExit).ifEmpty { later }
            repeat(minOf(1 + random.nextInt(2), free.size)) {
                val locked = pick(free.filter { it.id !in used })
                changed[locked.id] = locked.copy(lock = colour)
            }
        }
        // A rotating arrow traps the player when it gates the rest of the board: at its turn it can only leave one way
        // (the other sides blocked by arrows still there) and few arrows can leave without it ("fillers"). Missing its
        // window then leaves too few moves to wait for the next one. Such arrows are preferred.
        fun gate(arrow: Arrow): Pair<Int, Int> {
            val later = board.copy(arrows = inLine.filter { turn(it) >= turn(arrow) })
            val closed = Direction.entries.count { later.blockerOf(arrow.copy(direction = it)) != null }
            var rest = later.copy(arrows = later.arrows - arrow)
            var fillers = 0
            while (true) {
                val free = rest.freeArrows().filter { candidate ->
                    later.copy(arrows = rest.arrows + arrow).blockerOf(candidate) == null
                }
                if (free.isEmpty()) break
                fillers += free.size
                rest = rest.copy(arrows = rest.arrows - free.toSet())
            }
            return closed to fillers
        }
        repeat(mechanics.rotating) {
            val options = inLine.filter { it.id !in used && turn(it) > 0 }
                .map { it to gate(it) }
                .sortedWith(compareByDescending<Pair<Arrow, Pair<Int, Int>>> { it.second.first }.thenBy { it.second.second })
                .take(ROTATING_CHOICES)
                .map { it.first }
            if (options.isEmpty()) return@repeat
            val arrow = pick(options)
            changed[arrow.id] = arrow.copy(rotating = true, direction = arrow.direction.turned(-turn(arrow)))
        }
        return board.copy(arrows = board.arrows.map { changed.getValue(it.id) })
    }

    /**
     * What a board carries besides plain arrows: [rotating] arrows, [lockColours] lock colours (each with one key and
     * one or two locks), and at least [minTraps] traps over the first moves when possible.
     */
    data class Mechanics(val rotating: Int = 0, val lockColours: Int = 0, val minTraps: Int = 0)

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
