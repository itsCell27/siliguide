package com.example.siliguide.presentation.features.iot.bluetooth_setup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.siliguide.R
import com.example.siliguide.core.ui.components.LoadingOverlay
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.presentation.features.iot.DEVICE_NAME_MAX_LENGTH
import com.example.siliguide.presentation.features.iot.IoTViewModel

/**
 * Screen 3 — Device Details.
 * Collects a friendly device name and an optional plant link before the
 * verified Bluetooth device is saved to the user's account.
 *
 * Visually mirrors IoTSetupAPModeScreen / APStep2Content so the AP wizard
 * and the Bluetooth wizard feel like the same product.
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
    val maxChar = DEVICE_NAME_MAX_LENGTH

    LoadingOverlay(
        isLoading = viewModel.isClaiming,
        message = "Saving device..."
    ) {
        Scaffold(
            modifier = modifier.safeDrawingPadding(),

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
                        IconButton(
                            enabled = !viewModel.isClaiming,
                            onClick = onBack
                        ) {
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
                            text = "Bluetooth Setup",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = "Step 2 of 2",
                            style = MaterialTheme.typography.labelSmall,
                            color = GreenPrimary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { 1.0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                            .height(3.dp)
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
                        onClick = onSave,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            contentColor = White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        enabled = deviceName.isNotBlank() && !viewModel.isClaiming
                    ) {
                        Text(
                            text = "Save Device",
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
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(0.5.dp))

                // Success Icon
                Box(
                    modifier = Modifier.size(66.dp).background(GreenPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = White, modifier = Modifier.size(32.dp))
                }

                // Header
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Device verified!", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = Fraunces, color = Black)
                    Text("Give this device a name before saving it.", fontSize = 13.sp, color = GrayMedium, modifier = Modifier.padding(top = 3.dp))
                }

                // Mode Badge
                Text(
                    text = "🔵 Bluetooth Mode · Verified",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GreenDark,
                    modifier = Modifier
                        .background(GreenBgLight, RoundedCornerShape(7.dp))
                        .border(1.dp, GreenLight, RoundedCornerShape(7.dp))
                        .padding(horizontal = 11.dp, vertical = 6.dp)
                )

                // Form Fields
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // --- EDITABLE DEVICE NAME FIELD ---
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Device name", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GrayBody)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(White, RoundedCornerShape(12.dp))
                                .border(1.5.dp, GreenPrimary, RoundedCornerShape(12.dp))
                                .padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(9.dp))

                            BasicTextField(
                                value = deviceName,
                                onValueChange = {
                                    if (it.length <= maxChar) onDeviceNameChanged(it)
                                },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(fontSize = 14.sp, color = Black, fontFamily = DMSans),
                                cursorBrush = SolidColor(GreenPrimary),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                decorationBox = { innerTextField ->
                                    if (deviceName.isEmpty()) {
                                        Text("Enter device name...", fontSize = 14.sp, color = GrayHint)
                                    }
                                    innerTextField()
                                }
                            )

                            Text("${deviceName.length}/$maxChar", fontSize = 11.sp, color = GreenPrimary)
                        }
                    }

                    // Link to a plant
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Link to a plant ", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GrayBody)
                            Text(
                                "Optional",
                                fontSize = 10.sp,
                                color = GrayHint,
                                modifier = Modifier
                                    .background(OffWhite, RoundedCornerShape(4.dp))
                                    .border(1.dp, Border, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .background(White, RoundedCornerShape(12.dp))
                                .border(1.dp, Border, RoundedCornerShape(12.dp))
                                .padding(horizontal = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(16.dp).background(GreenPrimary, CircleShape))
                            Spacer(modifier = Modifier.width(9.dp))
                            Text(
                                text = selectedPlant ?: "Select a plant to link",
                                fontSize = 14.sp,
                                color = if (selectedPlant != null) Black else GrayMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = GrayHint)
                        }
                    }
                }

                // Info Box
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White, RoundedCornerShape(11.dp))
                        .border(1.dp, Border, RoundedCornerShape(11.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Rounded.Bluetooth, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your phone communicates directly with this device using Bluetooth Classic. No Wi-Fi connection is required.",
                        fontSize = 12.sp,
                        color = GrayBody,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(0.5.dp))
            }
        }
    }

    if (validationDialog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissValidationDialog() },
            title = { Text(text = validationDialog.title) },
            text = { Text(text = validationDialog.message) },
            confirmButton = {
                Button(onClick = { viewModel.dismissValidationDialog() }) {
                    Text("OK")
                }
            }
        )
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