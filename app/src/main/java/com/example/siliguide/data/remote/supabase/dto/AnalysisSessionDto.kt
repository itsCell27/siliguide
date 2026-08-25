package com.example.siliguide.data.remote.supabase.dto

import com.example.siliguide.data.local.mapper.JsonMapper
import com.example.siliguide.domain.model.AnalysisSession
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.time.Instant

private val jsonSerializer = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

@Serializable
data class AnalysisSessionDto(
    @SerialName("session_id")
    val sessionId: String,
    @SerialName("plant_id")
    val plantId: String,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("session_timestamp")
    val sessionTimestamp: String,
    @SerialName("image_path")
    val imagePath: String, // Relative storage path in analysis-images bucket: "{userId}/{sessionId}.jpg"
    @SerialName("predicted_class")
    val predictedClass: String,
    @SerialName("confidence")
    val confidence: Float,
    @SerialName("segmentation_result")
    val segmentationResult: JsonElement? = null,
    @SerialName("plant_condition")
    val plantCondition: String? = null,
    @SerialName("plant_condition_description")
    val plantConditionDescription: String? = null,
    @SerialName("possible_causes")
    val possibleCauses: JsonElement? = null,
    @SerialName("characteristics")
    val characteristics: JsonElement? = null,
    @SerialName("recommended_actions")
    val recommendedActions: JsonElement? = null,
    @SerialName("is_partial")
    val isPartial: Boolean = false,
    @SerialName("soil_moisture")
    val soilMoisture: Double? = null,
    @SerialName("soil_ph")
    val soilPh: Double? = null,
    @SerialName("soil_temp")
    val soilTemp: Double? = null,
    @SerialName("electrical_conductivity")
    val electricalConductivity: Int? = null,
    @SerialName("nitrogen")
    val nitrogen: Int? = null,
    @SerialName("phosphorus")
    val phosphorus: Int? = null,
    @SerialName("potassium")
    val potassium: Int? = null,
    @SerialName("light_intensity")
    val lightIntensity: Int? = null,
    @SerialName("air_temp")
    val airTemp: Double? = null,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false
)

fun AnalysisSession.toDto(storageImagePath: String): AnalysisSessionDto {
    val segElement = this.segmentationResult?.let { detections ->
        val jsonString = JsonMapper.encodeSegmentationResult(detections)
        jsonString?.let { runCatching { jsonSerializer.parseToJsonElement(it) }.getOrNull() }
    }

    val causesElement = this.possibleCauses?.let { str ->
        runCatching { jsonSerializer.parseToJsonElement(str) }.getOrNull()
    }

    val charsElement = this.characteristics?.let { str ->
        runCatching { jsonSerializer.parseToJsonElement(str) }.getOrNull()
    }

    val recsElement = this.recommendedActions?.let { str ->
        runCatching { jsonSerializer.parseToJsonElement(str) }.getOrNull()
    }

    return AnalysisSessionDto(
        sessionId = this.sessionId,
        plantId = this.plantId,
        deviceId = this.deviceId,
        userId = this.userId,
        sessionTimestamp = Instant.ofEpochMilli(this.sessionTimestamp).toString(),
        imagePath = storageImagePath,
        predictedClass = this.predictedClass,
        confidence = this.confidence.toFloat(),
        segmentationResult = segElement,
        plantCondition = this.plantCondition,
        plantConditionDescription = this.plantConditionDescription,
        possibleCauses = causesElement,
        characteristics = charsElement,
        recommendedActions = recsElement,
        isPartial = this.isPartial,
        soilMoisture = this.soilMoisture,
        soilPh = this.soilPh,
        soilTemp = this.soilTemp,
        electricalConductivity = this.electricalConductivity,
        nitrogen = this.nitrogen,
        phosphorus = this.phosphorus,
        potassium = this.potassium,
        lightIntensity = this.lightIntensity,
        airTemp = this.airTemp,
        updatedAt = Instant.ofEpochMilli(this.updatedAt).toString(),
        isDeleted = this.isDeleted
    )
}

fun AnalysisSessionDto.toDomain(localDeviceImagePath: String): AnalysisSession {
    val timestampMillis = runCatching { Instant.parse(this.sessionTimestamp).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())
    val updatedMillis = runCatching { Instant.parse(this.updatedAt).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())

    val detections = this.segmentationResult?.let { element ->
        JsonMapper.decodeSegmentationResult(element.toString())
    }

    return AnalysisSession(
        sessionId = this.sessionId,
        plantId = this.plantId,
        deviceId = this.deviceId,
        userId = this.userId,
        sessionTimestamp = timestampMillis,
        imagePath = localDeviceImagePath,
        predictedClass = this.predictedClass,
        confidence = this.confidence.toDouble(),
        segmentationResult = detections,
        plantCondition = this.plantCondition,
        plantConditionDescription = this.plantConditionDescription,
        possibleCauses = this.possibleCauses?.toString(),
        characteristics = this.characteristics?.toString(),
        recommendedActions = this.recommendedActions?.toString(),
        isPartial = this.isPartial,
        soilMoisture = this.soilMoisture,
        soilPh = this.soilPh,
        soilTemp = this.soilTemp,
        electricalConductivity = this.electricalConductivity,
        nitrogen = this.nitrogen,
        phosphorus = this.phosphorus,
        potassium = this.potassium,
        lightIntensity = this.lightIntensity,
        airTemp = this.airTemp,
        updatedAt = updatedMillis,
        isSynced = true
    )
}
