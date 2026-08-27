package com.example.siliguide.presentation.features.analysis

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.R
import com.example.siliguide.core.permission.PermissionStatus
import com.example.siliguide.core.ui.components.LoadingOverlay
import com.example.siliguide.core.ui.components.PermissionDialog
import com.example.siliguide.core.ui.components.SiliGuideBackButton
import com.example.siliguide.core.ui.theme.*

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class SensorPreviewData(
    val soilMoisture: String  = "52%",
    val soilTemp: String      = "26.8°C",
    val soilPh: String        = "6.6",
    val soilN: String         = "48 ppm",
    val soilP: String         = "26 ppm",
    val soilK: String         = "162 ppm",
    val soilEc: String        = "1.3",
    val airTemp: String       = "28.4°C",
    val light: String         = "9.1k lx",
    val lastUpdateSec: Int    = 2
)

data class OnTheGoDevice(
    val deviceId: String,
    val name: String,
    val meta: String
)

sealed class PreAnalysisState {
    data class LinkedConnected(
        val deviceId: String,
        val deviceName: String,
        val deviceMeta: String,
        val sensors: SensorPreviewData
    ) : PreAnalysisState()

    data class LinkedDisconnected(
        val deviceName: String,
        val lastSeenLabel: String,
        val onTheGoDevices: List<OnTheGoDevice>
    ) : PreAnalysisState()

    data class NoDevice(
        val availableDevices: List<OnTheGoDevice> = emptyList()
    ) : PreAnalysisState()
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PreAnalysisScreen(
    viewModel: PreAnalysisViewModel,
    state: PreAnalysisState,
    permissionStatus: PermissionStatus,
    onBack: () -> Unit,
    showPermissionDialog: Boolean,
    onRequestCameraPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onPermissionDenied: () -> Unit,
    onContinueToCamera: () -> Unit = {},
    onContinueWithOtherDevice: (deviceId: String) -> Unit = {},
    onSkipToImageOnly: () -> Unit = {},
    onContinueImageOnly: () -> Unit = {},
    onAddIoTDevice: () -> Unit = {},
) {
    val preUiState by viewModel.preUiState.collectAsState()
    var selectedOnTheGoId by remember { mutableStateOf<String?>(null) }
    var imageOnlySelected by remember { mutableStateOf(false) }

    // BUG FIX: Smart Auto-Selection that doesn't overwrite user choices
    LaunchedEffect(state) {
        if (imageOnlySelected) return@LaunchedEffect

        val currentAvailableList = when (state) {
            is PreAnalysisState.LinkedDisconnected -> state.onTheGoDevices
            is PreAnalysisState.NoDevice -> state.availableDevices
            else -> emptyList()
        }

        val selectionStillExists = currentAvailableList.any { it.deviceId == selectedOnTheGoId }

        if (!selectionStillExists) {
            selectedOnTheGoId = currentAvailableList.firstOrNull()?.deviceId
        }
    }

    LoadingOverlay(
        isLoading = preUiState.isLoading,
        message = preUiState.loadingMessage
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhite)
        ) {
            TopBar(onBack = onBack)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (state) {
                    is PreAnalysisState.LinkedConnected -> LinkedConnectedContent(state)
                    is PreAnalysisState.LinkedDisconnected -> LinkedDisconnectedContent(
                        state = state,
                        selectedId = selectedOnTheGoId,
                        onSelectDevice = { selectedOnTheGoId = it }
                    )
                    is PreAnalysisState.NoDevice -> NoDeviceContent(
                        state = state,
                        selectedId = selectedOnTheGoId,
                        imageOnlySelected = imageOnlySelected,
                        onSelectDevice = {
                            selectedOnTheGoId = it
                            imageOnlySelected = false
                        },
                        onSelectImageOnly = {
                            selectedOnTheGoId = null
                            imageOnlySelected = true
                        }
                    )
                }
            }

            PinnedButtons(
                state = state,
                selectedOnTheGoId = selectedOnTheGoId,
                imageOnlySelected = imageOnlySelected,
                onContinueToCamera = onContinueToCamera,
                onContinueWithOther = onContinueWithOtherDevice,
                onSkipToImageOnly = onSkipToImageOnly,
                onContinueImageOnly = onContinueImageOnly,
                onAddIoTDevice = onAddIoTDevice
            )

            if (showPermissionDialog && permissionStatus == PermissionStatus.ShowRationale) {
                PermissionDialog(
                    title = "Camera Permission Required",
                    message = "SiliGuide needs camera access to analyze your plant leaves. Please allow camera permission to continue.",
                    confirmText = "Continue",
                    dismissText = "Cancel",
                    onConfirm = onRequestCameraPermission,
                    onDismiss = onPermissionDenied
                )
            }

            if (showPermissionDialog && permissionStatus == PermissionStatus.PermanentlyDenied) {
                PermissionDialog(
                    title = "Camera Permission Disabled",
                    message = "Camera permission has been permanently denied. Please enable it in Settings to continue.",
                    confirmText = "Open Settings",
                    dismissText = "Cancel",
                    onConfirm = onOpenSettings,
                    onDismiss = onPermissionDenied
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SiliGuideBackButton(onClick = onBack)

            Text(
                text  = "Before You Analyze",
                style = MaterialTheme.typography.headlineSmall,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        HorizontalDivider(thickness = 1.dp, color = Border)
    }
}

@Composable
private fun LinkedConnectedContent(state: PreAnalysisState.LinkedConnected) {
    Text(
        text  = "SiliGuide will use your linked device's live sensor readings together with the leaf image for a complete analysis.",
        style = MaterialTheme.typography.bodySmall,
        color = GrayBody,
        lineHeight = 17.sp
    )
    GroupLabel("Linked device")
    StatusCard(variant = StatusCardVariant.Green) {
        DeviceStatusRow(deviceName = state.deviceName, deviceMeta = state.deviceMeta, isConnected = true)
    }
}

@Composable
private fun LinkedDisconnectedContent(
    state: PreAnalysisState.LinkedDisconnected,
    selectedId: String?,
    onSelectDevice: (String) -> Unit
) {
    GroupLabel("Linked device")
    StatusCard(variant = StatusCardVariant.Amber) {
        DeviceStatusRow(deviceName = state.deviceName, deviceMeta = state.lastSeenLabel, isConnected = false)
        Text(
            text  = "Your linked device isn't reachable right now. Use another connected device or skip to image-only analysis.",
            style = MaterialTheme.typography.bodySmall,
            color = Warning,
            lineHeight = 16.sp
        )
    }
    if (state.onTheGoDevices.isNotEmpty()) {
        GroupLabel("Use another device?")
        state.onTheGoDevices.forEach { device ->
            OnTheGoDeviceOption(
                device = device,
                isSelected = device.deviceId == selectedId,
                onClick = { onSelectDevice(device.deviceId) }
            )
        }
        OrDivider()
    }
    InfoBanner(
        isAmber = false,
        titleText = "Skip — analyze with image only",
        bodyText  = "Without sensor data, only visual symptoms and possible causes will be shown (partial analysis)."
    )
}

@Composable
private fun NoDeviceContent(
    state: PreAnalysisState.NoDevice,
    selectedId: String?,
    imageOnlySelected: Boolean,
    onSelectDevice: (String) -> Unit,
    onSelectImageOnly: () -> Unit
) {
    StatusCard(variant = StatusCardVariant.Gray) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DeviceIconTile(variant = StatusCardVariant.Gray)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "No IoT Device Linked",
                    style = MaterialTheme.typography.titleMedium,
                    color = GrayBody,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "This plant isn't assigned to an IoT device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayHint
                )
            }
        }
    }

    if (state.availableDevices.isNotEmpty()) {
        GroupLabel("AVAILABLE CONNECTED DEVICES")
        state.availableDevices.forEach { device ->
            OnTheGoDeviceOption(
                device = device,
                isSelected = device.deviceId == selectedId,
                onClick = { onSelectDevice(device.deviceId) }
            )
        }

        OrDivider()

        SelectableInfoBanner(
            selected = imageOnlySelected,
            onClick = onSelectImageOnly,
            title = "Partial Analysis",
            description = "Analyze using only the leaf image. Sensor data won't be included."
        )
    } else {
        InfoBanner(
            isAmber = false,
            useGreenIcon = true,
            titleText = "Partial analysis",
            bodyText  = "Without a connected sensor, SiliGuide will detect the visual symptom from your leaf image and list possible causes — but cannot pinpoint a specific condition or give targeted recommendations."
        )

        GroupLabel("What you'll get")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CheckRow(text = "Visual symptom prediction with confidence score", checked = true)
            CheckRow(text = "List of possible plant conditions", checked = true)
            CheckRow(text = "No specific care recommendations", checked = false)
        }
    }
}

@Composable
private fun PinnedButtons(
    state: PreAnalysisState,
    selectedOnTheGoId: String?,
    imageOnlySelected: Boolean,
    onContinueToCamera: () -> Unit,
    onContinueWithOther: (String) -> Unit,
    onSkipToImageOnly: () -> Unit,
    onContinueImageOnly: () -> Unit,
    onAddIoTDevice: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = White,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(top = 10.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            HorizontalDivider(thickness = 1.dp, color = Border, modifier = Modifier.padding(bottom = 6.dp))

            when (state) {
                is PreAnalysisState.LinkedConnected -> {
                    PrimaryButton(label = "Continue to Camera", onClick = onContinueToCamera)
                }
                is PreAnalysisState.LinkedDisconnected -> {
                    val label = if (selectedOnTheGoId != null) {
                        val name = state.onTheGoDevices.find { it.deviceId == selectedOnTheGoId }?.name ?: "Other Device"
                        "Continue with $name"
                    } else {
                        "Continue with Image Only"
                    }
                    PrimaryButton(
                        label = label,
                        onClick = {
                            if (selectedOnTheGoId != null) onContinueWithOther(selectedOnTheGoId)
                            else onSkipToImageOnly()
                        }
                    )
                }
                is PreAnalysisState.NoDevice -> {
                    val selectedDevice = state.availableDevices.find { it.deviceId == selectedOnTheGoId }
                    PrimaryButton(
                        label = when {
                            imageOnlySelected -> "Continue with Image Only"
                            selectedDevice != null -> "Continue with ${selectedDevice.name}"
                            else -> "Continue with Image Only"
                        },
                        onClick = {
                            when {
                                imageOnlySelected -> onContinueImageOnly()
                                selectedDevice != null -> onContinueWithOther(selectedDevice.deviceId)
                                else -> onContinueImageOnly()
                            }
                        }
                    )
                    TextLinkButton(label = "Add an IoT device", onClick = onAddIoTDevice)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE SMALL COMPOSABLES
// ─────────────────────────────────────────────────────────────────────────────

enum class StatusCardVariant { Green, Amber, Gray }

@Composable
private fun StatusCard(variant: StatusCardVariant, content: @Composable ColumnScope.() -> Unit) {
    val (bg, border) = when (variant) {
        StatusCardVariant.Green -> GreenBgLight to GreenLight
        StatusCardVariant.Amber -> WarningBg    to WarningBorder
        StatusCardVariant.Gray  -> White        to GrayLight
    }
    val borderStroke = if (variant == StatusCardVariant.Gray) BorderStroke(1.dp, GrayLight) else BorderStroke(1.dp, border)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = bg),
        border   = borderStroke
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
private fun DeviceIconTile(variant: StatusCardVariant) {
    val (bg, border) = when (variant) {
        StatusCardVariant.Green -> White to GreenLight
        StatusCardVariant.Amber -> White to WarningBorder
        StatusCardVariant.Gray  -> OffWhite to Border
    }
    val iconTint = when (variant) {
        StatusCardVariant.Green -> GreenPrimary
        StatusCardVariant.Amber -> Warning
        StatusCardVariant.Gray  -> GrayMedium
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(bg, RoundedCornerShape(10.dp))
            .border(1.dp, border, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_iot_device),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun DeviceStatusRow(deviceName: String, deviceMeta: String, isConnected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DeviceIconTile(variant = if (isConnected) StatusCardVariant.Green else StatusCardVariant.Amber)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.titleMedium,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text  = deviceMeta,
                style = MaterialTheme.typography.bodySmall,
                color = if (isConnected) GrayMedium else Warning,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            modifier = Modifier
                .wrapContentWidth()
                .background(
                    color = if (isConnected) GreenPrimary else White,
                    shape = RoundedCornerShape(999.dp)
                )
                .then(if (!isConnected) Modifier.border(1.dp, WarningBorder, RoundedCornerShape(999.dp)) else Modifier)
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) White else Warning)
            )
            Text(
                text       = if (isConnected) "Connected" else "Disconnected",
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color      = if (isConnected) White else Warning,
                maxLines   = 1
            )
        }
    }
}

@Composable
private fun OnTheGoDeviceOption(device: OnTheGoDevice, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (isSelected) GreenPrimary else Border, label = "device_border")
    val bgColor by animateColorAsState(if (isSelected) GreenBgLight else White, label = "device_bg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(if (isSelected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(White, RoundedCornerShape(8.dp))
                .border(1.dp, GreenLight, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_iot_device),
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.size(14.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = device.name,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = device.meta,
                fontSize = 10.sp,
                fontFamily = DMSans,
                color = GrayMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(18.dp)
                .border(
                    width = if (isSelected) 2.dp else 1.5.dp,
                    color = if (isSelected) GreenPrimary else GrayLight,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary)
                )
            }
        }
    }
}

@Composable
private fun SelectableInfoBanner(
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
    title: String,
    description: String
) {
    val borderColor by animateColorAsState(if (selected) GreenPrimary else Border, label = "")
    val bgColor by animateColorAsState(if (selected) GreenBgLight else OffWhite, label = "")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                enabled = enabled,
                onClick = onClick
            )
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(
                    width = if (selected) 2.dp else 1.5.dp,
                    color = if (selected) GreenPrimary else GrayLight,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(text = description, style = MaterialTheme.typography.bodySmall, color = GrayBody)
        }
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = Border, thickness = 1.dp)
        Text(text = "or", fontSize = 11.sp, fontFamily = DMSans, color = GrayHint)
        HorizontalDivider(modifier = Modifier.weight(1f), color = Border, thickness = 1.dp)
    }
}

@Composable
private fun InfoBanner(
    isAmber: Boolean,
    useGreenIcon: Boolean = false,
    titleText: String,
    bodyText: String,
    modifier: Modifier = Modifier
) {
    val bg = if (isAmber) WarningBg else OffWhite
    val border = if (isAmber) WarningBorder else Border
    val titleColor = if (isAmber) Warning else Black
    val bodyColor = if (isAmber) Warning else GrayBody
    val iconTint = when {
        isAmber -> Warning
        useGreenIcon -> GreenPrimary
        else -> GrayMedium
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val strokeW = 1.2.dp.toPx()
                val r = (size.minDimension - strokeW) / 2f

                // Outer circle perfectly centered at (cx, cy)
                drawCircle(
                    color = iconTint,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeW)
                )

                // Exclamation stem (centered at cx, extending from cy - 0.45r to cy + 0.12r)
                drawLine(
                    color = iconTint,
                    start = Offset(cx, cy - r * 0.45f),
                    end = Offset(cx, cy + r * 0.12f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )

                // Exclamation dot (centered at cx, located at cy + 0.50r)
                drawCircle(
                    color = iconTint,
                    radius = strokeW * 0.65f,
                    center = Offset(cx, cy + r * 0.50f)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = titleText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = titleColor
            )
            Text(
                text = bodyText,
                fontSize = 11.sp,
                fontFamily = DMSans,
                color = bodyColor,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun CheckRow(
    text: String,
    checked: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val strokeW = 1.2.dp.toPx()
                val r = (size.minDimension - strokeW) / 2f

                if (checked) {
                    drawCircle(
                        color = GreenBgLight,
                        radius = r,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = GreenLight,
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(strokeW)
                    )

                    // Checkmark centered at (cx, cy)
                    val path = Path().apply {
                        moveTo(cx - r * 0.42f, cy - r * 0.02f)
                        lineTo(cx - r * 0.12f, cy + r * 0.30f)
                        lineTo(cx + r * 0.44f, cy - r * 0.32f)
                    }
                    drawPath(
                        path = path,
                        color = GreenPrimary,
                        style = Stroke(
                            width = 1.3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                } else {
                    drawCircle(
                        color = OffWhite,
                        radius = r,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = GrayLight,
                        radius = r,
                        center = Offset(cx, cy),
                        style = Stroke(strokeW)
                    )

                    // X mark centered at (cx, cy)
                    val offset = r * 0.36f
                    val xStroke = 1.3.dp.toPx()
                    drawLine(
                        color = GrayHint,
                        start = Offset(cx - offset, cy - offset),
                        end = Offset(cx + offset, cy + offset),
                        strokeWidth = xStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = GrayHint,
                        start = Offset(cx + offset, cy - offset),
                        end = Offset(cx - offset, cy + offset),
                        strokeWidth = xStroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Text(
            text = text,
            fontSize = 12.sp,
            fontFamily = DMSans,
            color = if (checked) GrayBody else GrayHint,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = GrayMedium,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun PrimaryButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GreenPrimary)
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color = White
        )
    }
}

@Composable
private fun TextLinkButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color = GrayMedium,
            textDecoration = TextDecoration.Underline
        )
    }
}