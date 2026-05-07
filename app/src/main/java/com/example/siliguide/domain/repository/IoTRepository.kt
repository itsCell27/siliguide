package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.SensorResponse

// The Domain layer defines WHAT it needs, not HOW to get it.
interface IoTRepository {
    suspend fun getSensors(): SensorResponse

    // Returns the deviceId if successful, or an Exception if it fails
    suspend fun verifyDeviceConnection(): Result<String>

    fun connectToDeviceWifi(
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    )
}