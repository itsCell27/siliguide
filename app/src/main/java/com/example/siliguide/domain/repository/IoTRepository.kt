package com.example.siliguide.domain.repository

import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.SensorResponse
import kotlinx.coroutines.flow.Flow

// The Domain layer defines WHAT it needs, not HOW to get it.
interface IoTRepository {
    suspend fun getSensors(): SensorResponse

    // Returns the deviceId if successful, or an Exception if it fails
    suspend fun verifyDeviceConnection(): Result<DeviceInfoDto>

    fun connectToDeviceWifi(
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    )

    // ADD THESE TWO LINES:
    suspend fun saveDevice(device: IoTDevice)
    suspend fun deleteDevice(deviceId: String)

    // Add this to the interface
    fun getAllDevices(): Flow<List<IoTDevice>>
}