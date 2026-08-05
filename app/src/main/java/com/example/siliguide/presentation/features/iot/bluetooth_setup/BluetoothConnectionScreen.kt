package com.example.siliguide.presentation.features.iot.bluetooth_setup

import android.Manifest
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.siliguide.core.ui.components.LoadingOverlay
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.DangerBg
import com.example.siliguide.core.ui.theme.DangerBorder
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.presentation.features.iot.IoTViewModel
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothIllustrationCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothSetupTopBar
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.CircularIconBadge
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.OpenBluetoothSettingsCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.ProgressHeader
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.RequirementChecklist
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.UnsupportedDeviceCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.VerificationCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.WizardBottomActions
import com.example.siliguide.presentation.features.iot.bluetooth_setup.state.BluetoothConnectionUiState
import com.example.siliguide.presentation.features.iot.bluetooth_setup.state.VerificationStage
import com.example.siliguide.presentation.features.iot.bluetooth_setup.state.VerificationStageStatus
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

private const val STEP_1_PROGRESS = 0.5f

// ============================================================================
// 1. STATEFUL WRAPPER (Mirrors your IoTSetupAPModeScreen setup)
// ============================================================================
@Composable
fun BluetoothConnectionScreen(
    viewModel: IoTViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.bluetoothSetupState.collectAsState()
    val validationDialog = viewModel.validationDialog
    val context = LocalContext.current
    val iotUiState by viewModel.iotUiState.collectAsState()

    val bluetoothSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // This callback runs ONLY when the user closes/returns from the Bluetooth settings screen
        viewModel.verifyPairedBluetoothDevices()
    }

    // EXACT MATCH to your AP Mode AlertDialog
    if (validationDialog != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissValidationDialog()
            },
            title = {
                Text(text = validationDialog.title)
            },
            text = {
                Text(text = validationDialog.message)
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissValidationDialog() }
                ) {
                    Text("OK")
                }
            }
        )
    }

    LoadingOverlay(
        isLoading = iotUiState.isLoading,
        message = iotUiState.loadingMessage
    ) {
        // Call the stateless UI block
        BluetoothConnectionScreenContent(
            uiState = uiState,
            onOpenBluetoothSettings = {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                bluetoothSettingsLauncher.launch(intent)
            },
            onRetry = { viewModel.resetBluetoothSetup() },
            onBack = onBack,
            modifier = modifier
        )
    }


}

// ============================================================================
// 2. STATELESS UI CONTENT (Your original layout)
// ============================================================================
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothConnectionScreenContent(
    uiState: BluetoothConnectionUiState,
    onOpenBluetoothSettings: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bluetoothPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
        )
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            BluetoothSetupTopBar(stepLabel = "Step 1 of 2", onBack = onBack)
        },
        bottomBar = {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .background(White)
                    .border(BorderStroke(1.dp, Border))
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                BluetoothConnectionBottomBar(
                    uiState = uiState,
                    onOpenBluetoothSettings = onOpenBluetoothSettings,
                    onRetry = onRetry
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
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            ProgressHeader(progress = STEP_1_PROGRESS)

            WaitingContent(onOpenBluetoothSettings)
        }
    }
}

@Composable
private fun BluetoothConnectionBottomBar(
    uiState: BluetoothConnectionUiState,
    onOpenBluetoothSettings: () -> Unit,
    onRetry: () -> Unit
) {
    val (primaryLabel, onPrimaryClick, primaryEnabled) = when (uiState) {
        is BluetoothConnectionUiState.Waiting -> Triple("Open Bluetooth Settings", onOpenBluetoothSettings, true)
        is BluetoothConnectionUiState.Searching -> Triple("Open Bluetooth Settings", onOpenBluetoothSettings, true)
        is BluetoothConnectionUiState.Verifying -> Triple("Verifying…", onOpenBluetoothSettings, false)
        is BluetoothConnectionUiState.Unsupported -> Triple("Try Again", onRetry, true)
    }

    WizardBottomActions(
        primaryLabel = primaryLabel,
        onPrimaryClick = onPrimaryClick,
        primaryEnabled = primaryEnabled,
    )
}

@Composable
private fun WaitingContent(onOpenBluetoothSettings: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "Pair your device", style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Pair your SiliGuide IoT device using Android Bluetooth Settings. " +
                    "After pairing, return to SiliGuide.",
            style = MaterialTheme.typography.bodySmall,
            color = com.example.siliguide.core.ui.theme.GrayBody
        )
        BluetoothIllustrationCard()
        OpenBluetoothSettingsCard(onClick = onOpenBluetoothSettings)
        RequirementChecklist(
            items = listOf(
                "Only pair once per device — SiliGuide remembers it after",
                "Returning here shows every paired SiliGuide device to choose from"
            )
        )
    }

}

@Composable
private fun SearchingContent() {
    Text(text = "Connect to your device", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Bluetooth device detected. Verifying this is a SiliGuide IoT device — no action needed.",
        style = MaterialTheme.typography.bodySmall,
        color = com.example.siliguide.core.ui.theme.GrayBody
    )
    VerificationCard(
        stages = listOf(
            VerificationStage("Bluetooth device detected…", VerificationStageStatus.Pending),
            VerificationStage("Verifying device…", VerificationStageStatus.Waiting),
            VerificationStage("Retrieving device information…", VerificationStageStatus.Waiting),
            VerificationStage("Connected successfully", VerificationStageStatus.Waiting)
        )
    )
}

@Composable
private fun VerifyingContent() {
    Text(text = "Connect to your device", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Bluetooth connection detected. Verifying this is a SiliGuide IoT device — no action needed.",
        style = MaterialTheme.typography.bodySmall,
        color = com.example.siliguide.core.ui.theme.GrayBody
    )
    VerificationCard(
        stages = listOf(
            VerificationStage("Bluetooth connected", VerificationStageStatus.Done),
            VerificationStage("Verifying device…", VerificationStageStatus.Pending),
            VerificationStage("Retrieving device information…", VerificationStageStatus.Waiting),
            VerificationStage("Connected successfully", VerificationStageStatus.Waiting)
        )
    )
}

@Composable
private fun UnsupportedContent(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        CircularIconBadge(
            background = DangerBg,
            borderColor = DangerBorder,
            iconTint = MaterialTheme.colorScheme.error,
            icon = Icons.Default.SignalWifiOff
        )
        Text(text = "Unsupported Bluetooth Device", style = MaterialTheme.typography.titleLarge)
        UnsupportedDeviceCard(message = message)
    }
}

// ============================================================================
// 3. PREVIEWS
// ============================================================================
@Preview(name = "Waiting", showSystemUi = true)
@Composable
private fun BluetoothConnectionWaitingPreview() {
    SiliGuideTheme {
        BluetoothConnectionScreenContent(
            uiState = BluetoothConnectionUiState.Waiting,
            onOpenBluetoothSettings = {},
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(name = "Searching", showSystemUi = true)
@Composable
private fun BluetoothConnectionSearchingPreview() {
    SiliGuideTheme {
        BluetoothConnectionScreenContent(
            uiState = BluetoothConnectionUiState.Searching,
            onOpenBluetoothSettings = {},
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(name = "Verifying", showSystemUi = true)
@Composable
private fun BluetoothConnectionVerifyingPreview() {
    SiliGuideTheme {
        BluetoothConnectionScreenContent(
            uiState = BluetoothConnectionUiState.Verifying,
            onOpenBluetoothSettings = {},
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(name = "Unsupported", showSystemUi = true)
@Composable
private fun BluetoothConnectionUnsupportedPreview() {
    SiliGuideTheme {
        BluetoothConnectionScreenContent(
            uiState = BluetoothConnectionUiState.Unsupported(
                "The connected Bluetooth device is not a SiliGuide IoT device."
            ),
            onOpenBluetoothSettings = {},
            onRetry = {},
            onBack = {},
        )
    }
}