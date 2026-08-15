package io.github.gitlrd.gpstripcomputer

/**
 * Speeds at or above this count as "moving" (0.5 m/s is about 1.8 km/h). Below it the
 * reading is treated as GPS jitter while stationary rather than genuine movement.
 */
const val MOVING_THRESHOLD_MPS = 0.5

/**
 * A single trip computer.
 *
 * Average speed is derived from distance over time rather than by averaging the individual
 * GPS speed readings — a sample mean is biased by however often the receiver happens to
 * report, and cannot account for time spent stopped.
 *
 * Distance and time accumulate separately: distance advances when a new fix arrives,
 * time advances on a fixed tick. That keeps a long stop (during which the GPS may send
 * nothing at all) from being invisible to the average.
 */
data class Trip(
    val distanceMetres: Double = 0.0,
    val elapsedMillis: Long = 0L,
    val movingMillis: Long = 0L
) {
    /**
     * @param includeStoppedTime when true, divides by total elapsed time so stops drag the
     *   average down; when false, divides only by time spent above [MOVING_THRESHOLD_MPS].
     */
    fun averageSpeedMps(includeStoppedTime: Boolean): Double {
        val millis = if (includeStoppedTime) elapsedMillis else movingMillis
        if (millis <= 0L) return 0.0
        return distanceMetres / (millis / 1000.0)
    }

    fun plusDistance(metres: Double): Trip =
        if (metres <= 0.0) this else copy(distanceMetres = distanceMetres + metres)

    fun plusTime(millis: Long, speedMps: Double): Trip {
        if (millis <= 0L) return this
        val moving = speedMps >= MOVING_THRESHOLD_MPS
        return copy(
            elapsedMillis = elapsedMillis + millis,
            movingMillis = movingMillis + if (moving) millis else 0L
        )
    }
}
