package io.github.gitlrd.gpstripcomputer

/**
 * The same plain delimited format [encodeTrips] uses, and the same policy: anything
 * malformed decodes to empty stopwatches rather than throwing.
 *
 * A stopped stopwatch writes an empty start-time field. That has to be distinguishable from
 * a stopwatch started at elapsedRealtime zero, so the field is empty rather than a sentinel
 * number — zero is a perfectly valid start time on a device that has just booted.
 */
private const val FIELD = '|'
private const val RECORD = ';'

fun encodeStopwatches(stopwatches: List<Stopwatch>): String =
    stopwatches.joinToString(RECORD.toString()) { stopwatch ->
        "${stopwatch.accumulatedMillis}$FIELD${stopwatch.startedAtRealtime ?: ""}"
    }

fun decodeStopwatches(encoded: String?, expectedCount: Int): List<Stopwatch> {
    val empty = List(expectedCount) { Stopwatch() }
    if (encoded.isNullOrBlank()) return empty

    val records = encoded.split(RECORD)
    if (records.size != expectedCount) return empty

    return records.map { record ->
        val fields = record.split(FIELD)
        if (fields.size != 2) return empty
        Stopwatch(
            accumulatedMillis = fields[0].toLongOrNull() ?: return empty,
            startedAtRealtime = if (fields[1].isEmpty()) {
                null
            } else {
                fields[1].toLongOrNull() ?: return empty
            }
        )
    }
}
