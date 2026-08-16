package io.github.gitlrd.gpstripcomputer

/** With no fix for this long, assume signal is lost and show zero rather than a stale speed. */
const val STALE_FIX_MILLIS = 5000L

/**
 * Every decision tracking makes, with no dependency on Android or on a clock — the caller
 * supplies elapsed times. [TripTracker] is the adapter that feeds this from a real
 * [android.location.LocationManager].
 *
 * Distance and time deliberately advance through different paths. Distance advances only
 * on a fix, and only while the receiver reports genuine movement, because a stationary GPS
 * jitters by several metres a second and would otherwise clock up distance while parked.
 * Time advances on a tick whether or not fixes are arriving, so a stop during which the
 * receiver goes quiet is still visible to the average.
 */
class TrackingState(tripCount: Int = 2) {

    var trips: List<Trip> = List(tripCount) { Trip() }
        private set

    var currentSpeedMps: Double = 0.0
        private set

    /**
     * @param metresSincePreviousFix straight-line distance from the previous fix, or 0 for
     *   the first fix of a session.
     * @param reportedSpeedMps the receiver's own speed, or null if it did not supply one.
     * @param millisSinceLastFix used only to derive a speed when [reportedSpeedMps] is null.
     */
    fun onFix(
        metresSincePreviousFix: Double,
        reportedSpeedMps: Double?,
        millisSinceLastFix: Long
    ) {
        currentSpeedMps =
            reportedSpeedMps ?: derivedSpeed(metresSincePreviousFix, millisSinceLastFix)

        if (currentSpeedMps >= MOVING_THRESHOLD_MPS) {
            trips = trips.map { it.plusDistance(metresSincePreviousFix) }
        }
    }

    fun onTick(deltaMillis: Long, millisSinceLastFix: Long) {
        if (millisSinceLastFix > STALE_FIX_MILLIS) currentSpeedMps = 0.0
        trips = trips.map { it.plusTime(deltaMillis, currentSpeedMps) }
    }

    fun reset(index: Int) {
        if (index !in trips.indices) return
        trips = trips.mapIndexed { i, trip -> if (i == index) Trip() else trip }
    }

    fun clearSpeed() {
        currentSpeedMps = 0.0
    }

    private fun derivedSpeed(metres: Double, millisSinceLastFix: Long): Double =
        if (millisSinceLastFix <= 0L) 0.0 else metres / (millisSinceLastFix / 1000.0)
}
