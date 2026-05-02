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

@Composable
fun SiliGuideNavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Dashboard.route
) {
    NavHost(navController = navController, startDestination = startDestination) {

        composable(Screen.Welcome.route) { /* WelcomeScreen() */ }
        composable(Screen.Login.route) { /* LoginScreen() */ }
        composable(Screen.SignUp.route) { /* SignUpScreen() */ }
        composable(Screen.AddPlant.route) { /* AddPlantScreen() */ }
        composable(Screen.Profile.route) { /* ProfileScreen() */ }
        composable(Screen.IoTSetup.route) { /* IoTSetupScreen() */ }

        composable(route = Screen.Dashboard.route) {
            DashboardScreen(onNavigateToCamera = { navController.navigate(Screen.Camera.route) })
        }

        composable(route = Screen.Camera.route) {
            CameraScreen(
                onBack = { navController.popBackStack() },
                onImageCaptured = { rawUriString ->
                    // Camera gives us a raw image. Send it to the Crop screen!
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
                    // Crop screen gives us the final image. Send it to Analysis!
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
                imageUri = decodedUri, // We now pass the decoded URI here!
                onBack = {
                    navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                }
            )
        }

        composable(
            route = Screen.PlantDetail.route,
            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
        ) { /* PlantDetailScreen() */ }
    }
}