package com.example.siliguide.presentation.features.iot.ap_setup

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.presentation.features.iot.IoTViewModel
import com.example.siliguide.presentation.navigation.Screen

@Composable
fun IoTSetupAPModeScreen(
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    onSetupComplete: () -> Unit
) {
    val viewModel: IoTViewModel = hiltViewModel()
    val connectionState by viewModel.apConnectionState.collectAsState()

    var currentStep by remember { mutableIntStateOf(1) }
    var qrResult by remember { mutableStateOf<String?>(null) }
    var isChecking by remember { mutableStateOf(false) }

    val savedStateHandle = navController.currentBackStackEntry?.savedStateHandle
    val qrResultFromNav = savedStateHandle?.get<String>("qr_result")

    val context = LocalContext.current

    val defaultName = "SiliGuide IoT${viewModel.connectedDeviceId ?: ""}"
    var deviceName by remember { mutableStateOf(defaultName) }

    val apConnectionState by viewModel.apConnectionState.collectAsState()
    val wifiOff = apConnectionState == "WIFI_OFF"

    var showWifiModal by remember { mutableStateOf(false) }

    var previousWifiOff by remember { mutableStateOf(false) }

    LaunchedEffect(wifiOff) {
        if (wifiOff && !previousWifiOff) {
            showWifiModal = true
        }
        previousWifiOff = wifiOff
    }

    // STEP 1: Check connection immediately on entry (Removed legacy monitoring)
    LaunchedEffect(Unit) {
        isChecking = true
        viewModel.forceCheckConnection()
    }

    // STEP 2: Handle QR result from navigation
    LaunchedEffect(qrResultFromNav) {
        qrResultFromNav?.let { result ->
            qrResult = result
            viewModel.connectToDeviceFromQr(result)
            savedStateHandle?.remove<String>("qr_result")
        }
    }

    // STEP 3: Auto move to Step 2 when connection successful
    LaunchedEffect(connectionState) {
        when (connectionState) {
            "CONNECTED" -> {
                isChecking = false
                currentStep = 2
            }
            "FAILED" -> {
                isChecking = false
                // Stay on step 1, user can retry
            }
            "CONNECTING" -> {
                isChecking = true
            }
        }
    }

    // STEP 4: Setup lifecycle listener for when user returns from WiFi settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> {
                    // User came back from WiFi settings - check connection immediately
                    isChecking = true
                    viewModel.forceCheckConnection()
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            // Disconnect WiFi when leaving screen completely
            viewModel.disconnectFromDeviceWifi()
        }
    }



    val handleBackPress: () -> Unit = {
        if (currentStep == 2) {
            // If user go back to step 2, disconnect from device
            viewModel.disconnectFromDeviceWifi()
            currentStep = 1
        } else {
            onNavigateBack()
        }
    }

    // WIFI OFF MODAL
    if (showWifiModal) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .width(300.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = White)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        IconButton(
                            onClick = { showWifiModal = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = GrayMedium,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Icon(
                        Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        "WiFi is Off",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )

                    Text(
                        "Turn on WiFi to connect to your IoT device",
                        fontSize = 13.sp,
                        color = GrayMedium,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                            context.startActivity(intent)
                            showWifiModal = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Open WiFi Settings", color = White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),

        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = handleBackPress) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(OffWhite)
                                .border(1.dp, Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.chevron_left),
                                contentDescription = "Back",
                                tint = GrayBody,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "AP Setup",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GrayMedium
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    val displayStep = if (currentStep == 2) 2 else 1

                    Text(
                        text = "Step $displayStep of 2",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = GreenDark
                    )
                }

                LinearProgressIndicator(
                    progress = {
                        if (currentStep == 2) 1.0f else 0.5f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = GreenPrimary,
                    trackColor = Border,
                )
            }
        },

        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .border(BorderStroke(1.dp, Border))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Button(
                    onClick = {
                        if (currentStep == 1) {
                            if (isChecking) {
                                // If checking, allow user to retry immediately
                                isChecking = true
                                viewModel.forceCheckConnection()
                            } else {
                                // Otherwise go to QR scanner
                                navController.navigate(Screen.QrScanner.route)
                            }
                        } else {
                            // Step 2 - Save device
                            viewModel.finalizeSetup(deviceName)
                            onSetupComplete()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = when {
                            currentStep == 2 -> GreenPrimary
                            isChecking -> GreenMid
                            else -> GreenLight
                        },
                        contentColor = White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isChecking || currentStep == 2
                ) {
                    Text(
                        text = when {
                            currentStep == 1 && isChecking -> "Checking device…"
                            currentStep == 1 -> "Scan QR Code"
                            else -> "Save Device"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },

        containerColor = OffWhite
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {

            Crossfade(
                targetState = currentStep,
                animationSpec = tween(300),
                label = "step_transition"
            ) { step ->

                when (step) {

                    // STEP 1: Connect to Device
                    1 -> APStep1Content(
                        onScanQrClick = {
                            navController.navigate(Screen.QrScanner.route)
                        },
                        isChecking = isChecking,
                        onManualRetry = {
                            isChecking = true
                            viewModel.forceCheckConnection()
                        },
                    )

                    // STEP 2: Device Found - Configure
                    2 -> APStep2Content(
                        viewModel = viewModel,
                        deviceName = deviceName,              // ← PASS DOWN
                        onDeviceNameChange = { newName ->     // ← CALLBACK UP
                            deviceName = newName
                        },
                        onBack = { currentStep = 1 }
                    )
                }
            }
        }
    }
}

@Composable
@Preview
fun APpreview() {
    IoTSetupAPModeScreen(
        navController = androidx.navigation.compose.rememberNavController(),
        onNavigateBack = {},
        onSetupComplete = {}
    )
}