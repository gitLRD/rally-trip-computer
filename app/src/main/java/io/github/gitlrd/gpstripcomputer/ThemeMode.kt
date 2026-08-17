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

/**
 * Colour roles, used consistently across all three themes so the dashboard code never has
 * to know which one is active:
 *
 *   background          the panel it is all mounted on
 *   surface*            a single instrument's face
 *   outlineVariant      its bezel — the hairline that makes it an object rather than text
 *   onSurface           the digits
 *   onSurfaceVariant    engraved legends and secondary readings
 *   primary             the one accent, used only for state that matters
 */
private val NightRed = Color(0xFFFF4A3D)
private val NightRedDim = Color(0xFF9A2E2E)
private val NightSurface = Color(0xFF0B0303)

/**
 * The bezel is the reason night mode works at all. With the panel almost black on a black
 * ground there is nothing to see the instrument by, and the screen becomes numbers floating
 * in a void; a dim red hairline costs almost no light and gives every reading an edge.
 */
private val NightBezel = Color(0xFF4A1414)

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
    outlineVariant = NightBezel
)

/**
 * Amber, lifted straight off the goggles in the app icon. An accent has to come from
 * somewhere, and taking it from the app's own mark ties the two together rather than
 * picking a colour at random.
 */
private val GoggleAmber = Color(0xFFF2A93C)

private val DarkColorScheme = darkColorScheme(
    primary = GoggleAmber,
    onPrimary = Color(0xFF241A08),
    secondary = GoggleAmber,
    background = Color(0xFF0A0B0D),
    onBackground = Color(0xFFF3F6F9),
    surface = Color(0xFF15181C),
    onSurface = Color(0xFFF3F6F9),
    surfaceVariant = Color(0xFF15181C),
    onSurfaceVariant = Color(0xFF8B949F),
    surfaceContainerLowest = Color(0xFF0A0B0D),
    surfaceContainerLow = Color(0xFF121519),
    surfaceContainer = Color(0xFF15181C),
    surfaceContainerHigh = Color(0xFF191D22),
    surfaceContainerHighest = Color(0xFF1D2228),
    outline = Color(0xFF565E68),
    outlineVariant = Color(0xFF2E343C)
)

/**
 * Deliberately a cool instrument grey rather than the warm cream that light themes drift
 * towards: this is read in daylight next to a dashboard, not on paper.
 */
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF9C5511),
    onPrimary = Color.White,
    secondary = Color(0xFF9C5511),
    background = Color(0xFFE6E8EA),
    onBackground = Color(0xFF15181B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF15181B),
    surfaceVariant = Color(0xFFFFFFFF),
    onSurfaceVariant = Color(0xFF667079),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F8F9),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF2F4F5),
    surfaceContainerHighest = Color(0xFFEDEFF1),
    outline = Color(0xFF8B939B),
    outlineVariant = Color(0xFFC3C8CE)
)

fun colorSchemeFor(mode: ThemeMode): ColorScheme = when (mode) {
    ThemeMode.LIGHT -> LightColorScheme
    ThemeMode.DARK -> DarkColorScheme
    ThemeMode.NIGHT -> NightColorScheme
}
