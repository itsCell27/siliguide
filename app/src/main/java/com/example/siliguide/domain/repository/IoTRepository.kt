package com.example.siliguide.domain.repository

import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.data.remote.iot.dto.WiFiStatusDto
import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.LastDeviceSensorData
import com.example.siliguide.domain.model.SensorResponse
import kotlinx.coroutines.flow.Flow

// The Domain layer defines WHAT it needs, not HOW to get it.
interface IoTRepository {
    suspend fun getSensors(deviceId: String? = null): SensorResponse

    // Returns the deviceId if successful, or an Exception if it fails
    suspend fun verifyDeviceConnection(host: String): Result<DeviceInfoDto>

    suspend fun isDeviceOnline(deviceId: String): Boolean

    /**
     * Claims an unclaimed ESP32 device.
     *
     * Throws an exception if the ESP32 rejects the request.
     */
    suspend fun claimDevice(
        host: String,
        token: String
    )

    suspend fun getDeviceById(deviceId: String): IoTDevice?

    suspend fun getDeviceByHardwareMac(hardwareMac: String): IoTDevice?

    fun connectToDeviceWifi(
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    )

    fun disconnectFromWifi()

    suspend fun configureWiFi(
        host: String,
        token: String,
        ssid: String,
        password: String
    )

    suspend fun checkWiFiStatus(host: String): WiFiStatusDto

    suspend fun saveDevice(device: IoTDevice)
    suspend fun deleteDevice(deviceId: String)

    // UPDATED: Now requires userId for multi-user isolation
    fun getAllDevices(userId: String): Flow<List<IoTDevice>>

    fun getAllUnlinkedDevices(userId: String): Flow<List<IoTDevice>>

    suspend fun updateDeviceName(deviceName: String, updatedAt: Long, deviceId: String)
    suspend fun updateDeviceIp(ip: String, updatedAt: Long, deviceId: String)

    suspend fun updateDeviceConnectionMode(connectionMode: ConnectionMode, updatedAt: Long, deviceId: String)

    suspend fun updateLastDeviceSensorData(deviceId: String, lastSensorData: LastDeviceSensorData)

    suspend fun getLastDeviceSensorData(deviceId: String): LastDeviceSensorData?

    suspend fun markDeviceDeleted(updatedAt: Long, deviceId: String)

    suspend fun resetDevice(deviceId: String)

    // ADDED: Crucial for the sync process
    suspend fun markDeviceSynced(deviceId: String)
}
