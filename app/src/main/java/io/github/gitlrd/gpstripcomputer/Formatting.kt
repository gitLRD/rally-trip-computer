package io.github.gitlrd.gpstripcomputer

import java.util.Locale

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
