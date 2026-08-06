package com.example.siliguide.data.local.mapper

import com.example.siliguide.data.remote.iot.dto.SensorResponseDto
import com.example.siliguide.domain.model.EnvironmentData
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.model.SoilData
import com.example.siliguide.domain.model.SystemData

// This extension function translates the Network DTO into the Pure Domain Model
fun SensorResponseDto.toDomain(): SensorResponse {
    return SensorResponse(
        soil = SoilData(
            moisture = this.soil.moisture,
            temperature = this.soil.temperature,
            ph = this.soil.ph,
            ec = this.soil.ec,
            nitrogen = this.soil.nitrogen,
            phosphorus = this.soil.phosphorus,
            potassium = this.soil.potassium
        ),
        environment = EnvironmentData(
            light = this.environment.light,
            airTemperature = this.environment.airTemperature,
            status = this.environment.status
        ),
        system = SystemData(
            sensorOK = this.system.sensorOK,
            dhtOK = this.system.dhtOK
        )
    )
}