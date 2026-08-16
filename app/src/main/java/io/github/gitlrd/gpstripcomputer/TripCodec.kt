package io.github.gitlrd.gpstripcomputer

/**
 * Trips are written to storage on a tick so a rally is not lost if Android reclaims the
 * process mid-event. A plain delimited string keeps that write cheap and needs no
 * serialisation library.
 *
 * Anything malformed decodes to empty trips rather than throwing — a corrupt preference
 * should cost you the numbers, not the app.
 */
private const val FIELD = '|'
private const val RECORD = ';'

fun encodeTrips(trips: List<Trip>): String =
    trips.joinToString(RECORD.toString()) { trip ->
        listOf(
            trip.distanceMetres,
            trip.elapsedMillis,
            trip.movingMillis,
            trip.maxSpeedMps
        ).joinToString(FIELD.toString())
    }

fun decodeTrips(encoded: String?, expectedCount: Int): List<Trip> {
    val empty = List(expectedCount) { Trip() }
    if (encoded.isNullOrBlank()) return empty

    val records = encoded.split(RECORD)
    if (records.size != expectedCount) return empty

    return records.map { record ->
        val fields = record.split(FIELD)
        if (fields.size != 4) return empty
        Trip(
            distanceMetres = fields[0].toDoubleOrNull() ?: return empty,
            elapsedMillis = fields[1].toLongOrNull() ?: return empty,
            movingMillis = fields[2].toLongOrNull() ?: return empty,
            maxSpeedMps = fields[3].toDoubleOrNull() ?: return empty
        )
    }
}
