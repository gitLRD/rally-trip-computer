package io.github.gitlrd.rallytripcomputer

import android.Manifest
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * How often the stopwatch readout is repainted while running. Matches the tenth of a second
 * it displays; nothing is repainted at all while every stopwatch is stopped.
 *
 * This is a redraw interval and nothing more — the reading itself comes from the clock, so a
 * late or missed repaint costs a frame rather than any time.
 */
private const val STOPWATCH_REDRAW_MILLIS = 100L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripComputerScreen(viewModel: TripComputerViewModel) {
    var showHelp by remember { mutableStateOf(false) }
    /** The mode the user has asked for but not yet confirmed clearing everything for. */
    var pendingRallyMode by remember { mutableStateOf<RallyMode?>(null) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ApplyScreenBrightness(viewModel.screenBrightness)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted[Manifest.permission.ACCESS_FINE_LOCATION] == true) viewModel.startTracking()
    }

    fun requestPermissionsAndStart() {
        permissionLauncher.launch(
            buildList {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
                // Without this the foreground service still runs, but silently.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }.toTypedArray()
        )
    }

    // Resume only what the user left running; a casual launch starts nothing.
    LaunchedEffect(Unit) {
        if (!viewModel.shouldResumeTracking) return@LaunchedEffect
        if (viewModel.hasLocationPermission()) {
            viewModel.startTracking()
        } else {
            requestPermissionsAndStart()
        }
    }

    MaterialTheme(colorScheme = colorSchemeFor(viewModel.themeMode)) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = drawerState.isOpen,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))

                    SettingRow(label = stringResource(R.string.tracking)) {
                        Switch(
                            checked = viewModel.isTracking,
                            onCheckedChange = { wantsTracking ->
                                when {
                                    !wantsTracking -> viewModel.stopTracking()
                                    viewModel.hasLocationPermission() -> viewModel.startTracking()
                                    else -> requestPermissionsAndStart()
                                }
                            }
                        )
                    }

                    RallyModeSetting(
                        selected = viewModel.rallyMode,
                        onSelect = { pendingRallyMode = it }
                    )

                    UnitSetting(
                        selected = viewModel.unitSystem,
                        onSelect = viewModel::onUnitSystemSelected
                    )

                    // Only meaningful as a property of the average speed, which regularity
                    // mode does not have.
                    if (viewModel.rallyMode.showsAverageSpeed) {
                        SettingRow(
                            label = stringResource(R.string.include_stopped_time),
                            supporting = stringResource(R.string.include_stopped_time_summary)
                        ) {
                            Switch(
                                checked = viewModel.includeStoppedTime,
                                onCheckedChange = viewModel::onIncludeStoppedTimeChanged
                            )
                        }
                    }

                    ThemeSetting(
                        selected = viewModel.themeMode,
                        onSelect = viewModel::onThemeModeSelected
                    )

                    BrightnessSetting(
                        brightness = viewModel.screenBrightness,
                        onChange = viewModel::onScreenBrightnessChanged
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHelp = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.help), modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Help,
                            contentDescription = null,
                            modifier = Modifier.size(ButtonDefaults.IconSize)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, stringResource(R.string.menu))
                            }
                        }
                    )
                }
            ) { paddingValues ->
                TripComputerContent(
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(8.dp)
                )
            }

            if (showHelp) {
                HelpDialog(onDismiss = { showHelp = false })
            }

            pendingRallyMode?.let { mode ->
                ConfirmModeSwitchDialog(
                    onConfirm = {
                        viewModel.onRallyModeSelected(mode)
                        pendingRallyMode = null
                    },
                    onDismiss = { pendingRallyMode = null }
                )
            }
        }
    }
}

/**
 * Rally use is at night with the phone mounted, so the navigator needs the screen dimmer
 * than the system would normally allow without leaving the app.
 */
@Composable
private fun ApplyScreenBrightness(brightness: Float) {
    val view = LocalView.current
    LaunchedEffect(brightness, view) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        window.attributes = window.attributes.apply {
            screenBrightness = if (brightness < 0f) {
                WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            } else {
                brightness.coerceIn(0.05f, 1f)
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    supporting: String? = null,
    control: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        control()
    }
}

@Composable
private fun UnitSetting(selected: UnitSystem, onSelect: (UnitSystem) -> Unit) {
    SettingRow(label = stringResource(R.string.units)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selected == UnitSystem.IMPERIAL,
                onClick = { onSelect(UnitSystem.IMPERIAL) }
            )
            Text(stringResource(R.string.imperial), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            RadioButton(
                selected = selected == UnitSystem.METRIC,
                onClick = { onSelect(UnitSystem.METRIC) }
            )
            Text(stringResource(R.string.metric), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Selecting a mode only proposes it. The switch itself goes through
 * [ConfirmModeSwitchDialog], because it clears both trips and both stopwatches, and losing
 * three hours of an event to a mistap in the dark would be unforgivable. The dialog doubles
 * as the statement that the numbers really are being destroyed rather than hidden.
 */
@Composable
private fun RallyModeSetting(selected: RallyMode, onSelect: (RallyMode) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.rally_mode), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = stringResource(R.string.rally_mode_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            RallyMode.entries.forEach { mode ->
                RadioButton(selected = selected == mode, onClick = { onSelect(mode) })
                Text(
                    text = stringResource(
                        when (mode) {
                            RallyMode.STANDARD -> R.string.rally_mode_standard
                            RallyMode.REGULARITY -> R.string.rally_mode_regularity
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ConfirmModeSwitchDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.switch_mode_title)) },
        text = { Text(stringResource(R.string.switch_mode_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.switch_mode_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun ThemeSetting(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ThemeMode.entries.forEach { mode ->
                RadioButton(selected = selected == mode, onClick = { onSelect(mode) })
                Text(
                    text = stringResource(
                        when (mode) {
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                            ThemeMode.NIGHT -> R.string.theme_night
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BrightnessSetting(brightness: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.brightness),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (brightness < 0f) {
                    stringResource(R.string.brightness_system)
                } else {
                    "${(brightness * 100).toInt()}%"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = if (brightness < 0f) 1f else brightness,
            onValueChange = onChange,
            valueRange = 0.05f..1f
        )
    }
}

/**
 * Picks its arrangement from the width it is actually given, so it follows a fold, a
 * rotation or a multi-window resize without caring which of those happened.
 */
@Composable
internal fun TripComputerContent(viewModel: TripComputerViewModel, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val layout = screenLayoutFor(maxWidth.value.toInt())

        // A running stopwatch is the only thing on this screen that changes without the
        // tracker publishing something, so it drives its own repaint — and only while it is
        // actually running.
        var now by remember { mutableLongStateOf(viewModel.stopwatchNow()) }
        val ticking = viewModel.rallyMode.showsStopwatch && viewModel.anyStopwatchRunning
        LaunchedEffect(ticking) {
            now = viewModel.stopwatchNow()
            while (ticking) {
                delay(STOPWATCH_REDRAW_MILLIS)
                now = viewModel.stopwatchNow()
            }
        }

        val trips: @Composable (Modifier) -> Unit = { tripsModifier ->
            Column(
                modifier = tripsModifier,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                viewModel.trips.forEachIndexed { index, trip ->
                    TripRow(
                        tripNumber = index + 1,
                        trip = trip,
                        unitSystem = viewModel.unitSystem,
                        includeStoppedTime = viewModel.includeStoppedTime,
                        rallyMode = viewModel.rallyMode,
                        onReset = { viewModel.resetTrip(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        stopwatchMillis = viewModel.stopwatchElapsed(index, now),
                        stopwatchRunning = viewModel.stopwatches.getOrNull(index)?.isRunning == true,
                        onStopwatchTap = { viewModel.onStopwatchTapped(index) },
                        onStopwatchHold = { viewModel.onStopwatchHeld(index) }
                    )
                }
            }
        }

        val speed: @Composable (Modifier) -> Unit = { speedModifier ->
            SpeedCard(
                speedMps = viewModel.currentSpeedMps,
                unitSystem = viewModel.unitSystem,
                modifier = speedModifier
            )
        }

        when (layout) {
            ScreenLayout.STACKED -> Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trips(Modifier.fillMaxWidth().weight(2f))
                speed(Modifier.fillMaxWidth().weight(1f))
            }

            ScreenLayout.SIDE_BY_SIDE -> Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                trips(Modifier.fillMaxHeight().weight(1f))
                speed(Modifier.fillMaxHeight().weight(1f))
            }
        }
    }
}

/**
 * Distance sits first because that is what a road-rally navigator reads off against the
 * roadbook; trip time is its secondary line.
 *
 * The second card depends on the rally mode. In [RallyMode.STANDARD] it is average speed,
 * with maximum speed underneath. Under [RallyMode.REGULARITY], where the regulations forbid
 * an average speed computer, it is a stopwatch instead — and maximum speed moves under the
 * distance card so it is not lost with it.
 */
@Composable
internal fun TripRow(
    tripNumber: Int,
    trip: Trip,
    unitSystem: UnitSystem,
    includeStoppedTime: Boolean,
    rallyMode: RallyMode,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    stopwatchMillis: Long = 0L,
    stopwatchRunning: Boolean = false,
    onStopwatchTap: () -> Unit = {},
    onStopwatchHold: () -> Unit = {}
) {
    val header = stringResource(R.string.trip_label, tripNumber)
    val maxSpeed = stringResource(
        R.string.trip_max,
        formatMeasurement(
            metresPerSecondTo(trip.maxSpeedMps, unitSystem.speedUnit),
            unitSystem.speedUnit.abbreviation
        )
    )
    val tripTime = stringResource(R.string.trip_time, formatDuration(trip.elapsedMillis))

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricPanel(
            eyebrow = "$header · ${stringResource(R.string.distance)}",
            value = formatNumber(metresTo(trip.distanceMetres, unitSystem.distanceUnit)),
            unit = unitSystem.distanceUnit.abbreviation,
            footer = if (rallyMode.showsAverageSpeed) tripTime else "$tripTime · $maxSpeed",
            onClick = onReset,
            modifier = Modifier.weight(1f).fillMaxSize()
        )

        if (rallyMode.showsAverageSpeed) {
            MetricPanel(
                eyebrow = "$header · ${stringResource(R.string.average_speed)}",
                value = formatNumber(
                    metresPerSecondTo(
                        trip.averageSpeedMps(includeStoppedTime),
                        unitSystem.speedUnit
                    )
                ),
                unit = unitSystem.speedUnit.abbreviation,
                footer = maxSpeed,
                onClick = onReset,
                modifier = Modifier.weight(1f).fillMaxSize()
            )
        } else {
            MetricPanel(
                eyebrow = "$header · ${stringResource(R.string.stopwatch)}",
                value = formatStopwatch(stopwatchMillis),
                unit = null,
                footer = when {
                    stopwatchRunning -> stringResource(R.string.stopwatch_running)
                    stopwatchMillis > 0L -> stringResource(R.string.stopwatch_stopped)
                    else -> stringResource(R.string.stopwatch_hint)
                },
                onClick = onStopwatchTap,
                onLongClick = onStopwatchHold,
                // A running stopwatch is the one thing on the dashboard whose state you
                // cannot infer from its reading at a glance, so it gets the indicator.
                indicator = if (stopwatchRunning) MaterialTheme.colorScheme.primary else null,
                valueColor = if (stopwatchRunning) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f).fillMaxSize()
            )
        }
    }
}

/**
 * A trip meter's face. Eyebrow at the top, the reading filling the middle, secondary
 * reading closing it off — so the number sits where the eye lands and the labels stay out
 * of its way.
 */
@Composable
private fun MetricPanel(
    eyebrow: String,
    value: String,
    unit: String?,
    footer: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    indicator: Color? = null,
    valueColor: Color? = null
) {
    Panel(modifier = modifier, onClick = onClick, onLongClick = onLongClick) {
        PanelEyebrow(eyebrow, indicator = indicator)
        Spacer(Modifier.weight(1f))
        PanelReadout(
            value = value,
            unit = unit,
            maxSize = 44.sp,
            color = valueColor ?: MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.weight(1f))
        PanelFooter(footer)
    }
}

/**
 * The speedometer. Given the largest numerals on the screen because it is the only reading
 * that has to be caught rather than read — everything else you choose to look at.
 */
@Composable
internal fun SpeedCard(speedMps: Double, unitSystem: UnitSystem, modifier: Modifier = Modifier) {
    val speed = metresPerSecondTo(speedMps, unitSystem.speedUnit)
    // Full scale chosen per unit rather than converted, so the ticks land on round numbers
    // instead of on 96 mph.
    val fullScale = when (unitSystem.speedUnit) {
        SpeedUnit.MILES_PER_HOUR -> 100.0
        SpeedUnit.KILOMETRES_PER_HOUR -> 160.0
    }
    Panel(modifier = modifier) {
        PanelEyebrow(stringResource(R.string.current_speed))
        Spacer(Modifier.weight(1f))
        PanelReadout(
            value = formatNumber(speed),
            unit = unitSystem.speedUnit.abbreviation,
            maxSize = 92.sp
        )
        Spacer(Modifier.weight(1f))
        SpeedScale(speed = speed, fullScale = fullScale)
    }
}

@Composable
private fun HelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.help)) },
        text = { Text(stringResource(R.string.help_body)) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.ok)) }
        }
    )
}

@Composable
private fun formatMeasurement(value: Double, abbreviation: String): String =
    stringResource(R.string.measurement, formatNumber(value), abbreviation)

/**
 * The bare number, with no unit attached. The dashboard sets the two separately so the unit
 * can be given its own, quieter treatment; [formatMeasurement] is still used where the two
 * belong in one run of text, such as the secondary readings and the notification.
 */
private fun formatNumber(value: Double): String =
    String.format(Locale.getDefault(), "%.2f", value)
