package com.example.siliguide.domain.model

data class AnalysisSession(
    val sessionId: String,
    val plantId: String,
    val deviceId: String?,
    val sessionTimestamp: Long,
    val imagePath: String?,
    val predictedClass: String,
    val confidence: Double,
    val airTemp: Double?,
    val lightIntensity: Double?,
    val nitrogen: Int?,
    val phosphorus: Int?,
    val potassium: Int?,
    val electricalConductivity: Int?,
    val soilMoisture: Double?,
    val soilTemp: Double?,
    val soilPh: Double?,
    val plantCondition: String?,
    val possibleCauses: String?,
    val symptoms: String?,
    val recommendedActions: String?,
    val isPartial: Boolean
)