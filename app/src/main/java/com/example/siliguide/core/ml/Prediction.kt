package com.example.siliguide.core.ml

class Prediction (
    val label: String,
    val confidence: Float,
    val allScores: FloatArray
)