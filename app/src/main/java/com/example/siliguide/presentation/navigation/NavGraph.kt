package com.example.siliguide.presentation.navigation

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.presentation.features.iot.IoTSetupConnectionModeScreen
import com.example.siliguide.presentation.features.iot.ap_setup.IoTSetupAPModeScreen
import com.example.siliguide.presentation.features.iot.components.QrScannerScreen
import com.example.siliguide.presentation.features.plants.AddPlantScreen
import com.example.siliguide.presentation.features.plants.PlantDetailScreen
import androidx.compose.runtime.getValue
import com.example.siliguide.presentation.features.analysis.AnalysisViewModel
import com.example.siliguide.presentation.features.analysis.PreAnalysisScreen
import com.example.siliguide.presentation.features.analysis.PreAnalysisState
import com.example.siliguide.presentation.features.analysis.PreAnalysisViewModel
import com.example.siliguide.presentation.features.iot.IoTViewModel
import com.example.siliguide.presentation.features.iot.bluetooth_setup.BluetoothConnectionScreen
import com.example.siliguide.presentation.features.iot.bluetooth_setup.BluetoothDeviceDetailsScreen
import com.example.siliguide.presentation.features.iot.bluetooth_setup.BluetoothIntroScreen
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.navigation
import com.example.siliguide.core.permission.PermissionStatus
import com.example.siliguide.core.permission.PermissionUtils

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
                onNavigateToPlantDetail = { plantId ->
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

                    navController.navigate(
                        Screen.PreAnalysis.createRoute(plantId)
                    ) {

                        popUpTo(Screen.AddPlant.route) {
                            inclusive = true
                        }
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
                    // Navigate to PreAnalysis first
                    navController.navigate(Screen.PreAnalysis.createRoute(plantId))
                },
                onNavigateToAnalysisDetail = { sessionId ->
                    navController.navigate(Screen.AnalysisHistoryResult.createRoute(sessionId))
                }
            )
        }
        // --- PLANT MANAGEMENT ---



        // --- IOT SETUP WIZARD ---
        composable(route = Screen.IoTSetup.route) {
            IoTSetupConnectionModeScreen(
                onNavigateBack = { navController.popBackStack() },
                onNext = { selectedMode ->
                    when (selectedMode) {
                        ConnectionMode.AP -> {
                            navController.navigate(Screen.IoTSetupAP.route)
                        }
                        ConnectionMode.BLUETOOTH -> {
                            navController.navigate(Screen.BluetoothSetupGraph.route)
                        }
                        ConnectionMode.LAN -> {
                            navController.navigate(Screen.IoTSetupLAN.route)
                        }
                    }
                }
            )
        }

        // Bluetooth Setup
        navigation(
            route = Screen.BluetoothSetupGraph.route,
            startDestination = Screen.IoTSetupBluetooth.route
        ) {

            composable(Screen.IoTSetupBluetooth.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.BluetoothSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)

                BluetoothIntroScreen(
                    onContinue = {
                        viewModel.startBluetoothSetup()

                        navController.navigate(
                            Screen.BluetoothConnectionScreen.route
                        )
                    },
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.BluetoothConnectionScreen.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.BluetoothSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)

                val bluetoothUiState by viewModel.bluetoothSetupState.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.bluetoothVerified.collect {
                        navController.navigate(
                            Screen.BluetoothDeviceDetailsScreen.route
                        )
                    }
                }

                BluetoothConnectionScreen(
                    uiState = bluetoothUiState,

                    onOpenBluetoothSettings = {

                        val intent = android.content.Intent(
                            android.provider.Settings.ACTION_BLUETOOTH_SETTINGS
                        )

                        navController.context.startActivity(intent)
                    },

                    onVerifyDevices = {
                        viewModel.verifyPairedBluetoothDevices()
                    },

                    onRetry = {
                        viewModel.resetBluetoothSetup()
                    },

                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.BluetoothDeviceDetailsScreen.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.BluetoothSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)

                LaunchedEffect(Unit) {
                    viewModel.setupComplete.collect {
                        navController.popBackStack(
                            Screen.Dashboard.route,
                            false
                        )
                    }
                }

                BluetoothDeviceDetailsScreen(

                    deviceName = viewModel.deviceName,
                    onDeviceNameChanged = viewModel::updateDeviceName,

                    availablePlants = emptyList(),
                    selectedPlant = null,
                    onPlantSelected = {},

                    onSave = {
                        viewModel.finalizeSetup(
                            customName = viewModel.deviceName,
                            connectionMode = ConnectionMode.BLUETOOTH
                        )
                    },

                    onBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        // AP Setup
        composable(Screen.IoTSetupAP.route) {
            IoTSetupAPModeScreen(
                navController = navController,
                onNavigateBack = { navController.popBackStack() },
                onSetupComplete = {
                    navController.popBackStack(Screen.Dashboard.route, false)
                }
            )
        }

        composable(Screen.IoTSetupLAN.route) {
            // TODO: Replace this with your actual LAN Setup Screen Composable
            Column() {
                Button(onClick = { navController.popBackStack() }) {
                    Text("Coming Soon, Go Back")
                }
            }
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
            val context = LocalContext.current
            val permissionLauncher =
                rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) {
                    viewModel.refreshCameraPermission(context)
                }

            // 1. Collect the state from the ViewModel
            val state by viewModel.state.collectAsState()

            // 2. Load the specific plant and its devices when the screen is entered
            LaunchedEffect(plantId) {
                viewModel.loadData(plantId)
            }

            LaunchedEffect(Unit) {
                viewModel.refreshCameraPermission(context)
            }

            val lifecycleOwner = LocalLifecycleOwner.current

            DisposableEffect(lifecycleOwner) {

                val observer =
                    LifecycleEventObserver { _, event ->

                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshCameraPermission(context)
                        }
                    }

                lifecycleOwner.lifecycle.addObserver(observer)

                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            fun continueToCamera(
                navigate: () -> Unit
            ) {

                when (viewModel.permissionStatus) {

                    PermissionStatus.Granted -> {

                        navigate()
                    }

                    PermissionStatus.RequestPermission -> {

                        viewModel.markCameraPermissionRequested()

                        permissionLauncher.launch(
                            Manifest.permission.CAMERA
                        )
                    }

                    PermissionStatus.ShowRationale -> {

                        viewModel.showPermissionDialog()
                    }

                    PermissionStatus.PermanentlyDenied -> {

                        viewModel.showPermissionDialog()
                    }
                }
            }

            PreAnalysisScreen(

                state = state,

                permissionStatus = viewModel.permissionStatus,

                onBack = {
                    navController.popBackStack()
                },

                showPermissionDialog =
                    viewModel.shouldShowPermissionDialog,

                onRequestCameraPermission = {
                    viewModel.hidePermissionDialog()

                    viewModel.markCameraPermissionRequested()

                    permissionLauncher.launch(
                        Manifest.permission.CAMERA
                    )
                },

                onOpenSettings = {
                    viewModel.hidePermissionDialog()

                    PermissionUtils.openAppSettings(context)
                },

                onPermissionDenied = {
                    viewModel.hidePermissionDialog()
                },

                onContinueToCamera = {

                    continueToCamera {

                        val deviceId =
                            (state as? PreAnalysisState.LinkedConnected)
                                ?.deviceId

                        navController.navigate(
                            Screen.Camera.createRoute(
                                plantId,
                                deviceId
                            )
                        )
                    }
                },

                onContinueWithOtherDevice = { deviceId ->

                    continueToCamera {

                        navController.navigate(
                            Screen.Camera.createRoute(
                                plantId,
                                deviceId
                            )
                        )
                    }
                },

                onSkipToImageOnly = {

                    continueToCamera {

                        navController.navigate(
                            Screen.Camera.createRoute(plantId)
                        )
                    }
                },

                onContinueImageOnly = {

                    continueToCamera {

                        navController.navigate(
                            Screen.Camera.createRoute(plantId)
                        )
                    }
                },

                onAddIoTDevice = {

                    navController.navigate(
                        Screen.IoTSetup.route
                    )
                }
            )
        }

        // --- START ANALYSIS PIPELINE ---
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

        // Analysis Result route for live data
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
                    viewModel.clearCurrentCache()
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
                                viewModel.clearCurrentCache()
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

        // Analysis Result route that the data comes from history or database
        composable(
            route = Screen.AnalysisHistoryResult.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->

            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""

            val viewModel: AnalysisViewModel = hiltViewModel()

            LaunchedEffect(sessionId) {
                viewModel.loadAnalysisSession(sessionId)
            }

            AnalysisResultScreen(
                imageUri = "",
                plantId = null,

                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaveAsNote = { },
                isHistoryMode = true
            )
        }
        // --- END ANALYSIS PIPELINE ---




    }
}