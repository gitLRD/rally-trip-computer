package io.github.gitlrd.gpstripcomputer

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingTest {

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
