package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class FetchSensorsUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    // The 'operator fun invoke' allows us to call the class like a function
    suspend operator fun invoke(deviceId: String? = null): SensorResponse {
        // You could add business logic here later (e.g., checking if the data is valid)
        return repository.getSensors(deviceId)
    }
}