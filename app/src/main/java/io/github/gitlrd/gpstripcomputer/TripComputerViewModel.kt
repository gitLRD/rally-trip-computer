package io.github.gitlrd.gpstripcomputer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

/**
 * Holds everything that must outlive the Activity.
 *
 * Trips used to live in MainActivity, so rotating the device threw away the journey so far
 * and restarted tracking. A ViewModel survives configuration changes, which is the whole
 * reason this class exists.
 *
 * The application context is deliberate: the tracker outlives any single Activity, so
 * holding an Activity context here would leak it.
 */
class TripComputerViewModel(application: Application) : AndroidViewModel(application) {

    private val settings = Settings(application)
    private val tracker = TripTracker(application, viewModelScope)

    val trips: List<Trip> get() = tracker.trips
    val currentSpeedMps: Double get() = tracker.currentSpeedMps
    val isTracking: Boolean get() = tracker.isTracking

    var unitSystem by mutableStateOf(settings.unitSystem)
        private set

    var includeStoppedTime by mutableStateOf(settings.includeStoppedTime)
        private set

    fun onUnitSystemSelected(value: UnitSystem) {
        unitSystem = value
        settings.unitSystem = value
    }

    fun onIncludeStoppedTimeChanged(value: Boolean) {
        includeStoppedTime = value
        settings.includeStoppedTime = value
    }

    fun hasLocationPermission(): Boolean = tracker.hasLocationPermission()

    fun startTracking() = tracker.start()

    fun stopTracking() = tracker.stop()

    fun resetTrip(index: Int) = tracker.resetTrip(index)

    override fun onCleared() {
        tracker.stop()
        super.onCleared()
    }
}
