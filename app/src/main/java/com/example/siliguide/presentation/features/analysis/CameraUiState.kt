package com.example.siliguide.presentation.features.analysis

import androidx.camera.core.ImageCapture
import androidx.compose.ui.geometry.Offset
import com.example.siliguide.core.permission.PermissionStatus

data class CameraUiState(
    val flashMode: Int = ImageCapture.FLASH_MODE_OFF,

    // Tap to focus state
    val tapPoint: Offset? = null,
    val isFocusSuccessful: Boolean? = null,

    val hasCameraPermission: Boolean = false,

    val isProcessingGalleryImage: Boolean = false,

    val isCapturing: Boolean = false,

    val permissionStatus: PermissionStatus =
        PermissionStatus.RequestPermission,
)