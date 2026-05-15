package com.example.siliguide.data.local.mapper

import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.data.local.Analysis_sessions as AnalysisSessions

fun AnalysisSessions.toDomainModel(): AnalysisSession {
    return AnalysisSession(
        sessionId = session_id,
        plantId = plant_id,
        deviceId = device_id,
        sessionTimestamp = session_timestamp,
        imagePath = image_path,
        predictedClass = predicted_class,
        confidence = confidence,
        airTemp = air_temp,
        lightIntensity = light_intensity,
        nitrogen = nitrogen?.toInt(),
        phosphorus = phosphorus?.toInt(),
        potassium = potassium?.toInt(),
        electricalConductivity = electrical_conductivity?.toInt(),
        soilMoisture = soil_moisture,
        soilTemp = soil_temp,
        soilPh = soil_ph,
        plantCondition = plant_condition,
        possibleCauses = possible_causes,
        symptoms = symptoms,
        recommendedActions = recommended_actions,
        isPartial = is_partial == 1L
    )
}