package io.github.gitlrd.rallytripcomputer

/**
 * How far the rally clock may be set from the phone's, in either direction.
 *
 * Ten minutes is deliberate. An organiser's clock is out by seconds, or at worst a minute or
 * two, and bounding it means a stuck finger in a dark car cannot turn the readout into
 * fiction. A phone an hour out is a phone to fix, not an offset to dial in.
 */
const val MAX_RALLY_OFFSET_SECONDS = 600

/**
 * The event's official time, as an offset from the phone's clock.
 *
 * A rally is run to the organiser's clock, not to network time, and the two are rarely the
 * same — the one this was written for ran about 20 seconds behind. Everything on a schedule
 * has to be read against the official time, so the app shows that rather than making the
 * navigator do the sum in their head at every time control.
 *
 * The offset is held against the wall clock rather than anchored to a moment of
 * synchronisation, so it stays correct across a reboot and keeps whatever accuracy the phone
 * itself has: if the phone is right and the organiser is 20 seconds slow, −20 s is the truth
 * of it for the whole event.
 *
 * Nothing here touches Android, so the caller supplies the time.
 */
data class RallyClock(val offsetSeconds: Int = 0) {

    /** True once the clock has been set away from the phone's own time. */
    val isOffset: Boolean get() = offsetSeconds != 0

    fun timeAt(nowEpochMillis: Long): Long = nowEpochMillis + offsetSeconds * 1_000L

    /** One press of a nudge button. Clamped, so holding one down cannot run away. */
    fun nudged(bySeconds: Int): RallyClock = of(offsetSeconds + bySeconds)

    /** Back onto the phone's own time. */
    fun zeroed(): RallyClock = RallyClock()

    companion object {
        /**
         * Clamps on the way in as well as on a nudge, so a corrupt or hand-edited stored
         * value cannot get past the range the buttons enforce.
         */
        fun of(offsetSeconds: Int): RallyClock =
            RallyClock(offsetSeconds.coerceIn(-MAX_RALLY_OFFSET_SECONDS, MAX_RALLY_OFFSET_SECONDS))
    }
}
