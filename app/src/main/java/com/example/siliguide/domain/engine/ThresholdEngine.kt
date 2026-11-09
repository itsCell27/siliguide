package com.example.siliguide.domain.engine

import com.example.siliguide.domain.model.ThresholdConfig

object ThresholdEngine {

    /**
     * Evaluates a raw sensor reading against Siling Labuyo constants.
     */
    fun evaluate(value: Double?, config: ThresholdConfig): ThresholdLevel {
        if (value == null) return ThresholdLevel.UNKNOWN

        return when {
            value < config.normalMin -> ThresholdLevel.LOW
            value > config.normalMax -> ThresholdLevel.HIGH
            else -> ThresholdLevel.NORMAL
        }
    }
}