package com.example.siliguide.presentation.navigation

import java.net.URLEncoder

// A sealed class prevents typos by hardcoding our route names
sealed class Screen(val route: String) {

    object Welcome : Screen("welcome")
    object GuestInfo : Screen("guest_info")
    object Dashboard : Screen("dashboard")

    object PreAnalysis : Screen("pre_analysis/{plantId}") {
        fun createRoute(plantId: String) = "pre_analysis/$plantId"
    }

    // Updated to include optional plantId AND optional deviceId
    object Camera : Screen("camera?plantId={plantId}&deviceId={deviceId}") {
        fun createRoute(plantId: String? = null, deviceId: String? = null): String {
            val base = "camera"
            val params = mutableListOf<String>()
            plantId?.let { params.add("plantId=$it") }
            deviceId?.let { params.add("deviceId=$it") }
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }

    // Updated to include optional plantId AND optional deviceId
    object CropAdjust : Screen("crop_adjust/{imageUri}?plantId={plantId}&deviceId={deviceId}") {
        fun createRoute(imageUri: String, plantId: String? = null, deviceId: String? = null): String {
            val encodedUri = URLEncoder.encode(imageUri, "UTF-8")
            val base = "crop_adjust/$encodedUri"
            val params = mutableListOf<String>()
            plantId?.let { params.add("plantId=$it") }
            deviceId?.let { params.add("deviceId=$it") }
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }

    // Updated to include optional plantId AND optional deviceId
    // Analysis route if the data comes from the camera to crop adjust screen to this route
    object AnalysisResult : Screen("analysis_result/{imageUri}?plantId={plantId}&deviceId={deviceId}") {
        fun createRoute(imageUri: String, plantId: String? = null, deviceId: String? = null): String {
            val encodedUri = URLEncoder.encode(imageUri, "UTF-8")
            val base = "analysis_result/$encodedUri"
            val params = mutableListOf<String>()
            plantId?.let { params.add("plantId=$it") }
            deviceId?.let { params.add("deviceId=$it") }
            return if (params.isEmpty()) base else "$base?${params.joinToString("&")}"
        }
    }

    // Analysis route for past analysis results from the database
    object AnalysisHistoryResult : Screen("analysis_history_result/{sessionId}") {
        fun createRoute(sessionId: String): String {
            return "analysis_history_result/$sessionId"
        }
    }

    object AddPlant : Screen("add_plant")
    object PlantDetail : Screen("plant/{plantId}") {
        fun createRoute(plantId: String) = "plant/$plantId"
    }
    object IoTSetup : Screen("iot_setup")

    object IoTSetupBluetooth : Screen("iot_setup_bluetooth")
    object BluetoothConnectionScreen : Screen("bluetooth_connection")
    object BluetoothDeviceDetailsScreen : Screen("bluetooth_device_details")
    object BluetoothSetupGraph : Screen("bluetooth_setup_graph")

    object IoTSetupAP : Screen("iot_setup_ap")
    object IoTSetupLAN : Screen("iot_setup_lan")
    object QrScanner : Screen("qr_scanner")
}