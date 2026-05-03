package com.example.siliguide.domain.model

data class ThresholdConfig(
    val sensorName: String,
    val normalMin: Double,
    val normalMax: Double
)