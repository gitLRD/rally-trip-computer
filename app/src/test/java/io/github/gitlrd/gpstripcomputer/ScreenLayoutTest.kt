package io.github.gitlrd.gpstripcomputer

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenLayoutTest {

    @Test
    fun `a phone in portrait stacks`() {
        assertEquals(ScreenLayout.STACKED, screenLayoutFor(widthDp = 411))
    }

    /** The cover screen of a folded device is narrow even though the device is large. */
    @Test
    fun `a folded cover screen stacks`() {
        assertEquals(ScreenLayout.STACKED, screenLayoutFor(widthDp = 424))
    }

    @Test
    fun `an unfolded inner screen goes side by side`() {
        assertEquals(ScreenLayout.SIDE_BY_SIDE, screenLayoutFor(widthDp = 940))
    }

    @Test
    fun `a phone in landscape goes side by side`() {
        assertEquals(ScreenLayout.SIDE_BY_SIDE, screenLayoutFor(widthDp = 891))
    }

    @Test
    fun `the breakpoint itself counts as wide`() {
        assertEquals(ScreenLayout.SIDE_BY_SIDE, screenLayoutFor(WIDE_LAYOUT_MIN_WIDTH_DP))
        assertEquals(ScreenLayout.STACKED, screenLayoutFor(WIDE_LAYOUT_MIN_WIDTH_DP - 1))
    }

    /** A narrow multi-window split should not try to use two columns. */
    @Test
    fun `a narrow split window stacks`() {
        assertEquals(ScreenLayout.STACKED, screenLayoutFor(widthDp = 320))
    }
}
