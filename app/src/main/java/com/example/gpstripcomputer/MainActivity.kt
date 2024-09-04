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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

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

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxHeight = maxHeight
                val maxWidth = maxWidth

                Column(modifier = Modifier.fillMaxSize()) {
                    InfoGrid(
                        speed = speed,
                        trips = trips,
                        modifier = Modifier.weight(1f)
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
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(trips.size * 2) { index ->
                val tripIndex = index % trips.size
                val isDistance = index >= trips.size
                InfoCard(
                    infoHeader = "Trip ${tripIndex + 1}",
                    infoValue = if (isDistance) {
                        String.format(Locale.getDefault(), "%.2f km", trips[tripIndex].distance)
                    } else {
                        String.format(Locale.getDefault(), "%.2f km/h", trips[tripIndex].averageSpeed)
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
                        .fillMaxWidth()
                        .aspectRatio(1f) // This keeps the grid cells square
                )
            }
        }
    }

    @Composable
    fun SpeedCard(speed: Float, modifier: Modifier = Modifier) {
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

    private fun checkPermissionsAndStartTracking(context: Context, permissionLauncher: ActivityResultLauncher<String>, trips: SnapshotStateList<Trip>) {
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

    private fun startTracking(context: Context, trips: SnapshotStateList<Trip>, onSpeedChange: (Float) -> Unit) {
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

                for (i in 0 until trips.size) {
                    val updatedTrip = trips[i].copy()
                    updatedTrip.update(speed, deltaDistance)
                    trips[i] = updatedTrip // Update specific trip in the MutableStateList
                }

                previousLocation = location

                if (isDebugMode) {
                    Log.d("TripComputer", "Location updated: Speed = $speed km/h, Distance = $deltaDistance km")
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        }
    }
}
