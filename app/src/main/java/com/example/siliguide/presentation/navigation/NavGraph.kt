package com.example.siliguide.presentation.navigation

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.siliguide.presentation.features.iot.bluetooth_setup.BluetoothSelectionScreen

import com.example.siliguide.core.ui.components.PermissionDialog
import com.example.siliguide.presentation.features.iot.lan_setup.LanApProvisioningScreen
import com.example.siliguide.presentation.features.iot.lan_setup.LanLocationPermissionScreen
import com.example.siliguide.presentation.features.iot.lan_setup.LanPreflightAction
import com.example.siliguide.presentation.features.iot.lan_setup.LanWifiSelectionScreen
import com.example.siliguide.presentation.features.iot.lan_setup.LanWifiUiState
import com.example.siliguide.presentation.features.iot.lan_setup.LanProvisioningMethod
import com.example.siliguide.presentation.features.iot.lan_setup.LanSetupMethodScreen
import com.example.siliguide.presentation.features.iot.lan_setup.LanSetupSuccessScreen
import com.example.siliguide.presentation.features.iot.lan_setup.LanSetupSuccessUiState
import androidx.navigation.navArgument
import com.example.siliguide.core.ui.components.LoadingOverlay
import com.example.siliguide.core.ui.components.MessageDialog
import com.example.siliguide.presentation.features.iot.lan_setup.LanSaveAction
import com.example.siliguide.presentation.features.iot.lan_setup.LanSetupSuccessScreen
import com.example.siliguide.presentation.features.iot.lan_setup.LanSuccessStage
import com.example.siliguide.presentation.features.iot.lan_setup.LanBluetoothProvisioningScreen
import com.example.siliguide.presentation.features.iot.lan_setup.LanBluetoothProvisioningUiState
import com.example.siliguide.presentation.features.iot.device_detail.IoTDeviceDetailScreen
import com.example.siliguide.presentation.features.iot.device_detail.IoTDeviceDetailViewModel

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
                onNavigateToDeviceDetail = { deviceId ->
                    navController.navigate(Screen.IoTDeviceDetail.createRoute(deviceId))
                },
                onNavigateToSeeAll = { /* Future: Navigate to history list */ }
            )
        }


        // --- IOT DEVICE DETAIL ---
        composable(
            route = Screen.IoTDeviceDetail.route,
            arguments = listOf(
                navArgument("deviceId") { type = NavType.StringType }
            )
        ) { backStackEntry ->

            val rawDeviceId = backStackEntry.arguments?.getString("deviceId").orEmpty()
            val deviceId = URLDecoder.decode(rawDeviceId, "UTF-8")

            val viewModel: IoTDeviceDetailViewModel = hiltViewModel()
            val state by viewModel.uiState.collectAsState()

            DisposableEffect(deviceId) {
                viewModel.start(deviceId)
                onDispose { viewModel.stop() }
            }

            IoTDeviceDetailScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onReconnect = { viewModel.retryNow() },
                onLinkPlant = { /* TODO: plant linking */ },
                onUnlinkPlant = { /* TODO: plant unlinking */ },
                onRemoveDevice = { /* TODO: device removal */ },
                onModeChangeConfirmed = { /* TODO: mode switching */ }
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

                LaunchedEffect(Unit) {
                    viewModel.navigateToDeviceSelection.collect {
                        navController.navigate(Screen.BluetoothSelectionScreen.route)
                    }
                }

                BluetoothConnectionScreen(
                    viewModel = viewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.BluetoothSelectionScreen.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.BluetoothSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)
                val context = LocalContext.current
                val pairedDevices by viewModel.pairedDevices.collectAsState()
                val uiState by viewModel.iotUiState.collectAsState()

                val bluetoothSettingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    // This callback runs ONLY when the user closes/returns from the Bluetooth settings screen
                    viewModel.verifyPairedBluetoothDevices()
                }

                LaunchedEffect(Unit) {                              // NEW
                    viewModel.bluetoothVerified.collect {
                        navController.navigate(Screen.BluetoothDeviceDetailsScreen.route)
                    }
                }

                BluetoothSelectionScreen(
                    viewModel = viewModel,
                    devices = pairedDevices,
                    onDeviceSelected = { pairedDevice ->
                        viewModel.selectPairedDevice(pairedDevice)
                    },
                    onOpenBluetoothSettings = {
                        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                        bluetoothSettingsLauncher.launch(intent)
                    },
                    onBack = {
                        navController.popBackStack()
                    },
                    isLoading = uiState.isLoading,
                    message = uiState.loadingMessage
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
                    viewModel = viewModel,

                    deviceName = viewModel.deviceName,
                    onDeviceNameChanged = viewModel::updateDeviceName,

                    availablePlants = emptyList(),
                    selectedPlant = null,
                    onPlantSelected = {},

                    onSave = {
                        viewModel.finalizeBluetoothSetup(customName = viewModel.deviceName)
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


        /**
         * LAN Setup — nested graph so every step shares one IoTViewModel
         */
        navigation(
            route = Screen.LanSetupGraph.route,
            startDestination = Screen.LanSetupMethod.route
        ) {

            // ── Step 1 of 4 — Choose Setup Method ──────────────────────
            composable(Screen.LanSetupMethod.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.LanSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)

                val methodState by viewModel.lanSetupMethodState.collectAsState()

                // Step 1 → Step 2. Both transports need the same permissions,
                // so the choice only decides which Step 4 runs later.
                LaunchedEffect(Unit) {
                    viewModel.lanMethodChosen.collect {
                        if (navController.currentDestination?.route ==
                            Screen.LanSetupMethod.route
                        ) {
                            navController.navigate(Screen.LanLocationPermission.route)
                        }
                    }
                }

                LanSetupMethodScreen(
                    state = methodState,

                    onBack = {
                        viewModel.resetLanSetup()
                        navController.popBackStack()
                    },

                    onSelectMethod = { viewModel.selectLanProvisioningMethod(it) },

                    onContinue = { viewModel.continueFromLanSetupMethod() }
                )
            }

            // ── Step 2 of 4 — Location Permission ──────────────────────
            composable(Screen.LanLocationPermission.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.LanSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)

                // Activity context — PermissionManager needs it to separate a
                // rationale from a permanent denial
                val context = LocalContext.current

                val permissionState by viewModel.lanPermissionState.collectAsState()

                // OS permission prompt
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    viewModel.onLanPermissionResult(
                        context = context,
                        granted = grants.values.all { it }
                    )
                }

                // Returning from Location / WiFi settings — re-run the gate
                val settingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    viewModel.continueFromLanPermission(context)
                }

                // Step 2 → Step 3
                LaunchedEffect(Unit) {
                    viewModel.lanPreflightPassed.collect {

                        // Guard against a double navigate when ON_RESUME and
                        // the settings launcher both fire
                        if (navController.currentDestination?.route ==
                            Screen.LanLocationPermission.route
                        ) {
                            navController.navigate(Screen.LanWifiSelection.createRoute())
                        }
                    }
                }

                // A missing permission needs the OS prompt, not a dialog
                val blocker = viewModel.lanBlocker

                LaunchedEffect(blocker) {
                    if (blocker?.action == LanPreflightAction.REQUEST_PERMISSION) {
                        viewModel.dismissLanBlocker()
                        viewModel.markLanPermissionRequested()
                        permissionLauncher.launch(viewModel.lanPermissionArray)
                    }
                }

                // Keep the artwork honest across app switches
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {

                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.refreshLanPermissionState(context)
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LanLocationPermissionScreen(
                    state = permissionState,

                    // Back returns to Step 1, it does not abandon the wizard
                    onBack = { navController.popBackStack() },

                    onGrantPermission = {
                        viewModel.continueFromLanPermission(context)
                    },

                    // Same gate — permissions are already in, so this falls
                    // through to the Location service and Wi-Fi checks
                    onContinue = {
                        viewModel.continueFromLanPermission(context)
                    },

                    onWhyNeeded = {
                        viewModel.showLanPermissionRationale()
                    },

                    onOpenAppSettings = {
                        PermissionUtils.openAppSettings(context)
                    }
                )

                // Blockers: Location off, WiFi off, permanently denied
                if (blocker != null &&
                    blocker.action != LanPreflightAction.REQUEST_PERMISSION
                ) {
                    PermissionDialog(
                        title = blocker.title,
                        message = blocker.message,

                        confirmText = when (blocker.action) {
                            LanPreflightAction.OPEN_LOCATION_SETTINGS -> "Open Location Settings"
                            LanPreflightAction.OPEN_WIFI_SETTINGS -> "Open WiFi Settings"
                            LanPreflightAction.OPEN_APP_SETTINGS -> "Open App Settings"
                            else -> "OK"
                        },

                        dismissText = "Cancel",

                        onConfirm = {
                            val action = blocker.action

                            viewModel.dismissLanBlocker()

                            when (action) {

                                LanPreflightAction.OPEN_LOCATION_SETTINGS ->
                                    settingsLauncher.launch(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    )

                                LanPreflightAction.OPEN_WIFI_SETTINGS ->
                                    settingsLauncher.launch(
                                        Intent(Settings.ACTION_WIFI_SETTINGS)
                                    )

                                LanPreflightAction.OPEN_APP_SETTINGS ->
                                    PermissionUtils.openAppSettings(context)

                                else -> Unit
                            }
                        },

                        onDismiss = { viewModel.dismissLanBlocker() }
                    )
                }

                // Rationale dialog from "Why is this needed?"
                viewModel.validationDialog?.let { dialog ->
                    PermissionDialog(
                        title = dialog.title,
                        message = dialog.message,
                        confirmText = "OK",
                        dismissText = "Close",
                        onConfirm = { viewModel.dismissValidationDialog() },
                        onDismiss = { viewModel.dismissValidationDialog() }
                    )
                }
            }

            // ── Step 3 of 4 — Connect to Home Wi-Fi ────────────────────
            composable(
                route = Screen.LanWifiSelection.route,
                arguments = listOf(
                    navArgument("deviceId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.LanSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)

                val context = LocalContext.current

                val wifiState by viewModel.lanWifiState.collectAsState()
                val iotUiState by viewModel.iotUiState.collectAsState()

                // Present only when Step 3 is reused to fix a saved device
                val reconfigureDeviceId = backStackEntry.arguments
                    ?.getString("deviceId")
                    ?.takeIf { it.isNotBlank() && !it.startsWith("{") }

                val hasReconfiguredDeviceId = reconfigureDeviceId != null

                LaunchedEffect(reconfigureDeviceId) {
                    if (reconfigureDeviceId != null) {
                        viewModel.startLanWifiReconfigure(reconfigureDeviceId)
                    } else {
                        viewModel.clearLanWifiReconfigure()
                    }
                }

                // Device is already claimed and saved — there is nowhere sane to
                // go back to, so swallow system back while re-provisioning
                BackHandler(enabled = reconfigureDeviceId != null) {
                    viewModel.finishDeviceWiFiSetupDialog()
                }

                // Re-provisioning worked — back to Success for the hand-off
                LaunchedEffect(Unit) {
                    viewModel.lanHandoffReady.collect {

                        if (navController.currentDestination?.route !=
                            Screen.LanWifiSelection.route
                        ) {
                            return@collect
                        }

                        val popped = navController.popBackStack(
                            route = Screen.LanSetupSuccess.route,
                            inclusive = false
                        )

                        if (!popped) {
                            navController.navigate(Screen.LanSetupSuccess.route)
                        }
                    }
                }

                // Returning from Location / WiFi settings — scan again
                val settingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    viewModel.startLanWifiScan(context)
                }

                // First scan on entry
                LaunchedEffect(Unit) {
                    viewModel.startLanWifiScan(context)
                }

                // Step 3 → Step 4a (AP) or Step 4b (Bluetooth), decided by
                // the method chosen back on Step 1
                LaunchedEffect(Unit) {
                    viewModel.lanCredentialsReady.collect {

                        if (navController.currentDestination?.route !=
                            Screen.LanWifiSelection.route
                        ) {
                            return@collect
                        }

                        when (viewModel.lanProvisioningMethod) {

                            LanProvisioningMethod.ACCESS_POINT ->
                                navController.navigate(Screen.LanApProvisioning.route)

                            LanProvisioningMethod.BLUETOOTH ->
                                navController.navigate(Screen.LanBluetoothProvisioning.route)
                        }
                    }
                }

                // A radio flipped off while the user sat here — rescan on resume
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {

                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME) {
                            viewModel.startLanWifiScan(context)
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                // Re-provisioning succeeded — device is on the LAN and saved
                LaunchedEffect(Unit) {
                    viewModel.lanSetupFinished.collect {
                        viewModel.resetLanSetup()

                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.LanSetupGraph.route) { inclusive = true }
                        }
                    }
                }

                LoadingOverlay(
                    isLoading = iotUiState.isLoading,
                    message = iotUiState.loadingMessage
                ) {
                    LanWifiSelectionScreen(
                        state = wifiState,
                        onBack = { navController.popBackStack() },
                        hasReconfiguredDeviceId = hasReconfiguredDeviceId,
                        onRefresh = { viewModel.startLanWifiScan(context) },
                        onSelectNetwork = { viewModel.selectLanWifiNetwork(it) },
                        onPasswordChange = { viewModel.updateLanWifiPassword(it) },
                        onTogglePasswordVisibility = { viewModel.toggleLanWifiPasswordVisibility() },
                        onContinue = { viewModel.continueFromLanWifi() }
                    )
                }

                // WiFi off, Location off, permission revoked
                val blocker = viewModel.lanBlocker

                if (blocker != null) {
                    PermissionDialog(
                        title = blocker.title,
                        message = blocker.message,

                        confirmText = when (blocker.action) {
                            LanPreflightAction.OPEN_WIFI_SETTINGS -> "Open WiFi Settings"
                            LanPreflightAction.OPEN_LOCATION_SETTINGS -> "Open Location Settings"
                            else -> "Open App Settings"
                        },

                        dismissText = "Cancel",

                        onConfirm = {
                            val action = blocker.action

                            viewModel.dismissLanBlocker()

                            when (action) {

                                LanPreflightAction.OPEN_WIFI_SETTINGS ->
                                    settingsLauncher.launch(
                                        Intent(Settings.ACTION_WIFI_SETTINGS)
                                    )

                                LanPreflightAction.OPEN_LOCATION_SETTINGS ->
                                    settingsLauncher.launch(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    )

                                // Permission was revoked mid-flow — settings is
                                // the only reliable way back
                                LanPreflightAction.REQUEST_PERMISSION,
                                LanPreflightAction.OPEN_APP_SETTINGS ->
                                    PermissionUtils.openAppSettings(context)
                            }
                        },

                        onDismiss = { viewModel.dismissLanBlocker() }
                    )
                }

                // "Password Required" and other validation copy
                viewModel.validationDialog?.let { dialog ->
                    PermissionDialog(
                        title = dialog.title,
                        message = dialog.message,
                        confirmText = "OK",
                        dismissText = "Close",
                        onConfirm = { viewModel.dismissValidationDialog() },
                        onDismiss = { viewModel.dismissValidationDialog() }
                    )
                }

                // Re-provisioning failure — let the user try another network
                viewModel.lanSaveDialog?.let { dialog ->
                    MessageDialog(
                        title = dialog.title,
                        message = dialog.message,
                        confirmText = dialog.confirmText,
                        onConfirm = { viewModel.dismissLanSaveDialog() }
                    )
                }
            }

            // ── Step 4 of 4 — Device Hotspot (AP) Provisioning Path ────
            composable(Screen.LanApProvisioning.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.LanSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)

                val apState by viewModel.lanApProvisioningState.collectAsState()

                // Coming back from Wi-Fi settings is the cue to ping the device
                val wifiSettingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    viewModel.forceCheckConnection()
                }

                // Start mirroring apConnectionState onto the wizard state
                DisposableEffect(Unit) {
                    viewModel.startLanApProvisioning()
                    onDispose { viewModel.stopLanApProvisioning() }
                }

                // Verified an unclaimed SiliGuide device → Success
                LaunchedEffect(Unit) {
                    viewModel.lanApVerified.collect {
                        if (navController.currentDestination?.route ==
                            Screen.LanApProvisioning.route
                        ) {
                            navController.navigate(Screen.LanSetupSuccess.route)
                        }
                    }
                }

                LanApProvisioningScreen(
                    state = apState,

                    onBack = {
                        viewModel.disconnectFromDeviceWifi()
                        navController.popBackStack()
                    },

                    onOpenWifiSettings = {
                        wifiSettingsLauncher.launch(Intent(Settings.ACTION_WIFI_SETTINGS))
                    },

                    onRetry = { viewModel.retryLanApProvisioning() },

                    onSwitchToBluetooth = {
                        viewModel.selectLanProvisioningMethod(
                            LanProvisioningMethod.BLUETOOTH
                        )
                        navController.navigate(Screen.LanBluetoothProvisioning.route) {
                            popUpTo(Screen.LanApProvisioning.route) { inclusive = true }
                        }
                    }
                )

                // Unsupported device, already registered, already claimed,
                // WiFi off, unreachable — all raised by verifyEsp32ConnectionViaIp
                viewModel.validationDialog?.let { dialog ->
                    PermissionDialog(
                        title = dialog.title,
                        message = dialog.message,
                        confirmText = "Open WiFi Settings",
                        dismissText = "Close",
                        onConfirm = {
                            viewModel.dismissValidationDialog()
                            wifiSettingsLauncher.launch(Intent(Settings.ACTION_WIFI_SETTINGS))
                        },
                        onDismiss = { viewModel.dismissValidationDialog() }
                    )
                }
            }


            // ── Step 4 of 4 — Bluetooth Provisioning Path ──────────────
            composable(Screen.LanBluetoothProvisioning.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.LanSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)

                val context = LocalContext.current

                val btState by viewModel.lanBluetoothState.collectAsState()
                val iotUiState by viewModel.iotUiState.collectAsState()

                // BLUETOOTH_CONNECT on API 31+
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { grants ->
                    if (grants.values.all { it }) {
                        viewModel.startLanBluetoothProvisioning(context)
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.lanBluetoothPermissionNeeded.collect {
                        viewModel.markLanBluetoothPermissionRequested()
                        permissionLauncher.launch(viewModel.lanBluetoothPermissionArray)
                    }
                }

                // Returning from Bluetooth Settings is the cue to look again
                val btSettingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    viewModel.startLanBluetoothProvisioning(context)
                }

                // Verified a paired, unclaimed SiliGuide device → Success
                LaunchedEffect(Unit) {
                    viewModel.lanBluetoothVerified.collect {
                        if (navController.currentDestination?.route ==
                            Screen.LanBluetoothProvisioning.route
                        ) {
                            navController.navigate(Screen.LanSetupSuccess.route)
                        }
                    }
                }

                // Pairing happens in Android Settings, so re-check on resume
//                val lifecycleOwner = LocalLifecycleOwner.current
//
//                DisposableEffect(lifecycleOwner) {
//
//                    val observer = LifecycleEventObserver { _, event ->
//                        if (event == Lifecycle.Event.ON_RESUME) {
//                            viewModel.startLanBluetoothProvisioning(context)
//                        }
//                    }
//
//                    lifecycleOwner.lifecycle.addObserver(observer)
//
//                    onDispose {
//                        lifecycleOwner.lifecycle.removeObserver(observer)
//                    }
//                }



                LoadingOverlay(
                    isLoading = iotUiState.isLoading,
                    message = iotUiState.loadingMessage
                ) {
                    LanBluetoothProvisioningScreen(
                        state = btState,

                        onBack = {
                            viewModel.stopLanBluetoothProvisioning()
                            navController.popBackStack()
                        },

                        onOpenBluetoothSettings = {
                            btSettingsLauncher.launch(
                                Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            )
                        },

                        onSelectDevice = { viewModel.selectLanBluetoothDevice(it) },

                        onRetry = { viewModel.retryLanBluetoothProvisioning(context) },

                        onSwitchToDeviceHotspot = {
                            viewModel.stopLanBluetoothProvisioning()
                            viewModel.selectLanProvisioningMethod(
                                LanProvisioningMethod.ACCESS_POINT
                            )
                            navController.navigate(Screen.LanApProvisioning.route) {
                                popUpTo(Screen.LanBluetoothProvisioning.route) {
                                    inclusive = true
                                }
                            }
                        }
                    )
                }

                // Bluetooth off, permission denied, already registered / claimed
                viewModel.validationDialog?.let { dialog ->
                    PermissionDialog(
                        title = dialog.title,
                        message = dialog.message,
                        confirmText = "Open Bluetooth Settings",
                        dismissText = "Close",
                        onConfirm = {
                            viewModel.dismissValidationDialog()
                            btSettingsLauncher.launch(
                                Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                            )
                        },
                        onDismiss = { viewModel.dismissValidationDialog() }
                    )
                }
            }


            // ── Success — reached from either provisioning path ─────────
            composable(Screen.LanSetupSuccess.route) { backStackEntry ->

                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry(Screen.LanSetupGraph.route)
                }

                val viewModel: IoTViewModel = hiltViewModel(parentEntry)

                val context = LocalContext.current

                val successState = LanSetupSuccessUiState(
                    stage = viewModel.lanSuccessStage,
                    provisioningPath = viewModel.lanProvisioningPathLabel,
                    deviceName = viewModel.deviceName,
                    linkedPlantName = null,
                    isSaving = viewModel.isFinalizingLan,
                    targetSsid = viewModel.lanSelectedSsid.orEmpty(),
                    deviceIp = viewModel.lanDeviceIp,
                    switchFailed = viewModel.lanSwitchFailed
                )

                // Coming back from Wi-Fi settings is the cue to re-check
                val wifiSettingsLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { _ ->
                    viewModel.verifyPhoneOnDeviceNetwork()
                }

                /**
                 * Settings.Panel.ACTION_WIFI shows an inline Wi-Fi sheet with no
                 * app chooser. ACTION_WIFI_SETTINGS matches several activities on
                 * some OEM builds, which is why an "Open with" dialog appeared.
                 */
                fun openWifiPicker() {
                    val intent =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Intent(Settings.Panel.ACTION_WIFI)
                        } else {
                            Intent(Settings.ACTION_WIFI_SETTINGS)
                        }

                    wifiSettingsLauncher.launch(intent)
                }

                // Phone verified on the same network as the device → Dashboard
                LaunchedEffect(Unit) {
                    viewModel.lanSetupFinished.collect {
                        viewModel.resetLanSetup()

                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.LanSetupGraph.route) { inclusive = true }
                        }
                    }
                }

                BackHandler(enabled = viewModel.lanSuccessStage != LanSuccessStage.NAMING) {
                    viewModel.connectToSameWiFiDialog()
                }

                // The user may switch networks from the notification shade
                // without ever leaving the app, so ON_RESUME re-checks too
                val lifecycleOwner = LocalLifecycleOwner.current

                DisposableEffect(lifecycleOwner) {

                    val observer = LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_RESUME &&
                            viewModel.lanSuccessStage == LanSuccessStage.SWITCH_NETWORK
                        ) {
                            viewModel.verifyPhoneOnDeviceNetwork()
                        }
                    }

                    lifecycleOwner.lifecycle.addObserver(observer)

                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LanSetupSuccessScreen(
                    state = successState,

                    onBack = {
                        // Backing out mid-hand-off would abandon a saved device
                        if (viewModel.lanSuccessStage == LanSuccessStage.NAMING) {
                            viewModel.resetLanApVerification()
                            navController.popBackStack()
                        }
                    },

                    onDeviceNameChange = { viewModel.updateDeviceName(it) },

                    onPickPlant = {
                        // TODO: plant picker — linking stays optional for now
                    },

                    onSaveDevice = {
                        when (viewModel.lanProvisioningMethod) {
                            LanProvisioningMethod.ACCESS_POINT ->
                                viewModel.finalizeLanAPMode()

                            LanProvisioningMethod.BLUETOOTH ->
                                viewModel.finalizeLanBluetoothMode()
                        }
                    },

                    onOpenWifiSettings = { openWifiPicker() },

                    onCheckConnection = { viewModel.verifyPhoneOnDeviceNetwork() }
                )

                // Claim / save / provisioning failures
                viewModel.lanSaveDialog?.let { dialog ->
                    MessageDialog(
                        title = dialog.title,
                        message = dialog.message,
                        confirmText = dialog.confirmText,
                        onConfirm = {
                            val action = dialog.action
                            val deviceId = viewModel.connectedDeviceId

                            viewModel.dismissLanSaveDialog()

                            if (action == LanSaveAction.RECONFIGURE_WIFI && deviceId != null) {
                                navController.navigate(
                                    Screen.LanWifiSelection.createRoute(deviceId)
                                )
                            }
                        }
                    )
                }

                if (viewModel.lanSaveDialog == null) {
                    viewModel.validationDialog?.let { dialog ->
                        MessageDialog(
                            title = dialog.title,
                            message = dialog.message,
                            confirmText = "Got it",
                            onConfirm = { viewModel.dismissValidationDialog() }
                        )
                    }
                }
            }

        }

        // Old entry point kept so existing callers keep working
        composable(Screen.IoTSetupLAN.route) {
            LaunchedEffect(Unit) {
                navController.navigate(Screen.LanSetupGraph.route) {
                    popUpTo(Screen.IoTSetupLAN.route) { inclusive = true }
                }
            }
        }
        /**
         * LAN Setup End
         */


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
                viewModel = viewModel,

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