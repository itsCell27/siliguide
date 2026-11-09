package com.example.siliguide.data.cache

import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.presentation.features.analysis.AnalysisCacheKey
import com.example.siliguide.presentation.features.analysis.AnalysisUiState

data class AnalysisCacheEntry(

    val key: AnalysisCacheKey,

    val uiState: AnalysisUiState,

    val sensorData: SensorResponse?,

    val createdAt: Long = System.currentTimeMillis()
)