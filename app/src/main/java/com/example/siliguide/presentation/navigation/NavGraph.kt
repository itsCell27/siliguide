package com.example.siliguide.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.example.siliguide.presentation.features.auth.AuthViewModel
import com.example.siliguide.presentation.features.auth.GuestInfoScreen
import com.example.siliguide.presentation.features.auth.WelcomeScreen
import com.example.siliguide.presentation.features.iot.ConnectionMode
import com.example.siliguide.presentation.features.iot.IoTSetupConnectionModeScreen
import com.example.siliguide.presentation.features.iot.ap_setup.IoTSetupAPModeScreen
import com.example.siliguide.presentation.features.iot.components.QrScannerScreen
import com.example.siliguide.presentation.features.plants.AddPlantScreen
import com.example.siliguide.presentation.features.plants.PlantDetailScreen

@Composable
fun SiliGuideNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route // App boots instantly to Dashboard
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        // --- AUTHENTICATION ---
        composable(route = Screen.Welcome.route) {
            WelcomeScreen(
                onLoginClick = { /* TODO */ },
                onSignUpClick = { /* TODO */ },
                onGuestClick = { navController.navigate(Screen.GuestInfo.route) }
            )
        }

        composable(route = Screen.GuestInfo.route) {
            val authViewModel: AuthViewModel = hiltViewModel()

            GuestInfoScreen(
                onContinueAsGuest = {
                    authViewModel.continueAsGuest(
                        onSuccess = {
                            navController.navigate(Screen.Dashboard.route) {
                                // Clear Welcome & Guest screens so user can't hit "Back" to login
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                },
                onCreateAccount = { /* TODO: Route to SignUp */ }
            )
        }

        // --- DASHBOARD (HOME) ---
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToAddPlant = { navController.navigate(Screen.AddPlant.route) },
                onNavigateToAddIoT = { navController.navigate(Screen.IoTSetup.route) },
                onNavigateToPlantDetail = { plantId ->  // ← ADD THIS
                    navController.navigate(Screen.PlantDetail.createRoute(plantId))
                },
                onNavigateToSeeAll = { /* Future: Navigate to history list */ }
            )
        }

        // --- PLANT MANAGEMENT ---
        composable(route = Screen.AddPlant.route) {
            AddPlantScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToCamera = { plantId ->
                    // pass new plant id to camera
                    navController.navigate(Screen.Camera.createRoute(plantId))
                }
            )
        }

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
        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("plantId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId")
            CameraScreen(
                onBack = {
                    if (plantId != null) {
                        // Try to pop back to Detail (works if we came from Detail)
                        val popped = navController.popBackStack(Screen.PlantDetail.route, false)

                        // If it failed (popped == false), it means we came from "Add Plant"
                        if (!popped) {
                            // Navigate forward to Detail instead
                            navController.navigate(Screen.PlantDetail.createRoute(plantId)) {
                                // Remove the "Add Plant" screens from history
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                            }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onImageCaptured = { rawUriString ->
                    navController.navigate(Screen.CropAdjust.createRoute(rawUriString, plantId))
                }
            )
        }

        composable(
            route = Screen.CropAdjust.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("plantId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val decodedUri = URLDecoder.decode(encodedUri, "UTF-8")
            val plantId = backStackEntry.arguments?.getString("plantId")

            CropAdjustScreen(
                rawImageUri = decodedUri,
                onCancel = { navController.popBackStack() },
                onCropped = { croppedUriString ->
                    navController.navigate(Screen.AnalysisResult.createRoute(croppedUriString, plantId)) {
                        // Keep the backstack clean
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.AnalysisResult.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("plantId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val decodedUri = URLDecoder.decode(encodedUri, "UTF-8")
            val plantId = backStackEntry.arguments?.getString("plantId")

            AnalysisResultScreen(
                imageUri = decodedUri,
                onNavigateBack = {
                    if (plantId != null) {
                        // Try to pop back to Detail (works if we came from Detail)
                        val popped = navController.popBackStack(Screen.PlantDetail.route, false)

                        // If it failed (popped == false), it means we came from "Add Plant"
                        if (!popped) {
                            // Navigate forward to Detail instead
                            navController.navigate(Screen.PlantDetail.createRoute(plantId)) {
                                // Remove the "Add Plant" screens from history
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                            }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onSaveAsNote = { /* ... */ }
            )
        }

        composable(
            route = Screen.PlantDetail.route,
            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
            PlantDetailScreen(
                plantId = plantId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAnalysis = {
                    navController.navigate(Screen.Camera.createRoute(plantId))
                }
            )
        }

    }
}