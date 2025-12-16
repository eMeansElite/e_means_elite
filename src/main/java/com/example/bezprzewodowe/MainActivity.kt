package com.example.bezprzewodowe


import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.nio.ByteBuffer
import java.util.*

class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var valueTextView: TextView

    // Zmienne BLE
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothGatt: BluetoothGatt? = null

    companion object {
        private const val TAG = "BLE_GekoSense"

        //private const val DEVICE_ADDRESS = "CB:71:AD:8A:4C:55"
        private const val DEVICE_ADDRESS = "14:3F:A6:71:7B:81"
        //  UUID trzeba zmienic przy Service i Characteristic na odpowiednie od czujnika
        //val SERVICE_UUID: UUID = UUID.fromString("a61c8642-2e46-4d1d-2137-f77d8adb5e41")

        //val CHAR_UUID: UUID = UUID.fromString("a61c0001-2e46-4d1d-2137-f77d8adb5e41")

        val SERVICE_UUID: UUID = UUID.fromString("69a7f243-e52f-4443-a7f9-cb4d053c74d6")

        val CHAR_UUID: UUID = UUID.fromString("3f92019d-ac1d-48dc-9d94-86a0fb507591")

        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") //UUID aplikacji dont changeee!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        valueTextView = findViewById(R.id.valueTextView)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Sprawdź uprawnienia i połącz
        if (hasPermissions()) {
            connectToDevice()
        } else {
            requestBlePermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice() {
        val device = bluetoothAdapter?.getRemoteDevice(DEVICE_ADDRESS)
        if (device == null) {
            Log.e(TAG, "Nie znaleziono urządzenia")
            return
        }

        Log.d(TAG, "Łączenie z: ${device.address}")
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Połączono. Odkrywanie serwisów...")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Rozłączono.")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                enableNotifications(gatt)
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val data = characteristic.value
            val hexString = data.toHexString()

            val parsedValue = parseGekoSenseData(data)

            Log.d(TAG, "Nowa wartość Hex: $hexString, Parsowana wartość: $parsedValue")

            runOnUiThread {
                // Pokaż parsowaną wartość w UI
                valueTextView.text = "Hex: $hexString\nValue: $parsedValue"
            }
        }

    }

    @SuppressLint("MissingPermission")
    private fun enableNotifications(gatt: BluetoothGatt) {
        val service = gatt.getService(SERVICE_UUID)
        if (service == null) {
            Log.e(TAG, "Serwis nie znaleziony!")
            return
        }

        val characteristic = service.getCharacteristic(CHAR_UUID)
        if (characteristic == null) {
            Log.e(TAG, "Charakterystyka nie znaleziona!")
            service.characteristics.forEach {
                Log.d(TAG, "Dostępna char: ${it.uuid}")
            }
            return
        }

        gatt.setCharacteristicNotification(characteristic, true)

        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            gatt.writeDescriptor(descriptor)
            Log.d(TAG, "Wysłano żądanie powiadomień do sensora")
        }
    }

    private fun parseGekoSenseData(data: ByteArray): String {
        if (data.size < 8) {
            return "Błąd: Zbyt mało danych (${data.size} bajtów)."
        }
        try {
            val buffer1 = java.nio.ByteBuffer.wrap(data, 0, 4)
            val value1 = buffer1.float

            val buffer2 = java.nio.ByteBuffer.wrap(data, 4, 4)
            val value2 = buffer2.float

            return "Parametr 1: %.2f | Parametr 2: %.2f".format(value1, value2)

        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas parsowania Float: ${e.message}")
            return "Błąd parsowania: Spróbuj innej konwersji (np. Int)"
        }
    }
    private fun ByteArray.toHexString(): String {
        return joinToString(separator = "-") { byte ->
            "%02X".format(byte)
        }
    }
    private fun hasPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        }
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBlePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                1
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
        }
    }
}