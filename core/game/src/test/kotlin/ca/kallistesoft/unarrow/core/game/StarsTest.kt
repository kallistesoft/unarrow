package ca.kallistesoft.unarrow.core.game

import org.junit.Assert.assertEquals
import org.junit.Test

class StarsTest {
    @Test
    fun `the target time grows with the board`() {
        assertEquals(60, Stars.targetSeconds(10))
        assertEquals(188, Stars.targetSeconds(42))
    }

    @Test
    fun `three stars for no mistake in time, two for one mistake or a hint, one otherwise`() {
        assertEquals(3, Stars.of(arrowCount = 10, mistakes = 0, seconds = 60, hintUsed = false))
        assertEquals("too slow", 2, Stars.of(10, mistakes = 0, seconds = 61, hintUsed = false))
        assertEquals("a hint", 2, Stars.of(10, mistakes = 0, seconds = 10, hintUsed = true))
        assertEquals("one mistake, any time", 2, Stars.of(10, mistakes = 1, seconds = 600, hintUsed = false))
        assertEquals(1, Stars.of(10, mistakes = 2, seconds = 10, hintUsed = false))
        assertEquals(1, Stars.of(10, mistakes = 2, seconds = 10, hintUsed = true))
    }
}
