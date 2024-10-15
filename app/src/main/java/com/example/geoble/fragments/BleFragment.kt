package com.example.geoble.fragments

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.geoble.R
import com.example.geoble.activities.DeviceDetailsActivity

class BleFragment : Fragment(R.layout.fragment_ble) {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val deviceList = mutableListOf<String>()
    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>
    private var selectedDevice: BluetoothDevice? = null

    companion object {
        private const val REQUEST_BLUETOOTH_PERMISSIONS = 3
        private const val REQUEST_ENABLE_BT = 1
        private const val REQUEST_LOCATION_SETTINGS = 2
        private const val TAG = "BLEScanner"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ble, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listView = view.findViewById(R.id.devices_list)
        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, deviceList)
        listView.adapter = adapter

        val selectedDeviceLabel: TextView = view.findViewById(R.id.selected_device_label)
        val connectButton: Button = view.findViewById(R.id.connect_button)
        val scanButton: Button = view.findViewById(R.id.scan_button)

        // Initialize Bluetooth Adapter
        val bluetoothManager = requireContext().getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        // Connect button click listener
        connectButton.setOnClickListener {
            checkBluetoothAndLocationStatus()
            connectToSelectedDevice()
        }

        // Scan button click listener
        scanButton.setOnClickListener {
            checkBluetoothAndLocationStatus() 
        }

        // Device selection listener
        listView.setOnItemClickListener { _, _, position, _ ->
            val deviceInfo = deviceList[position]
            selectedDevice = bluetoothAdapter.getRemoteDevice(deviceInfo.substringAfter("(").substringBefore(")"))
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Bluetooth permissions not granted", Toast.LENGTH_SHORT).show()
            }
            selectedDeviceLabel.text = "Selected Device: ${selectedDevice?.name ?: "Unknown"}"
            Toast.makeText(requireContext(), "Selected: ${selectedDevice?.name ?: "Unknown"}", Toast.LENGTH_SHORT).show()
        }
    }

    // Check if Bluetooth and Location are enabled
    private fun checkBluetoothAndLocationStatus() {
        if (!bluetoothAdapter.isEnabled) {
            // Bluetooth is disabled, request to enable
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else if (!isLocationEnabled()) {
            // Location is disabled, request to enable location services
            val locationSettingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivityForResult(locationSettingsIntent, REQUEST_LOCATION_SETTINGS)
        } else {
            // If both are enabled, proceed with BLE scan
            checkPermissionsAndStartScan()
        }
    }

    // Check if location services are enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Handle the result of Bluetooth and Location enable requests
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ENABLE_BT) {
            if (bluetoothAdapter.isEnabled) {
                // Bluetooth is now enabled, check location
                if (!isLocationEnabled()) {
                    // Location is still disabled, request to enable it
                    val locationSettingsIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(locationSettingsIntent, REQUEST_LOCATION_SETTINGS)
                } else {
                    // Both Bluetooth and Location are enabled, start BLE scan
                    checkPermissionsAndStartScan()
                }
            } else {
                // Bluetooth not enabled
                Toast.makeText(requireContext(), "Bluetooth not enabled", Toast.LENGTH_SHORT).show()
            }
        } else if (requestCode == REQUEST_LOCATION_SETTINGS) {
            if (isLocationEnabled()) {
                // Location is enabled, proceed with BLE scan
                checkPermissionsAndStartScan()
            } else {
                // Location still not enabled
                Toast.makeText(requireContext(), "Location not enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionsAndStartScan() {
        val permissionsToRequest = mutableListOf<String>()

        // Check necessary permissions for BLE scanning
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        // Request permissions if needed
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(requireActivity(), permissionsToRequest.toTypedArray(), REQUEST_BLUETOOTH_PERMISSIONS)
        } else {
            startBleScan()
        }
    }

    private fun startBleScan() {
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        Toast.makeText(requireContext(), "Scanning for BLE devices...", Toast.LENGTH_SHORT).show()

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        bluetoothLeScanner.startScan(leScanCallback)
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device

            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return
            }

            val deviceName = device.name ?: "Unknown Device"
            val deviceAddress = device.address
            val deviceInfo = "$deviceName ($deviceAddress)"

            // Add device to the list if it's not already present
            if (!deviceList.contains(deviceInfo)) {
                deviceList.add(deviceInfo)
                adapter.notifyDataSetChanged()
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Scan failed with error code: $errorCode")
        }
    }

    private fun connectToSelectedDevice() {
        selectedDevice?.let {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(requireContext(), "Bluetooth permissions not granted", Toast.LENGTH_SHORT).show()
                return
            }

            // Start the DeviceDetailsActivity and pass the selected device's information
            val intent = Intent(requireContext(), DeviceDetailsActivity::class.java).apply {
                putExtra("device_name", it.name ?: "Unknown Device")
                putExtra("device_address", it.address)
            }
            startActivity(intent)
        } ?: run {
            Toast.makeText(requireContext(), "No device selected", Toast.LENGTH_SHORT).show()
        }
    }
}
