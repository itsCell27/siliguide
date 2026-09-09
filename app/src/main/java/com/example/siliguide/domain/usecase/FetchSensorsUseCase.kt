package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.SensorFetchResult
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class FetchSensorsUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    suspend operator fun invoke(deviceId: String? = null): SensorResponse {
        return when (val result = repository.getSensors(deviceId)) {
            is SensorFetchResult.Success -> result.response
            SensorFetchResult.Unreachable -> throw IllegalStateException("Device unreachable")
            SensorFetchResult.TokenRejected -> throw SecurityException("IoT device is not claimed.")
            SensorFetchResult.DecryptionFailed -> throw SecurityException("Failed to decrypt sensor payload. Invalid token or tampered data.")
        }
    }
}