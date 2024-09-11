package com.example.gpstripcomputer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import kotlinx.coroutines.launch

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


class MainActivity : ComponentActivity() {

    // Define a class-level boolean variable for debug mode
    private var isDebugMode: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SpeedometerApp()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SpeedometerApp() {
        var speed by remember { mutableStateOf(0f) }
        var isDarkTheme by remember { mutableStateOf(false) }  // New: Track theme state
        val trips = remember { mutableStateListOf(Trip(), Trip()) }
        val context = LocalContext.current

        val drawerState = rememberDrawerState(DrawerValue.Closed) // New: Manage drawer state
        val scope = rememberCoroutineScope() // New: Coroutine scope for drawer

        MaterialTheme(
            colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme() // New: Apply dark/light theme
        ) {
            // Drawer with scaffold wrapping the content
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = drawerState.isOpen,
                drawerContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize() // Ensure it fills the entire drawer
                            .background(MaterialTheme.colorScheme.surface) // Background color
                            .padding(0.dp) // Remove any padding if added previously
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize() // Ensure Column fills the Box
                                .padding(16.dp) // Apply padding within the Column
                        ) {
                            Text(text = "Settings", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(16.dp))

                            // Theme toggle button inside the drawer
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = "Theme Icon"
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isDarkTheme) "Light Theme" else "Dark Theme",
                                    modifier = Modifier.clickable {
                                        isDarkTheme = !isDarkTheme // Toggle theme state
                                    }
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
                                IconButton(onClick = {
                                    scope.launch {
                                        drawerState.open() // Open the drawer when the icon is clicked
                                    }
                                }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu Icon") // Hamburger icon
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
                    InfoCard(
                        infoHeader = "Trip ${tripIndex + 1}",
                        infoValue = if (isDistance) {
                            String.format(Locale.getDefault(), "%.2f km", trips[tripIndex].distance)
                        } else {
                            String.format(
                                Locale.getDefault(),
                                "%.2f km/h",
                                trips[tripIndex].averageSpeed
                            )
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
                    text = String.format(Locale.getDefault(), "%.2f km/h", speed),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
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
