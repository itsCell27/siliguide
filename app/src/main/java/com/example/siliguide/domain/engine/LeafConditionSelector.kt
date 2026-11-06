
package com.example.siliguide.domain.engine

import com.example.siliguide.core.ml.LeafSegmenter

private const val SYMPTOM_THRESHOLD = 10.0

object LeafConditionSelector {

    fun selectCondition(detections: List<LeafSegmenter.Detection>): String {

        var curlingCount = 0
        var spotsCount = 0
        var yellowingCount = 0
        var healthyCount = 0

        // Count detected leaves by class ID.
        detections.forEach { detection ->
            when (detection.classId) {
                0 -> curlingCount++
                1 -> healthyCount++
                2 -> spotsCount++
                3 -> yellowingCount++
            }
        }

        // Count only recognized leaf detections.
        val totalDetections = curlingCount + healthyCount + spotsCount + yellowingCount

        // No valid leaf detections.
        if (totalDetections == 0) {
            return "unknown"
        }

        // Calculate total affected leaves.
        val affectedCount = curlingCount + spotsCount + yellowingCount

        val affectedPercentage = (affectedCount.toDouble() / totalDetections) * 100.0

        // No visible symptoms among detected leaves.
        if (affectedCount == 0) {
            return "healthy"
        }

        // Localized symptoms: below the 10% threshold.
        if (affectedPercentage < SYMPTOM_THRESHOLD) {
            return "healthy"
        }

        // At least 10% affected: select the dominant affected class.
        // If tied for the highest count, apply severity priority:
        // Curling > Spots > Yellowing.

        val highestCount = maxOf(
            curlingCount,
            spotsCount,
            yellowingCount
        )

        return when {
            curlingCount == highestCount -> "curling"
            spotsCount == highestCount -> "spots"
            yellowingCount == highestCount -> "yellowing"
            else -> "unknown"
        }
    }
}
