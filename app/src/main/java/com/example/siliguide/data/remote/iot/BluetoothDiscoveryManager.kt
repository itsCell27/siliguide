package com.example.siliguide.data.remote.iot

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import javax.inject.Inject

class BluetoothDiscoveryManager @Inject constructor(
    private val bluetoothAdapter: BluetoothAdapter
) {

    @SuppressLint("MissingPermission") // Temporary
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter.bondedDevices.toList()
    }

    @SuppressLint("MissingPermission") // Temporary
    fun getPairedSiliGuideDevices(): List<BluetoothDevice> {
        return bluetoothAdapter.bondedDevices
            .filter { device ->
                device.name?.startsWith("SiliGuide_") == true
            }
    }

    @SuppressLint("MissingPermission") // Temporary
    fun getSpecificPairedDevice(deviceId: String): BluetoothDevice? {
        return bluetoothAdapter.bondedDevices
            .firstOrNull { device ->
                device.name?.startsWith("SiliGuide_") == true &&
                        device.name?.takeLast(4) == deviceId.takeLast(4)
            }
    }
}