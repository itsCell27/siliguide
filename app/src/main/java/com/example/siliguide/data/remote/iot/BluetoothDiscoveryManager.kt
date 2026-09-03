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
        return runCatching { bluetoothAdapter.bondedDevices.toList() }.getOrElse { emptyList() }
    }

    @SuppressLint("MissingPermission") // Temporary
    fun getPairedSiliGuideDevices(): List<BluetoothDevice> {
        return runCatching {
            bluetoothAdapter.bondedDevices.filter { device ->
                val name = try { device.name.orEmpty() } catch (_: SecurityException) { "" }
                name.startsWith("SiliGuide", ignoreCase = true)
            }
        }.getOrElse { emptyList() }
    }

    @SuppressLint("MissingPermission") // Temporary
    fun getSpecificPairedDevice(hardwareMac: String): BluetoothDevice? {
        val cleanTargetMac = hardwareMac.replace(":", "").trim().uppercase()
        val bonded = runCatching { bluetoothAdapter.bondedDevices }.getOrNull() ?: return null

        return bonded.firstOrNull { device ->
            val deviceAddress = device.address?.replace(":", "")?.trim()?.uppercase().orEmpty()
            val rawAddress = device.address.orEmpty()
            val name = try { device.name.orEmpty().trim() } catch (_: SecurityException) { "" }

            // 1. Direct address match (ignoring formatting and case)
            if (deviceAddress == cleanTargetMac || rawAddress.equals(hardwareMac, ignoreCase = true)) {
                return@firstOrNull true
            }

            // 2. Name-based match (e.g., "SiliGuide_CA4B70" matches suffix "CA4B70")
            if (name.startsWith("SiliGuide", ignoreCase = true)) {
                val suffix = name.substringAfter("SiliGuide", "")
                    .removePrefix("_")
                    .removePrefix("-")
                    .trim()
                    .uppercase()

                if (suffix.isNotEmpty() && (
                    cleanTargetMac.endsWith(suffix) ||
                    suffix.endsWith(cleanTargetMac.takeLast(4)) ||
                    suffix.endsWith(cleanTargetMac.takeLast(6)) ||
                    cleanTargetMac.endsWith(suffix.takeLast(4)) ||
                    cleanTargetMac.endsWith(suffix.takeLast(6))
                )) {
                    return@firstOrNull true
                }
            }

            // 3. ESP32 Wi-Fi vs. Bluetooth factory MAC offset tolerance (+/- 2 on the last byte)
            if (cleanTargetMac.length == 12 && deviceAddress.length == 12) {
                val targetPrefix = cleanTargetMac.dropLast(2)
                val devicePrefix = deviceAddress.dropLast(2)
                if (targetPrefix == devicePrefix) {
                    val targetLastByte = cleanTargetMac.takeLast(2).toIntOrNull(16)
                    val deviceLastByte = deviceAddress.takeLast(2).toIntOrNull(16)
                    if (targetLastByte != null && deviceLastByte != null && kotlin.math.abs(targetLastByte - deviceLastByte) <= 2) {
                        return@firstOrNull true
                    }
                }
            }

            false
        }
    }
}