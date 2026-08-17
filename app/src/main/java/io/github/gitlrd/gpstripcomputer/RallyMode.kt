package io.github.gitlrd.gpstripcomputer

/**
 * Which set of rally regulations the app is being used under.
 *
 * A regularity is run to a set average speed, and the regulations for one normally forbid
 * carrying any device that computes average speed — a trip meter and a stopwatch are what
 * you are allowed. So [REGULARITY] does not merely hide the average: switching modes clears
 * every trip and every stopwatch, so the numbers can be shown to be genuinely gone rather
 * than sitting behind a toggle waiting to come back.
 */
enum class RallyMode(val key: String) {

    /** Navigational and touring events: distance, average speed, the lot. */
    STANDARD("standard"),

    /** Regularity: average speed replaced by a stopwatch. */
    REGULARITY("regularity");

    val showsAverageSpeed: Boolean get() = this == STANDARD
    val showsStopwatch: Boolean get() = this == REGULARITY

    companion object {
        val DEFAULT = STANDARD

        fun fromKey(key: String?): RallyMode =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}
