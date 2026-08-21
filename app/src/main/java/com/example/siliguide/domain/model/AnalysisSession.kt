package com.example.siliguide.domain.model

data class AnalysisSession(
    val sessionId: String,
    val plantId: String,
    val deviceId: String?,
    val userId: String,          // REQUIRED: Multi-user isolation
    val sessionTimestamp: Long,
    val imagePath: String,

    val predictedClass: String,
    val confidence: Double,

    val segmentationResult: List<SegmentationDetection>?,

    val plantCondition: String?,
    val plantConditionDescription: String?,
    val possibleCauses: String?,
    val characteristics: String?,
    val recommendedActions: String?,

    val isPartial: Boolean,

    val soilMoisture: Double?,
    val soilPh: Double?,
    val soilTemp: Double?,
    val electricalConductivity: Int?,
    val nitrogen: Int?,
    val phosphorus: Int?,
    val potassium: Int?,
    val lightIntensity: Int?,
    val airTemp: Double?,

    val isSynced: Boolean = false, // REQUIRED: Sync status
    val isDeleted: Boolean = false,

    val updatedAt: Long
)