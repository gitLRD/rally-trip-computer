package io.github.gitlrd.gpstripcomputer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel

/**
 * Holds the display settings and exposes the process-wide tracker to the UI.
 *
 * Trips deliberately do not live here. They belong to [TripComputerApplication] so that
 * [TrackingService] can keep them running with no Activity present at all; the ViewModel's
 * own job is to survive configuration changes — rotating, and unfolding a foldable.
 */
class TripComputerViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<TripComputerApplication>()
    private val settings get() = app.settings
    private val tracker get() = app.tracker

    val trips: List<Trip> get() = tracker.trips
    val currentSpeedMps: Double get() = tracker.currentSpeedMps
    val isTracking: Boolean get() = tracker.isTracking

    var unitSystem by mutableStateOf(settings.unitSystem)
        private set

    var includeStoppedTime by mutableStateOf(settings.includeStoppedTime)
        private set

    var themeMode by mutableStateOf(settings.themeMode)
        private set

    var screenBrightness by mutableStateOf(settings.screenBrightness)
        private set

    fun onUnitSystemSelected(value: UnitSystem) {
        unitSystem = value
        settings.unitSystem = value
    }

    fun onIncludeStoppedTimeChanged(value: Boolean) {
        includeStoppedTime = value
        settings.includeStoppedTime = value
    }

    fun onThemeModeSelected(value: ThemeMode) {
        themeMode = value
        settings.themeMode = value
    }

    fun onScreenBrightnessChanged(value: Float) {
        screenBrightness = value
        settings.screenBrightness = value
    }

    fun hasLocationPermission(): Boolean = tracker.hasLocationPermission()

    /** True when the user left tracking running last time, so launching should resume it. */
    val shouldResumeTracking: Boolean get() = settings.trackingEnabled

    /** Tracking runs in the service so it survives the app going to the background. */
    fun startTracking() {
        settings.trackingEnabled = true
        TrackingService.start(app)
    }

    fun stopTracking() {
        settings.trackingEnabled = false
        TrackingService.stop(app)
    }

    fun resetTrip(index: Int) = tracker.resetTrip(index)
}
