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
        var trip1 by remember { mutableStateOf(Trip()) }
        var trip2 by remember { mutableStateOf(Trip()) }
        val context = LocalContext.current

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted: Boolean ->
                if (isGranted) {
                    startTracking(context, listOf(trip1, trip2)) { newSpeed ->
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
            checkPermissionsAndStartTracking(context, permissionLauncher, listOf(trip1, trip2))
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(    // Wrap grid in a box to fill entire space
                modifier = Modifier.fillMaxSize()
            ) {
                InfoGrid(speed, trip1 = trip1, trip2 = trip2) // Grid should now fill the screen vertically
            }
        }
    }

    @Composable
    fun InfoGrid(speed: Float, trip1: Trip, trip2: Trip) {
        val infoList = listOf(
            "Speedometer",
            "Trip 1: ${String.format(Locale.getDefault(), "%.2f km", trip1.distance)}",
            "Trip 1 Avg Speed: ${String.format(Locale.getDefault(), "%.2f km/h", trip1.averageSpeed)}",
            "Trip 2: ${String.format(Locale.getDefault(), "%.2f km", trip2.distance)}",
            "Trip 2 Avg Speed: ${String.format(Locale.getDefault(), "%.2f km/h", trip2.averageSpeed)}"
        )


        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(infoList.size) { index ->
                if (index == 0) {
                    SpeedCard(speed)
                } else {
                    InfoCard(info = infoList[index])
                }
            }
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

    private fun checkPermissionsAndStartTracking(context: Context, permissionLauncher: ActivityResultLauncher<String>, trips: List<Trip>) {
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

    private fun startTracking(context: Context, trips: List<Trip>, onSpeedChange: (Float) -> Unit) {
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

                trips.forEach { trip ->
                    trip.update(speed, deltaDistance)
                }

                previousLocation = location

                if (isDebugMode) {
                    Log.d("TripComputer", "Location updated: Speed = $speed km/h")
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
