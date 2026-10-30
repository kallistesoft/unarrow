package ca.kallistesoft.unarrow.core.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SolverTest {
    private val solver = Solver()

    /** Plays [moves] as exits only; true when they clear [board]. */
    private fun clears(board: Board, moves: List<Int>): Boolean = moves.fold(board as Board?) { b, id ->
        (b?.tap(id) as? TapResult.Exited)?.board
    }?.isCleared == true

    @Test
    fun `a plain board is solved by exits only`() {
        val board = Levels.spec(9).board()
        val line = (solver.solve(board) as Solution.Found).moves
        assertTrue(clears(board, line))
        assertEquals(Solution.Found(emptyList()), solver.solve(Board(1, 1, emptyList())))
    }

    @Test
    fun `two arrows facing each other have no perfect line`() {
        val facing = Board(1, 2, listOf(Arrow(0, 0, 0, Direction.RIGHT), Arrow(1, 0, 1, Direction.LEFT)))
        assertEquals(Solution.None, solver.solve(facing))
    }

    @Test
    fun `a key leaves before its lock`() {
        val locked = Arrow(0, 0, 0, Direction.UP, lock = 0)
        val blocker = Arrow(1, 1, 1, Direction.LEFT)
        val key = Arrow(2, 1, 2, Direction.UP, key = 0)
        val board = Board(2, 3, listOf(locked, blocker, key))
        val line = (solver.solve(board) as Solution.Found).moves
        assertTrue(line.indexOf(key.id) < line.indexOf(locked.id))
        assertTrue(clears(board, line))
    }

    @Test
    fun `a rotating arrow waits for its turn, and with nothing to wait with there is no perfect line`() {
        // . U .       U ↓ and L → wait on the rotating R in the middle, which points ↑ (into U):
        // L R .       after one other move it points → and can leave.
        // . . .
        val spinner = Arrow(0, 1, 1, Direction.UP, rotating = true)
        val waiting = listOf(spinner, Arrow(1, 0, 1, Direction.DOWN), Arrow(2, 1, 0, Direction.RIGHT))
        assertEquals(Solution.None, solver.solve(Board(3, 3, waiting)))

        val filler = Arrow(3, 0, 0, Direction.UP)
        val board = Board(3, 3, waiting + filler)
        val line = (solver.solve(board) as Solution.Found).moves
        assertEquals(listOf(filler.id, spinner.id), line.take(2))
        assertTrue(clears(board, line))
    }

    @Test
    fun `a slide can trap the player`() {
        // → . ↓      the → is blocked by the ↓, which waits on the ←, which waits on the ↑.
        // . ↑ ←      Sliding the → to the middle puts it on the ↑'s path: they all wait on each other.
        val right = Arrow(0, 0, 0, Direction.RIGHT)
        val board = Board(
            2, 3,
            listOf(right, Arrow(1, 0, 2, Direction.DOWN), Arrow(2, 1, 2, Direction.LEFT), Arrow(3, 1, 1, Direction.UP)),
        )
        assertTrue(solver.solve(board) is Solution.Found)
        val slid = board.tap(right.id) as TapResult.Slid
        assertEquals(1, slid.distance)
        assertEquals(Solution.None, solver.solve(slid.board))
        val difficulty = Difficulty.of(board)!!
        assertEquals(1, difficulty.traps)
        assertEquals(0, difficulty.trapExits)
        assertEquals(4, difficulty.moves)
        assertEquals(1, difficulty.freeAtStart)
    }

    @Test
    fun `the search gives up past its budget`() {
        assertEquals(Solution.Unknown, Solver(budget = 0).solve(Levels.daily(Levels.FIRST_DAILY_EPOCH_DAY).board()))
        assertEquals(null, Difficulty.of(Levels.daily(Levels.FIRST_DAILY_EPOCH_DAY).board(), solver = Solver(budget = 0)))
    }

    @Test
    fun `the solver agrees with trying every order on small boards with every mechanic`() {
        for (seed in 0L until 120L) {
            val size = 3 + (seed % 2).toInt()
            val count = 4 + (seed % 4).toInt()
            val board = PuzzleGenerator.generate(
                size, size, count, SeededRandom(seed),
                mechanics = PuzzleGenerator.Mechanics(rotating = 1 + (seed % 2).toInt(), lockColours = 1),
            )
            // Scramble it with one random slide: some of these lose their perfect line.
            val blocked = board.arrows.firstOrNull { !board.canExit(it) && !board.isLocked(it) }
            val scrambled = blocked?.let { (board.tap(it.id) as TapResult.Slid).board } ?: board
            for (b in listOf(board, scrambled)) {
                val expected = bruteForce(b)
                when (val found = solver.solve(b)) {
                    is Solution.Found -> assertTrue("seed $seed", expected && clears(b, found.moves))
                    else -> assertEquals("seed $seed", false, expected)
                }
            }
        }
    }

    /** Tries every order of exits. */
    private fun bruteForce(board: Board): Boolean =
        board.isCleared || board.freeArrows().any { bruteForce((board.tap(it.id) as TapResult.Exited).board) }
}
