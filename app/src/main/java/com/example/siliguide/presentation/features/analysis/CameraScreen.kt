package com.example.siliguide.presentation.features.analysis

import android.util.Log
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.components.LoadingOverlay
import com.example.siliguide.presentation.features.analysis.components.CameraOverlay
import kotlinx.coroutines.delay
import java.io.File

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

    val scope = rememberCoroutineScope() // Needed for background file copying

    var imageCaptureRef by remember { mutableStateOf<ImageCapture?>(null) }

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
                    cameraUiState.isCapturing ||
                            cameraUiState.isProcessingGalleryImage,
                message = if (cameraUiState.isCapturing)
                    "Capturing image..."
                else
                    "Preparing image..."
            ) {
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

                    // Your existing overlay logic remains exactly the same!
                    CameraOverlay(
                        onCapture = {
                            viewModel.onEvent(
                                CameraEvent.CaptureStarted
                            )

                            val imageCapture = imageCaptureRef ?: run {
                                viewModel.onEvent(
                                    CameraEvent.CaptureFinished
                                )
                                return@CameraOverlay
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
                        tapPoint = cameraUiState.tapPoint,
                        isFocusSuccessful = cameraUiState.isFocusSuccessful,
                        flashMode = cameraUiState.flashMode,
                        onFlashModeChange = {
                            viewModel.onEvent(CameraEvent.FlashClicked)
                        }
                    )
                }
            }


}

