package ca.kallistesoft.unarrow.core.game

/**
 * How hard a board is to play perfectly, measured along a perfect line (exits only):
 * - [moves]: the length of the line (every arrow leaves once);
 * - [traps]: legal taps along the line (exits or slides) after which no perfect line is left, so more lives would be
 *   lost; [trapExits] of them are exits, which look safe (only rotating arrows make an exit a trap);
 * - [freeAtStart]: arrows that can leave at the start;
 * - [safeExits]: on average, how many exits keep a perfect line at each step (fewer: more look-ahead needed).
 */
data class Difficulty(
    val moves: Int,
    val traps: Int,
    val trapExits: Int,
    val freeAtStart: Int,
    val safeExits: Double,
) {
    companion object {
        /** Search budget for each state checked: a state the solver gives up on isn't counted as a trap. */
        const val CHECK_BUDGET = 3_000

        /**
         * Measures [board] over the first [steps] states of its perfect line; null when no perfect line was found.
         * Exits on a board without rotating arrows never trap (an exit only clears paths), so only slides are checked
         * there.
         */
        fun of(board: Board, steps: Int = Int.MAX_VALUE, solver: Solver = Solver()): Difficulty? {
            val line = (solver.solve(board) as? Solution.Found)?.moves ?: return null
            val check = Solver(CHECK_BUDGET)
            var state = board
            var traps = 0
            var trapExits = 0
            var safe = 0
            var measured = 0
            for (move in line.take(steps)) {
                val rotating = state.arrows.any { it.rotating }
                for (arrow in state.arrows) {
                    when (val result = state.tap(arrow.id)) {
                        is TapResult.Exited -> {
                            val trap = rotating && check.solve(result.board) == Solution.None
                            if (trap) trapExits++ else safe++
                            if (trap) traps++
                        }
                        is TapResult.Slid -> if (check.solve(result.board) == Solution.None) traps++
                        else -> Unit
                    }
                }
                measured++
                state = (state.tap(move) as TapResult.Exited).board
            }
            return Difficulty(line.size, traps, trapExits, board.freeArrows().size, safe.toDouble() / maxOf(1, measured))
        }
    }
}
