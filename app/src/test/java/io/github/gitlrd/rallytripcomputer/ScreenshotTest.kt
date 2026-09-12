package io.github.gitlrd.rallytripcomputer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.util.TimeZone

/**
 * Pixel-level regression cover for the readouts, run on the JVM through Robolectric so it
 * lands in the fast build job rather than needing an emulator.
 *
 * Golden images live in src/test/screenshots. Record them with:
 *
 *     ./gradlew recordRoborazziDebug
 *
 * and note that they must be recorded on the same platform CI compares them on, because
 * text rasterisation differs between systems.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    /** A trip mid-rally: 8 km covered, half an hour elapsed, 25 minutes of it moving. */
    private val trip = Trip(
        distanceMetres = 8_046.7,
        elapsedMillis = 1_800_000,
        movingMillis = 1_500_000,
        maxSpeedMps = 26.8
    )

    private fun capture(
        name: String,
        themeMode: ThemeMode = ThemeMode.DARK,
        width: Dp = 411.dp,
        height: Dp = 891.dp,
        content: @Composable () -> Unit
    ) {
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(width, height))
            ) {
                MaterialTheme(colorScheme = colorSchemeFor(themeMode)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Box(Modifier.padding(8.dp)) { content() }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", screenshotOptions())
    }

    @Test
    fun tripRowImperialDark() {
        capture("trip_row_imperial_dark") {
            TripRow(
                tripNumber = 1,
                trip = trip,
                unitSystem = UnitSystem.IMPERIAL,
                includeStoppedTime = true,
                rallyMode = RallyMode.STANDARD,
                onReset = {}
            )
        }
    }

    @Test
    fun tripRowMetricLight() {
        capture("trip_row_metric_light", themeMode = ThemeMode.LIGHT) {
            TripRow(
                tripNumber = 1,
                trip = trip,
                unitSystem = UnitSystem.METRIC,
                includeStoppedTime = true,
                rallyMode = RallyMode.STANDARD,
                onReset = {}
            )
        }
    }

    /** The one that matters on a rally night — red on black, and genuinely dark. */
    @Test
    fun tripRowNightMode() {
        capture("trip_row_night", themeMode = ThemeMode.NIGHT) {
            TripRow(
                tripNumber = 1,
                trip = trip,
                unitSystem = UnitSystem.IMPERIAL,
                includeStoppedTime = true,
                rallyMode = RallyMode.STANDARD,
                onReset = {}
            )
        }
    }

    @Test
    fun tripRowMovingAverage() {
        capture("trip_row_moving_average") {
            TripRow(
                tripNumber = 2,
                trip = trip,
                unitSystem = UnitSystem.IMPERIAL,
                includeStoppedTime = false,
                rallyMode = RallyMode.STANDARD,
                onReset = {}
            )
        }
    }

    // --- regularity mode ----------------------------------------------------------------

    /** No average speed anywhere on the row: a stopwatch, and max speed moved to the left. */
    @Test
    fun tripRowRegularityRunning() {
        capture("trip_row_regularity_running") {
            TripRow(
                tripNumber = 1,
                trip = trip,
                unitSystem = UnitSystem.IMPERIAL,
                includeStoppedTime = true,
                rallyMode = RallyMode.REGULARITY,
                onReset = {},
                stopwatchMillis = 271_200,
                stopwatchRunning = true
            )
        }
    }

    @Test
    fun tripRowRegularityStopped() {
        capture("trip_row_regularity_stopped") {
            TripRow(
                tripNumber = 2,
                trip = trip,
                unitSystem = UnitSystem.IMPERIAL,
                includeStoppedTime = true,
                rallyMode = RallyMode.REGULARITY,
                onReset = {},
                stopwatchMillis = 271_200,
                stopwatchRunning = false
            )
        }
    }

    /** Fresh into regularity mode: the hint is what tells you how the card works. */
    @Test
    fun tripRowRegularityCleared() {
        capture("trip_row_regularity_cleared") {
            TripRow(
                tripNumber = 1,
                trip = Trip(),
                unitSystem = UnitSystem.IMPERIAL,
                includeStoppedTime = true,
                rallyMode = RallyMode.REGULARITY,
                onReset = {}
            )
        }
    }

    @Test
    fun tripRowRegularityNightMode() {
        capture("trip_row_regularity_night", themeMode = ThemeMode.NIGHT) {
            TripRow(
                tripNumber = 1,
                trip = trip,
                unitSystem = UnitSystem.IMPERIAL,
                includeStoppedTime = true,
                rallyMode = RallyMode.REGULARITY,
                onReset = {},
                stopwatchMillis = 271_200,
                stopwatchRunning = true
            )
        }
    }

    @Test
    fun speedCardImperial() {
        capture("speed_card_imperial") {
            SpeedCard(speedMps = 26.8, unitSystem = UnitSystem.IMPERIAL)
        }
    }

    @Test
    fun speedCardNightMode() {
        capture("speed_card_night", themeMode = ThemeMode.NIGHT) {
            SpeedCard(speedMps = 26.8, unitSystem = UnitSystem.IMPERIAL)
        }
    }

    @Test
    fun emptyTripRow() {
        capture("trip_row_empty") {
            TripRow(
                tripNumber = 1,
                trip = Trip(),
                unitSystem = UnitSystem.IMPERIAL,
                includeStoppedTime = true,
                rallyMode = RallyMode.STANDARD,
                onReset = {}
            )
        }
    }

    // --- the rally clock ----------------------------------------------------------------

    /** 2026-01-15 20:33:40 UTC — a plausible hour to be out on a 12-car. */
    private val someEvening = 1_768_509_220_000L
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")

    /**
     * The offset clock, which is the state it spends a rally in. The indicator dot is the
     * thing to look at: it is the only sign on the dashboard that the clock is not the
     * phone's, so a change that loses it loses the warning.
     */
    @Test
    fun rallyTimeOffsetNight() {
        capture("rally_time_offset_night", themeMode = ThemeMode.NIGHT) {
            RallyTimePanel(
                clock = RallyClock(offsetSeconds = -20),
                nowEpochMillis = someEvening,
                timeZone = utc
            )
        }
    }

    /** And unset, for the difference between the two to be visible in the goldens. */
    @Test
    fun rallyTimeOnPhoneTimeDark() {
        capture("rally_time_phone_dark") {
            RallyTimePanel(
                clock = RallyClock(),
                nowEpochMillis = someEvening,
                timeZone = utc
            )
        }
    }

    @Test
    fun rallyTimeLight() {
        capture("rally_time_light", themeMode = ThemeMode.LIGHT) {
            RallyTimePanel(
                clock = RallyClock(offsetSeconds = -20),
                nowEpochMillis = someEvening,
                timeZone = utc
            )
        }
    }

    /**
     * The drawer row, whose five buttons have to fit a narrow sheet without the labels
     * wrapping or being clipped — the thing most likely to break on a small screen.
     */
    @Test
    fun rallyTimeSettingNight() {
        capture("rally_time_setting_night", themeMode = ThemeMode.NIGHT, width = 320.dp) {
            RallyTimeSetting(
                clock = RallyClock(offsetSeconds = -20),
                nowEpochMillis = someEvening,
                timeZone = utc,
                onNudge = {},
                onZero = {}
            )
        }
    }
}
