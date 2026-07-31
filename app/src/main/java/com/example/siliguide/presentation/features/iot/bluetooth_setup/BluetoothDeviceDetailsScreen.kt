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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
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
    deviceName: String,
    onDeviceNameChanged: (String) -> Unit,
    availablePlants: List<String>,
    selectedPlant: String?,
    onPlantSelected: (String) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            BluetoothSetupTopBar(stepLabel = "Step 2 of 2", onBack = onBack)
        },
        bottomBar = {
            WizardBottomActions(
                primaryLabel = "Save Device",
                onPrimaryClick = onSave,
                primaryEnabled = deviceName.isNotBlank(),
                secondaryLabel = "Back",
                onSecondaryClick = onBack,
                secondaryIsLink = false,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
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
            onBack = {}
        )
    }
}