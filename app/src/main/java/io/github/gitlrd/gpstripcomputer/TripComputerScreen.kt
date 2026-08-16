package io.github.gitlrd.gpstripcomputer

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripComputerScreen(viewModel: TripComputerViewModel) {
    val systemInDarkTheme = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemInDarkTheme) }
    LaunchedEffect(systemInDarkTheme) { isDarkTheme = systemInDarkTheme }

    var showHelp by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.startTracking() }

    LaunchedEffect(Unit) {
        if (viewModel.hasLocationPermission()) {
            viewModel.startTracking()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
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
                                    else -> permissionLauncher.launch(
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    )
                                }
                            }
                        )
                    }

                    UnitSetting(
                        selected = viewModel.unitSystem,
                        onSelect = viewModel::onUnitSystemSelected
                    )

                    SettingRow(
                        label = stringResource(R.string.include_stopped_time),
                        supporting = stringResource(R.string.include_stopped_time_summary)
                    ) {
                        Switch(
                            checked = viewModel.includeStoppedTime,
                            onCheckedChange = viewModel::onIncludeStoppedTimeChanged
                        )
                    }

                    SettingRow(label = stringResource(R.string.theme)) {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isDarkTheme = it },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isDarkTheme) {
                                        Icons.Filled.DarkMode
                                    } else {
                                        Icons.Filled.LightMode
                                    },
                                    contentDescription = stringResource(
                                        if (isDarkTheme) R.string.dark_mode else R.string.light_mode
                                    ),
                                    modifier = Modifier.size(ButtonDefaults.IconSize)
                                )
                            }
                        )
                    }

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
                selected = selected == UnitSystem.METRIC,
                onClick = { onSelect(UnitSystem.METRIC) }
            )
            Text(stringResource(R.string.metric), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.width(8.dp))
            RadioButton(
                selected = selected == UnitSystem.IMPERIAL,
                onClick = { onSelect(UnitSystem.IMPERIAL) }
            )
            Text(stringResource(R.string.imperial), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun TripRow(
    tripNumber: Int,
    trip: Trip,
    unitSystem: UnitSystem,
    includeStoppedTime: Boolean,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val header = stringResource(R.string.trip_label, tripNumber)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoCard(
            header = header,
            value = formatMeasurement(
                metresPerSecondTo(trip.averageSpeedMps(includeStoppedTime), unitSystem.speedUnit),
                unitSystem.speedUnit.abbreviation
            ),
            caption = stringResource(R.string.average_speed),
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        )
        InfoCard(
            header = header,
            value = formatMeasurement(
                metresTo(trip.distanceMetres, unitSystem.distanceUnit),
                unitSystem.distanceUnit.abbreviation
            ),
            caption = stringResource(R.string.distance),
            onClick = onReset,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        )
    }
}

@Composable
private fun InfoCard(
    header: String,
    value: String,
    caption: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(header, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(caption, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
internal fun SpeedCard(speedMps: Double, unitSystem: UnitSystem, modifier: Modifier = Modifier) {
    Card(modifier = modifier, elevation = CardDefaults.cardElevation(8.dp)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.current_speed),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatMeasurement(
                    metresPerSecondTo(speedMps, unitSystem.speedUnit),
                    unitSystem.speedUnit.abbreviation
                ),
                style = MaterialTheme.typography.headlineLarge
            )
        }
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
    stringResource(
        R.string.measurement,
        String.format(Locale.getDefault(), "%.2f", value),
        abbreviation
    )

/**
 * Picks its arrangement from the width it is actually given, so it follows a fold, a
 * rotation or a multi-window resize without caring which of those happened.
 */
@Composable
internal fun TripComputerContent(viewModel: TripComputerViewModel, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val layout = screenLayoutFor(maxWidth.value.toInt())

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
                        onReset = { viewModel.resetTrip(index) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
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
