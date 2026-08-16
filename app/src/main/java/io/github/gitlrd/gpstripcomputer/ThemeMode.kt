package io.github.gitlrd.gpstripcomputer

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val key: String) {
    LIGHT("light"),
    DARK("dark"),

    /**
     * Red on black. 12-car rallies run at night, and a navigator reading a bright screen
     * loses dark adaptation for a good while afterwards — the same reason aircraft and
     * rally instruments have used red lighting for decades. Red light is the least
     * disruptive to scotopic vision.
     */
    NIGHT("night");

    companion object {
        val DEFAULT = DARK

        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

private val NightRed = Color(0xFFE04B4B)
private val NightRedDim = Color(0xFF9A2F2F)
private val NightSurface = Color(0xFF120404)

/**
 * Every surface role is pinned, not just a few. Material 3 picks different roles for a
 * filled card, an elevated card and the tonal elevation overlay, so leaving any of them at
 * the dark-theme default leaves grey panels glowing in the middle of a black screen.
 */
private val NightColorScheme = darkColorScheme(
    primary = NightRed,
    onPrimary = Color.Black,
    secondary = NightRedDim,
    onSecondary = Color.Black,
    tertiary = NightRedDim,
    onTertiary = Color.Black,
    background = Color.Black,
    onBackground = NightRed,
    surface = NightSurface,
    onSurface = NightRed,
    surfaceVariant = NightSurface,
    onSurfaceVariant = NightRedDim,
    surfaceDim = Color.Black,
    surfaceBright = NightSurface,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = NightSurface,
    surfaceContainer = NightSurface,
    surfaceContainerHigh = NightSurface,
    surfaceContainerHighest = NightSurface,
    // Without this the tonal elevation overlay tints raised cards back towards grey.
    surfaceTint = NightSurface,
    inverseSurface = NightRed,
    inverseOnSurface = Color.Black,
    outline = NightRedDim,
    outlineVariant = NightSurface
)

fun colorSchemeFor(mode: ThemeMode): ColorScheme = when (mode) {
    ThemeMode.LIGHT -> lightColorScheme()
    ThemeMode.DARK -> darkColorScheme()
    ThemeMode.NIGHT -> NightColorScheme
}
