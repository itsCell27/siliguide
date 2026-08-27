package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.SensorFetchResult
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class FetchSensorsWithStatusUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    suspend operator fun invoke(deviceId: String? = null): SensorFetchResult {
        return repository.getSensors(deviceId)
    }
}
