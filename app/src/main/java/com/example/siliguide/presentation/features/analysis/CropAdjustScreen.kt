package com.example.siliguide.presentation.features.analysis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.siliguide.core.image.BlurDetector
import com.example.siliguide.core.ui.theme.GreenDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import androidx.core.net.toUri
import androidx.core.graphics.scale
import android.graphics.Canvas
import android.graphics.Paint

@Composable
fun CropAdjustScreen(
    rawImageUri: String,
    plantId: String? = null,
    onCropped: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // 1. Load the raw image efficiently
    LaunchedEffect(rawImageUri) {
        withContext(Dispatchers.IO) {
            try {
                val uri = rawImageUri.toUri()

                // FIRST PASS: Only get the dimensions, don't load into RAM yet
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }

                // Calculate a safe downsampling scale (max 1080x1080)
                options.inSampleSize = calculateInSampleSize(options, 1080, 1080)

                // SECOND PASS: Actually load the scaled-down image into RAM
                options.inJustDecodeBounds = false
                options.inPreferredConfig = Bitmap.Config.ARGB_8888

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    bitmap = BitmapFactory.decodeStream(stream, null, options)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    if (bitmap == null) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
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
            bitmap = bitmap!!.asImageBitmap(),
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

        // Square bounding box
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
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onCancel, modifier = Modifier.background(Color.Black.copy(0.4f), CircleShape)) {
                Icon(Icons.Outlined.Close, contentDescription = "Cancel", tint = Color.White)
            }
            Text("Adjust Image", color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.CenterVertically))
            IconButton(
                onClick = {
                    if (isProcessing) return@IconButton
                    isProcessing = true
                    scope.launch(Dispatchers.Default) {
                        // 2. Perform the crop
                        val cropped = performManualCrop(bitmap!!, containerSize, boxRect, scale, offset)

                        // 3. Blur Detection
                        val blurBitmap = cropped.scale(256, 256)
                        val isBlurry = BlurDetector.isBlurry(blurBitmap)
                        blurBitmap.recycle()

                        withContext(Dispatchers.Main) {
                            if (isBlurry) {
                                Toast.makeText(context, "Image is too blurry. Try again.", Toast.LENGTH_SHORT).show()
                                isProcessing = false
                            } else {
                                // 4. Save and pass the new URI
                                val file = File(context.cacheDir, "cropped_leaf_${System.currentTimeMillis()}.jpg")
                                file.outputStream().use { out ->
                                    cropped.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                }
                                onCropped(Uri.fromFile(file).toString())
                            }
                        }
                    }
                },
                modifier = Modifier.background(GreenDark, CircleShape)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.Check, contentDescription = "Confirm", tint = Color.White)
                }
            }
        }
    }
}

private fun performManualCrop(bitmap: Bitmap, containerSize: IntSize, boxRect: androidx.compose.ui.geometry.Rect, scale: Float, offset: Offset): Bitmap {
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
    val cropped = Bitmap.createBitmap(
        bitmap,
        left,
        top,
        right - left,
        bottom - top
    )

    android.util.Log.d(
        "CropAdjust",
        "Crop result = ${cropped.width} x ${cropped.height}"
    )

    // -----------------------------
    // DEBUG: Draw a red border
    // -----------------------------
    val canvas = Canvas(cropped)

    val paint = Paint().apply {
        color = android.graphics.Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    canvas.drawRect(
        0f,
        0f,
        cropped.width.toFloat(),
        cropped.height.toFloat(),
        paint
    )

    return cropped
}

// Memory Optimization Helper
private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
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