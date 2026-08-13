package com.example.siliguide.presentation.features.iot.device_detail

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.window.Dialog
import com.example.siliguide.core.ui.components.MessageDialog
import com.example.siliguide.presentation.features.iot.DEVICE_NAME_MAX_LENGTH

// ── UI models (presentation-only; map to domain ConnectionMode later) ───────

enum class DetailConnectionMode(
    val shortLabel: String,
    val badgeLabel: String,
    val optionTitle: String,
    val optionDesc: String,
    val switchLabel: String,
    val switchWarning: String
) {
    AP(
        shortLabel = "AP",
        badgeLabel = "📡 AP",
        optionTitle = "AP Mode",
        optionDesc = "Phone connects directly to the device's own WiFi. Best for on-the-go field use.",
        switchLabel = "Switch to AP Mode",
        switchWarning = "Switching to AP Mode will disconnect the device from your home WiFi. You'll need to reconnect your phone to the device's hotspot (SiliGuide_IoTSensors) to continue using it."
    ),
    LAN(
        shortLabel = "LAN",
        badgeLabel = "🏠 LAN",
        optionTitle = "LAN Mode",
        optionDesc = "Device stays on your home WiFi. Works offline if phone is on the same network.",
        switchLabel = "Switch to LAN Mode",
        switchWarning = "Switching to LAN Mode requires a short setup — you'll need to provide your home WiFi credentials so the device can join your router. The setup wizard will open automatically after confirming."
    ),
    BLUETOOTH(
        shortLabel = "BT",
        badgeLabel = "🔵 BT",
        optionTitle = "BT Mode",
        optionDesc = "Phone pairs directly over Bluetooth. Short range, no WiFi needed.",
        switchLabel = "Switch to BT Mode",
        switchWarning = "Switching to BT Mode pairs your phone directly with the device over Bluetooth. Keep the phone within a few meters of the device — no WiFi is used."
    )
}

enum class DeviceConnectionStatus { CONNECTED, OFFLINE, RECONNECTING }

data class SensorReadingUi(
    val label: String,
    val value: String
)

data class LinkedPlantUi(
    val name: String,
    val meta: String
)

data class IoTDeviceDetailUiState(
    val deviceName: String = "Garden Sensor",
    val status: DeviceConnectionStatus = DeviceConnectionStatus.CONNECTED,
    val mode: DetailConnectionMode = DetailConnectionMode.LAN,
    val sensors: List<SensorReadingUi> = emptyList(),
    val lastSeenText: String? = null,
    val staleNote: String? = null,
    val offlineNote: String = "Device not reachable. Make sure the device is powered on and your phone is on the same WiFi network.",
    val linkedPlant: LinkedPlantUi? = null,
    val deviceIdentifier: String = "A4:CF:12:7E:9B:34",
    val firmware: String = "v2.1.4",
    val dateRegistered: String = "Apr 10, 2026",

    val isModeChangeSheetOpen: Boolean = false
)

// ── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IoTDeviceDetailScreen(
    viewModel: IoTDeviceDetailViewModel,
    state: IoTDeviceDetailUiState,
    onBack: () -> Unit = {},
    onLinkPlant: () -> Unit = {},
    onUnlinkPlant: () -> Unit = {},
    onReconnect: () -> Unit = {},
    onRenameDevice: (String) -> Unit = {},
    onRemoveDevice: () -> Unit = {},
    onModeChangeConfirmed: (DetailConnectionMode) -> Unit = {}
) {

    val offlineWarning by viewModel.offlineWarningMessage.collectAsState()
    var showRemoveDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showUnlinkDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var selectedMode by remember(state.mode) { mutableStateOf(state.mode) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(PageFrameBg)
        ) {

            DetailTopBar(
                title = state.deviceName,
                showMenu = showMenu,
                onBack = onBack,
                onMenuToggle = { showMenu = !showMenu },
                onMenuDismiss = { showMenu = false },
                onRename = {
                    showMenu = false
                    showRenameDialog = true
                },
                onRemove = {
                    showMenu = false
                    showRemoveDialog = true
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                StatusCard(
                    state = state,
                    onReconnect = onReconnect
                )

                SectionHeading("Linked plant")

                if (state.linkedPlant != null) {

                    LinkedPlantCard(
                        plant = state.linkedPlant,
                        dimmed = state.status == DeviceConnectionStatus.RECONNECTING,
                        onUnlink = { showUnlinkDialog = true }
                    )
                } else {

                    NoLinkedPlantCard(onLinkPlant = onLinkPlant)
                }

                SectionHeading("Device info")

                DeviceInfoCard(
                    state = state,
                    dimmed = state.status == DeviceConnectionStatus.RECONNECTING,
                    onConnectionModeClick = {
                        selectedMode = state.mode
                        viewModel.requestModeChange()
                    }
                )
            }
        }

        if (showRenameDialog) {

            RenameDeviceDialog(
                currentName = state.deviceName,
                onCancel = { showRenameDialog = false },
                onConfirm = { newName ->
                    showRenameDialog = false
                    onRenameDevice(newName)
                }
            )
        }

        if (showRemoveDialog) {

            RemoveDeviceDialog(
                deviceName = state.deviceName,
                linkedPlantName = state.linkedPlant?.name,
                onCancel = { showRemoveDialog = false },
                onConfirm = {
                    showRemoveDialog = false
                    onRemoveDevice()
                }
            )
        }

        if (showUnlinkDialog && state.linkedPlant != null) {
            UnlinkPlantDialog(
                plantName = state.linkedPlant.name,
                onCancel = { showUnlinkDialog = false },
                onConfirm = {
                    showUnlinkDialog = false
                    onUnlinkPlant()
                }
            )
        }
    }

    // --- OFFLINE WARNING DIALOG ---
    offlineWarning?.let { message ->
        MessageDialog(
            title = "Device Offline",
            message = message,
            confirmText = "Got it",
            onConfirm = { viewModel.dismissOfflineWarning() }
        )
    }

    // 12C / 12D — Change Connection Mode sheet (drag down to close)
    if (state.isModeChangeSheetOpen) {

        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissModeChangeSheet() },
            sheetState = sheetState,
            containerColor = White,
            scrimColor = ModalScrim,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = { SheetHandleBar() }
        ) {

            ChangeConnectionModeSheet(
                currentMode = state.mode,
                selectedMode = selectedMode,
                onSelectMode = { selectedMode = it },
                onConfirm = {
                    scope.launch {
                        sheetState.hide()
                        viewModel.dismissModeChangeSheet()
                        onModeChangeConfirmed(selectedMode)
                    }
                },
                onCancel = {
                    scope.launch {
                        sheetState.hide()
                        viewModel.dismissModeChangeSheet()
                    }
                }
            )
        }
    }
}

// ── Top bar ────────────────────────────────────────────────────────────────

@Composable
private fun DetailTopBar(
    title: String,
    showMenu: Boolean,
    onBack: () -> Unit,
    onMenuToggle: () -> Unit,
    onMenuDismiss: () -> Unit,
    onRename: () -> Unit,
    onRemove: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            CircleIconButton(onClick = onBack) {

                Canvas(modifier = Modifier.size(14.dp)) {

                    drawPath(
                        path = Path().apply {
                            moveTo(size.width * 0.64f, size.height * 0.79f)
                            lineTo(size.width * 0.36f, size.height * 0.5f)
                            lineTo(size.width * 0.64f, size.height * 0.21f)
                        },
                        color = GrayBody,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialThemeHeadline,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // ⋮ menu — matches PlantDetailTopBar
            Box {

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (showMenu) GreenBgLight else OffWhite)
                        .border(
                            width = if (showMenu) 1.5.dp else 1.dp,
                            color = if (showMenu) GreenLight else Border,
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onMenuToggle
                        ),
                    contentAlignment = Alignment.Center
                ) {

                    Canvas(modifier = Modifier.size(14.dp)) {

                        drawThreeDots(
                            color = if (showMenu) GreenPrimary else GrayMedium
                        )
                    }
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onMenuDismiss,
                    offset = DpOffset(x = (-130).dp, y = 4.dp),
                    modifier = Modifier
                        .width(180.dp)
                        .background(White)
                ) {

                    // --- Rename ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRename() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(GreenBgLight, RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(13.dp)) {
                                drawPencil(
                                    color = GreenPrimary,
                                    strokeWidthPx = 1.4.dp.toPx()
                                )
                            }
                        }
                        Text(
                            text = "Rename Device",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = DMSans,
                            color = Black
                        )
                    }

                    HorizontalDivider(color = Border, thickness = 1.dp)

                    // --- Remove ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRemove() }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(DangerBg, RoundedCornerShape(7.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(13.dp)) {
                                drawTrashGlyph(
                                    color = Danger,
                                    strokeWidthPx = 1.4.dp.toPx()
                                )
                            }
                        }
                        Text(
                            text = "Remove Device",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = DMSans,
                            color = Danger
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border)
        )
    }
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(OffWhite)
            .border(width = 1.dp, color = Border, shape = CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SectionHeading(text: String) {

    Text(
        text = text.uppercase(),
        style = SectionLabelStyle,
        color = GrayMedium,
        modifier = Modifier.padding(horizontal = 2.dp)
    )
}

// ── Status hero card (12A / 12E / 12F) ─────────────────────────────────────

@Composable
private fun StatusCard(
    state: IoTDeviceDetailUiState,
    onReconnect: () -> Unit
) {

    val isOffline = state.status == DeviceConnectionStatus.OFFLINE

    val bg = if (isOffline) WarningBg else GreenBgLight
    val bd = if (isOffline) WarningBorder else GreenLight
    val tileStroke = if (isOffline) GrayMedium else GreenPrimary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(width = 1.dp, color = bd, shape = RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(White)
                    .border(
                        width = 1.dp,
                        color = bd,
                        shape = RoundedCornerShape(11.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {

                Canvas(modifier = Modifier.size(22.dp)) {

                    drawDeviceGlyph(
                        color = tileStroke,
                        strokeWidthPx = 1.4.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = state.deviceName,
                    style = DeviceNameStyle,
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(3.dp))

                ModeChip(mode = state.mode)
            }

            Spacer(modifier = Modifier.width(10.dp))

            when (state.status) {

                DeviceConnectionStatus.CONNECTED -> ConnectedPill()

                DeviceConnectionStatus.OFFLINE -> OfflinePill()

                DeviceConnectionStatus.RECONNECTING -> Unit
            }
        }

        if (isOffline && state.lastSeenText != null) {

            Text(
                text = "⏱ ${state.lastSeenText}",
                style = BadgeLabelStyle,
                color = Warning,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (isOffline) {

            WarningNote(
                text = state.offlineNote,
                background = White.copy(alpha = 0.6f)
            )
        }

        if (state.sensors.isNotEmpty() && state.status != DeviceConnectionStatus.RECONNECTING) {

            SensorGrid(
                sensors = state.sensors,
                isStale = isOffline
            )
        }

        if (isOffline && state.staleNote != null) {

            Text(
                text = state.staleNote,
                style = BadgeLabelStyle,
                color = Warning,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when (state.status) {

            DeviceConnectionStatus.OFFLINE -> ReconnectButton(onClick = onReconnect)

            DeviceConnectionStatus.RECONNECTING -> ReconnectingButton()

            DeviceConnectionStatus.CONNECTED -> Unit
        }

        if (state.status == DeviceConnectionStatus.RECONNECTING) {

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = GreenPrimary,
                trackColor = GreenLight
            )
        }
    }
}

@Composable
private fun ModeChip(mode: DetailConnectionMode) {

    val isLan = mode == DetailConnectionMode.LAN

    Text(
        text = mode.shortLabel,
        style = WizardMicroLabelStyle,
        color = if (isLan) GreenDark else GrayMedium,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(White)
            .border(
                width = 1.dp,
                color = if (isLan) GreenLight else Border,
                shape = RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun ConnectedPill() {

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GreenPrimary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(White)
        )

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = "Connected",
            style = BadgeLabelStyle,
            color = White
        )
    }
}

@Composable
private fun OfflinePill() {

    Text(
        text = "Offline",
        style = BadgeLabelStyle,
        color = GrayBody,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GrayLight)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun SensorGrid(
    sensors: List<SensorReadingUi>,
    isStale: Boolean
) {

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        sensors.chunked(3).forEach { rowItems ->

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                rowItems.forEach { sensor ->

                    SensorCell(
                        sensor = sensor,
                        isStale = isStale,
                        modifier = Modifier.weight(1f)
                    )
                }

                repeat(3 - rowItems.size) {

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun SensorCell(
    sensor: SensorReadingUi,
    isStale: Boolean,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(White)
            .border(
                width = 1.dp,
                color = if (isStale) GrayLight else GreenLight,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {

        Text(
            text = sensor.label.uppercase(),
            style = SensorCellLabelStyle,
            color = if (isStale) GrayHint else GrayMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = sensor.value,
            style = SensorCellValueStyle,
            color = if (isStale) GrayHint else Black,
            maxLines = 1
        )
    }
}

@Composable
private fun WarningNote(
    text: String,
    background: Color = WarningBg
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = WarningBorder,
                shape = RoundedCornerShape(9.dp)
            )
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {

        Canvas(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(13.dp)
        ) {

            drawInfoGlyph(
                color = Warning,
                strokeWidthPx = 1.2.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = text,
            style = WizardCaptionStyle,
            color = Warning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ReconnectButton(onClick: () -> Unit) {

    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(White)
            .border(
                width = 1.5.dp,
                color = GreenPrimary,
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Canvas(modifier = Modifier.size(14.dp)) {

            drawRefreshGlyph(
                color = GreenPrimary,
                strokeWidthPx = 1.4.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = "Try to Reconnect",
            style = SheetButtonStyleFallback,
            color = GreenPrimary
        )
    }
}

@Composable
private fun ReconnectingButton() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(GreenBgLight)
            .border(
                width = 1.5.dp,
                color = GreenLight,
                shape = RoundedCornerShape(11.dp)
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = GreenPrimary,
            trackColor = GreenLight,
            strokeWidth = 2.dp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Reconnecting…",
            style = SheetButtonStyleFallback,
            color = GreenDark
        )
    }
}

// ── Linked plant ───────────────────────────────────────────────────────────

@Composable
private fun LinkedPlantCard(
    plant: LinkedPlantUi,
    dimmed: Boolean,
    onUnlink: () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alphaIf(dimmed)
            .clip(RoundedCornerShape(14.dp))
            .background(White)
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(GreenBgLight)
                .border(
                    width = 1.dp,
                    color = GreenLight,
                    shape = RoundedCornerShape(11.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Canvas(modifier = Modifier.size(24.dp)) {

                drawLeafGlyph(filled = true, color = GreenPrimary, strokeWidthPx = 0f)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = plant.name,
                style = WizardBodyMediumStyle,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

//            Spacer(modifier = Modifier.height(1.dp))
//
//            Text(
//                text = plant.meta,
//                style = WizardSubCaptionStyle,
//                color = GrayMedium,
//                maxLines = 1,
//                overflow = TextOverflow.Ellipsis
//            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(
            modifier = Modifier.clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onUnlink
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Canvas(modifier = Modifier.size(11.dp)) {

                drawUnlinkGlyph(
                    color = Danger,
                    strokeWidthPx = 1.3.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = "Unlink",
                style = WizardCaptionStyle,
                color = Danger
            )
        }
    }
}

@Composable
private fun NoLinkedPlantCard(onLinkPlant: () -> Unit) {

    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(White)
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(OffWhite)
                .border(
                    width = 1.dp,
                    color = GrayLight,
                    shape = RoundedCornerShape(14.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            Canvas(modifier = Modifier.size(26.dp)) {

                drawLeafGlyph(
                    filled = false,
                    color = GrayHint,
                    strokeWidthPx = 1.4.dp.toPx()
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "No plant linked yet",
            style = WizardBodyMediumStyle,
            color = Black
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Link this device to a plant to start tracking sensor readings and analyses.",
            style = WizardSubCaptionStyle,
            color = GrayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(GreenPrimary)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onLinkPlant
                )
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Canvas(modifier = Modifier.size(12.dp)) {

                drawPlusGlyph(
                    color = White,
                    strokeWidthPx = 1.6.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "Link to a plant",
                style = WizardBodyMediumStyle,
                color = White
            )
        }
    }
}

// ── Device info card ───────────────────────────────────────────────────────

@Composable
private fun DeviceInfoCard(
    state: IoTDeviceDetailUiState,
    dimmed: Boolean,
    onConnectionModeClick: () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alphaIf(dimmed)
            .clip(RoundedCornerShape(14.dp))
            .background(White)
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp)
    ) {

        InfoRow(
            label = "Device identifier",
            value = state.deviceIdentifier,
            isMono = true
        )

        RowDivider()

        InfoRow(
            label = "Firmware",
            value = state.firmware,
            isMono = true
        )

        RowDivider()

        InfoRow(
            label = "Date registered",
            value = state.dateRegistered,
            isMono = false
        )

        RowDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onConnectionModeClick
                )
                .padding(vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Connection mode",
                style = WizardCaptionStyle,
                color = GrayMedium,
                modifier = Modifier.weight(1f)
            )

            ModeBadge(mode = state.mode)

            Spacer(modifier = Modifier.width(8.dp))

            Canvas(modifier = Modifier.size(14.dp)) {

                drawPath(
                    path = Path().apply {
                        moveTo(size.width * 0.39f, size.height * 0.25f)
                        lineTo(size.width * 0.64f, size.height * 0.5f)
                        lineTo(size.width * 0.39f, size.height * 0.75f)
                    },
                    color = GrayHint,
                    style = Stroke(
                        width = 1.4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    isMono: Boolean
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = label,
            style = WizardCaptionStyle,
            color = GrayMedium,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = value,
            style = if (isMono) InfoValueMonoStyle else WizardCaptionStyle,
            color = if (isMono) Black else GrayBody,
            maxLines = 1
        )
    }
}

@Composable
private fun RowDivider() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border)
    )
}

@Composable
private fun ModeBadge(mode: DetailConnectionMode) {

    val isLan = mode == DetailConnectionMode.LAN

    Text(
        text = mode.badgeLabel,
        style = BadgeLabelStyle,
        color = if (isLan) GreenDark else GrayBody,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (isLan) GreenBgLight else OffWhite)
            .border(
                width = 1.dp,
                color = if (isLan) GreenLight else Border,
                shape = RoundedCornerShape(5.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

// ── Change Connection Mode sheet (12C / 12D) ───────────────────────────────

@Composable
private fun SheetHandleBar() {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(GrayLight)
        )
    }
}

@Composable
private fun ChangeConnectionModeSheet(
    currentMode: DetailConnectionMode,
    selectedMode: DetailConnectionMode,
    onSelectMode: (DetailConnectionMode) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {

    val confirmInteraction = remember { MutableInteractionSource() }
    val cancelInteraction = remember { MutableInteractionSource() }
    val isChanged = selectedMode != currentMode

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 32.dp)
    ) {

        Text(
            text = "Change Connection Mode",
            style = WizardTitleStyle,
            color = Black
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Current mode: ${currentMode.badgeLabel} Mode. Select a new mode for this device.",
            style = WizardBodyStyle,
            color = GrayMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        DetailConnectionMode.entries.forEach { mode ->

            ModeOptionCard(
                mode = mode,
                isSelected = mode == selectedMode,
                isCurrent = mode == currentMode,
                onClick = { onSelectMode(mode) }
            )

            Spacer(modifier = Modifier.height(9.dp))
        }

        if (isChanged) {

            WarningNote(text = selectedMode.switchWarning)

            Spacer(modifier = Modifier.height(14.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .alphaIf(!isChanged)
                .clip(RoundedCornerShape(12.dp))
                .background(GreenPrimary)
                .clickable(
                    enabled = isChanged,
                    interactionSource = confirmInteraction,
                    indication = null,
                    onClick = onConfirm
                )
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = selectedMode.switchLabel,
                style = SheetButtonStyleFallback,
                color = White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cancel",
            style = WizardBodyStyle,
            color = GrayMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = cancelInteraction,
                    indication = null,
                    onClick = onCancel
                )
                .padding(vertical = 6.dp)
        )
    }
}

@Composable
private fun ModeOptionCard(
    mode: DetailConnectionMode,
    isSelected: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(13.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isSelected) GreenBgLight else White)
            .border(
                width = if (isSelected) 2.dp else 1.5.dp,
                color = if (isSelected) GreenPrimary else Border,
                shape = shape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top
    ) {

        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) GreenPrimary else OffWhite)
                .border(
                    width = if (isSelected) 0.dp else 1.dp,
                    color = if (isSelected) Color.Transparent else Border,
                    shape = RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {

            val glyphColor = if (isSelected) White else GrayMedium

            Canvas(modifier = Modifier.size(20.dp)) {

                when (mode) {

                    DetailConnectionMode.AP -> drawApGlyph(
                        color = glyphColor,
                        strokeWidthPx = 1.4.dp.toPx()
                    )

                    DetailConnectionMode.LAN -> drawDeviceGlyph(
                        color = glyphColor,
                        strokeWidthPx = 1.4.dp.toPx()
                    )

                    DetailConnectionMode.BLUETOOTH -> drawBluetoothGlyph(
                        color = glyphColor,
                        strokeWidthPx = 1.4.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {

            Row(verticalAlignment = Alignment.CenterVertically) {

                Text(
                    text = mode.optionTitle,
                    style = WizardBodyMediumStyle,
                    color = if (isSelected) GreenDark else Black
                )

                if (isCurrent) {

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = "Current",
                        style = BadgeLabelStyle,
                        color = if (isSelected) GreenDark else GrayBody,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) GreenBgLight else OffWhite)
                            .border(
                                width = 1.dp,
                                color = if (isSelected) GreenLight else Border,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = mode.optionDesc,
                style = WizardCaptionStyle,
                color = if (isSelected) GreenPrimary else GrayMedium
            )
        }

        Spacer(modifier = Modifier.width(11.dp))

        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(17.dp)
                .clip(CircleShape)
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

// ── Remove device dialog (12B) ─────────────────────────────────────────────

@Composable
private fun RemoveDeviceDialog(
    deviceName: String,
    linkedPlantName: String?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {

    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    val cancelInteraction = remember { MutableInteractionSource() }
    val removeInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ModalScrim)
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = onCancel
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(White)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = { /* swallow taps */ }
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DangerBg),
                contentAlignment = Alignment.Center
            ) {

                Canvas(modifier = Modifier.size(24.dp)) {

                    drawTrashGlyph(
                        color = Danger,
                        strokeWidthPx = 1.6.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Remove $deviceName?",
                style = DialogTitleStyle,
                color = Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (linkedPlantName != null)
                    "This device will be unlinked from $linkedPlantName and removed from your account. Past sensor history won't be deleted."
                else
                    "This device will be removed from your account. Past sensor history won't be deleted.",
                style = WizardBodyStyle,
                color = GrayBody,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(White)
                        .border(
                            width = 1.5.dp,
                            color = Border,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable(
                            interactionSource = cancelInteraction,
                            indication = null,
                            onClick = onCancel
                        )
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Cancel",
                        style = WizardBodyMediumStyle,
                        color = GrayBody
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Danger)
                        .clickable(
                            interactionSource = removeInteraction,
                            indication = null,
                            onClick = onConfirm
                        )
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {

                    Text(
                        text = "Remove",
                        style = WizardBodyMediumStyle,
                        color = White
                    )
                }
            }
        }
    }
}


// ── Rename Dialog ──────────────────────────────────────────────────────────

@Composable
private fun RenameDeviceDialog(
    currentName: String,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {

    var name by remember(currentName) { mutableStateOf(currentName) }

    val trimmed = name.trim()
    val canSave = trimmed.isNotEmpty() && trimmed != currentName

    Dialog(onDismissRequest = onCancel) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "Rename Device",
                style = MaterialThemeHeadline,
                color = Black
            )

            Text(
                text = "Give this device a name you'll recognize on the dashboard.",
                fontSize = 12.sp,
                fontFamily = DMSans,
                color = GrayBody,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(White)
                    .border(
                        width = 1.5.dp,
                        color = if (trimmed.isEmpty()) GreenLight else GreenPrimary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                Canvas(modifier = Modifier.size(13.dp)) {
                    drawDeviceGlyph(
                        color = GreenPrimary,
                        strokeWidthPx = 1.3.dp.toPx()
                    )
                }

                BasicTextField(
                    value = name,
                    onValueChange = { typed ->
                        if (typed.length <= DEVICE_NAME_MAX_LENGTH) name = typed
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = Black
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(GreenPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (name.isEmpty()) {
                                Text(
                                    text = "Device name",
                                    fontSize = 12.sp,
                                    fontFamily = DMSans,
                                    color = GrayHint
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Text(
                    text = "${name.length}/$DEVICE_NAME_MAX_LENGTH",
                    fontSize = 10.sp,
                    fontFamily = DMSans,
                    color = GreenPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OffWhite)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = GrayBody
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSave) GreenPrimary else GreenLight)
                        .then(
                            if (canSave) {
                                Modifier.clickable { onConfirm(trimmed) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = White
                    )
                }
            }
        }
    }
}


// ── Unlink plant dialog ──────────────────────────────────────────────────

@Composable
private fun UnlinkPlantDialog(
    plantName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val scrimInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }
    val cancelInteraction = remember { MutableInteractionSource() }
    val unlinkInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ModalScrim)
            .clickable(
                interactionSource = scrimInteraction,
                indication = null,
                onClick = onCancel
            )
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(White)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = { /* swallow taps */ }
                )
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DangerBg),
                contentAlignment = Alignment.Center
            ) {
                // Swapped to the unlink glyph
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawUnlinkGlyph(
                        color = Danger,
                        strokeWidthPx = 1.6.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Unlink $plantName?",
                style = DialogTitleStyle,
                color = Black,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "This device will no longer track data for this plant. Past sensor history will remain saved.",
                style = WizardBodyStyle,
                color = GrayBody,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(White)
                        .border(
                            width = 1.5.dp,
                            color = Border,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable(
                            interactionSource = cancelInteraction,
                            indication = null,
                            onClick = onCancel
                        )
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = WizardBodyMediumStyle,
                        color = GrayBody
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Danger)
                        .clickable(
                            interactionSource = unlinkInteraction,
                            indication = null,
                            onClick = onConfirm
                        )
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unlink",
                        style = WizardBodyMediumStyle,
                        color = White
                    )
                }
            }
        }
    }
}


// ── Small helpers ──────────────────────────────────────────────────────────

/** 13sp medium button label (matches M3 titleMedium in this theme). */
private val SheetButtonStyleFallback = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp
)

private fun Modifier.alphaIf(dimmed: Boolean): Modifier =
    if (dimmed) this.then(Modifier.graphicsLayerAlpha(0.7f)) else this

private fun Modifier.graphicsLayerAlpha(value: Float): Modifier =
    graphicsLayer {
        alpha = value
    }

/** Page title style — 18sp Fraunces (M3 headlineSmall in this theme). */
private val MaterialThemeHeadline = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Bold,
    fontSize = 18.sp,
    letterSpacing = (-0.3).sp
)

// ── Canvas glyphs ──────────────────────────────────────────────────────────

private fun DrawScope.drawPencil(color: Color, strokeWidthPx: Float) {

    val s = size.minDimension / 13f

    drawPath(
        path = Path().apply {
            moveTo(9f * s, 1.5f * s)
            lineTo(11.5f * s, 4f * s)
            lineTo(4.5f * s, 11f * s)
            lineTo(1f * s, 12f * s)
            lineTo(2f * s, 8.5f * s)
            close()
        },
        color = color,
        style = Stroke(width = strokeWidthPx)
    )
}

private fun DrawScope.drawDeviceGlyph(color: Color, strokeWidthPx: Float) {

    val s = size.minDimension / 22f
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    drawRoundRect(
        color = color,
        topLeft = Offset(3f * s, 6f * s),
        size = androidx.compose.ui.geometry.Size(16f * s, 11f * s),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.4f * s),
        style = stroke
    )

    drawPath(
        path = Path().apply {
            moveTo(7f * s, 6f * s)
            lineTo(7f * s, 4.5f * s)
            quadraticTo(7f * s, 3.5f * s, 8f * s, 3.5f * s)
            lineTo(14f * s, 3.5f * s)
            quadraticTo(15f * s, 3.5f * s, 15f * s, 4.5f * s)
            lineTo(15f * s, 6f * s)
        },
        color = color,
        style = stroke
    )

    drawCircle(
        color = color,
        radius = 1.6f * s,
        center = Offset(11f * s, 11.5f * s)
    )
}

private fun DrawScope.drawApGlyph(color: Color, strokeWidthPx: Float) {

    val s = size.minDimension / 20f
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    drawPath(
        path = Path().apply {
            moveTo(2f * s, 7f * s)
            quadraticTo(10f * s, 1f * s, 18f * s, 7f * s)
        },
        color = color,
        style = stroke
    )

    drawPath(
        path = Path().apply {
            moveTo(4.5f * s, 9.5f * s)
            quadraticTo(10f * s, 5.5f * s, 15.5f * s, 9.5f * s)
        },
        color = color,
        style = stroke
    )

    drawCircle(
        color = color,
        radius = 1.8f * s,
        center = Offset(10f * s, 13f * s)
    )
}

private fun DrawScope.drawBluetoothGlyph(color: Color, strokeWidthPx: Float) {

    val s = size.minDimension / 20f
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    drawPath(
        path = Path().apply {
            moveTo(6f * s, 4f * s)
            lineTo(14f * s, 12f * s)
            lineTo(10f * s, 16f * s)
            lineTo(10f * s, 4f * s)
            lineTo(14f * s, 8f * s)
            lineTo(6f * s, 16f * s)
        },
        color = color,
        style = stroke
    )
}

private fun DrawScope.drawLeafGlyph(filled: Boolean, color: Color, strokeWidthPx: Float) {

    val w = size.width
    val h = size.height

    val leaf = Path().apply {
        moveTo(w * 0.5f, h * 0.13f)
        cubicTo(w * 0.85f, h * 0.30f, w * 0.88f, h * 0.75f, w * 0.5f, h * 0.92f)
        cubicTo(w * 0.12f, h * 0.75f, w * 0.15f, h * 0.30f, w * 0.5f, h * 0.13f)
        close()
    }

    if (filled) {

        drawPath(path = leaf, color = color)
        return
    }

    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    drawPath(path = leaf, color = color, style = stroke)

    drawLine(
        color = color,
        start = Offset(w * 0.5f, h * 0.92f),
        end = Offset(w * 0.5f, h * 0.46f),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawCircle(
        color = color,
        radius = w * 0.08f,
        center = Offset(w * 0.5f, h * 0.42f),
        style = Stroke(width = strokeWidthPx)
    )
}

private fun DrawScope.drawUnlinkGlyph(color: Color, strokeWidthPx: Float) {

    val w = size.width
    val h = size.height

    drawLine(
        color = color,
        start = Offset(w * 0.2f, h * 0.2f),
        end = Offset(w * 0.8f, h * 0.8f),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = Offset(w * 0.8f, h * 0.2f),
        end = Offset(w * 0.2f, h * 0.8f),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawPlusGlyph(color: Color, strokeWidthPx: Float) {

    val w = size.width
    val h = size.height

    drawLine(
        color = color,
        start = Offset(w * 0.5f, h * 0.17f),
        end = Offset(w * 0.5f, h * 0.83f),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = Offset(w * 0.17f, h * 0.5f),
        end = Offset(w * 0.83f, h * 0.5f),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawInfoGlyph(color: Color, strokeWidthPx: Float) {

    val w = size.width
    val h = size.height

    drawCircle(
        color = color,
        radius = w * 0.42f,
        style = Stroke(width = strokeWidthPx)
    )

    drawLine(
        color = color,
        start = Offset(w * 0.5f, h * 0.30f),
        end = Offset(w * 0.5f, h * 0.53f),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawCircle(
        color = color,
        radius = w * 0.055f,
        center = Offset(w * 0.5f, h * 0.69f)
    )
}

private fun DrawScope.drawRefreshGlyph(color: Color, strokeWidthPx: Float) {

    val s = size.minDimension / 14f
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    drawArc(
        color = color,
        startAngle = -40f,
        sweepAngle = 290f,
        useCenter = false,
        topLeft = Offset(2f * s, 2f * s),
        size = androidx.compose.ui.geometry.Size(10f * s, 10f * s),
        style = stroke
    )

    drawPath(
        path = Path().apply {
            moveTo(2f * s, 4f * s)
            lineTo(2f * s, 7f * s)
            lineTo(5f * s, 7f * s)
        },
        color = color,
        style = stroke
    )
}

private fun DrawScope.drawTrashGlyph(color: Color, strokeWidthPx: Float) {

    val s = size.minDimension / 24f
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    drawLine(
        color = color,
        start = Offset(5f * s, 8f * s),
        end = Offset(19f * s, 8f * s),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawPath(
        path = Path().apply {
            moveTo(9f * s, 8f * s)
            lineTo(9f * s, 6f * s)
            quadraticTo(9f * s, 4f * s, 11f * s, 4f * s)
            lineTo(13f * s, 4f * s)
            quadraticTo(15f * s, 4f * s, 15f * s, 6f * s)
            lineTo(15f * s, 8f * s)
        },
        color = color,
        style = stroke
    )

    drawPath(
        path = Path().apply {
            moveTo(7f * s, 8f * s)
            lineTo(8f * s, 20f * s)
            quadraticTo(8f * s, 22f * s, 10f * s, 22f * s)
            lineTo(14f * s, 22f * s)
            quadraticTo(16f * s, 22f * s, 16f * s, 20f * s)
            lineTo(17f * s, 8f * s)
        },
        color = color,
        style = stroke
    )
}

/** Vertical ⋮ — three dots, same weight as the plant screen's icon. */
private fun DrawScope.drawThreeDots(color: Color) {

    val radius = size.minDimension * 0.09f
    val centerX = size.width / 2f

    listOf(0.22f, 0.5f, 0.78f).forEach { fraction ->
        drawCircle(
            color = color,
            radius = radius,
            center = Offset(centerX, size.height * fraction)
        )
    }
}

// ── Previews ───────────────────────────────────────────────────────────────

private val SampleSensors = listOf(
    SensorReadingUi("Soil Moisture", "52%"),
    SensorReadingUi("Soil Temp", "26.8°C"),
    SensorReadingUi("Soil pH", "6.6"),
    SensorReadingUi("Soil N", "48 ppm"),
    SensorReadingUi("Soil P", "26 ppm"),
    SensorReadingUi("Soil K", "162 ppm"),
    SensorReadingUi("Soil EC", "1.3"),
    SensorReadingUi("Air Temp", "28.4°C"),
    SensorReadingUi("Light", "9.1k lx")
)

private val ConnectedSample = IoTDeviceDetailUiState(
    deviceName = "Garden Sensor",
    status = DeviceConnectionStatus.CONNECTED,
    mode = DetailConnectionMode.LAN,
    sensors = SampleSensors,
    linkedPlant = LinkedPlantUi(
        name = "Labuyo Plant #2",
        meta = "Linked Apr 10 · 4 analyses"
    )
)

private val OfflineSample = ConnectedSample.copy(
    status = DeviceConnectionStatus.OFFLINE,
    lastSeenText = "Last seen 14 min ago",
    staleNote = "Showing last known readings · 14 min ago"
)

//@Preview(name = "12A · 360x740", showSystemUi = true, widthDp = 360, heightDp = 740)
//@Composable
//private fun PreviewConnected() {
//
//    SiliGuideTheme {
//
//        IoTDeviceDetailScreen(state = ConnectedSample)
//    }
//}
//
//@Preview(name = "12E · 320x640", showSystemUi = true, widthDp = 320, heightDp = 640)
//@Composable
//private fun PreviewOffline() {
//
//    SiliGuideTheme {
//
//        IoTDeviceDetailScreen(state = OfflineSample)
//    }
//}
//
//@Preview(name = "12F · 412x915", showSystemUi = true, widthDp = 412, heightDp = 915)
//@Composable
//private fun PreviewReconnecting() {
//
//    SiliGuideTheme {
//
//        IoTDeviceDetailScreen(
//            state = ConnectedSample.copy(
//                status = DeviceConnectionStatus.RECONNECTING,
//                linkedPlant = null
//            )
//        )
//    }
//}