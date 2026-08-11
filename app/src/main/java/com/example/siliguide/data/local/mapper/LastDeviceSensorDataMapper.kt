package com.example.siliguide.data.local.mapper

import com.example.siliguide.data.local.GetLastDeviceSensorData
import com.example.siliguide.domain.model.LastDeviceSensorData

fun GetLastDeviceSensorData.toLastDeviceSensorDataDomain(): LastDeviceSensorData {
    return LastDeviceSensorData(
        lastSeenAt = last_seen_at,
        lastSoilMoisture = last_soil_moisture,
        lastSoilTemp = last_soil_temp,
        lastSoilPh = last_soil_ph,
        lastNitrogen = last_nitrogen?.toInt(),
        lastPhosphorus = last_phosphorus?.toInt(),
        lastPotassium = last_potassium?.toInt(),
        lastElectricalConductivity = last_electrical_conductivity?.toInt(),
        lastAirTemp = last_air_temp,
        lastLightIntensity = last_light_intensity?.toInt()
    )
}