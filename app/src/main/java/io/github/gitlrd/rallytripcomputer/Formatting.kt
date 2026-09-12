package io.github.gitlrd.rallytripcomputer

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Elapsed time as a navigator would read it: m:ss under an hour, h:mm:ss beyond.
 * Locale.ROOT so the digits stay ASCII whatever the device language.
 */
/**
 * A stopwatch reading, to a tenth: m:ss.t under an hour, h:mm:ss.t beyond.
 *
 * Tenths are truncated rather than rounded, so the display never shows time that has not
 * elapsed — a stopwatch reading 4.0 s when 3.96 s have passed is lying in the direction
 * that costs you marks.
 */
fun formatStopwatch(millis: Long): String {
    val total = millis.coerceAtLeast(0)
    val tenths = (total % 1000) / 100
    val totalSeconds = total / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d.%d", hours, minutes, seconds, tenths)
    } else {
        String.format(Locale.ROOT, "%d:%02d.%d", minutes, seconds, tenths)
    }
}

fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/**
 * A time of day, as a rally reads one: 24-hour, to the second.
 *
 * Always 24-hour rather than following the device's own preference, because roadbooks, time
 * cards and marshals' clocks are, and a navigator comparing the two under a maplight should
 * not have to translate. Locale.ROOT keeps the digits ASCII for the same reason.
 *
 * [java.util.Calendar] rather than java.time: minSdk here is 24, where java.time needs core
 * library desugaring, and one formatter is not worth desugaring the JDK for. Calendar still
 * handles the zone and its daylight saving correctly, and is plain JDK, so this stays
 * testable on the JVM.
 */
fun formatTimeOfDay(epochMillis: Long, timeZone: TimeZone = TimeZone.getDefault()): String {
    val calendar = Calendar.getInstance(timeZone, Locale.ROOT).apply { timeInMillis = epochMillis }
    return String.format(
        Locale.ROOT,
        "%02d:%02d:%02d",
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        calendar.get(Calendar.SECOND)
    )
}

/**
 * A clock offset as the settings drawer shows it — always signed, because the sign is the
 * information: "-20 s" says the rally is running behind the phone at a glance. No offset gets
 * a plain zero, since a direction it does not have would only read as noise.
 */
fun formatClockOffset(offsetSeconds: Int): String = when {
    offsetSeconds == 0 -> "0 s"
    else -> String.format(Locale.ROOT, "%+d s", offsetSeconds)
}
