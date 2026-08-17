package io.github.gitlrd.gpstripcomputer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchTest {

    @Test
    fun `a new stopwatch is stopped at zero`() {
        val stopwatch = Stopwatch()
        assertFalse(stopwatch.isRunning)
        assertEquals(0L, stopwatch.elapsedAt(nowRealtime = 1_000))
    }

    @Test
    fun `a running stopwatch reads the time since it was started`() {
        val stopwatch = Stopwatch().toggledAt(1_000)
        assertTrue(stopwatch.isRunning)
        assertEquals(3_000L, stopwatch.elapsedAt(nowRealtime = 4_000))
    }

    /**
     * The reason elapsed time is derived from the clock rather than accumulated on a tick:
     * a redraw that never happens must not cost the stopwatch any time. Reading it once,
     * late, gives exactly the same answer as reading it every 100 ms would have.
     */
    @Test
    fun `elapsed time does not depend on how often it is read`() {
        val stopwatch = Stopwatch().toggledAt(0)
        assertEquals(600_000L, stopwatch.elapsedAt(nowRealtime = 600_000))
    }

    @Test
    fun `stopping banks the time it was running for`() {
        val stopwatch = Stopwatch().toggledAt(1_000).toggledAt(4_000)
        assertFalse(stopwatch.isRunning)
        assertEquals(3_000L, stopwatch.accumulatedMillis)
    }

    @Test
    fun `a stopped stopwatch holds its reading however long it sits there`() {
        val stopwatch = Stopwatch().toggledAt(1_000).toggledAt(4_000)
        assertEquals(3_000L, stopwatch.elapsedAt(nowRealtime = 4_000))
        assertEquals(3_000L, stopwatch.elapsedAt(nowRealtime = 900_000))
    }

    @Test
    fun `restarting adds to the banked time rather than replacing it`() {
        val stopwatch = Stopwatch()
            .toggledAt(1_000)
            .toggledAt(4_000) // banked 3 s
            .toggledAt(10_000) // running again

        assertTrue(stopwatch.isRunning)
        assertEquals(3_000L + 2_000L, stopwatch.elapsedAt(nowRealtime = 12_000))
    }

    @Test
    fun `clearing empties a running stopwatch`() {
        val stopwatch = Stopwatch().toggledAt(1_000).cleared()
        assertFalse(stopwatch.isRunning)
        assertEquals(0L, stopwatch.elapsedAt(nowRealtime = 9_000))
    }

    @Test
    fun `clearing empties a stopped stopwatch`() {
        val stopwatch = Stopwatch().toggledAt(1_000).toggledAt(4_000).cleared()
        assertEquals(Stopwatch(), stopwatch)
    }

    // --- surviving the process going away ------------------------------------------------

    /**
     * A stopwatch that was left running keeps counting while the app is dead. That is the
     * point of storing a start time rather than a tally: Android reclaiming the process
     * mid-regularity must not quietly stop the clock.
     */
    @Test
    fun `a running stopwatch restored in the same boot keeps counting through the gap`() {
        val saved = Stopwatch().toggledAt(1_000)
        val restored = saved.restoredAt(nowRealtime = 61_000)

        assertTrue(restored.isRunning)
        assertEquals(60_000L, restored.elapsedAt(nowRealtime = 61_000))
    }

    /**
     * elapsedRealtime restarts from zero on reboot, so a stored start time in the future
     * means the device has restarted. How long it ran across the reboot is unknowable, so
     * the honest answer is to stop and keep only what was banked before it.
     */
    @Test
    fun `a reboot stops a running stopwatch and keeps the time banked before it`() {
        val saved = Stopwatch(accumulatedMillis = 5_000, startedAtRealtime = 500_000)
        val restored = saved.restoredAt(nowRealtime = 3_000)

        assertFalse(restored.isRunning)
        assertEquals(5_000L, restored.accumulatedMillis)
    }

    @Test
    fun `restoring a stopped stopwatch changes nothing`() {
        val saved = Stopwatch(accumulatedMillis = 5_000)
        assertEquals(saved, saved.restoredAt(nowRealtime = 3_000))
    }

    /** A clock that appears to go backwards must never produce a negative reading. */
    @Test
    fun `elapsed time never goes negative`() {
        val stopwatch = Stopwatch().toggledAt(10_000)
        assertEquals(0L, stopwatch.elapsedAt(nowRealtime = 9_000))
    }
}
