package ca.kallistesoft.unarrow.feature.game

import ca.kallistesoft.unarrow.core.game.Board
import ca.kallistesoft.unarrow.core.game.Levels
import ca.kallistesoft.unarrow.core.game.Solution
import ca.kallistesoft.unarrow.core.game.Solver
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** What a hint can say about the board as it is now. */
sealed interface Hint {
    /** Tapping [arrowId] keeps a perfect line. */
    data class Move(val arrowId: Int) : Hint

    /** No perfect line is left: only a restart (or more lives lost) gets out. */
    data object Stuck : Hint
}

/**
 * Boards and hints, worked out off the main thread: generating a board (with its solver checks) or searching a
 * perfect line takes up to a few tenths of a second on a phone. Tests pass their own dispatcher and boards.
 */
class Puzzles(
    private val dispatcher: CoroutineDispatcher,
    private val levelBoard: (Int) -> Board = { Levels.spec(it).board() },
    private val dailyBoard: (Long) -> Board = { Levels.daily(it).board() },
    private val solver: Solver = Solver(),
) {
    @Inject
    constructor() : this(Dispatchers.Default)

    suspend fun level(level: Int): Board = withContext(dispatcher) { levelBoard(level) }

    suspend fun daily(epochDay: Long): Board = withContext(dispatcher) { dailyBoard(epochDay) }

    /**
     * The next move of a perfect line from [board]. When the search gives up (a rare, very open board), any arrow that
     * can leave now is pointed at instead.
     */
    suspend fun hint(board: Board): Hint = withContext(dispatcher) {
        when (val solution = solver.solve(board)) {
            is Solution.Found -> Hint.Move(solution.moves.first())
            Solution.None -> Hint.Stuck
            Solution.Unknown -> board.freeArrows().firstOrNull()?.let { Hint.Move(it.id) } ?: Hint.Stuck
        }
    }
}
