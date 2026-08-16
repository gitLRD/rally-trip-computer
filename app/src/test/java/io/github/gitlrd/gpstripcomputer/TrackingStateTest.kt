package io.github.gitlrd.gpstripcomputer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TrackingStateTest {

    private val movingMps = 11.11 // 40 km/h

    @Test
    fun `starts empty`() {
        val state = TrackingState()
        assertEquals(2, state.trips.size)
        assertEquals(0.0, state.currentSpeedMps, 0.0)
        state.trips.forEach { assertEquals(Trip(), it) }
    }

    @Test
    fun `the first fix of a session adds no distance`() {
        val state = TrackingState()
        // No previous location to measure from, so the caller passes zero metres.
        state.onFix(metresSincePreviousFix = 0.0, reportedSpeedMps = movingMps, millisSinceLastFix = 0)
        state.trips.forEach { assertEquals(0.0, it.distanceMetres, 0.0) }
    }

    @Test
    fun `a moving fix adds distance to every trip`() {
        val state = TrackingState()
        state.onFix(11.1, movingMps, 1_000)
        state.onFix(11.1, movingMps, 1_000)
        state.trips.forEach { assertEquals(22.2, it.distanceMetres, 1e-9) }
    }

    /** A parked GPS still jitters; without the movement gate it would clock up distance. */
    @Test
    fun `a stationary fix adds no distance even when the position jumps`() {
        val state = TrackingState()
        state.onFix(metresSincePreviousFix = 4.0, reportedSpeedMps = 0.0, millisSinceLastFix = 1_000)
        state.trips.forEach { assertEquals(0.0, it.distanceMetres, 0.0) }
        assertEquals(0.0, state.currentSpeedMps, 0.0)
    }

    @Test
    fun `speed exactly at the threshold counts as moving`() {
        val state = TrackingState()
        state.onFix(10.0, MOVING_THRESHOLD_MPS, 1_000)
        state.trips.forEach { assertEquals(10.0, it.distanceMetres, 1e-9) }
    }

    @Test
    fun `speed just below the threshold does not`() {
        val state = TrackingState()
        state.onFix(10.0, MOVING_THRESHOLD_MPS - 0.01, 1_000)
        state.trips.forEach { assertEquals(0.0, it.distanceMetres, 0.0) }
    }

    @Test
    fun `speed is derived from displacement when the receiver reports none`() {
        val state = TrackingState()
        // 20 m in 2 s = 10 m/s.
        state.onFix(metresSincePreviousFix = 20.0, reportedSpeedMps = null, millisSinceLastFix = 2_000)
        assertEquals(10.0, state.currentSpeedMps, 1e-9)
        state.trips.forEach { assertEquals(20.0, it.distanceMetres, 1e-9) }
    }

    @Test
    fun `a reported speed is preferred over the derived one`() {
        val state = TrackingState()
        // Displacement implies 10 m/s, but the receiver says 3 m/s. Trust the receiver.
        state.onFix(metresSincePreviousFix = 20.0, reportedSpeedMps = 3.0, millisSinceLastFix = 2_000)
        assertEquals(3.0, state.currentSpeedMps, 1e-9)
    }

    @Test
    fun `deriving a speed with no elapsed time does not divide by zero`() {
        val state = TrackingState()
        state.onFix(metresSincePreviousFix = 20.0, reportedSpeedMps = null, millisSinceLastFix = 0)
        assertEquals(0.0, state.currentSpeedMps, 0.0)
    }

    @Test
    fun `a tick within the stale window keeps the current speed`() {
        val state = TrackingState()
        state.onFix(11.1, movingMps, 1_000)
        state.onTick(deltaMillis = 1_000, millisSinceLastFix = STALE_FIX_MILLIS)
        assertEquals(movingMps, state.currentSpeedMps, 1e-9)
        state.trips.forEach { assertEquals(1_000L, it.movingMillis) }
    }

    /** Signal loss: the readout must fall to zero rather than sit at the last speed. */
    @Test
    fun `a tick past the stale window zeroes the speed`() {
        val state = TrackingState()
        state.onFix(11.1, movingMps, 1_000)
        state.onTick(deltaMillis = 1_000, millisSinceLastFix = STALE_FIX_MILLIS + 1)
        assertEquals(0.0, state.currentSpeedMps, 0.0)
    }

    @Test
    fun `time spent stale counts as elapsed but not as moving`() {
        val state = TrackingState()
        state.onFix(11.1, movingMps, 1_000)
        repeat(10) { state.onTick(deltaMillis = 1_000, millisSinceLastFix = STALE_FIX_MILLIS + 1) }
        state.trips.forEach {
            assertEquals(10_000L, it.elapsedMillis)
            assertEquals(0L, it.movingMillis)
        }
    }

    /** The whole point of the feature, driven through the same calls the tracker makes. */
    @Test
    fun `a checkpoint stop halves the overall average but leaves the moving average alone`() {
        val state = TrackingState()

        // 30 s at 11.11 m/s.
        repeat(30) {
            state.onFix(11.11, movingMps, 1_000)
            state.onTick(1_000, 0)
        }
        val movingBefore = state.trips[0].averageSpeedMps(includeStoppedTime = false)
        assertEquals(11.11, movingBefore, 0.01)

        // 30 s stationary, receiver still reporting but at zero.
        repeat(30) {
            state.onFix(0.0, 0.0, 1_000)
            state.onTick(1_000, 0)
        }

        val overallAfter = state.trips[0].averageSpeedMps(includeStoppedTime = true)
        val movingAfter = state.trips[0].averageSpeedMps(includeStoppedTime = false)

        assertEquals(11.11 / 2, overallAfter, 0.02)
        assertEquals(movingBefore, movingAfter, 1e-9)
    }

    @Test
    fun `distance is unchanged by a stop`() {
        val state = TrackingState()
        repeat(10) { state.onFix(11.1, movingMps, 1_000) }
        val distance = state.trips[0].distanceMetres

        repeat(30) {
            state.onFix(0.0, 0.0, 1_000)
            state.onTick(1_000, 0)
        }
        assertEquals(distance, state.trips[0].distanceMetres, 1e-9)
    }

    @Test
    fun `resetting one trip leaves the other untouched`() {
        val state = TrackingState()
        repeat(5) {
            state.onFix(11.1, movingMps, 1_000)
            state.onTick(1_000, 0)
        }
        assertEquals(state.trips[0], state.trips[1])

        state.reset(0)

        assertEquals(Trip(), state.trips[0])
        assertNotEquals(Trip(), state.trips[1])
    }

    @Test
    fun `resetting out of range is ignored`() {
        val state = TrackingState()
        state.onFix(11.1, movingMps, 1_000)
        val before = state.trips

        state.reset(-1)
        state.reset(99)

        assertEquals(before, state.trips)
    }

    @Test
    fun `a reset trip keeps accumulating afterwards`() {
        val state = TrackingState()
        repeat(5) {
            state.onFix(11.1, movingMps, 1_000)
            state.onTick(1_000, 0)
        }
        state.reset(0)
        state.onFix(11.1, movingMps, 1_000)
        state.onTick(1_000, 0)

        assertEquals(11.1, state.trips[0].distanceMetres, 1e-9)
        assertEquals(1_000L, state.trips[0].elapsedMillis)
    }

    @Test
    fun `clearSpeed zeroes the readout without touching the trips`() {
        val state = TrackingState()
        state.onFix(11.1, movingMps, 1_000)
        state.onTick(1_000, 0)
        val trips = state.trips

        state.clearSpeed()

        assertEquals(0.0, state.currentSpeedMps, 0.0)
        assertEquals(trips, state.trips)
    }
}
