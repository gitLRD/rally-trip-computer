package com.example.gpstripcomputer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.util.Locale

data class Trip(
    var distance: Float = 0f, // Distance in kilometers
    var totalSpeed: Float = 0f, // Cumulative speed for average calculation
    var speedCount: Int = 0 // Number of speed readings
) {
    val averageSpeed: Float
        get() = if (speedCount > 0) totalSpeed / speedCount else 0f

    fun update(speed: Float, deltaDistance: Float) {
        distance += deltaDistance
        totalSpeed += speed
        speedCount++
    }

}

// Data class for navigation drawer items
data class NavigationDrawerItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val badgeCount: Int? = null
)

enum class SpeedUnit {
    KILOMETERS_PER_HOUR,
    MILES_PER_HOUR;

    fun unitAbbreviation(): String {
        return when (this) {
            KILOMETERS_PER_HOUR -> "km/h"
            MILES_PER_HOUR -> "mph"
        }
    }
}

enum class DistanceUnit {
    KILOMETERS,
    MILES;

    fun unitAbbreviation(): String {
        return when (this) {
            KILOMETERS -> "km"
            MILES -> "mi"
        }
    }
}

class MainActivity : ComponentActivity() {

    // Define a class-level boolean variable for debug mode
    private var isDebugMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeedometerApp()
        }
    }

    private val sharedPrefs by lazy { getSharedPreferences("settings", Context.MODE_PRIVATE) }

    // Function to get the unit preference
    private fun getUnitPreference(): String {
        return sharedPrefs.getString("units", "metric") ?: "metric" // Default to metric
    }

    // Function to set the unit preference
    private fun setUnitPreference(units: String) {
        with(sharedPrefs.edit()) {
            putString("units", units)
            apply()
        }
    }

    private fun convertSpeed(speed: Float, fromUnit: SpeedUnit, toUnit: SpeedUnit): Float {
        return when {
            fromUnit == toUnit -> speed
            fromUnit == SpeedUnit.KILOMETERS_PER_HOUR && toUnit == SpeedUnit.MILES_PER_HOUR -> speed * 0.621371f
            else -> speed * 1.60934f
        }
    }

    private fun convertDistance(distance: Float, fromUnit: DistanceUnit, toUnit: DistanceUnit): Float {
        return when {
            fromUnit == toUnit -> distance
            fromUnit == DistanceUnit.KILOMETERS && toUnit == DistanceUnit.MILES -> distance * 0.621371f
            else -> distance * 1.60934f
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SpeedometerApp() {
        var speed by remember { mutableStateOf(0f) }
        val isAlreadyDark = isSystemInDarkTheme()
        var isDarkTheme by remember { mutableStateOf(isAlreadyDark) }
        val trips = remember { mutableStateListOf(Trip(), Trip()) }
        val context = LocalContext.current
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        var selectedItemIndex by remember { mutableStateOf(0) } // Track selected item

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                startTracking(context, trips) { newSpeed -> speed = newSpeed }
            } else {
                if (isDebugMode) {
                    Log.d("Speedometer", "Permission denied")
                }
            }
        }

        LaunchedEffect(Unit) {
            checkPermissionsAndStartTracking(context, permissionLauncher, trips) { newSpeed ->
                speed = newSpeed
            }
        }

        // Define your drawer items
        val items = listOf(
            NavigationDrawerItem(
                title = "Toggle Theme",
                selectedIcon = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                unselectedIcon = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
            ),
            NavigationDrawerItem(
                title = "Units",
                selectedIcon = Icons.Filled.Settings, // Or a more appropriate icon
                unselectedIcon = Icons.Filled.Settings
            )
        )

        MaterialTheme(colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    ModalDrawerSheet {
                        Spacer(Modifier.height(12.dp))
                        items.forEachIndexed { index, item ->
                            if (item.title == "Toggle Theme") { // Check if it's the first item (Toggle Theme)
                                NavigationDrawerItem(
                                    label = { Text(item.title) },
                                    selected = false, // Theme toggle is not selectable
                                    onClick = {
                                        isDarkTheme = !isDarkTheme
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                            contentDescription = item.title
                                        )
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            } else if (item.title == "Units") {
                                UnitSetting { newUnits ->
                                    setUnitPreference(newUnits)
                                    // Might need to trigger recomposition or recalculate values here
                                }
                            } else {
                                NavigationDrawerItem(
                                    label = { Text(item.title) },
                                    selected = index == selectedItemIndex,
                                    onClick = {
                                        selectedItemIndex = index
                                        scope.launch { drawerState.close() }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = if (index == selectedItemIndex) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title
                                        )
                                    },
                                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                                )
                            }
                        }
                    }
                }
            ) {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text(text = "Speedometer") },
                            navigationIcon = {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu Icon")
                                }
                            }
                        )
                    },
                    content = {
                        Column(modifier = Modifier.fillMaxSize()) {
                            InfoGrid(
                                speed = speed,
                                trips = trips,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )

                            SpeedCard(
                                speed = speed,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(0.5f)
                                    .padding(16.dp)
                            )
                        }
                    }
                )
            }
        }
    }

    @Composable
    fun InfoCard(
        infoHeader: String,
        infoValue: String,
        infoType: String,
        onTripReset: (Int) -> Unit,
        modifier: Modifier = Modifier
    ) {
        Card(modifier = modifier.clickable {
            val tripIndex = infoHeader.substringAfter("Trip ").toIntOrNull() ?: -1
            if (tripIndex >= 0) {
                onTripReset(tripIndex - 1) // Adjust index for 0-based list
            }
        }) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = infoHeader,
                    style = MaterialTheme.typography.labelMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = infoValue,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = infoType,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }

    @Composable
    fun InfoGrid(speed: Float, trips: SnapshotStateList<Trip>, modifier: Modifier = Modifier) {
        val padding = 8.dp
        val totalPadding = padding * 2

        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val maxWidth = maxWidth - totalPadding
            val maxHeight = maxHeight - totalPadding

            val numberOfRows = 2
            val cellHeight = maxHeight / numberOfRows
            val numberOfColumns = trips.size
            val cellWidth = maxWidth / numberOfColumns

            LazyHorizontalGrid(
                rows = GridCells.Fixed(numberOfRows),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cellHeight * numberOfRows)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(padding),
                horizontalArrangement = Arrangement.spacedBy(padding)
            ) {
                items(trips.size * 2, key = { index -> index }) { index ->
                    val tripIndex = index % trips.size
                    val isDistance = index >= trips.size

                    val distanceUnit = if (getUnitPreference() == "metric") DistanceUnit.KILOMETERS else DistanceUnit.MILES
                    val fromDistanceUnit = if (getUnitPreference() == "metric") DistanceUnit.KILOMETERS else DistanceUnit.MILES
                    val toDistanceUnit = if (getUnitPreference() == "metric") DistanceUnit.KILOMETERS else DistanceUnit.MILES

                    InfoCard(
                        infoHeader = "Trip ${tripIndex + 1}",
                        infoValue = if (isDistance) {
                            val convertedDistance = convertDistance(trips[tripIndex].distance, fromDistanceUnit, toDistanceUnit)
                            String.format(Locale.getDefault(), "%.2f ${distanceUnit.unitAbbreviation()}", convertedDistance)
                        } else {
                            val speedUnit = if (getUnitPreference() == "metric") SpeedUnit.KILOMETERS_PER_HOUR else SpeedUnit.MILES_PER_HOUR
                            val convertedSpeed = convertSpeed(trips[tripIndex].averageSpeed, SpeedUnit.KILOMETERS_PER_HOUR, speedUnit) // Convert average speed
                            String.format(Locale.getDefault(), "%.2f ${speedUnit.unitAbbreviation()}", convertedSpeed) // Use converted speed and correct unit
                        },
                        infoType = if (isDistance) {
                            "Distance"
                        } else {
                            "Average Speed"
                        },
                        onTripReset = { tripIndex ->
                            trips[tripIndex] = Trip() // Reset trip data using Trip()
                        },
                        modifier = Modifier
                            .size(cellWidth, cellHeight)
                    )
                }
            }
            }
    }


    @Composable
    fun SpeedCard(speed: Float, modifier: Modifier = Modifier) {
        // Log to see if the SpeedCard is being recomposed
        Log.d("Speedometer", "SpeedCard recomposed with speed: $speed")

        val speedUnit = if (getUnitPreference() == "metric") SpeedUnit.KILOMETERS_PER_HOUR else SpeedUnit.MILES_PER_HOUR
        val convertedSpeed = convertSpeed(speed, SpeedUnit.KILOMETERS_PER_HOUR, speedUnit)

        val speedUnitEnum = when (getUnitPreference()) { // Define speedUnitEnum here
            "metric" -> SpeedUnit.KILOMETERS_PER_HOUR
            "imperial" -> SpeedUnit.MILES_PER_HOUR
            else -> SpeedUnit.KILOMETERS_PER_HOUR // Default to metric
        }

        /*val distanceUnit = if (getUnitPreference() == "metric") DistanceUnit.KILOMETERS else DistanceUnit.MILES
        val fromDistanceUnit = if (getUnitPreference() == "metric") SpeedUnit.KILOMETERS_PER_HOUR else SpeedUnit.MILES_PER_HOUR
        val toDistanceUnit = if (getUnitPreference() == "metric") SpeedUnit.KILOMETERS_PER_HOUR else SpeedUnit.MILES_PER_HOUR
        val convertedDistance = convertDistance(speed, DistanceUnit.KILOMETERS, DistanceUnit.MILES)*/

        Card(
            modifier = modifier,
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Current Speed",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = String.format(Locale.getDefault(), "%.2f ${speedUnitEnum.unitAbbreviation()}", convertedSpeed),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    @Composable
    fun UnitSetting(onUnitChange: (String) -> Unit) {
        var selectedUnit by remember { mutableStateOf(getUnitPreference()) }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Units", style = MaterialTheme.typography.bodyLarge)

            Row {
                RadioButton(
                    selected = selectedUnit == "metric",
                    onClick = {
                        selectedUnit = "metric"
                        onUnitChange(selectedUnit)
                    }
                )
                Text("Metric", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = selectedUnit == "imperial",
                    onClick = {
                        selectedUnit = "imperial"
                        onUnitChange(selectedUnit)
                    }
                )
                Text("Imperial", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    private fun checkPermissionsAndStartTracking(
        context: Context,
        permissionLauncher: ActivityResultLauncher<String>,
        trips: SnapshotStateList<Trip>,
        onSpeedChange: (Float) -> Unit // This should be the proper type
    ) {
        if (isDebugMode) {
            Log.d("Speedometer", "Checking permissions...")
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startTracking(context, trips, onSpeedChange) // Ensure speed is updated in the callback
        }
    }

    private fun startTracking(
        context: Context,
        trips: SnapshotStateList<Trip>,
        onSpeedChange: (Float) -> Unit
    ) {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var previousLocation: Location? = null

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val speed = (location.speed * 3600) / 1000 // Convert from m/s to km/h

                onSpeedChange(speed)  // This will recompose the SpeedCard

                // Update trips if necessary (for distance tracking)
                val lastLocation = previousLocation
                val deltaDistance = if (lastLocation != null) {
                    location.distanceTo(lastLocation) / 1000 // Convert meters to km
                } else {
                    0f
                }

                for (i in trips.indices) {
                    val updatedTrip = trips[i].copy()
                    updatedTrip.update(speed, deltaDistance)
                    trips[i] = updatedTrip
                }

                previousLocation = location
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                0,
                0f,
                locationListener
            )
        }
    }
}
