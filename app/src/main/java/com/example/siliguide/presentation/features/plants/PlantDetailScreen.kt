package com.example.siliguide.presentation.features.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun PlantDetailScreen(
    plantId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAnalysis: () -> Unit
) {
    val viewModel: PlantViewModel = hiltViewModel()
    val plant by viewModel.plantState.collectAsState()
    val analysisHistory by viewModel.analysisHistoryState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Load plant on enter
    LaunchedEffect(plantId) {
        viewModel.loadPlant(plantId)
    }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Get latest analysis session for sensors + current status
    val latestSession = analysisHistory.firstOrNull()

    // Replace DUMMY DATA with real data:
    val plantName = plant?.name ?: "Loading..."
    val linkedDevice = plant?.deviceId ?: "No device"
    val isDeviceConnected = plant?.deviceId != null
    val currentCondition = latestSession?.plantCondition ?: "No analysis yet"
    val currentSymptom = latestSession?.predictedClass ?: "Unknown"
    val lastAnalyzedAt = if (latestSession != null) formatTimestamp(latestSession.sessionTimestamp) else "Never"

    // Convert analysisHistory to AnalysisHistoryItem for display
    val displayHistory = analysisHistory.map { session ->
        AnalysisHistoryItem(
            symptom = session.predictedClass,
            condition = session.plantCondition ?: "No issues detected",
            confidence = session.confidence.toInt(),
            isFullAnalysis = !session.isPartial,
            timestamp = formatTimestamp(session.sessionTimestamp),
            gradientStart = Color(0xFF7BA34C),
            gradientEnd = Color(0xFF3B6D11)
        )
    }

    // Get sensors from LATEST session, not Plant
    val sensorData = listOf(
        SensorReading("Soil Moisture", "${latestSession?.soilMoisture?.toInt()}%", ThresholdLevel.NORMAL),
        SensorReading("Soil pH", "${latestSession?.soilPh}", ThresholdLevel.NORMAL),
        SensorReading("Soil Temp", "${latestSession?.soilTemp?.toInt()}°C", ThresholdLevel.NORMAL),
        SensorReading("EC", "${latestSession?.electricalConductivity}", ThresholdLevel.NORMAL),
        SensorReading("Nitrogen (N)", "${latestSession?.nitrogen} mg/kg", ThresholdLevel.NORMAL),
        SensorReading("Phosphorus (P)", "${latestSession?.phosphorus} mg/kg", ThresholdLevel.NORMAL),
        SensorReading("Potassium (K)", "${latestSession?.potassium} mg/kg", ThresholdLevel.NORMAL),
        SensorReading("Light Intensity", "${latestSession?.lightIntensity?.toInt()} lux", ThresholdLevel.NORMAL),
        SensorReading("Air Temp", "${latestSession?.airTemp?.toInt()}°C", ThresholdLevel.NORMAL)
    )

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(40.dp)) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(OffWhite)
                                .border(1.dp, Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = GrayBody,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Text(
                        text = plantName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Fraunces,
                        color = Black,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    )

                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { showMenu = !showMenu }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "Menu", tint = GrayMedium)
                        }
                        PlantMenuDropdown(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            onRename = { showMenu = false },
                            onDelete = {
                                showDeleteConfirm = true
                                showMenu = false
                            }
                        )
                    }
                }
                HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 20.dp))
            }
        },
        floatingActionButton = {
            Button(
                onClick = onNavigateToAnalysis,
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("New Analysis", fontSize = 15.sp, fontWeight = FontWeight.Medium, color = White)
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = OffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Plant Card
            PlantInfoCard(
                plantName = plantName,
                linkedDevice = linkedDevice,
                isDeviceConnected = isDeviceConnected
            )

            // Current Status
            CurrentStatusCard(
                condition = currentCondition,
                symptom = currentSymptom,
                timestamp = lastAnalyzedAt,
                isDeviceConnected = isDeviceConnected
            )

            // Sensor Grid - only show if has latest session
            if (latestSession != null && isDeviceConnected) {
                SensorGridCard(sensors = sensorData)
            }

            // Analysis History
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "ANALYSIS HISTORY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GrayMedium,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "See all",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = GreenPrimary
                    )
                }

                if (analysisHistory.isEmpty()) {
                    Text(
                        "No analysis records yet",
                        fontSize = 13.sp,
                        color = GrayMedium,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    displayHistory.forEach { item ->
                        AnalysisHistoryCard(item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Delete Confirmation Modal
    if (showDeleteConfirm) {
        DeletePlantModal(
            plantName = plantName,
            onConfirm = {
                onNavigateBack()
            },
            onDismiss = {
                showDeleteConfirm = false
            }
        )
    }
}

@Composable
fun PlantInfoCard(
    plantName: String,
    linkedDevice: String,
    isDeviceConnected: Boolean
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
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(GreenBgLight, RoundedCornerShape(14.dp))
                .border(1.dp, GreenLight, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = GreenPrimary,
                modifier = Modifier.size(28.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                plantName,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = Black,
                lineHeight = 18.sp
            )
            Row(
                modifier = Modifier.padding(top = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    linkedDevice,
                    fontSize = 11.sp,
                    color = GrayMedium
                )
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .background(GrayLight, CircleShape)
                )
                Text(
                    if (isDeviceConnected) "Connected" else "Disconnected",
                    fontSize = 11.sp,
                    color = if (isDeviceConnected) GreenPrimary else Warning
                )
            }
        }
    }
}

@Composable
fun CurrentStatusCard(
    condition: String,
    symptom: String,
    timestamp: String,
    isDeviceConnected: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        GreenBgLight.copy(alpha = 0.8f),
                        White
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, GreenLight, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        symptom,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Black
                    )
                    Text(
                        condition,
                        fontSize = 13.sp,
                        color = GreenDark,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                Pill(
                    label = if (isDeviceConnected) "LIVE" else "STALE",
                    isLive = isDeviceConnected
                )
            }

            Text(
                timestamp,
                fontSize = 11.sp,
                color = GrayMedium
            )
        }
    }
}

@Composable
fun SensorGridCard(sensors: List<SensorReading>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "SENSOR READINGS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = GrayMedium,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(White, RoundedCornerShape(14.dp))
                .border(1.dp, GreenLight, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            for (i in 0 until 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(65.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (j in 0 until 3) {
                        val index = i * 3 + j
                        if (index < sensors.size) {
                            SensorCell(sensors[index])
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SensorCell(sensor: SensorReading) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(White, RoundedCornerShape(10.dp))
            .border(1.dp, GreenLight, RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            sensor.name,
            fontSize = 9.sp,
            color = GrayMedium,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp,
            maxLines = 1
        )
        Text(
            sensor.value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Black
        )
        ThresholdChip(sensor.level)
    }
}

@Composable
fun ThresholdChip(level: ThresholdLevel) {
    val (bgColor, textColor) = when (level) {
        ThresholdLevel.NORMAL -> GreenBgLight to GreenDark
        ThresholdLevel.LOW -> InfoBg to Info
        ThresholdLevel.HIGH -> DangerBg to Danger
        ThresholdLevel.UNKNOWN -> OffWhite to GrayHint
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            level.name,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            letterSpacing = 0.3.sp
        )
    }
}

@Composable
fun AnalysisHistoryCard(item: AnalysisHistoryItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    Brush.linearGradient(
                        listOf(item.gradientStart, item.gradientEnd)
                    ),
                    RoundedCornerShape(8.dp)
                )
        )

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                item.symptom,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Black
            )
            Text(
                item.condition,
                fontSize = 11.sp,
                color = GrayMedium
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(
                    modifier = Modifier
                        .background(
                            if (item.isFullAnalysis) GreenBgLight else OffWhite,
                            RoundedCornerShape(4.dp)
                        )
                        .border(
                            1.dp,
                            if (item.isFullAnalysis) GreenLight else Border,
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        if (item.isFullAnalysis) "Full analysis" else "Partial · image only",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (item.isFullAnalysis) GreenDark else GrayMedium
                    )
                }
                Text(
                    item.timestamp,
                    fontSize = 10.sp,
                    color = GrayMedium
                )
            }
        }

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(
                modifier = Modifier
                    .background(
                        when {
                            item.confidence >= 80 -> GreenBgLight
                            item.confidence >= 60 -> WarningBg
                            else -> DangerBg
                        },
                        RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    "${item.confidence}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        item.confidence >= 80 -> GreenDark
                        item.confidence >= 60 -> Warning
                        else -> Danger
                    }
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = GrayHint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun Pill(label: String, isLive: Boolean) {
    Row(
        modifier = Modifier
            .background(
                if (isLive) GreenPrimary else WarningBg,
                RoundedCornerShape(999.dp)
            )
            .border(
                if (isLive) 0.dp else 1.dp,
                if (isLive) Color.Transparent else WarningBorder,
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(if (isLive) White else Warning, CircleShape)
        )
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = if (isLive) White else Warning
        )
    }
}

@Composable
fun PlantMenuDropdown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(White)
    ) {
        DropdownMenuItem(
            text = { Text("Rename Plant") },
            onClick = onRename
        )
        DropdownMenuItem(
            text = { Text("Delete Plant", color = Danger) },
            onClick = onDelete
        )
    }
}

@Composable
fun DeletePlantModal(
    plantName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable(enabled = true) { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .width(280.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Danger,
                    modifier = Modifier.size(48.dp)
                )

                Text(
                    "Delete $plantName?",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    "All analysis history and linked device will be removed. This cannot be undone.",
                    fontSize = 13.sp,
                    color = GrayMedium,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OffWhite),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel", color = Black, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Danger),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Delete", color = White, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ──────────── DATA CLASSES ────────────

data class AnalysisHistoryItem(
    val symptom: String,
    val condition: String,
    val confidence: Int,
    val isFullAnalysis: Boolean,
    val timestamp: String,
    val gradientStart: Color,
    val gradientEnd: Color
)

data class SensorReading(
    val name: String,
    val value: String,
    val level: ThresholdLevel
)

enum class ThresholdLevel {
    NORMAL,
    LOW,
    HIGH,
    UNKNOWN
}

// Utility function
private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy · h:mm a", Locale.US)
    return sdf.format(Date(timestamp))
}