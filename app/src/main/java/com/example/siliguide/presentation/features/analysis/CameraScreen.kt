package com.example.siliguide.presentation.features.analysis

import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.components.LoadingOverlay
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.CameraBracket
import com.example.siliguide.core.ui.theme.CameraChipBg
import com.example.siliguide.core.ui.theme.CameraChipBorder
import com.example.siliguide.core.ui.theme.CameraControlsGradientEnd
import com.example.siliguide.core.ui.theme.CameraControlsGradientMid
import com.example.siliguide.core.ui.theme.CameraFocusFail
import com.example.siliguide.core.ui.theme.CameraFocusOk
import com.example.siliguide.core.ui.theme.CameraFocusRing
import com.example.siliguide.core.ui.theme.CameraGlassBg
import com.example.siliguide.core.ui.theme.CameraGlassBorder
import com.example.siliguide.core.ui.theme.CameraHintBg
import com.example.siliguide.core.ui.theme.CameraHintBorder
import com.example.siliguide.core.ui.theme.CameraHintStyle
import com.example.siliguide.core.ui.theme.CameraHintText
import com.example.siliguide.core.ui.theme.CameraPillStyle
import com.example.siliguide.core.ui.theme.CameraScrim
import com.example.siliguide.core.ui.theme.CameraSheetButtonStyle
import com.example.siliguide.core.ui.theme.CameraSheetTitleStyle
import com.example.siliguide.core.ui.theme.CameraShutterGlow
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenMid
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.LeafBgDark
import com.example.siliguide.core.ui.theme.White
import kotlinx.coroutines.delay
import java.io.File
import com.example.siliguide.core.ui.theme.CameraDialogBackdrop
import com.example.siliguide.core.ui.theme.CameraGlassBgActive
import com.example.siliguide.core.ui.theme.CameraTipNumberStyle
import com.example.siliguide.core.ui.theme.CameraTipTextStyle
import com.example.siliguide.core.ui.theme.GreenBgLight

/** Framing box = 75% of the smaller viewport dimension (per mockup notes). */
private const val CAMERA_BOX_FRACTION = 0.75f

@Composable
fun CameraScreen(
    plantId: String? = null,
    onImageCaptured: (String) -> Unit, // Now passes a URI String
    onBack: () -> Unit
) {

    val viewModel: CameraViewModel = hiltViewModel()

    val cameraUiState = viewModel.uiState
    val context = LocalContext.current
    val lifecycleOwner =
        androidx.lifecycle.compose.LocalLifecycleOwner.current

    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

    // Framing & lighting tips sheet (lightbulb button)
    var showTips by rememberSaveable { mutableStateOf(false) }

    // Gallery Picker: Copy the file to cache to avoid SecurityException permission denials
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->

        uri?.let {

            viewModel.processGalleryImage(
                context = context,
                galleryUri = it,
                onSuccess = onImageCaptured
            )
        }
    }

    // Clear focus ring after a delay
    LaunchedEffect(cameraUiState.tapPoint) {
        if (cameraUiState.tapPoint != null) {
            delay(1500)
            viewModel.onEvent(CameraEvent.ClearFocus)
        }
    }

    LoadingOverlay(
        isLoading =
            false,
        message = if (cameraUiState.isCapturing)
            "Capturing image..."
        else
            "Preparing image..."
    ) {

        var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }

        DisposableEffect(Unit) {
            onDispose { cameraProvider?.unbindAll() }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .safeDrawingPadding()
        ) {

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
                                .setFlashMode(cameraUiState.flashMode)
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
                            viewModel.onEvent(
                                CameraEvent.FocusTapped(
                                    Offset(event.x, event.y)
                                )
                            )

                            // Create the action and execute it
                            val action = FocusMeteringAction.Builder(point).build()
                            val result = cameraRef?.cameraControl?.startFocusAndMetering(action)

                            // Listen for focus completion
                            result?.addListener({
                                try {
                                    val focusResult = result.get()
                                    viewModel.onEvent(
                                        CameraEvent.FocusFinished(
                                            focusResult.isFocusSuccessful
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e("Camera", "Focus metering failed", e)
                                    viewModel.onEvent(
                                        CameraEvent.FocusFinished(false)
                                    )
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
                    imageCaptureRef?.flashMode = cameraUiState.flashMode
                }
            )

            CameraCaptureOverlay(
                onCapture = {
                    viewModel.onEvent(
                        CameraEvent.CaptureStarted
                    )

                    val imageCapture = imageCaptureRef ?: run {
                        viewModel.onEvent(
                            CameraEvent.CaptureFinished
                        )
                        return@CameraCaptureOverlay
                    }
                    val file = File.createTempFile("raw_capture_", ".jpg", context.cacheDir)
                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

                    imageCapture.takePicture(
                        outputFileOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(
                                output: ImageCapture.OutputFileResults
                            ) {

                                viewModel.onEvent(
                                    CameraEvent.CaptureFinished
                                )

                                output.savedUri?.let {
                                    onImageCaptured(it.toString())
                                } ?: onImageCaptured(
                                    android.net.Uri.fromFile(file).toString()
                                )
                            }
                            override fun onError(exc: ImageCaptureException) {

                                viewModel.onEvent(
                                    CameraEvent.CaptureFinished
                                )

                                Log.e(
                                    "Camera",
                                    "Capture failed",
                                    exc
                                )
                            }
                        }
                    )
                },
                onUpload = { galleryLauncher.launch("image/*") },
                onBack = onBack,
                onTips = { showTips = true },
                isTipsActive = showTips,
                tapPoint = cameraUiState.tapPoint,
                isFocusSuccessful = cameraUiState.isFocusSuccessful,
                flashMode = cameraUiState.flashMode,
                onFlashModeChange = {
                    viewModel.onEvent(CameraEvent.FlashClicked)
                }
            )

            if (showTips) {

                CapturingTipsDialog(
                    onDismiss = { showTips = false }
                )
            }
        }
    }
}

// ── Viewfinder overlay (screen 8A) ─────────────────────────────────────────

@Composable
private fun CameraCaptureOverlay(
    onCapture: () -> Unit,
    onUpload: () -> Unit,
    onBack: () -> Unit,
    onTips: () -> Unit,
    tapPoint: Offset?,
    isFocusSuccessful: Boolean?,
    flashMode: Int,
    onFlashModeChange: () -> Unit,
    isTipsActive: Boolean = false,
    bracketColor: Color = CameraBracket,
    instruction: String = "Center the Siling Labuyo leaf inside the box",
    hint: String? = null
) {

    Box(modifier = Modifier.fillMaxSize()) {

        // Scrim with a square cutout over the framing box
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    compositingStrategy = CompositingStrategy.Offscreen
                )
        ) {

            val boxPx = size.minDimension * CAMERA_BOX_FRACTION
            val left = (size.width - boxPx) / 2f
            val top = (size.height - boxPx) / 2f

            drawRect(color = CameraScrim)

            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxPx, boxPx),
                cornerRadius = CornerRadius(6.dp.toPx()),
                blendMode = BlendMode.Clear
            )
        }

        // Corner brackets only — no full border
        Canvas(modifier = Modifier.fillMaxSize()) {

            drawCornerBrackets(
                boxPx = size.minDimension * CAMERA_BOX_FRACTION,
                color = bracketColor,
                strokeWidthPx = 3.dp.toPx(),
                armPx = 30.dp.toPx(),
                radiusPx = 8.dp.toPx()
            )
        }

        // Tap-to-focus ring
        if (tapPoint != null) {

            val ringColor = when (isFocusSuccessful) {
                true -> CameraFocusOk
                false -> CameraFocusFail
                null -> CameraFocusRing
            }

            Canvas(modifier = Modifier.fillMaxSize()) {

                drawCircle(
                    color = ringColor,
                    radius = 30.dp.toPx(),
                    center = tapPoint,
                    style = Stroke(width = 1.6.dp.toPx())
                )

                drawCircle(
                    color = ringColor,
                    radius = 3.dp.toPx(),
                    center = tapPoint
                )
            }
        }

        // Close (top-left)
        RoundGlassButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 18.dp, top = 14.dp),
            onClick = onBack
        ) {
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

        // Flash (top-right)
        RoundGlassButton(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 18.dp, top = 14.dp),
            onClick = onFlashModeChange
        ) {
            Box(contentAlignment = Alignment.Center) {

                Canvas(modifier = Modifier.size(16.dp)) {

                    drawBolt(
                        filled = flashMode != ImageCapture.FLASH_MODE_OFF,
                        color = White,
                        strokeWidthPx = 1.8.dp.toPx()
                    )
                }

                if (flashMode == ImageCapture.FLASH_MODE_OFF) {

                    Canvas(modifier = Modifier.size(20.dp)) {

                        drawLine(
                            color = White,
                            start = Offset(size.width * 0.14f, size.height * 0.86f),
                            end = Offset(size.width * 0.86f, size.height * 0.14f),
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                if (flashMode == ImageCapture.FLASH_MODE_AUTO) {

                    Text(
                        text = "A",
                        style = CameraHintStyle,
                        color = White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 5.dp, y = 3.dp)
                    )
                }
            }
        }

        // Bottom controls: gradient + gallery / shutter / tips
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(160.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.6f to CameraControlsGradientMid,
                        1f to CameraControlsGradientEnd
                    )
                )
        ) {

            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(start = 26.dp, end = 26.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                SquareGlassButton(onClick = onUpload) {

                    Canvas(modifier = Modifier.size(20.dp)) {

                        drawUploadIcon(
                            color = White,
                            strokeWidthPx = 1.8.dp.toPx()
                        )
                    }
                }

                ShutterButton(onClick = onCapture)

                SquareGlassButton(
                    onClick = onTips,
                    active = isTipsActive
                ) {

                    Canvas(modifier = Modifier.size(20.dp)) {

                        drawBulbIcon(
                            color = White,
                            strokeWidthPx = 1.8.dp.toPx()
                        )
                    }
                }
            }
        }

        // Instruction pill
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 175.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(CameraChipBg)
                .border(
                    width = 1.dp,
                    color = CameraChipBorder,
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Canvas(modifier = Modifier.size(11.dp)) {

                drawCircle(
                    color = GreenMid,
                    radius = size.minDimension * 0.18f
                )

                drawCircle(
                    color = GreenMid,
                    radius = size.minDimension * 0.41f,
                    style = Stroke(width = 1.2.dp.toPx())
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = instruction,
                style = CameraPillStyle,
                color = White,
                maxLines = 1
            )
        }

        // Uppercase micro hint
        if (hint != null) {

            Text(
                text = hint,
                style = CameraHintStyle,
                color = CameraHintText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 210.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(CameraHintBg)
                    .border(
                        width = 1.dp,
                        color = CameraHintBorder,
                        shape = RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }
    }
}

// ── Capturing Tips dialog (screen 8B) ──────────────────────────────────────

private val CapturingTips = listOf(
    "Get close to the Siling Labuyo leaf so it fills most of the bounding box.",
    "Hold the camera steady and let it focus before capturing.",
    "Shoot in even daylight — avoid harsh shadows or backlight on the leaf."
)

@Composable
private fun CapturingTipsDialog(
    onDismiss: () -> Unit
) {

    val backdropInteraction = remember { MutableInteractionSource() }
    val cardInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CameraDialogBackdrop)
            .clickable(
                interactionSource = backdropInteraction,
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.Center
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(White)
                .clickable(
                    interactionSource = cardInteraction,
                    indication = null,
                    onClick = { /* swallow taps on the card */ }
                )
                .padding(start = 22.dp, end = 22.dp, top = 26.dp, bottom = 22.dp)
        ) {

            Text(
                text = "Capturing Tips",
                style = CameraSheetTitleStyle,
                color = Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            CapturingTips.forEachIndexed { index, tip ->

                if (index > 0) {

                    Spacer(modifier = Modifier.height(14.dp))
                }

                TipRow(
                    number = index + 1,
                    text = tip
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GotItButton(onClick = onDismiss)
        }
    }
}

@Composable
private fun TipRow(
    number: Int,
    text: String
) {

    Row(modifier = Modifier.fillMaxWidth()) {

        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(GreenBgLight),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text = number.toString(),
                style = CameraTipNumberStyle,
                color = GreenPrimary
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            style = CameraTipTextStyle,
            color = GrayBody,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun GotItButton(
    onClick: () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }

    Box(
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
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = "Got it",
            style = CameraSheetButtonStyle,
            color = White
        )
    }
}

// ── Glass / shutter buttons ────────────────────────────────────────────────

@Composable
private fun RoundGlassButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(CameraChipBg)
            .border(
                width = 1.dp,
                color = CameraChipBorder,
                shape = CircleShape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun SquareGlassButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    active: Boolean = false,
    content: @Composable () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(14.dp)

    Box(
        modifier = modifier
            .size(46.dp)
            .clip(shape)
            .background(if (active) CameraGlassBgActive else CameraGlassBg)
            .border(
                width = 1.dp,
                color = CameraGlassBorder,
                shape = shape
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun ShutterButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .size(82.dp)
            .clip(CircleShape)
            .background(CameraShutterGlow)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {

        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(White),
            contentAlignment = Alignment.Center
        ) {

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary)
            )
        }
    }
}

// ── Canvas icon painters ───────────────────────────────────────────────────

private fun DrawScope.drawCornerBrackets(
    boxPx: Float,
    color: Color,
    strokeWidthPx: Float,
    armPx: Float,
    radiusPx: Float
) {

    val left = (size.width - boxPx) / 2f
    val top = (size.height - boxPx) / 2f
    val right = left + boxPx
    val bottom = top + boxPx

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
        style = stroke
    )
}

/** Lightning bolt, 24-unit source coords scaled to canvas. */
private fun DrawScope.drawBolt(
    filled: Boolean,
    color: Color,
    strokeWidthPx: Float
) {

    val s = size.minDimension / 24f

    val path = Path().apply {
        moveTo(13f * s, 2f * s)
        lineTo(3f * s, 14f * s)
        lineTo(12f * s, 14f * s)
        lineTo(11f * s, 22f * s)
        lineTo(21f * s, 10f * s)
        lineTo(12f * s, 10f * s)
        close()
    }

    if (filled) {

        drawPath(path = path, color = color)
    } else {

        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = strokeWidthPx,
                cap = StrokeCap.Round
            )
        )
    }
}

/** Upload-from-gallery icon (arrow out of tray). */
private fun DrawScope.drawUploadIcon(
    color: Color,
    strokeWidthPx: Float
) {

    val s = size.minDimension / 24f

    val stroke = Stroke(
        width = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawPath(
        path = Path().apply {
            moveTo(3f * s, 15f * s)
            lineTo(3f * s, 19f * s)
            quadraticTo(3f * s, 21f * s, 5f * s, 21f * s)
            lineTo(19f * s, 21f * s)
            quadraticTo(21f * s, 21f * s, 21f * s, 19f * s)
            lineTo(21f * s, 15f * s)
        },
        color = color,
        style = stroke
    )

    drawPath(
        path = Path().apply {
            moveTo(7f * s, 8f * s)
            lineTo(12f * s, 3f * s)
            lineTo(17f * s, 8f * s)
        },
        color = color,
        style = stroke
    )

    drawLine(
        color = color,
        start = Offset(12f * s, 3f * s),
        end = Offset(12f * s, 15f * s),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )
}

/** Lightbulb tips icon. */
private fun DrawScope.drawBulbIcon(
    color: Color,
    strokeWidthPx: Float
) {

    val s = size.minDimension / 24f

    val stroke = Stroke(
        width = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawPath(
        path = Path().apply {
            moveTo(15f * s, 14f * s)
            quadraticTo(15.6f * s, 12.4f * s, 16.5f * s, 11.5f * s)
            quadraticTo(18f * s, 10f * s, 18f * s, 8f * s)
            quadraticTo(18f * s, 2.5f * s, 12f * s, 2.5f * s)
            quadraticTo(6f * s, 2.5f * s, 6f * s, 8f * s)
            quadraticTo(6f * s, 10f * s, 7.5f * s, 11.5f * s)
            quadraticTo(8.5f * s, 12.5f * s, 9f * s, 14f * s)
        },
        color = color,
        style = stroke
    )

    drawLine(
        color = color,
        start = Offset(9f * s, 18f * s),
        end = Offset(15f * s, 18f * s),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawLine(
        color = color,
        start = Offset(10f * s, 21.5f * s),
        end = Offset(14f * s, 21.5f * s),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )
}

/** Small camera glyph for the sheet CTA. */
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

/** Leaf thumbnail. Blurry variant = stacked low-alpha copies. */
private fun DrawScope.drawExampleLeaf(sharp: Boolean) {

    val w = size.width
    val h = size.height

    fun leafPath(scale: Float): Path {

        val cx = w / 2f
        val cy = h / 2f

        fun px(v: Float) = cx + (v - 0.5f) * w * scale
        fun py(v: Float) = cy + (v - 0.5f) * h * scale

        return Path().apply {
            moveTo(px(0.5f), py(0.08f))
            cubicTo(
                px(0.92f), py(0.28f),
                px(0.90f), py(0.66f),
                px(0.58f), py(0.94f)
            )
            cubicTo(
                px(0.24f), py(0.82f),
                px(0.10f), py(0.42f),
                px(0.5f), py(0.08f)
            )
            close()
        }
    }

    val brush = Brush.radialGradient(
        colors = listOf(GreenMid, GreenDark),
        center = Offset(w * 0.45f, h * 0.45f),
        radius = w * 0.75f
    )

    if (sharp) {

        drawPath(path = leafPath(1f), brush = brush)

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
    } else {

        listOf(
            1.14f to 0.16f,
            1.08f to 0.22f,
            1.02f to 0.30f,
            0.96f to 0.42f
        ).forEach { (scale, alpha) ->

            drawPath(
                path = leafPath(scale),
                brush = brush,
                alpha = alpha
            )
        }
    }
}