package com.example.siliguide.features.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.siliguide.core.image.BlurDetector
import com.example.siliguide.core.ui.theme.DarkGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CameraScreen(
    onImageCaptured: (Bitmap) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPreparingImage by remember { mutableStateOf(false) }

    // FIX 1: Hold ImageCapture reference so the capture button can actually use it
    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            isPreparingImage = true
            scope.launch(Dispatchers.IO) {
                try {
                    // Sample down to a reasonable size to avoid OOM
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }

                    options.inJustDecodeBounds = false
                    options.inSampleSize = calculateInSampleSize(options, 1080, 1080)
                    // FIX 4 (OOM): Force ARGB_8888 for consistent memory layout
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888

                    val bitmap = context.contentResolver.openInputStream(it)?.use { stream ->
                        BitmapFactory.decodeStream(stream, null, options)
                    }

                    if (bitmap != null) {
                        // FIX 4 (OOM): Scale blur check to a small thumbnail to avoid processing huge bitmaps
                        val blurBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                        val isBlurry = BlurDetector.isBlurry(blurBitmap)
                        blurBitmap.recycle()

                        withContext(Dispatchers.Main) {
                            if (isBlurry) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Uploaded image is too blurry.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                bitmap.recycle()
                            } else {
                                capturedBitmap = bitmap
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Camera", "Failed to load image", e)
                } finally {
                    withContext(Dispatchers.Main) { isPreparingImage = false }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (capturedBitmap != null) {
            ImageAdjustmentScreen(
                bitmap = capturedBitmap!!,
                onConfirm = { cropped ->
                    scope.launch(Dispatchers.Default) {
                        // FIX 4 (OOM): scale-down before blur check
                        val blurBitmap = Bitmap.createScaledBitmap(cropped, 256, 256, true)
                        val isBlurry = BlurDetector.isBlurry(blurBitmap)
                        blurBitmap.recycle()

                        withContext(Dispatchers.Main) {
                            if (isBlurry) {
                                android.widget.Toast.makeText(
                                    context,
                                    "Image is too blurry. Try again.",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                onImageCaptured(cropped)
                                capturedBitmap = null
                            }
                        }
                    }
                },
                onCancel = {
                    capturedBitmap?.recycle()
                    capturedBitmap = null
                }
            )
        } else if (hasCameraPermission) {
            var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

            DisposableEffect(Unit) {
                onDispose { cameraProvider?.unbindAll() }
            }

            // FIX 1: Build ImageCapture here and expose it via imageCaptureRef
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    ProcessCameraProvider.getInstance(ctx).apply {
                        addListener({
                            val provider = get()
                            cameraProvider = provider

                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageCapture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .build()

                            try {
                                provider.unbindAll()
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture
                                )
                                // Expose the capture use-case so the button can use it
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

            // FIX 1: Pass the capture lambda that actually calls takePicture
            CameraOverlay(
                onCapture = {
                    val imageCapture = imageCaptureRef ?: return@CameraOverlay
                    isPreparingImage = true

                    val outputFileOptions = ImageCapture.OutputFileOptions
                        .Builder(File.createTempFile("capture_", ".jpg", context.cacheDir))
                        .build()

                    imageCapture.takePicture(
                        outputFileOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val savedUri = output.savedUri ?: return@launch
                                        val opts = BitmapFactory.Options().apply {
                                            inPreferredConfig = Bitmap.Config.ARGB_8888
                                        }
                                        val bitmap = context.contentResolver
                                            .openInputStream(savedUri)
                                            ?.use { BitmapFactory.decodeStream(it, null, opts) }

                                        if (bitmap != null) {
                                            // FIX 4 (OOM): scale-down for blur check
                                            val blurBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
                                            val isBlurry = BlurDetector.isBlurry(blurBitmap)
                                            blurBitmap.recycle()

                                            withContext(Dispatchers.Main) {
                                                isPreparingImage = false
                                                if (isBlurry) {
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "Photo is too blurry. Try again.",
                                                        android.widget.Toast.LENGTH_SHORT
                                                    ).show()
                                                    bitmap.recycle()
                                                } else {
                                                    capturedBitmap = bitmap
                                                }
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) { isPreparingImage = false }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Camera", "Error processing captured image", e)
                                        withContext(Dispatchers.Main) { isPreparingImage = false }
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                Log.e("Camera", "Capture failed: ${exception.message}", exception)
                                isPreparingImage = false
                            }
                        }
                    )
                },
                onUpload = { galleryLauncher.launch("image/*") },
                onBack = onBack,
                onBoxCalculated = {}
            )
        }

        if (isPreparingImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.4f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
    val (height: Int, width: Int) = options.outHeight to options.outWidth
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2
        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

@Composable
fun ImageAdjustmentScreen(
    bitmap: Bitmap,
    onConfirm: (Bitmap) -> Unit,
    onCancel: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var boxRect by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { containerSize = it.size }
    ) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale, scaleY = scale,
                    translationX = offset.x, translationY = offset.y
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset += pan
                    }
                }
        )

        // FIX 2: Square bounding box — use the same dimension for width and height
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSide = minOf(size.width, size.height) * 0.75f
            val left = (size.width - boxSide) / 2
            val top = (size.height - boxSide) / 2
            boxRect = androidx.compose.ui.geometry.Rect(left, top, left + boxSide, top + boxSide)

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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)
            ) {
                Icon(Icons.Outlined.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text(
                "Adjust Image",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            IconButton(
                onClick = {
                    onConfirm(performManualCrop(bitmap, containerSize, boxRect, scale, offset))
                },
                modifier = Modifier.background(DarkGreen, CircleShape)
            ) {
                Icon(Icons.Outlined.Check, contentDescription = "Confirm", tint = Color.White)
            }
        }
    }
}

private fun performManualCrop(
    bitmap: Bitmap,
    containerSize: IntSize,
    boxRect: androidx.compose.ui.geometry.Rect,
    scale: Float,
    offset: Offset
): Bitmap {
    val bitmapWidth = bitmap.width.toFloat()
    val bitmapHeight = bitmap.height.toFloat()
    val containerWidth = containerSize.width.toFloat()
    val containerHeight = containerSize.height.toFloat()
    val fitScale = minOf(containerWidth / bitmapWidth, containerHeight / bitmapHeight)
    val matrix = Matrix().apply {
        postTranslate(-bitmapWidth / 2f, -bitmapHeight / 2f)
        postScale(fitScale * scale, fitScale * scale)
        postTranslate(containerWidth / 2f + offset.x, containerHeight / 2f + offset.y)
    }
    val inverse = Matrix().apply { matrix.invert(this) }
    val points = floatArrayOf(boxRect.left, boxRect.top, boxRect.right, boxRect.bottom)
    inverse.mapPoints(points)
    val left = points[0].toInt().coerceIn(0, bitmap.width - 1)
    val top = points[1].toInt().coerceIn(0, bitmap.height - 1)
    val right = points[2].toInt().coerceIn(left + 1, bitmap.width)
    val bottom = points[3].toInt().coerceIn(top + 1, bitmap.height)
    return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
}

@Composable
fun CameraOverlay(
    onCapture: () -> Unit,
    onUpload: () -> Unit,
    onBack: () -> Unit,
    onBoxCalculated: (Rect) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // FIX 2: Square viewfinder box in the camera overlay too
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxSide = minOf(size.width, size.height) * 0.75f
            val left = (size.width - boxSide) / 2
            val top = (size.height - boxSide) / 2
            onBoxCalculated(
                Rect(left.toInt(), top.toInt(), (left + boxSide).toInt(), (top + boxSide).toInt())
            )
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
                .padding(bottom = 60.dp)
                .size(80.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onCapture() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(DarkGreen)
                )
            }
        }

        IconButton(
            onClick = onUpload,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 72.dp, end = 40.dp)
                .size(56.dp)
                .background(Color.White.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(Icons.Outlined.Image, contentDescription = "Upload", tint = Color.White)
        }
    }
}