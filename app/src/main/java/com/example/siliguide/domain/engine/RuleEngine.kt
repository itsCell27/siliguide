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

            // ── CURLING ─────────────────────────────────────────────────────

            "curling", "leaf_curling" -> when {

                // Compound rules first so a single-sensor rule never hides them
                // Rule 1: Nitrogen = HIGH and Soil EC = HIGH -> Too Much Nitrogen
                nitrogen == ThresholdLevel.HIGH && ec == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Too Much Nitrogen",
                    conditionDescription = "The plant is receiving too much nitrogen, which can cause excessive leafy growth and downward-curled leaves.",
                    recommendations      = listOf(
                        "Stop using nitrogen-rich fertilizers such as urea (46-0-0) and ammonium sulfate for now.",
                        "Check what fertilizer was used recently and follow its label. If the plant is in a pot and the soil drains well, slowly water the soil to wash away extra fertilizer, then let the excess water drain out.",
                        "When feeding is needed again, use a balanced fertilizer suitable for chili pepper. Too much nitrogen can cause excessive leaf growth and affect the plant's overall development."
                    ),
                    isUsingSensors       = true
                )

                // Rule 2: Soil Moisture = HIGH and Air Temp = HIGH -> Possible Bacterial Wilt
                moisture == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Possible Bacterial Wilt",
                    conditionDescription = "The plant is wilting or curling even though the soil is already wet. A serious disease such as bacterial wilt may be possible.",
                    recommendations      = listOf(
                        "Check whether the whole plant is wilting even when the soil is moist. This may be a sign of a serious plant disease, but the symptoms should be checked by an agricultural technician if possible.",
                        "Keep the affected plant separate. Clean garden tools after touching it, and do not move its soil or water to other plants.",
                        "If bacterial wilt is strongly suspected or confirmed, remove the plant and dispose of it away from the garden. Sprays generally cannot cure a plant once this disease has spread inside it."
                    ),
                    isUsingSensors       = true
                )

                // Rule 3: Air Temp = LOW and Soil Temp = LOW -> Cold Stress
                airTemp == ThresholdLevel.LOW && soilTemp == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Cold Stress",
                    conditionDescription = "The plant is being exposed to temperatures that are too cold for healthy growth.",
                    recommendations      = listOf(
                        "If the plant is in a pot, move it to a warmer place protected from cold wind.",
                        "Avoid watering when the soil is already wet. Cold, wet soil can further stress the roots.",
                        "Wait until the plant starts growing normally again before applying fertilizer."
                    ),
                    isUsingSensors       = true
                )

                // Rule 4: Air Temp = HIGH and Light = HIGH -> Heat and Light Stress
                airTemp == ThresholdLevel.HIGH && light == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Heat and Light Stress",
                    conditionDescription = "High heat combined with strong sunlight is putting the plant under stress and causing excessive water loss.",
                    recommendations      = listOf(
                        "Give the plant some shade during the hottest part of the day, but allow it to receive gentle sunlight.",
                        "Water the soil around the plant when it starts to dry. Do not keep the soil soaked.",
                        "Add clean organic mulch, such as dry leaves or straw, on top of the soil to help keep it cool. Keep the mulch away from the stem."
                    ),
                    isUsingSensors       = true
                )

                // Single-sensor rules
                // Rule 5: Air Temp = HIGH -> Heat Stress
                airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Heat Stress",
                    conditionDescription = "High air temperature can cause pepper leaves to curl inward as the plant attempts to reduce water loss.",
                    recommendations      = listOf(
                        "Give the plant some shade during the hottest part of the day and allow air to move around it.",
                        "Water around the base of the plant when the soil begins to dry. Avoid both very dry and waterlogged soil.",
                        "Add clean organic mulch, such as dry leaves or straw, to help keep the soil cool."
                    ),
                    isUsingSensors       = true
                )

                // Rule 6: Soil Moisture = LOW -> Water Stress
                moisture == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Water Stress",
                    conditionDescription = "Low soil moisture can cause the plant to curl its leaves as it responds to insufficient water availability.",
                    recommendations      = listOf(
                        "Water slowly around the base of the plant until the soil is moist. Let extra water drain away.",
                        "Check the soil before watering again. Water when the soil begins to dry rather than giving small amounts too often.",
                        "Use mulch, such as dry leaves or straw, to help the soil hold moisture."
                    ),
                    isUsingSensors       = true
                )

                // Rule 7: Soil Moisture = HIGH -> Excess Moisture Stress
                moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Moisture Stress",
                    conditionDescription = "Excessive moisture can stress the roots and interfere with normal root function, potentially contributing to abnormal leaf growth.",
                    recommendations      = listOf(
                        "Stop watering for now if the soil is still very wet. Empty water from plant trays and make sure the pot has holes for drainage.",
                        "If the soil stays wet, improve drainage or move the plant to a pot with suitable potting soil.",
                        "Water around the base of the plant and avoid regularly wetting the leaves."
                    ),
                    isUsingSensors       = true
                )

                // Rule 8: Light = HIGH -> Light Stress
                light == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Light Stress",
                    conditionDescription = "Excessive light can increase leaf temperature and water loss, contributing to leaf curling.",
                    recommendations      = listOf(
                        "Give the plant some shade during very strong midday or afternoon sunlight.",
                        "Keep it in a bright place where it can still receive gentle sunlight.",
                        "Avoid suddenly moving a plant from a shaded place into strong direct sunlight."
                    ),
                    isUsingSensors       = true
                )

                // Rule 9: Soil Temp = HIGH -> Root Zone Heat Stress
                soilTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Root Zone Heat Stress",
                    conditionDescription = "High soil temperature can stress the root system and increase the plant's water demand.",
                    recommendations      = listOf(
                        "Cover the soil with clean dry leaves, straw, or another suitable organic mulch to help keep the roots cool. Do not pile it against the stem.",
                        "Move pots away from hot concrete or pavement. Provide some afternoon shade if the area becomes very hot.",
                        "Keep the soil moist but not soaked."
                    ),
                    isUsingSensors       = true
                )

                // Rule 10: Soil EC = HIGH -> Salt/Fertilizer Stress
                ec == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Salt/Fertilizer Stress",
                    conditionDescription = "High soluble salt concentration can cause root stress and interfere with water uptake, potentially contributing to leaf curling.",
                    recommendations      = listOf(
                        "Stop applying fertilizer for now, especially if fertilizer was recently added.",
                        "If the plant is in a pot and water can drain freely, slowly water the soil to wash away extra fertilizer salts. Let all extra water drain out. Do not do this if the soil is already waterlogged or cannot drain.",
                        "Check the soil salt reading again before using fertilizer. Follow the fertilizer label when feeding the plant again."
                    ),
                    isUsingSensors       = true
                )

                // Rule 11: Soil pH = LOW -> Acidic Soil Stress
                ph == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Acidic Soil Stress",
                    conditionDescription = "Low soil pH can interfere with nutrient availability and contribute to abnormal plant growth.",
                    recommendations      = listOf(
                        "Check the soil pH again, or have the soil tested, before adding anything.",
                        "If the soil is confirmed to be too acidic, agricultural lime may help make it less acidic. Choose the type and amount based on the soil test or advice from an agricultural technician.",
                        "Do not pile lime against the plant stem or add it just because of one sensor reading."
                    ),
                    isUsingSensors       = true
                )

                // Rule 12: Soil pH = HIGH -> Alkaline Soil Stress
                ph == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Alkaline Soil Stress",
                    conditionDescription = "High soil pH can reduce the availability of certain nutrients and contribute to plant stress.",
                    recommendations      = listOf(
                        "Check the soil pH again, or have the soil tested, before trying to change it.",
                        "If the soil is confirmed to be too alkaline, an appropriate soil treatment such as agricultural sulfur may help. Ask an agricultural technician about the right product and amount.",
                        "Do not pour strong acids or other chemicals directly into the soil."
                    ),
                    isUsingSensors       = true
                )

                // Rule 67: Soil Temp = LOW -> Cold Root-Zone Stress
                soilTemp == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Cold Root-Zone Stress",
                    conditionDescription = "The soil temp reading is low relative to the configured SiliGuide threshold. This may contribute to cold root-zone stress, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "Move potted plants away from cold concrete and protect them from cold wind.",
                        "Cover the soil with a light layer of clean organic mulch to help protect the roots. Keep it away from the stem.",
                        "Avoid overwatering while the soil is cold."
                    ),
                    isUsingSensors       = true
                )

                // Rule 68: Soil EC = LOW -> Low Soluble Nutrient Level
                ec == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Low Soluble Nutrient Level",
                    conditionDescription = "The soil ec reading is low relative to the configured SiliGuide threshold. This may contribute to low soluble nutrient level, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "A low soil salt reading does not show exactly which nutrient is missing. Check the leaves and have the soil tested if possible.",
                        "If the plant needs feeding, use a complete balanced NPK fertilizer with minor nutrients. Follow the product label.",
                        "Do not apply extra fertilizer if the soil pH is unsuitable or if the soil salt reading is already high."
                    ),
                    isUsingSensors       = true
                )

                // Rule 69: Nitrogen = LOW -> Possible Nitrogen Deficiency
                nitrogen == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible Nitrogen Deficiency",
                    conditionDescription = "The nitrogen reading is low relative to the configured SiliGuide threshold. This may contribute to possible nitrogen deficiency, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "If older leaves are turning pale or yellow and low nitrogen is confirmed, use urea (46-0-0) or a complete NPK fertilizer that contains nitrogen.",
                        "Follow the product label and keep fertilizer away from the stem.",
                        "Avoid adding more nitrogen if the plant already has dark green leaves or excessive leafy growth."
                    ),
                    isUsingSensors       = true
                )

                // Rule 70: Nitrogen = HIGH -> Excess Nitrogen Risk
                nitrogen == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Nitrogen Risk",
                    conditionDescription = "The nitrogen reading is high relative to the configured SiliGuide threshold. This may contribute to excess nitrogen risk, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "Stop using nitrogen-rich fertilizers such as urea (46-0-0) and ammonium sulfate for now.",
                        "Check which fertilizer was used recently. If the plant is in a pot and the soil drains well, slowly water the soil to wash away extra fertilizer, then let the water drain out.",
                        "When fertilizer is needed again, use a balanced fertilizer suitable for chili pepper and follow its label."
                    ),
                    isUsingSensors       = true
                )

                // Rule 71: Phosphorus = LOW -> Possible Phosphorus Deficiency
                phosphorus == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible Phosphorus Deficiency",
                    conditionDescription = "The phosphorus reading is low relative to the configured SiliGuide threshold. This may contribute to possible phosphorus deficiency, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "Have the soil checked if possible. A sensor reading alone cannot confirm a phosphorus deficiency.",
                        "If low phosphorus is confirmed, use a complete NPK fertilizer that contains phosphorus. Follow the product label.",
                        "Avoid repeatedly applying high-phosphorus fertilizer once the soil has enough phosphorus."
                    ),
                    isUsingSensors       = true
                )

                // Rule 72: Phosphorus = HIGH -> Excess Phosphorus Risk
                phosphorus == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Phosphorus Risk",
                    conditionDescription = "The phosphorus reading is high relative to the configured SiliGuide threshold. This may contribute to excess phosphorus risk, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "Stop using high-phosphorus fertilizers until the soil has been checked.",
                        "Do not add iron, zinc, or other nutrients automatically. First confirm whether the plant is actually lacking them.",
                        "Follow soil test results or agricultural advice when choosing the next fertilizer."
                    ),
                    isUsingSensors       = true
                )

                // Rule 73: Potassium = LOW -> Possible Potassium Deficiency
                potassium == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible Potassium Deficiency",
                    conditionDescription = "The potassium reading is low relative to the configured SiliGuide threshold. This may contribute to possible potassium deficiency, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "If low potassium is confirmed, use a potassium-rich NPK fertilizer suitable for chili pepper.",
                        "Muriate of potash is another potassium fertilizer, but it is concentrated. Use it only if recommended by a soil test or agricultural expert and follow its label.",
                        "Avoid adding extra fertilizer without checking the soil first."
                    ),
                    isUsingSensors       = true
                )

                // Rule 74: Potassium = HIGH -> Excess Potassium Risk
                potassium == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Potassium Risk",
                    conditionDescription = "The potassium reading is high relative to the configured SiliGuide threshold. This may contribute to excess potassium risk, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "Stop using potassium-rich fertilizers until the soil has been checked.",
                        "Do not try to balance it by adding calcium or magnesium unless a soil test or agricultural expert confirms that they are needed.",
                        "Check the soil salt reading and follow soil test advice before applying fertilizer again."
                    ),
                    isUsingSensors       = true
                )

                // Rule 75: Light = LOW -> Insufficient Light
                light == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Insufficient Light",
                    conditionDescription = "The light reading is low relative to the configured SiliGuide threshold. This may contribute to insufficient light, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "Move the plant to a brighter place where it can receive gentle sunlight, preferably in the morning.",
                        "Remove weeds or objects that block the light.",
                        "Let the plant adjust gradually if it has been growing in a shaded area."
                    ),
                    isUsingSensors       = true
                )

                // Rule 76: Air Temp = LOW -> Cold Stress
                airTemp == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Cold Stress",
                    conditionDescription = "The air temp reading is low relative to the configured SiliGuide threshold. This may contribute to cold stress, but the sensor reading alone does not confirm the cause of the visible leaf curling condition.",
                    recommendations      = listOf(
                        "If the plant is in a pot, move it to a warmer place protected from cold wind.",
                        "Avoid watering when the soil is already wet. Cold, wet soil can further stress the roots.",
                        "Wait until the plant starts growing normally again before applying fertilizer."
                    ),
                    isUsingSensors       = true
                )

                // Rule 13: All Sensors NORMAL -> Possible Pest or Virus
                allNormal -> RuleResult(
                    condition            = "Possible Pest or Virus",
                    conditionDescription = "The plant is showing leaf curling even though the growing conditions look normal. Pests such as aphids or thrips, or a plant virus, may be involved.",
                    recommendations      = listOf(
                        "Check the undersides of leaves and new shoots for small insects such as aphids, thrips, or mites. Look for sticky leaves, tiny moving insects, webbing, or unusual leaf patterns.",
                        "If you find a small number of insects, try washing them off with a gentle stream of water and remove badly affected leaves. Keep the plant clean and check it regularly.",
                        "If the insects continue to spread, use an insecticide whose label specifically lists the pest and chili pepper. Follow the product instructions and safety warnings. If a virus is suspected, remove the badly affected plant; insecticide cannot cure a virus."
                    ),
                    isUsingSensors       = true
                )

                else -> fallbackRuleWithSensor(prediction)
            }

            // ── SPOTS ───────────────────────────────────────────────────────

            "spots", "leaf_spots" -> when {

                // Compound rules first so a single-sensor rule never hides them
                // Rule 15: Soil Moisture = HIGH and Light = LOW -> Water Stress / Edema
                moisture == ThresholdLevel.HIGH && light == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Water Stress / Edema",
                    conditionDescription = "The plant is taking up water faster than it can release it, which can cause small blister-like spots on the leaves.",
                    recommendations      = listOf(
                        "Let the soil begin to dry before watering again. Make sure the pot or garden bed can drain extra water.",
                        "Water around the base of the plant instead of spraying the leaves.",
                        "Remove badly damaged leaves and avoid keeping the plant in a place with poor airflow."
                    ),
                    isUsingSensors       = true
                )

                // Rule 16: Soil Moisture = HIGH and Air Temp = HIGH -> High Risk of Fungal Disease
                moisture == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "High Risk of Fungal Disease",
                    conditionDescription = "Warm and wet conditions can encourage fungal diseases and make leaf spots more likely to spread.",
                    recommendations      = listOf(
                        "Avoid watering the leaves. Water the soil around the base of the plant instead.",
                        "Remove badly affected leaves and dispose of them away from healthy plants. Keep enough space around plants for air to move freely.",
                        "If the spots continue to spread and a fungal disease is identified, use a fungicide labeled for that disease and chili pepper. Follow its product instructions."
                    ),
                    isUsingSensors       = true
                )

                // Rule 17: Light = HIGH and Air Temp = HIGH -> Sun Damage
                light == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Sun Damage",
                    conditionDescription = "The spots may be caused by intense sunlight and heat rather than a disease.",
                    recommendations      = listOf(
                        "Give the plant some shade during the hottest part of the day.",
                        "Keep the soil evenly moist, but do not overwater.",
                        "Remove badly damaged leaves if needed and avoid suddenly exposing shaded plants to strong sunlight."
                    ),
                    isUsingSensors       = true
                )

                // Rule 18: Air Temp = LOW and Soil Moisture = HIGH -> Cool and Wet Disease Risk
                airTemp == ThresholdLevel.LOW && moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Cool and Wet Disease Risk",
                    conditionDescription = "Cool and wet conditions can encourage certain plant diseases and cause spotting or wilting.",
                    recommendations      = listOf(
                        "Avoid watering while the soil is already wet. Make sure extra water can drain away.",
                        "Water around the base of the plant and keep leaves as dry as possible.",
                        "Remove badly affected leaves and check for spreading spots. Ask an agricultural technician if symptoms continue."
                    ),
                    isUsingSensors       = true
                )

                // Single-sensor rules
                // Rule 19: Air Temp = HIGH -> Heat Stress
                airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Heat Stress",
                    conditionDescription = "High temperature can increase plant stress and may worsen existing leaf damage.",
                    recommendations      = listOf(
                        "Give the plant some shade during the hottest part of the day and allow air to move around it.",
                        "Water around the base of the plant when the soil begins to dry. Avoid both very dry and waterlogged soil.",
                        "Add clean organic mulch, such as dry leaves or straw, to help keep the soil cool."
                    ),
                    isUsingSensors       = true
                )

                // Rule 20: Soil Moisture = HIGH -> High Moisture Disease Risk
                moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "High Moisture Disease Risk",
                    conditionDescription = "Excessive moisture can create conditions that favor moisture-associated leaf diseases.",
                    recommendations      = listOf(
                        "Reduce watering while the soil remains wet and improve drainage.",
                        "Water the soil around the plant instead of wetting the leaves. Keep plants spaced so air can move between them.",
                        "Remove badly affected leaves and dispose of them away from healthy plants."
                    ),
                    isUsingSensors       = true
                )

                // Rule 21: Soil Moisture = LOW -> Water Stress
                moisture == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Water Stress",
                    conditionDescription = "Low soil moisture can stress the plant and reduce its ability to recover from leaf damage.",
                    recommendations      = listOf(
                        "Water slowly around the base of the plant until the soil is moist. Let extra water drain away.",
                        "Check the soil before watering again. Water when the soil begins to dry rather than giving small amounts too often.",
                        "Use mulch, such as dry leaves or straw, to help the soil hold moisture."
                    ),
                    isUsingSensors       = true
                )

                // Rule 22: Light = HIGH -> Light/Heat Leaf Damage
                light == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Light/Heat Leaf Damage",
                    conditionDescription = "Excessive light can increase leaf temperature and may contribute to scorched or damaged leaf tissue.",
                    recommendations      = listOf(
                        "Give the plant some shade during the hottest part of the day while allowing it to receive gentle sunlight.",
                        "Keep the soil moist but not soaked.",
                        "Avoid spraying plant oils or other treatments on leaves during hot, strong sunlight."
                    ),
                    isUsingSensors       = true
                )

                // Rule 23: Soil Temp = HIGH -> Root Zone Heat Stress
                soilTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Root Zone Heat Stress",
                    conditionDescription = "High soil temperature can place additional stress on the plant and its root system.",
                    recommendations      = listOf(
                        "Cover the soil with clean dry leaves, straw, or another suitable organic mulch to help keep the roots cool. Do not pile it against the stem.",
                        "Move pots away from hot concrete or pavement. Provide some afternoon shade if the area becomes very hot.",
                        "Keep the soil moist but not soaked."
                    ),
                    isUsingSensors       = true
                )

                // Rule 24: Soil EC = HIGH -> Fertilizer/Salt Stress
                ec == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Fertilizer/Salt Stress",
                    conditionDescription = "High soluble salt levels can damage roots and contribute to overall plant stress.",
                    recommendations      = listOf(
                        "Stop applying fertilizer for now, especially if fertilizer was recently added.",
                        "If the plant is in a pot and water can drain freely, slowly water the soil to wash away extra fertilizer salts. Let the extra water drain out. Do not do this if the soil is already waterlogged.",
                        "Check the soil salt reading before feeding the plant again and follow the fertilizer label."
                    ),
                    isUsingSensors       = true
                )

                // Rule 25: Soil pH = LOW -> Acidic Soil Stress
                ph == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Acidic Soil Stress",
                    conditionDescription = "Low pH can interfere with nutrient availability and contribute to plant stress.",
                    recommendations      = listOf(
                        "Check the soil pH again, or have the soil tested, before adding anything.",
                        "If the soil is confirmed to be too acidic, agricultural lime may help make it less acidic. Choose the type and amount based on the soil test or advice from an agricultural technician.",
                        "Do not pile lime against the plant stem or add it just because of one sensor reading."
                    ),
                    isUsingSensors       = true
                )

                // Rule 26: Soil pH = HIGH -> Alkaline Soil Stress
                ph == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Alkaline Soil Stress",
                    conditionDescription = "High pH can reduce the availability of certain nutrients and contribute to plant stress.",
                    recommendations      = listOf(
                        "Check the soil pH again, or have the soil tested, before trying to change it.",
                        "If the soil is confirmed to be too alkaline, an appropriate soil treatment such as agricultural sulfur may help. Ask an agricultural technician about the right product and amount.",
                        "Do not pour strong acids or other chemicals directly into the soil."
                    ),
                    isUsingSensors       = true
                )

                // Rule 27: Light = LOW -> Low-Light Stress
                light == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Low-Light Stress",
                    conditionDescription = "Low light can reduce photosynthetic activity and may slow the plant's recovery from leaf damage.",
                    recommendations      = listOf(
                        "Move the plant to a brighter place where it can receive gentle sunlight, preferably in the morning.",
                        "Remove nearby weeds or objects that block the light.",
                        "Let the plant adjust gradually if it has been growing in a shaded area."
                    ),
                    isUsingSensors       = true
                )

                // Rule 77: Soil Temp = LOW -> Cold Root Stress
                soilTemp == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Cold Root Stress",
                    conditionDescription = "The soil temp reading is low relative to the configured SiliGuide threshold. This may contribute to cold root stress, but the sensor reading alone does not confirm the cause of the visible leaf spots condition.",
                    recommendations      = listOf(
                        "Move potted plants away from cold concrete and protect them from cold wind.",
                        "Cover the soil with a light layer of clean organic mulch to help protect the roots. Keep it away from the stem.",
                        "Avoid overwatering while the soil is cold."
                    ),
                    isUsingSensors       = true
                )

                // Rule 78: Soil EC = LOW -> Low Soluble Nutrient Level
                ec == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Low Soluble Nutrient Level",
                    conditionDescription = "The soil ec reading is low relative to the configured SiliGuide threshold. This may contribute to low soluble nutrient level, but the sensor reading alone does not confirm the cause of the visible leaf spots condition.",
                    recommendations      = listOf(
                        "A low soil salt reading does not show exactly which nutrient is missing. Check the leaves and have the soil tested if possible.",
                        "If the plant needs feeding, use a complete balanced NPK fertilizer with minor nutrients. Follow the product label.",
                        "Do not apply extra fertilizer if the soil pH is unsuitable or if the soil salt reading is already high."
                    ),
                    isUsingSensors       = true
                )

                // Rule 79: Nitrogen = LOW -> Possible Nitrogen Deficiency
                nitrogen == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible Nitrogen Deficiency",
                    conditionDescription = "The nitrogen reading is low relative to the configured SiliGuide threshold. This may contribute to possible nitrogen deficiency, but the sensor reading alone does not confirm the cause of the visible leaf spots condition.",
                    recommendations      = listOf(
                        "If older leaves are turning pale or yellow and low nitrogen is confirmed, use urea (46-0-0) or a complete NPK fertilizer that contains nitrogen.",
                        "Follow the product label and keep fertilizer away from the stem.",
                        "Avoid adding more nitrogen if the plant already has dark green leaves or excessive leafy growth."
                    ),
                    isUsingSensors       = true
                )

                // Rule 80: Nitrogen = HIGH -> Excess Nitrogen Risk
                nitrogen == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Nitrogen Risk",
                    conditionDescription = "The nitrogen reading is high relative to the configured SiliGuide threshold. This may contribute to excess nitrogen risk, but the sensor reading alone does not confirm the cause of the visible leaf spots condition.",
                    recommendations      = listOf(
                        "Stop using nitrogen-rich fertilizers such as urea (46-0-0) and ammonium sulfate for now.",
                        "Check which fertilizer was used recently. If the plant is in a pot and the soil drains well, slowly water the soil to wash away extra fertilizer, then let the water drain out.",
                        "When fertilizer is needed again, use a balanced fertilizer suitable for chili pepper and follow its label."
                    ),
                    isUsingSensors       = true
                )

                // Rule 81: Phosphorus = LOW -> Possible Phosphorus Deficiency
                phosphorus == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible Phosphorus Deficiency",
                    conditionDescription = "The phosphorus reading is low relative to the configured SiliGuide threshold. This may contribute to possible phosphorus deficiency, but the sensor reading alone does not confirm the cause of the visible leaf spots condition.",
                    recommendations      = listOf(
                        "Have the soil checked if possible. A sensor reading alone cannot confirm a phosphorus deficiency.",
                        "If low phosphorus is confirmed, use a complete NPK fertilizer that contains phosphorus. Follow the product label.",
                        "Avoid repeatedly applying high-phosphorus fertilizer once the soil has enough phosphorus."
                    ),
                    isUsingSensors       = true
                )

                // Rule 82: Phosphorus = HIGH -> Excess Phosphorus Risk
                phosphorus == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Phosphorus Risk",
                    conditionDescription = "The phosphorus reading is high relative to the configured SiliGuide threshold. This may contribute to excess phosphorus risk, but the sensor reading alone does not confirm the cause of the visible leaf spots condition.",
                    recommendations      = listOf(
                        "Stop using high-phosphorus fertilizers until the soil has been checked.",
                        "Do not add iron, zinc, or other nutrients automatically. First confirm whether the plant is actually lacking them.",
                        "Follow soil test results or agricultural advice when choosing the next fertilizer."
                    ),
                    isUsingSensors       = true
                )

                // Rule 83: Potassium = LOW -> Possible Potassium Deficiency
                potassium == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible Potassium Deficiency",
                    conditionDescription = "The potassium reading is low relative to the configured SiliGuide threshold. This may contribute to possible potassium deficiency, but the sensor reading alone does not confirm the cause of the visible leaf spots condition.",
                    recommendations      = listOf(
                        "If low potassium is confirmed, use a potassium-rich NPK fertilizer suitable for chili pepper.",
                        "Muriate of potash is another potassium fertilizer, but it is concentrated. Use it only if recommended by a soil test or agricultural expert and follow its label.",
                        "Avoid adding extra fertilizer without checking the soil first."
                    ),
                    isUsingSensors       = true
                )

                // Rule 84: Potassium = HIGH -> Excess Potassium Risk
                potassium == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Potassium Risk",
                    conditionDescription = "The potassium reading is high relative to the configured SiliGuide threshold. This may contribute to excess potassium risk, but the sensor reading alone does not confirm the cause of the visible leaf spots condition.",
                    recommendations      = listOf(
                        "Stop using potassium-rich fertilizers until the soil has been checked.",
                        "Do not try to balance it by adding calcium or magnesium unless a soil test or agricultural expert confirms that they are needed.",
                        "Check the soil salt reading and follow soil test advice before applying fertilizer again."
                    ),
                    isUsingSensors       = true
                )

                // Rule 85: Air Temp = LOW -> Cold / Wet Disease Risk
                airTemp == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Cold / Wet Disease Risk",
                    conditionDescription = "The air temp reading is low relative to the configured SiliGuide threshold. This may contribute to cold / wet disease risk, but the sensor reading alone does not confirm the cause of the visible leaf spots condition.",
                    recommendations      = listOf(
                        "Avoid watering while the soil is already wet and make sure extra water can drain away.",
                        "Water around the base of the plant and keep the leaves as dry as possible.",
                        "Remove badly affected leaves and check whether spots are spreading. Ask an agricultural technician if the problem continues."
                    ),
                    isUsingSensors       = true
                )

                // Rule 28: All Sensors NORMAL -> Possible Bacterial or Fungal Disease
                allNormal -> RuleResult(
                    condition            = "Possible Bacterial or Fungal Disease",
                    conditionDescription = "Leaf spots are appearing even though the growing conditions look normal, which may indicate a disease.",
                    recommendations      = listOf(
                        "Leaf spots can have several causes. Check both sides of the leaves and look for spots that are spreading or changing.",
                        "Remove badly affected leaves using clean scissors or garden tools. Water the soil around the plant and keep the leaves dry.",
                        "If the problem continues, ask an agricultural technician to identify the disease. Use a suitable fungicide or copper-based product only if its label allows use on chili pepper and the identified problem."
                    ),
                    isUsingSensors       = true
                )

                else -> fallbackRuleWithSensor(prediction)
            }

            // ── YELLOWING ───────────────────────────────────────────────────

            "yellowing", "yellowing_leaves" -> when {

                // Compound rules first so a single-sensor rule never hides them
                // Rule 30: Nitrogen = LOW and Phosphorus = LOW and Potassium = LOW -> Not Enough Nutrients
                nitrogen == ThresholdLevel.LOW && phosphorus == ThresholdLevel.LOW && potassium == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Not Enough Nutrients",
                    conditionDescription = "The plant may be lacking several important nutrients needed for healthy growth.",
                    recommendations      = listOf(
                        "If possible, have the soil tested to find out which nutrients are lacking.",
                        "If several nutrients are low, use a complete balanced NPK fertilizer suitable for chili pepper. Follow the product label and choose one that also contains needed minor nutrients.",
                        "Keep fertilizer away from the stem and water the soil after applying it, as directed on the label. Do not use several fertilizers at the same time."
                    ),
                    isUsingSensors       = true
                )

                // Single-sensor rules
                // Rule 31: Phosphorus = HIGH -> Too Much Phosphorus
                phosphorus == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Too Much Phosphorus",
                    conditionDescription = "Excess phosphorus may interfere with the plant's ability to absorb certain micronutrients such as iron and zinc.",
                    recommendations      = listOf(
                        "Stop using fertilizers that contain a lot of phosphorus until the soil has been checked.",
                        "Do not add iron, zinc, or other nutrients unless a soil test or agricultural expert confirms that they are needed.",
                        "Use only the fertilizer nutrients that the plant actually needs."
                    ),
                    isUsingSensors       = true
                )

                // Rule 32: Nitrogen = LOW -> Not Enough Nitrogen
                nitrogen == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Not Enough Nitrogen",
                    conditionDescription = "The plant may not have enough nitrogen to support healthy leaf growth, causing older leaves to turn yellow.",
                    recommendations      = listOf(
                        "If older leaves are turning pale or yellow and low nitrogen is confirmed, use a nitrogen fertilizer such as urea (46-0-0), or a complete NPK fertilizer that contains nitrogen.",
                        "Follow the product label and keep fertilizer away from the stem.",
                        "Avoid adding more nitrogen if the plant already has very dark green leaves or excessive leafy growth."
                    ),
                    isUsingSensors       = true
                )

                // Rule 33: Potassium = LOW -> Not Enough Potassium
                potassium == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Not Enough Potassium",
                    conditionDescription = "The plant may not have enough potassium. A deficiency can cause yellowing or scorching around the edges of older leaves and weaken overall plant growth.",
                    recommendations      = listOf(
                        "If low potassium is confirmed, use a potassium-rich fertilizer suitable for chili pepper.",
                        "Muriate of potash is one possible potassium fertilizer, but use it only if a soil test or agricultural expert recommends it. Follow the product label.",
                        "Avoid adding more fertilizer than needed because excess fertilizer can harm the roots."
                    ),
                    isUsingSensors       = true
                )

                // Rule 34: Air Temp = HIGH -> Heat Stress
                airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Heat Stress",
                    conditionDescription = "High air temperature may cause heat stress, which can interfere with normal plant growth and contribute to leaf discoloration.",
                    recommendations      = listOf(
                        "Give the plant some shade during the hottest part of the day and allow air to move around it.",
                        "Water around the base of the plant when the soil begins to dry. Avoid both very dry and waterlogged soil.",
                        "Add clean organic mulch, such as dry leaves or straw, to help keep the soil cool."
                    ),
                    isUsingSensors       = true
                )

                // Rule 35: Soil Moisture = LOW -> Water Stress
                moisture == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Water Stress",
                    conditionDescription = "Low soil moisture may prevent the plant from receiving enough water to maintain normal leaf function, resulting in yellowing and stress.",
                    recommendations      = listOf(
                        "Water slowly around the base of the plant until the soil is moist. Let extra water drain away.",
                        "Check the soil before watering again. Water when the soil begins to dry rather than giving small amounts too often.",
                        "Use mulch, such as dry leaves or straw, to help the soil hold moisture."
                    ),
                    isUsingSensors       = true
                )

                // Rule 36: Soil Moisture = HIGH -> Excess Moisture Stress
                moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Moisture Stress",
                    conditionDescription = "Excessive soil moisture can stress the roots and interfere with normal water and nutrient uptake, which may contribute to yellowing.",
                    recommendations      = listOf(
                        "Stop watering for now if the soil is still very wet. Empty water from plant trays and make sure the pot has holes for drainage.",
                        "If the soil stays wet, improve drainage or move the plant to a pot with suitable potting soil.",
                        "Water around the base of the plant and avoid regularly wetting the leaves."
                    ),
                    isUsingSensors       = true
                )

                // Rule 37: Light = HIGH -> Light Stress
                light == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Light Stress",
                    conditionDescription = "Excessive light exposure can contribute to leaf stress and discoloration, particularly when associated with high temperatures.",
                    recommendations      = listOf(
                        "Give the plant some shade during very strong midday or afternoon sunlight.",
                        "Keep it in a bright place where it can still receive gentle sunlight.",
                        "Avoid suddenly moving a plant from a shaded place into strong direct sunlight."
                    ),
                    isUsingSensors       = true
                )

                // Rule 38: Soil Temp = HIGH -> Root Zone Heat Stress
                soilTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Root Zone Heat Stress",
                    conditionDescription = "High soil temperature can place stress on the root zone and interfere with normal root function.",
                    recommendations      = listOf(
                        "Cover the soil with clean dry leaves, straw, or another suitable organic mulch to help keep the roots cool. Do not pile it against the stem.",
                        "Move pots away from hot concrete or pavement. Provide some afternoon shade if the area becomes very hot.",
                        "Keep the soil moist but not soaked."
                    ),
                    isUsingSensors       = true
                )

                // Rule 39: Soil EC = HIGH -> Salt/Fertilizer Stress
                ec == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Salt/Fertilizer Stress",
                    conditionDescription = "High electrical conductivity indicates elevated soluble salts in the growing medium, which can stress roots and interfere with water uptake.",
                    recommendations      = listOf(
                        "Stop applying fertilizer for now, especially if fertilizer was recently added.",
                        "If the plant is in a pot and water can drain freely, slowly water the soil to wash away extra fertilizer salts. Let all extra water drain out. Do not do this if the soil is already waterlogged or cannot drain.",
                        "Check the soil salt reading again before using fertilizer. Follow the fertilizer label when feeding the plant again."
                    ),
                    isUsingSensors       = true
                )

                // Rule 40: Soil EC = LOW -> Low Nutrient Availability
                ec == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Low Nutrient Availability",
                    conditionDescription = "Low electrical conductivity may indicate relatively low concentrations of dissolved nutrients available to the plant.",
                    recommendations      = listOf(
                        "A low soil salt reading does not show which nutrient is missing. Check the leaves and, if possible, have the soil tested.",
                        "If the plant needs feeding and the soil is not too salty or too acidic/alkaline, use a complete balanced NPK fertilizer with micronutrients. Follow the product label.",
                        "Avoid adding several fertilizers at once. Too much fertilizer can damage the roots."
                    ),
                    isUsingSensors       = true
                )

                // Rule 41: Soil pH = LOW -> Acidic Soil Stress
                ph == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Acidic Soil Stress",
                    conditionDescription = "Low soil pH can affect nutrient availability and may contribute to nutrient-related leaf discoloration.",
                    recommendations      = listOf(
                        "Check the soil pH again, or have the soil tested, before adding anything.",
                        "If the soil is confirmed to be too acidic, agricultural lime may help make it less acidic. Choose the type and amount based on the soil test or advice from an agricultural technician.",
                        "Do not pile lime against the plant stem or add it just because of one sensor reading."
                    ),
                    isUsingSensors       = true
                )

                // Rule 42: Soil pH = HIGH -> Alkaline Soil Stress
                ph == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Alkaline Soil Stress",
                    conditionDescription = "High soil pH can reduce the availability of certain nutrients and may contribute to leaf yellowing.",
                    recommendations      = listOf(
                        "Check the soil pH again, or have the soil tested, before trying to change it.",
                        "If the soil is confirmed to be too alkaline, an appropriate soil treatment such as agricultural sulfur may help. Ask an agricultural technician about the right product and amount.",
                        "Do not pour strong acids or other chemicals directly into the soil."
                    ),
                    isUsingSensors       = true
                )

                // Rule 43: Light = LOW -> Insufficient Light Stress
                light == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Insufficient Light Stress",
                    conditionDescription = "Insufficient light can reduce photosynthetic activity and may contribute to weak or pale foliage.",
                    recommendations      = listOf(
                        "Move the plant to a brighter place where it can receive gentle sunlight, preferably in the morning.",
                        "If it has been growing in shade, let it get used to stronger sunlight gradually to avoid leaf damage."
                    ),
                    isUsingSensors       = true
                )

                // Rule 86: Soil Temp = LOW -> Cold Root-Zone Stress
                soilTemp == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Cold Root-Zone Stress",
                    conditionDescription = "The soil temp reading is low relative to the configured SiliGuide threshold. This may contribute to cold root-zone stress, but the sensor reading alone does not confirm the cause of the visible leaf yellowing condition.",
                    recommendations      = listOf(
                        "Move potted plants away from cold concrete and protect them from cold wind.",
                        "Cover the soil with a light layer of clean organic mulch to help protect the roots. Keep it away from the stem.",
                        "Avoid overwatering while the soil is cold."
                    ),
                    isUsingSensors       = true
                )

                // Rule 87: Nitrogen = HIGH -> Excess Nitrogen Risk
                nitrogen == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Nitrogen Risk",
                    conditionDescription = "The nitrogen reading is high relative to the configured SiliGuide threshold. This may contribute to excess nitrogen risk, but the sensor reading alone does not confirm the cause of the visible leaf yellowing condition.",
                    recommendations      = listOf(
                        "Stop using nitrogen-rich fertilizers such as urea (46-0-0) and ammonium sulfate for now.",
                        "Check which fertilizer was used recently. If the plant is in a pot and the soil drains well, slowly water the soil to wash away extra fertilizer, then let the water drain out.",
                        "When fertilizer is needed again, use a balanced fertilizer suitable for chili pepper and follow its label."
                    ),
                    isUsingSensors       = true
                )

                // Rule 88: Phosphorus = LOW -> Possible Phosphorus Deficiency
                phosphorus == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible Phosphorus Deficiency",
                    conditionDescription = "The phosphorus reading is low relative to the configured SiliGuide threshold. This may contribute to possible phosphorus deficiency, but the sensor reading alone does not confirm the cause of the visible leaf yellowing condition.",
                    recommendations      = listOf(
                        "Have the soil checked if possible. A sensor reading alone cannot confirm a phosphorus deficiency.",
                        "If low phosphorus is confirmed, use a complete NPK fertilizer that contains phosphorus. Follow the product label.",
                        "Avoid repeatedly applying high-phosphorus fertilizer once the soil has enough phosphorus."
                    ),
                    isUsingSensors       = true
                )

                // Rule 89: Potassium = HIGH -> Excess Potassium Risk
                potassium == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Excess Potassium Risk",
                    conditionDescription = "The potassium reading is high relative to the configured SiliGuide threshold. This may contribute to excess potassium risk, but the sensor reading alone does not confirm the cause of the visible leaf yellowing condition.",
                    recommendations      = listOf(
                        "Stop using potassium-rich fertilizers until the soil has been checked.",
                        "Do not try to balance it by adding calcium or magnesium unless a soil test or agricultural expert confirms that they are needed.",
                        "Check the soil salt reading and follow soil test advice before applying fertilizer again."
                    ),
                    isUsingSensors       = true
                )

                // Rule 90: Air Temp = LOW -> Cold Stress
                airTemp == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Cold Stress",
                    conditionDescription = "The air temp reading is low relative to the configured SiliGuide threshold. This may contribute to cold stress, but the sensor reading alone does not confirm the cause of the visible leaf yellowing condition.",
                    recommendations      = listOf(
                        "If the plant is in a pot, move it to a warmer place protected from cold wind.",
                        "Avoid watering when the soil is already wet. Cold, wet soil can further stress the roots.",
                        "Wait until the plant starts growing normally again before applying fertilizer."
                    ),
                    isUsingSensors       = true
                )

                // Rule 44: All Sensors NORMAL -> Possible Magnesium Deficiency
                allNormal -> RuleResult(
                    condition            = "Possible Magnesium Deficiency",
                    conditionDescription = "The leaves may be yellowing because the plant does not have enough available magnesium, especially when the area between the veins becomes yellow.",
                    recommendations      = listOf(
                        "Look for yellow areas between the veins of older leaves. This can be a sign of low magnesium, but other problems can look similar.",
                        "If low magnesium is confirmed, use a magnesium fertilizer such as Epsom salt, following its product label.",
                        "Do not add lime or other fertilizers unless a soil test or agricultural expert indicates that they are needed."
                    ),
                    isUsingSensors       = true
                )

                else -> fallbackRuleWithSensor(prediction)
            }

            // ── HEALTHY ─────────────────────────────────────────────────────

            "healthy" -> when {

                // Compound rules first so a single-sensor rule never hides them
                // Rule 46: Nitrogen = HIGH and Phosphorus = LOW -> Too Much Nitrogen
                nitrogen == ThresholdLevel.HIGH && phosphorus == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Too Much Nitrogen",
                    conditionDescription = "The plant may be receiving more nitrogen than it needs. Excess nitrogen can encourage overly lush leafy growth and disrupt balanced plant development.",
                    recommendations      = listOf(
                        "Stop using nitrogen-rich fertilizers such as urea (46-0-0) and ammonium sulfate for now.",
                        "Check what fertilizer was used recently and follow its label. If the plant is in a pot and the soil drains well, slowly water the soil to wash away extra fertilizer, then let the excess water drain out.",
                        "When feeding is needed again, use a balanced fertilizer suitable for chili pepper. Too much nitrogen can cause excessive leaf growth and affect the plant's overall development."
                    ),
                    isUsingSensors       = true
                )

                // Rule 54: 2 or more of: Soil EC = LOW, Nitrogen = LOW, Phosphorus = LOW, Potassium = LOW -> Not Enough Nutrients
                listOf(ec, nitrogen, phosphorus, potassium).count { it == ThresholdLevel.LOW } >= 2 -> RuleResult(
                    condition            = "Not Enough Nutrients",
                    conditionDescription = "The soil may not have enough available nutrients to support healthy growth.",
                    recommendations      = listOf(
                        "If possible, have the soil tested to find out which nutrients are lacking.",
                        "If several nutrients are low, use a complete balanced NPK fertilizer suitable for chili pepper. Follow the product label and choose one that also contains needed minor nutrients.",
                        "Keep fertilizer away from the stem and water the soil after applying it, as directed on the label. Do not use several fertilizers at the same time."
                    ),
                    isUsingSensors       = true
                )

                // Single-sensor rules
                // Rule 47: Air Temp = HIGH -> Heat Stress
                airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Heat Stress",
                    conditionDescription = "Prolonged high air temperatures can increase water loss, cause leaf stress, and interfere with normal plant growth and development.",
                    recommendations      = listOf(
                        "Give the plant some shade during the hottest part of the day and allow air to move around it.",
                        "Water around the base of the plant when the soil begins to dry. Avoid both very dry and waterlogged soil.",
                        "Add clean organic mulch, such as dry leaves or straw, to help keep the soil cool."
                    ),
                    isUsingSensors       = true
                )

                // Rule 48: Air Temp = LOW -> Cold Stress
                airTemp == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Cold Stress",
                    conditionDescription = "The temperature around the plant is lower than ideal and may slow healthy growth.",
                    recommendations      = listOf(
                        "If the plant is in a pot, move it to a warmer place protected from cold wind.",
                        "Avoid watering when the soil is already wet. Cold, wet soil can further stress the roots.",
                        "Wait until the plant starts growing normally again before applying fertilizer."
                    ),
                    isUsingSensors       = true
                )

                // Rule 49: Soil Moisture = LOW -> Not Enough Water
                moisture == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Not Enough Water",
                    conditionDescription = "The soil is getting too dry, which can cause the plant to wilt and affect growth.",
                    recommendations      = listOf(
                        "Water slowly around the base of the plant until the soil is moist. Let extra water drain away.",
                        "Check the soil before watering again. Water when it begins to dry.",
                        "Add clean organic mulch, such as dry leaves or straw, to help the soil hold moisture."
                    ),
                    isUsingSensors       = true
                )

                // Rule 50: Soil Moisture = HIGH -> Too Much Water
                moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Too Much Water",
                    conditionDescription = "The soil is staying too wet, which can reduce healthy root growth.",
                    recommendations      = listOf(
                        "Stop watering while the soil is still very wet. Empty water from plant trays and make sure the pot has drainage holes.",
                        "If the soil stays wet, improve drainage or move the plant to a pot with fresh, well-draining soil.",
                        "Water around the base of the plant only when the soil begins to dry."
                    ),
                    isUsingSensors       = true
                )

                // Rule 51: Soil pH = LOW -> Soil Too Acidic
                ph == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Soil Too Acidic",
                    conditionDescription = "The soil is more acidic than the plant prefers, which can affect nutrient availability.",
                    recommendations      = listOf(
                        "Check the soil pH again or have the soil tested before changing it.",
                        "If the soil is confirmed to be too acidic, agricultural lime may help. Follow soil test advice or ask an agricultural technician which type to use.",
                        "Do not pile lime against the plant stem."
                    ),
                    isUsingSensors       = true
                )

                // Rule 52: Soil pH = HIGH -> Soil Too Alkaline
                ph == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Soil Too Alkaline",
                    conditionDescription = "The soil is more alkaline than the plant prefers, which can make some nutrients harder for the plant to absorb.",
                    recommendations      = listOf(
                        "Check the soil pH again or have the soil tested before changing it.",
                        "If the soil is confirmed to be too alkaline, ask an agricultural technician about a suitable soil treatment, such as agricultural sulfur.",
                        "Do not pour strong acids or chemicals directly into the soil."
                    ),
                    isUsingSensors       = true
                )

                // Rule 53: Soil EC = HIGH -> Too Much Fertilizer
                ec == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Too Much Fertilizer",
                    conditionDescription = "There may be too much fertilizer or dissolved salts in the soil, which can stress the roots.",
                    recommendations      = listOf(
                        "Stop applying fertilizer for now and remove any unused fertilizer granules you can see on the soil surface.",
                        "If the plant is in a pot and water can drain freely, slowly water the soil to wash away extra fertilizer. Let the extra water drain out.",
                        "Check the soil salt reading before feeding again. Follow the product label and avoid using more fertilizer than recommended."
                    ),
                    isUsingSensors       = true
                )

                // Rule 55: Light = LOW -> Not Enough Light
                light == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Not Enough Light",
                    conditionDescription = "The plant is not receiving enough light for healthy growth.",
                    recommendations      = listOf(
                        "Move the plant to a brighter place where it can receive gentle sunlight, preferably in the morning.",
                        "Remove nearby weeds or objects that block the sunlight.",
                        "If the plant has been in shade, let it adjust gradually to brighter conditions."
                    ),
                    isUsingSensors       = true
                )

                // Rule 56: Light = HIGH -> Too Much Light
                light == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Too Much Light",
                    conditionDescription = "The plant may be receiving more intense sunlight than it can comfortably handle, especially during hot conditions.",
                    recommendations      = listOf(
                        "Give the plant some shade during the hottest part of the day, while allowing it to receive gentle sunlight.",
                        "Keep the soil moist but not soaked.",
                        "Avoid suddenly moving the plant between deep shade and strong direct sunlight."
                    ),
                    isUsingSensors       = true
                )

                // Rule 57: Soil Temp = LOW -> Soil Too Cold
                soilTemp == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Soil Too Cold",
                    conditionDescription = "The soil is colder than ideal, which can slow root activity and nutrient uptake.",
                    recommendations      = listOf(
                        "Move potted plants away from cold concrete and protect them from cold wind.",
                        "Cover the soil with a light layer of clean organic mulch to help protect the roots. Keep it away from the stem.",
                        "Avoid watering while the soil is already wet."
                    ),
                    isUsingSensors       = true
                )

                // Rule 58: Soil Temp = HIGH -> Soil Too Hot
                soilTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Soil Too Hot",
                    conditionDescription = "The soil is getting too warm, which can increase water loss and stress the roots.",
                    recommendations      = listOf(
                        "Cover the soil with clean dry leaves, straw, or another suitable organic mulch to help keep the roots cool. Keep it away from the stem.",
                        "Move pots away from hot concrete or pavement and provide some afternoon shade if needed.",
                        "Keep the soil moist but not soaked."
                    ),
                    isUsingSensors       = true
                )

                // Rule 91: Soil EC = LOW -> Possible nutrient limitation (no visible symptom)
                ec == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible nutrient limitation (no visible symptom)",
                    conditionDescription = "The soil ec reading is low relative to the configured SiliGuide threshold. This may contribute to possible nutrient limitation (no visible symptom), but the sensor reading alone does not confirm the cause of the visible healthy condition.",
                    recommendations      = listOf(
                        "The sensor reading may suggest that nutrients are low, but it does not show exactly which nutrient is missing. Check the plant and have the soil tested if possible.",
                        "If the plant needs feeding, use a complete balanced NPK fertilizer suitable for chili pepper and follow the product label.",
                        "Avoid adding several fertilizers at once."
                    ),
                    isUsingSensors       = true
                )

                // Rule 92: Nitrogen = LOW -> Possible nutrient limitation (no visible symptom)
                nitrogen == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible nutrient limitation (no visible symptom)",
                    conditionDescription = "The nitrogen reading is low relative to the configured SiliGuide threshold. This may contribute to possible nutrient limitation (no visible symptom), but the sensor reading alone does not confirm the cause of the visible healthy condition.",
                    recommendations      = listOf(
                        "The sensor reading may suggest that nutrients are low, but it does not show exactly which nutrient is missing. Check the plant and have the soil tested if possible.",
                        "If the plant needs feeding, use a complete balanced NPK fertilizer suitable for chili pepper and follow the product label.",
                        "Avoid adding several fertilizers at once."
                    ),
                    isUsingSensors       = true
                )

                // Rule 93: Nitrogen = HIGH -> Potential nutrient or salt imbalance
                nitrogen == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Potential nutrient or salt imbalance",
                    conditionDescription = "The nitrogen reading is high relative to the configured SiliGuide threshold. This may contribute to potential nutrient or salt imbalance, but the sensor reading alone does not confirm the cause of the visible healthy condition.",
                    recommendations      = listOf(
                        "Stop applying extra fertilizer until the soil has been checked.",
                        "Check the fertilizer used recently. If the plant is in a pot and the soil drains well, slowly water the soil to wash away extra fertilizer and let the water drain out.",
                        "Ask an agricultural technician or use a soil test to decide which fertilizer, if any, is needed."
                    ),
                    isUsingSensors       = true
                )

                // Rule 94: Phosphorus = LOW -> Possible nutrient limitation (no visible symptom)
                phosphorus == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible nutrient limitation (no visible symptom)",
                    conditionDescription = "The phosphorus reading is low relative to the configured SiliGuide threshold. This may contribute to possible nutrient limitation (no visible symptom), but the sensor reading alone does not confirm the cause of the visible healthy condition.",
                    recommendations      = listOf(
                        "The sensor reading may suggest that nutrients are low, but it does not show exactly which nutrient is missing. Check the plant and have the soil tested if possible.",
                        "If the plant needs feeding, use a complete balanced NPK fertilizer suitable for chili pepper and follow the product label.",
                        "Avoid adding several fertilizers at once."
                    ),
                    isUsingSensors       = true
                )

                // Rule 95: Phosphorus = HIGH -> Potential nutrient or salt imbalance
                phosphorus == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Potential nutrient or salt imbalance",
                    conditionDescription = "The phosphorus reading is high relative to the configured SiliGuide threshold. This may contribute to potential nutrient or salt imbalance, but the sensor reading alone does not confirm the cause of the visible healthy condition.",
                    recommendations      = listOf(
                        "Stop applying extra fertilizer until the soil has been checked.",
                        "Check the fertilizer used recently. If the plant is in a pot and the soil drains well, slowly water the soil to wash away extra fertilizer and let the water drain out.",
                        "Ask an agricultural technician or use a soil test to decide which fertilizer, if any, is needed."
                    ),
                    isUsingSensors       = true
                )

                // Rule 96: Potassium = LOW -> Possible nutrient limitation (no visible symptom)
                potassium == ThresholdLevel.LOW -> RuleResult(
                    condition            = "Possible nutrient limitation (no visible symptom)",
                    conditionDescription = "The potassium reading is low relative to the configured SiliGuide threshold. This may contribute to possible nutrient limitation (no visible symptom), but the sensor reading alone does not confirm the cause of the visible healthy condition.",
                    recommendations      = listOf(
                        "The sensor reading may suggest that nutrients are low, but it does not show exactly which nutrient is missing. Check the plant and have the soil tested if possible.",
                        "If the plant needs feeding, use a complete balanced NPK fertilizer suitable for chili pepper and follow the product label.",
                        "Avoid adding several fertilizers at once."
                    ),
                    isUsingSensors       = true
                )

                // Rule 97: Potassium = HIGH -> Potential nutrient or salt imbalance
                potassium == ThresholdLevel.HIGH -> RuleResult(
                    condition            = "Potential nutrient or salt imbalance",
                    conditionDescription = "The potassium reading is high relative to the configured SiliGuide threshold. This may contribute to potential nutrient or salt imbalance, but the sensor reading alone does not confirm the cause of the visible healthy condition.",
                    recommendations      = listOf(
                        "Stop applying extra fertilizer until the soil has been checked.",
                        "Check the fertilizer used recently. If the plant is in a pot and the soil drains well, slowly water the soil to wash away extra fertilizer and let the water drain out.",
                        "Ask an agricultural technician or use a soil test to decide which fertilizer, if any, is needed."
                    ),
                    isUsingSensors       = true
                )

                // Rule 59: All Sensors NORMAL -> Healthy Plant
                allNormal -> RuleResult(
                    condition            = "Healthy Plant",
                    conditionDescription = "The plant currently shows no concerning visual symptoms and the sensor readings are within the expected range.",
                    recommendations      = listOf(
                        "Water around the base of the plant when the soil begins to dry. Make sure extra water can drain away.",
                        "Feed the plant with a complete balanced NPK fertilizer suitable for chili pepper. Follow the product label and avoid using too much nitrogen.",
                        "Give the plant enough sunlight, keep the area around it free of weeds, and regularly check the leaves for insects or damage."
                    ),
                    isUsingSensors       = true
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
                conditionDescription = "Unable to determine exact condition from current sensor combination. Characteristics: Uniform vibrant green, smooth margins, firm turgid surface.",
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
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
                ),
                isUsingSensors   = true
            )

            "curling", "leaf_curling" -> RuleResult(
                condition        = "Unknown Condition",
                conditionDescription = "Unable to determine exact condition from current sensor combination. Characteristics: Upward cupping or rolling of leaf margins, downward curling or severe twisting, puckering or crinkling of leaf surface.",
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
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
                ),
                isUsingSensors   = true
            )

            "spots", "leaf_spots" -> RuleResult(
                condition        = "Unknown Condition",
                conditionDescription = "Unable to determine exact condition from current sensor combination. Characteristics: Frog-eye spots (gray center with dark margin), water-soaked lesions, concentric rings, or shot-hole effect.",
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
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
                ),
                isUsingSensors   = true
            )

            "yellowing", "yellowing_leaves" -> RuleResult(
                condition        = "Unknown Condition",
                conditionDescription = "Unable to determine exact condition from current sensor combination. Characteristics: Chlorosis (pale green/yellow older leaves), interveinal chlorosis, or mottling/mosaic patterns.",
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
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
                ),
                isUsingSensors   = true
            )

            else -> RuleResult(
                condition        = "Unknown Condition",
                conditionDescription = "Unrecognized visual symptom with sensor data. Characteristics: raw prediction label. Unable to determine cause without sensor data.",
                characteristics         = listOf(prediction),
                possibleCauses   = listOf("Unable to determine cause without sensor data."),
                recommendations  = listOf(
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
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
                conditionDescription = "Camera-only fallback: unable to evaluate sensor thresholds. Characteristics: Uniform vibrant green color, smooth margins, firm leaf surface.",
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
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
                ),
                isUsingSensors   = false
            )

            "curling", "leaf_curling" -> RuleResult(
                condition        = "Unknown Condition",
                conditionDescription = "Camera-only fallback: unable to evaluate sensor thresholds. Characteristics: Upward cupping or rolling of margins, downward curling, or crinkling.",
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
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
                ),
                isUsingSensors   = false
            )

            "spots", "leaf_spots" -> RuleResult(
                condition        = "Unknown Condition",
                conditionDescription = "Camera-only fallback: unable to evaluate sensor thresholds. Characteristics: Frog-eye spots, water-soaked lesions, target rings, or shot holes.",
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
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
                ),
                isUsingSensors   = false
            )

            "yellowing", "yellowing_leaves" -> RuleResult(
                condition        = "Unknown Condition",
                conditionDescription = "Camera-only fallback: unable to evaluate sensor thresholds. Characteristics: General chlorosis, interveinal chlorosis, or mosaic patterns.",
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
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
                ),
                isUsingSensors   = false
            )

            else -> RuleResult(
                condition        = "Unknown Condition",
                conditionDescription = "Unrecognized visual symptom and no sensor data.",
                characteristics         = listOf(prediction),
                possibleCauses   = listOf("Unable to determine cause without sensor data."),
                recommendations  = listOf(
                    "Check whether the soil is too dry or too wet and adjust watering if needed.",
                    "Look closely at the leaves, including their undersides, for insects, spots, or changes in color.",
                    "Check the sensor readings again. If the cause remains unclear or the plant continues to worsen, ask an agricultural technician for help."
                ),
                isUsingSensors   = false
            )
        }
    }
}