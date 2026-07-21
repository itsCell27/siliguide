package com.example.siliguide.presentation.features.analysis

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.siliguide.core.ui.theme.GreenDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CameraScreen(
    plantId: String? = null,
    onImageCaptured: (String) -> Unit, // Now passes a URI String
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope() // Needed for background file copying

    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    // Tap to focus state
    var tapPoint by remember { mutableStateOf<Offset?>(null) }
    var isFocusSuccessful by remember { mutableStateOf<Boolean?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) onBack()
        }
    )

    // Gallery Picker: Copy the file to cache to avoid SecurityException permission denials
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { galleryUri ->
            scope.launch(Dispatchers.IO) {
                try {
                    // 1. Figure out the actual file extension (png, heic, jpg, etc.)
                    val contentResolver = context.contentResolver
                    val mimeType = contentResolver.getType(galleryUri)
                    val extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"

                    // 2. Create the temp file with the CORRECT extension
                    val file = File.createTempFile("gallery_raw_", ".$extension", context.cacheDir)

                    // 3. Copy the data over safely
                    contentResolver.openInputStream(galleryUri)?.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        onImageCaptured(android.net.Uri.fromFile(file).toString())
                    }
                } catch (e: Exception) {
                    Log.e("CameraScreen", "Failed to process gallery image", e)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Clear focus ring after a delay
    LaunchedEffect(tapPoint) {
        if (tapPoint != null) {
            delay(1500)
            tapPoint = null
            isFocusSuccessful = null
        }
    }

    if (hasCameraPermission) {
        var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

        DisposableEffect(Unit) {
            onDispose { cameraProvider?.unbindAll() }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    var cameraRef: Camera? = null // Reference to control focus

                    ProcessCameraProvider.getInstance(ctx).apply {
                        addListener({
                            val provider = get()
                            cameraProvider = provider

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setFlashMode(flashMode)
                                .build()

                            try {
                                provider.unbindAll()
                                // Save the camera instance returned by bindToLifecycle
                                cameraRef = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                                imageCaptureRef = imageCapture
                            } catch (e: Exception) {
                                Log.e("Camera", "Binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }

                    // --- TAP TO FOCUS IMPLEMENTATION ---
                    previewView.setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            // Convert screen tap coordinates to camera sensor coordinates
                            val factory = previewView.meteringPointFactory
                            val point = factory.createPoint(event.x, event.y)

                            // Update UI state for the ring
                            tapPoint = Offset(event.x, event.y)
                            isFocusSuccessful = null

                            // Create the action and execute it
                            val action = FocusMeteringAction.Builder(point).build()
                            val result = cameraRef?.cameraControl?.startFocusAndMetering(action)

                            // Listen for focus completion
                            result?.addListener({
                                try {
                                    val focusResult = result.get()
                                    isFocusSuccessful = focusResult.isFocusSuccessful
                                } catch (e: Exception) {
                                    Log.e("Camera", "Focus metering failed", e)
                                    isFocusSuccessful = false
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            view.performClick() // Good practice for accessibility
                        }
                        true // Consume the touch event
                    }

                    previewView
                },
                modifier = Modifier.fillMaxSize(),
                update = {
                    imageCaptureRef?.flashMode = flashMode
                }
            )

            // Your existing overlay logic remains exactly the same!
            CameraOverlay(
                onCapture = {
                    val imageCapture = imageCaptureRef ?: return@CameraOverlay
                    val file = File.createTempFile("raw_capture_", ".jpg", context.cacheDir)
                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                    imageCapture.takePicture(
                        outputFileOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                output.savedUri?.let { onImageCaptured(it.toString()) }
                                    ?: onImageCaptured(android.net.Uri.fromFile(file).toString())
                            }
                            override fun onError(exc: ImageCaptureException) {
                                Log.e("Camera", "Capture failed", exc)
                            }
                        }
                    )
                },
                onUpload = { galleryLauncher.launch("image/*") },
                onBack = onBack,
                tapPoint = tapPoint,
                isFocusSuccessful = isFocusSuccessful,
                flashMode = flashMode,
                onFlashModeChange = {
                    flashMode = when (flashMode) {
                        ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                        ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                        else -> ImageCapture.FLASH_MODE_OFF
                    }
                }
            )
        }
    }
}

@Composable
fun CameraOverlay(
    onCapture: () -> Unit,
    onUpload: () -> Unit,
    onBack: () -> Unit,
    tapPoint: Offset? = null,
    isFocusSuccessful: Boolean? = null,
    flashMode: Int = ImageCapture.FLASH_MODE_OFF,
    onFlashModeChange: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // Flash Button
        IconButton(
            onClick = onFlashModeChange,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            val icon = when (flashMode) {
                ImageCapture.FLASH_MODE_ON -> Icons.Outlined.FlashOn
                ImageCapture.FLASH_MODE_AUTO -> Icons.Outlined.FlashAuto
                else -> Icons.Outlined.FlashOff
            }
            Icon(icon, contentDescription = "Flash Mode", tint = Color.White)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSide = minOf(size.width, size.height) * 0.75f
            val left = (size.width - boxSide) / 2
            val top = (size.height - boxSide) / 2
            val lineLength = 80f
            val right = left + boxSide
            val bottom = top + boxSide

            // Draw Corner Brackets
            drawLine(Color.White, Offset(left, top), Offset(left + lineLength, top), strokeWidth = 6f)
            drawLine(Color.White, Offset(left, top), Offset(left, top + lineLength), strokeWidth = 6f)
            drawLine(Color.White, Offset(right, top), Offset(right - lineLength, top), strokeWidth = 6f)
            drawLine(Color.White, Offset(right, top), Offset(right, top + lineLength), strokeWidth = 6f)
            drawLine(Color.White, Offset(left, bottom), Offset(left + lineLength, bottom), strokeWidth = 6f)
            drawLine(Color.White, Offset(left, bottom), Offset(left, bottom - lineLength), strokeWidth = 6f)
            drawLine(Color.White, Offset(right, bottom), Offset(right - lineLength, bottom), strokeWidth = 6f)
            drawLine(Color.White, Offset(right, bottom), Offset(right, bottom - lineLength), strokeWidth = 6f)

            // Draw Tap-to-Focus Ring
            tapPoint?.let { point ->
                val color = when (isFocusSuccessful) {
                    true -> Color.Green
                    false -> Color.Red
                    null -> Color(0xFFFFEB3B) // Yellowish-gold
                }
                val ringSize = 70.dp.toPx()
                drawRect(
                    color = color,
                    topLeft = Offset(point.x - ringSize / 2, point.y - ringSize / 2),
                    size = androidx.compose.ui.geometry.Size(ringSize, ringSize),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 60.dp)
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(modifier = Modifier.size(64.dp).clip(CircleShape).background(Color.White).clickable { onCapture() }, contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(GreenDark))
            }
        }

        IconButton(
            onClick = onUpload,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 72.dp, end = 40.dp)
                .size(56.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(Icons.Outlined.Image, contentDescription = "Upload", tint = Color.White)
        }
    }
}
