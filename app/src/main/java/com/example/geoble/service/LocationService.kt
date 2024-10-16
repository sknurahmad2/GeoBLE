package com.example.geoble.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import com.example.geoble.R
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*

class LocationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val channelId = "location_service_channel"

    override fun onCreate() {
        super.onCreate()

        // Initialize the FusedLocationProviderClient to get location updates
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Define the location callback, which will be triggered when the location is updated
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                p0 ?: return
                for (location in p0.locations) {
                    // Send location data to the UI and update the notification
                    sendLocationDataToUI(location)
                    updateNotification(location.latitude, location.longitude)
                }
            }
        }

        // Start requesting location updates
        startLocationUpdates()

        // Start as a foreground service (for background location access)
        startForegroundService()
    }

    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        // Check for permissions before requesting location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions not granted; return early
            return
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
    }

    // Function to broadcast location data to the UI
    private fun sendLocationDataToUI(location: Location) {
        val intent = Intent("LocationUpdates").apply {
            putExtra("latitude", location.latitude)
            putExtra("longitude", location.longitude)
        }

        // Use LocalBroadcastManager for local broadcasts
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    // Start the service with the initial notification
    private fun startForegroundService() {
        // Create a notification channel (required for Android 8.0 and above)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Location Service"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT // Change this based on your requirements
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        // Initial notification with placeholder text
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tracking Location")
            .setContentText("Waiting for location...")
            .setSmallIcon(R.drawable.ic_my_location)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        startForeground(1, notification) // ID of 1 is just an example
    }

    // Method to update the notification with the current latitude and longitude
    private fun updateNotification(latitude: Double, longitude: Double) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Current Location")
            .setContentText("Latitude: $latitude, Longitude: $longitude")
            .setSmallIcon(R.drawable.ic_my_location) // Ensure you have a suitable icon
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        notificationManager?.notify(1, notification) // Updates the existing notification
    }

    override fun onBind(intent: Intent?): IBinder? {
        // This is a started service, so binding is not required
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop location updates when the service is destroyed
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
}
