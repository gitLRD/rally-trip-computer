package io.github.gitlrd.gpstripcomputer

import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The stopwatches, republished as Compose state and persisted on every change.
 *
 * Deliberately thin: every decision about what a stopwatch reads lives in [Stopwatch], which
 * knows nothing of Android and is tested on the JVM. This is only the adapter that supplies
 * a real clock and somewhere to write to.
 *
 * Like the tracker, one of these exists per process and is owned by
 * [TripComputerApplication], so a timing survives the Activity being destroyed — an unfold,
 * a rotation, or Android reclaiming the screen while the phone is in a pocket.
 */
class StopwatchBank(
    private val settings: Settings,
    count: Int = 2,
    private val clock: () -> Long = SystemClock::elapsedRealtime
) {
    var stopwatches by mutableStateOf(
        settings.loadStopwatches(count).map { it.restoredAt(clock()) }
    )
        private set

    /** True while any stopwatch is running, so the UI knows whether it needs to repaint. */
    val anyRunning: Boolean get() = stopwatches.any { it.isRunning }

    fun elapsedAt(index: Int, nowRealtime: Long): Long =
        stopwatches.getOrNull(index)?.elapsedAt(nowRealtime) ?: 0L

    /** Tap. */
    fun toggle(index: Int) = update(index) { it.toggledAt(clock()) }

    /** Hold. */
    fun clear(index: Int) = update(index) { it.cleared() }

    /** Everything back to zero, for a change of rally mode. */
    fun clearAll() {
        stopwatches = stopwatches.map { Stopwatch() }
        settings.saveStopwatches(stopwatches)
    }

    fun now(): Long = clock()

    private fun update(index: Int, change: (Stopwatch) -> Stopwatch) {
        if (index !in stopwatches.indices) return
        stopwatches = stopwatches.mapIndexed { i, stopwatch ->
            if (i == index) change(stopwatch) else stopwatch
        }
        settings.saveStopwatches(stopwatches)
    }
}
