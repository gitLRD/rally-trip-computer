package io.github.gitlrd.rallytripcomputer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

private const val TICK_MILLIS = 1000L

/**
 * How often fixes are asked for. This is a request, not a promise: the receiver delivers at
 * whatever rate its hardware runs at, which on most phones is 1 Hz regardless of what is
 * asked for. [FIX_RATE_TAG] reports what actually arrives, in debug builds.
 *
 * The rate matters for distance because a fix is a point, and the ground between two fixes
 * is measured as the straight line joining them. At 60 mph a 1 Hz fix rate draws 27 m
 * chords across whatever the road really did, so every bend reads slightly short.
 */
private const val FIX_INTERVAL_MILLIS = 1000L

/** `adb logcat -s GpsFixRate` while driving to see the rate the receiver really delivers. */
private const val FIX_RATE_TAG = "GpsFixRate"
private const val FIX_RATE_REPORT_EVERY = 30

/** Trips are written to storage this often while tracking, rather than on every tick. */
private const val SAVE_EVERY_TICKS = 5

/**
 * Drives [TrackingState] from the platform location provider and republishes it as Compose
 * state. All the decisions live in [TrackingState]; this class only supplies real
 * locations and a real clock, so the logic can be tested without either.
 *
 * One of these exists per process, owned by [TripComputerApplication], because tracking has
 * to outlive both the Activity and the ViewModel while the foreground service is running.
 */
class TripTracker(
    private val context: Context,
    private val scope: CoroutineScope,
    private val settings: Settings,
    private val state: TrackingState = TrackingState()
) {
    init {
        state.restore(settings.loadTrips(state.trips.size))
    }

    var trips by mutableStateOf(state.trips)
        private set

    var currentSpeedMps by mutableStateOf(state.currentSpeedMps)
        private set

    var isTracking by mutableStateOf(false)
        private set

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private var listener: LocationListener? = null
    private var tickerJob: Job? = null

    /** The last fix trusted enough to measure from. Held put when a fix is rejected. */
    private var anchor: Location? = null
    private var anchorAt = 0L
    private var ticksSinceSave = 0

    private var fixCount = 0
    private var firstFixAt = 0L

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun start() {
        if (isTracking || !hasLocationPermission()) return

        anchor = null
        anchorAt = SystemClock.elapsedRealtime()
        state.clearSpeed()

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) = onFix(location)

            // Required below API 30, where these have no default implementation.
            @Deprecated("Deprecated in Java", ReplaceWith(""))
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                FIX_INTERVAL_MILLIS,
                0f,
                locationListener
            )
        } catch (revoked: SecurityException) {
            // Permission can be taken away between the check above and this call.
            return
        }
        listener = locationListener

        tickerJob = scope.launch {
            var lastTick = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(TICK_MILLIS)
                val now = SystemClock.elapsedRealtime()
                state.onTick(deltaMillis = now - lastTick)
                lastTick = now
                publish()

                if (++ticksSinceSave >= SAVE_EVERY_TICKS) {
                    ticksSinceSave = 0
                    settings.saveTrips(state.trips)
                }
            }
        }

        isTracking = true
    }

    fun stop() {
        listener?.let(locationManager::removeUpdates)
        listener = null
        tickerJob?.cancel()
        tickerJob = null
        anchor = null
        state.clearSpeed()
        publish()
        settings.saveTrips(state.trips)
        isTracking = false
    }

    fun resetTrip(index: Int) {
        state.reset(index)
        publish()
        settings.saveTrips(state.trips)
    }

    fun resetAllTrips() {
        state.resetAll()
        publish()
        settings.saveTrips(state.trips)
    }

    private fun onFix(location: Location) {
        val now = SystemClock.elapsedRealtime()
        val measuredFrom = anchor

        val advanceAnchor = state.onFix(
            metresSinceAnchor = measuredFrom?.distanceTo(location)?.toDouble() ?: 0.0,
            reportedSpeedMps = if (location.hasSpeed()) location.speed.toDouble() else null,
            accuracyMetres = if (location.hasAccuracy()) location.accuracy.toDouble() else null,
            millisSinceAnchor = now - anchorAt
        )

        if (measuredFrom == null || advanceAnchor) {
            anchor = location
            anchorAt = now
        }
        publish()

        if (BuildConfig.DEBUG) reportFixRate(now)
    }

    /**
     * Reports the rate fixes actually arrive at, which is the thing worth knowing before
     * changing [FIX_INTERVAL_MILLIS]: asking for a faster rate than the receiver supports
     * costs battery for nothing, and most phones run their GNSS engine at a fixed 1 Hz.
     */
    private fun reportFixRate(now: Long) {
        if (fixCount == 0) firstFixAt = now
        fixCount++
        if (fixCount < 2 || fixCount % FIX_RATE_REPORT_EVERY != 0) return

        val meanInterval = (now - firstFixAt) / (fixCount - 1)
        if (meanInterval <= 0L) return
        Log.d(
            FIX_RATE_TAG,
            "$fixCount fixes, mean interval $meanInterval ms " +
                "(${String.format(Locale.ROOT, "%.2f", 1000.0 / meanInterval)} Hz), " +
                "requested every $FIX_INTERVAL_MILLIS ms"
        )
    }

    private fun publish() {
        trips = state.trips
        currentSpeedMps = state.currentSpeedMps
    }
}
