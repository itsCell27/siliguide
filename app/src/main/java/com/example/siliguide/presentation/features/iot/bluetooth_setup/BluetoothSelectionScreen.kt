package com.example.siliguide.presentation.features.iot.bluetooth_setup

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.components.LoadingOverlay
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GrayHint
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.Info
import com.example.siliguide.core.ui.theme.InfoBg
import com.example.siliguide.core.ui.theme.InfoBorder
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.presentation.features.iot.IoTViewModel
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothSetupTopBar
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.CircularIconBadge
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.ProgressHeader
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.WizardBottomActions

private const val STEP_1_PROGRESS = 0.5f

// ============================================================================
// Model
// ============================================================================

/** One row in the OS-level paired-device list, filtered to SiliGuide devices only. */
data class PairedSiliGuideDevice(
    val name: String,
    val deviceId: String,
    val device: BluetoothDevice
)

// ============================================================================
// STATELESS UI CONTENT — Screen 3 · Step 1 of 2 — Select Paired SiliGuide Device
// ============================================================================
@Composable
fun BluetoothSelectionScreen(
    viewModel: IoTViewModel,
    devices: List<PairedSiliGuideDevice>,
    onDeviceSelected: (PairedSiliGuideDevice) -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    message: String
) {

    val validationDialog = viewModel.validationDialog
    val uiState by viewModel.iotUiState.collectAsState()

    if (validationDialog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissValidationDialog() },
            title = { Text(validationDialog.title) },
            text = { Text(validationDialog.message) },
            confirmButton = {
                Button(onClick = { viewModel.dismissValidationDialog() }) { Text("OK") }
            }
        )
    }

    LoadingOverlay(
        isLoading = isLoading,
        message = message
    ) {
        Scaffold(
            modifier = modifier
                .fillMaxSize()
                .safeDrawingPadding(),
            topBar = {
                BluetoothSetupTopBar(stepLabel = "Step 1 of 2", onBack = onBack)
            },
            bottomBar = {
                if (devices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(White)
                            .border(BorderStroke(1.dp, Border))
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                    ) {
                        WizardBottomActions(
                            primaryLabel = "Open Bluetooth Settings",
                            onPrimaryClick = onOpenBluetoothSettings
                        )
                    }
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

                if (devices.isEmpty()) {
                    BluetoothEmptyDevicesState(
                        hasPairedDevice = uiState.hasPairedDevices,
                        isBluetoothOn = uiState.isBluetoothOn
                    )
                } else {
                    Text(
                        text = "Select your device",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp)
                    )
                    Text(
                        text = "Choose a paired SiliGuide device to register. Tap a device to verify it automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayBody
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        devices.forEach { device ->
                            PairedDeviceCard(
                                device = device,
                                onClick = { onDeviceSelected(device) }
                            )
                        }
                    }
                }
            }
        }
    }

}

// ============================================================================
// Paired device row (.dvc)
// ============================================================================
@Composable
private fun PairedDeviceCard(
    device: PairedSiliGuideDevice,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = BorderStroke(1.dp, Border),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = InfoBg,
                border = BorderStroke(1.5.dp, InfoBorder),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = Info,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
//                Text(
//                    text = "Device ID: ${device.deviceId}",
//                    style = MaterialTheme.typography.bodySmall,
//                    color = GrayMedium
//                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = GrayHint,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
private fun BluetoothEmptyDevicesState(
    modifier: Modifier = Modifier,
    hasPairedDevice: Boolean,
    isBluetoothOn: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val title = when {
            !isBluetoothOn -> "Bluetooth is Off"
            !hasPairedDevice -> "No Paired Devices Found"
            hasPairedDevice -> "No Active Devices Found"
            else -> "No Paired Devices Found"
        }
        val message = when {
            !isBluetoothOn -> "Please turn on Bluetooth in your phone's settings to scan for SiliGuide devices."
            !hasPairedDevice -> "No paired SiliGuide devices were found. Pair a device in Bluetooth Settings first."
            hasPairedDevice -> "Paired SiliGuide devices were found, but none are currently reachable. Ensure your IoT device is powered on, in range, and try again."
            else -> "No paired SiliGuide devices were found. Pair a device in Bluetooth Settings first."
        }

        CircularIconBadge(
            background = Color(0xFFF7F7F5),   // matches --soft in mockup
            borderColor = Border,             // matches --hair, same token already used elsewhere
            iconTint = GrayHint,               // matches --hint stroke on the icon
            icon = Icons.Default.Bluetooth
        )
        Spacer(modifier = Modifier.height(13.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
            textAlign = TextAlign.Center
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = GrayBody,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}
