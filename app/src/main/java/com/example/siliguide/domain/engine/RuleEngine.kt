package com.example.siliguide.domain.engine

import com.example.siliguide.domain.model.SensorResponse

object RuleEngine {

    fun evaluate(prediction: String, data: SensorResponse?): RuleResult {

        // If no sensor data or sensor hardware failure → camera-only fallback
        if (data == null || !data.system.sensorOK) {
            return fallbackRule(prediction)
        }

        val soil = data.soil
        val env  = data.environment

        // Evaluate all thresholds
        val moisture   = ThresholdEngine.evaluate(soil.moisture,           SiliGuideThresholds.SOIL_MOISTURE)
        val soilTemp   = ThresholdEngine.evaluate(soil.temperature,        SiliGuideThresholds.SOIL_TEMP)
        val ph         = ThresholdEngine.evaluate(soil.ph,                 SiliGuideThresholds.SOIL_PH)
        val ec         = ThresholdEngine.evaluate(soil.ec.toDouble(),       SiliGuideThresholds.SOIL_EC)
        val nitrogen   = ThresholdEngine.evaluate(soil.nitrogen.toDouble(), SiliGuideThresholds.NITROGEN)
        val phosphorus = ThresholdEngine.evaluate(soil.phosphorus.toDouble(),SiliGuideThresholds.PHOSPHORUS)
        val potassium  = ThresholdEngine.evaluate(soil.potassium.toDouble(), SiliGuideThresholds.POTASSIUM)
        val light      = ThresholdEngine.evaluate(env.light.toDouble(),     SiliGuideThresholds.LIGHT_INTENSITY)
        val airTemp    = ThresholdEngine.evaluate(env.airTemperature,       SiliGuideThresholds.AIR_TEMP)

        val allNormal = listOf(moisture, soilTemp, ph, ec, nitrogen, phosphorus, potassium, light, airTemp)
            .all { it == ThresholdLevel.NORMAL }

        return when (prediction.lowercase()) {

            // ── CURLING ──────────────────────────────────────────────────────

            "curling", "leaf_curling" -> when {

                // N = HIGH + EC = HIGH → Too Much Nitrogen
                nitrogen == ThresholdLevel.HIGH && ec == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Too Much Nitrogen",
                    conditionDescription = "The plant is receiving too much nitrogen, which can cause excessive leafy growth and downward-curled leaves.",
                    recommendations  = listOf(
                        "Stop using nitrogen-rich fertilizer for now.",
                        "If the pot has good drainage, flush the soil with clean water and allow excess water to drain away."
                    ),
                    isUsingSensors   = true
                )

                // Soil Moisture = HIGH + Air Temp = HIGH → Possible Bacterial Wilt
                moisture == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Possible Bacterial Wilt",
                    conditionDescription = "The plant is wilting or curling even though the soil is already wet. A serious disease such as bacterial wilt may be possible.",
                    recommendations  = listOf(
                        "Isolate the affected plant from nearby chili plants and other susceptible plants.",
                        "Stop watering the affected plant for now and avoid moving wet soil or plant material to other plants.",
                        "If the plant continues to wilt or the condition worsens, consider removing it to help protect nearby plants."
                    ),
                    isUsingSensors   = true
                )

                // Air Temp = LOW + Soil Temp = LOW → Cold Stress
                airTemp == ThresholdLevel.LOW && soilTemp == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Cold Stress",
                    conditionDescription = "The plant is being exposed to temperatures that are too cold for healthy growth.",
                    recommendations  = listOf(
                        "Move the plant to a warmer, sheltered area.",
                        "Protect it from cold air at night and avoid leaving the pot on a very cold surface."
                    ),
                    isUsingSensors   = true
                )

                // Air Temp = HIGH + Light = HIGH → Heat and Light Stress
                airTemp == ThresholdLevel.HIGH && light == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Heat and Light Stress",
                    conditionDescription = "High heat combined with strong sunlight is putting the plant under stress and causing excessive water loss.",
                    recommendations  = listOf(
                        "Provide temporary shade during the hottest part of the day.",
                        "Move the plant away from intense midday sunlight if possible and improve airflow around the leaves."
                    ),
                    isUsingSensors   = true
                )

                // Soil Moisture = LOW → Not Enough Water
                moisture == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Not Enough Water",
                    conditionDescription = "The soil is too dry, causing the leaves to lose water and begin curling or drooping.",
                    recommendations  = listOf(
                        "Water the plant thoroughly around the root area.",
                        "If the soil is extremely dry, water gradually rather than flooding it all at once."
                    ),
                    isUsingSensors   = true
                )

                // All sensors NORMAL → Possible Pest or Virus
                allNormal -> RuleResult(
                    condition        = "Possible Pest or Virus",
                    conditionDescription = "The plant is showing leaf curling even though the growing conditions look normal. Pests such as aphids or thrips, or a plant virus, may be involved.",
                    recommendations  = listOf(
                        "Inspect the leaves and stems carefully for pests or unusual markings.",
                        "If pests or a possible virus are found, isolate the affected plant from nearby chili plants and other susceptible plants.",
                        "Treat pests with an appropriate product according to its label.",
                        "Clean your hands and tools after handling the affected plant."
                    ),
                    isUsingSensors   = true
                )

                else -> fallbackRuleWithSensor(prediction)
            }

            // ── SPOTS ────────────────────────────────────────────────────────

            "spots", "leaf_spots" -> when {

                // Soil Moisture = HIGH + Light = LOW → Water Stress / Edema
                moisture == ThresholdLevel.HIGH && light == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Water Stress / Edema",
                    conditionDescription = "The plant is taking up water faster than it can release it, which can cause small blister-like spots on the leaves.",
                    recommendations  = listOf(
                        "Reduce watering and allow the soil to dry slightly.",
                        "Improve airflow and provide adequate light.",
                        "Do not treat this condition as a fungal disease unless other disease symptoms are present."
                    ),
                    isUsingSensors   = true
                )

                // Soil Moisture = HIGH + Air Temp = HIGH → High Risk of Fungal Disease
                moisture == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "High Risk of Fungal Disease",
                    conditionDescription = "Warm and wet conditions can encourage fungal diseases and make leaf spots more likely to spread.",
                    recommendations  = listOf(
                        "Reduce excess moisture and improve airflow around the plant.",
                        "Avoid overhead watering and keep the leaves dry.",
                        "If disease symptoms are present, isolate the affected plant from nearby chili plants and other susceptible plants."
                    ),
                    isUsingSensors   = true
                )

                // Light = HIGH + Air Temp = HIGH → Sun Damage
                light == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Sun Damage",
                    conditionDescription = "The spots may be caused by intense sunlight and heat rather than a disease.",
                    recommendations  = listOf(
                        "Provide partial shade during the hottest part of the day.",
                        "Protect the plant from strong midday sunlight."
                    ),
                    isUsingSensors   = true
                )

                // Air Temp = LOW + Soil Moisture = HIGH → Cool and Wet Disease Risk
                airTemp == ThresholdLevel.LOW && moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Cool and Wet Disease Risk",
                    conditionDescription = "Cool and wet conditions can encourage certain plant diseases and cause spotting or wilting.",
                    recommendations  = listOf(
                        "Reduce excess moisture and improve airflow.",
                        "Avoid wetting the leaves when watering.",
                        "If disease symptoms are present, isolate the affected plant from nearby chili plants and other susceptible plants.",
                        "Remove severely affected plant parts and clean tools after handling the plant."
                    ),
                    isUsingSensors   = true
                )

                // EC = HIGH → Too Much Fertilizer
                ec == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Too Much Fertilizer",
                    conditionDescription = "There may be too much fertilizer or dissolved salt in the soil, which can stress the roots and cause leaf damage.",
                    recommendations  = listOf(
                        "Stop fertilizing for now.",
                        "If the pot drains well, flush the soil with clean water to help remove excess fertilizer salts."
                    ),
                    isUsingSensors   = true
                )

                // All sensors NORMAL → Possible Bacterial or Fungal Disease
                allNormal -> RuleResult(
                    condition        = "Possible Bacterial or Fungal Disease",
                    conditionDescription = "Leaf spots are appearing even though the growing conditions look normal, which may indicate a disease.",
                    recommendations  = listOf(
                        "Isolate the affected plant from nearby chili plants and other susceptible plants.",
                        "Remove badly affected leaves and dispose of them away from healthy plants.",
                        "Keep the leaves dry when watering and clean tools after handling the plant.",
                        "If the spots continue to spread, seek a more specific diagnosis before applying treatment."
                    ),
                    isUsingSensors   = true
                )

                else -> fallbackRuleWithSensor(prediction)
            }

            // ── YELLOWING ────────────────────────────────────────────────────

            "yellowing", "yellowing_leaves" -> when {

                // N = LOW + P = LOW + K = LOW → Not Enough Nutrients
                nitrogen == ThresholdLevel.LOW && phosphorus == ThresholdLevel.LOW && potassium == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Not Enough Nutrients",
                    conditionDescription = "The plant may be lacking several important nutrients needed for healthy growth.",
                    recommendations  = listOf(
                        "Apply a complete, balanced fertilizer according to the product instructions.",
                        "If possible, check the soil pH as well, since an unsuitable pH can affect nutrient availability."
                    ),
                    isUsingSensors   = true
                )

                // pH = LOW or pH = HIGH + N, P, K = NORMAL → Soil pH Problem
                (ph == ThresholdLevel.LOW || ph == ThresholdLevel.HIGH) &&
                        nitrogen == ThresholdLevel.NORMAL &&
                        phosphorus == ThresholdLevel.NORMAL &&
                        potassium == ThresholdLevel.NORMAL -> RuleResult(
                    condition        = "Soil pH Problem",
                    conditionDescription = "The soil pH may be preventing the plant from using some nutrients properly, even though nutrients are present.",
                    recommendations  = listOf(
                        "Adjust the soil pH gradually using a suitable soil amendment.",
                        "Follow the product instructions and avoid adding more fertilizer until the pH problem is addressed."
                    ),
                    isUsingSensors   = true
                )

                // P = HIGH + pH = NORMAL → Too Much Phosphorus
                phosphorus == ThresholdLevel.HIGH && ph == ThresholdLevel.NORMAL -> RuleResult(
                    condition        = "Too Much Phosphorus",
                    conditionDescription = "Excess phosphorus may interfere with the plant's ability to absorb certain micronutrients such as iron and zinc.",
                    recommendations  = listOf(
                        "Stop adding phosphorus-rich fertilizer for now.",
                        "Avoid adding more nutrients until the cause is confirmed, and consider checking the soil before applying micronutrients."
                    ),
                    isUsingSensors   = true
                )

                // Soil Moisture = HIGH → Too Much Water
                moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Too Much Water",
                    conditionDescription = "The soil is staying too wet, which can reduce the oxygen available to the roots and cause yellowing.",
                    recommendations  = listOf(
                        "Stop watering for now and make sure excess water can drain from the pot.",
                        "Allow the soil to dry slightly before watering again."
                    ),
                    isUsingSensors   = true
                )

                // N = LOW → Not Enough Nitrogen
                nitrogen == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Not Enough Nitrogen",
                    conditionDescription = "The plant may not have enough nitrogen to support healthy leaf growth, causing older leaves to turn yellow.",
                    recommendations  = listOf(
                        "Apply a balanced fertilizer according to the product instructions.",
                        "Avoid applying excessive nitrogen, especially when the plant is flowering."
                    ),
                    isUsingSensors   = true
                )

                // K = LOW → Not Enough Potassium
                potassium == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Not Enough Potassium",
                    conditionDescription = "The plant may not have enough potassium, which can cause yellowing around the edges of older leaves and affect fruit development.",
                    recommendations  = listOf(
                        "Apply a balanced fertilizer that contains potassium according to the product instructions.",
                        "Avoid adding large amounts of fertilizer at once."
                    ),
                    isUsingSensors   = true
                )

                // Light = HIGH + Air Temp = NORMAL → Too Much Light
                light == ThresholdLevel.HIGH && airTemp == ThresholdLevel.NORMAL -> RuleResult(
                    condition        = "Too Much Light",
                    conditionDescription = "Very strong light may be damaging the leaves and causing them to become pale or yellow.",
                    recommendations  = listOf(
                        "Provide partial shade, especially during periods of very strong sunlight."
                    ),
                    isUsingSensors   = true
                )

                // All sensors NORMAL → Possible Magnesium Deficiency
                allNormal -> RuleResult(
                    condition        = "Possible Magnesium Deficiency",
                    conditionDescription = "The leaves may be yellowing because the plant does not have enough available magnesium, especially when the area between the veins becomes yellow.",
                    recommendations  = listOf(
                        "Consider a magnesium-containing fertilizer or supplement only after checking the plant's nutrient needs.",
                        "Follow the product instructions and avoid adding unnecessary nutrients."
                    ),
                    isUsingSensors   = true
                )

                else -> fallbackRuleWithSensor(prediction)
            }

            // ── HEALTHY ──────────────────────────────────────────────────────

            "healthy" -> when {

                // N = HIGH + P = LOW → Too Much Nitrogen
                nitrogen == ThresholdLevel.HIGH && phosphorus == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Too Much Nitrogen",
                    conditionDescription = "The plant may be receiving more nitrogen than it needs, which can encourage leafy growth and reduce flowering.",
                    recommendations  = listOf(
                        "Reduce or stop nitrogen-rich fertilizer for now.",
                        "Use a balanced fertilizer appropriate for the plant's growth stage rather than adding more nitrogen."
                    ),
                    isUsingSensors   = true
                )

                // Air Temp = HIGH → Heat Stress During Flowering
                airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Heat Stress During Flowering",
                    conditionDescription = "The plant may look healthy, but prolonged high temperatures can interfere with flowering and cause flowers to drop.",
                    recommendations  = listOf(
                        "Provide afternoon shade and improve airflow around the plant.",
                        "Keep the soil evenly moist during hot weather without overwatering."
                    ),
                    isUsingSensors   = true
                )

                // Air Temperature — Low → Cold Stress
                airTemp == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Cold Stress",
                    conditionDescription = "The temperature around the plant is lower than ideal and may slow healthy growth.",
                    recommendations  = listOf(
                        "Move the plant to a warmer, sheltered area and protect it from cold air."
                    ),
                    isUsingSensors   = true
                )

                // Soil Moisture — Low → Not Enough Water
                moisture == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Not Enough Water",
                    conditionDescription = "The soil is getting too dry, which can cause the plant to wilt and affect growth.",
                    recommendations  = listOf(
                        "Water the plant thoroughly around the roots. Check the soil before watering again."
                    ),
                    isUsingSensors   = true
                )

                // Soil Moisture — High → Too Much Water
                moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Too Much Water",
                    conditionDescription = "The soil is staying too wet, which can reduce healthy root growth.",
                    recommendations  = listOf(
                        "Stop watering for now and let the soil dry slightly. Make sure excess water can drain from the pot."
                    ),
                    isUsingSensors   = true
                )

                // Soil pH — Low → Soil Too Acidic
                ph == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Soil Too Acidic",
                    conditionDescription = "The soil is more acidic than the plant prefers, which can affect nutrient availability.",
                    recommendations  = listOf(
                        "Consider adding a suitable soil amendment to gradually raise the soil pH. Follow the product instructions."
                    ),
                    isUsingSensors   = true
                )

                // Soil pH — High → Soil Too Alkaline
                ph == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Soil Too Alkaline",
                    conditionDescription = "The soil is more alkaline than the plant prefers, which can make some nutrients harder for the plant to absorb.",
                    recommendations  = listOf(
                        "Consider using a suitable soil amendment to gradually lower the soil pH. Follow the product instructions."
                    ),
                    isUsingSensors   = true
                )

                // Soil EC — High → Too Much Fertilizer
                ec == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Too Much Fertilizer",
                    conditionDescription = "There may be too much fertilizer or dissolved salts in the soil, which can stress the roots.",
                    recommendations  = listOf(
                        "Stop fertilizing for now and flush the soil with clean water if the pot has good drainage."
                    ),
                    isUsingSensors   = true
                )

                // Soil EC / NPK — Low → Not Enough Nutrients
                ec == ThresholdLevel.LOW || nitrogen == ThresholdLevel.LOW || phosphorus == ThresholdLevel.LOW || potassium == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Not Enough Nutrients",
                    conditionDescription = "The soil may not have enough available nutrients to support healthy growth.",
                    recommendations  = listOf(
                        "Apply a balanced fertilizer according to the product instructions."
                    ),
                    isUsingSensors   = true
                )

                // Light Intensity — Low → Not Enough Light
                light == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Not Enough Light",
                    conditionDescription = "The plant is not receiving enough light for healthy growth.",
                    recommendations  = listOf(
                        "Move the plant to a brighter location where it can receive more sunlight."
                    ),
                    isUsingSensors   = true
                )

                // Light Intensity — High → Too Much Light
                light == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Too Much Light",
                    conditionDescription = "The plant may be receiving more intense sunlight than it can comfortably handle, especially during hot conditions.",
                    recommendations  = listOf(
                        "Provide partial shade during the hottest part of the day to protect the plant from strong sunlight."
                    ),
                    isUsingSensors   = true
                )

                // Soil Temperature — Low → Soil Too Cold
                soilTemp == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Soil Too Cold",
                    conditionDescription = "The soil is colder than ideal, which can slow root activity and nutrient uptake.",
                    recommendations  = listOf(
                        "Move the plant to a warmer location and protect the soil from cold surfaces. Adding mulch can also help."
                    ),
                    isUsingSensors   = true
                )

                // Soil Temperature — High → Soil Too Hot
                soilTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Soil Too Hot",
                    conditionDescription = "The soil is getting too warm, which can increase water loss and stress the roots.",
                    recommendations  = listOf(
                        "Protect the soil from strong midday heat. Add mulch around the plant and keep the pot out of direct afternoon heat."
                    ),
                    isUsingSensors   = true
                )

                // All sensors NORMAL → Healthy Plant
                allNormal -> RuleResult(
                    condition        = "Healthy Plant",
                    conditionDescription = "The plant currently shows no concerning visual symptoms and the sensor readings are within the expected range.",
                    recommendations  = listOf(
                        "Continue your normal watering, feeding, lighting, and monitoring routine."
                    ),
                    isUsingSensors   = true
                )

                else -> fallbackRuleWithSensor(prediction)
            }

            else -> fallbackRuleWithSensor(prediction)
        }
    }

    private fun fallbackRuleWithSensor(prediction: String): RuleResult {
        return when (prediction.lowercase()) {

            "healthy" -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(
                    "Uniform, vibrant green to dark emerald color",
                    "Smooth, continuous leaf margins",
                    "Flat, turgid (firm) leaf surface",
                    "Clear, unbroken venation pattern"
                ),
                possibleCauses   = listOf(
                    "Adequate macronutrient (N, P, K) and micronutrient balance",
                    "Optimal soil moisture and temperature conditions"
                ),
                recommendations  = listOf(
                    "No action needed based on visual inspection.",
                    "Connect an IoT device to confirm all sensor readings are optimal."
                ),
                isUsingSensors   = true
            )

            "curling", "leaf_curling" -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(
                    "Upward cupping or rolling of leaf margins",
                    "Downward curling or severe twisting",
                    "Puckering or crinkling of leaf surface"
                ),
                possibleCauses   = listOf(
                    "Heat & Drought Stress: extreme heat (>35°C) and dry soil cause protective curling",
                    "Pests: Thrips, Aphids, or Broad Mites draining sap from young leaves",
                    "Virus: Pepper Leaf Curl Virus (PepLCV) / Chili Leaf Curl Virus (ChiLCV) via Whiteflies",
                    "Herbicide drift from nearby chemical sprays",
                    "Cold stress: temperatures below 18°C cause drooping and curling"
                ),
                recommendations  = listOf(
                    "Check soil moisture by pressing a finger 2–3 cm into the soil.",
                    "Inspect leaf undersides for tiny insects (Thrips, Aphids, Mites).",
                    "Connect an IoT device for precise soil moisture and temperature readings."
                ),
                isUsingSensors   = true
            )

            "spots", "leaf_spots" -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(
                    "Frog-eye spots: light gray/white center with dark brown margin",
                    "Water-soaked lesions that dry out and turn black or brown",
                    "Concentric rings forming target-like patterns",
                    "Shot-hole effect: dead tissue falls out leaving holes"
                ),
                possibleCauses   = listOf(
                    "Fungal infection: Cercospora capsici (frog-eye spots) or Colletotrichum (Anthracnose)",
                    "Bacterial infection: Xanthomonas campestris in high-humidity conditions",
                    "Sunscald from extreme UV exposure",
                    "Fertilizer or pesticide burn",
                    "Rare: Calcium deficiency causing localized necrosis on new growth"
                ),
                recommendations  = listOf(
                    "Avoid wetting leaves when watering — water at the soil base only.",
                    "Remove and dispose of heavily spotted leaves.",
                    "Connect an IoT device to check humidity and soil moisture levels."
                ),
                isUsingSensors   = true
            )

            "yellowing", "yellowing_leaves" -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(
                    "Chlorosis: entire leaf turns pale green or uniform yellow (starts on oldest leaves)",
                    "Interveinal chlorosis: tissue between veins yellows while veins stay green",
                    "Mottling/Mosaic: patchy irregular yellow and light green patterns"
                ),
                possibleCauses   = listOf(
                    "Nitrogen (N) Deficiency: general yellowing starting from the bottom of the plant",
                    "Magnesium (Mg) or Iron (Fe) Deficiency: interveinal chlorosis",
                    "Overwatering (Hypoxia): waterlogged soil chokes roots, blocking nutrient uptake",
                    "Viral infection: Cucumber Mosaic Virus (CMV) or Tobacco Mosaic Virus (TMV) causing mosaic patterns",
                    "Severe drought eventually leads to yellowing and leaf drop"
                ),
                recommendations  = listOf(
                    "Check soil moisture manually — soggy soil suggests overwatering.",
                    "Inspect the pattern of yellowing: bottom-up → Nitrogen; interveinal → Magnesium/Iron; patchy → possible virus.",
                    "Connect an IoT device for soil moisture, pH, and NPK readings to confirm the exact cause."
                ),
                isUsingSensors   = true
            )

            else -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(prediction),
                possibleCauses   = listOf("Unable to determine cause without sensor data."),
                recommendations  = listOf(
                    "Monitor the plant closely.",
                    "Connect an IoT device for a full diagnosis."
                ),
                isUsingSensors   = true
            )
        }
    }

    // ── CAMERA-ONLY FALLBACK (No IoT data) ──────────────────────────────────
    private fun fallbackRule(prediction: String): RuleResult {
        return when (prediction.lowercase()) {

            "healthy" -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(
                    "Uniform, vibrant green to dark emerald color",
                    "Smooth, continuous leaf margins",
                    "Flat, turgid (firm) leaf surface",
                    "Clear, unbroken venation pattern"
                ),
                possibleCauses   = listOf(
                    "Adequate macronutrient (N, P, K) and micronutrient balance",
                    "Optimal soil moisture and temperature conditions"
                ),
                recommendations  = listOf(
                    "No action needed based on visual inspection.",
                    "Connect an IoT device to confirm all sensor readings are optimal."
                ),
                isUsingSensors   = false
            )

            "curling", "leaf_curling" -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(
                    "Upward cupping or rolling of leaf margins",
                    "Downward curling or severe twisting",
                    "Puckering or crinkling of leaf surface"
                ),
                possibleCauses   = listOf(
                    "Heat & Drought Stress: extreme heat (>35°C) and dry soil cause protective curling",
                    "Pests: Thrips, Aphids, or Broad Mites draining sap from young leaves",
                    "Virus: Pepper Leaf Curl Virus (PepLCV) / Chili Leaf Curl Virus (ChiLCV) via Whiteflies",
                    "Herbicide drift from nearby chemical sprays",
                    "Cold stress: temperatures below 18°C cause drooping and curling"
                ),
                recommendations  = listOf(
                    "Check soil moisture by pressing a finger 2–3 cm into the soil.",
                    "Inspect leaf undersides for tiny insects (Thrips, Aphids, Mites).",
                    "Connect an IoT device for precise soil moisture and temperature readings."
                ),
                isUsingSensors   = false
            )

            "spots", "leaf_spots" -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(
                    "Frog-eye spots: light gray/white center with dark brown margin",
                    "Water-soaked lesions that dry out and turn black or brown",
                    "Concentric rings forming target-like patterns",
                    "Shot-hole effect: dead tissue falls out leaving holes"
                ),
                possibleCauses   = listOf(
                    "Fungal infection: Cercospora capsici (frog-eye spots) or Colletotrichum (Anthracnose)",
                    "Bacterial infection: Xanthomonas campestris in high-humidity conditions",
                    "Sunscald from extreme UV exposure",
                    "Fertilizer or pesticide burn",
                    "Rare: Calcium deficiency causing localized necrosis on new growth"
                ),
                recommendations  = listOf(
                    "Avoid wetting leaves when watering — water at the soil base only.",
                    "Remove and dispose of heavily spotted leaves.",
                    "Connect an IoT device to check humidity and soil moisture levels."
                ),
                isUsingSensors   = false
            )

            "yellowing", "yellowing_leaves" -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(
                    "Chlorosis: entire leaf turns pale green or uniform yellow (starts on oldest leaves)",
                    "Interveinal chlorosis: tissue between veins yellows while veins stay green",
                    "Mottling/Mosaic: patchy irregular yellow and light green patterns"
                ),
                possibleCauses   = listOf(
                    "Nitrogen (N) Deficiency: general yellowing starting from the bottom of the plant",
                    "Magnesium (Mg) or Iron (Fe) Deficiency: interveinal chlorosis",
                    "Overwatering (Hypoxia): waterlogged soil chokes roots, blocking nutrient uptake",
                    "Viral infection: Cucumber Mosaic Virus (CMV) or Tobacco Mosaic Virus (TMV) causing mosaic patterns",
                    "Severe drought eventually leads to yellowing and leaf drop"
                ),
                recommendations  = listOf(
                    "Check soil moisture manually — soggy soil suggests overwatering.",
                    "Inspect the pattern of yellowing: bottom-up → Nitrogen; interveinal → Magnesium/Iron; patchy → possible virus.",
                    "Connect an IoT device for soil moisture, pH, and NPK readings to confirm the exact cause."
                ),
                isUsingSensors   = false
            )

            else -> RuleResult(
                condition        = "Unknown Condition",
                characteristics         = listOf(prediction),
                possibleCauses   = listOf("Unable to determine cause without sensor data."),
                recommendations  = listOf(
                    "Monitor the plant closely.",
                    "Connect an IoT device for a full diagnosis."
                ),
                isUsingSensors   = false
            )
        }
    }
}