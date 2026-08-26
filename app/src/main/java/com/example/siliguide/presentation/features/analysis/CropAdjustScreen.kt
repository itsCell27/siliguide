package com.example.siliguide.presentation.features.analysis

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.scale
import androidx.core.net.toUri
import com.example.siliguide.core.image.BlurDetector
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.CameraBracket
import com.example.siliguide.core.ui.theme.CameraBracketError
import com.example.siliguide.core.ui.theme.CameraExampleLabelStyle
import com.example.siliguide.core.ui.theme.CameraGlassBg
import com.example.siliguide.core.ui.theme.CameraGlassBorder
import com.example.siliguide.core.ui.theme.CameraHintBorder
import com.example.siliguide.core.ui.theme.CameraSheetBodyStyle
import com.example.siliguide.core.ui.theme.CameraSheetButtonStyle
import com.example.siliguide.core.ui.theme.CameraSheetTitleStyle
import com.example.siliguide.core.ui.theme.CameraTipHeadingStyle
import com.example.siliguide.core.ui.theme.CropGridLine
import com.example.siliguide.core.ui.theme.CropHintBg
import com.example.siliguide.core.ui.theme.CropHintStyle
import com.example.siliguide.core.ui.theme.CropHintText
import com.example.siliguide.core.ui.theme.CropLoadingScrim
import com.example.siliguide.core.ui.theme.CropLoadingStepStyle
import com.example.siliguide.core.ui.theme.CropLoadingTitleStyle
import com.example.siliguide.core.ui.theme.CropScrim
import com.example.siliguide.core.ui.theme.CropStepActive
import com.example.siliguide.core.ui.theme.CropStepDone
import com.example.siliguide.core.ui.theme.CropStepPending
import com.example.siliguide.core.ui.theme.CropTopBarTitleStyle
import com.example.siliguide.core.ui.theme.ExampleBadBorder
import com.example.siliguide.core.ui.theme.ExampleBadMark
import com.example.siliguide.core.ui.theme.ExampleGoodBorder
import com.example.siliguide.core.ui.theme.ExampleGoodMark
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenMid
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.LeafBgDark
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.SheetHandle
import com.example.siliguide.core.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.example.siliguide.R

/** Crop box = 75% of the smaller container dimension. */
private const val CROP_BOX_FRACTION = 0.75f

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

    // Blurry feedback state — holds the user's own rejected crop for preview
    var blurryPreview by remember { mutableStateOf<Bitmap?>(null) }
    val showBlurSheet = blurryPreview != null

    val goodFocusImage = ImageBitmap.imageResource(R.drawable.curling).asAndroidBitmap()
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = GreenDark)
        }
        return
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var isGesturing by remember { mutableStateOf(false) }

    // Rule-of-thirds grid only shows during / just after interaction
    LaunchedEffect(scale, offset) {
        if (isGesturing) {
            delay(900)
            isGesturing = false
        }
    }

    val boxRect = remember(containerSize) {
        val w = containerSize.width.toFloat()
        val h = containerSize.height.toFloat()
        val side = minOf(w, h) * CROP_BOX_FRACTION
        val left = (w - side) / 2f
        val top = (h - side) / 2f
        Rect(left, top, left + side, top + side)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
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
                .pointerInput(showBlurSheet) {
                    if (showBlurSheet) return@pointerInput
                    detectTransformGestures { _, pan, zoom, _ ->
                        isGesturing = true
                        scale = (scale * zoom).coerceIn(0.5f, 5f)
                        offset += pan
                    }
                }
        )

        // Scrim with the square cutout
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    compositingStrategy = CompositingStrategy.Offscreen
                )
        ) {
            drawRect(color = CropScrim)

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(boxRect.left, boxRect.top),
                size = Size(boxRect.width, boxRect.height),
                cornerRadius = CornerRadius(6.dp.toPx()),
                blendMode = BlendMode.Clear
            )
        }

        // Thirds grid + corner brackets
        Canvas(modifier = Modifier.fillMaxSize()) {

            if (isGesturing && !showBlurSheet) {

                drawThirdsGrid(
                    rect = boxRect,
                    color = CropGridLine,
                    strokeWidthPx = 1.dp.toPx()
                )
            }

            drawCornerBrackets(
                rect = boxRect,
                color = if (showBlurSheet) CameraBracketError else CameraBracket,
                alpha = if (showBlurSheet) 0.6f else 1f,
                strokeWidthPx = 3.dp.toPx(),
                armPx = 30.dp.toPx(),
                radiusPx = 8.dp.toPx()
            )
        }

        // Top bar: cancel · title · confirm
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            GlassCircleButton(onClick = onCancel, isProcessing = !isProcessing) {

                Canvas(modifier = Modifier.size(14.dp)) {

                    val sw = 1.6.dp.toPx()

                    drawLine(
                        color = White,
                        start = Offset(size.width * 0.25f, size.height * 0.25f),
                        end = Offset(size.width * 0.75f, size.height * 0.75f),
                        strokeWidth = sw,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = White,
                        start = Offset(size.width * 0.75f, size.height * 0.25f),
                        end = Offset(size.width * 0.25f, size.height * 0.75f),
                        strokeWidth = sw,
                        cap = StrokeCap.Round
                    )
                }
            }

            Text(
                text = "Adjust Image",
                style = CropTopBarTitleStyle,
                color = White
            )

            ConfirmCircleButton(
                enabled = !isProcessing && !showBlurSheet,
                onClick = {
                    if (isProcessing) return@ConfirmCircleButton
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
                                // Show the sheet with the user's own rejected crop
                                blurryPreview = cropped
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
                }
            )
        }

        // Gesture hint pill
        if (!showBlurSheet && !isProcessing) {

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 130.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(CropHintBg)
                    .border(
                        width = 1.dp,
                        color = CameraHintBorder,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Pinch to zoom",
                    style = CropHintStyle,
                    color = CropHintText
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(CropStepPending)
                )

                Text(
                    text = "Drag to reposition",
                    style = CropHintStyle,
                    color = CropHintText
                )
            }
        }

        // Pipeline loading overlay (screen 9B)
//        if (isProcessing) {
//
//            PipelineLoadingOverlay()
//        }

        // Blurry feedback sheet (screen 9C)
        AnimatedVisibility(
            visible = showBlurSheet,
            enter = slideInVertically(
                animationSpec = tween(durationMillis = 400),
                initialOffsetY = { it }
            ),
            exit = slideOutVertically(
                animationSpec = tween(durationMillis = 250),
                targetOffsetY = { it }
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {

            BlurryImageSheet(
                blurryImage = blurryPreview,
                onNewPicture = {
                    blurryPreview?.recycle()
                    blurryPreview = null
                    onCancel()
                },
                goodFocusImage = goodFocusImage
            )
        }
    }
}

// ── Loading overlay (screen 9B) ────────────────────────────────────────────

private enum class PipelineStep(val label: String) {
    Quality("Checking image quality"),
    LeafCheck("Verifying Siling Labuyo leaf"),
    Symptom("Predicting visual symptom")
}

@Composable
private fun PipelineLoadingOverlay() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CropLoadingScrim),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            CircularProgressIndicator(
                modifier = Modifier.size(46.dp),
                color = White,
                trackColor = CropGridLine,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "Checking image quality…",
                style = CropLoadingTitleStyle,
                color = White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Step 1 of 3",
                style = CropLoadingStepStyle,
                color = CropStepPending
            )

            Spacer(modifier = Modifier.height(14.dp))

            Column(horizontalAlignment = Alignment.Start) {

                PipelineStep.entries.forEachIndexed { index, step ->

                    if (index > 0) {

                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    StepRow(
                        label = step.label,
                        isActive = index == 0,
                        isDone = false
                    )
                }
            }
        }
    }
}

@Composable
private fun StepRow(
    label: String,
    isActive: Boolean,
    isDone: Boolean
) {

    val tint = when {
        isDone -> CropStepDone
        isActive -> CropStepActive
        else -> CropStepPending
    }

    Row(verticalAlignment = Alignment.CenterVertically) {

        Box(
            modifier = Modifier.size(14.dp),
            contentAlignment = Alignment.Center
        ) {

            Canvas(modifier = Modifier.fillMaxSize()) {

                drawCircle(
                    color = tint,
                    radius = size.minDimension / 2f - 0.75.dp.toPx(),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                if (isDone) {

                    drawPath(
                        path = Path().apply {
                            moveTo(size.width * 0.28f, size.height * 0.52f)
                            lineTo(size.width * 0.45f, size.height * 0.70f)
                            lineTo(size.width * 0.74f, size.height * 0.32f)
                        },
                        color = tint,
                        style = Stroke(
                            width = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )
                } else if (isActive) {

                    drawCircle(
                        color = tint,
                        radius = 2.5.dp.toPx()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = label,
            style = CropLoadingStepStyle,
            color = tint
        )
    }
}

// ── Blurry feedback sheet (screen 9C) ──────────────────────────────────────

@Composable
private fun BlurryImageSheet(
    blurryImage: Bitmap?,
    onNewPicture: () -> Unit,
    goodFocusImage: Bitmap
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp
                )
            )
            .background(OffWhite)
            .navigationBarsPadding()
            .padding(start = 22.dp, end = 22.dp, top = 14.dp, bottom = 28.dp)
    ) {

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(SheetHandle)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Image too blurry",
            style = CameraSheetTitleStyle,
            color = Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Make sure your camera clearly focuses on the leaf as shown in the example images.",
            style = CameraSheetBodyStyle,
            color = GrayBody
        )

        Spacer(modifier = Modifier.height(14.dp))

        NewPictureButton(onClick = onNewPicture)

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "How to take a better picture:",
            style = CameraTipHeadingStyle,
            color = Black
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            // Left: the user's own rejected crop
            ExampleCard(
                modifier = Modifier.weight(1f),
                label = "Too blurry",
                isGood = false,
                image = blurryImage
            )

            // Right: reference for good focus
            ExampleCard(
                modifier = Modifier.weight(1f),
                label = "Good focus",
                isGood = true,
                image = goodFocusImage
            )
        }
    }
}

@Composable
private fun ExampleCard(
    modifier: Modifier = Modifier,
    label: String,
    isGood: Boolean,
    image: Bitmap?
) {

    Column(modifier = modifier) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(LeafBgDark)
                .border(
                    width = 2.dp,
                    color = if (isGood) ExampleGoodBorder else ExampleBadBorder,
                    shape = RoundedCornerShape(10.dp)
                )
        ) {

            if (image != null) {

                Image(
                    bitmap = image.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {

                Canvas(modifier = Modifier.fillMaxSize()) {

                    drawExampleLeaf()
                }
            }
        }

        Spacer(modifier = Modifier.height(5.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {

            Text(
                text = if (isGood) "✓" else "✕",
                style = CameraExampleLabelStyle,
                color = if (isGood) ExampleGoodMark else ExampleBadMark
            )

            Spacer(modifier = Modifier.width(5.dp))

            Text(
                text = label,
                style = CameraExampleLabelStyle,
                color = GrayBody
            )
        }
    }
}

@Composable
private fun NewPictureButton(
    onClick: () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(GreenPrimary)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Canvas(modifier = Modifier.size(15.dp)) {

            drawCameraGlyph(
                color = White,
                strokeWidthPx = 1.3.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "New picture",
            style = CameraSheetButtonStyle,
            color = White
        )
    }
}

// ── Top bar buttons ────────────────────────────────────────────────────────

@Composable
private fun GlassCircleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isProcessing: Boolean = true,
    content: @Composable () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(CameraGlassBg)
            .border(
                width = 1.dp,
                color = CameraGlassBorder,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interaction,
                enabled = isProcessing,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ConfirmCircleButton(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onClick: () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(38.dp)
            .graphicsLayer(alpha = if (enabled) 1f else 0.4f)
            .clip(CircleShape)
            .background(GreenPrimary)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.size(16.dp)) {

            drawPath(
                path = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.53f)
                    lineTo(size.width * 0.44f, size.height * 0.72f)
                    lineTo(size.width * 0.75f, size.height * 0.28f)
                },
                color = White,
                style = Stroke(
                    width = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

// ── Canvas painters ────────────────────────────────────────────────────────

private fun DrawScope.drawThirdsGrid(
    rect: Rect,
    color: Color,
    strokeWidthPx: Float
) {

    val thirdW = rect.width / 3f
    val thirdH = rect.height / 3f

    for (i in 1..2) {

        drawLine(
            color = color,
            start = Offset(rect.left + thirdW * i, rect.top),
            end = Offset(rect.left + thirdW * i, rect.bottom),
            strokeWidth = strokeWidthPx
        )

        drawLine(
            color = color,
            start = Offset(rect.left, rect.top + thirdH * i),
            end = Offset(rect.right, rect.top + thirdH * i),
            strokeWidth = strokeWidthPx
        )
    }
}

private fun DrawScope.drawCornerBrackets(
    rect: Rect,
    color: Color,
    alpha: Float,
    strokeWidthPx: Float,
    armPx: Float,
    radiusPx: Float
) {

    val left = rect.left
    val top = rect.top
    val right = rect.right
    val bottom = rect.bottom

    val stroke = Stroke(
        width = strokeWidthPx,
        cap = StrokeCap.Round
    )

    // Top-left
    drawPath(
        path = Path().apply {
            moveTo(left, top + armPx)
            lineTo(left, top + radiusPx)
            quadraticTo(left, top, left + radiusPx, top)
            lineTo(left + armPx, top)
        },
        color = color,
        alpha = alpha,
        style = stroke
    )

    // Top-right
    drawPath(
        path = Path().apply {
            moveTo(right - armPx, top)
            lineTo(right - radiusPx, top)
            quadraticTo(right, top, right, top + radiusPx)
            lineTo(right, top + armPx)
        },
        color = color,
        alpha = alpha,
        style = stroke
    )

    // Bottom-left
    drawPath(
        path = Path().apply {
            moveTo(left, bottom - armPx)
            lineTo(left, bottom - radiusPx)
            quadraticTo(left, bottom, left + radiusPx, bottom)
            lineTo(left + armPx, bottom)
        },
        color = color,
        alpha = alpha,
        style = stroke
    )

    // Bottom-right
    drawPath(
        path = Path().apply {
            moveTo(right - armPx, bottom)
            lineTo(right - radiusPx, bottom)
            quadraticTo(right, bottom, right, bottom - radiusPx)
            lineTo(right, bottom - armPx)
        },
        color = color,
        alpha = alpha,
        style = stroke
    )
}

/** Reference "good focus" leaf thumbnail. */
private fun DrawScope.drawExampleLeaf() {

    val w = size.width
    val h = size.height

    val path = Path().apply {
        moveTo(w * 0.5f, h * 0.08f)
        cubicTo(
            w * 0.92f, h * 0.28f,
            w * 0.90f, h * 0.66f,
            w * 0.58f, h * 0.94f
        )
        cubicTo(
            w * 0.24f, h * 0.82f,
            w * 0.10f, h * 0.42f,
            w * 0.5f, h * 0.08f
        )
        close()
    }

    drawPath(
        path = path,
        brush = Brush.radialGradient(
            colors = listOf(GreenMid, GreenDark),
            center = Offset(w * 0.45f, h * 0.45f),
            radius = w * 0.75f
        )
    )

    drawPath(
        path = Path().apply {
            moveTo(w * 0.5f, h * 0.14f)
            quadraticTo(
                w * 0.44f, h * 0.5f,
                w * 0.58f, h * 0.9f
            )
        },
        color = LeafBgDark,
        style = Stroke(width = w * 0.014f, cap = StrokeCap.Round)
    )

    drawCircle(
        color = Color(0x995A3818),
        radius = w * 0.035f,
        center = Offset(w * 0.40f, h * 0.46f)
    )

    drawCircle(
        color = Color(0x993A2A10),
        radius = w * 0.028f,
        center = Offset(w * 0.58f, h * 0.62f)
    )
}

/** Small camera glyph for the New picture CTA. */
private fun DrawScope.drawCameraGlyph(
    color: Color,
    strokeWidthPx: Float
) {

    val s = size.minDimension / 15f

    val stroke = Stroke(
        width = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawPath(
        path = Path().apply {
            moveTo(3f * s, 4.5f * s)
            lineTo(5f * s, 4.5f * s)
            lineTo(6f * s, 3.3f * s)
            lineTo(9f * s, 3.3f * s)
            lineTo(10f * s, 4.5f * s)
            lineTo(12f * s, 4.5f * s)
            quadraticTo(13f * s, 4.5f * s, 13f * s, 5.5f * s)
            lineTo(13f * s, 11.5f * s)
            quadraticTo(13f * s, 12.5f * s, 12f * s, 12.5f * s)
            lineTo(3f * s, 12.5f * s)
            quadraticTo(2f * s, 12.5f * s, 2f * s, 11.5f * s)
            lineTo(2f * s, 5.5f * s)
            quadraticTo(2f * s, 4.5f * s, 3f * s, 4.5f * s)
            close()
        },
        color = color,
        style = stroke
    )

    drawCircle(
        color = color,
        radius = 2.2f * s,
        center = Offset(7.5f * s, 8.5f * s),
        style = stroke
    )
}

private fun performManualCrop(bitmap: Bitmap, containerSize: IntSize, boxRect: Rect, scale: Float, offset: Offset): Bitmap {
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