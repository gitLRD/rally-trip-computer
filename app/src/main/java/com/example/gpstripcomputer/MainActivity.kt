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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    fun reset() {
        distance = 0f
        totalSpeed = 0f
        speedCount = 0
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

    @Composable
    fun SpeedometerApp() {
        var speed by remember { mutableStateOf(0f) }
        val trips = remember { mutableStateListOf(Trip(), Trip()) }
        val context = LocalContext.current

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted: Boolean ->
                if (isGranted) {
                    startTracking(context, trips) { newSpeed ->
                        speed = newSpeed
                    }
                } else {
                    // Use isDebugMode to decide whether to log or display messages
                    if (isDebugMode) {
                        Log.d("Speedometer", "Permission denied")
                    }
                    Toast.makeText(context, "Location permission required for speedometer", Toast.LENGTH_SHORT).show()
                }
            }
        )

        LaunchedEffect(Unit) {
            checkPermissionsAndStartTracking(context, permissionLauncher, trips)

            //Update speed when trips are updated
            startTracking(context, trips) { newSpeed ->
                speed = newSpeed
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(    // Wrap grid in a box to fill entire space
                modifier = Modifier.fillMaxSize()
            ) {
                InfoGrid(speed, trips) // Grid should now fill the screen vertically
            }
        }
    }

    @Composable
    fun InfoGrid(speed: Float, trips: List<Trip>) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween // Ensure the speedometer stays at the bottom
        ) {
            // Trip information grid at the top
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // This will allow the grid to take the needed space but not push the speedometer off the screen
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(trips.size * 2) { index ->
                    val tripIndex = index / 2
                    val isDistance = index % 2 == 0
                    if (isDistance) {
                        InfoCard(info = "Trip ${tripIndex + 1}: ${String.format(Locale.getDefault(), "%.2f km", trips[tripIndex].distance)}")
                    } else {
                        InfoCard(info = "Trip ${tripIndex + 1} Avg Speed: ${String.format(Locale.getDefault(), "%.2f km/h", trips[tripIndex].averageSpeed)}")
                    }
                }
            }

            // Speedometer at the bottom
            SpeedCard(speed)
        }
    }

    @Composable
    fun InfoCard(info: String) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.5f), // Aspect ratio to make the card a bit rectangular
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = info,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    @Composable
    fun SpeedCard(speed: Float) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .aspectRatio(1.5f),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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

    private fun checkPermissionsAndStartTracking(context: Context, permissionLauncher: ActivityResultLauncher<String>, trips: MutableList<Trip>) {
        // Use isDebugMode within functions
        if (isDebugMode) {
            Log.d("Speedometer", "Checking permissions...")
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            startTracking(context, trips) { speed ->
                // Update UI with new speed
            }
        }
    }

    private fun startTracking(context: Context, trips: MutableList<Trip>, onSpeedChange: (Float) -> Unit) {
        val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        var previousLocation: Location? = null

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val speed = (location.speed * 3600) / 1000 // Convert from m/s to km/h
                onSpeedChange(speed) // Update the speed value

                val lastLocation = previousLocation
                val deltaDistance = if (lastLocation != null) {
                    val distance = location.distanceTo(lastLocation) / 1000 // convert meters to km
                    distance
                } else {
                    0f
                }

                trips.forEachIndexed { index, trip ->
                    val updatedTrip = trip.copy()
                    updatedTrip.update(speed, deltaDistance)
                    trips[index] = updatedTrip // Update specific trip in the MutableList
                }

                previousLocation = location

                if (isDebugMode) {
                    Log.d("TripComputer", "Location updated: Speed = $speed km/h, Distance = $deltaDistance km")
                }
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        }
    }
}
