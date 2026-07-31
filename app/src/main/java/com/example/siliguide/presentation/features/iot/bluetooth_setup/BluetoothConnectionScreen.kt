package com.example.siliguide.presentation.features.iot.bluetooth_setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalWifiOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.siliguide.core.ui.theme.DangerBg
import com.example.siliguide.core.ui.theme.DangerBorder
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothIllustrationCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothSetupTopBar
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.CircularIconBadge
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.OpenBluetoothSettingsCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.ProgressHeader
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.UnsupportedDeviceCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.VerificationCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.WaitingCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.WizardBottomActions
import com.example.siliguide.presentation.features.iot.bluetooth_setup.state.BluetoothConnectionUiState
import com.example.siliguide.presentation.features.iot.bluetooth_setup.state.VerificationStage
import com.example.siliguide.presentation.features.iot.bluetooth_setup.state.VerificationStageStatus
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.Manifest
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

private const val STEP_1_PROGRESS = 0.5f

/**
 * Screen 2 — Connect to your device.
 * A single screen with several visual states driven by [BluetoothConnectionUiState].
 * The user never taps "Connect" manually — connection detection and verification
 * happen automatically once Android reports a paired SiliGuide device.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BluetoothConnectionScreen(
    uiState: BluetoothConnectionUiState,
    onOpenBluetoothSettings: () -> Unit,
    onVerifyDevices: () -> Unit,
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

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME &&
                uiState is BluetoothConnectionUiState.Waiting
            ) {
                onVerifyDevices()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BluetoothSetupTopBar(stepLabel = "Step 1 of 2", onBack = onBack)
        },
        bottomBar = {
            BluetoothConnectionBottomBar(
                uiState = uiState,
                onOpenBluetoothSettings = onOpenBluetoothSettings,
                onRetry = onRetry,
                onBack = onBack
            )
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

            when (uiState) {
                is BluetoothConnectionUiState.Waiting -> WaitingContent(onOpenBluetoothSettings)
                is BluetoothConnectionUiState.Searching -> SearchingContent()
                is BluetoothConnectionUiState.Verifying -> VerifyingContent()
                is BluetoothConnectionUiState.Unsupported -> UnsupportedContent(uiState.message)
            }
        }
    }
}

@Composable
private fun BluetoothConnectionBottomBar(
    uiState: BluetoothConnectionUiState,
    onOpenBluetoothSettings: () -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit
) {
    val (primaryLabel, onPrimaryClick, primaryEnabled) = when (uiState) {
        is BluetoothConnectionUiState.Waiting -> Triple("Open Bluetooth Settings", onOpenBluetoothSettings, true)
        is BluetoothConnectionUiState.Searching -> Triple("Open Bluetooth Settings", onOpenBluetoothSettings, true)
        is BluetoothConnectionUiState.Verifying -> Triple("Verifying…", onOpenBluetoothSettings, false)
        is BluetoothConnectionUiState.Unsupported -> Triple("Try Again", onRetry, true)
    }

    val isUnsupported = uiState is BluetoothConnectionUiState.Unsupported

    WizardBottomActions(
        primaryLabel = primaryLabel,
        onPrimaryClick = onPrimaryClick,
        primaryEnabled = primaryEnabled,
        secondaryLabel = if (isUnsupported) "Open Bluetooth Settings" else "Back",
        onSecondaryClick = if (isUnsupported) onOpenBluetoothSettings else onBack,
        secondaryIsLink = !isUnsupported,
        tertiaryLinkLabel = if (isUnsupported) "Back" else null,
        onTertiaryLinkClick = if (isUnsupported) onBack else null,
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
private fun WaitingContent(onOpenBluetoothSettings: () -> Unit) {
    Text(text = "Connect to your device", style = MaterialTheme.typography.headlineSmall)
    Text(
        text = "Open your phone's Bluetooth Settings and pair with your SiliGuide IoT device. " +
                "After pairing, return to SiliGuide — the app will automatically detect and verify the connection.",
        style = MaterialTheme.typography.bodySmall,
        color = com.example.siliguide.core.ui.theme.GrayBody
    )
    BluetoothIllustrationCard()
    OpenBluetoothSettingsCard(onClick = onOpenBluetoothSettings)
    WaitingCard(
        title = "Waiting for Bluetooth connection…",
        subtitle = "Looking for a paired SiliGuide device…"
    )
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

@Preview(name = "Waiting", showSystemUi = true)
@Composable
private fun BluetoothConnectionWaitingPreview() {
    SiliGuideTheme {
        BluetoothConnectionScreen(
            uiState = BluetoothConnectionUiState.Waiting,
            onOpenBluetoothSettings = {},
            onRetry = {},
            onBack = {},
            onVerifyDevices = {}
        )
    }
}

@Preview(name = "Searching", showSystemUi = true)
@Composable
private fun BluetoothConnectionSearchingPreview() {
    SiliGuideTheme {
        BluetoothConnectionScreen(
            uiState = BluetoothConnectionUiState.Searching,
            onOpenBluetoothSettings = {},
            onRetry = {},
            onBack = {},
            onVerifyDevices = {}
        )
    }
}

@Preview(name = "Verifying", showSystemUi = true)
@Composable
private fun BluetoothConnectionVerifyingPreview() {
    SiliGuideTheme {
        BluetoothConnectionScreen(
            uiState = BluetoothConnectionUiState.Verifying,
            onOpenBluetoothSettings = {},
            onRetry = {},
            onBack = {},
            onVerifyDevices = {}
        )
    }
}

@Preview(name = "Unsupported", showSystemUi = true)
@Composable
private fun BluetoothConnectionUnsupportedPreview() {
    SiliGuideTheme {
        BluetoothConnectionScreen(
            uiState = BluetoothConnectionUiState.Unsupported(
                "The connected Bluetooth device is not a SiliGuide IoT device."
            ),
            onOpenBluetoothSettings = {},
            onRetry = {},
            onBack = {},
            onVerifyDevices = {}
        )
    }
}