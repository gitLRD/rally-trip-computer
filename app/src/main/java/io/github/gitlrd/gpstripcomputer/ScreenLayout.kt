package io.github.gitlrd.gpstripcomputer

/**
 * Below this the window is treated as a phone in portrait — a normal handset, or the cover
 * screen of a folded device. At or above it there is room for two columns: an unfolded
 * foldable, a tablet, landscape, or a large multi-window split.
 *
 * 600dp is the conventional Android breakpoint between compact and medium width.
 */
const val WIDE_LAYOUT_MIN_WIDTH_DP = 600

enum class ScreenLayout {
    /** Trips above one another with the speed readout beneath. */
    STACKED,

    /** Trips in a left column, speed readout filling a right column. */
    SIDE_BY_SIDE
}

/**
 * Chosen from the window size rather than the device, so it follows folding, rotation and
 * multi-window resizing without needing to know which of those happened.
 */
fun screenLayoutFor(widthDp: Int): ScreenLayout =
    if (widthDp >= WIDE_LAYOUT_MIN_WIDTH_DP) ScreenLayout.SIDE_BY_SIDE else ScreenLayout.STACKED
