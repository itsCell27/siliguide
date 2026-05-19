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

                // R-19: Nitrogen Toxicity "The Claw" — most dangerous, check first
                nitrogen == ThresholdLevel.HIGH && ec == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Severe Nitrogen Toxicity (\"The Claw\")",
                    conditionDescription = "The excess nitrogen is causing rapid, unbalanced cellular growth, forcing leaves to \"claw\" downwards.",
                    recommendations  = listOf(
                        "Flush the soil immediately",
                        "Halt all nitrogen fertilizers.",
                    ),
                    isUsingSensors   = true
                )

                // R-20: Suspected Bacterial Wilt
                moisture == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Suspected Bacterial Wilt",
                    conditionDescription = "The plant is wilting despite wet soil.",
                    recommendations  = listOf(
                        "Isolate the plant immediately. Do NOT water.",
                        "Check if the plant recovers turgor overnight. If wilting persists by morning, destroy the plant to prevent soil contamination."
                    ),
                    isUsingSensors   = true
                )

                // R-18: Cold Stress
                airTemp == ThresholdLevel.LOW && soilTemp == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Cold Stress / Chilling Injury",
                    conditionDescription = "Capsicum metabolic processes halt below 18°C, causing leaves to droop and curl.",
                    recommendations  = listOf(
                        "Move potted plants indoors or to a warmer location.",
                        "Cover outdoor plants with a thermal blanket at night.",
                        "Avoid watering with cold water — use room-temperature water."
                    ),
                    isUsingSensors   = true
                )

                // R-02: Heat & Light Stress
                airTemp == ThresholdLevel.HIGH && light == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Heat & Light Stress",
                    recommendations  = listOf(
                        "Provide temporary shade using a 30–50% shade net.",
                        "Move the plant away from direct midday sun.",
                        "Ensure good airflow to lower the ambient temperature around the leaves."
                    ),
                    isUsingSensors   = true
                )

                // R-01: Underwatering
                moisture == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Underwatering / Drought Stress",
                    recommendations  = listOf(
                        "Water immediately, applying water gradually to the root zone.",
                        "Continue until soil moisture returns to the 40%–70% range.",
                        "Avoid flooding bone-dry soil instantly to prevent root shock.",
                        "Apply mulch to the soil surface to retain moisture."
                    ),
                    isUsingSensors   = true
                )

                // R-12: All normal → Pest or Virus
                allNormal -> RuleResult(
                    condition        = "Pest or Viral Infestation",
                    conditionDescription = "If the environment is perfect but leaves are curling, pests (Aphids/Thrips) or a virus (PepLCV) are attacking.",
                    recommendations  = listOf(
                        "Inspect for pests immediately.",
                        "Apply Neem oil or insecticidal soap."
                    ),
                    isUsingSensors   = true
                )

                else -> fallbackRule(prediction)
            }

            // ── YELLOWING ────────────────────────────────────────────────────

            "yellowing", "yellowing_leaves" -> when {

                // R-05: NPK all low — most severe deficiency, check before individual
                nitrogen == ThresholdLevel.LOW && phosphorus == ThresholdLevel.LOW && potassium == ThresholdLevel.LOW -> RuleResult(
                    condition        = "General Nutrient Deficiency (NPK)",
                    recommendations  = listOf(
                        "Apply balanced NPK fertilizer.",
                        "Feed the plant with a complete, balanced fertilizer.",
                        "Follow up with a liquid fertilizer for faster uptake."
                    ),
                    isUsingSensors   = true
                )

                // R-14: pH out of range with normal NPK → Lockout
                ph != ThresholdLevel.NORMAL &&
                        nitrogen == ThresholdLevel.NORMAL &&
                        phosphorus == ThresholdLevel.NORMAL &&
                        potassium == ThresholdLevel.NORMAL -> RuleResult(
                    condition = "pH-Induced Nutrient Lockout",
                    conditionDescription = "Nutrients are present but chemically locked.",
                    recommendations = listOf(
                        "Do NOT add more fertilizer — the problem is availability, not quantity.",
                        "If pH is too low (<5.5): Apply agricultural lime.",
                        "If pH is too high (>7.0): Apply elemental sulfur or acidic organic compost.",
                        "Retest soil pH after 2 weeks to confirm correction."
                    ),
                    isUsingSensors   = true
                )

                // R-21: P high + pH normal → Iron/Zinc Lockout
                phosphorus == ThresholdLevel.HIGH && ph == ThresholdLevel.NORMAL -> RuleResult(
                    condition        = "Phosphorus-Induced Iron/Zinc Lockout",
                    conditionDescription = "Excess Phosphorus in the soil chemically blocks the roots from absorbing Iron and Zinc, causing new leaves to yellow.",
                    recommendations  = listOf(
                        "Flush soil and apply foliar micronutrients."
                    ),
                    isUsingSensors   = true
                )

                // R-03: Overwatering
                moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Overwatering / Root Hypoxia",
                    recommendations  = listOf(
                        "Stop watering immediately.",
                        "Check that all drainage holes are clear and functional.",
                        "Do not water again until soil moisture drops below 70%."
                    ),
                    isUsingSensors   = true
                )

                // R-04: Nitrogen deficiency
                nitrogen == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Nitrogen Deficiency",
                    recommendations  = listOf(
                        "Apply a mild, balanced liquid fertilizer or compost tea.",
                        "Avoid heavy synthetic nitrogen (like Urea) if the plant is currently flowering, as excess nitrogen causes blossom drop."
                    ),
                    isUsingSensors   = true
                )

                // R-11: Potassium deficiency
                potassium == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Potassium Deficiency",
                    conditionDescription = "Potassium deficiency often presents as yellowing at the leaf edges.",
                    recommendations  = listOf(
                        "Apply a potassium-rich fertilizer (e.g., Muriate of Potash, 0-0-60)."
                    ),
                    isUsingSensors   = true
                )

                // R-22: Light bleaching
                light == ThresholdLevel.HIGH && airTemp == ThresholdLevel.NORMAL -> RuleResult(
                    condition        = "Light Bleaching (Phototoxicity)",
                    conditionDescription = " The light intensity is so extreme that it is destroying chlorophyll faster than the plant can produce it, causing top leaves to turn pale or yellow.",
                    recommendations  = listOf(
                        "Provide partial shade."
                    ),
                    isUsingSensors   = true
                )

                // R-15: All normal → Micronutrient (Magnesium) deficiency
                allNormal -> RuleResult(
                    condition        = "Micronutrient Deficiency (Magnesium)",
                    conditionDescription = "If NPK is fine but leaves are yellowing (often between the veins), it is a Magnesium deficiency.",
                    recommendations  = listOf(
                        "Apply Epsom salts.",
                        "Apply a Magnesium Sulfate solution as a foliar spray or root drench."
                    ),
                    isUsingSensors   = true
                )

                else -> fallbackRule(prediction)
            }

            // ── SPOTS ────────────────────────────────────────────────────────

            "spots", "leaf_spots" -> when {

                // R-17: Edema — wet + dark, cells burst
                moisture == ThresholdLevel.HIGH && light == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Edema (Cellular Rupture)",
                    conditionDescription = "The plant is absorbing water faster than it can transpire it, causing cells to burst and form blister-like spots. Do NOT apply fungicide.",
                    recommendations  = listOf(
                        "Increase airflow and light around the plant.",
                        "Reduce watering.",
                    ),
                    isUsingSensors   = true
                )

                // R-06: Fungal infection — hot and wet
                moisture == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Fungal Infection Risk",
                    recommendations  = listOf(
                        "Improve airflow and reduce moisture.",
                        "Prune dense foliage and lower leaves.",
                        "Stop overhead watering (keep leaves dry).",
                        "Apply a copper-based fungicide or neem oil as a preventive measure."
                    ),
                    isUsingSensors   = true
                )

                // R-16: Sunscald
                light == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Sunscald / Severe Phototoxicity",
                    conditionDescription = "The spots are papery and necrotic from extreme UV exposure.",
                    recommendations  = listOf(
                        "Install shade netting.",
                        "Remove severely burnt leaves to prevent secondary fungal infections."
                    ),
                    isUsingSensors   = true
                )

                // R-23: Cool-Weather Fungal Blight
                airTemp == ThresholdLevel.LOW && moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Cool-Weather Fungal Blight",
                    conditionDescription = "While many fungi like heat, blights (like Phytophthora) thrive in cool, damp conditions.",
                    recommendations  = listOf(
                        "Apply a systemic fungicide.",
                        "Keep leaves dry.",
                        "Remove spotted leaves immediately."
                    ),
                    isUsingSensors   = true
                )

                // R-07: Salt/Fertilizer burn
                ec == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Salt / Fertilizer Burn",
                    recommendations  = listOf(
                        "Flush the soil.",
                        "Pour a heavy volume of clean, unfertilized water through the soil to leach out excess salts until the EC level drops.",
                        "Halt all fertilization for 2 weeks."
                    ),
                    isUsingSensors   = true
                )

                // R-13: All normal → Active pathogen
                allNormal -> RuleResult(
                    condition        = "Active Bacterial / Fungal Infection",
                    conditionDescription = "Spots appearing when soil/climate sensors are perfect means a primary pathogen (like Cercospora) is present.",
                    recommendations  = listOf(
                        "Apply fungicide.",
                        "Apply copper-based fungicide and avoid wetting leaves."
                    ),
                    isUsingSensors   = true
                )

                else -> fallbackRule(prediction)
            }

            // ── HEALTHY ──────────────────────────────────────────────────────

            "healthy" -> when {

                // R-09: N high + P low → Nitrogen Toxicity risk
                nitrogen == ThresholdLevel.HIGH && phosphorus == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Vegetative Shift Risk (Nitrogen Toxicity)",
                    conditionDescription = "The plant looks great, but high nitrogen during flowering causes flowers to drop.",
                    recommendations  = listOf(
                        "Halt all nitrogen-containing fertilizers immediately.",
                        "Switch to a Phosphorus/Potassium bloom booster."
                    ),
                    isUsingSensors   = true
                )

                // R-10: Air temp high → Blossom drop risk
                airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Heat-Induced Blossom Drop Risk",
                    conditionDescription = "The leaves look perfectly fine, but sustained heat above 35°C denatures chili pollen.",
                    recommendations  = listOf(
                        "Provide afternoon shade."
                    ),
                    isUsingSensors   = true
                )

                // R-08: All normal → Truly healthy
                allNormal -> RuleResult(
                    condition        = "Plant is Healthy",
                    conditionDescription = "Soil parameters are optimal, and no visual symptoms are detected.",
                    recommendations  = listOf(
                        "Maintain current watering and feeding schedule.",
                        "Continue standard monitoring — check soil moisture every 2 days.",
                        "Inspect for early signs of pests weekly."
                    ),
                    isUsingSensors   = true
                )

                else -> fallbackRule(prediction)
            }

            else -> fallbackRule(prediction)
        }
    }

    // ── CAMERA-ONLY FALLBACK (No IoT data) ──────────────────────────────────
    private fun fallbackRule(prediction: String): RuleResult {
        return when (prediction.lowercase()) {

            "healthy" -> RuleResult(
                condition        = "Healthy",
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
                condition        = "Curling",
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
                condition        = "Spots",
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
                condition        = "Yellowing",
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