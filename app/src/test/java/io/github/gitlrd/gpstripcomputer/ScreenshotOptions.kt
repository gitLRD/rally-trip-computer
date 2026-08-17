package io.github.gitlrd.gpstripcomputer

import com.dropbox.differ.SimpleImageComparator
import com.github.takahirom.roborazzi.RoborazziOptions

/**
 * Comparison settings shared by the screenshot tests.
 *
 * Goldens are recorded on a developer's machine and verified on CI, and the two rasterise
 * antialiased edges very slightly differently. Measured on the bezel introduced with the
 * instrument-panel design: about 0.03% of pixels differ, and **the largest difference in any
 * channel is 2/255**. That is rounding, not a regression, but an exact comparison fails on it
 * and turns every design change into a red build.
 *
 * The fix is a per-pixel colour tolerance rather than a "how many pixels may differ" budget.
 * The distinction matters: a budget would let a genuinely missing element through if it were
 * small enough — the 7dp running indicator is only about 0.02% of the screen — whereas a
 * colour tolerance catches it, because that pixel goes from a bright accent to the panel
 * behind it, a distance of nearly 1.0.
 *
 * 0.03 leaves generous room above the 0.0078 observed while still failing on any change of
 * roughly 8/255 or more, which is well below anything a person would call a visual change.
 */
internal fun screenshotOptions(): RoborazziOptions = RoborazziOptions(
    compareOptions = RoborazziOptions.CompareOptions(
        imageComparator = SimpleImageComparator(maxDistance = 0.03f)
    )
)
