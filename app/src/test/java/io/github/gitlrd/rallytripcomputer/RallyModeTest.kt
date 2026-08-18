package io.github.gitlrd.rallytripcomputer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RallyModeTest {

    @Test
    fun `keys round trip`() {
        RallyMode.entries.forEach { mode ->
            assertEquals(mode, RallyMode.fromKey(mode.key))
        }
    }

    /**
     * Standard is the default: a first launch must not silently hide average speed from
     * someone who never asked for regularity rules.
     */
    @Test
    fun `unknown or missing keys fall back to standard`() {
        assertEquals(RallyMode.STANDARD, RallyMode.DEFAULT)
        assertEquals(RallyMode.DEFAULT, RallyMode.fromKey(null))
        assertEquals(RallyMode.DEFAULT, RallyMode.fromKey("navigational"))
    }

    /** The whole point of the mode: no average speed under regularity regulations. */
    @Test
    fun `only standard mode shows average speed`() {
        assertTrue(RallyMode.STANDARD.showsAverageSpeed)
        assertFalse(RallyMode.REGULARITY.showsAverageSpeed)
    }

    @Test
    fun `only regularity mode shows the stopwatch`() {
        assertFalse(RallyMode.STANDARD.showsStopwatch)
        assertTrue(RallyMode.REGULARITY.showsStopwatch)
    }
}
