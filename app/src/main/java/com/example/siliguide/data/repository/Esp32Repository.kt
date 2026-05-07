package com.example.siliguide.data.repository

import android.content.Context
import com.example.siliguide.data.remote.iot.Esp32Api
import com.example.siliguide.data.remote.iot.WifiProvisioningManager
import com.example.siliguide.data.remote.iot.dto.toDomain
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

// This class implements the interface. It knows HOW to get the data (via Ktor API).
class Esp32Repository @Inject constructor(
    private val api: Esp32Api,
    private val wifiProvisioningManager: WifiProvisioningManager,
    private val context: Context
) : IoTRepository {

    override suspend fun getSensors(): SensorResponse {
        // Fetch the DTO from the API, then immediately map it to the pure Domain model
        val dto = api.getSensorData()
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

    override suspend fun verifyDeviceConnection(): Result<String> {
        return try {
            val info = api.getDeviceInfo()

            // Validate the payload
            if (info.device == "SiliGuide" && info.mode == "AP") {
                Result.success(info.deviceId)
            } else {
                Result.failure(Exception("Unrecognized device format."))
            }
        } catch (e: Exception) {
            // Ktor will automatically throw timeouts or connection errors
            Result.failure(e)
        }
    }
}