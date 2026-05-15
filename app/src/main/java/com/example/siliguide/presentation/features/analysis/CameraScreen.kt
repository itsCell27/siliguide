package com.example.siliguide.presentation.features.analysis

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
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
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.siliguide.core.ui.theme.GreenDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CameraScreen(
    onImageCaptured: (String) -> Unit, // Now passes a URI String
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val scope = rememberCoroutineScope() // Needed for background file copying

    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

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

    if (hasCameraPermission) {
        var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

        DisposableEffect(Unit) {
            onDispose { cameraProvider?.unbindAll() }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    ProcessCameraProvider.getInstance(ctx).apply {
                        addListener({
                            val provider = get()
                            cameraProvider = provider

                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                                imageCaptureRef = imageCapture
                            } catch (e: Exception) {
                                Log.e("Camera", "Binding failed", e)
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                    }
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

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
                                // Camera Capture: Immediately pass the saved file URI to the crop screen!
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
                onBack = onBack
            )
        }
    }
}

@Composable
fun CameraOverlay(onCapture: () -> Unit, onUpload: () -> Unit, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSide = minOf(size.width, size.height) * 0.75f
            val left = (size.width - boxSide) / 2
            val top = (size.height - boxSide) / 2
            val lineLength = 80f
            val right = left + boxSide
            val bottom = top + boxSide
            drawLine(Color.White, Offset(left, top), Offset(left + lineLength, top), strokeWidth = 6f)
            drawLine(Color.White, Offset(left, top), Offset(left, top + lineLength), strokeWidth = 6f)
            drawLine(Color.White, Offset(right, top), Offset(right - lineLength, top), strokeWidth = 6f)
            drawLine(Color.White, Offset(right, top), Offset(right, top + lineLength), strokeWidth = 6f)
            drawLine(Color.White, Offset(left, bottom), Offset(left + lineLength, bottom), strokeWidth = 6f)
            drawLine(Color.White, Offset(left, bottom), Offset(left, bottom - lineLength), strokeWidth = 6f)
            drawLine(Color.White, Offset(right, bottom), Offset(right - lineLength, bottom), strokeWidth = 6f)
            drawLine(Color.White, Offset(right, bottom), Offset(right, bottom - lineLength), strokeWidth = 6f)
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