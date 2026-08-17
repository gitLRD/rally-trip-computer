package io.github.gitlrd.gpstripcomputer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackingStateTest {

    private val movingMps = 11.11 // 40 km/h
    private val goodAccuracy = 5.0

    private fun TrackingState.fix(
        metres: Double,
        speed: Double? = null,
        accuracy: Double? = goodAccuracy,
        millis: Long = 1_000
    ) = onFix(metres, speed, accuracy, millis)

    @Test
    fun `starts empty`() {
        val state = TrackingState()
        assertEquals(2, state.trips.size)
        assertEquals(0.0, state.currentSpeedMps, 0.0)
        state.trips.forEach { assertEquals(Trip(), it) }
    }

    @Test
    fun `a moving fix adds distance to every trip`() {
        val state = TrackingState()
        state.fix(11.1, movingMps)
        state.fix(11.1, movingMps)
        state.trips.forEach { assertEquals(22.2, it.distanceMetres, 1e-9) }
    }

    /** A parked GPS still jitters; without the movement gate it would clock up distance. */
    @Test
    fun `a stationary fix adds no distance even when the position jumps`() {
        val state = TrackingState()
        state.fix(metres = 4.0, speed = 0.0)
        state.trips.forEach { assertEquals(0.0, it.distanceMetres, 0.0) }
        assertEquals(0.0, state.currentSpeedMps, 0.0)
    }

    // Displacements here match the speed being reported, or the jump check fires instead.
    @Test
    fun `speed exactly at the threshold counts as moving`() {
        val state = TrackingState()
        state.fix(MOVING_THRESHOLD_MPS, MOVING_THRESHOLD_MPS)
        state.trips.forEach { assertEquals(MOVING_THRESHOLD_MPS, it.distanceMetres, 1e-9) }
    }

    @Test
    fun `speed just below the threshold does not`() {
        val state = TrackingState()
        state.fix(0.49, MOVING_THRESHOLD_MPS - 0.01)
        state.trips.forEach { assertEquals(0.0, it.distanceMetres, 0.0) }
    }

    // --- fix quality ------------------------------------------------------------------

    /** Under trees or in a cutting a receiver emits fixes tens of metres out. */
    @Test
    fun `a fix with poor accuracy is ignored entirely`() {
        val state = TrackingState()
        val advance = state.fix(
            metres = 500.0,
            speed = movingMps,
            accuracy = MAX_FIX_ACCURACY_METRES + 1
        )
        assertFalse("a rejected fix must not move the anchor", advance)
        state.trips.forEach { assertEquals(0.0, it.distanceMetres, 0.0) }
        assertEquals("a rejected fix must not set the speed", 0.0, state.currentSpeedMps, 0.0)
    }

    @Test
    fun `a fix exactly at the accuracy limit is still trusted`() {
        val state = TrackingState()
        val advance = state.fix(10.0, movingMps, accuracy = MAX_FIX_ACCURACY_METRES)
        assertTrue(advance)
        state.trips.forEach { assertEquals(10.0, it.distanceMetres, 1e-9) }
    }

    @Test
    fun `a fix with unknown accuracy is trusted`() {
        val state = TrackingState()
        assertTrue(state.fix(10.0, movingMps, accuracy = null))
        state.trips.forEach { assertEquals(10.0, it.distanceMetres, 1e-9) }
    }

    /**
     * The anchor is only advanced on an accepted, moving fix. Holding it means ground
     * covered below the threshold is added once real movement resumes, rather than lost —
     * which is what stops a slow crawl along a farm track from under-reading.
     */
    @Test
    fun `the anchor is held while stationary so a crawl is not lost`() {
        val state = TrackingState()
        assertFalse("stationary must not advance the anchor", state.fix(0.5, speed = 0.1))
        assertFalse(state.fix(1.0, speed = 0.2))
        // Caller has kept measuring from the original anchor, so the whole 12 m arrives.
        assertTrue(state.fix(12.0, speed = movingMps))
        state.trips.forEach { assertEquals(12.0, it.distanceMetres, 1e-9) }
    }

    /**
     * A receiver coming back after a tunnel, or bouncing off a building, reports a position
     * far from the last one while still claiming a sane speed. Banking that leap adds
     * distance that was never driven — the thing that matters most to get right here.
     */
    @Test
    fun `a position jump larger than the reported speed allows is not counted`() {
        val state = TrackingState()
        // Claims 11 m/s but has moved 500 m in one second.
        val advance = state.fix(metres = 500.0, speed = movingMps, millis = 1_000)

        assertTrue("the new position should still be adopted", advance)
        state.trips.forEach { assertEquals(0.0, it.distanceMetres, 0.0) }
        assertEquals("the reported speed is still shown", movingMps, state.currentSpeedMps, 1e-9)
    }

    @Test
    fun `ordinary acceleration is not mistaken for a jump`() {
        val state = TrackingState()
        // Reported 10 m/s, actually covered 15 m in the second: brisk, but plausible.
        state.fix(metres = 15.0, speed = 10.0, millis = 1_000)
        state.trips.forEach { assertEquals(15.0, it.distanceMetres, 1e-9) }
    }

    @Test
    fun `jitter while barely moving is not mistaken for a jump`() {
        val state = TrackingState()
        // Reported 0.6 m/s with 4 m of jitter: under the floor, so counted normally.
        state.fix(metres = 4.0, speed = 0.6, millis = 1_000)
        state.trips.forEach { assertEquals(4.0, it.distanceMetres, 1e-9) }
    }

    @Test
    fun `a jump cannot be counted later either, because the anchor moves`() {
        val state = TrackingState()
        state.fix(metres = 500.0, speed = movingMps, millis = 1_000)
        state.fix(metres = 11.1, speed = movingMps, millis = 1_000)
        // Only the honest 11.1 m survives.
        state.trips.forEach { assertEquals(11.1, it.distanceMetres, 1e-9) }
    }

    /** With no reported speed there is nothing to check the displacement against. */
    @Test
    fun `jump detection needs a reported speed`() {
        val state = TrackingState()
        state.fix(metres = 500.0, speed = null, millis = 1_000)
        state.trips.forEach { assertEquals(500.0, it.distanceMetres, 1e-9) }
    }

    // --- speed ------------------------------------------------------------------------

    @Test
    fun `speed is derived from displacement since the anchor when none is reported`() {
        val state = TrackingState()
        state.fix(metres = 20.0, speed = null, millis = 2_000)
        assertEquals(10.0, state.currentSpeedMps, 1e-9)
    }

    @Test
    fun `a reported speed is preferred over the derived one`() {
        val state = TrackingState()
        state.fix(metres = 20.0, speed = 3.0, millis = 2_000)
        assertEquals(3.0, state.currentSpeedMps, 1e-9)
    }

    @Test
    fun `deriving a speed with no elapsed time does not divide by zero`() {
        val state = TrackingState()
        state.fix(metres = 20.0, speed = null, millis = 0)
        assertEquals(0.0, state.currentSpeedMps, 0.0)
    }

    @Test
    fun `maximum speed is the highest trusted reading`() {
        val state = TrackingState()
        state.fix(10.0, 20.0)
        state.fix(10.0, 35.0)
        state.fix(10.0, 12.0)
        state.trips.forEach { assertEquals(35.0, it.maxSpeedMps, 1e-9) }
    }

    @Test
    fun `a rejected fix cannot set a maximum speed`() {
        val state = TrackingState()
        state.fix(10.0, 20.0)
        state.fix(10.0, 99.0, accuracy = MAX_FIX_ACCURACY_METRES + 1)
        state.trips.forEach { assertEquals(20.0, it.maxSpeedMps, 1e-9) }
    }

    // --- ticks ------------------------------------------------------------------------

    @Test
    fun `a tick within the stale window keeps the current speed`() {
        val state = TrackingState()
        state.fix(11.1, movingMps)
        state.onTick(deltaMillis = 1_000)
        assertEquals(movingMps, state.currentSpeedMps, 1e-9)
        state.trips.forEach { assertEquals(1_000L, it.movingMillis) }
    }

    @Test
    fun `the speed survives right up to the stale limit`() {
        val state = TrackingState()
        state.fix(11.1, movingMps)
        repeat(5) { state.onTick(1_000) } // exactly STALE_FIX_MILLIS
        assertEquals(movingMps, state.currentSpeedMps, 1e-9)
    }

    /** Signal loss: the readout must fall to zero rather than sit at the last speed. */
    @Test
    fun `ticking past the stale window zeroes the speed`() {
        val state = TrackingState()
        state.fix(11.1, movingMps)
        repeat(6) { state.onTick(1_000) }
        assertEquals(0.0, state.currentSpeedMps, 0.0)
    }

    @Test
    fun `a trusted fix restarts the stale countdown`() {
        val state = TrackingState()
        state.fix(11.1, movingMps)
        repeat(4) { state.onTick(1_000) }
        state.fix(11.1, movingMps)
        repeat(4) { state.onTick(1_000) }
        assertEquals("a fresh fix should have reset the clock", movingMps, state.currentSpeedMps, 1e-9)
    }

    /**
     * A rejected fix is not a fix. Under heavy tree cover a receiver emits a steady stream
     * of positions it cannot vouch for; counting those as evidence that the signal is alive
     * left the speed readout frozen at the last good reading indefinitely, which is worse
     * than showing nothing because it looks like a working speedo.
     */
    @Test
    fun `untrusted fixes do not hold off the stale timeout`() {
        val state = TrackingState()
        state.fix(11.1, movingMps)
        assertEquals(movingMps, state.currentSpeedMps, 1e-9)

        repeat(6) {
            state.fix(50.0, movingMps, accuracy = MAX_FIX_ACCURACY_METRES + 1)
            state.onTick(1_000)
        }

        assertEquals(0.0, state.currentSpeedMps, 0.0)
    }

    @Test
    fun `time after the signal is lost counts as elapsed but not as moving`() {
        val state = TrackingState()
        state.fix(11.1, movingMps)
        repeat(16) { state.onTick(1_000) }
        state.trips.forEach {
            assertEquals(16_000L, it.elapsedMillis)
            // The five seconds before the fix went stale still count: until then we had
            // every reason to believe the car was moving.
            assertEquals(5_000L, it.movingMillis)
        }
    }

    /** The whole point of the feature, driven through the calls the tracker makes. */
    @Test
    fun `a checkpoint stop halves the overall average but leaves the moving average alone`() {
        val state = TrackingState()

        repeat(30) {
            state.fix(11.11, movingMps)
            state.onTick(1_000)
        }
        val movingBefore = state.trips[0].averageSpeedMps(includeStoppedTime = false)
        assertEquals(11.11, movingBefore, 0.01)

        repeat(30) {
            state.fix(0.0, 0.0)
            state.onTick(1_000)
        }

        assertEquals(11.11 / 2, state.trips[0].averageSpeedMps(includeStoppedTime = true), 0.02)
        assertEquals(movingBefore, state.trips[0].averageSpeedMps(includeStoppedTime = false), 1e-9)
    }

    // --- reset and restore ------------------------------------------------------------

    @Test
    fun `resetting one trip leaves the other untouched`() {
        val state = TrackingState()
        repeat(5) {
            state.fix(11.1, movingMps)
            state.onTick(1_000)
        }
        assertEquals(state.trips[0], state.trips[1])

        state.reset(0)

        assertEquals(Trip(), state.trips[0])
        assertNotEquals(Trip(), state.trips[1])
    }

    /**
     * Switching rally mode has to clear everything, so the numbers can be shown to be gone
     * rather than merely hidden behind a toggle.
     */
    @Test
    fun `resetting everything empties every trip`() {
        val state = TrackingState()
        repeat(5) {
            state.fix(11.1, movingMps)
            state.onTick(1_000)
        }

        state.resetAll()

        state.trips.forEach { assertEquals(Trip(), it) }
    }

    @Test
    fun `resetting everything also clears the maximum speed`() {
        val state = TrackingState()
        state.fix(10.0, 35.0)
        state.resetAll()
        state.trips.forEach { assertEquals(0.0, it.maxSpeedMps, 0.0) }
    }

    @Test
    fun `resetting out of range is ignored`() {
        val state = TrackingState()
        state.fix(11.1, movingMps)
        val before = state.trips
        state.reset(-1)
        state.reset(99)
        assertEquals(before, state.trips)
    }

    @Test
    fun `clearSpeed zeroes the readout without touching the trips`() {
        val state = TrackingState()
        state.fix(11.1, movingMps)
        state.onTick(1_000)
        val trips = state.trips

        state.clearSpeed()

        assertEquals(0.0, state.currentSpeedMps, 0.0)
        assertEquals(trips, state.trips)
    }

    @Test
    fun `restore adopts saved trips`() {
        val state = TrackingState()
        val saved = listOf(
            Trip(distanceMetres = 100.0, elapsedMillis = 1_000),
            Trip(distanceMetres = 200.0, elapsedMillis = 2_000)
        )
        state.restore(saved)
        assertEquals(saved, state.trips)
    }

    @Test
    fun `restore ignores a saved list of the wrong size`() {
        val state = TrackingState()
        state.restore(listOf(Trip(distanceMetres = 100.0)))
        state.trips.forEach { assertEquals(Trip(), it) }
    }
}
