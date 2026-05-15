package com.example.siliguide.presentation.features.analysis

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.R
import com.example.siliguide.core.ml.Prediction
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.domain.engine.RuleResult

// ─── Threshold level for sensor chip display ───────────────────────────────
enum class SensorLevel { LOW, NORMAL, HIGH }

data class SensorRowData(
    val name: String,
    val value: String,
    val level: SensorLevel
)

// ─── Main Screen ────────────────────────────────────────────────────────────
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

    // Determine which state we are in
    val confidence = prediction?.confidence ?: 0f
    val confidencePct = (confidence * 100).toInt()
    val isOutOfScope = !isLoading && (result == null || confidencePct < 50)

    Scaffold(
        topBar = {
            ResultTopBar(
                isOutOfScope = isOutOfScope,
                plantLabel = "Labuyo Plant · Just now",
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
                    padding = padding
                )
            }

            else -> {
                ResultContent(
                    result = result!!,
                    confidence = confidence,
                    predictedClass = prediction?.label ?: "Unknown",
                    bitmap = bitmapState.value,
                    padding = padding
                )
            }
        }
    }
}

// ─── Top Bar ────────────────────────────────────────────────────────────────
@Composable
fun ResultTopBar(
    isOutOfScope: Boolean,
    plantLabel: String,
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
                    text = if (isOutOfScope) "Image rejected · just now" else plantLabel,
                    fontSize = 11.sp,
                    color = GrayMedium,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        HorizontalDivider(color = Border)
    }
}

// ─── Bottom Bar ─────────────────────────────────────────────────────────────

@Composable
fun ResultBottomBar(
    isOutOfScope: Boolean,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(White)
            .border(topBorder = true)
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
            .padding(top = 10.dp, bottom = 20.dp),
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

// Helper extension — adds a top hairline border to a Column/Row
private fun Modifier.border(topBorder: Boolean): Modifier = if (topBorder)
    this.border(1.dp, Border, RoundedCornerShape(0.dp)) else this

// ─── 10A / 10B Content ──────────────────────────────────────────────────────
@Composable
fun ResultContent(
    result: RuleResult,
    confidence: Float,
    predictedClass: String,
    bitmap: android.graphics.Bitmap?,
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
        // Gray info banner — only for partial (no sensors)
        if (!result.isUsingSensors) {
            InfoBannerCard()
        }

        // 1:1 Leaf image
        LeafImageCard(bitmap = bitmap)

        // Prediction card
        PredictionResultCard(
            predictedClass = predictedClass,
            confidence = confidence,
            pct = pct
        )

        // Plant condition card
        if (result.isUsingSensors) {
            PlantConditionCard(
                result = result,
                confidencePct = pct,
                symptom = result.symptoms.firstOrNull() ?: "Unknown"
            )
        } else {
            PossibleConditionsCard(causes = result.possibleCauses)
            SymptomDescriptionCard(symptom = result.symptoms.firstOrNull() ?: "Unknown")
        }

        // Sensor readings — only for full analysis
        if (result.isUsingSensors) {
            // TODO: wire real sensor data from ViewModel when available
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

// ─── Leaf image ─────────────────────────────────────────────────────────────
@Composable
fun LeafImageCard(bitmap: android.graphics.Bitmap?) {
    if (bitmap == null) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .background(Color(0xFF1F3909))
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Cropped leaf",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Bottom label
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text("📷 Cropped sample · 1:1", fontSize = 10.sp, color = White)
        }
    }
}

// ─── Info banner ────────────────────────────────────────────────────────────
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

// ─── Prediction card ────────────────────────────────────────────────────────
@Composable
fun PredictionResultCard(
    predictedClass: String,
    confidence: Float,
    pct: Int
) {
    val isHigh = pct >= 80
    val isModerate = pct in 50..79

    // Badge colors
    val badgeBg = when {
        isHigh -> GreenBgLight; isModerate -> WarningBg; else -> DangerBg
    }
    val badgeBd = when {
        isHigh -> GreenLight; isModerate -> WarningBorder; else -> DangerBorder
    }
    val badgeTx = when {
        isHigh -> GreenDark; isModerate -> Warning; else -> Danger
    }
    val badgeLabel = when {
        isHigh -> "High confidence"; isModerate -> "Moderate"; else -> "Low confidence"
    }
    val barColor = when {
        isHigh -> GreenPrimary; isModerate -> Warning; else -> Danger
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

        // Symptom name
        Text(
            text = predictedClass,   // "Healthy" / "Curling" / "Spots" / "Yellowing"
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
            Text("Confidence", fontSize = 11.sp, color = GrayMedium, letterSpacing = 0.4.sp)
            Text("$pct%", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Black)
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

        // Warn box — moderate confidence only
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

// ─── Plant Condition card (Full analysis) ───────────────────────────────────
@Composable
fun PlantConditionCard(
    result: RuleResult,
    symptom: String,
    confidencePct: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenBgLight, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
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

        // Body — condition name only
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)
        ) {
            Text(
                text = result.condition ?: "Unknown",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = Black,
                letterSpacing = (-0.2f).sp
            )
        }
    }
}

// ─── Sensor Readings card ───────────────────────────────────────────────────
@Composable
fun SensorReadingsCard(sensors: List<SensorRowData>) {
    if (sensors.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
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

        // Sensor rows
        Column(modifier = Modifier.padding(horizontal = 14.dp)) {
            sensors.forEachIndexed { index, sensor ->
                SensorTableRow(sensor = sensor)
                if (index < sensors.lastIndex) {
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
            Text(sensor.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Black)
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

// ─── Care Recommendations card ──────────────────────────────────────────────
@Composable
fun CareRecommendationsCard(items: List<String>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenBgLight, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
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
                    // Number circle
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

// ─── Possible Conditions card (Partial) ─────────────────────────────────────
@Composable
fun PossibleConditionsCard(causes: List<String>?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OffWhite, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
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

// ─── Symptom description card (Partial) ─────────────────────────────────────
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
                Text(symptom, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = GreenDark)
            }
            Spacer(modifier = Modifier.height(7.dp))
            Text(description, fontSize = 11.sp, color = GrayBody, lineHeight = 16.sp)
        }
    }
}

// ─── Locked Recommendations card (Partial) ──────────────────────────────────
@Composable
fun LockedRecommendationsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OffWhite, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
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
            Icon(Icons.Rounded.Info, null, tint = GrayMedium, modifier = Modifier.size(32.dp))
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

// ─── 10C Out-of-scope content ───────────────────────────────────────────────
@Composable
fun OutOfScopeContent(
    confidencePct: Int,
    bitmap: android.graphics.Bitmap?,
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
        // Smaller 1:1 image
        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .background(Color(0xFF1F3909))
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Cropped sample",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text("📷 Cropped sample · 1:1", fontSize = 10.sp, color = White)
                }
            }
        }

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
                Icon(Icons.Rounded.Close, null, tint = Danger, modifier = Modifier.size(26.dp))
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
                Box(modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Danger))
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

        // Supported symptoms 2x2 grid
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

// ─── Supported symptoms 2x2 grid ────────────────────────────────────────────
@Composable
fun SupportedSymptomsCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GreenBgLight, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
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
                ScopeTileCompose(
                    modifier = Modifier.weight(1f),
                    title = "Healthy",
                    tag = "OK",
                    desc = "Vibrant green, intact, no spotting or curling",
                    imageResId = R.drawable.healthy
                )
                ScopeTileCompose(
                    modifier = Modifier.weight(1f),
                    title = "Curling",
                    tag = "!",
                    desc = "Edges roll inward or downward",
                    imageResId = R.drawable.curling
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScopeTileCompose(
                    modifier = Modifier.weight(1f),
                    title = "Spots",
                    tag = "!",
                    desc = "Brown/black blemishes on leaf surface",
                    imageResId = R.drawable.spots
                )
                ScopeTileCompose(
                    modifier = Modifier.weight(1f),
                    title = "Yellowing",
                    tag = "!",
                    desc = "Loss of green pigment, pale or yellow",
                    imageResId = R.drawable.yellowing
                )
            }
        }
    }
}

@Composable
fun ScopeTileCompose(
    modifier: Modifier,
    title: String,
    tag: String,
    desc: String,
    imageResId: Int
) {
    Column(
        modifier = modifier
            .background(White, RoundedCornerShape(10.dp))
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Black)
            Box(
                modifier = Modifier
                    .background(GreenBgLight, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(tag, fontSize = 9.sp, fontWeight = FontWeight.Medium, color = GreenDark)
            }
        }
        Text(desc, fontSize = 10.sp, color = GrayMedium, lineHeight = 14.sp)
    }
}

// ─── Shared helpers ─────────────────────────────────────────────────────────
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
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBg, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
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