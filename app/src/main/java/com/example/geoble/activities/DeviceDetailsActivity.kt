package com.example.geoble.activities

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.geoble.R
import com.example.geoble.adapter.ServicesAdapter
import java.util.*

class DeviceDetailsActivity : AppCompatActivity() {

    private lateinit var servicesListView: ListView
    private lateinit var characteristicsListView: ListView
    private lateinit var servicesAdapter: ServicesAdapter
    private lateinit var deviceAddressTextView: TextView
    private lateinit var disconnectButton: Button
    private lateinit var bluetoothGatt: BluetoothGatt

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_device_details)

        // Get the device name and address from the intent
        val deviceName = intent.getStringExtra("device_name")
        val deviceAddress = intent.getStringExtra("device_address")

        // Display the device information
        val deviceInfoTextView: TextView = findViewById(R.id.device_info)
        deviceAddressTextView = findViewById(R.id.device_address)
        deviceInfoTextView.text = "Device Name: $deviceName"
        deviceAddressTextView.text = "Device Address: $deviceAddress"

        // Setup ListView for services and characteristics
        servicesListView = findViewById(R.id.services_list_view)
        characteristicsListView = findViewById(R.id.characteristics_list_view)

        // Start connecting to the device
        connectToDevice(deviceAddress!!)

        // Set up the Disconnect button
        disconnectButton = findViewById(R.id.disconnect_button)
        disconnectButton.setOnClickListener {
            disconnectDevice()
        }
    }

    private fun disconnectDevice() {
        // Disconnect the GATT connection
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        bluetoothGatt.disconnect()
        bluetoothGatt.close() // Clean up resources
        runOnUiThread {
            deviceAddressTextView.text = "Disconnected"
        }
        finish()
    }

    private fun connectToDevice(deviceAddress: String) {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            return
        }
        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback)
    }

    // GATT callback to handle connection and service discovery events
    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Connected to the device, start service discovery
                if (ActivityCompat.checkSelfPermission(
                        this@DeviceDetailsActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    return
                }
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // Handle disconnection
                runOnUiThread {
                    deviceAddressTextView.text = "Disconnected"
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Get the list of services and display them
                val services = gatt.services
                val serviceNames = services.map { it.uuid.toString() }
                runOnUiThread {
                    servicesAdapter = ServicesAdapter(this@DeviceDetailsActivity, serviceNames)
                    servicesListView.adapter = servicesAdapter
                }

                // Optionally, fetch characteristics for the first service
                if (services.isNotEmpty()) {
                    fetchCharacteristics(services[0])
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val charValue = characteristic.value
                // Handle characteristic value
                runOnUiThread {
                    // Update UI with characteristic value
                }
            }
        }
    }

    private fun fetchCharacteristics(service: BluetoothGattService) {
        val characteristics = service.characteristics
        val characteristicNames = characteristics.map { it.uuid.toString() }

        runOnUiThread {
            val characteristicsAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, characteristicNames)
            characteristicsListView.adapter = characteristicsAdapter
        }
    }
}
