package com.example.siliguide.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import java.net.URLDecoder
import com.example.siliguide.presentation.features.dashboard.DashboardScreen
import com.example.siliguide.presentation.features.analysis.CameraScreen
import com.example.siliguide.presentation.features.analysis.CropAdjustScreen
import com.example.siliguide.presentation.features.analysis.AnalysisResultScreen
import com.example.siliguide.presentation.features.iot.ConnectionMode
import com.example.siliguide.presentation.features.iot.IoTSetupConnectionModeScreen
import com.example.siliguide.presentation.features.iot.ap_setup.IoTSetupAPModeScreen
import com.example.siliguide.presentation.features.iot.components.QrScannerScreen

@Composable
fun SiliGuideNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route // App boots instantly to Dashboard
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // --- DASHBOARD (HOME) ---
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToAddPlant = { navController.navigate(Screen.AddPlant.route) },
                onNavigateToAddIoT = { navController.navigate(Screen.IoTSetup.route) },
                onNavigateToSeeAll = { /* Future: Navigate to history list */ }
            )
        }

        // --- PLANT MANAGEMENT ---
        composable(Screen.AddPlant.route) { /* AddPlantScreen() */ }

        composable(
            route = Screen.PlantDetail.route,
            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
        ) { /* PlantDetailScreen() */ }

        // --- IOT SETUP WIZARD ---
        composable(route = Screen.IoTSetup.route) {
            IoTSetupConnectionModeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNext = { selectedMode ->
                    when (selectedMode) {
                        ConnectionMode.AP_MODE -> {
                            navController.navigate(Screen.IoTSetupAP.route)
                        }
                        ConnectionMode.LAN_MODE -> {
                            // Assumes you have a Screen.IoTSetupLAN route defined
                            navController.navigate("iot_setup_lan")
                        }
                    }
                }
            )
        }

        composable(Screen.IoTSetupAP.route) {
            IoTSetupAPModeScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() },
                onSetupComplete = {
                    navController.popBackStack(Screen.Dashboard.route, false)
                }
            )
        }

        composable(Screen.QrScanner.route) {
            QrScannerScreen(
                onQrScanned = { result ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("qr_result", result)
                    navController.popBackStack()
                },
                onManualEntry = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }

        // --- ANALYSIS PIPELINE ---
        composable(route = Screen.Camera.route) {
            CameraScreen(
                onBack = { navController.popBackStack() },
                onImageCaptured = { rawUriString ->
                    // Camera gives us a raw image. Send it to the Crop screen
                    navController.navigate(Screen.CropAdjust.createRoute(rawUriString))
                }
            )
        }

        composable(
            route = Screen.CropAdjust.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val decodedUri = URLDecoder.decode(encodedUri, "UTF-8")

            CropAdjustScreen(
                rawImageUri = decodedUri,
                onCancel = { navController.popBackStack() },
                onCropped = { croppedUriString ->
                    // Crop screen gives us the final image. Send it to Analysis
                    navController.navigate(Screen.AnalysisResult.createRoute(croppedUriString)) {
                        // Pop the camera and crop screens off the backstack so the user can't hit "back" into them
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.AnalysisResult.route,
            arguments = listOf(navArgument("imageUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val decodedUri = URLDecoder.decode(encodedUri, "UTF-8")

            AnalysisResultScreen(
                imageUri = decodedUri,
                onNavigateBack = {
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                },
                onSaveAsNote = {
                    // Trigger SaveAnalysisUseCase here later
                }
            )
        }
    }
}