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
                    symptoms         = listOf("Leaf edges curl downward (\"clawing\")"),
                    possibleCauses   = listOf(
                        "Excess nitrogen causing rapid, unbalanced cellular growth",
                        "High soil EC confirming salt buildup from over-fertilization"
                    ),
                    recommendations  = listOf(
                        "Flush the soil immediately with clean, unfertilized water.",
                        "Halt all nitrogen fertilizers.",
                        "Do not fertilize again until EC drops to the normal range (200–800 µS/cm)."
                    ),
                    isUsingSensors   = true
                )

                // R-20: Suspected Bacterial Wilt
                moisture == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Suspected Bacterial Wilt",
                    symptoms         = listOf("Leaves curling/wilting despite waterlogged soil"),
                    possibleCauses   = listOf(
                        "Plant is wilting even though soil is wet — classic sign of bacterial wilt",
                        "Ralstonia solanacearum thrives in hot, wet conditions"
                    ),
                    recommendations  = listOf(
                        "Isolate the plant immediately. Do NOT water.",
                        "Check if the plant recovers turgor overnight.",
                        "If wilting persists by morning, destroy the plant to prevent soil contamination."
                    ),
                    isUsingSensors   = true
                )

                // R-18: Cold Stress
                airTemp == ThresholdLevel.LOW && soilTemp == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Cold Stress / Chilling Injury",
                    symptoms         = listOf("Leaves drooping and curling due to low temperatures"),
                    possibleCauses   = listOf(
                        "Air temperature below 18°C halts Capsicum metabolic processes",
                        "Cold soil slows water and nutrient uptake"
                    ),
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
                    symptoms         = listOf("Leaf margins curling upward to reduce sun exposure"),
                    possibleCauses   = listOf(
                        "Air temperature exceeds 35°C, triggering a protective curl response",
                        "Extreme light intensity (>50,000 lux) intensifying heat on leaf surface"
                    ),
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
                    symptoms         = listOf("Leaves curling inward to conserve water"),
                    possibleCauses   = listOf(
                        "Soil moisture critically below 40% — plant is dehydrated",
                        "Insufficient watering frequency for current temperature"
                    ),
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
                    symptoms         = listOf("Leaves curling despite optimal environmental conditions"),
                    possibleCauses   = listOf(
                        "Sucking insects: Thrips, Aphids, or Broad Mites draining leaf sap",
                        "Viral infection: Pepper Leaf Curl Virus (PepLCV) or Chili Leaf Curl Virus (ChiLCV) transmitted by Whiteflies"
                    ),
                    recommendations  = listOf(
                        "Inspect leaf undersides closely for tiny insects or webbing.",
                        "Apply Neem oil or insecticidal soap as a first-line treatment.",
                        "Isolate the plant to prevent spread to other plants.",
                        "If stunting and crinkling appear, consult an agricultural extension officer for virus testing."
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
                    symptoms         = listOf("Widespread yellowing across multiple leaves"),
                    possibleCauses   = listOf(
                        "All three major macronutrients (N, P, K) are critically depleted",
                        "Soil pH may be locking out nutrients even if fertilizer was applied"
                    ),
                    recommendations  = listOf(
                        "Apply a complete, balanced NPK fertilizer immediately.",
                        "Check and correct soil pH — nutrients lock out below 5.5 or above 7.0.",
                        "Follow up with a liquid fertilizer for faster uptake.",
                        "Monitor all leaves for improvement over 7–10 days."
                    ),
                    isUsingSensors   = true
                )

                // R-14: pH out of range with normal NPK → Lockout
                ph != ThresholdLevel.NORMAL &&
                        nitrogen == ThresholdLevel.NORMAL &&
                        phosphorus == ThresholdLevel.NORMAL &&
                        potassium == ThresholdLevel.NORMAL -> RuleResult(
                    condition        = "pH-Induced Nutrient Lockout",
                    symptoms         = listOf("Yellowing despite adequate NPK levels in the soil"),
                    possibleCauses   = listOf(
                        "Soil pH is outside the 5.5–7.0 range, chemically binding nutrients",
                        "Nutrients are present but unavailable to the plant roots"
                    ),
                    recommendations  = listOf(
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
                    symptoms         = listOf("Yellowing on new growth and young leaves"),
                    possibleCauses   = listOf(
                        "Excess phosphorus in the soil chemically blocks Iron (Fe) and Zinc (Zn) absorption",
                        "Iron and Zinc deficiency cause interveinal chlorosis on young leaves"
                    ),
                    recommendations  = listOf(
                        "Flush the soil with clean water to leach excess phosphorus.",
                        "Apply a foliar spray of chelated Iron (Fe-EDTA) and Zinc sulfate.",
                        "Halt all phosphorus fertilizers for at least 4 weeks.",
                        "Monitor new leaf growth for recovery."
                    ),
                    isUsingSensors   = true
                )

                // R-03: Overwatering
                moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Overwatering / Root Hypoxia",
                    symptoms         = listOf("Leaves turning yellow due to oxygen-starved roots"),
                    possibleCauses   = listOf(
                        "Waterlogged soil (>70% moisture) suffocates roots, blocking nutrient uptake",
                        "Anaerobic conditions in the root zone promote root rot pathogens"
                    ),
                    recommendations  = listOf(
                        "Stop watering immediately.",
                        "Check that all drainage holes are clear and functional.",
                        "Do not water again until soil moisture drops below 70%.",
                        "If root rot is suspected, remove the plant and trim black/mushy roots before repotting."
                    ),
                    isUsingSensors   = true
                )

                // R-04: Nitrogen deficiency
                nitrogen == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Nitrogen Deficiency",
                    symptoms         = listOf("General yellowing starting from the oldest lower leaves"),
                    possibleCauses   = listOf(
                        "Insufficient nitrogen levels in the soil",
                        "Nitrogen may have leached out from frequent heavy watering"
                    ),
                    recommendations  = listOf(
                        "Apply a mild, balanced liquid fertilizer or compost tea.",
                        "Avoid heavy synthetic nitrogen (like Urea) if the plant is currently flowering — excess N causes blossom drop.",
                        "Monitor lower leaves for greening over 7 days."
                    ),
                    isUsingSensors   = true
                )

                // R-11: Potassium deficiency
                potassium == ThresholdLevel.LOW -> RuleResult(
                    condition        = "Potassium Deficiency",
                    symptoms         = listOf("Yellowing at leaf edges and tips, older leaves affected first"),
                    possibleCauses   = listOf(
                        "Potassium levels below 100 mg/kg",
                        "Potassium is essential for fruit development and is often depleted in fruiting plants"
                    ),
                    recommendations  = listOf(
                        "Apply a potassium-rich fertilizer (e.g., Muriate of Potash, 0-0-60).",
                        "Potassium is mandatory for fruit set — address this before flowering.",
                        "Avoid over-watering, as potassium leaches quickly in wet soil."
                    ),
                    isUsingSensors   = true
                )

                // R-22: Light bleaching
                light == ThresholdLevel.HIGH && airTemp == ThresholdLevel.NORMAL -> RuleResult(
                    condition        = "Light Bleaching (Phototoxicity)",
                    symptoms         = listOf("Upper/younger leaves turning pale, washed-out, or yellow"),
                    possibleCauses   = listOf(
                        "Extreme light intensity (>50,000 lux) destroying chlorophyll faster than the plant produces it",
                        "Top leaves most exposed show symptoms first"
                    ),
                    recommendations  = listOf(
                        "Install a 30–50% shade net.",
                        "Move the plant to a location with bright, indirect light.",
                        "Do not remove bleached leaves immediately — they still contribute some photosynthesis."
                    ),
                    isUsingSensors   = true
                )

                // R-15: All normal → Micronutrient (Magnesium) deficiency
                allNormal -> RuleResult(
                    condition        = "Micronutrient Deficiency (Magnesium)",
                    symptoms         = listOf("Interveinal chlorosis — yellowing between leaf veins, veins stay green"),
                    possibleCauses   = listOf(
                        "Magnesium (Mg) deficiency — NPK sensors are normal but Mg is not tracked",
                        "Possible Calcium (Ca) or Iron (Fe) deficiency with similar visual pattern"
                    ),
                    recommendations  = listOf(
                        "Apply Epsom salts (Magnesium Sulfate) as a foliar spray or root drench.",
                        "Dissolve 1 tablespoon of Epsom salts in 1 liter of water and apply weekly.",
                        "If no improvement in 2 weeks, also check for Calcium and Iron deficiency."
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
                    symptoms         = listOf("Blister-like, water-soaked spots or corky bumps on leaves"),
                    possibleCauses   = listOf(
                        "Plant absorbing water faster than it can transpire — cells burst",
                        "High soil moisture combined with low light reduces transpiration rate"
                    ),
                    recommendations  = listOf(
                        "Increase airflow around the plant.",
                        "Move to a brighter location to boost transpiration.",
                        "Reduce watering immediately.",
                        "Do NOT apply fungicide — this is physiological, not infectious."
                    ),
                    isUsingSensors   = true
                )

                // R-06: Fungal infection — hot and wet
                moisture == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Fungal Infection Risk",
                    symptoms         = listOf("Dark, water-soaked spots or frog-eye lesions on leaves"),
                    possibleCauses   = listOf(
                        "Hot, wet conditions ideal for Cercospora capsici and Colletotrichum spp.",
                        "Bacterial Spot (Xanthomonas) also thrives in high humidity"
                    ),
                    recommendations  = listOf(
                        "Prune dense foliage and lower leaves to improve airflow.",
                        "Stop overhead watering — keep leaves dry.",
                        "Apply copper-based fungicide or Neem oil spray.",
                        "Remove and dispose of heavily spotted leaves."
                    ),
                    isUsingSensors   = true
                )

                // R-16: Sunscald
                light == ThresholdLevel.HIGH && airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Sunscald / Severe Phototoxicity",
                    symptoms         = listOf("Papery, necrotic (bleached/white) spots on sun-exposed leaves"),
                    possibleCauses   = listOf(
                        "Extreme UV and heat directly burning leaf tissue",
                        "Light intensity >50,000 lux combined with >35°C air temperature"
                    ),
                    recommendations  = listOf(
                        "Install 30–50% shade netting immediately.",
                        "Remove severely burnt leaves to prevent secondary fungal colonization.",
                        "Water in the early morning to cool the root zone."
                    ),
                    isUsingSensors   = true
                )

                // R-23: Cool-Weather Fungal Blight
                airTemp == ThresholdLevel.LOW && moisture == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Cool-Weather Fungal Blight",
                    symptoms         = listOf("Large, spreading brown spots appearing in cool, wet conditions"),
                    possibleCauses   = listOf(
                        "Phytophthora or Botrytis blight thriving in cool, damp conditions",
                        "Cool air (<18°C) reduces plant immune response"
                    ),
                    recommendations  = listOf(
                        "Apply a systemic fungicide (e.g., Mancozeb or Metalaxyl) immediately.",
                        "Remove all spotted leaves and dispose of them away from the garden.",
                        "Keep leaves dry — do not water overhead.",
                        "Improve drainage to lower soil moisture."
                    ),
                    isUsingSensors   = true
                )

                // R-07: Salt/Fertilizer burn
                ec == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Salt / Fertilizer Burn",
                    symptoms         = listOf("Brown, crispy spots or scorched leaf edges"),
                    possibleCauses   = listOf(
                        "High soil EC (>800 µS/cm) indicates excess salt buildup from over-fertilization",
                        "Salt draws water out of root cells via osmosis, causing tissue burn"
                    ),
                    recommendations  = listOf(
                        "Flush the soil heavily with clean, unfertilized water.",
                        "Pour water until it runs freely through drainage holes multiple times.",
                        "Halt all fertilization for at least 2 weeks.",
                        "Retest EC — target the 200–800 µS/cm range before fertilizing again."
                    ),
                    isUsingSensors   = true
                )

                // R-13: All normal → Active pathogen
                allNormal -> RuleResult(
                    condition        = "Active Bacterial / Fungal Infection",
                    symptoms         = listOf("Spots appearing despite optimal environmental conditions"),
                    possibleCauses   = listOf(
                        "Primary pathogen (Cercospora capsici) present independent of environment",
                        "Bacterial Spot (Xanthomonas campestris) from infected water splash or tools"
                    ),
                    recommendations  = listOf(
                        "Apply copper-based fungicide immediately.",
                        "Avoid wetting the leaves when watering — water at the base only.",
                        "Remove and destroy all spotted leaves.",
                        "Disinfect garden tools with 70% alcohol between uses."
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
                    symptoms         = listOf("Leaves appear visually healthy — but internal imbalance detected"),
                    possibleCauses   = listOf(
                        "High nitrogen promotes excessive leafy growth at the expense of fruiting",
                        "Low phosphorus blocks flower and fruit development"
                    ),
                    recommendations  = listOf(
                        "Halt all nitrogen-containing fertilizers immediately.",
                        "Switch to a Phosphorus/Potassium bloom booster (e.g., 0-15-15).",
                        "If the plant is flowering, high N will cause flowers to drop."
                    ),
                    isUsingSensors   = true
                )

                // R-10: Air temp high → Blossom drop risk
                airTemp == ThresholdLevel.HIGH -> RuleResult(
                    condition        = "Heat-Induced Blossom Drop Risk",
                    symptoms         = listOf("Leaves look healthy — but high temperature threatens flowers"),
                    possibleCauses   = listOf(
                        "Air temperature above 35°C denatures chili pollen, preventing fertilization",
                        "Flowers will drop even if the plant looks perfectly fine"
                    ),
                    recommendations  = listOf(
                        "Provide afternoon shade to lower ambient temperature.",
                        "Water in the early morning to cool the root zone.",
                        "Consider a light misting of flowers in extreme heat (but avoid wetting leaves)."
                    ),
                    isUsingSensors   = true
                )

                // R-08: All normal → Truly healthy
                allNormal -> RuleResult(
                    condition        = "Plant is Healthy",
                    symptoms         = listOf("Uniform vibrant green leaves, no visual symptoms"),
                    possibleCauses   = listOf("All soil and environmental parameters are within optimal range"),
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
                condition        = "Plant Appears Healthy",
                symptoms         = listOf(
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
                condition        = "Possible Drought, Heat, or Pest Stress",
                symptoms         = listOf(
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
                condition        = "Possible Fungal, Bacterial, or Environmental Damage",
                symptoms         = listOf(
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
                condition        = "Possible Nutrient Deficiency, Overwatering, or Viral Infection",
                symptoms         = listOf(
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
                symptoms         = listOf(prediction),
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