package com.example.siliguide.presentation.features.iot.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.siliguide.core.ui.theme.*

// ML Kit imports
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage


@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(
    onQrScanned: (String) -> Unit,
    onManualEntry: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) onBack()
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (hasCameraPermission) {

        // Prevent multiple scans
        var isScanning by remember { mutableStateOf(true) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {

            AndroidView(
                factory = { ctx ->

                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({

                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        // ML Kit QR Scanner
                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()

                        val scanner = BarcodeScanning.getClient(options)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(
                                ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
                            )
                            .build()
                            .apply {

                                setAnalyzer(
                                    ContextCompat.getMainExecutor(ctx)
                                ) { imageProxy ->

                                    if (!isScanning) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }

                                    val mediaImage = imageProxy.image

                                    if (mediaImage != null) {

                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            imageProxy.imageInfo.rotationDegrees
                                        )

                                        scanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                val value = barcodes.firstOrNull()?.rawValue

                                                if (!value.isNullOrEmpty()) {
                                                    isScanning = false
                                                    onQrScanned(value)
                                                }
                                            }
                                            .addOnFailureListener {
                                                Log.e("QrScanner", "Scan failed", it)
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }

                                    } else {
                                        imageProxy.close()
                                    }
                                }
                            }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("QrScanner", "Camera binding failed", e)
                        }

                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            QrScannerOverlay(
                onBack = onBack,
                onManualEntry = onManualEntry
            )
        }
    }
}

@Composable
fun QrScannerOverlay(
    onBack: () -> Unit,
    onManualEntry: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                "Scan Device QR",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.99f)
            ) {

                val boxSize = 240.dp.toPx()
                val cornerLength = 32.dp.toPx()
                val strokeThickness = 4.dp.toPx()

                val left = (size.width - boxSize) / 2
                val top = (size.height - boxSize) / 2
                val right = left + boxSize
                val bottom = top + boxSize

                drawRect(color = Color.Black.copy(alpha = 0.6f))

                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(boxSize, boxSize),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    blendMode = BlendMode.Clear
                )

                val cornerColor = GreenPrimary

                drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth = strokeThickness)
                drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth = strokeThickness)

                drawLine(cornerColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth = strokeThickness)
                drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth = strokeThickness)

                drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth = strokeThickness)
                drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth = strokeThickness)

                drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth = strokeThickness)
                drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth = strokeThickness)
            }

            Text(
                text = "Point your camera at the QR code located on the back of the SiliGuide sensor.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 300.dp, start = 40.dp, end = 40.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(White)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(Border, CircleShape)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "Having trouble?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = Black
            )

            Text(
                "Ensure the room is well-lit and the sticker is not scratched or damaged.",
                fontSize = 12.sp,
                color = GrayMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            Button(
                onClick = onManualEntry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OffWhite,
                    contentColor = GreenPrimary
                ),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Enter Details Manually",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}