package com.example.siliguide.data.local.mapper

import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.data.local.Analysis_sessions

fun Analysis_sessions.toDomainModel(): AnalysisSession {
    return AnalysisSession(
        sessionId = session_id,
        plantId = plant_id,
        deviceId = device_id,
        userId = user_id, // Map new field
        sessionTimestamp = session_timestamp,
        imagePath = image_path,

        predictedClass = predicted_class,
        confidence = confidence,

        segmentationResult = JsonMapper.decodeSegmentationResult(segmentation_result),

        plantCondition = plant_condition,
        plantConditionDescription = plant_condition_description,
        possibleCauses = possible_causes,
        characteristics = characteristics,
        recommendedActions = recommended_actions,
        isPartial = is_partial == 1L,
        soilMoisture = soil_moisture,
        soilPh = soil_ph,
        soilTemp = soil_temp,
        electricalConductivity = electrical_conductivity?.toInt(),
        nitrogen = nitrogen?.toInt(),
        phosphorus = phosphorus?.toInt(),
        potassium = potassium?.toInt(),
        lightIntensity = light_intensity?.toInt(),
        airTemp = air_temp,

        isSynced = is_synced == 1L, // Map new field
        isDeleted = is_deleted == 1L,
        updatedAt = updated_at
    )
}