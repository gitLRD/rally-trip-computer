package io.github.gitlrd.rallytripcomputer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
 * The screenshots on the F-Droid listing.
 *
 * These are generated rather than captured by hand, and they are written straight into
 * `fastlane/metadata/`, which is the directory F-Droid actually publishes from. That makes
 * them ordinary Roborazzi goldens: `verifyRoborazziDebug` compares them against a fresh
 * render on every CI run, so a design change that leaves the store listing showing an older
 * version of the app fails the build instead of going unnoticed. The previous set was
 * captured by hand and sat two redesigns and a rename out of date.
 *
 * `w360dp-h800dp-xxhdpi` gives exactly 1080x2400, a common phone resolution and the same
 * size as the hand-captured set it replaces.
 *
 * The chrome is reproduced here rather than driven through [TripComputerScreen], which
 * cannot render outside an Activity: it holds a permission launcher and a navigation drawer.
 * Only the title bar is duplicated; everything below it is the real [TripRow] and
 * [SpeedCard] the app draws.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xxhdpi")
class StoreScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    /** Numbers a navigator would recognise: a leg of a rally, part way through. */
    private val trip1 = Trip(
        distanceMetres = 38_624.0,
        elapsedMillis = 3_732_000,
        movingMillis = 3_402_000,
        maxSpeedMps = 27.3
    )

    /** The second trip, zeroed at the last junction. */
    private val trip2 = Trip(
        distanceMetres = 4_506.2,
        elapsedMillis = 462_000,
        movingMillis = 448_000,
        maxSpeedMps = 21.9
    )

    private fun listing(
        name: String,
        themeMode: ThemeMode,
        rallyMode: RallyMode = RallyMode.STANDARD,
        stopwatchMillis: Long = 0L,
        stopwatchRunning: Boolean = false
    ) {
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp))
            ) {
                MaterialTheme(colorScheme = colorSchemeFor(themeMode)) {
                    ListingChrome {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().weight(2f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                TripRow(
                                    tripNumber = 1,
                                    trip = trip1,
                                    unitSystem = UnitSystem.IMPERIAL,
                                    includeStoppedTime = true,
                                    rallyMode = rallyMode,
                                    onReset = {},
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    stopwatchMillis = stopwatchMillis,
                                    stopwatchRunning = stopwatchRunning
                                )
                                TripRow(
                                    tripNumber = 2,
                                    trip = trip2,
                                    unitSystem = UnitSystem.IMPERIAL,
                                    includeStoppedTime = true,
                                    rallyMode = rallyMode,
                                    onReset = {},
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    stopwatchMillis = 0L,
                                    stopwatchRunning = false
                                )
                            }
                            SpeedCard(
                                speedMps = 19.2,
                                unitSystem = UnitSystem.IMPERIAL,
                                modifier = Modifier.fillMaxWidth().weight(1f)
                            )
                        }
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage(
            "../fastlane/metadata/android/en-US/images/phoneScreenshots/$name.png",
            screenshotOptions()
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ListingChrome(content: @Composable () -> Unit) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.app_name)) },
                    navigationIcon = {
                        IconButton(onClick = {}) {
                            Icon(Icons.Default.Menu, stringResource(R.string.menu))
                        }
                    }
                )
            }
        ) { paddingValues ->
            Column(modifier = Modifier.padding(paddingValues).padding(8.dp)) { content() }
        }
    }

    /** The app as most people will first see it. */
    @Test
    fun standardDark() = listing("1", ThemeMode.DARK)

    /** Night mode, which is the reason the app exists in the form it does. */
    @Test
    fun night() = listing("2", ThemeMode.NIGHT)

    /** Regularity mode, with a stopwatch running in place of the average speed. */
    @Test
    fun regularity() = listing(
        name = "3",
        themeMode = ThemeMode.DARK,
        rallyMode = RallyMode.REGULARITY,
        stopwatchMillis = 754_300,
        stopwatchRunning = true
    )
}
