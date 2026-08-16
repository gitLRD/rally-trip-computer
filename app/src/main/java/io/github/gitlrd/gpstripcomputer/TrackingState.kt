package io.github.gitlrd.gpstripcomputer

/** With no fix for this long, assume signal is lost and show zero rather than a stale speed. */
const val STALE_FIX_MILLIS = 5000L

/**
 * Fixes reporting worse horizontal accuracy than this are not trusted to measure distance.
 * Under tree cover, in cuttings and between buildings a receiver will happily emit fixes
 * tens of metres out; on a road rally those turn into distance that was never driven.
 */
const val MAX_FIX_ACCURACY_METRES = 25.0

/**
 * Every decision tracking makes, with no dependency on Android or on a clock — the caller
 * supplies elapsed times. [TripTracker] is the adapter that feeds this from a real
 * [android.location.LocationManager].
 *
 * Distance and time deliberately advance through different paths. Distance advances only
 * on a trusted fix showing genuine movement, because a stationary GPS jitters by several
 * metres a second and would otherwise clock up distance while parked. Time advances on a
 * tick whether or not fixes are arriving, so a stop during which the receiver goes quiet
 * is still visible to the average.
 */
class TrackingState(tripCount: Int = 2) {

    var trips: List<Trip> = List(tripCount) { Trip() }
        private set

    var currentSpeedMps: Double = 0.0
        private set

    /**
     * @param metresSinceAnchor straight-line distance from the last accepted fix, or 0 when
     *   there is not one yet.
     * @param reportedSpeedMps the receiver's own speed, or null if it did not supply one.
     * @param accuracyMetres reported horizontal accuracy, or null if unknown.
     * @param millisSinceLastFix used only to derive a speed when [reportedSpeedMps] is null.
     * @return whether the caller should move its measuring anchor to this fix. When false
     *   the anchor stays put, so ground covered in the meantime is added on a later fix
     *   rather than being lost — which is what keeps a slow crawl from under-reading.
     */
    fun onFix(
        metresSinceAnchor: Double,
        reportedSpeedMps: Double?,
        accuracyMetres: Double?,
        millisSinceLastFix: Long
    ): Boolean {
        if (accuracyMetres != null && accuracyMetres > MAX_FIX_ACCURACY_METRES) return false

        val speed = reportedSpeedMps ?: derivedSpeed(metresSinceAnchor, millisSinceLastFix)
        currentSpeedMps = speed
        trips = trips.map { it.withSpeedSample(speed) }

        if (speed < MOVING_THRESHOLD_MPS) return false

        trips = trips.map { it.plusDistance(metresSinceAnchor) }
        return true
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

    fun restore(saved: List<Trip>) {
        if (saved.size == trips.size) trips = saved
    }

    private fun derivedSpeed(metres: Double, millisSinceLastFix: Long): Double =
        if (millisSinceLastFix <= 0L) 0.0 else metres / (millisSinceLastFix / 1000.0)
}
