package com.example.siliguide.presentation.features.iot.bluetooth_setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.presentation.features.iot.IoTViewModel
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothModeBadge
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.BluetoothSetupTopBar
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.CircularIconBadge
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.DeviceDetailsForm
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.InfoCard
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.ProgressHeader
import com.example.siliguide.presentation.features.iot.bluetooth_setup.components.WizardBottomActions

private const val STEP_2_PROGRESS = 1f

/**
 * Screen 3 — Device Details.
 * Collects a friendly device name and an optional plant link before the
 * verified Bluetooth device is saved to the user's account.
 */
@Composable
fun BluetoothDeviceDetailsScreen(
    viewModel: IoTViewModel,
    deviceName: String,
    onDeviceNameChanged: (String) -> Unit,
    availablePlants: List<String>,
    selectedPlant: String?,
    onPlantSelected: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val validationDialog = viewModel.validationDialog

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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        topBar = {
            BluetoothSetupTopBar(stepLabel = "Step 2 of 2", onBack = onBack)
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
                WizardBottomActions(
                    primaryLabel = "Save Device",
                    onPrimaryClick = onSave,
                    primaryEnabled = deviceName.isNotBlank() && !viewModel.isClaiming
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
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            ProgressHeader(progress = STEP_2_PROGRESS)

            CircularIconBadge(
                background = GreenPrimary,
                borderColor = GreenPrimary,
                iconTint = White,
                icon = Icons.Default.Check,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Device Details",
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Your Bluetooth device has been verified successfully. Give this device a " +
                            "friendly name before saving it to your account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = com.example.siliguide.core.ui.theme.GrayBody,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }

            BluetoothModeBadge(
                connected = true,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            DeviceDetailsForm(
                deviceName = deviceName,
                onDeviceNameChanged = onDeviceNameChanged,
                availablePlants = availablePlants,
                selectedPlant = selectedPlant,
                onPlantSelected = onPlantSelected
            )

            InfoCard(
                text = "Your phone communicates directly with this SiliGuide device using Bluetooth " +
                        "Classic. No Wi-Fi connection is required."
            )
        }
    }
}

@Preview(name = "Small phone", showSystemUi = true, widthDp = 320, heightDp = 640)
@Preview(name = "Standard phone", showSystemUi = true, widthDp = 360, heightDp = 740)
@Preview(name = "Large phone", showSystemUi = true, widthDp = 412, heightDp = 915)
@Composable
private fun BluetoothDeviceDetailsScreenPreview() {
    SiliGuideTheme {
        BluetoothDeviceDetailsScreen(
            deviceName = "",
            onDeviceNameChanged = {},
            availablePlants = listOf("Chili Plant #1", "Chili Plant #2"),
            selectedPlant = null,
            onPlantSelected = {},
            onSave = {},
            onBack = {},
            viewModel = viewModel()
        )
    }
}