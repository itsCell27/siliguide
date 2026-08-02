package com.example.siliguide.presentation.features.iot.bluetooth_setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.Info
import com.example.siliguide.core.ui.theme.InfoBg
import com.example.siliguide.core.ui.theme.InfoBorder
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothIllustrationCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothSetupTopBar
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.CircularIconBadge
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.ProgressHeader
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.RequirementChecklist
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.WizardBottomActions
import android.Manifest
import android.util.Log
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.google.accompanist.permissions.isGranted
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.permission.PermissionStatus
import com.example.siliguide.core.permission.PermissionType
import com.example.siliguide.core.permission.PermissionUtils
import com.example.siliguide.core.ui.components.PermissionDialog
import com.example.siliguide.presentation.features.iot.IoTViewModel
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
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothIntroScreen(
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: IoTViewModel = hiltViewModel()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showRationaleDialog by remember {
        mutableStateOf(false)
    }

    var showSettingsDialog by remember {
        mutableStateOf(false)
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->

        val granted =
            result.values.all { it }

        if (granted) {

            onContinue()
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BluetoothSetupTopBar(stepLabel = null, onBack = onBack)
        },
        bottomBar = {
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
                },
                secondaryLabel = "Back",
                onSecondaryClick = onBack,
                modifier = Modifier
                    .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.navigationBars)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            )
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
                background = InfoBg,
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
                    text = "Use Bluetooth Classic to communicate directly with your SiliGuide IoT device.",
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
                "Bluetooth permission has been permanently denied. Please enable it in Settings to continue.",

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