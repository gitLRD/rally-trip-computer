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
 * A fix whose implied speed exceeds the reported speed by more than this is treated as the
 * receiver having jumped rather than the car having moved — multipath, or re-acquisition
 * after a tunnel. The position is adopted, but the leap is not counted as distance.
 *
 * The floor keeps the check from firing on ordinary jitter while barely moving.
 */
const val JUMP_TOLERANCE_FACTOR = 3.0
const val JUMP_TOLERANCE_FLOOR_MPS = 5.0

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
     * Time since the last fix that was actually trusted, which is deliberately not the same
     * as the time since the last fix. Owning it here rather than taking it from the caller
     * is what makes the distinction impossible to get wrong: a receiver under heavy cover
     * emits a steady stream of positions too inaccurate to use, and treating those as
     * evidence of signal froze the speed readout at the last good value indefinitely.
     */
    private var millisSinceTrustedFix = 0L

    /**
     * @param metresSinceAnchor straight-line distance from the last accepted fix, or 0 when
     *   there is not one yet.
     * @param reportedSpeedMps the receiver's own speed, or null if it did not supply one.
     * @param accuracyMetres reported horizontal accuracy, or null if unknown.
     * @param millisSinceAnchor time since that anchor fix, not since the last fix — the
     *   anchor may be several fixes old, and the implied speed must be measured over the
     *   same interval as the distance.
     * @return whether the caller should move its measuring anchor to this fix. When false
     *   the anchor stays put, so ground covered in the meantime is added on a later fix
     *   rather than being lost — which is what keeps a slow crawl from under-reading.
     */
    fun onFix(
        metresSinceAnchor: Double,
        reportedSpeedMps: Double?,
        accuracyMetres: Double?,
        millisSinceAnchor: Long
    ): Boolean {
        if (accuracyMetres != null && accuracyMetres > MAX_FIX_ACCURACY_METRES) return false

        millisSinceTrustedFix = 0L

        val impliedSpeed = derivedSpeed(metresSinceAnchor, millisSinceAnchor)
        val speed = reportedSpeedMps ?: impliedSpeed

        currentSpeedMps = speed
        trips = trips.map { it.withSpeedSample(speed) }

        val jumped = reportedSpeedMps != null &&
            impliedSpeed > reportedSpeedMps * JUMP_TOLERANCE_FACTOR + JUMP_TOLERANCE_FLOOR_MPS
        // Adopt the new position so later fixes measure from it, but do not bank the leap.
        if (jumped) return true

        if (speed < MOVING_THRESHOLD_MPS) return false

        trips = trips.map { it.plusDistance(metresSinceAnchor) }
        return true
    }

    fun onTick(deltaMillis: Long) {
        millisSinceTrustedFix += deltaMillis
        if (millisSinceTrustedFix > STALE_FIX_MILLIS) currentSpeedMps = 0.0
        trips = trips.map { it.plusTime(deltaMillis, currentSpeedMps) }
    }

    fun reset(index: Int) {
        if (index !in trips.indices) return
        trips = trips.mapIndexed { i, trip -> if (i == index) Trip() else trip }
    }

    /** Every trip back to zero, for the clean slate a change of rally mode demands. */
    fun resetAll() {
        trips = trips.map { Trip() }
    }

    fun clearSpeed() {
        currentSpeedMps = 0.0
        millisSinceTrustedFix = 0L
    }

    fun restore(saved: List<Trip>) {
        if (saved.size == trips.size) trips = saved
    }

    private fun derivedSpeed(metres: Double, millis: Long): Double =
        if (millis <= 0L) 0.0 else metres / (millis / 1000.0)
}
