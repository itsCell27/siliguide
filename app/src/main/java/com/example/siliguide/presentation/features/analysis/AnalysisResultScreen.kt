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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
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
import androidx.compose.ui.draw.blur
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.domain.engine.RuleResult

enum class SensorLevel { LOW, NORMAL, HIGH }

data class SensorRowData(
    val name: String,
    val value: String,
    val level: SensorLevel
)

@Composable
fun AnalysisResultScreen(
    imageUri: String,
    viewModel: AnalysisViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSaveAsNote: () -> Unit
) {
    val result = viewModel.ruleResult
    val prediction = viewModel.prediction
    val isLoading = viewModel.isAnalyzing
    val heatmapOverlay = viewModel.heatmapOverlay // ← NEW
    val context = LocalContext.current
    val bitmapState = remember { mutableStateOf<android.graphics.Bitmap?>(null) }

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
                    heatmapOverlay = heatmapOverlay, // ← NEW
                    padding = padding
                )
            }

            else -> {
                ResultContent(
                    result = result!!,
                    confidence = confidence,
                    predictedClass = prediction?.label ?: "Unknown",
                    bitmap = bitmapState.value,
                    heatmapOverlay = heatmapOverlay, // ← NEW
                    padding = padding
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
        TextButton(
            onClick = onSecondary,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isOutOfScope) "Save as note instead" else "Analyze Another Leaf",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GreenPrimary
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
    heatmapOverlay: android.graphics.Bitmap?, // ← NEW param
    padding: PaddingValues
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
        LeafImageCard(bitmap = bitmap, heatmapOverlay = heatmapOverlay) // ← NEW param

        // Prediction card
        PredictionResultCard(
            predictedClass = predictedClass,
            confidence = confidence,
            pct = pct
        )

        // Plant condition or partial diagnosis
        if (result.isUsingSensors) {
            PlantConditionCard(
                condition = result.condition ?: "Unknown",
                symptom = result.symptoms.firstOrNull() ?: "Unknown"
            )
        } else {
            PossibleConditionsCard(causes = result.possibleCauses)
            SymptomDescriptionCard(symptom = result.symptoms.firstOrNull() ?: "Unknown")
        }

        // Sensor readings - only with full analysis
        if (result.isUsingSensors) {
            SensorReadingsCard(sensors = emptyList())
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

// ─── LeafImageCard with perfectly scaled heatmap overlay ─────────────────

@Composable
fun LeafImageCard(
    bitmap: android.graphics.Bitmap?,
    heatmapOverlay: android.graphics.Bitmap? = null // ← null = no heatmap UI
) {
    if (bitmap == null) return

    var showHeatmap by remember { mutableStateOf(false) }

    // 250 ms fade. Target lowered to 0.55f for standard Python-style CAM transparency
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
        // Layer 1 — The original leaf photo
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Cropped leaf",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Layer 2 — The properly scaled CAM Heatmap
        if (heatmapOverlay != null) {
            Image(
                bitmap = heatmapOverlay.asImageBitmap(),
                contentDescription = "AI Heatmap Overlay",
                // This scales the 224x224 AI map to perfectly match the layout bounds!
                contentScale = ContentScale.Crop,
                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(4.dp)
                    .graphicsLayer(alpha = heatmapAlpha)
            )

            // Layer 3 — Legend top-right, visible only when heatmap is on
            if (showHeatmap) {
                HeatmapLegend(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                )
            }
        }

        // Layer 4 — Eye toggle button bottom-right
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

/**
 * Eye-off / eye-on toggle. Tint shifts to #FF8A4C when active, matching
 * `.img-card.heat-on .heat-btn { color: #FF8A4C }` in the HTML.
 * Drawn inline via Canvas — no drawable resource required.
 */
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

            // Outer almond eye shape
            val eyePath = Path().apply {
                moveTo(0f, h / 2f)
                cubicTo(w * 0.25f, 0f,   w * 0.75f, 0f,   w,       h / 2f)
                cubicTo(w * 0.75f, h,    w * 0.25f, h,    0f,      h / 2f)
                close()
            }
            drawPath(eyePath, color = iconTint, style = stroke)

            // Pupil circle
            drawCircle(color = iconTint, radius = h * 0.17f, center = Offset(w / 2f, h / 2f))

            // Diagonal slash when eye is OFF (heatmap hidden)
            if (!isActive) {
                drawLine(
                    color = iconTint,
                    start = Offset(w * 0.12f, h * 0.12f),
                    end   = Offset(w * 0.88f, h * 0.88f),
                    strokeWidth = strokeWidthPx, // Use the updated stroke width here too
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Redesigned Horizontal Pill Legend.
 * Takes up 50% less space so it doesn't block the leaf!
 */
@Composable
fun HeatmapLegend(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp)) // Smooth rounded edges
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

        // The Gradient Bar (Slightly narrower)
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
    symptom: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenBgLight)
                .padding(horizontal = 14.dp, vertical = 11.dp)
        ) {
            Text(
                "PLANT CONDITION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GreenDark,
                letterSpacing = 0.4.sp
            )
        }
        HorizontalDivider(color = GreenLight)

        // Body
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
        }
    }
}

@Composable
fun SensorReadingsCard(sensors: List<SensorRowData>) {
    if (sensors.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "SENSOR READINGS · AT ANALYSIS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Black,
                letterSpacing = 0.4.sp
            )
            Text("9 sensors", fontSize = 10.sp, color = GrayMedium)
        }
        HorizontalDivider(color = Border)

        // Sensor rows (dummy data for now)
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            val dummySensors = listOf(
                SensorRowData("Soil Moisture", "65%", SensorLevel.NORMAL),
                SensorRowData("Soil Temp", "22.5°C", SensorLevel.NORMAL),
                SensorRowData("Soil pH", "6.8", SensorLevel.NORMAL),
                SensorRowData("EC", "1.2 mS/cm", SensorLevel.NORMAL),
                SensorRowData("Nitrogen", "45 ppm", SensorLevel.LOW),
                SensorRowData("Phosphorus", "32 ppm", SensorLevel.NORMAL),
                SensorRowData("Potassium", "210 ppm", SensorLevel.NORMAL),
                SensorRowData("Light Intensity", "45000 lux", SensorLevel.HIGH),
                SensorRowData("Air Temp", "28°C", SensorLevel.NORMAL)
            )

            dummySensors.forEachIndexed { index, sensor ->
                SensorTableRow(sensor = sensor)
                if (index < dummySensors.lastIndex) {
                    HorizontalDivider(
                        color = Border.copy(alpha = 0.5f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 0.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SensorTableRow(sensor: SensorRowData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon + name
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(GreenBgLight, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("·", fontSize = 14.sp, color = GreenPrimary)
            }
            Text(
                sensor.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Black
            )
        }

        // Value
        Text(
            sensor.value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = GrayBody,
            modifier = Modifier.padding(end = 10.dp)
        )

        // Chip
        SensorChip(level = sensor.level)
    }
}

@Composable
fun SensorChip(level: SensorLevel) {
    val (bg, bd, tx, label) = when (level) {
        SensorLevel.NORMAL -> arrayOf(GreenBgLight, GreenLight, GreenDark, "Normal")
        SensorLevel.LOW -> arrayOf(InfoBg, InfoBorder, Info, "Low")
        SensorLevel.HIGH -> arrayOf(DangerBg, DangerBorder, Danger, "High")
    }
    Box(
        modifier = Modifier
            .background(bg as Color, RoundedCornerShape(5.dp))
            .border(1.dp, bd as Color, RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    ) {
        Text(
            label as String,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = tx as Color,
            letterSpacing = 0.4.sp
        )
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
fun SymptomDescriptionCard(symptom: String) {
    val description = when (symptom.lowercase()) {
        "yellowing", "yellowing leaves" ->
            "A loss of green pigment (chlorophyll) where leaves turn pale yellow or pale green. Pattern matters: uniform vs. blotchy vs. between-vein yellowing each point to different causes. Sensor data would help narrow the cause to one specific condition."

        "curling", "leaf curling" ->
            "Leaf edges roll inward or downward, reducing surface area to conserve moisture. Can indicate heat, drought, or pest activity. Sensor data helps distinguish between water stress and environmental causes."

        "spots", "leaf spots" ->
            "Brown or black blemishes on the leaf surface, which may indicate fungal infection, bacterial disease, or pest damage. Sensor data is helpful to rule out environmental stress."

        else ->
            "Visual symptom detected. Connect an IoT device to get sensor readings for a more accurate diagnosis."
    }

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
                "SYMPTOM DESCRIPTION",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Black,
                letterSpacing = 0.4.sp
            )
        }
        HorizontalDivider(color = Border)

        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Box(
                modifier = Modifier
                    .background(GreenBgLight, RoundedCornerShape(5.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    symptom,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = GreenDark
                )
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                description,
                fontSize = 11.sp,
                color = GrayBody,
                lineHeight = 16.sp
            )
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
                "Specific recommendations require live moisture, pH, and temperature readings.",
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
    heatmapOverlay: android.graphics.Bitmap?, // ← NEW param
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
        // Image — reuses LeafImageCard; gatekeeper heatmap shown here too
        LeafImageCard(bitmap = bitmap, heatmapOverlay = heatmapOverlay) // ← replaces the old inline Box

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
            // Confidence pill
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .background(White, CircleShape)
                    .border(1.dp, DangerBorder, CircleShape)
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Danger)
                )
                Text(
                    "Confidence $confidencePct%",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Danger,
                    letterSpacing = 0.4.sp
                )
            }
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