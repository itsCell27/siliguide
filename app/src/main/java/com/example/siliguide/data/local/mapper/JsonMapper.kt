package com.example.siliguide.data.local.mapper

import com.example.siliguide.domain.model.SegmentationDetection
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

object JsonMapper {

    private val stringListSerializer =
        ListSerializer(String.serializer())

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeStringList(
        list: List<String>?
    ): String? {

        if (list == null)
            return null

        return json.encodeToString(
            stringListSerializer,
            list
        )
    }

    fun decodeStringList(
        jsonString: String?
    ): List<String> {

        if (jsonString.isNullOrBlank())
            return emptyList()

        return json.decodeFromString(
            stringListSerializer,
            jsonString
        )
    }

    // json mapper for segmentation result
    fun encodeSegmentationResult(
        detections: List<SegmentationDetection>?
    ): String? {
        if (detections == null) return null

        return json.encodeToString(
            ListSerializer(SegmentationDetection.serializer()),
            detections
        )
    }
    fun decodeSegmentationResult(
        jsonString: String?
    ): List<SegmentationDetection>? {
        if (jsonString.isNullOrBlank()) return null

        return json.decodeFromString(
            ListSerializer(SegmentationDetection.serializer()),
            jsonString
        )
    }
}