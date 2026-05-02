package com.example.siliguide.domain.model

data class SoilData(
    val moisture: Double,
    val temperature: Double,
    val ph: Double,
    val ec: Int,
    val nitrogen: Int,
    val phosphorus: Int,
    val potassium: Int
)

data class EnvironmentData(
    val light: Int,
    val airTemperature: Double,
    val status: String
)

data class SystemData(
    val sensorOK: Boolean,
    val dhtOK: Boolean
)

data class SensorResponse(
    val soil: SoilData,
    val environment: EnvironmentData,
    val system: SystemData
)