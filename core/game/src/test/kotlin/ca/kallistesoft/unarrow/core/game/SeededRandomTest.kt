package ca.kallistesoft.unarrow.core.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeededRandomTest {
    @Test
    fun `the sequence is fixed by the seed`() {
        // SplitMix64 reference values for seed 0: these must never change, or every level would change.
        val random = SeededRandom(0)
        assertEquals(-2152535657050944081L, random.nextLong())
        assertEquals(7960286522194355700L, random.nextLong())
    }

    @Test
    fun `nextInt stays in bounds`() {
        val random = SeededRandom(5)
        repeat(1_000) { assertTrue(random.nextInt(7) in 0 until 7) }
    }

    @Test
    fun `shuffle keeps every element`() {
        val list = (1..20).toMutableList()
        SeededRandom(3).shuffle(list)
        assertEquals((1..20).toList(), list.sorted())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `bound must be positive`() {
        SeededRandom(1).nextInt(0)
    }
}
