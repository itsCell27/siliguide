package com.example.siliguide.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.siliguide.presentation.features.analysis.AnalysisViewModel
import com.example.siliguide.presentation.features.analysis.PreAnalysisScreen
import com.example.siliguide.presentation.features.analysis.PreAnalysisState
import com.example.siliguide.presentation.features.analysis.PreAnalysisViewModel

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

        // --- PRE-ANALYSIS ---
        composable(
            route = Screen.PreAnalysis.route,arguments = listOf(
                navArgument("plantId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
            val viewModel: PreAnalysisViewModel = hiltViewModel()

            // 1. Collect the state from the ViewModel
            val state by viewModel.state.collectAsState()

            // 2. Load the specific plant and its devices when the screen is entered
            LaunchedEffect(plantId) {
                viewModel.loadData(plantId)
            }

            PreAnalysisScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onContinueToCamera = {
                    // Navigate to Camera with the plantId
                    val deviceId = (state as? PreAnalysisState.LinkedConnected)?.deviceId
                    navController.navigate(Screen.Camera.createRoute(plantId, deviceId))
                },
                onContinueWithOtherDevice = { deviceId ->
                    // Navigate to Camera with both IDs
                    // (Ensure Screen.Camera.createRoute is updated to handle deviceId)
                    navController.navigate(Screen.Camera.createRoute(plantId, deviceId))
                },
                onAddIoTDevice = {
                    navController.navigate(Screen.IoTSetup.route)
                },
                onSkipToImageOnly = {
                    navController.navigate(Screen.Camera.createRoute(plantId))
                },
                onContinueImageOnly = {
                    navController.navigate(Screen.Camera.createRoute(plantId))
                }
            )
        }

        // --- ANALYSIS PIPELINE ---
        composable(
            route = Screen.Camera.route,
            arguments = listOf(
                navArgument("plantId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("deviceId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
        val plantId = backStackEntry.arguments?.getString("plantId")
        val deviceId = backStackEntry.arguments?.getString("deviceId")

        CameraScreen(
            onBack = {
                if (plantId != null) {
                    val popped = navController.popBackStack(Screen.PlantDetail.route, false)
                    if (!popped) {
                        navController.navigate(Screen.PlantDetail.createRoute(plantId)) {
                            popUpTo(Screen.Dashboard.route) { inclusive = false }
                        }
                    }
                } else {
                    navController.popBackStack()
                }
            },
            onImageCaptured = { rawUriString ->
                // Pass BOTH plantId and deviceId to the next screen
                navController.navigate(Screen.CropAdjust.createRoute(rawUriString, plantId, deviceId))
            }
        )
    }

        composable(
            route = Screen.CropAdjust.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("plantId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("deviceId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val decodedUri = URLDecoder.decode(encodedUri, "UTF-8")
            val plantId = backStackEntry.arguments?.getString("plantId")
            val deviceId = backStackEntry.arguments?.getString("deviceId")

            CropAdjustScreen(
                rawImageUri = decodedUri,
                onCancel = { navController.popBackStack() },
                onCropped = { croppedUriString ->
                    // Pass BOTH plantId and deviceId to the result screen
                    navController.navigate(Screen.AnalysisResult.createRoute(croppedUriString, plantId, deviceId)) {
                        popUpTo(Screen.Camera.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.AnalysisResult.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType },
                navArgument("plantId") { type = NavType.StringType; nullable = true; defaultValue = null },
                navArgument("deviceId") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: ""
            val decodedUri = URLDecoder.decode(encodedUri, "UTF-8")
            val plantId = backStackEntry.arguments?.getString("plantId")
            val deviceId = backStackEntry.arguments?.getString("deviceId")

            // Initialize ViewModel and trigger analysis with the passed deviceId
            val viewModel: AnalysisViewModel = hiltViewModel()

            AnalysisResultScreen(
                imageUri = decodedUri,
                plantId = plantId, // Pass ID if needed for UI or saving
                onNavigateBack = {
                    if (plantId != null) {
                        val popped = navController.popBackStack(Screen.PlantDetail.route, false)
                        if (!popped) {
                            navController.navigate(Screen.PlantDetail.createRoute(plantId)) {
                                popUpTo(Screen.Dashboard.route) { inclusive = false }
                            }
                        }
                    } else {
                        navController.popBackStack()
                    }
                },
                onSaveAsNote = {
                    // Logic to save the analysis result
                    plantId?.let { id ->
                        viewModel.saveCurrentAnalysis(
                            plantId = id,
                            imagePath = decodedUri,
                            onSuccess = {
                                // 3. Navigate back to PlantDetail on success
                                val popped = navController.popBackStack(Screen.PlantDetail.route, false)
                                if (!popped) {
                                    navController.navigate(Screen.PlantDetail.createRoute(id)) {
                                        // Clear the analysis pipeline from history
                                        popUpTo(Screen.Dashboard.route) { inclusive = false }
                                    }
                                }
                            }
                        )
                    }
                }
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
                    // CHANGE THIS: Navigate to PreAnalysis first
                    navController.navigate(Screen.PreAnalysis.createRoute(plantId))
                }
            )
        }

    }
}