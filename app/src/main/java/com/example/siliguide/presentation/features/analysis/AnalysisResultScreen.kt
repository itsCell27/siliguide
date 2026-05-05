package com.example.siliguide.presentation.features.analysis

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.core.net.toUri
import com.example.siliguide.domain.engine.RuleResult
import com.example.siliguide.R

// Import your custom theme colors!
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.Border

@OptIn(ExperimentalMaterial3Api::class)
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
                val bitmap = BitmapFactory.decodeStream(stream)
                stream?.close()

                if (bitmap != null) {
                    bitmapState.value = bitmap
                    viewModel.analyzeImage(bitmap, null)
                }
            } catch (_: Exception) {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OffWhite // Themed
                )
            )
        },
        bottomBar = {
            if (!isLoading && result != null) {
                BottomActions(
                    onBack = onNavigateBack,
                    onSave = onSaveAsNote
                )
            }
        },
        containerColor = OffWhite // Themed
    ) { padding ->

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = GreenPrimary) // Themed
                }
            }

            result == null -> {
                OutOfScopeUI(
                    confidence = viewModel.prediction?.confidence,
                    bitmap = bitmapState.value,
                    padding = padding
                )
            }

            else -> {
                MainResultUI(result, prediction?.confidence, bitmapState.value, padding)
            }
        }
    }
}

@Composable
fun MainResultUI(result: RuleResult, confidence: Float?, bitmap: android.graphics.Bitmap?, padding: PaddingValues) {

    Column(
        modifier = Modifier
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(14.dp))
            )
        }

        if (!result.isUsingSensors) {
            InfoBanner()
        }

        PredictionCard(result, confidence)

        ConditionCard(result)

        if (result.recommendations.isNotEmpty()) {
            RecommendationCard(result.recommendations)
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
fun PredictionCard(result: RuleResult, confidence: Float?) {

    val prediction = result.symptoms.firstOrNull() ?: "Unknown"
    // Use the real confidence, or default to 0f if it's null for some reason
    val actualConfidence = confidence ?: 0f
    val percent = (actualConfidence * 100).toInt()

    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = White)) {
        Column(Modifier.padding(16.dp)) {

            Text(
                "Siling Labuyo leaf detected",
                style = MaterialTheme.typography.labelSmall,
                color = GreenDark // Themed
            )

            Text(
                prediction,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MaterialTheme.typography.titleMedium.fontFamily
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Confidence", style = MaterialTheme.typography.labelSmall, color = GrayMedium) // Themed
                Text("$percent%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { actualConfidence },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = GreenPrimary, // Themed
                trackColor = Border // Themed
            )
        }
    }
}

@Composable
fun ConditionCard(result: RuleResult) {

    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = White)) {
        Column {

            Box(
                Modifier
                    .fillMaxWidth()
                    .background(GreenBgLight) // Themed
                    .padding(12.dp)
            ) {
                Text("PLANT CONDITION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }

            Column(Modifier.padding(16.dp)) {

                Text(
                    result.condition ?: "Unknown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                result.possibleCauses.forEach {
                    Row {
                        Text("• ", style = MaterialTheme.typography.bodyMedium)
                        Text(it, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoBanner() {
    Card(
        colors = CardDefaults.cardColors(containerColor = OffWhite), // Themed
        shape = RoundedCornerShape(10.dp)
    ) {
        Text(
            "No sensor data available — showing possible causes based on visual symptom only.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
fun RecommendationCard(items: List<String>) {

    Card(
        colors = CardDefaults.cardColors(containerColor = GreenBgLight), // Themed
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            Text("CARE RECOMMENDATIONS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = GreenDark)

            Spacer(modifier = Modifier.height(8.dp))

            items.forEachIndexed { i, rec ->
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text("${i + 1}. ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(rec, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun OutOfScopeUI(
    confidence: Float?,
    bitmap: android.graphics.Bitmap?,
    padding: PaddingValues
) {
    val errorRed = Color(0xFF791F1F)
    val errorBg = Color(0xFFFCEBEB)
    val percent = confidence?.let { (it * 100).toInt() } ?: 0

    Column(
        Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {

        // 1. The 1:1 Cropped Image
        bitmap?.let {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Cropped sample",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth() // Stretches to fit the screen width (minus padding)
                        .aspectRatio(1f) // Locks the height to perfectly match the width
                        .clip(RoundedCornerShape(14.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("📷 Cropped sample · 1:1", style = MaterialTheme.typography.labelSmall, color = GrayMedium)
            }
        }

        // 2. Hero Rejection & Confidence Pill
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(errorBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Warning, null, tint = errorRed, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text("We can't analyze this image", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = errorRed)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "The image doesn't appear to show a Siling Labuyo leaf, or the symptom isn't one of the four we currently support.",
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))

            // The Confidence Pill
//            Surface(
//                shape = CircleShape,
//                border = androidx.compose.foundation.BorderStroke(1.dp, errorRed),
//                color = Color.Transparent
//            ) {
//                Text(
//                    "Confidence $percent%",
//                    color = errorRed,
//                    style = MaterialTheme.typography.labelSmall,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
//                )
//            }
        }

        // 3. Why we couldn't classify Card
        Card(colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Why we couldn't classify", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                BulletPoint("The leaf detected isn't recognized as Siling Labuyo")
                BulletPoint("Or the visible symptom is outside our current four-class scope")
                BulletPoint("Or image quality is too low for confident prediction (< 50%)")
            }
        }

        // 4. Supported Symptoms Grid
        SupportedSymptomsGrid()

        // 5. Tips for a better photo Card
        Card(colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Tips for a better photo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                BulletPoint("Frame a single Siling Labuyo leaf inside the bounding box")
                BulletPoint("Use natural daylight — avoid heavy shadows or direct flash")
                BulletPoint("Hold the camera steady from the leaf")
                //BulletPoint("If the symptom isn't curling, spots, or yellowing, save a note instead")
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Scroll clearance for bottom buttons
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(Modifier.padding(vertical = 4.dp)) {
        Text("• ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

// Recreates the HTML .scope-grid exactly
@Composable
fun SupportedSymptomsGrid() {
    Card(colors = CardDefaults.cardColors(containerColor = White), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Supported symptoms", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("4 classes", style = MaterialTheme.typography.labelSmall, color = GreenDark)
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 2x2 Grid
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScopeTile(modifier = Modifier.weight(1f), title = "Healthy", tag = "OK", desc = "Vibrant green, intact, no spotting", imageResId = R.drawable.healthy)
                ScopeTile(modifier = Modifier.weight(1f), title = "Curling", tag = "!", desc = "Edges roll inward or downward", imageResId = R.drawable.curling)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScopeTile(modifier = Modifier.weight(1f), title = "Spots", tag = "!", desc = "Brown/black blemishes on surface", imageResId = R.drawable.spots)
                ScopeTile(modifier = Modifier.weight(1f), title = "Yellowing", tag = "!", desc = "Loss of green pigment, pale/yellow", imageResId = R.drawable.yellowing)
            }
        }
    }
}

@Composable
fun ScopeTile(modifier: Modifier, title: String, tag: String, desc: String, imageResId: Int) {
    Column(modifier = modifier) {
        Image(
            painter = painterResource(id = imageResId),
            contentDescription = title,
            contentScale = ContentScale.Crop, // This ensures the image fills the 1:1 square perfectly without stretching!
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))
            Surface(color = OffWhite, shape = RoundedCornerShape(4.dp)) {
                Text(tag, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(desc, style = MaterialTheme.typography.labelSmall, color = GrayMedium, lineHeight = 14.sp)
    }
}

@Composable
fun BottomActions(onBack: () -> Unit, onSave: () -> Unit) {
    Column(
        Modifier
            .background(OffWhite) // Themed
            .navigationBarsPadding() // Your fix from earlier is preserved!
    ) {

        Box(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, OffWhite) // Themed
                    )
                )
        )

        Column(Modifier.padding(16.dp)) {
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary, // Themed
                    contentColor = White // Themed
                )
            ) {
                Text("Save Analysis",
                    style = MaterialTheme.typography.titleMedium,
                    color = White
                )
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Analyze Another Leaf", color = GreenPrimary, style = MaterialTheme.typography.titleMedium) // Themed
            }
        }
    }
}