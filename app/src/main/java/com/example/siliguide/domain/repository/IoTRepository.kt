package com.example.siliguide.domain.repository

import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.SensorResponse
import kotlinx.coroutines.flow.Flow

// The Domain layer defines WHAT it needs, not HOW to get it.
interface IoTRepository {
    suspend fun getSensors(deviceId: String? = null): SensorResponse

    // Returns the deviceId if successful, or an Exception if it fails
    suspend fun verifyDeviceConnection(): Result<DeviceInfoDto>

    suspend fun getDeviceById(deviceId: String): IoTDevice?

    fun connectToDeviceWifi(
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    )

    fun disconnectFromWifi()

    // ADD THESE TWO LINES:
    suspend fun saveDevice(device: IoTDevice)
    suspend fun deleteDevice(deviceId: String)

    // UPDATED: Now requires userId for multi-user isolation
    fun getAllDevices(userId: String): Flow<List<IoTDevice>>

    // ADDED: Crucial for the sync process
    suspend fun markDeviceSynced(deviceId: String)
}
