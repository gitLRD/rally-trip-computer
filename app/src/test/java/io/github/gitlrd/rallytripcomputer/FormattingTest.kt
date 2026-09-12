package io.github.gitlrd.rallytripcomputer

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import java.util.TimeZone

class FormattingTest {

    @Test
    fun `a stopwatch reads to a tenth of a second`() {
        assertEquals("0:00.0", formatStopwatch(0))
        assertEquals("0:04.3", formatStopwatch(4_312))
        assertEquals("4:31.2", formatStopwatch(271_200))
    }

    @Test
    fun `a stopwatch past an hour grows an hours field`() {
        assertEquals("1:00:00.0", formatStopwatch(3_600_000))
        assertEquals("2:03:04.5", formatStopwatch(7_384_500))
    }

    /** Never show time that has not elapsed yet: truncate rather than round up. */
    @Test
    fun `tenths are truncated, not rounded`() {
        assertEquals("0:00.9", formatStopwatch(999))
        assertEquals("0:59.9", formatStopwatch(59_999))
    }

    @Test
    fun `a negative reading shows as zero`() {
        assertEquals("0:00.0", formatStopwatch(-1))
    }


    @Test
    fun `under a minute`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:07", formatDuration(7_000))
        assertEquals("0:59", formatDuration(59_999))
    }

    @Test
    fun `minutes and seconds`() {
        assertEquals("1:00", formatDuration(60_000))
        assertEquals("12:34", formatDuration(754_000))
        assertEquals("59:59", formatDuration(3_599_000))
    }

    @Test
    fun `hours appear only once needed`() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        // A long rally night.
        assertEquals("5:03:09", formatDuration(18_189_000))
    }

    @Test
    fun `negative durations read as zero rather than going backwards`() {
        assertEquals("0:00", formatDuration(-5_000))
    }

    // --- time of day ---------------------------------------------------------------------

    private val utc = TimeZone.getTimeZone("UTC")
    private val london = TimeZone.getTimeZone("Europe/London")

    /** 2026-01-15 00:00:00 UTC. */
    private val midnight = 1_768_435_200_000L

    @After
    fun restoreLocale() {
        Locale.setDefault(Locale.UK)
    }

    @Test
    fun `the clock reads to the second`() {
        assertEquals("00:00:00", formatTimeOfDay(midnight, utc))
        // 2026-01-15 20:33:40 UTC
        assertEquals("20:33:40", formatTimeOfDay(1_768_509_220_000L, utc))
    }

    /** Roadbooks and time cards are 24-hour, so the clock is too, whatever the phone does. */
    @Test
    fun `afternoon reads as 24-hour, not as a twelve with a suffix`() {
        // 2026-01-15 12:00:00 and 23:59:59 UTC
        assertEquals("12:00:00", formatTimeOfDay(1_768_478_400_000L, utc))
        assertEquals("23:59:59", formatTimeOfDay(1_768_521_599_000L, utc))
    }

    /**
     * An offset that pushes the clock over midnight has to roll the day, not read 24:00:10.
     * A rally that starts at eight in the evening is on the wrong side of midnight for most
     * of its length.
     */
    @Test
    fun `a clock nudged past midnight rolls into the next day`() {
        // 2026-01-15 23:59:50 UTC, put twenty seconds ahead.
        val justBefore = 1_768_521_590_000L
        assertEquals("00:00:10", formatTimeOfDay(RallyClock(offsetSeconds = 20).timeAt(justBefore), utc))
    }

    /** And the other way: a slow rally clock drops the display back into the previous day. */
    @Test
    fun `a clock nudged back past midnight rolls into the previous day`() {
        assertEquals("23:59:40", formatTimeOfDay(RallyClock(offsetSeconds = -20).timeAt(midnight), utc))
    }

    /** The zone is honoured, so a summer rally reads British Summer Time rather than UTC. */
    @Test
    fun `the time is shown in the given zone, daylight saving included`() {
        // 2026-07-01 19:33:40 UTC is 20:33:40 in London.
        assertEquals("20:33:40", formatTimeOfDay(1_782_934_420_000L, london))
    }

    /**
     * Locale.ROOT, so the digits stay ASCII. A phone set to a locale with its own numerals
     * would otherwise render a time nobody can read against a roadbook.
     */
    @Test
    fun `the digits stay ASCII whatever the device locale`() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        assertEquals("20:33:40", formatTimeOfDay(1_768_509_220_000L, utc))
    }

    // --- the clock offset, as the settings drawer shows it --------------------------------

    /** The sign is the whole point: it says which way the rally clock is running. */
    @Test
    fun `an offset is always shown signed`() {
        assertEquals("-20 s", formatClockOffset(-20))
        assertEquals("+20 s", formatClockOffset(20))
        assertEquals("-600 s", formatClockOffset(-MAX_RALLY_OFFSET_SECONDS))
    }

    /** No offset has no direction, so it gets no sign rather than reading "+0 s". */
    @Test
    fun `no offset reads as a plain zero`() {
        assertEquals("0 s", formatClockOffset(0))
    }

    @Test
    fun `the offset digits stay ASCII whatever the device locale`() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        assertEquals("-20 s", formatClockOffset(-20))
    }
}
