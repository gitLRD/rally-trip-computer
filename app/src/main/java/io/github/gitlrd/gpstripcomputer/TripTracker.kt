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
import androidx.compose.runtime.mutableStateListOf
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

/** With no fix for this long, assume we have lost signal and show zero rather than a stale speed. */
private const val STALE_FIX_MILLIS = 5000L

/**
 * Owns the location subscription and the trip state.
 *
 * Distance is only accumulated while the receiver reports genuine movement — a stationary
 * GPS still jitters by several metres a second, which would otherwise clock up phantom
 * distance while parked. Time, by contrast, accumulates on a steady tick regardless of
 * whether fixes are arriving, so a long stop is visible to the average-speed calculation.
 */
class TripTracker(
    private val context: Context,
    private val scope: CoroutineScope
) {
    val trips = mutableStateListOf(Trip(), Trip())

    var currentSpeedMps by mutableStateOf(0.0)
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
            override fun onLocationChanged(location: Location) {
                onFix(location)
            }

            // Required below API 30, where these have no default implementation.
            @Deprecated("Deprecated in Java", ReplaceWith(""))
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            override fun onProviderEnabled(provider: String) = Unit
            override fun onProviderDisabled(provider: String) = Unit
        }
        listener = locationListener

        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,
            FIX_INTERVAL_MILLIS,
            0f,
            locationListener
        )

        tickerJob = scope.launch {
            var lastTick = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(TICK_MILLIS)
                val now = SystemClock.elapsedRealtime()
                val delta = now - lastTick
                lastTick = now

                if (now - lastFixAt > STALE_FIX_MILLIS) currentSpeedMps = 0.0

                for (i in trips.indices) {
                    trips[i] = trips[i].plusTime(delta, currentSpeedMps)
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
        previousLocation = null
        currentSpeedMps = 0.0
        isTracking = false
    }

    fun resetTrip(index: Int) {
        if (index in trips.indices) trips[index] = Trip()
    }

    private fun onFix(location: Location) {
        val previous = previousLocation
        val metres = previous?.distanceTo(location)?.toDouble() ?: 0.0
        val secondsSinceFix = (SystemClock.elapsedRealtime() - lastFixAt) / 1000.0

        // Not every device populates speed; fall back to distance over time.
        val speed = when {
            location.hasSpeed() -> location.speed.toDouble()
            previous != null && secondsSinceFix > 0.0 -> metres / secondsSinceFix
            else -> 0.0
        }

        currentSpeedMps = speed
        lastFixAt = SystemClock.elapsedRealtime()

        if (speed >= MOVING_THRESHOLD_MPS) {
            for (i in trips.indices) {
                trips[i] = trips[i].plusDistance(metres)
            }
        }
        previousLocation = location
    }
}
