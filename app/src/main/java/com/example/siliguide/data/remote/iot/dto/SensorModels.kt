package com.example.siliguide.data.remote.iot.dto

import kotlinx.serialization.Serializable

@Serializable
data class SoilDataDto(
    val moisture: Double,
    val temperature: Double,
    val ph: Double,
    val ec: Int,
    val nitrogen: Int,
    val phosphorus: Int,
    val potassium: Int
)

@Serializable
data class EnvironmentDataDto(
    val light: Int,
    val airTemperature: Double, // Don't forget to match this in the esp32 code
    val status: String
)

@Serializable
data class SystemDataDto(
    val sensorOK: Boolean,
    val dhtOK: Boolean
)

@Serializable
data class SensorResponseDto(
    val soil: SoilDataDto,
    val environment: EnvironmentDataDto,
    val system: SystemDataDto
)