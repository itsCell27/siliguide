package com.example.siliguide.features.dashboard

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.R
import com.example.siliguide.core.ml.LabuyoLeafChecker
import com.example.siliguide.core.ml.LeafClassifier
import com.example.siliguide.core.ml.Prediction
import com.example.siliguide.core.ui.theme.DarkGreen
import com.example.siliguide.core.ui.theme.LightGreenBg
import com.example.siliguide.core.ui.theme.LightWhiteBg
import com.example.siliguide.features.analysis.AnalysisResultScreen
import com.example.siliguide.features.camera.CameraScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun DashboardScreen() {
    var showCamera by remember { mutableStateOf(false) }
    var analyzedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var prediction by remember { mutableStateOf<Prediction?>(null) }
    var analysisError by remember { mutableStateOf<String?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (analyzedBitmap != null) {
        // Only show result screen when analysis is DONE
        AnalysisResultScreen(
            bitmap = analyzedBitmap!!,
            prediction = prediction,
            error = analysisError,
            onBack = {
                analyzedBitmap = null
                prediction = null
                analysisError = null
            }
        )
    } else if (showCamera) {
        CameraScreen(
            onImageCaptured = { bitmap ->
                showCamera = false
                isAnalyzing = true
                
                scope.launch {
                    try {
                        val result = withContext(Dispatchers.Default) {
                            var finalResult: Prediction? = null
                            
                            // 1. Run Gatekeeper
                            val leafChecker = LabuyoLeafChecker(context)
                            val isLabuyo = try {
                                leafChecker.isLeaf(bitmap)
                            } finally {
                                leafChecker.close()
                            }

                            if (isLabuyo) {
                                // 2. Run Classifier
                                val classifier = LeafClassifier(context)
                                finalResult = try {
                                    classifier.predict(bitmap)
                                } finally {
                                    classifier.close()
                                }
                            }
                            finalResult
                        }
                        
                        // Set results and transition UI together
                        if (result == null) {
                            analysisError = "Please upload a siling labuyo leaf."
                        } else {
                            prediction = result
                        }
                        analyzedBitmap = bitmap
                    } catch (e: Exception) {
                        Log.e("Analysis", "Error during processing", e)
                        analysisError = "Analysis failed. Please try again."
                        analyzedBitmap = bitmap // Show result screen even on error to display message
                    } finally {
                        isAnalyzing = false
                    }
                }
            },
            onBack = {
                showCamera = false
            }
        )
    } else {
        Scaffold(
            bottomBar = {
                DashboardBottomBar(onCameraClick = { showCamera = true })
            },
            containerColor = LightWhiteBg
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "SiliGuide",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkGreen,
                        modifier = Modifier.padding(24.dp)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(LightGreenBg)
                    )
                }
                
                if (isAnalyzing) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Analyzing Leaf...",
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardBottomBar(onCameraClick: () -> Unit) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 32.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                painter = painterResource(id = R.drawable.house),
                label = "Home",
                isSelected = true
            )

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkGreen)
                    .clickable { onCameraClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.camera),
                    contentDescription = "Scan",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            BottomNavItem(
                painter = painterResource(id = R.drawable.user),
                label = "Profile",
                isSelected = false
            )
        }
    }
}

@Composable
fun BottomNavItem(painter: Painter, label: String, isSelected: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            tint = if (isSelected) DarkGreen else DarkGreen.copy(alpha = 0.6f),
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) DarkGreen else DarkGreen.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
