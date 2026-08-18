package io.github.gitlrd.rallytripcomputer

/**
 * A stopwatch, for use where a regularity's regulations forbid an average speed computer.
 *
 * Elapsed time is derived from the clock rather than tallied up on a tick. A tally loses a
 * millisecond every time a tick is dropped — under memory pressure, with the screen off, or
 * while the process is dead — and drift is precisely what makes a stopwatch useless for
 * timing a regularity. Here a missed redraw costs a redraw and nothing else.
 *
 * The times are [android.os.SystemClock.elapsedRealtime] values, which count from boot and
 * are unaffected by the wall clock being adjusted mid-event. Nothing here touches Android,
 * so the caller supplies them.
 */
data class Stopwatch(
    val accumulatedMillis: Long = 0L,
    /** When the current run began, or null when stopped. */
    val startedAtRealtime: Long? = null
) {
    val isRunning: Boolean get() = startedAtRealtime != null

    fun elapsedAt(nowRealtime: Long): Long {
        val startedAt = startedAtRealtime ?: return accumulatedMillis
        return accumulatedMillis + (nowRealtime - startedAt).coerceAtLeast(0L)
    }

    /** Tap: starts a stopped stopwatch, or stops a running one and banks its time. */
    fun toggledAt(nowRealtime: Long): Stopwatch =
        if (isRunning) {
            Stopwatch(accumulatedMillis = elapsedAt(nowRealtime), startedAtRealtime = null)
        } else {
            copy(startedAtRealtime = nowRealtime)
        }

    /** Hold: back to zero and stopped. */
    fun cleared(): Stopwatch = Stopwatch()

    /**
     * Reconciles a stopwatch loaded from storage with the current clock.
     *
     * A start time in the future means the device has rebooted, since elapsedRealtime
     * restarts from zero. How long it ran across the reboot cannot be known, so the honest
     * answer is to stop and keep only what was banked beforehand — better a reading that is
     * visibly stopped than one that silently counts the hours the phone was off.
     */
    fun restoredAt(nowRealtime: Long): Stopwatch {
        val startedAt = startedAtRealtime ?: return this
        return if (startedAt > nowRealtime) copy(startedAtRealtime = null) else this
    }
}
