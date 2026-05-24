package com.example.siliguide.presentation.features.analysis

import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.ColumnScope
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.domain.engine.RuleResult
import com.example.siliguide.domain.engine.ThresholdLevel

data class SensorRowData(
    val name: String,
    val value: String,
    val level: ThresholdLevel,
    val iconType: SensorIconType
)

// Icon variants — drawn inline via Canvas, no drawable resources needed
enum class SensorIconType {
    SOIL_TEMP,       // line-graph path
    SOIL_PH,         // circle + crosshair
    SOIL_EC,         // 3 horizontal lines
    SOIL_MOISTURE,   // water-drop fill
    LETTER_N,        // text "N"
    LETTER_P,        // text "P"
    LETTER_K,        // text "K"
    LIGHT,           // sun: filled circle + 4 rays
    AIR_TEMP         // thermometer stem + bulb
}

@Composable
fun AnalysisResultScreen(
    imageUri: String,
    plantId: String? = null,
    viewModel: AnalysisViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSaveAsNote: () -> Unit
) {
    val result = viewModel.ruleResult
    val prediction = viewModel.prediction
    val isLoading = viewModel.isAnalyzing
    val heatmapOverlay = viewModel.heatmapOverlay
    val context = LocalContext.current
    val bitmapState = remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val sensorUiState = viewModel.sensorUiState
    val predictionName = viewModel.predictionName

    LaunchedEffect(imageUri) {
        if (result == null && !isLoading) {
            try {
                val uri = imageUri.toUri()
                val stream = context.contentResolver.openInputStream(uri)
                val bmp = BitmapFactory.decodeStream(stream)
                stream?.close()
                if (bmp != null) {
                    bitmapState.value = bmp
                    viewModel.analyzeImage(bmp)
                }
            } catch (_: Exception) {
            }
        }
    }

    val confidence = prediction?.confidence ?: 0f
    val confidencePct = (confidence * 100).toInt()
    val isOutOfScope = !isLoading && (result == null || confidencePct < 50)

    Scaffold(
        topBar = {
            ResultTopBar(
                isOutOfScope = isOutOfScope,
                onBack = onNavigateBack
            )
        },
        bottomBar = {
            if (!isLoading) {
                ResultBottomBar(
                    isOutOfScope = isOutOfScope,
                    onPrimary = if (isOutOfScope) onNavigateBack else onSaveAsNote,
                    onSecondary = onNavigateBack
                )
            }
        },
        containerColor = OffWhite
    ) { padding ->

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary)
                }
            }

            isOutOfScope -> {
                OutOfScopeContent(
                    confidencePct = confidencePct,
                    bitmap = bitmapState.value,
                    heatmapOverlay = heatmapOverlay,
                    padding = padding
                )
            }

            else -> {
                ResultContent(
                    result = result!!,
                    confidence = confidence,
                    predictedClass = predictionName ?: "Unknown",
                    bitmap = bitmapState.value,
                    heatmapOverlay = heatmapOverlay,
                    padding = padding,
                    sensorUiState = sensorUiState,
                )
            }
        }
    }
}

@Composable
fun ResultTopBar(
    isOutOfScope: Boolean,
    onBack: () -> Unit
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
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OffWhite)
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = GrayBody,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isOutOfScope) "Couldn't Analyze" else "Analysis Results",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Fraunces,
                    color = Black,
                    letterSpacing = (-0.3f).sp,
                    lineHeight = 18.sp
                )
                Text(
                    text = if (isOutOfScope) "Image rejected · just now" else "Labuyo plant · just now",
                    fontSize = 11.sp,
                    color = GrayMedium,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        HorizontalDivider(color = Border)
    }
}

@Composable
fun ResultBottomBar(
    isOutOfScope: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .border(BorderStroke(1.dp, Border), RoundedCornerShape(0.dp))
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 10.dp)
            .padding(top = 12.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Button(
            onClick = onPrimary,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isOutOfScope) "Try Another Photo" else "Save Analysis",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = White
            )
        }
    }
}

@Composable
fun ResultContent(
    result: RuleResult,
    confidence: Float,
    predictedClass: String,
    bitmap: android.graphics.Bitmap?,
    heatmapOverlay: android.graphics.Bitmap?,
    padding: PaddingValues,
    sensorUiState: List<SensorRowData>
) {
    val pct = (confidence * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Info banner - only if no sensors
        if (!result.isUsingSensors) {
            InfoBannerCard()
        }

        // Leaf image - centered, 1:1 aspect
        LeafImageCard(bitmap = bitmap, heatmapOverlay = heatmapOverlay)

        // Prediction card
        PredictionResultCard(
            predictedClass = predictedClass,
            confidence = confidence,
            pct = pct
        )

        // Plant condition or partial diagnosis
        if (result.isUsingSensors && result.condition != null) {
            PlantConditionCard(
                condition = result.condition,
                description = result.conditionDescription ?: "Unknown"
            )
        } else if (result.isUsingSensors) {
            PlantConditionCard(
                condition = "Unknown Condition",
                description = "Unknown"
            )
            PossibleConditionsCard(causes = result.possibleCauses)
            CharacteristicsDescriptionCard(prediction = predictedClass, characteristics = result.characteristics)
        } else {
            PossibleConditionsCard(causes = result.possibleCauses)
            CharacteristicsDescriptionCard(prediction = predictedClass, characteristics = result.characteristics)
        }

        // Draw Sensor Readings if state is available
        if (result.isUsingSensors && sensorUiState.isNotEmpty()) {
            SensorReadingsCard(sensors = sensorUiState)
        }

        // Care recommendations
        if (result.isUsingSensors && result.recommendations.isNotEmpty()) {
            CareRecommendationsCard(items = result.recommendations)
        } else if (!result.isUsingSensors) {
            LockedRecommendationsCard()
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

// ─── LeafImageCard (heatmap overlay) ─────────────────────────────────────────

@Composable
fun LeafImageCard(
    bitmap: android.graphics.Bitmap?,
    heatmapOverlay: android.graphics.Bitmap? = null
) {
    if (bitmap == null) return

    var showHeatmap by remember { mutableStateOf(false) }

    val heatmapAlpha by animateFloatAsState(
        targetValue = if (showHeatmap) 0.55f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "heatmap_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .background(LeafBgDark)
    ) {
        // Layer 1 — leaf photo
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Cropped leaf",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2 — CAM heatmap
        if (heatmapOverlay != null) {
            Image(
                bitmap = heatmapOverlay.asImageBitmap(),
                contentDescription = "AI Heatmap Overlay",
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(4.dp)
                    .graphicsLayer(alpha = heatmapAlpha)
            )

            // Layer 3 — legend top-right
            if (showHeatmap) {
                HeatmapLegend(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }

        // Layer 4 — eye toggle button bottom-right
        if (heatmapOverlay != null) {
            HeatmapToggleButton(
                isActive = showHeatmap,
                onToggle = { showHeatmap = !showHeatmap },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }
    }
}

@Composable
fun HeatmapToggleButton(
    isActive: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isActive) Color(0xFFFF8A4C) else White

    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.1f))
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(16.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidthPx = 2.dp.toPx()
            val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

            val eyePath = Path().apply {
                moveTo(0f, h / 2f)
                cubicTo(w * 0.25f, 0f,   w * 0.75f, 0f,   w,       h / 2f)
                cubicTo(w * 0.75f, h,    w * 0.25f, h,    0f,      h / 2f)
                close()
            }
            drawPath(eyePath, color = iconTint, style = stroke)
            drawCircle(color = iconTint, radius = h * 0.17f, center = Offset(w / 2f, h / 2f))

            if (!isActive) {
                drawLine(
                    color = iconTint,
                    start = Offset(w * 0.12f, h * 0.12f),
                    end   = Offset(w * 0.88f, h * 0.88f),
                    strokeWidth = strokeWidthPx,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun HeatmapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
            .border(1.dp, White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "AI Focus:  Low",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            color = White.copy(alpha = 0.85f),
            letterSpacing = 0.3.sp
        )
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        colorStops = arrayOf(
                            0.00f to Color(0xFF1A4DFF),
                            0.28f to Color(0xFF36F0A0),
                            0.55f to Color(0xFFFFE000),
                            0.80f to Color(0xFFFF7A1F),
                            1.00f to Color(0xFFFF1A1A)
                        )
                    )
                )
        )
        Text(
            text = "High",
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Medium,
            color = White.copy(alpha = 0.85f),
            letterSpacing = 0.3.sp
        )
    }
}

// ─── Sensor Readings Card — Screen 10A ───────────────────────────────────────
//
// Header: plain white bg, title "SENSOR READINGS · AT ANALYSIS" (ink), "9 sensors" (muted)
// Body top padding: 4dp. Bottom padding: 8dp.
// Each row: 3-column grid — [icon+name] [value] [chip]
// Rows separated by 1dp dashed --hairline divider (last row has none).
// Icon box: 22×22dp, GreenBgLight bg, 6dp corners — contains unique inline Canvas icon.
// N/P/K rows use a bold letter text instead of a canvas icon.
// chip-norm → GreenBgLight bg, GreenLight border, GreenDark text
// chip-low  → InfoBg bg, InfoBorder border, Info text
// chip-high → DangerBg bg, DangerBorder border, Danger text

@Composable
fun SensorReadingsCard(sensors: List<SensorRowData>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SENSOR READINGS · AT ANALYSIS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Black,
                letterSpacing = 0.4.sp
            )
            Text(
                text = "${sensors.size} sensors",
                fontSize = 10.sp,
                color = GrayMedium
            )
        }
        HorizontalDivider(color = Border)

        Column(
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 8.dp)
        ) {
            sensors.forEachIndexed { index, sensor ->
                SensorTableRow(sensor = sensor)
                if (index < sensors.lastIndex) {
                    DashedDivider()
                }
            }
        }
    }
}

@Composable
private fun DashedDivider() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
    ) {
        val dashWidth = 6.dp.toPx()
        val gapWidth = 4.dp.toPx()
        val y = size.height / 2f
        var x = 0f
        while (x < size.width) {
            drawLine(color = Border, start = Offset(x, y), end = Offset((x + dashWidth).coerceAtMost(size.width), y), strokeWidth = 1.dp.toPx())
            x += dashWidth + gapWidth
        }
    }
}

@Composable
fun SensorTableRow(sensor: SensorRowData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SensorIconBox(iconType = sensor.iconType)
            Text(text = sensor.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Black)
        }
        Text(text = sensor.value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GrayBody)
        SensorChip(level = sensor.level)
    }
}

@Composable
fun SensorIconBox(iconType: SensorIconType) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(GreenBgLight, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        when (iconType) {
            // N / P / K — bold text label, matches HTML `font-size:9px;font-weight:600;color:#3B6D11`
            SensorIconType.LETTER_N,
            SensorIconType.LETTER_P,
            SensorIconType.LETTER_K -> {
                val letter = when (iconType) {
                    SensorIconType.LETTER_N -> "N"
                    SensorIconType.LETTER_P -> "P"
                    else                    -> "K"
                }
                Text(
                    text = letter,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }

            // All other icons drawn on an 11×11dp Canvas to match the HTML SVG viewBox="0 0 11 11"
            else -> {
                Canvas(modifier = Modifier.size(11.dp)) {
                    val w  = size.width
                    val h  = size.height
                    val ic = GreenPrimary

                    when (iconType) {

                        // Soil Temp — line graph: M2 8 L5 5 L7 7 L9 4
                        SensorIconType.SOIL_TEMP -> {
                            val path = Path().apply {
                                moveTo(w * 2 / 11f, h * 8 / 11f)
                                lineTo(w * 5 / 11f, h * 5 / 11f)
                                lineTo(w * 7 / 11f, h * 7 / 11f)
                                lineTo(w * 9 / 11f, h * 4 / 11f)
                            }
                            drawPath(
                                path  = path,
                                color = ic,
                                style = Stroke(
                                    width = 1.4.dp.toPx(),
                                    cap   = StrokeCap.Round,
                                    join  = StrokeJoin.Round
                                )
                            )
                        }

                        // Soil pH — circle r=3.5 at centre + crosshair lines
                        SensorIconType.SOIL_PH -> {
                            drawCircle(
                                color  = ic,
                                radius = h * 3.5f / 11f,
                                center = Offset(w / 2f, h / 2f),
                                style  = Stroke(width = 1.2.dp.toPx())
                            )
                            // horizontal M2.5 5.5 h6
                            drawLine(ic, Offset(w * 2.5f / 11f, h / 2f), Offset(w * 8.5f / 11f, h / 2f), 1.1.dp.toPx())
                            // vertical   M5.5 2.5 v6
                            drawLine(ic, Offset(w / 2f, h * 2.5f / 11f), Offset(w / 2f, h * 8.5f / 11f), 1.1.dp.toPx())
                        }

                        // Soil EC — 3 horizontal lines at y=3, 5.5, 8
                        SensorIconType.SOIL_EC -> {
                            val sw = 1.2.dp.toPx()
                            val cap = StrokeCap.Round
                            drawLine(ic, Offset(w * 2 / 11f, h * 3 / 11f),   Offset(w * 9 / 11f, h * 3 / 11f),   sw, cap)
                            drawLine(ic, Offset(w * 2 / 11f, h * 5.5f / 11f), Offset(w * 9 / 11f, h * 5.5f / 11f), sw, cap)
                            drawLine(ic, Offset(w * 2 / 11f, h * 8 / 11f),   Offset(w * 9 / 11f, h * 8 / 11f),   sw, cap)
                        }

                        // Soil Moisture — filled water-drop teardrop
                        // M5.5 1.5 C5.5 1.5 9 4.5 9 7 C9 8.66 7.43 10 5.5 10 S2 8.66 2 7 C2 4.5 5.5 1.5 5.5 1.5Z
                        SensorIconType.SOIL_MOISTURE -> {
                            val drop = Path().apply {
                                val cx = w / 2f
                                moveTo(cx, h * 1.5f / 11f)
                                cubicTo(cx, h * 1.5f / 11f,  w * 9 / 11f, h * 4.5f / 11f,  w * 9 / 11f, h * 7 / 11f)
                                cubicTo(w * 9 / 11f, h * 8.66f / 11f,  w * 7.43f / 11f, h * 10 / 11f,  cx, h * 10 / 11f)
                                cubicTo(w * 2 / 11f, h * 8.66f / 11f,  w * 2 / 11f, h * 7 / 11f,  w * 2 / 11f, h * 7 / 11f)
                                cubicTo(w * 2 / 11f, h * 4.5f / 11f,  cx, h * 1.5f / 11f,  cx, h * 1.5f / 11f)
                                close()
                            }
                            drawPath(drop, color = ic)
                        }

                        // Light — filled circle r=2 + 4 ray lines (N, S, W, E)
                        SensorIconType.LIGHT -> {
                            drawCircle(ic, radius = h * 2 / 11f, center = Offset(w / 2f, h / 2f))
                            val sw  = 1.dp.toPx()
                            val cap = StrokeCap.Round
                            // top
                            drawLine(ic, Offset(w / 2f, h * 1 / 11f),   Offset(w / 2f, h * 2.4f / 11f), sw, cap)
                            // bottom
                            drawLine(ic, Offset(w / 2f, h * 8.6f / 11f), Offset(w / 2f, h * 10 / 11f),  sw, cap)
                            // left
                            drawLine(ic, Offset(w * 1 / 11f, h / 2f),   Offset(w * 2.4f / 11f, h / 2f), sw, cap)
                            // right
                            drawLine(ic, Offset(w * 8.6f / 11f, h / 2f), Offset(w * 10 / 11f, h / 2f),  sw, cap)
                        }

                        // Air Temp — vertical stem + filled bulb at bottom
                        // M5.5 1.5 v6.2   circle cx=5.5 cy=9 r=1.5
                        SensorIconType.AIR_TEMP -> {
                            drawLine(
                                color       = ic,
                                start       = Offset(w / 2f, h * 1.5f / 11f),
                                end         = Offset(w / 2f, h * 7.7f / 11f),
                                strokeWidth = 1.4.dp.toPx(),
                                cap         = StrokeCap.Round
                            )
                            drawCircle(ic, radius = h * 1.5f / 11f, center = Offset(w / 2f, h * 9 / 11f))
                        }

                    }
                }
            }
        }
    }
}

// ─── Sensor chip ──────────────────────────────────────────────────────────────
// chip-norm → GreenBgLight / GreenLight / GreenDark  "Normal"
// chip-low  → InfoBg / InfoBorder / Info             "Low"
// chip-high → DangerBg / DangerBorder / Danger       "High"
@Composable
fun SensorChip(level: ThresholdLevel) {
    val (bg, bd, tx, label) = when (level) {
        ThresholdLevel.NORMAL -> arrayOf(GreenBgLight, GreenLight, GreenDark, "Normal")
        ThresholdLevel.LOW -> arrayOf(InfoBg, InfoBorder, Info, "Low")
        ThresholdLevel.HIGH -> arrayOf(DangerBg, DangerBorder, Danger, "High")
        ThresholdLevel.UNKNOWN -> arrayOf(OffWhite, Border, GrayBody, "Unknown")
    }
    Box(
        modifier = Modifier
            .background(bg as Color, RoundedCornerShape(5.dp))
            .border(1.dp, bd as Color, RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(text = label as String, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = tx as Color, letterSpacing = 0.4.sp)
    }
}

@Composable
fun InfoBannerCard() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OffWhite, RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            Icons.Rounded.Info,
            contentDescription = null,
            tint = GrayMedium,
            modifier = Modifier
                .size(14.dp)
                .padding(top = 1.dp)
        )
        Text(
            "No sensor data available — showing possible causes based on visual symptom only.",
            fontSize = 11.sp,
            color = GrayBody,
            lineHeight = 16.sp
        )
    }
}

@Composable
fun PredictionResultCard(
    predictedClass: String,
    confidence: Float,
    pct: Int
) {
    val isHigh = pct >= 80
    val isModerate = pct in 50..79

    val badgeBg = when {
        isHigh -> GreenBgLight
        isModerate -> WarningBg
        else -> DangerBg
    }
    val badgeBd = when {
        isHigh -> GreenLight
        isModerate -> WarningBorder
        else -> DangerBorder
    }
    val badgeTx = when {
        isHigh -> GreenDark
        isModerate -> Warning
        else -> Danger
    }
    val badgeLabel = when {
        isHigh -> "High confidence"
        isModerate -> "Moderate"
        else -> "Low confidence"
    }
    val barColor = when {
        isHigh -> GreenPrimary
        isModerate -> Warning
        else -> Danger
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // "Siling Labuyo leaf detected" label
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, null, tint = White, modifier = Modifier.size(7.dp))
            }
            Text(
                "Siling Labuyo leaf detected",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = GreenDark,
                letterSpacing = 0.5.sp
            )
        }

        // Symptom name - Large
        Text(
            text = predictedClass,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = Fraunces,
            color = Black,
            letterSpacing = (-0.3f).sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
        )

        // Confidence row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Confidence",
                fontSize = 11.sp,
                color = GrayMedium,
                letterSpacing = 0.4.sp
            )
            Text(
                "$pct%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Black
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(OffWhite)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(confidence)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(barColor.copy(alpha = 0.7f), barColor)
                        ),
                        RoundedCornerShape(4.dp)
                    )
            )
        }

        // Confidence badge
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .background(badgeBg, RoundedCornerShape(6.dp))
                .border(1.dp, badgeBd, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                badgeLabel,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = badgeTx,
                letterSpacing = 0.4.sp
            )
        }

        // Moderate confidence warning
        if (isModerate) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .background(WarningBg, RoundedCornerShape(10.dp))
                    .border(1.dp, WarningBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    Icons.Rounded.Info,
                    null,
                    tint = Warning,
                    modifier = Modifier
                        .size(14.dp)
                        .padding(top = 1.dp)
                )
                Text(
                    "Consider retaking with better lighting and a clearer view of the leaf for higher confidence.",
                    fontSize = 11.sp,
                    color = Warning,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun PlantConditionCard(
    condition: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Header — green bg, uppercase label
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenBgLight)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                text = "PLANT CONDITION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GreenDark,
                letterSpacing = 0.4.sp
            )
        }
        HorizontalDivider(color = GreenLight)

        // Body — condition name + description
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Text(
                text = condition,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = Black,
                letterSpacing = (-0.2f).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF5F5E5A), // --body var from design
                lineHeight = 16.5.sp       // ~1.5 line-height
            )
        }
    }
}

@Composable
fun CareRecommendationsCard(items: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenBgLight)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                "CARE RECOMMENDATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GreenDark,
                letterSpacing = 0.4.sp
            )
        }
        HorizontalDivider(color = GreenLight)

        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEachIndexed { i, rec ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(GreenBgLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${i + 1}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = GreenDark
                        )
                    }
                    Text(
                        rec,
                        fontSize = 11.sp,
                        color = GrayBody,
                        lineHeight = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun PossibleConditionsCard(causes: List<String>?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OffWhite)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                "POSSIBLE CONDITIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GrayBody,
                letterSpacing = 0.4.sp
            )
        }
        HorizontalDivider(color = Border)

        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            causes?.forEach { ResultBullet(it) } ?: Text(
                "No causes available",
                fontSize = 11.sp,
                color = GrayMedium
            )
        }
    }
}

@Composable
fun CharacteristicsDescriptionCard(prediction: String, characteristics: List<String>) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                "CHARACTERISTICS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Black,
                letterSpacing = 0.4.sp
            )
        }
        HorizontalDivider(color = Border)

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            // Prediction tag pill
            Box(
                modifier = Modifier
                    .background(GreenBgLight, RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    prediction,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = GreenDark
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Bullet list
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                characteristics.forEach { item ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Green dot bullet
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(5.dp)
                                .background(GreenMid, CircleShape) // use your green-400 token
                        )
                        Text(
                            text = item,
                            fontSize = 11.sp,
                            color = GrayBody,
                            lineHeight = 16.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LockedRecommendationsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OffWhite)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                "CARE RECOMMENDATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GrayBody,
                letterSpacing = 0.4.sp
            )
        }
        HorizontalDivider(color = Border)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Rounded.Info,
                null,
                tint = GrayMedium,
                modifier = Modifier.size(32.dp)
            )
            Text(
                "Connect an IoT device for tailored advice",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GrayBody,
                textAlign = TextAlign.Center
            )
            Text(
                "Specific recommendations require sensor data readings.",
                fontSize = 11.sp,
                color = GrayMedium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
fun OutOfScopeContent(
    confidencePct: Int,
    bitmap: android.graphics.Bitmap?,
    heatmapOverlay: android.graphics.Bitmap?,
    padding: PaddingValues
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LeafImageCard(bitmap = bitmap, heatmapOverlay = heatmapOverlay)

        // Rejection hero
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(DangerBg, White)),
                    RoundedCornerShape(14.dp)
                )
                .border(1.dp, DangerBorder, RoundedCornerShape(14.dp))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(White)
                    .border(1.5.dp, DangerBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close,
                    null,
                    tint = Danger,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                "We can't analyze this image",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = Danger,
                textAlign = TextAlign.Center,
                letterSpacing = (-0.2f).sp
            )
            Text(
                "The image doesn't appear to show a Siling Labuyo leaf, or the symptom isn't one of the four we currently support.",
                fontSize = 11.5.sp,
                color = GrayBody,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
//            // Confidence pill
//            Row(
//                modifier = Modifier
//                    .padding(top = 4.dp)
//                    .background(White, CircleShape)
//                    .border(1.dp, DangerBorder, CircleShape)
//                    .padding(horizontal = 9.dp, vertical = 3.dp),
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(5.dp)
//            ) {
//                Box(
//                    modifier = Modifier
//                        .size(6.dp)
//                        .clip(CircleShape)
//                        .background(Danger)
//                )
//                Text(
//                    "Confidence $confidencePct%",
//                    fontSize = 10.sp,
//                    fontWeight = FontWeight.Medium,
//                    color = Danger,
//                    letterSpacing = 0.4.sp
//                )
//            }
        }

        // Why we couldn't classify
        SectionCard(
            headerText = "WHY WE COULDN'T CLASSIFY",
            headerBg = OffWhite,
            headerTextColor = GrayBody
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ResultBullet("The leaf detected isn't recognized as Siling Labuyo (Capsicum frutescens)")
                ResultBullet("Or the visible symptom is outside our current four-class scope")
                ResultBullet("Or image quality is too low for confident prediction (< 50%)")
            }
        }

        // Supported symptoms grid
        SupportedSymptomsCard()

        // Tips
        SectionCard(
            headerText = "TIPS FOR A BETTER PHOTO",
            headerBg = White,
            headerTextColor = Black
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ResultBullet("Frame a single Siling Labuyo leaf inside the bounding box")
                ResultBullet("Use natural daylight — avoid heavy shadows or direct flash")
                ResultBullet("Hold the camera steady, ~15–20 cm from the leaf")
                ResultBullet("Ensure the symptom is clearly visible before capturing")
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SupportedSymptomsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenBgLight)
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SUPPORTED SYMPTOMS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GreenDark,
                letterSpacing = 0.4.sp
            )
            Text("4 classes", fontSize = 10.sp, color = GreenDark)
        }
        HorizontalDivider(color = GreenLight)

        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScopeTile(
                    modifier = Modifier.weight(1f),
                    title = "Healthy",
                    tag = "OK",
                    desc = "Vibrant green, intact, no spotting or curling"
                )
                ScopeTile(
                    modifier = Modifier.weight(1f),
                    title = "Curling",
                    tag = "!",
                    desc = "Edges roll inward or downward"
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScopeTile(
                    modifier = Modifier.weight(1f),
                    title = "Spots",
                    tag = "!",
                    desc = "Brown/black blemishes on leaf surface"
                )
                ScopeTile(
                    modifier = Modifier.weight(1f),
                    title = "Yellowing",
                    tag = "!",
                    desc = "Loss of green pigment, pale or yellow"
                )
            }
        }
    }
}

@Composable
fun ScopeTile(
    modifier: Modifier,
    title: String,
    tag: String,
    desc: String
) {
    Column(
        modifier = modifier
            .background(White, RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Placeholder for leaf image (using emoji)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(LeafBgDark),
            contentAlignment = Alignment.Center
        ) {
            Text("🌿", fontSize = 28.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Black
            )
            Box(
                modifier = Modifier
                    .background(GreenBgLight, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = GreenDark
                )
            }
        }
        Text(
            desc,
            fontSize = 10.sp,
            color = GrayMedium,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun ResultBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(5.dp)
                .clip(CircleShape)
                .background(GreenPrimary)
        )
        Text(
            text,
            fontSize = 11.sp,
            color = GrayBody,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun SectionCard(
    headerText: String,
    headerBg: Color,
    headerTextColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                headerText,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = headerTextColor,
                letterSpacing = 0.4.sp
            )
        }
        HorizontalDivider(color = Border)
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            content()
        }
    }
}