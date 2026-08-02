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
import com.example.siliguide.core.permission.PermissionStatus
import com.example.siliguide.core.ui.components.PermissionDialog
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.R
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
    val meta: String          // e.g. "AP Mode · Connected"
)

/** Three possible screen states */
sealed class PreAnalysisState {
    /** 7A — Plant has a linked device AND it is currently connected */
    data class LinkedConnected(
        val deviceId: String,
        val deviceName: String,
        val deviceMeta: String,         // e.g. "LAN Mode · 192.168.1.42"
        val sensors: SensorPreviewData
    ) : PreAnalysisState()

    /** 7B — Plant has a linked device BUT it is disconnected right now */
    data class LinkedDisconnected(
        val deviceName: String,
        val lastSeenLabel: String,      // e.g. "Last seen 14 min ago"
        val onTheGoDevices: List<OnTheGoDevice>
    ) : PreAnalysisState()

    /** 7C — No IoT device linked (or no devices registered at all) */
    object NoDevice : PreAnalysisState()
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PreAnalysisScreen(
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
    // On-the-go selection (7B only)
    var selectedOnTheGoId by remember { mutableStateOf<String?>(null) }

    // Auto-select first device when entering 7B
    LaunchedEffect(state) {
        if (state is PreAnalysisState.LinkedDisconnected) {
            selectedOnTheGoId = state.onTheGoDevices.firstOrNull()?.deviceId
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        TopBar(onBack = onBack)

        // ── Scrollable content ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (state) {
                is PreAnalysisState.LinkedConnected    -> LinkedConnectedContent(state)
                is PreAnalysisState.LinkedDisconnected -> LinkedDisconnectedContent(
                    state            = state,
                    selectedId       = selectedOnTheGoId,
                    onSelectDevice   = { selectedOnTheGoId = it }
                )
                is PreAnalysisState.NoDevice           -> NoDeviceContent()
            }
        }

        // ── Pinned bottom button(s) ───────────────────────────────────────
        PinnedButtons(
            state               = state,
            selectedOnTheGoId   = selectedOnTheGoId,
            onContinueToCamera  = onContinueToCamera,
            onContinueWithOther = onContinueWithOtherDevice,
            onSkipToImageOnly   = onSkipToImageOnly,
            onContinueImageOnly = onContinueImageOnly,
            onAddIoTDevice      = onAddIoTDevice
        )

        if (
            showPermissionDialog &&
            permissionStatus == PermissionStatus.ShowRationale
        ) {

            PermissionDialog(

                title = "Camera Permission Required",

                message =
                    "SILI-GUIDE needs camera access to analyze your plant leaves. Please allow camera permission to continue.",

                confirmText = "Continue",

                dismissText = "Cancel",

                onConfirm = onRequestCameraPermission,

                onDismiss = onPermissionDenied
            )
        }

        if (
            showPermissionDialog &&
            permissionStatus == PermissionStatus.PermanentlyDenied
        ) {

            PermissionDialog(

                title = "Camera Permission Disabled",

                message =
                    "Camera permission has been permanently denied. Please enable it in Settings to continue.",

                confirmText = "Open Settings",

                dismissText = "Cancel",

                onConfirm = onOpenSettings,

                onDismiss = onPermissionDenied
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TopBar(onBack: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OffWhite)
                    .border(1.dp, Border, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                // Chevron left drawn with Canvas (no Icon dependency on back arrow)
                Canvas(modifier = Modifier.size(14.dp)) {
                    val p = Path().apply {
                        moveTo(size.width * 0.65f, size.height * 0.18f)
                        lineTo(size.width * 0.35f, size.height * 0.50f)
                        lineTo(size.width * 0.65f, size.height * 0.82f)
                    }
                    drawPath(
                        path = p,
                        color = GrayBody,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Text(
                text  = "Before You Analyze",
                style = MaterialTheme.typography.headlineSmall,
                color = Black
            )
        }
        HorizontalDivider(thickness = 1.dp, color = Border)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 7A — LINKED + CONNECTED CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LinkedConnectedContent(state: PreAnalysisState.LinkedConnected) {
    // Helper intro
    Text(
        text  = "SiliGuide will use your linked device's live sensor readings together with the leaf image for a complete analysis.",
        style = MaterialTheme.typography.bodySmall,
        color = GrayBody,
        lineHeight = 17.sp
    )

    // Section label
    GroupLabel("Linked device")

    // Green status card
    StatusCard(variant = StatusCardVariant.Green) {
        DeviceStatusRow(
            deviceName    = state.deviceName,
            deviceMeta    = state.deviceMeta,
            isConnected   = true
        )
    }

    // Sensor preview label
    GroupLabel("Live sensor preview")

    // 3×3 sensor grid
    SensorGrid(sensors = state.sensors)

    // Last update helper
    val updateText = buildAnnotatedString {
        append("Receiving data · last update ")
        withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = GreenDark)) {
            append("${state.sensors.lastUpdateSec} sec ago")
        }
    }
    Text(
        text  = updateText,
        style = MaterialTheme.typography.bodySmall,
        color = GrayHint,
        lineHeight = 16.sp
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 7B — LINKED + DISCONNECTED CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LinkedDisconnectedContent(
    state: PreAnalysisState.LinkedDisconnected,
    selectedId: String?,
    onSelectDevice: (String) -> Unit
) {
    // Section label
    GroupLabel("Linked device")

    // Amber warning card
    StatusCard(variant = StatusCardVariant.Amber) {
        DeviceStatusRow(
            deviceName  = state.deviceName,
            deviceMeta  = state.lastSeenLabel,
            isConnected = false
        )
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
            val isSelected = device.deviceId == selectedId
            OnTheGoDeviceOption(
                device     = device,
                isSelected = isSelected,
                onClick    = { onSelectDevice(device.deviceId) }
            )
        }

        OrDivider()
    }

    // Gray "skip" info banner
    InfoBanner(
        isAmber   = false,
        titleText = "Skip — analyze with image only",
        bodyText  = "Without sensor data, only visual symptoms and possible causes will be shown (partial analysis)."
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 7C — NO DEVICE CONTENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun NoDeviceContent() {
    // Gray status card
    StatusCard(variant = StatusCardVariant.Gray) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DeviceIconTile(variant = StatusCardVariant.Gray)
            Column {
                Text(
                    text  = "No IoT Device Connected",
                    style = MaterialTheme.typography.titleMedium,
                    color = GrayBody
                )
                Text(
                    text  = "Sensor data won't be included",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayHint
                )
            }
        }
    }

    // Green-icon info banner
    InfoBanner(
        isAmber       = false,
        useGreenIcon  = true,
        titleText     = "Partial analysis",
        bodyText      = "Without a connected sensor, SiliGuide will detect the visual symptom from your leaf image and list possible causes — but cannot pinpoint a specific condition or give targeted recommendations."
    )

    // "What you'll get" list
    GroupLabel("What you'll get")

    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        CheckRow(text = "Visual symptom prediction with confidence score", checked = true)
        CheckRow(text = "List of possible plant conditions",               checked = true)
        CheckRow(text = "No specific care recommendations",                checked = false)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PINNED BOTTOM BUTTONS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PinnedButtons(
    state: PreAnalysisState,
    selectedOnTheGoId: String?,
    onContinueToCamera: () -> Unit,
    onContinueWithOther: (String) -> Unit,
    onSkipToImageOnly: () -> Unit,
    onContinueImageOnly: () -> Unit,
    onAddIoTDevice: () -> Unit
) {
    Surface(
        modifier       = Modifier.fillMaxWidth(),
        color          = White,
        shadowElevation = 0.dp,
        tonalElevation  = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            HorizontalDivider(thickness = 1.dp, color = Border, modifier = Modifier.padding(bottom = 10.dp))

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
                        label   = label,
                        onClick = {
                            if (selectedOnTheGoId != null) onContinueWithOther(selectedOnTheGoId)
                            else onSkipToImageOnly()
                        }
                    )
                }

                is PreAnalysisState.NoDevice -> {
                    PrimaryButton(label = "Continue with Image Only", onClick = onContinueImageOnly)
                    TextLinkButton(label = "Add an IoT device first", onClick = onAddIoTDevice)
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
    val borderStroke = if (variant == StatusCardVariant.Gray)
        BorderStroke(1.dp, GrayLight)   // dashed look via thin border
    else
        BorderStroke(1.dp, border)

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
        DeviceIconTile(
            variant = if (isConnected) StatusCardVariant.Green else StatusCardVariant.Amber
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(text = deviceName, style = MaterialTheme.typography.titleMedium, color = Black)
            Text(
                text  = deviceMeta,
                style = MaterialTheme.typography.bodySmall,
                color = if (isConnected) GrayMedium else Warning
            )
        }

        // Connected / Disconnected pill
        Row(
            modifier = Modifier
                .background(
                    color = if (isConnected) GreenPrimary else White,
                    shape = RoundedCornerShape(999.dp)
                )
                .then(
                    if (!isConnected)
                        Modifier.border(1.dp, WarningBorder, RoundedCornerShape(999.dp))
                    else Modifier
                )
                .padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Pulse dot
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
                color      = if (isConnected) White else Warning
            )
        }
    }
}

@Composable
private fun SensorGrid(sensors: SensorPreviewData) {
    val items = listOf(
        "Soil Moisture" to sensors.soilMoisture,
        "Soil Temp"     to sensors.soilTemp,
        "Soil pH"       to sensors.soilPh,
        "Soil N"        to sensors.soilN,
        "Soil P"        to sensors.soilP,
        "Soil K"        to sensors.soilK,
        "Soil EC"       to sensors.soilEc,
        "Air Temp"      to sensors.airTemp,
        "Light"         to sensors.light
    )
    // Manual 3-col grid (LazyVerticalGrid not usable inside verticalScroll)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items.chunked(3).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { (name, value) ->
                    SensorMiniCard(name = name, value = value, modifier = Modifier.weight(1f))
                }
                // Fill empty slots in last row
                repeat(3 - row.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SensorMiniCard(name: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(White, RoundedCornerShape(10.dp))
            .border(1.dp, GreenLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Column {
            Text(
                text      = name.uppercase(),
                fontSize  = 8.5.sp,
                fontFamily = DMSans,
                fontWeight = FontWeight.Medium,
                color     = GrayMedium,
                letterSpacing = 0.3.sp,
                maxLines  = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text      = value,
                fontSize  = 11.5.sp,
                fontFamily = DMSans,
                fontWeight = FontWeight.Medium,
                color     = Black
            )
        }
    }
}

@Composable
private fun OnTheGoDeviceOption(
    device: OnTheGoDevice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) GreenPrimary else Border,
        label = "device_border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) GreenBgLight else White,
        label = "device_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Small device icon tile
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
            Text(text = device.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = Black)
            Text(text = device.meta, fontSize = 10.sp, fontFamily = DMSans, color = GrayMedium)
        }

        // Radio button
        Box(
            modifier = Modifier
                .size(16.dp)
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
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary)
                )
            }
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
    bodyText: String
) {
    val bg     = if (isAmber) WarningBg  else OffWhite
    val border = if (isAmber) WarningBorder else Border
    val titleColor = if (isAmber) Warning else Black
    val bodyColor  = if (isAmber) Warning else GrayBody
    val iconTint   = when {
        isAmber      -> Warning
        useGreenIcon -> GreenPrimary
        else         -> GrayMedium
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        // Info circle icon
        Canvas(
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp)
        ) {
            val r = size.minDimension / 2f
            drawCircle(color = iconTint, radius = r, style = Stroke(width = 1.2.dp.toPx()))
            // vertical line
            drawLine(iconTint, start = Offset(r, r * 0.6f), end = Offset(r, r * 1.3f), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
            // dot
            drawCircle(color = iconTint, radius = 0.7.dp.toPx(), center = Offset(r, r * 1.65f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = titleText,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color      = titleColor
            )
            Text(
                text       = bodyText,
                fontSize   = 11.sp,
                fontFamily = DMSans,
                color      = bodyColor,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun CheckRow(text: String, checked: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // Check / X circle icon
        Canvas(modifier = Modifier.size(14.dp).padding(top = 2.dp)) {
            val r = size.minDimension / 2f
            if (checked) {
                drawCircle(color = GreenBgLight, radius = r)
                drawCircle(color = GreenLight, radius = r, style = Stroke(1.dp.toPx()))
                // checkmark
                val path = Path().apply {
                    moveTo(r * 0.55f, r)
                    lineTo(r * 0.85f, r * 1.35f)
                    lineTo(r * 1.45f, r * 0.65f)
                }
                drawPath(path, color = GreenPrimary, style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            } else {
                drawCircle(color = OffWhite, radius = r)
                drawCircle(color = GrayLight, radius = r, style = Stroke(1.dp.toPx()))
                // X
                drawLine(GrayHint, start = Offset(r * 0.6f, r * 0.6f), end = Offset(r * 1.4f, r * 1.4f), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
                drawLine(GrayHint, start = Offset(r * 1.4f, r * 0.6f), end = Offset(r * 0.6f, r * 1.4f), strokeWidth = 1.4.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Text(
            text       = text,
            fontSize   = 12.sp,
            fontFamily = DMSans,
            color      = if (checked) GrayBody else GrayHint,
            lineHeight = 18.sp,
            modifier   = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(
        text      = text.uppercase(),
        style     = SectionLabelStyle,
        color     = GrayMedium,
        modifier  = Modifier.padding(top = 4.dp)
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
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = White)
    }
}

@Composable
private fun OutlineButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(White)
            .border(1.5.dp, GreenPrimary, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = GreenPrimary)
    }
}

@Composable
private fun TextLinkButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text           = label,
            fontSize       = 12.sp,
            fontWeight     = FontWeight.Medium,
            fontFamily     = DMSans,
            color          = GrayMedium,
            textDecoration = TextDecoration.Underline
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "7A — Linked & Connected", showSystemUi = true)
@Composable
fun PreAnalysis7APreview() {
    SiliGuideTheme {
        PreAnalysisScreen(
            state = PreAnalysisState.LinkedConnected(
                deviceName = "Garden Sensor",
                deviceMeta = "LAN Mode · 192.168.1.42",
                sensors = SensorPreviewData(),
                deviceId = "dev_1"
            ),
            onBack = {},
            permissionStatus = PermissionStatus.Granted,
            showPermissionDialog = false,

            onRequestCameraPermission = {},

            onOpenSettings = {},

            onPermissionDenied = {},

            onContinueToCamera = {},

            onContinueWithOtherDevice = {},

            onSkipToImageOnly = {},

            onContinueImageOnly = {},

            onAddIoTDevice = {}
        )
    }
}

@Preview(showBackground = true, name = "7B — Linked & Disconnected", showSystemUi = true)
@Composable
fun PreAnalysis7BPreview() {
    SiliGuideTheme {
        PreAnalysisScreen(
            state = PreAnalysisState.LinkedDisconnected(
                deviceName = "Garden Sensor",
                lastSeenLabel = "Last seen 14 min ago",
                onTheGoDevices = listOf(
                    OnTheGoDevice(
                        "dev_2",
                        "Backup Sensor",
                        "AP Mode · Connected — for this analysis only"
                    ),
                    OnTheGoDevice(
                        "dev_3",
                        "Field Unit",
                        "LAN Mode · Connected — for this analysis only"
                    )
                )
            ),
            onBack = {},
            permissionStatus = PermissionStatus.Granted,
            showPermissionDialog = false,

            onRequestCameraPermission = {},

            onOpenSettings = {},

            onPermissionDenied = {},

            onContinueToCamera = {},

            onContinueWithOtherDevice = {},

            onSkipToImageOnly = {},

            onContinueImageOnly = {},

            onAddIoTDevice = {}
        )
    }
}

@Preview(showBackground = true, name = "7C — No Device", showSystemUi = true)
@Composable
fun PreAnalysis7CPreview() {
    SiliGuideTheme {
        PreAnalysisScreen(
            state = PreAnalysisState.NoDevice,
            onBack = {},
            permissionStatus = PermissionStatus.Granted,
            showPermissionDialog = false,

            onRequestCameraPermission = {},

            onOpenSettings = {},

            onPermissionDenied = {},

            onContinueToCamera = {},

            onContinueWithOtherDevice = {},

            onSkipToImageOnly = {},

            onContinueImageOnly = {},

            onAddIoTDevice = {}
        )
    }
}