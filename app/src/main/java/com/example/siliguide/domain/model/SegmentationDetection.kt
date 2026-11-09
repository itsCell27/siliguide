package com.example.siliguide.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SegmentationDetection(
    val classId: Int,
    val label: String,
    val confidence: Float,
    val boundingBox: BoundingBox,

    // Polygon extracted from the segmentation mask
    val polygon: List<PolygonPoint>
)

@Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

@Serializable
data class PolygonPoint(
    val x: Float,
    val y: Float
)