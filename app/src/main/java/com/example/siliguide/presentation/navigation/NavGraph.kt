package com.example.siliguide.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.siliguide.presentation.features.auth.GuestInfoScreen
import com.example.siliguide.presentation.features.auth.WelcomeScreen
import com.example.siliguide.presentation.features.iot.ConnectionMode
import com.example.siliguide.presentation.features.iot.IoTSetupConnectionModeScreen
import com.example.siliguide.presentation.features.iot.ap_setup.IoTSetupAPModeScreen
import com.example.siliguide.presentation.features.iot.components.QrScannerScreen

@Composable
fun SiliGuideNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Welcome.route
) {
    val startupViewModel: StartupViewModel = hiltViewModel()

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startupViewModel.checkSession { hasUser ->
            startDestination = if (hasUser) {
                Screen.Dashboard.route
            } else {
                Screen.Welcome.route
            }
        }
    }

    // Prevent NavHost from building before decision
    if (startDestination == null) return
    NavHost(
        navController = navController,
        startDestination = startDestination!!
    ) {

        composable(Screen.Login.route) { /* LoginScreen() */ }
        composable(Screen.SignUp.route) { /* SignUpScreen() */ }
        composable(Screen.AddPlant.route) { /* AddPlantScreen() */ }
        composable(Screen.Profile.route) { /* ProfileScreen() */ }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onLogin = { },
                onSignUp = { },
                onContinueGuest = {
                    navController.navigate(Screen.GuestInfo.route)
                }
            )
        }

        composable(Screen.GuestInfo.route) {
            GuestInfoScreen(
                onContinueSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                },
                onCreateAccount = { }
            )
        }

        composable(route = Screen.IoTSetup.route) {
            IoTSetupConnectionModeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNext = { selectedMode ->
                    when (selectedMode) {
                        ConnectionMode.AP_MODE -> {
                            navController.navigate(Screen.IoTSetupAP.route)
                        }
                        ConnectionMode.LAN_MODE -> {
                            navController.navigate(Screen.IoTSetupLAN.route)
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
                onManualEntry = {
                    navController.popBackStack()
                },
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToCamera = { navController.navigate(Screen.Camera.route) },
                onNavigateToAddPlant = { navController.navigate(Screen.AddPlant.route) },
                onNavigateToAddIoT = { navController.navigate(Screen.IoTSetup.route) },
                onNavigateToProfile = { navController.navigate(Screen.Profile.route) },
                onNavigateToSeeAll = { /* Future: Navigate to history list */ }
            )
        }

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
                onNavigateBack = { // Replaced 'onBack'
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                },
                onSaveAsNote = {
                    // We will trigger the SaveAnalysisUseCase here next
                }
            )
        }

        composable(
            route = Screen.PlantDetail.route,
            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
        ) { /* PlantDetailScreen() */ }
    }
}