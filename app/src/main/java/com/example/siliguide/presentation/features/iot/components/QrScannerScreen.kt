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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.siliguide.R
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

                        val preview = androidx.camera.core.Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        // ML Kit QR Scanner
                        val options = BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()

                        val scanner = BarcodeScanning.getClient(options)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .apply {
                                setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
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
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // ── Top Header ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onBack
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.chevron_left),
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                "Scan Device QR",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = DMSans
            )
        }

        // ── Viewport / Reticle (Responsive to available dimensions) ─────────────
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val density = LocalDensity.current
            val maxAvailableW = constraints.maxWidth.toFloat()
            val maxAvailableH = constraints.maxHeight.toFloat()

            // Calculate responsive box dimensions bounded between 180dp and 260dp
            val boxSizePx = minOf(
                maxAvailableW * 0.70f,
                maxAvailableH * 0.52f,
                with(density) { 260.dp.toPx() }
            ).coerceAtLeast(with(density) { 180.dp.toPx() })

            val cornerLength = (boxSizePx * 0.16f).coerceIn(
                with(density) { 32.dp.toPx() },
                with(density) { 48.dp.toPx() }
            )
            val strokeThickness = with(density) { 4.dp.toPx() }

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val left = (size.width - boxSizePx) / 2
                val top = (size.height - boxSizePx) / 2
                val right = left + boxSizePx
                val bottom = top + boxSizePx

                val cornerColor = LightGreen

                // Top-Left Corner
                drawLine(cornerColor, Offset(left, top), Offset(left + cornerLength, top), strokeWidth = strokeThickness, cap = StrokeCap.Round)
                drawLine(cornerColor, Offset(left, top), Offset(left, top + cornerLength), strokeWidth = strokeThickness, cap = StrokeCap.Round)

                // Top-Right Corner
                drawLine(cornerColor, Offset(right, top), Offset(right - cornerLength, top), strokeWidth = strokeThickness, cap = StrokeCap.Round)
                drawLine(cornerColor, Offset(right, top), Offset(right, top + cornerLength), strokeWidth = strokeThickness, cap = StrokeCap.Round)

                // Bottom-Left Corner
                drawLine(cornerColor, Offset(left, bottom), Offset(left + cornerLength, bottom), strokeWidth = strokeThickness, cap = StrokeCap.Round)
                drawLine(cornerColor, Offset(left, bottom), Offset(left, bottom - cornerLength), strokeWidth = strokeThickness, cap = StrokeCap.Round)

                // Bottom-Right Corner
                drawLine(cornerColor, Offset(right, bottom), Offset(right - cornerLength, bottom), strokeWidth = strokeThickness, cap = StrokeCap.Round)
                drawLine(cornerColor, Offset(right, bottom), Offset(right, bottom - cornerLength), strokeWidth = strokeThickness, cap = StrokeCap.Round)
            }

            // Instruction label dynamically positioned right below the cutout window
            val instructionTopPadding = with(density) {
                val boxBottom = (maxAvailableH + boxSizePx) / 2
                (boxBottom + 16.dp.toPx()).toDp()
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = instructionTopPadding, start = 32.dp, end = 32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = "Point your camera at the QR code located on the back of the SiliGuide sensor.",
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontFamily = DMSans,
                    textAlign = TextAlign.Center
                )
            }
        }

        // ── Bottom Sheet Help & Manual Entry ────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(White)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .background(Border, CircleShape)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Having trouble?",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = Black
            )

            Text(
                text = "Ensure the room is well-lit and the sticker is not scratched or damaged.",
                fontSize = 12.sp,
                fontFamily = DMSans,
                color = GrayMedium,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
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
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
            ) {
                Text(
                    text = "Enter Details Manually",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = DMSans
                )
            }
        }
    }
}

// ── Multi-Device Previews ───────────────────────────────────────────────────

@Preview(name = "Phone - Standard (390x844)", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun PreviewQrScannerStandard() {
    SiliGuideTheme {
        QrScannerOverlay(onBack = {}, onManualEntry = {})
    }
}

@Preview(name = "Phone - Compact (320x580)", widthDp = 320, heightDp = 580, showBackground = true)
@Composable
private fun PreviewQrScannerCompact() {
    SiliGuideTheme {
        QrScannerOverlay(onBack = {}, onManualEntry = {})
    }
}

@Preview(name = "Phone - Tall (412x915)", widthDp = 412, heightDp = 915, showBackground = true)
@Composable
private fun PreviewQrScannerTall() {
    SiliGuideTheme {
        QrScannerOverlay(onBack = {}, onManualEntry = {})
    }
}

@Preview(name = "Tablet (800x1280)", widthDp = 800, heightDp = 1280, showBackground = true)
@Composable
private fun PreviewQrScannerTablet() {
    SiliGuideTheme {
        QrScannerOverlay(onBack = {}, onManualEntry = {})
    }
}