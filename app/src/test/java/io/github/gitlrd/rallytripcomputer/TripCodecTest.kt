package io.github.gitlrd.rallytripcomputer

import org.junit.Assert.assertEquals
import org.junit.Test

class TripCodecTest {

    private val trips = listOf(
        Trip(distanceMetres = 12345.678, elapsedMillis = 3_600_000, movingMillis = 3_000_000, maxSpeedMps = 31.2),
        Trip(distanceMetres = 42.0, elapsedMillis = 60_000, movingMillis = 60_000, maxSpeedMps = 8.5)
    )

    @Test
    fun `trips survive a round trip`() {
        assertEquals(trips, decodeTrips(encodeTrips(trips), expectedCount = 2))
    }

    @Test
    fun `empty trips survive a round trip`() {
        val empty = listOf(Trip(), Trip())
        assertEquals(empty, decodeTrips(encodeTrips(empty), expectedCount = 2))
    }

    @Test
    fun `nothing stored decodes to empty trips`() {
        assertEquals(listOf(Trip(), Trip()), decodeTrips(null, expectedCount = 2))
        assertEquals(listOf(Trip(), Trip()), decodeTrips("", expectedCount = 2))
        assertEquals(listOf(Trip(), Trip()), decodeTrips("   ", expectedCount = 2))
    }

    /** A corrupt preference should cost the numbers, not the app. */
    @Test
    fun `malformed data decodes to empty trips rather than throwing`() {
        val expected = listOf(Trip(), Trip())
        assertEquals(expected, decodeTrips("nonsense", expectedCount = 2))
        assertEquals(expected, decodeTrips("1|2|3", expectedCount = 2))
        assertEquals(expected, decodeTrips("1|2|3|4", expectedCount = 2))
        assertEquals(expected, decodeTrips("a|b|c|d;e|f|g|h", expectedCount = 2))
        assertEquals(expected, decodeTrips("1|2|3|4;5|6|7|8;9|1|2|3", expectedCount = 2))
    }

    @Test
    fun `a partly valid record still yields empty trips`() {
        // Second record is broken; the whole thing is discarded rather than half-restored.
        assertEquals(
            listOf(Trip(), Trip()),
            decodeTrips("1.0|2|3|4.0;bad", expectedCount = 2)
        )
    }

    @Test
    fun `the count is respected`() {
        val three = decodeTrips(encodeTrips(trips), expectedCount = 3)
        assertEquals(3, three.size)
        three.forEach { assertEquals(Trip(), it) }
    }
}
