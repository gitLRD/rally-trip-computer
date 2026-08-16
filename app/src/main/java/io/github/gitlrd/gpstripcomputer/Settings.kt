package io.github.gitlrd.gpstripcomputer

import android.content.Context

/** Sentinel for "leave the screen at whatever the system is doing". */
const val BRIGHTNESS_FOLLOW_SYSTEM = -1f

/**
 * Persisted settings and in-progress trips.
 *
 * Everything lives in one preferences file — an earlier version wrote units to "settings"
 * but read them back from "unit_prefs", so a saved unit choice was never seen.
 */
class Settings(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var unitSystem: UnitSystem
        get() = UnitSystem.fromKey(prefs.getString(KEY_UNITS, null))
        set(value) = prefs.edit().putString(KEY_UNITS, value.key).apply()

    /** Default true: a stop at a checkpoint should pull the average down. */
    var includeStoppedTime: Boolean
        get() = prefs.getBoolean(KEY_INCLUDE_STOPPED_TIME, true)
        set(value) = prefs.edit().putBoolean(KEY_INCLUDE_STOPPED_TIME, value).apply()

    /**
     * Whether the user last left tracking running. Opening the app resumes it only if so,
     * rather than silently starting background location every time it is launched.
     */
    var trackingEnabled: Boolean
        get() = prefs.getBoolean(KEY_TRACKING_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_TRACKING_ENABLED, value).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, null))
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value.key).apply()

    /** [BRIGHTNESS_FOLLOW_SYSTEM], or 0..1 once the navigator has set it explicitly. */
    var screenBrightness: Float
        get() = prefs.getFloat(KEY_BRIGHTNESS, BRIGHTNESS_FOLLOW_SYSTEM)
        set(value) = prefs.edit().putFloat(KEY_BRIGHTNESS, value).apply()

    /**
     * Trips survive the process being killed. Written on a tick while tracking, so an
     * event is not lost if Android reclaims the app between stages.
     */
    fun saveTrips(trips: List<Trip>) {
        prefs.edit().putString(KEY_TRIPS, encodeTrips(trips)).apply()
    }

    fun loadTrips(expectedCount: Int): List<Trip> =
        decodeTrips(prefs.getString(KEY_TRIPS, null), expectedCount)

    companion object {
        const val PREFS_NAME = "settings"
        const val KEY_UNITS = "units"
        const val KEY_INCLUDE_STOPPED_TIME = "include_stopped_time"
        const val KEY_TRACKING_ENABLED = "tracking_enabled"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_BRIGHTNESS = "screen_brightness"
        const val KEY_TRIPS = "trips"
    }
}
