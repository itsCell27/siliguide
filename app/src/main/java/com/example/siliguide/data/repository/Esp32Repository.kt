package com.example.siliguide.data.repository

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.example.siliguide.data.local.SiliGuideDatabase
import com.example.siliguide.data.remote.iot.Esp32Api
import com.example.siliguide.data.remote.iot.WifiProvisioningManager
import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.data.remote.iot.dto.toDomain
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.siliguide.data.local.mapper.toIotDomain
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Esp32Repository @Inject constructor(
    private val api: Esp32Api,
    private val wifiProvisioningManager: WifiProvisioningManager,
    @param:ApplicationContext private val context: Context,
    driver: SqlDriver
) : IoTRepository {

    private val database = SiliGuideDatabase(driver)
    private val queries = database.siliGuideQueries

    override suspend fun getSensors(deviceId: String?): SensorResponse {
        // 1. Determine which IP to use
        val targetIp = if (deviceId != null) {
            val device = getDeviceById(deviceId)
            device?.lastKnownIp ?: "192.168.4.1"
        } else {
            "192.168.4.1"
        }

        // 2. OPTIONAL: Identity Verification (Highly Recommended)
        // Verify that the device at 'targetIp' is actually the 'deviceId' we want
        if (deviceId != null && targetIp != "192.168.4.1") {
            try {
                val info = api.getDeviceInfo(targetIp)
                if (info.deviceId != deviceId) {
                    // If the hardware ID doesn't match our DB record,
                    // the IP might have changed or it's the wrong device.
                    throw Exception("Device mismatch: Expected $deviceId but found ${info.deviceId}")
                }
            } catch (e: Exception) {
                // Handle connection failure or mismatch
                throw Exception("Failed to verify IoT device at $targetIp: ${e.message}")
            }
        }

        // 3. Fetch the data
        val dto = api.getSensorData(targetIp)
        return dto.toDomain()
    }

    override fun connectToDeviceWifi(
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        wifiProvisioningManager.connect(
            context = context,
            ssid = ssid,
            password = password,
            onSuccess = onSuccess,
            onFailure = onFailure
        )
    }

    override fun disconnectFromWifi() {
        wifiProvisioningManager.disconnect(context)
    }

    override suspend fun verifyDeviceConnection(): Result<DeviceInfoDto> {
        return try {
            val info = api.getDeviceInfo()

            if (info.device == "SiliGuide" && info.mode == "AP") {
                Result.success(info)
            } else {
                Result.failure(Exception("Unrecognized device format."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- DATABASE OPERATIONS ---

    override suspend fun getDeviceById(deviceId: String): IoTDevice? {
        return queries.getDeviceById(deviceId)
            .executeAsOneOrNull()
            ?.toIotDomain()
    }

    override suspend fun deleteDevice(deviceId: String) {
        queries.deleteDevice(deviceId)
    }

    override fun getAllDevices(userId: String): Flow<List<IoTDevice>> {
        return queries.getDevicesByUser(userId) // Now uses the scoped SQL query
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { sqlEntities ->
                sqlEntities.map { it.toIotDomain() }
            }
    }

    override suspend fun saveDevice(device: IoTDevice) {
        queries.insertDevice(
            device_id = device.deviceId,
            user_id = device.userId,
            device_name = device.deviceName,
            device_identifier = device.deviceIdentifier,
            is_active = if (device.isActive) 1L else 0L,
            last_known_ip = device.lastKnownIp,
            connection_mode = device.connectionMode,
            is_synced = if (device.isSynced) 1L else 0L, // Convert Boolean to Long
            date_added = device.dateAdded
        )
    }

    override suspend fun markDeviceSynced(deviceId: String) {
        queries.markDeviceSynced(deviceId)
    }
}
