package com.example.siliguide.data.repository

import com.example.siliguide.data.remote.iot.Esp32Api
import com.example.siliguide.data.remote.iot.dto.toDomain
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

// This class implements the interface. It knows HOW to get the data (via Ktor API).
class Esp32Repository @Inject constructor(
    private val api: Esp32Api
) : IoTRepository {

    override suspend fun getSensors(): SensorResponse {
        // Fetch the DTO from the API, then immediately map it to the pure Domain model
        val dto = api.getSensorData()
        return dto.toDomain()
    }
}