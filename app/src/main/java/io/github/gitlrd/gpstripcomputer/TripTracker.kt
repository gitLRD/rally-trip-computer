package io.github.gitlrd.gpstripcomputer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TICK_MILLIS = 1000L
private const val FIX_INTERVAL_MILLIS = 1000L

/**
 * Drives [TrackingState] from the platform location provider and republishes it as Compose
 * state. All the decisions live in [TrackingState]; this class only supplies real
 * locations and a real clock, so that the logic can be tested without either.
 */
class TripTracker(
    private val context: Context,
    private val scope: CoroutineScope,
    private val state: TrackingState = TrackingState()
) {
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
    private var previousLocation: Location? = null
    private var lastFixAt = 0L

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun start() {
        if (isTracking || !hasLocationPermission()) return

        previousLocation = null
        lastFixAt = SystemClock.elapsedRealtime()

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
                state.onTick(deltaMillis = now - lastTick, millisSinceLastFix = now - lastFixAt)
                lastTick = now
                publish()
            }
        }

        isTracking = true
    }

    fun stop() {
        listener?.let(locationManager::removeUpdates)
        listener = null
        tickerJob?.cancel()
        tickerJob = null
        previousLocation = null
        state.clearSpeed()
        publish()
        isTracking = false
    }

    fun resetTrip(index: Int) {
        state.reset(index)
        publish()
    }

    private fun onFix(location: Location) {
        val previous = previousLocation
        val now = SystemClock.elapsedRealtime()

        state.onFix(
            metresSincePreviousFix = previous?.distanceTo(location)?.toDouble() ?: 0.0,
            reportedSpeedMps = if (location.hasSpeed()) location.speed.toDouble() else null,
            millisSinceLastFix = now - lastFixAt
        )

        lastFixAt = now
        previousLocation = location
        publish()
    }

    private fun publish() {
        trips = state.trips
        currentSpeedMps = state.currentSpeedMps
    }
}
