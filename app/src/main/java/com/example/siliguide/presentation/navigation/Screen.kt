package com.example.siliguide.presentation.navigation

import java.net.URLEncoder

// A sealed class prevents typos by hardcoding our route names
sealed class Screen(val route: String) {

    object Welcome : Screen("welcome")
    object GuestInfo : Screen("guest_info")
    object Dashboard : Screen("dashboard")
    object Camera : Screen("camera")

    // Some screens need to pass data, like the URI of the photo we just took
    object CropAdjust : Screen("crop_adjust/{imageUri}") {
        fun createRoute(imageUri: String): String {
            val encodedUri = URLEncoder.encode(imageUri, "UTF-8")
            return "crop_adjust/$encodedUri"
        }
    }

    object AnalysisResult : Screen("analysis_result/{imageUri}") {
        fun createRoute(imageUri: String): String {
            val encodedUri = URLEncoder.encode(imageUri, "UTF-8")
            return "analysis_result/$encodedUri"
        }
    }

    object AddPlant : Screen("add_plant")
    object PlantDetail : Screen("plant/{plantId}") {
        fun createRoute(plantId: String) = "plant/$plantId"
    }
    object IoTSetup : Screen("iot_setup")
    object IoTSetupAP : Screen("iot_setup_ap")
    object IoTSetupLAN : Screen("iot_setup_lan")
    object QrScanner : Screen("qr_scanner")
}