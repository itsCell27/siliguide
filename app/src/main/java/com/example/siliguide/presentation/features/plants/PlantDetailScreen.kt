package com.example.siliguide.presentation.features.plants

import android.text.format.DateFormat
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// DATA MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class AnalysisHistoryItem(
    val sessionId: String = "",
    val symptom: String,
    val condition: String,
    val confidence: Int,
    val isFullAnalysis: Boolean,
    val timestamp: String,
    val imagePath: String?,
    val gradientStart: Color,
    val gradientEnd: Color
)

data class SensorReading(
    val name: String,
    val value: String,
    val level: ThresholdLevel,
    val isStale: Boolean = false
)

enum class ThresholdLevel { NORMAL, LOW, HIGH, UNKNOWN }

/** Monitoring card variant driven by linked device state */
enum class MonitoringState { NO_DEVICE, DISCONNECTED, CONNECTED }

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.US)
    return sdf.format(Date(timestamp))
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlantDetailScreen(
    plantId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: () -> Unit,
    onNavigateToSeeAllHistory: (plantId: String) -> Unit = {},
    onNavigateToAnalysisDetail: (sessionId: String) -> Unit = {},
    onNavigateToLinkDevice: () -> Unit = {},
    viewModel: PlantViewModel = hiltViewModel()
) {
    val plant            by viewModel.plantState.collectAsState()
    val analysisHistory  by viewModel.analysisHistoryState.collectAsState()

    LaunchedEffect(plantId) { viewModel.loadPlant(plantId) }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMenu          by remember { mutableStateOf(false) }

    // ── Derive display values from domain ─────────────────────────────────
    val plantName        = plant?.name ?: "Loading..."
    val linkedDevice     = plant?.deviceId
    val isDeviceLinked   = linkedDevice != null
    val isDeviceConnected = plant?.deviceId != null   // TODO: wire to real connectivity state
    val addedDate        = plant?.dateCreated?.let {
        SimpleDateFormat("MMM d", Locale.US).format(Date(it))
    } ?: "—"
    val latestSession    = analysisHistory.firstOrNull()

    val monitoringState  = when {
        !isDeviceLinked      -> MonitoringState.NO_DEVICE
        !isDeviceConnected   -> MonitoringState.DISCONNECTED
        else                 -> MonitoringState.CONNECTED
    }

    val fabIsFullAnalysis = monitoringState == MonitoringState.CONNECTED
    val fabNote = when (monitoringState) {
        MonitoringState.NO_DEVICE     -> "No sensor linked — result will be image-only (partial)"
        MonitoringState.DISCONNECTED  -> "Sensor disconnected — result will be image-only (partial)"
        MonitoringState.CONNECTED     -> null
    }

    val sensorData = listOf(
        SensorReading("Moist",   "${latestSession?.soilMoisture?.toInt()}%",  ThresholdLevel.NORMAL, !isDeviceConnected),
        SensorReading("Soil pH", "${latestSession?.soilPh}",                   ThresholdLevel.NORMAL, !isDeviceConnected),
        SensorReading("Soil T",  "${latestSession?.soilTemp?.toInt()}°",       ThresholdLevel.NORMAL, !isDeviceConnected),
        SensorReading("EC",      "${latestSession?.electricalConductivity}",   ThresholdLevel.NORMAL, !isDeviceConnected),
        SensorReading("N",       "${latestSession?.nitrogen}",                 ThresholdLevel.NORMAL, !isDeviceConnected),
        SensorReading("P",       "${latestSession?.phosphorus}",               ThresholdLevel.NORMAL, !isDeviceConnected),
        SensorReading("K",       "${latestSession?.potassium}",                ThresholdLevel.NORMAL, !isDeviceConnected),
        SensorReading("Light",   "${latestSession?.lightIntensity}k",          ThresholdLevel.NORMAL, !isDeviceConnected),
        SensorReading("Temp",    "${latestSession?.airTemp?.toInt()}°",        ThresholdLevel.NORMAL, !isDeviceConnected)
    )

    val displayHistory = analysisHistory.take(3).map { session ->
        AnalysisHistoryItem(
            sessionId      = session.sessionId,
            symptom        = session.predictedClass,
            condition      = session.plantCondition ?: "No issues detected",
            confidence     = (session.confidence * 100).toInt(),
            isFullAnalysis = !session.isPartial,
            timestamp      = formatTimestamp(session.sessionTimestamp),
            imagePath = session.imagePath,
            gradientStart  = Color(0xFF7BA34C),
            gradientEnd    = Color(0xFF3B6D11)
        )
    }

    Log.d("PlantDetailScreen", "displayHistory: $displayHistory")

    // ── Root layout ───────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            PlantDetailTopBar(
                plantName   = plantName,
                showMenu    = showMenu,
                onBack      = onNavigateBack,
                onMenuToggle = { showMenu = !showMenu },
                onMenuDismiss = { showMenu = false },
                onRename    = { showMenu = false /* TODO */ },
                onDelete    = { showDeleteConfirm = true; showMenu = false }
            )

            // Scrollable body
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 14.dp, bottom = 180.dp),  // bottom = space for FAB
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1 — Plant info card
                PlantInfoCard(
                    plantName   = plantName,
                    addedDate   = addedDate,
                    deviceName  = if (isDeviceLinked) (plant?.deviceId ?: "") else null
                )

                // 2 — Monitoring section
                SectionHeader(
                    title   = if (monitoringState == MonitoringState.CONNECTED) "Live monitoring" else "Monitoring",
                    seeAll  = null
                )
                MonitoringCard(
                    state        = monitoringState,
                    deviceName   = plant?.deviceId ?: "Garden Sensor",
                    sensors      = if (isDeviceLinked) sensorData else emptyList(),
                    onLinkDevice = onNavigateToLinkDevice
                )

                // 3 — Analysis history
                SectionHeader(
                    title  = "Analysis history · ${analysisHistory.size}",
                    seeAll = if (analysisHistory.isNotEmpty()) "See All" else null,
                    onSeeAll = { onNavigateToSeeAllHistory(plantId) }
                )

                if (displayHistory.isEmpty()) {
                    Text(
                        "No analysis records yet",
                        fontSize = 13.sp,
                        color = GrayMedium,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp)
                    )
                } else {
                    displayHistory.forEach { item ->
                        AnalysisHistoryRow(
                            item    = item,
                            compact = true,
                            onClick = { onNavigateToAnalysisDetail(item.sessionId) }
                        )
                    }
                }
            }
        }

        // ── FAB overlay ───────────────────────────────────────────────────
        FabSection(
            isFullAnalysis = fabIsFullAnalysis,
            note           = fabNote,
            modifier       = Modifier.align(Alignment.BottomCenter),
            onClick        = onNavigateToAnalysis
        )

        // ── Delete modal ──────────────────────────────────────────────────
        if (showDeleteConfirm) {
            DeletePlantModal(
                plantName = plantName,
                onConfirm = {
                    viewModel.deletePlant(plantId) {
                        // 1. Close the modal first
                        showDeleteConfirm = false

                        // 2. Then navigate away
                        onNavigateBack()
                    }
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlantDetailTopBar(
    plantName: String,
    showMenu: Boolean,
    onBack: () -> Unit,
    onMenuToggle: () -> Unit,
    onMenuDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
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
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint = GrayBody,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text       = plantName,
                style      = MaterialTheme.typography.headlineSmall,
                color      = Black,
                modifier   = Modifier.weight(1f)
            )

            // ⋮ button
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
                        .clickable { onMenuToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    ThreeDotsIcon(tint = if (showMenu) GreenPrimary else GrayMedium)
                }

                // FIX: Use DropdownMenu instead of manual if (showMenu) Box
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onMenuDismiss,
                    // Offset to match your design (-130.dp horizontally, slightly below the button)
                    offset = DpOffset(x = (-130).dp, y = 4.dp),
                    modifier = Modifier
                        .width(170.dp)
                        .background(White)
                ) {
                    // --- Rename Option ---
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .clickable { onRename() }
//                            .padding(horizontal = 14.dp, vertical = 12.dp),
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.spacedBy(10.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(26.dp)
//                                .background(GreenBgLight, RoundedCornerShape(7.dp)),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            PencilIcon()
//                        }
//                        Text(
//                            text = "Rename Plant",
//                            fontSize = 13.sp,
//                            fontWeight = FontWeight.Medium,
//                            fontFamily = DMSans,
//                            color = Black
//                        )
//                    }
//
//                    HorizontalDivider(color = Border, thickness = 1.dp)

                    // --- Delete Option ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDelete() }
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
                            TrashIcon()
                        }
                        Text(
                            text = "Delete Plant",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = DMSans,
                            color = Danger
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = Border, thickness = 1.dp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SECTION HEADER
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, seeAll: String?, onSeeAll: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text      = title.uppercase(),
            style     = SectionLabelStyle,
            color     = GrayMedium
        )
        if (seeAll != null) {
            Text(
                text     = seeAll,
                style    = SeeAllStyle,
                color    = GreenPrimary,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PLANT INFO CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PlantInfoCard(
    plantName: String,
    addedDate: String,
    deviceName: String?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Leaf icon tile
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(GreenBgLight, RoundedCornerShape(14.dp))
                .border(1.dp, GreenLight, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(R.drawable.ic_leaf),
                contentDescription = null,
                modifier = Modifier.size(30.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text   = plantName,
                style  = MaterialTheme.typography.titleLarge,
                color  = Black,
                lineHeight = 18.sp
            )
            Row(
                modifier = Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text("Added $addedDate", fontSize = 11.sp, fontFamily = DMSans, color = GrayMedium)
                if (deviceName != null) {
                    Box(modifier = Modifier
                        .size(3.dp)
                        .background(GrayLight, CircleShape))
                    Text(deviceName, fontSize = 11.sp, fontFamily = DMSans, color = GrayMedium)
                } else {
                    Box(modifier = Modifier
                        .size(3.dp)
                        .background(GrayLight, CircleShape))
                    Text("No device linked", fontSize = 11.sp, fontFamily = DMSans, color = GrayHint)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MONITORING CARD (3 states)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun MonitoringCard(
    state: MonitoringState,
    deviceName: String,
    sensors: List<SensorReading>,
    onLinkDevice: () -> Unit
) {
    when (state) {
        MonitoringState.NO_DEVICE -> NoDeviceCard(onLinkDevice = onLinkDevice)

        MonitoringState.DISCONNECTED -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(WarningBg, RoundedCornerShape(14.dp))
                    .border(1.dp, WarningBorder, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeviceStatusRow(
                    deviceName    = deviceName,
                    isConnected   = false
                )
                if (sensors.isNotEmpty()) {
                    SensorGrid3x3(sensors = sensors, isStale = true)
                    Text(
                        text      = "Last update: stale",
                        fontSize  = 10.sp,
                        fontFamily = DMSans,
                        color     = Warning,
                        modifier  = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        MonitoringState.CONNECTED -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GreenBgLight, RoundedCornerShape(14.dp))
                    .border(1.dp, GreenLight, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DeviceStatusRow(
                    deviceName  = deviceName,
                    isConnected = true
                )
                if (sensors.isNotEmpty()) {
                    SensorGrid3x3(sensors = sensors, isStale = false)
                }
            }
        }
    }
}

@Composable
private fun NoDeviceCard(onLinkDevice: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, GrayLight), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(GreenBgLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_iot_device),
                contentDescription = null,
                tint = GrayHint,
                modifier = Modifier.size(24.dp)
            )
        }
        Text("No IoT device linked", fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = GrayBody)
        Text(
            "Link a device to monitor live soil and climate data for this plant.",
            fontSize = 11.sp, fontFamily = DMSans, color = GrayMedium, textAlign = TextAlign.Center, lineHeight = 16.sp
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(White)
                .border(1.5.dp, GreenPrimary, RoundedCornerShape(10.dp))
                .clickable { onLinkDevice() }
                .padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text("Link a Device", fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = GreenPrimary)
        }
    }
}

@Composable
private fun DeviceStatusRow(deviceName: String, isConnected: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(White, RoundedCornerShape(10.dp))
                .border(
                    1.dp,
                    if (isConnected) GreenLight else WarningBorder,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_iot_device),
                contentDescription = null,
                tint = if (isConnected) GreenPrimary else Warning,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text       = deviceName,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color      = Black,
            modifier   = Modifier.weight(1f)
        )
        // Pill
        Row(
            modifier = Modifier
                .background(
                    if (isConnected) GreenPrimary else White,
                    RoundedCornerShape(999.dp)
                )
                .then(
                    if (!isConnected) Modifier.border(
                        1.dp,
                        WarningBorder,
                        RoundedCornerShape(999.dp)
                    )
                    else Modifier
                )
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
                color      = if (isConnected) White else Warning
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3×3 SENSOR GRID
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SensorGrid3x3(sensors: List<SensorReading>, isStale: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        sensors.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                rowItems.forEach { sensor ->
                    SensorCell3x3(
                        sensor  = sensor,
                        isStale = isStale,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun SensorCell3x3(sensor: SensorReading, isStale: Boolean, modifier: Modifier = Modifier) {
    val stale = isStale || sensor.isStale
    Column(
        modifier = modifier
            .background(
                if (stale) OffWhite else White,
                RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.dp,
                color = if (stale) GrayLight else GreenLight,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text      = sensor.name.uppercase(),
            fontSize  = 9.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color     = GrayMedium,
            letterSpacing = 0.3.sp,
            maxLines  = 1
        )
        Text(
            text      = sensor.value,
            fontSize  = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color     = if (stale) GrayHint else Black
        )
        ThresholdChip(level = if (stale) ThresholdLevel.UNKNOWN else sensor.level, isStale = stale)
    }
}

@Composable
fun ThresholdChip(level: ThresholdLevel, isStale: Boolean = false) {
    val (bg, fg, label) = when {
        isStale                   -> Triple(OffWhite,    GrayHint,  "Stale")
        level == ThresholdLevel.NORMAL -> Triple(GreenBgLight, GreenDark, "Norm")
        level == ThresholdLevel.LOW    -> Triple(InfoBg,      Info,      "Low")
        level == ThresholdLevel.HIGH   -> Triple(DangerBg,   Danger,    "High")
        else                           -> Triple(OffWhite,    GrayHint,  "—")
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .then(
                if (isStale) Modifier.border(
                    1.dp,
                    Border,
                    RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text      = label,
            fontSize  = 8.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color     = fg,
            letterSpacing = 0.3.sp
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ANALYSIS HISTORY ROW  (compact = main screen, !compact = AnalysisHistoryScreen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnalysisHistoryRow(
    item: AnalysisHistoryItem,
    compact: Boolean = true,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(if (compact) 12.dp else 13.dp))
            .border(1.dp, Border, RoundedCornerShape(if (compact) 12.dp else 13.dp))
            .clickable { onClick() }
            .padding(if (compact) 10.dp else 12.dp),
        verticalAlignment = if (compact) Alignment.CenterVertically else Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp)
    ) {
        // Gradient thumb
        val thumbSize = if (compact) 55.dp else 60.dp
        val thumbRadius = if (compact) 10.dp else 11.dp

        val image = item.imagePath
        if (!image.isNullOrEmpty() && File(image).exists()) {
            AsyncImage(
                model = File(image),
                contentDescription = "Leaf Image",
                modifier = Modifier
                    .size(thumbSize)
                    .clip(RoundedCornerShape(thumbRadius)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .background(
                        Brush.linearGradient(listOf(item.gradientStart, item.gradientEnd)),
                        RoundedCornerShape(thumbRadius)
                    )
            )
        }


        Column(
            modifier = Modifier.weight(1f),
            // Reduce the gap between the different text elements
            verticalArrangement = Arrangement.spacedBy(if (compact) 1.dp else 2.dp)
        ) {
            Text(
                text      = formatPrediction(item.symptom),
                fontSize  = if (compact) 11.sp else 13.sp,
                // 1. Force a tighter line height
                lineHeight = if (compact) 13.sp else 15.sp,
                // 2. Remove extra top/bottom reserved space
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color     = Black
            )
            Text(
                text     = item.condition,
                fontSize = if (compact) 10.sp else 11.sp,
                lineHeight = if (compact) 12.sp else 13.sp,
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                fontFamily = DMSans,
                color    = GrayBody
            )

            // ... (Full/Partial badge Row)

            Text(
                text     = item.timestamp,
                fontSize = if (compact) 9.sp else 10.sp,
                lineHeight = if (compact) 11.sp else 12.sp,
                style = LocalTextStyle.current.copy(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                fontFamily = DMSans,
                color    = GrayHint
            )
        }

        // Right: confidence badge + chevron
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        when {
                            item.confidence >= 80 -> GreenBgLight
                            item.confidence >= 60 -> WarningBg
                            else -> DangerBg
                        },
                        RoundedCornerShape(5.dp)
                    )
                    // 1. Reduced vertical padding from 3.dp to 1.5.dp
                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text      = "${item.confidence}%",
                    fontSize  = if (compact) 9.sp else 11.sp,
                    // 2. Set tight line height
                    lineHeight = if (compact) 9.sp else 11.sp,
                    // 3. Remove extra reserved font space
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSans,
                    color     = when {
                        item.confidence >= 80 -> GreenDark
                        item.confidence >= 60 -> Warning
                        else                  -> Danger
                    }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FAB SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FabSection(
    isFullAnalysis: Boolean,
    note: String?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val fabBg = if (isFullAnalysis)
        Brush.verticalGradient(listOf(Color.Transparent, GreenBgLight, GreenBgLight))
    else
        Brush.verticalGradient(listOf(Color.Transparent, OffWhite, OffWhite))

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(fabBg)
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 10.dp, bottom = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isFullAnalysis) {
                // Filled dark green button with leaf icon
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GreenPrimary)
                        .clickable { onClick() }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_leaf),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Analyze This Plant", fontSize = 14.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = White)
                    }
                }
            } else {
                // Outlined button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(White)
                        .border(1.5.dp, GreenPrimary, RoundedCornerShape(14.dp))
                        .clickable { onClick() }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        androidx.compose.foundation.Image(
                            painter = painterResource(R.drawable.ic_leaf),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text("Analyze This Plant", fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = GreenPrimary)
                    }
                }
                if (note != null) {
                    Text(
                        text      = note,
                        fontSize  = 10.sp,
                        fontFamily = DMSans,
                        color     = GrayMedium,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DELETE MODAL
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DeletePlantModal(
    plantName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .zIndex(12f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* consume click */ },
            shape  = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // Red trash icon circle
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(DangerBg),
                    contentAlignment = Alignment.Center
                ) {
                    TrashIcon(size = 26, tint = Danger)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text   = "Delete Plant?",
                    style  = MaterialTheme.typography.titleLarge,
                    color  = Black,
                    textAlign = TextAlign.Center
                )
                Text(
                    text      = "This will permanently delete $plantName and all its analysis history. Any linked IoT device will be unlinked but not deleted. This cannot be undone.",
                    fontSize  = 12.sp,
                    fontFamily = DMSans,
                    color     = GrayBody,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier  = Modifier.padding(vertical = 4.dp, horizontal = 4.dp)
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(White)
                            .border(1.5.dp, Border, RoundedCornerShape(11.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Cancel", fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = GrayBody)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(Danger)
                            .clickable { onConfirm() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Delete Plant", fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = White)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ICON HELPERS  (canvas-drawn — no extra drawable dependency)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ThreeDotsIcon(tint: Color = GrayMedium) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(16.dp)) {
        val cx = size.width / 2f
        val r  = 1.3.dp.toPx()
        listOf(3.5f, 8f, 12.5f).forEach { cy ->
            drawCircle(color = tint, radius = r, center = androidx.compose.ui.geometry.Offset(cx, cy.dp.toPx()))
        }
    }
}

@Composable
fun PencilIcon(tint: Color = GreenPrimary) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(13.dp)) {
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.62f, size.height * 0.11f)
            lineTo(size.width * 0.89f, size.height * 0.39f)
            lineTo(size.width * 0.35f, size.height * 0.85f)
            lineTo(size.width * 0.08f, size.height * 0.92f)
            lineTo(size.width * 0.15f, size.height * 0.65f)
            close()
        }
        drawPath(p, color = tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.4.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
fun TrashIcon(size: Int = 13, tint: Color = Danger) {
    androidx.compose.foundation.Canvas(modifier = Modifier.size(size.dp)) {
        val s = this.size
        // lid
        drawLine(tint, start = androidx.compose.ui.geometry.Offset(s.width * 0.15f, s.height * 0.23f), end = androidx.compose.ui.geometry.Offset(s.width * 0.85f, s.height * 0.23f), strokeWidth = 1.4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // handle arc approximation
        drawLine(tint, start = androidx.compose.ui.geometry.Offset(s.width * 0.35f, s.height * 0.23f), end = androidx.compose.ui.geometry.Offset(s.width * 0.35f, s.height * 0.15f), strokeWidth = 1.3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(tint, start = androidx.compose.ui.geometry.Offset(s.width * 0.65f, s.height * 0.23f), end = androidx.compose.ui.geometry.Offset(s.width * 0.65f, s.height * 0.15f), strokeWidth = 1.3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawLine(tint, start = androidx.compose.ui.geometry.Offset(s.width * 0.35f, s.height * 0.15f), end = androidx.compose.ui.geometry.Offset(s.width * 0.65f, s.height * 0.15f), strokeWidth = 1.3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        // body outline path
        val body = androidx.compose.ui.graphics.Path().apply {
            moveTo(s.width * 0.23f, s.height * 0.23f)
            lineTo(s.width * 0.27f, s.height * 0.88f)
            cubicTo(s.width * 0.27f, s.height * 0.93f, s.width * 0.31f, s.height * 0.96f, s.width * 0.37f, s.height * 0.96f)
            lineTo(s.width * 0.63f, s.height * 0.96f)
            cubicTo(s.width * 0.69f, s.height * 0.96f, s.width * 0.73f, s.height * 0.93f, s.width * 0.73f, s.height * 0.88f)
            lineTo(s.width * 0.77f, s.height * 0.23f)
        }
        drawPath(body, color = tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        // lines inside body
        listOf(0.42f to 0.35f, 0.58f to 0.35f).forEach { (cx, _) ->
            drawLine(tint, start = androidx.compose.ui.geometry.Offset(s.width * cx, s.height * 0.38f), end = androidx.compose.ui.geometry.Offset(s.width * cx, s.height * 0.78f), strokeWidth = 1.3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        }
    }
}

fun formatLastAnalyzed(millis: Long): String {

    val elapsed = System.currentTimeMillis() - millis

    val minutes = elapsed / 60_000
    val hours = elapsed / 3_600_000
    val days = elapsed / 86_400_000

    return when {
        elapsed < 0L -> DateFormat.format("MMM dd, yyyy", millis).toString()
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
        hours < 24 -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
        days < 7 -> "$days ${if (days == 1L) "day" else "days"} ago"
        else -> DateFormat.format("MMM dd, yyyy", millis).toString()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

// Use PlantDetailPreviewContent for previews (no ViewModel)
@Composable
private fun PlantDetailPreviewContent(
    monitoringState: MonitoringState,
    historyCount: Int
) {
    val dummyHistory = List(historyCount) { i ->
        AnalysisHistoryItem(
            sessionId      = "s$i",
            symptom        = listOf("Leaf Curling", "Healthy", "Yellowing Leaves")[i % 3],
            condition      = listOf("Underwatering / Drought stress", "No issues detected", "Possible: Nitrogen deficiency")[i % 3],
            confidence     = listOf(89, 94, 62)[i % 3],
            isFullAnalysis = i % 2 == 0,
            timestamp      = listOf("Apr 28, 2026 · 9:41 AM", "Apr 24, 2026 · 2:14 PM", "Apr 20, 2026 · 10:30 AM")[i % 3],
            imagePath = null,
            gradientStart  = listOf(Color(0xFF7BA34C), Color(0xFF3B6D11), Color(0xFFA8B460))[i % 3],
            gradientEnd    = listOf(Color(0xFF3B6D11), Color(0xFF1F3909), Color(0xFF6A8030))[i % 3]
        )
    }
    val dummySensors = listOf(
        SensorReading("Moist",   "58%",   ThresholdLevel.NORMAL),
        SensorReading("Soil pH", "6.6",   ThresholdLevel.NORMAL),
        SensorReading("Soil T",  "26°",   ThresholdLevel.NORMAL),
        SensorReading("EC",      "420",   ThresholdLevel.NORMAL),
        SensorReading("N",       "36",    ThresholdLevel.NORMAL),
        SensorReading("P",       "22",    ThresholdLevel.NORMAL),
        SensorReading("K",       "185",   ThresholdLevel.NORMAL),
        SensorReading("Light",   "22k",   ThresholdLevel.NORMAL),
        SensorReading("Temp",    "28.4°", ThresholdLevel.NORMAL)
    )
    val plantName = "Labuyo Plant #2"
    val fabIsFullAnalysis = monitoringState == MonitoringState.CONNECTED
    val fabNote = when (monitoringState) {
        MonitoringState.NO_DEVICE    -> "No sensor linked — result will be image-only (partial)"
        MonitoringState.DISCONNECTED -> "Sensor disconnected — result will be image-only (partial)"
        else                         -> null
    }

    Box(Modifier
        .fillMaxSize()
        .background(OffWhite)) {
        Column(Modifier.fillMaxSize()) {
            PlantDetailTopBar(plantName, false, {}, {}, {}, {}, {})
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 14.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PlantInfoCard(plantName = plantName, addedDate = "Apr 10", deviceName = if (monitoringState != MonitoringState.NO_DEVICE) "Garden Sensor" else null)
                SectionHeader(title = if (monitoringState == MonitoringState.CONNECTED) "Live monitoring" else "Monitoring", seeAll = null)
                MonitoringCard(state = monitoringState, deviceName = "Garden Sensor", sensors = if (monitoringState != MonitoringState.NO_DEVICE) dummySensors else emptyList(), onLinkDevice = {})
                SectionHeader(title = "Analysis history · ${dummyHistory.size}", seeAll = if (dummyHistory.isNotEmpty()) "See All" else null)
                dummyHistory.forEach { AnalysisHistoryRow(it, compact = true) }
            }
        }
        FabSection(isFullAnalysis = fabIsFullAnalysis, note = fabNote, modifier = Modifier.align(Alignment.BottomCenter), onClick = {})
    }
}

fun formatPrediction(string: String): String {
    return when (string.lowercase()) {
        "curling" -> "Leaf Curling"
        "healthy" -> "Healthy Leaf"
        "spots" -> "Leaf Spots"
        "yellowing" -> "Yellowing Leaf"
        else -> "Unknown"
    }
}

@Preview(showBackground = true, name = "11A — No device", showSystemUi = true)
@Composable fun PlantDetail11APreview() { SiliGuideTheme { PlantDetailPreviewContent(MonitoringState.NO_DEVICE, 2) } }

@Preview(showBackground = true, name = "11B — Disconnected", showSystemUi = true)
@Composable fun PlantDetail11BPreview() { SiliGuideTheme { PlantDetailPreviewContent(MonitoringState.DISCONNECTED, 4) } }

@Preview(showBackground = true, name = "11C — Connected", showSystemUi = true)
@Composable fun PlantDetail11CPreview() { SiliGuideTheme { PlantDetailPreviewContent(MonitoringState.CONNECTED, 4) } }