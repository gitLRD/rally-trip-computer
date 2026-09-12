package io.github.gitlrd.rallytripcomputer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyClockTest {

    /** 2026-09-17 20:33:40 UTC — a fixed instant to read the arithmetic against. */
    private val someInstant = 1_789_677_220_000L

    @Test
    fun `an unset clock reads the same as the phone`() {
        val clock = RallyClock()
        assertEquals(0, clock.offsetSeconds)
        assertFalse(clock.isOffset)
        assertEquals(someInstant, clock.timeAt(someInstant))
    }

    /** The case this exists for: the rally's clock was 20 seconds behind the phone. */
    @Test
    fun `a negative offset reads behind the phone`() {
        val clock = RallyClock(offsetSeconds = -20)
        assertTrue(clock.isOffset)
        assertEquals(someInstant - 20_000L, clock.timeAt(someInstant))
    }

    @Test
    fun `a positive offset reads ahead of the phone`() {
        assertEquals(someInstant + 95_000L, RallyClock(offsetSeconds = 95).timeAt(someInstant))
    }

    @Test
    fun `nudging moves the offset by the step`() {
        assertEquals(-1, RallyClock().nudged(-1).offsetSeconds)
        assertEquals(-10, RallyClock().nudged(-10).offsetSeconds)
        assertEquals(10, RallyClock().nudged(10).offsetSeconds)
    }

    @Test
    fun `nudges accumulate`() {
        val clock = RallyClock().nudged(-10).nudged(-10).nudged(1)
        assertEquals(-19, clock.offsetSeconds)
    }

    /**
     * Bounded on purpose. A rally clock is out by seconds, or at worst a minute or two; an
     * unbounded offset only lets a stuck finger in the dark turn the clock into fiction.
     */
    @Test
    fun `the offset cannot be nudged past ten minutes either way`() {
        assertEquals(MAX_RALLY_OFFSET_SECONDS, RallyClock(offsetSeconds = 599).nudged(10).offsetSeconds)
        assertEquals(-MAX_RALLY_OFFSET_SECONDS, RallyClock(offsetSeconds = -599).nudged(-10).offsetSeconds)
        assertEquals(600, MAX_RALLY_OFFSET_SECONDS)
    }

    @Test
    fun `nudging away from the limit still works`() {
        val clock = RallyClock(offsetSeconds = MAX_RALLY_OFFSET_SECONDS).nudged(-1)
        assertEquals(599, clock.offsetSeconds)
    }

    @Test
    fun `zeroing puts the clock back on the phone`() {
        assertEquals(RallyClock(), RallyClock(offsetSeconds = -20).zeroed())
    }

    /**
     * An offset restored from storage is clamped too, so a corrupt or hand-edited
     * preferences value cannot get past the range the nudge buttons enforce.
     */
    @Test
    fun `an out-of-range stored offset is clamped on the way in`() {
        assertEquals(MAX_RALLY_OFFSET_SECONDS, RallyClock.of(99_999).offsetSeconds)
        assertEquals(-MAX_RALLY_OFFSET_SECONDS, RallyClock.of(-99_999).offsetSeconds)
        assertEquals(-20, RallyClock.of(-20).offsetSeconds)
    }
}
