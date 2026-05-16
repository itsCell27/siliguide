package com.example.siliguide.core.ml

data class Prediction(
    val label: String,
    val confidence: Float,
    val allScores: FloatArray,
    val featureMaps: Array<Array<Array<FloatArray>>>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Prediction

        if (label != other.label) return false
        if (confidence != other.confidence) return false
        if (!allScores.contentEquals(other.allScores)) return false
        if (featureMaps != null) {
            if (other.featureMaps == null) return false
            if (!featureMaps.contentDeepEquals(other.featureMaps)) return false
        } else if (other.featureMaps != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = label.hashCode()
        result = 31 * result + confidence.hashCode()
        result = 31 * result + allScores.contentHashCode()
        result = 31 * result + (featureMaps?.contentDeepHashCode() ?: 0)
        return result
    }
}