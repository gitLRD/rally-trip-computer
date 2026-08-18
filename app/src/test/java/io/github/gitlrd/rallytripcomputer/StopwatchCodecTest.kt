package io.github.gitlrd.rallytripcomputer

import org.junit.Assert.assertEquals
import org.junit.Test

class StopwatchCodecTest {

    private val stopwatches = listOf(
        Stopwatch(accumulatedMillis = 125_400, startedAtRealtime = 998_877),
        Stopwatch(accumulatedMillis = 42)
    )

    @Test
    fun `stopwatches survive a round trip`() {
        assertEquals(stopwatches, decodeStopwatches(encodeStopwatches(stopwatches), expectedCount = 2))
    }

    @Test
    fun `a running stopwatch is still running after a round trip`() {
        val running = listOf(Stopwatch().toggledAt(5_000), Stopwatch())
        val decoded = decodeStopwatches(encodeStopwatches(running), expectedCount = 2)
        assertEquals(5_000L, decoded[0].startedAtRealtime)
        assertEquals(null, decoded[1].startedAtRealtime)
    }

    @Test
    fun `a stopwatch started at zero is not confused with a stopped one`() {
        val atZero = listOf(Stopwatch(startedAtRealtime = 0), Stopwatch())
        val decoded = decodeStopwatches(encodeStopwatches(atZero), expectedCount = 2)
        assertEquals(0L, decoded[0].startedAtRealtime)
        assertEquals(null, decoded[1].startedAtRealtime)
    }

    @Test
    fun `empty stopwatches survive a round trip`() {
        val empty = listOf(Stopwatch(), Stopwatch())
        assertEquals(empty, decodeStopwatches(encodeStopwatches(empty), expectedCount = 2))
    }

    @Test
    fun `nothing stored decodes to empty stopwatches`() {
        val empty = listOf(Stopwatch(), Stopwatch())
        assertEquals(empty, decodeStopwatches(null, expectedCount = 2))
        assertEquals(empty, decodeStopwatches("", expectedCount = 2))
        assertEquals(empty, decodeStopwatches("   ", expectedCount = 2))
    }

    /** A corrupt preference should cost the numbers, not the app. */
    @Test
    fun `malformed data decodes to empty stopwatches rather than throwing`() {
        val empty = listOf(Stopwatch(), Stopwatch())
        assertEquals(empty, decodeStopwatches("nonsense", expectedCount = 2))
        assertEquals(empty, decodeStopwatches("1", expectedCount = 2))
        assertEquals(empty, decodeStopwatches("1|2|3", expectedCount = 2))
        assertEquals(empty, decodeStopwatches("a|b;c|d", expectedCount = 2))
        assertEquals(empty, decodeStopwatches("1|2;3|4;5|6", expectedCount = 2))
    }

    @Test
    fun `the count is respected`() {
        val three = decodeStopwatches(encodeStopwatches(stopwatches), expectedCount = 3)
        assertEquals(3, three.size)
        three.forEach { assertEquals(Stopwatch(), it) }
    }
}
