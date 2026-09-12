package io.github.gitlrd.rallytripcomputer

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone

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
        compose.onNodeWithText("1.00").assertIsDisplayed()
        compose.onNodeWithText("km").assertIsDisplayed()
        compose.onNodeWithText("36.00").assertIsDisplayed()
        compose.onNodeWithText("km/h").assertIsDisplayed()
    }

    @Test
    fun excludingStoppedTimeShowsTheHigherMovingAverage() {
        showTrip(UnitSystem.METRIC, includeStoppedTime = false)
        compose.onNodeWithText("72.00").assertIsDisplayed()
        // Distance is unaffected by which average is shown.
        compose.onNodeWithText("1.00").assertIsDisplayed()
        compose.onNodeWithText("km").assertIsDisplayed()
    }

    @Test
    fun showsImperialUnitsWhenSelected() {
        showTrip(UnitSystem.IMPERIAL, includeStoppedTime = true)
        compose.onNodeWithText("0.62").assertIsDisplayed()
        compose.onNodeWithText("mi").assertIsDisplayed()
        compose.onNodeWithText("22.37").assertIsDisplayed()
        compose.onNodeWithText("mph").assertIsDisplayed()
    }

    @Test
    fun showsTripTimeAlongsideDistance() {
        showTrip()
        compose.onNodeWithText("Time 1:40", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun showsMaximumSpeedAlongsideTheAverage() {
        showTrip(UnitSystem.METRIC)
        compose.onNodeWithText("Max 90.00 km/h", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun maximumSpeedConvertsWithTheUnits() {
        showTrip(UnitSystem.IMPERIAL)
        compose.onNodeWithText("Max 55.92 mph", ignoreCase = true).assertIsDisplayed()
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
        compose.onNodeWithText("40.00").assertIsDisplayed()
    }

    @Test
    fun speedCardShowsImperial() {
        compose.setContent { SpeedCard(speedMps = 11.11, unitSystem = UnitSystem.IMPERIAL) }
        compose.onNodeWithText("24.85").assertIsDisplayed()
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
        // Distance and average speed both read 0.00 on a fresh trip, and the unit is now
        // a separate element, so the two are no longer distinguishable by text alone.
        compose.onAllNodesWithText("0.00").assertCountEquals(2)
        compose.onNodeWithText("mi").assertIsDisplayed()
        compose.onNodeWithText("mph").assertIsDisplayed()
        compose.onNodeWithText("Time 0:00", ignoreCase = true).assertIsDisplayed()
    }

    // --- regularity mode ----------------------------------------------------------------

    /**
     * The point of the mode. A regularity's regulations forbid an average speed computer,
     * so no average speed may appear on the row at all.
     */
    @Test
    fun regularityModeShowsNoAverageSpeed() {
        showRegularityTrip()
        compose.onNodeWithText("Average Speed", substring = true, ignoreCase = true)
            .assertDoesNotExist()
        compose.onNodeWithText("36.00").assertDoesNotExist()
        compose.onNodeWithText("Stopwatch", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun regularityModeKeepsDistanceTimeAndMaximumSpeed() {
        showRegularityTrip()
        compose.onNodeWithText("1.00").assertIsDisplayed()
        compose.onNodeWithText("km").assertIsDisplayed()
        compose.onNodeWithText("Time 1:40 · Max 90.00 km/h", ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun theStopwatchReadsToATenth() {
        showRegularityTrip(stopwatchMillis = 271_200, stopwatchRunning = true)
        compose.onNodeWithText("4:31.2").assertIsDisplayed()
        compose.onNodeWithText("Running", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun aStoppedStopwatchSaysSo() {
        showRegularityTrip(stopwatchMillis = 271_200, stopwatchRunning = false)
        compose.onNodeWithText("Stopped", ignoreCase = true).assertIsDisplayed()
    }

    @Test
    fun aClearedStopwatchExplainsHowToUseIt() {
        showRegularityTrip()
        compose.onNodeWithText("0:00.0").assertIsDisplayed()
        compose.onNodeWithText("Tap to start", ignoreCase = true).assertIsDisplayed()
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

    // --- the rally clock -----------------------------------------------------------------

    /** 2026-01-15 20:33:40 UTC. The zone is passed in so the assertion is not the emulator's. */
    private val someEvening = 1_768_509_220_000L
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    @Test
    fun theRallyTimePanelShowsTheOrganisersTimeRatherThanThePhones() {
        compose.setContent {
            RallyTimePanel(
                clock = RallyClock(offsetSeconds = -20),
                nowEpochMillis = someEvening,
                timeZone = utc
            )
        }
        compose.onNodeWithText("20:33:20").assertIsDisplayed()
        compose.onNodeWithText("20:33:40").assertDoesNotExist()
        compose.onNodeWithText("Rally time", substring = true, ignoreCase = true)
            .assertIsDisplayed()
    }

    @Test
    fun anUnsetRallyClockShowsThePhoneTimeUnchanged() {
        compose.setContent {
            RallyTimePanel(clock = RallyClock(), nowEpochMillis = someEvening, timeZone = utc)
        }
        compose.onNodeWithText("20:33:40").assertIsDisplayed()
    }

    /** The clock is a readout, not a control: a stray tap on it must do nothing. */
    @Test
    fun theRallyTimePanelIsNotTappable() {
        compose.setContent {
            RallyTimePanel(clock = RallyClock(), nowEpochMillis = someEvening, timeZone = utc)
        }
        compose.onAllNodes(hasClickAction()).assertCountEquals(0)
    }

    // --- setting it ----------------------------------------------------------------------

    private fun showRallyTimeSetting(
        clock: RallyClock = RallyClock(),
        onNudge: (Int) -> Unit = {},
        onZero: () -> Unit = {}
    ) {
        compose.setContent {
            RallyTimeSetting(
                clock = clock,
                nowEpochMillis = someEvening,
                timeZone = utc,
                onNudge = onNudge,
                onZero = onZero
            )
        }
    }

    @Test
    fun theNudgeButtonsStepTheOffsetByOneAndTenSeconds() {
        val steps = mutableListOf<Int>()
        showRallyTimeSetting(onNudge = { steps += it })

        compose.onNodeWithText("-10 s").performClick()
        compose.onNodeWithText("-1 s").performClick()
        compose.onNodeWithText("+1 s").performClick()
        compose.onNodeWithText("+10 s").performClick()

        assertEquals(listOf(-10, -1, 1, 10), steps)
    }

    /**
     * Labelled for a screen reader as what it does, not as the "0" printed on it — a bare
     * zero read aloud among four signed offsets says nothing about which one it is.
     */
    @Test
    fun theZeroButtonPutsTheClockBackOnPhoneTime() {
        var zeroed = 0
        showRallyTimeSetting(clock = RallyClock(offsetSeconds = -20), onZero = { zeroed++ })

        compose.onNodeWithContentDescription("Back to phone time").performClick()

        assertEquals(1, zeroed)
    }

    /** Zero sits between the two directions it separates, not off at one end. */
    @Test
    fun theNudgeButtonsRunFromMinusTenToPlusTenAroundZero() {
        showRallyTimeSetting()
        val labels = compose.onAllNodes(hasClickAction())
            .fetchSemanticsNodes()
            .size
        assertEquals("four nudges and a zero", 5, labels)
    }

    /** Set it against a marshal's clock by reading what it will say, not by doing sums. */
    @Test
    fun theSettingShowsWhatTheClockWillRead() {
        showRallyTimeSetting(clock = RallyClock(offsetSeconds = -20))
        compose.onNodeWithText("20:33:20", substring = true).assertIsDisplayed()
        compose.onNodeWithText("-20 s").assertIsDisplayed()
    }

    @Test
    fun theSettingSaysSoWhenTheClockIsStillOnPhoneTime() {
        showRallyTimeSetting(clock = RallyClock())
        compose.onNodeWithText("Phone time", ignoreCase = true).assertIsDisplayed()
    }
}
