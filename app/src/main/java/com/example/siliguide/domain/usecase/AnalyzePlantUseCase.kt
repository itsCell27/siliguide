package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.engine.RuleEngine
import com.example.siliguide.domain.engine.RuleResult
import com.example.siliguide.domain.model.SensorResponse
import javax.inject.Inject

class AnalyzePlantUseCase @Inject constructor() {

    // We move the RuleEngine call out of the ViewModel and into this UseCase
    operator fun invoke(prediction: String, sensorData: SensorResponse?): RuleResult {
        return RuleEngine.evaluate(prediction, sensorData)
    }
}