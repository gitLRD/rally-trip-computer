package io.github.gitlrd.gpstripcomputer

import java.util.Locale

/**
 * Elapsed time as a navigator would read it: m:ss under an hour, h:mm:ss beyond.
 * Locale.ROOT so the digits stay ASCII whatever the device language.
 */
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
