package com.example.siliguide.data.repository

/**
 * Repository implementation responsible for communicating with SiliGuide IoT devices.
 *
 * This repository automatically selects the appropriate communication transport
 * based on the IoT device's saved ConnectionMode:
 *
 * • AP          -> Direct Wi-Fi Hotspot (192.168.4.1)
 * • LAN         -> Local Wi-Fi using the device's saved IP address
 * • BLUETOOTH   -> Bluetooth Classic (SPP)
 *
 * The presentation and domain layers never need to know which transport
 * is being used—they simply request sensor data through the repository.
 */

import android.content.Context
import android.util.Log
import app.cash.sqldelight.db.SqlDriver
import com.example.siliguide.data.local.SiliGuideDatabase
import com.example.siliguide.data.remote.iot.Esp32Api
import com.example.siliguide.data.remote.iot.WifiProvisioningManager
import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.data.local.mapper.toDomain
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.siliguide.data.local.mapper.toIotDomain
import com.example.siliguide.data.local.mapper.toLastDeviceSensorDataDomain
import com.example.siliguide.data.remote.iot.BluetoothApi
import com.example.siliguide.data.remote.iot.dto.WiFiStatusDto
import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.model.LastDeviceSensorData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class IoTRepositoryImpl @Inject constructor(
    private val api: Esp32Api,
    private val bluetoothApi: BluetoothApi,
    private val wifiProvisioningManager: WifiProvisioningManager,
    @param:ApplicationContext private val context: Context,
    driver: SqlDriver
) : IoTRepository {

    companion object {
        /**
         * Default IP address used when the ESP32 is operating
         * in Access Point (AP) mode.
         */
        private const val DEFAULT_AP_IP = "192.168.4.1"
    }

    private val database = SiliGuideDatabase(driver)
    private val queries = database.siliGuideQueries

    override suspend fun getSensors(deviceId: String?): SensorResponse {

        // Retrieve the saved IoT device configuration from the local database.
        // If no deviceId is provided, we'll assume the app is communicating
        // with an ESP32 in its default Access Point (AP) mode.
        val device = deviceId?.let { getDeviceById(it) }

        // Select the communication transport based on how this IoT device
        // was originally registered.
        return when (device?.connectionMode) {

            // ------------------------------------------------------------
            // BLUETOOTH MODE
            // ------------------------------------------------------------
            // The ESP32 is already connected through Bluetooth
            // so no IP address is required.
            ConnectionMode.BLUETOOTH -> {
                val responseDto = bluetoothApi.getSpecificDeviceSensorData(
                    deviceId = device.deviceId,
                    token = device.pairingToken
                ) ?: throw IllegalStateException("Failed to fetch sensor data via Bluetooth")

                responseDto.toDomain()
            }

            // ------------------------------------------------------------
            // LOCAL AREA NETWORK (LAN) MODE
            // ------------------------------------------------------------
            // The ESP32 is connected to the same Wi-Fi network as the phone.
            // Use the last known IP address stored in the local database.
            ConnectionMode.LAN -> {

                val ip = device.lastKnownIp

                api.getSensorData(host = ip, token = device.pairingToken)
                    .toDomain()
            }

            // ------------------------------------------------------------
            // ACCESS POINT (AP) MODE
            // ------------------------------------------------------------
            // During initial setup (or if no device has been selected),
            // the phone connects directly to the ESP32 hotspot.
            // ESP32 always uses the default gateway IP 192.168.4.1 in AP mode.
            ConnectionMode.AP,
            null -> {

                api.getSensorData(host = DEFAULT_AP_IP, token = device?.pairingToken)
                    .toDomain()
            }
        }
    }

    override suspend fun isDeviceOnline(deviceId: String): Boolean {

        val savedDevice = getDeviceById(deviceId)
            ?: return false

        Log.d("IoTRepositoryImpl", "Checking if device is online... $savedDevice")
        Log.d("IoTRepositoryImpl", "Checking if device is online... ${savedDevice.connectionMode}")
        return try {

            val info = when (savedDevice.connectionMode) {

                ConnectionMode.AP ->
                    api.getDeviceInfo(DEFAULT_AP_IP)

                ConnectionMode.LAN ->
                    api.getDeviceInfo(savedDevice.lastKnownIp)

                ConnectionMode.BLUETOOTH ->
                    bluetoothApi.getSpecificDeviceInfo(savedDevice.deviceId)
            }
            Log.d("IoTRepositoryImpl", "Checking if device is online... ${info?.deviceId}")

            info?.deviceId == savedDevice.deviceId

        } catch (_: Exception) {

            false
        }
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

    override suspend fun verifyDeviceConnection(host: String): Result<DeviceInfoDto> {
        return try {

            val info = api.getDeviceInfo(host)

            Result.success(info)

        } catch (e: Exception) {

            Result.failure(e)

        }
    }

    override suspend fun claimDevice(
        host: String,
        token: String
    ) {
        api.claimDevice(
            host = host,
            token = token
        )
    }

    override suspend fun configureWiFi(
        host: String,
        token: String,
        ssid: String,
        password: String
    ) {
        api.configureWiFi(
            host = host,
            token = token,
            ssid = ssid,
            password = password
        )
    }

    override suspend fun checkWiFiStatus(host: String): WiFiStatusDto {
        return api.checkWiFiStatus(host)
    }

    // --- DATABASE OPERATIONS ---

    override suspend fun getDeviceById(deviceId: String): IoTDevice? {
        return queries.getDeviceById(deviceId)
            .executeAsOneOrNull()
            ?.toIotDomain()
    }

    override suspend fun updateDeviceName(deviceName: String, deviceId: String) {
        queries.updateDeviceName(deviceName, deviceId)
    }

    override suspend fun updateDeviceIp(deviceId: String, ip: String) {
        queries.updateDeviceIp(ip, deviceId)
    }

    override suspend fun updateDeviceConnectionMode(deviceId: String, connectionMode: ConnectionMode) {
        queries.updateDeviceConnectionMode(connectionMode.name, deviceId)
    }

    override suspend fun updateLastDeviceSensorData(
        deviceId: String,
        lastSensorData: LastDeviceSensorData
    ) {
        queries.updateDeviceSensorData(
            last_seen_at = lastSensorData.lastSeenAt,
            last_soil_moisture = lastSensorData.lastSoilMoisture,
            last_soil_temp = lastSensorData.lastSoilTemp,
            last_soil_ph = lastSensorData.lastSoilPh,
            last_nitrogen = lastSensorData.lastNitrogen?.toLong(),
            last_phosphorus = lastSensorData.lastPhosphorus?.toLong(),
            last_potassium = lastSensorData.lastPotassium?.toLong(),
            last_electrical_conductivity = lastSensorData.lastElectricalConductivity?.toLong(),
            last_air_temp = lastSensorData.lastAirTemp,
            last_light_intensity = lastSensorData.lastLightIntensity?.toLong(),
            device_id = deviceId
        )
    }

    override suspend fun getLastDeviceSensorData(deviceId: String): LastDeviceSensorData? {
        return queries.getLastDeviceSensorData(deviceId)
            .executeAsOneOrNull()
            ?.toLastDeviceSensorDataDomain()
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
            pairing_token = device.pairingToken,
            is_active = if (device.isActive) 1L else 0L,
            last_known_ip = device.lastKnownIp,
            connection_mode = device.connectionMode.name,
            is_synced = if (device.isSynced) 1L else 0L, // Convert Boolean to Long
            date_added = device.dateAdded
        )
    }

    override suspend fun markDeviceSynced(deviceId: String) {
        queries.markDeviceSynced(deviceId)
    }
}
