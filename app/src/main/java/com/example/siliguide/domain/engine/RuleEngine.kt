package com.example.siliguide.domain.engine

import com.example.siliguide.domain.model.SensorResponse

object RuleEngine {

    fun evaluate(
        prediction: String,
        data: SensorResponse?
    ): RuleResult {

        if (data == null || !data.system.sensorOK) {
            return fallbackRule(prediction)
        }

        val soil = data.soil
        val env = data.environment

        if (prediction == "yellowing") {

            val causes = mutableListOf<String>()
            val actions = mutableListOf<String>()

            if (soil.nitrogen == 0) {
                causes.add("Nitrogen deficiency")
                actions.add("Apply nitrogen fertilizer")
            }

            if (soil.moisture > 80) {
                causes.add("Overwatering")
                actions.add("Reduce watering")
            }

            if (env.airTemperature > 30) {
                causes.add("Heat stress")
                actions.add("Provide shade")
            }

            return RuleResult(
                condition = "Yellowing due to environmental stress",
                symptoms = emptyList(),
                possibleCauses = causes,
                recommendations = actions,
                isUsingSensors = true
            )
        }

        return fallbackRule(prediction)
    }

    private fun fallbackRule(prediction: String): RuleResult {

        return when (prediction) {

            "yellowing" -> RuleResult(
                condition = null,
                symptoms = listOf(
                    "Leaves turning yellow",
                    "The entire leaf turns pale green or uniform yellow. Often starts on the oldest, lowest leaves first.",
                    "The tissue between the veins turns yellow, while the veins themselves remain dark green.",
                    "Patchy, irregular yellow and light green patterns."
                ),
                possibleCauses = listOf(
                    "Nitrogen (N) Deficiency: Causes general yellowing starting at the bottom of the plant.",
                    "Magnesium (Mg) or Iron (Fe) Deficiency: Causes interveinal chlorosis.",
                    "Overwatering (Hypoxia): Waterlogged soil chokes the roots of oxygen. The roots die, cannot uptake nutrients, and the plant turns yellow.",
                    "Severe Drought: Prolonged lack of water eventually leads to yellowing and leaf drop."
                ),
                isUsingSensors = false
            )

            "leaf_spots" -> RuleResult(
                condition = null,
                symptoms = listOf(
                    "Spots on leaves",
                    "Frog-eye spots: Circular spots with a light gray/white center and a dark brown or reddish margin.",
                    "Water-soaked lesions: Dark, greasy-looking patches that eventually dry out and turn black or brown.",
                    "Concentric rings: Large brown spots with target-like rings.",
                    "Shot-hole effect: The dead tissue in the center of the spot falls out, leaving a literal hole in the leaf."
                ),
                possibleCauses = listOf(
                    "Fungal infection",
                    "Bacterial infection"
                ),
                isUsingSensors = false
            )

            "leaf_curling" -> RuleResult(
                condition = null,
                symptoms = listOf("Leaves curling"),
                possibleCauses = listOf(
                    "Heat stress",
                    "Pests",
                    "Nutrient imbalance"
                ),
                isUsingSensors = false
            )

            else -> RuleResult(
                condition = null,
                symptoms = listOf("Healthy leaves"),
                possibleCauses = listOf("No issue detected"),
                isUsingSensors = false
            )
        }
    }
}