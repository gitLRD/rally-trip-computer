package io.github.gitlrd.gpstripcomputer

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `keys round trip`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromKey(mode.key))
        }
    }

    @Test
    fun `unknown or missing keys fall back to the default`() {
        assertEquals(ThemeMode.DARK, ThemeMode.DEFAULT)
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromKey(null))
        assertEquals(ThemeMode.DEFAULT, ThemeMode.fromKey("chartreuse"))
    }

    /** Night mode has to be genuinely dark, or it defeats its own purpose. */
    @Test
    fun `night mode is red on black`() {
        val night = colorSchemeFor(ThemeMode.NIGHT)
        assertEquals(0f, night.background.red, 0.01f)
        assertEquals(0f, night.background.green, 0.01f)
        assertEquals(0f, night.background.blue, 0.01f)

        // Text is dominated by the red channel.
        assertEquals(true, night.onSurface.red > night.onSurface.green * 2)
        assertEquals(true, night.onSurface.red > night.onSurface.blue * 2)
    }
}
