package io.github.gitlrd.gpstripcomputer

import org.junit.Assert.assertEquals
import org.junit.Test

class TripTest {

    @Test
    fun `average speed is zero before any time has passed`() {
        assertEquals(0.0, Trip().averageSpeedMps(includeStoppedTime = true), 0.0)
        assertEquals(0.0, Trip().averageSpeedMps(includeStoppedTime = false), 0.0)
    }

    @Test
    fun `average speed is distance over time`() {
        // 100 m in 10 s = 10 m/s
        val trip = Trip(distanceMetres = 100.0, elapsedMillis = 10_000, movingMillis = 10_000)
        assertEquals(10.0, trip.averageSpeedMps(includeStoppedTime = true), 1e-9)
    }

    @Test
    fun `time only counts as moving above the threshold`() {
        val moving = Trip().plusTime(1_000, speedMps = MOVING_THRESHOLD_MPS)
        assertEquals(1_000L, moving.elapsedMillis)
        assertEquals(1_000L, moving.movingMillis)

        val stationary = Trip().plusTime(1_000, speedMps = MOVING_THRESHOLD_MPS - 0.01)
        assertEquals(1_000L, stationary.elapsedMillis)
        assertEquals(0L, stationary.movingMillis)
    }

    /**
     * The bug this feature exists to fix: stopping at a checkpoint used to leave the
     * average untouched. With stopped time included it must fall.
     */
    @Test
    fun `stopping at a checkpoint drags the overall average down but not the moving average`() {
        // 1 km covered in 100 s at 10 m/s, then stationary for 100 s.
        var trip = Trip()
            .plusDistance(1_000.0)
            .plusTime(100_000, speedMps = 10.0)

        val movingAverageBefore = trip.averageSpeedMps(includeStoppedTime = false)
        val overallAverageBefore = trip.averageSpeedMps(includeStoppedTime = true)
        assertEquals(10.0, movingAverageBefore, 1e-9)
        assertEquals(10.0, overallAverageBefore, 1e-9)

        trip = trip.plusTime(100_000, speedMps = 0.0)

        // Overall halves: same 1 km, now over 200 s.
        assertEquals(5.0, trip.averageSpeedMps(includeStoppedTime = true), 1e-9)
        // Moving average is unaffected by the stop.
        assertEquals(10.0, trip.averageSpeedMps(includeStoppedTime = false), 1e-9)
    }

    @Test
    fun `moving average divides by moving time only`() {
        // 500 m covered, 100 s elapsed, of which only 50 s were spent moving.
        val trip = Trip(distanceMetres = 500.0, elapsedMillis = 100_000, movingMillis = 50_000)
        assertEquals(5.0, trip.averageSpeedMps(includeStoppedTime = true), 1e-9)
        assertEquals(10.0, trip.averageSpeedMps(includeStoppedTime = false), 1e-9)
    }

    @Test
    fun `non-positive increments are ignored`() {
        val trip = Trip(distanceMetres = 10.0, elapsedMillis = 1_000)
        assertEquals(trip, trip.plusDistance(0.0))
        assertEquals(trip, trip.plusDistance(-5.0))
        assertEquals(trip, trip.plusTime(0, speedMps = 5.0))
        assertEquals(trip, trip.plusTime(-1_000, speedMps = 5.0))
    }

    @Test
    fun `maximum speed only ever climbs`() {
        var trip = Trip()
        assertEquals(0.0, trip.maxSpeedMps, 0.0)

        trip = trip.withSpeedSample(12.0)
        assertEquals(12.0, trip.maxSpeedMps, 1e-9)

        trip = trip.withSpeedSample(5.0)
        assertEquals("a slower sample must not lower it", 12.0, trip.maxSpeedMps, 1e-9)

        trip = trip.withSpeedSample(30.5)
        assertEquals(30.5, trip.maxSpeedMps, 1e-9)
    }

    @Test
    fun `a reset trip is empty`() {
        assertEquals(0.0, Trip().distanceMetres, 0.0)
        assertEquals(0L, Trip().elapsedMillis)
        assertEquals(0L, Trip().movingMillis)
        assertEquals(0.0, Trip().maxSpeedMps, 0.0)
    }
}
