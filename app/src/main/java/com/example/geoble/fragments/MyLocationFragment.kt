package com.example.geoble.fragments

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.geoble.R
import com.example.geoble.service.LocationService
import com.google.android.material.progressindicator.LinearProgressIndicator

class MyLocationFragment : Fragment(R.layout.fragment_my_location) {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private var isServiceStarted = false
    private lateinit var latitudeTextView: TextView
    private lateinit var longitudeTextView: TextView
    private lateinit var loadingBar: LinearProgressIndicator

    // BroadcastReceiver to receive location updates from the service
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val latitude = intent?.getDoubleExtra("latitude", 0.0)
            val longitude = intent?.getDoubleExtra("longitude", 0.0)

            // Update the UI with received location data
            loadingBar.visibility = View.INVISIBLE
            latitudeTextView.text = "Latitude: $latitude"
            longitudeTextView.text = "Longitude: $longitude"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize TextViews for updating location
        latitudeTextView = view.findViewById(R.id.latitudeTextView)
        longitudeTextView = view.findViewById(R.id.longitudeTextView)
        loadingBar = view.findViewById(R.id.linearProgressIndicator)

        // Request permissions if needed
        if (!checkPermissions()) {
            requestPermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isServiceStarted) {
            startLocationService()
            isServiceStarted = true
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(locationReceiver, IntentFilter("LocationUpdates"))
    }

    override fun onPause() {
        super.onPause()
        // Unregister the broadcast receiver to avoid memory leaks
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(locationReceiver)
    }

    private fun checkPermissions(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startLocationService()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
        requireContext().startService(serviceIntent)
    }

    private fun stopLocationService() {
        val serviceIntent = Intent(requireContext(), LocationService::class.java)
        requireContext().stopService(serviceIntent)
        isServiceStarted = false
    }
}
