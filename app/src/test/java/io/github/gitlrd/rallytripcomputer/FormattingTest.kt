package io.github.gitlrd.rallytripcomputer

import org.junit.Assert.assertEquals
import org.junit.Test

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
}
