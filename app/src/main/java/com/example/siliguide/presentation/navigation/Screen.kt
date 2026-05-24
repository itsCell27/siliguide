package com.example.siliguide.presentation.navigation

import java.net.URLEncoder

// A sealed class prevents typos by hardcoding our route names
sealed class Screen(val route: String) {

    object Welcome : Screen("welcome")
    object GuestInfo : Screen("guest_info")
    object Dashboard : Screen("dashboard")
    // Updated to include optional plantId
    object Camera : Screen("camera?plantId={plantId}") {
        fun createRoute(plantId: String? = null): String {
            return if (plantId != null) "camera?plantId=$plantId" else "camera"
        }
    }

    // Updated to include optional plantId
    object CropAdjust : Screen("crop_adjust/{imageUri}?plantId={plantId}") {
        fun createRoute(imageUri: String, plantId: String? = null): String {
            val encodedUri = URLEncoder.encode(imageUri, "UTF-8")
            return if (plantId != null) "crop_adjust/$encodedUri?plantId=$plantId" else "crop_adjust/$encodedUri"
        }
    }

    // Updated to include optional plantId
    object AnalysisResult : Screen("analysis_result/{imageUri}?plantId={plantId}") {
        fun createRoute(imageUri: String, plantId: String? = null): String {
            val encodedUri = URLEncoder.encode(imageUri, "UTF-8")
            return if (plantId != null) "analysis_result/$encodedUri?plantId=$plantId" else "analysis_result/$encodedUri"
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