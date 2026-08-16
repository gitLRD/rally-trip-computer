package io.github.gitlrd.gpstripcomputer

import android.content.Context

/**
 * Persisted user settings. Everything lives in one preferences file — an earlier version
 * wrote units to "settings" but read them back from "unit_prefs", so the saved value was
 * never seen.
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

    companion object {
        const val PREFS_NAME = "settings"
        const val KEY_UNITS = "units"
        const val KEY_INCLUDE_STOPPED_TIME = "include_stopped_time"
    }
}
