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
    fun getSpecificPairedDevice(hardwareMac: String): BluetoothDevice? {
        val cleanMac = hardwareMac.replace(":", "").uppercase()
        return bluetoothAdapter.bondedDevices
            .firstOrNull { device ->
                val deviceAddress = device.address?.replace(":", "")?.uppercase()
                val name = device.name.orEmpty()
                deviceAddress == cleanMac ||
                device.address.equals(hardwareMac, ignoreCase = true) ||
                (name.startsWith("SiliGuide_") && (
                    cleanMac.endsWith(name.removePrefix("SiliGuide_").uppercase()) ||
                    name.removePrefix("SiliGuide_").uppercase() == cleanMac.takeLast(4) ||
                    name.removePrefix("SiliGuide_").uppercase() == cleanMac.takeLast(6)
                ))
            }
    }
}