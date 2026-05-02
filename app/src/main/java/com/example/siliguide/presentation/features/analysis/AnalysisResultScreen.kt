package com.example.siliguide.presentation.features.analysis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.theme.DarkGreen
import com.example.siliguide.presentation.features.iot.IoTViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisResultScreen(
    imageUri: String, // Accepts the URI from NavGraph
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val iotViewModel: IoTViewModel = hiltViewModel()
    val analysisViewModel: AnalysisViewModel = hiltViewModel()

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // 1. Fetch sensors immediately
    LaunchedEffect(Unit) {
        iotViewModel.fetchSensors()
    }

    // 2. Load the cropped image from the URI
    LaunchedEffect(imageUri) {
        val uri = imageUri.toUri()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            bitmap = BitmapFactory.decodeStream(stream)
        }
    }

    // 3. Wait for BOTH sensor data and bitmap, then trigger ML Analysis!
    LaunchedEffect(bitmap, iotViewModel.isLoading) {
        if (bitmap != null && !iotViewModel.isLoading && !analysisViewModel.isAnalyzing && analysisViewModel.prediction == null && analysisViewModel.analysisError == null) {
            analysisViewModel.analyzeImage(bitmap!!, iotViewModel.sensorData)
        }
    }

    // We consider the screen "Processing" until we get either a valid prediction OR an error back
    val isProcessing = analysisViewModel.prediction == null && analysisViewModel.analysisError == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis Result") },
                navigationIcon = {
                    // Disable back button while processing to prevent accidental cancels
                    IconButton(onClick = onBack, enabled = !isProcessing) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isProcessing) Color.Gray else DarkGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White, titleContentColor = DarkGreen)
            )
        },
        bottomBar = {
            // Pin the DONE button to the bottom so it never gets cut off
            if (!isProcessing) {
                Surface(color = Color.White) {
                    Button(
                        onClick = onBack,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .navigationBarsPadding(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkGreen),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("DONE", modifier = Modifier.padding(vertical = 8.dp), color = Color.White)
                    }
                }
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

            // Loading Overlay
            if (isProcessing) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = DarkGreen)
                    Spacer(Modifier.height(16.dp))
                    // Dynamic text based on what it's currently waiting for
                    Text(
                        text = if (iotViewModel.isLoading) "Fetching IoT Sensor Data..." else "Analyzing Leaf Data...",
                        color = DarkGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                // Main UI
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()) // Makes the content scrollable!
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // IMAGE
                    if (bitmap != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().height(300.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = "Analyzed Leaf",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // ERROR OR RESULT
                    if (analysisViewModel.analysisError != null) {
                        Text("Verification Failed", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(analysisViewModel.analysisError!!, fontSize = 16.sp, color = Color.Gray)
                    } else if (analysisViewModel.prediction != null) {
                        Text("Siling Labuyo Detected", fontSize = 18.sp, color = DarkGreen, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Condition Box
                        Surface(modifier = Modifier.fillMaxWidth(), color = DarkGreen.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Condition: ${formatLabel(analysisViewModel.prediction!!.label)}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = DarkGreen)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Confidence Level: ${(analysisViewModel.prediction!!.confidence * 100).toInt()}%", fontSize = 16.sp, color = DarkGreen.copy(alpha = 0.8f))
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Rule Engine Box
                        if (analysisViewModel.ruleResult != null) {
                            Surface(modifier = Modifier.fillMaxWidth(), color = DarkGreen.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    val res = analysisViewModel.ruleResult!!
                                    if (res.isUsingSensors) {
                                        Text("Plant Condition", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkGreen)
                                        Text(res.condition ?: "Unknown", fontSize = 16.sp)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Recommendations", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkGreen)
                                        res.recommendations.forEach { Text("• $it") }
                                    } else {
                                        Text("Symptoms", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkGreen)
                                        res.symptoms.forEach { Text("• $it") }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text("Possible Causes", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = DarkGreen)
                                        res.possibleCauses.forEach { Text("• $it") }
                                    }
                                }
                            }
                        }
                    }

                    // Added a bit of extra padding at the bottom so the scroll clears the button nicely
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

private fun formatLabel(label: String): String {
    return label.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}