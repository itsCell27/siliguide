package com.example.siliguide.presentation.features.iot.bluetooth_setup

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.siliguide.core.permission.PermissionStatus
import com.example.siliguide.core.permission.PermissionType
import com.example.siliguide.core.permission.PermissionUtils
import com.example.siliguide.core.ui.components.PermissionDialog
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.Info
import com.example.siliguide.core.ui.theme.InfoBg
import com.example.siliguide.core.ui.theme.InfoBlue
import com.example.siliguide.core.ui.theme.InfoBorder
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.presentation.features.iot.IoTViewModel
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothIllustrationCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothSetupTopBar
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.CircularIconBadge
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.ProgressHeader
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.RequirementChecklist
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.WizardBottomActions
import kotlinx.coroutines.launch

private const val INTRO_PROGRESS = 0f

private val BLUETOOTH_REQUIREMENTS = listOf(
    "Bluetooth is enabled",
    "SiliGuide device is powered on",
    "If this is your first time connecting, you'll pair your device in the next step"
)

/**
 * Screen 1 — Bluetooth Introduction.
 * Explains what Bluetooth Classic mode does before the user pairs a device
 * through Android Bluetooth Settings.
 */
@Composable
fun BluetoothIntroScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: IoTViewModel = hiltViewModel()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var showRationaleDialog by remember {
        mutableStateOf(false)
    }

    var showSettingsDialog by remember {
        mutableStateOf(false)
    }

    val checkPermissionsAndNavigate: () -> Unit = {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            onContinue()
        } else {
            val connectGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            val scanGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

            if (connectGranted && scanGranted) {
                showRationaleDialog = false
                showSettingsDialog = false
                onContinue()
            }
        }
    }

    // Automatically navigate when user returns to screen with Bluetooth permissions granted
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissionsAndNavigate()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.isNotEmpty() && result.values.all { it }
        if (granted) {
            showRationaleDialog = false
            showSettingsDialog = false
            onContinue()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            BluetoothSetupTopBar(stepLabel = null, onBack = onBack)
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
                WizardBottomActions(
                    primaryLabel = "Continue",
                    onPrimaryClick = {

                        scope.launch {

                            val connectPermission =
                                viewModel.permissionManager.getPermissionStatus(
                                    context = context,
                                    permission = Manifest.permission.BLUETOOTH_CONNECT,
                                    type = PermissionType.BLUETOOTH
                                )

                            val scanPermission =
                                viewModel.permissionManager.getPermissionStatus(
                                    context = context,
                                    permission = Manifest.permission.BLUETOOTH_SCAN,
                                    type = PermissionType.BLUETOOTH
                                )

                            when {

                                connectPermission == PermissionStatus.Granted &&
                                        scanPermission == PermissionStatus.Granted -> {

                                    onContinue()
                                }

                                connectPermission == PermissionStatus.ShowRationale ||
                                        scanPermission == PermissionStatus.ShowRationale -> {

                                    showRationaleDialog = true
                                }

                                connectPermission == PermissionStatus.PermanentlyDenied ||
                                        scanPermission == PermissionStatus.PermanentlyDenied -> {

                                    showSettingsDialog = true
                                }

                                else -> {

                                    viewModel.permissionManager.markRequested(
                                        PermissionType.BLUETOOTH
                                    )

                                    bluetoothPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_CONNECT,
                                            Manifest.permission.BLUETOOTH_SCAN
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }

        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProgressHeader(progress = INTRO_PROGRESS)

            CircularIconBadge(
                background = InfoBlue,
                borderColor = InfoBorder,
                iconTint = Info,
                icon = Icons.Default.Bluetooth,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 4.dp)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Connect via Bluetooth",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Use Bluetooth to communicate directly with your SiliGuide IoT device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }

            BluetoothIllustrationCard(modifier = Modifier.fillMaxWidth())

            RequirementChecklist(items = BLUETOOTH_REQUIREMENTS)
        }
    }

    if (showRationaleDialog) {

        PermissionDialog(

            title = "Bluetooth Permission Required",

            message =
                "SILI-GUIDE needs Bluetooth permission to discover and communicate with your IoT device.",

            confirmText = "Continue",

            dismissText = "Cancel",

            onConfirm = {

                showRationaleDialog = false

                bluetoothPermissionLauncher.launch(

                    arrayOf(
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                    )
                )
            },

            onDismiss = {

                showRationaleDialog = false
            }
        )
    }

    if (showSettingsDialog) {

        PermissionDialog(

            title = "Bluetooth Permission Disabled",

            message =
                "Bluetooth permission has been permanently denied. Tap \"Open Settings,\" then go to Permissions → Nearby devices and select Allow to continue.",

            confirmText = "Open Settings",

            dismissText = "Cancel",

            onConfirm = {

                showSettingsDialog = false

                PermissionUtils.openAppSettings(context)
            },

            onDismiss = {

                showSettingsDialog = false
            }
        )
    }
}

@Preview(name = "Small phone", device = Devices.PHONE, showSystemUi = true, widthDp = 320, heightDp = 640)
@Preview(name = "Standard phone", showSystemUi = true, widthDp = 360, heightDp = 740)
@Preview(name = "Large phone", showSystemUi = true, widthDp = 412, heightDp = 915)
@Composable
private fun BluetoothIntroScreenPreview() {
    SiliGuideTheme {
        BluetoothIntroScreen(onContinue = {}, onBack = {})
    }
}