package com.example.siliguide.domain.engine

import com.example.siliguide.domain.model.ThresholdConfig

/**
 * baseline constants for Siling Labuyo
 */
object SiliGuideThresholds {

    // soil sensor
    val SOIL_MOISTURE = ThresholdConfig("Soil Moisture", 40.0, 70.0) // %
    val SOIL_TEMP = ThresholdConfig("Soil Temperature", 20.0, 30.0)  // °C
    val SOIL_PH = ThresholdConfig("Soil pH", 5.5, 7.0)
    val SOIL_EC = ThresholdConfig("Soil EC", 200.0, 800.0)           // µS/cm
    val NITROGEN = ThresholdConfig("Nitrogen", 20.0, 40.0)           // mg/kg
    val PHOSPHORUS = ThresholdConfig("Phosphorus", 10.0, 30.0)       // mg/kg
    val POTASSIUM = ThresholdConfig("Potassium", 100.0, 250.0)       // mg/kg

    // environmental sensors
    val LIGHT_INTENSITY = ThresholdConfig("Light", 10000.0, 50000.0) // lux
    val AIR_TEMP = ThresholdConfig("Air Temperature", 18.0, 35.0)    // °C
}