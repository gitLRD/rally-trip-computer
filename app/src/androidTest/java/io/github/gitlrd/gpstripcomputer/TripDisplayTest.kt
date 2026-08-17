package io.github.gitlrd.gpstripcomputer

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the cards actually render. Values assume the device locale uses a full stop as the
 * decimal separator, which is true of the en-US emulators these run on.
 */
@RunWith(AndroidJUnit4::class)
class TripDisplayTest {

    @get:Rule
    val compose = createComposeRule()

    /**
     * 1 km covered, 100 s elapsed, 50 s of it moving, peaking at 25 m/s.
     * Overall average 10 m/s, moving average 20 m/s.
     */
    private val trip = Trip(
        distanceMetres = 1_000.0,
        elapsedMillis = 100_000,
        movingMillis = 50_000,
        maxSpeedMps = 25.0
    )

    private fun showTrip(
        unitSystem: UnitSystem = UnitSystem.METRIC,
        includeStoppedTime: Boolean = true,
        onReset: () -> Unit = {}
    ) {
        compose.setContent {
            TripRow(
                tripNumber = 1,
                trip = trip,
                unitSystem = unitSystem,
                includeStoppedTime = includeStoppedTime,
                rallyMode = RallyMode.STANDARD,
                onReset = onReset
            )
        }
    }

    private fun showRegularityTrip(
        stopwatchMillis: Long = 0L,
        stopwatchRunning: Boolean = false,
        onReset: () -> Unit = {},
        onStopwatchTap: () -> Unit = {},
        onStopwatchHold: () -> Unit = {}
    ) {
        compose.setContent {
            TripRow(
                tripNumber = 1,
                trip = trip,
                unitSystem = UnitSystem.METRIC,
                includeStoppedTime = true,
                rallyMode = RallyMode.REGULARITY,
                onReset = onReset,
                stopwatchMillis = stopwatchMillis,
                stopwatchRunning = stopwatchRunning,
                onStopwatchTap = onStopwatchTap,
                onStopwatchHold = onStopwatchHold
            )
        }
    }

    @Test
    fun showsDistanceAndOverallAverageInMetric() {
        showTrip(UnitSystem.METRIC, includeStoppedTime = true)
        compose.onNodeWithText("1.00 km").assertIsDisplayed()
        compose.onNodeWithText("36.00 km/h").assertIsDisplayed()
    }

    @Test
    fun excludingStoppedTimeShowsTheHigherMovingAverage() {
        showTrip(UnitSystem.METRIC, includeStoppedTime = false)
        compose.onNodeWithText("72.00 km/h").assertIsDisplayed()
        // Distance is unaffected by which average is shown.
        compose.onNodeWithText("1.00 km").assertIsDisplayed()
    }

    @Test
    fun showsImperialUnitsWhenSelected() {
        showTrip(UnitSystem.IMPERIAL, includeStoppedTime = true)
        compose.onNodeWithText("0.62 mi").assertIsDisplayed()
        compose.onNodeWithText("22.37 mph").assertIsDisplayed()
    }

    @Test
    fun showsTripTimeAlongsideDistance() {
        showTrip()
        compose.onNodeWithText("Time 1:40").assertIsDisplayed()
    }

    @Test
    fun showsMaximumSpeedAlongsideTheAverage() {
        showTrip(UnitSystem.METRIC)
        compose.onNodeWithText("Max 90.00 km/h").assertIsDisplayed()
    }

    @Test
    fun maximumSpeedConvertsWithTheUnits() {
        showTrip(UnitSystem.IMPERIAL)
        compose.onNodeWithText("Max 55.92 mph").assertIsDisplayed()
    }

    @Test
    fun aTripRowHasTwoTappableCards() {
        showTrip()
        compose.onAllNodes(hasClickAction()).assertCountEquals(2)
    }

    @Test
    fun tappingTheDistanceCardResetsTheTrip() {
        var resets = 0
        showTrip(onReset = { resets++ })
        compose.onAllNodes(hasClickAction())[0].performClick()
        assertEquals(1, resets)
    }

    @Test
    fun tappingTheAverageCardAlsoResetsTheTrip() {
        var resets = 0
        showTrip(onReset = { resets++ })
        compose.onAllNodes(hasClickAction())[1].performClick()
        assertEquals(1, resets)
    }

    @Test
    fun speedCardConvertsToTheSelectedUnits() {
        compose.setContent { SpeedCard(speedMps = 11.11, unitSystem = UnitSystem.METRIC) }
        compose.onNodeWithText("40.00 km/h").assertIsDisplayed()
    }

    @Test
    fun speedCardShowsImperial() {
        compose.setContent { SpeedCard(speedMps = 11.11, unitSystem = UnitSystem.IMPERIAL) }
        compose.onNodeWithText("24.85 mph").assertIsDisplayed()
    }

    @Test
    fun aFreshTripReadsZero() {
        compose.setContent {
            TripRow(
                tripNumber = 1,
                trip = Trip(),
                unitSystem = UnitSystem.IMPERIAL,
                includeStoppedTime = true,
                rallyMode = RallyMode.STANDARD,
                onReset = {}
            )
        }
        compose.onNodeWithText("0.00 mi").assertIsDisplayed()
        compose.onNodeWithText("0.00 mph").assertIsDisplayed()
        compose.onNodeWithText("Time 0:00").assertIsDisplayed()
    }

    // --- regularity mode ----------------------------------------------------------------

    /**
     * The point of the mode. A regularity's regulations forbid an average speed computer,
     * so no average speed may appear on the row at all.
     */
    @Test
    fun regularityModeShowsNoAverageSpeed() {
        showRegularityTrip()
        compose.onNodeWithText("Average Speed").assertDoesNotExist()
        compose.onNodeWithText("36.00 km/h").assertDoesNotExist()
        compose.onNodeWithText("Stopwatch").assertIsDisplayed()
    }

    @Test
    fun regularityModeKeepsDistanceTimeAndMaximumSpeed() {
        showRegularityTrip()
        compose.onNodeWithText("1.00 km").assertIsDisplayed()
        compose.onNodeWithText("Time 1:40 · Max 90.00 km/h").assertIsDisplayed()
    }

    @Test
    fun theStopwatchReadsToATenth() {
        showRegularityTrip(stopwatchMillis = 271_200, stopwatchRunning = true)
        compose.onNodeWithText("4:31.2").assertIsDisplayed()
        compose.onNodeWithText("Running").assertIsDisplayed()
    }

    @Test
    fun aStoppedStopwatchSaysSo() {
        showRegularityTrip(stopwatchMillis = 271_200, stopwatchRunning = false)
        compose.onNodeWithText("Stopped").assertIsDisplayed()
    }

    @Test
    fun aClearedStopwatchExplainsHowToUseIt() {
        showRegularityTrip()
        compose.onNodeWithText("0:00.0").assertIsDisplayed()
        compose.onNodeWithText("Tap to start · hold to clear").assertIsDisplayed()
    }

    @Test
    fun tappingTheStopwatchStartsItWithoutResettingTheTrip() {
        var taps = 0
        var resets = 0
        showRegularityTrip(onReset = { resets++ }, onStopwatchTap = { taps++ })

        compose.onAllNodes(hasClickAction())[1].performClick()

        assertEquals(1, taps)
        assertEquals("the trip must not be reset by starting a stopwatch", 0, resets)
    }

    @Test
    fun holdingTheStopwatchClearsItRatherThanTogglingIt() {
        var taps = 0
        var holds = 0
        showRegularityTrip(onStopwatchTap = { taps++ }, onStopwatchHold = { holds++ })

        compose.onAllNodes(hasClickAction())[1].performTouchInput { longClick() }

        assertEquals(1, holds)
        assertEquals("a hold must not also register as a tap", 0, taps)
    }

    @Test
    fun theDistanceCardStillResetsTheTripInRegularityMode() {
        var resets = 0
        showRegularityTrip(onReset = { resets++ })
        compose.onAllNodes(hasClickAction())[0].performClick()
        assertEquals(1, resets)
    }
}
