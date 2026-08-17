package io.github.gitlrd.gpstripcomputer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The whole dashboard rather than a single card.
 *
 * [ScreenshotTest] covers the components in isolation, which catches a card regressing but
 * says nothing about how the three of them sit together — the vertical rhythm, whether the
 * numerals dominate as they should, and whether night mode is genuinely dark across the
 * full screen rather than only within one card.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
class DashboardScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    /** Mid-rally: 8 km covered in half an hour, 25 minutes of it moving. */
    private val trip1 = Trip(
        distanceMetres = 8_046.7,
        elapsedMillis = 1_800_000,
        movingMillis = 1_500_000,
        maxSpeedMps = 26.8
    )

    /** The second trip reset at the last junction. */
    private val trip2 = Trip(
        distanceMetres = 1_207.0,
        elapsedMillis = 240_000,
        movingMillis = 220_000,
        maxSpeedMps = 18.3
    )

    private fun dashboard(
        name: String,
        themeMode: ThemeMode,
        rallyMode: RallyMode = RallyMode.STANDARD,
        unitSystem: UnitSystem = UnitSystem.IMPERIAL
    ) {
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(411.dp, 891.dp))
            ) {
                MaterialTheme(colorScheme = colorSchemeFor(themeMode)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TripRow(
                                tripNumber = 1,
                                trip = trip1,
                                unitSystem = unitSystem,
                                includeStoppedTime = true,
                                rallyMode = rallyMode,
                                onReset = {},
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                stopwatchMillis = 271_200,
                                stopwatchRunning = true
                            )
                            TripRow(
                                tripNumber = 2,
                                trip = trip2,
                                unitSystem = unitSystem,
                                includeStoppedTime = true,
                                rallyMode = rallyMode,
                                onReset = {},
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                stopwatchMillis = 0,
                                stopwatchRunning = false
                            )
                            SpeedCard(
                                speedMps = 17.9,
                                unitSystem = unitSystem,
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png", screenshotOptions())
    }

    @Test
    fun dashboardDark() = dashboard("dashboard_dark", ThemeMode.DARK)

    @Test
    fun dashboardLight() = dashboard("dashboard_light", ThemeMode.LIGHT)

    /** The one that matters on a rally night. */
    @Test
    fun dashboardNight() = dashboard("dashboard_night", ThemeMode.NIGHT)

    @Test
    fun dashboardRegularityNight() =
        dashboard("dashboard_regularity_night", ThemeMode.NIGHT, RallyMode.REGULARITY)

    @Test
    fun dashboardRegularityDark() =
        dashboard("dashboard_regularity_dark", ThemeMode.DARK, RallyMode.REGULARITY)

    /**
     * The unfolded phone, where the layout goes side by side. Worth its own golden because
     * the readouts size themselves from the width they are given, so a change that looks
     * right in one column can clip or shrink oddly in two.
     */
    @Test
    fun dashboardUnfolded() {
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(840.dp, 700.dp))
            ) {
                MaterialTheme(colorScheme = colorSchemeFor(ThemeMode.NIGHT)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TripRow(
                                    tripNumber = 1,
                                    trip = trip1,
                                    unitSystem = UnitSystem.IMPERIAL,
                                    includeStoppedTime = true,
                                    rallyMode = RallyMode.STANDARD,
                                    onReset = {},
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                                TripRow(
                                    tripNumber = 2,
                                    trip = trip2,
                                    unitSystem = UnitSystem.IMPERIAL,
                                    includeStoppedTime = true,
                                    rallyMode = RallyMode.STANDARD,
                                    onReset = {},
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                            }
                            SpeedCard(
                                speedMps = 17.9,
                                unitSystem = UnitSystem.IMPERIAL,
                                modifier = Modifier.fillMaxHeight().weight(1f)
                            )
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("src/test/screenshots/dashboard_unfolded.png", screenshotOptions())
    }
}
