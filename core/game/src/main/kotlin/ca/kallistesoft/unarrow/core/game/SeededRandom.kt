package ca.kallistesoft.unarrow.core.game

/**
 * SplitMix64: a small random generator whose output is fixed by its seed forever. Levels and daily puzzles are
 * generated from seeds, so the same level must stay the same board on every device and in every app version, which
 * `kotlin.random.Random` doesn't promise.
 */
class SeededRandom(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** A number in 0 until [bound]. */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive: $bound" }
        return ((nextLong() ushr 1) % bound).toInt()
    }

    /** Fisher–Yates. */
    fun <T> shuffle(list: MutableList<T>) {
        for (i in list.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            list[i] = list[j].also { list[j] = list[i] }
        }
    }

    private companion object {
        const val GOLDEN_GAMMA = -0x61c8864680b583ebL
        const val MIX_1 = -0x40a7b892e31b1a47L
        const val MIX_2 = -0x6b2fb644ecceee15L
    }
}
