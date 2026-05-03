package com.example.siliguide.domain.engine

import com.example.siliguide.domain.model.SensorResponse

object RuleEngine {

    fun evaluate(prediction: String, data: SensorResponse?): RuleResult {

        // 1. If we have NO data, or the hardware reports a sensor failure, run the Camera-Only fallback
        if (data == null || !data.system.sensorOK) {
            return fallbackRule(prediction)
        }

        // 2. Extract the data
        val soil = data.soil
        val env = data.environment

        // 3. Run the data through our Siling Labuyo Thresholds!
        val moistureStatus = ThresholdEngine.evaluate(soil.moisture, SiliGuideThresholds.SOIL_MOISTURE)
        val soilTempStatus = ThresholdEngine.evaluate(soil.temperature, SiliGuideThresholds.SOIL_TEMP)
        val soilPhStatus = ThresholdEngine.evaluate(soil.ph, SiliGuideThresholds.SOIL_PH)
        val soilEcStatus = ThresholdEngine.evaluate(soil.ec.toDouble(), SiliGuideThresholds.SOIL_EC)
        val nitrogenStatus = ThresholdEngine.evaluate(soil.nitrogen.toDouble(), SiliGuideThresholds.NITROGEN)
        val phosphorusStatus = ThresholdEngine.evaluate(soil.phosphorus.toDouble(), SiliGuideThresholds.PHOSPHORUS)
        val potassiumStatus = ThresholdEngine.evaluate(soil.potassium.toDouble(), SiliGuideThresholds.POTASSIUM)
        val lightStatus = ThresholdEngine.evaluate(env.light.toDouble(), SiliGuideThresholds.LIGHT_INTENSITY)
        val airTempStatus = ThresholdEngine.evaluate(env.airTemperature, SiliGuideThresholds.AIR_TEMP)

        // 4. The Capstone Logic (Fusing Vision + IoT)
        return when (prediction) {
            "yellowing" -> {
                when {
                    moistureStatus == ThresholdLevel.HIGH -> RuleResult(
                        condition = "Overwatering / Root Hypoxia",
                        symptoms = listOf("Yellowing leaves"),
                        possibleCauses = listOf("Soil is too wet, suffocating roots."),
                        recommendations = listOf("Stop watering immediately.", "Ensure pot/soil has proper drainage."),
                        isUsingSensors = true
                    )
                    nitrogenStatus == ThresholdLevel.LOW -> RuleResult(
                        condition = "Nitrogen Deficiency",
                        symptoms = listOf("Yellowing leaves"),
                        possibleCauses = listOf("Insufficient nitrogen in the soil."),
                        recommendations = listOf("Apply high-nitrogen fertilizer (e.g., Urea or Compost)."),
                        isUsingSensors = true
                    )
                    else -> fallbackRule(prediction)
                }
            }
            "leaf_curling" -> {
                when {
                    moistureStatus == ThresholdLevel.LOW -> RuleResult(
                        condition = "Underwatering / Drought Stress",
                        symptoms = listOf("Curling leaves"),
                        possibleCauses = listOf("Soil is too dry."),
                        recommendations = listOf("Water the plant deeply.", "Apply mulch to retain moisture."),
                        isUsingSensors = true
                    )
                    airTempStatus == ThresholdLevel.HIGH -> RuleResult(
                        condition = "Heat Stress",
                        symptoms = listOf("Curling leaves"),
                        possibleCauses = listOf("Air temperature exceeds 35°C."),
                        recommendations = listOf("Provide afternoon shade.", "Water to cool the soil."),
                        isUsingSensors = true
                    )
                    else -> fallbackRule(prediction)
                }
            }
            "healthy" -> RuleResult(
                condition = "Plant is Healthy",
                symptoms = listOf("Leaves appear normal"),
                possibleCauses = listOf("Optimal environmental conditions."),
                recommendations = listOf("Maintain current watering and feeding schedule."),
                isUsingSensors = true
            )
            "leaf_spots" -> RuleResult(
                condition = "Fungal or Bacterial Risk",
                symptoms = listOf(
                    "Spots on leaves",
                    "Frog-eye spots or water-soaked lesions."
                ),
                possibleCauses = listOf("Pathogen activity often triggered by wet leaves or high humidity."),
                recommendations = listOf("Avoid wetting leaves when watering.", "Isolate plant if severe."),
                isUsingSensors = true
            )
            else -> fallbackRule(prediction)
        }
    }

    // Handles the "Camera Only" Analysis when IoT is disconnected
    private fun fallbackRule(prediction: String): RuleResult {
        return when (prediction) {
            "yellowing" -> RuleResult(
                condition = "Possible Nutrient Issue or Moisture Stress",
                symptoms = listOf("Yellowing leaves"),
                possibleCauses = listOf("Without sensor data, exact cause is unknown."),
                recommendations = listOf("Check soil moisture manually.", "Consider mild fertilizer."),
                isUsingSensors = false
            )
            "leaf_curling" -> RuleResult(
                condition = "Possible Drought or Heat Stress",
                symptoms = listOf("Curling leaves"),
                possibleCauses = listOf("Leaves curl to prevent water loss.", "Could be pest damage."),
                recommendations = listOf("Check for dry soil or extreme heat.", "Inspect undersides of leaves for pests."),
                isUsingSensors = false
            )
            "leaf_spots" -> RuleResult(
                condition = "Fungal or Bacterial Infection",
                symptoms = listOf("Spots on leaves"),
                possibleCauses = listOf("Fungal or bacterial pathogen."),
                recommendations = listOf("Isolate plant if severe.", "Ensure good airflow around the plant."),
                isUsingSensors = false
            )
            else -> RuleResult(
                condition = "Unknown Condition",
                symptoms = listOf(prediction),
                possibleCauses = listOf("Unable to determine."),
                recommendations = listOf("Monitor plant closely."),
                isUsingSensors = false
            )
        }
    }
}