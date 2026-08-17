package io.github.gitlrd.gpstripcomputer

import android.app.Application
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Folding a device is just a window resize, so the layout follows the width it is given
 * rather than any device query. These render at representative sizes: a Fold's cover
 * screen, and its inner screen.
 *
 * ForcedSize adjusts density so a window larger than the test device still lays out and
 * draws for real — without it the wide cases fall off the emulator screen and nothing
 * counts as displayed.
 */
@RunWith(AndroidJUnit4::class)
class ResponsiveLayoutTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showAt(width: Dp, height: Dp) {
        val application = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as Application
        val viewModel = TripComputerViewModel(application)

        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(width, height))
            ) {
                TripComputerContent(viewModel)
            }
        }
    }

    private fun assertEverythingVisible() {
        compose.onAllNodesWithText("Trip 1", substring = true, ignoreCase = true)[0].assertIsDisplayed()
        compose.onAllNodesWithText("Trip 2", substring = true, ignoreCase = true)[0].assertIsDisplayed()
        compose.onNodeWithText("Current Speed", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun aFoldedCoverScreenShowsEverything() {
        showAt(width = 424.dp, height = 990.dp)
        assertEverythingVisible()
    }

    @Test
    fun anUnfoldedInnerScreenShowsEverything() {
        showAt(width = 940.dp, height = 846.dp)
        assertEverythingVisible()
    }

    @Test
    fun aVeryNarrowSplitWindowStillShowsEverything() {
        showAt(width = 320.dp, height = 600.dp)
        assertEverythingVisible()
    }

    @Test
    fun aPhoneInLandscapeShowsEverything() {
        showAt(width = 891.dp, height = 411.dp)
        assertEverythingVisible()
    }

    @Test
    fun narrowWindowsPutTheSpeedCardBelowTheTrips() {
        showAt(width = 424.dp, height = 990.dp)

        val speed = compose.onNodeWithText("Current Speed", ignoreCase = true).getUnclippedBoundsInRoot()
        val trip1 = compose.onAllNodesWithText("Trip 1", substring = true, ignoreCase = true)[0].getUnclippedBoundsInRoot()
        val trip2 = compose.onAllNodesWithText("Trip 2", substring = true, ignoreCase = true)[0].getUnclippedBoundsInRoot()

        // One column: each trip sits above the next, and the speed card below both.
        assertTrue("trip 2 should be below trip 1", trip2.top > trip1.top)
        assertTrue("speed should be below trip 2", speed.top > trip2.top)
    }

    @Test
    fun wideWindowsPutTheSpeedCardBesideTheTrips() {
        showAt(width = 940.dp, height = 846.dp)

        val speed = compose.onNodeWithText("Current Speed", ignoreCase = true).getUnclippedBoundsInRoot()
        val trip1 = compose.onAllNodesWithText("Trip 1", substring = true, ignoreCase = true)[0].getUnclippedBoundsInRoot()
        val trip2 = compose.onAllNodesWithText("Trip 2", substring = true, ignoreCase = true)[0].getUnclippedBoundsInRoot()

        // Two columns: trips still stack on the left, speed card alongside on the right.
        assertTrue("trip 2 should be below trip 1", trip2.top > trip1.top)
        assertTrue(
            "speed should be right of the trips, got ${speed.left} vs ${trip1.left}",
            speed.left > trip1.left
        )
        assertTrue(
            "speed should start near the top, got ${speed.top}",
            speed.top < trip2.top
        )
    }
}
