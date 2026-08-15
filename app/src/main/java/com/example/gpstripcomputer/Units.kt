package com.example.gpstripcomputer

/**
 * Internally the app works in SI units throughout: metres for distance, metres per second
 * for speed. Conversion happens only at the point of display.
 */

const val METRES_PER_KILOMETRE = 1000.0
const val METRES_PER_MILE = 1609.344
const val MPS_TO_KMH = 3.6
const val MPS_TO_MPH = 2.2369362920544

enum class SpeedUnit(val abbreviation: String) {
    KILOMETRES_PER_HOUR("km/h"),
    MILES_PER_HOUR("mph")
}

enum class DistanceUnit(val abbreviation: String) {
    KILOMETRES("km"),
    MILES("mi")
}

enum class UnitSystem(
    val key: String,
    val speedUnit: SpeedUnit,
    val distanceUnit: DistanceUnit
) {
    METRIC("metric", SpeedUnit.KILOMETRES_PER_HOUR, DistanceUnit.KILOMETRES),
    IMPERIAL("imperial", SpeedUnit.MILES_PER_HOUR, DistanceUnit.MILES);

    companion object {
        /** Unrecognised or missing values fall back to metric. */
        fun fromKey(key: String?): UnitSystem =
            entries.firstOrNull { it.key == key } ?: METRIC
    }
}

fun metresTo(metres: Double, unit: DistanceUnit): Double = when (unit) {
    DistanceUnit.KILOMETRES -> metres / METRES_PER_KILOMETRE
    DistanceUnit.MILES -> metres / METRES_PER_MILE
}

fun metresPerSecondTo(metresPerSecond: Double, unit: SpeedUnit): Double = when (unit) {
    SpeedUnit.KILOMETRES_PER_HOUR -> metresPerSecond * MPS_TO_KMH
    SpeedUnit.MILES_PER_HOUR -> metresPerSecond * MPS_TO_MPH
}
