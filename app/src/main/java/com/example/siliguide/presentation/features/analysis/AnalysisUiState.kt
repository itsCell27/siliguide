package com.example.siliguide.presentation.features.analysis

import android.graphics.Bitmap
import com.example.siliguide.domain.engine.RuleResult

data class AnalysisUiState(

    val isLoading: Boolean = false,

    val imagePath: String? = null,

    val predictionLabel: String? = null,

    val predictionName: String? = null,

    val predictionConfidence: Float = 0f,

    val ruleResult: RuleResult? = null,

    val bitmap: Bitmap? = null,
    val segmentationOverlay: Bitmap? = null,

    val sensorUiState: List<SensorRowData> = emptyList(),

    val isSoilSensorOk: Boolean = false,

    val isAirTempSensorOk: Boolean = false,

    val analysisError: String? = null,
)

data class AnalysisCacheKey(
    val imageUri: String,
    val plantId: String?,
    val deviceId: String?
)