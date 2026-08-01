package com.example.siliguide.presentation.features.analysis

import androidx.camera.core.ImageCapture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@HiltViewModel
class CameraViewModel @Inject constructor() : ViewModel() {

    var uiState by mutableStateOf(CameraUiState())
        private set

    fun onEvent(event: CameraEvent) {

        uiState = when (event) {

            CameraEvent.FlashClicked -> {

                uiState.copy(
                    flashMode = when (uiState.flashMode) {
                        ImageCapture.FLASH_MODE_OFF ->
                            ImageCapture.FLASH_MODE_ON

                        ImageCapture.FLASH_MODE_ON ->
                            ImageCapture.FLASH_MODE_AUTO

                        else ->
                            ImageCapture.FLASH_MODE_OFF
                    }
                )
            }

            is CameraEvent.FocusTapped -> {

                uiState.copy(
                    tapPoint = event.point,
                    isFocusSuccessful = null
                )
            }

            is CameraEvent.FocusFinished -> {

                uiState.copy(
                    isFocusSuccessful = event.success
                )
            }

            is CameraEvent.PermissionChanged -> {

                uiState.copy(
                    hasCameraPermission = event.granted
                )
            }

            CameraEvent.ClearFocus -> {

                uiState.copy(
                    tapPoint = null,
                    isFocusSuccessful = null
                )
            }

            CameraEvent.GalleryProcessingStarted -> {

                uiState.copy(
                    isProcessingGalleryImage = true
                )
            }

            CameraEvent.GalleryProcessingFinished -> {

                uiState.copy(
                    isProcessingGalleryImage = false
                )
            }

            CameraEvent.CaptureStarted -> {

                uiState.copy(
                    isCapturing = true
                )
            }

            CameraEvent.CaptureFinished -> {

                uiState.copy(
                    isCapturing = false
                )
            }
        }
    }

    fun processGalleryImage(
        context: Context,
        galleryUri: Uri,
        onSuccess: (String) -> Unit
    ) {

        onEvent(
            CameraEvent.GalleryProcessingStarted
        )

        viewModelScope.launch(Dispatchers.IO) {

            try {

                val contentResolver =
                    context.contentResolver

                val mimeType =
                    contentResolver.getType(galleryUri)

                val extension =
                    MimeTypeMap
                        .getSingleton()
                        .getExtensionFromMimeType(mimeType)
                        ?: "jpg"

                val file =
                    File.createTempFile(
                        "gallery_raw_",
                        ".$extension",
                        context.cacheDir
                    )

                contentResolver
                    .openInputStream(galleryUri)
                    ?.use { input ->

                        file.outputStream().use { output ->

                            input.copyTo(output)
                        }
                    }

                withContext(Dispatchers.Main) {

                    onSuccess(
                        Uri.fromFile(file).toString()
                    )

                    onEvent(
                        CameraEvent.GalleryProcessingFinished
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "CameraViewModel",
                    "Failed to process gallery image",
                    e
                )

                withContext(Dispatchers.Main) {

                    onEvent(
                        CameraEvent.GalleryProcessingFinished
                    )
                }
            }
        }
    }
}