package com.example.siliguide.domain.model

data class LastDeviceSensorData (
    val lastSeenAt: Long?,

    val lastSoilMoisture: Double?,
    val lastSoilTemp: Double?,
    val lastSoilPh: Double?,
    val lastElectricalConductivity: Int?,
    val lastNitrogen: Int?,
    val lastPhosphorus: Int?,
    val lastPotassium: Int?,
    val lastAirTemp: Double?,
    val lastLightIntensity: Int?,
)