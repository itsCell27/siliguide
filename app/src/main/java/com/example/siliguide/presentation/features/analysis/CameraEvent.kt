package com.example.siliguide.presentation.features.analysis

import androidx.compose.ui.geometry.Offset

sealed interface CameraEvent {

    object FlashClicked : CameraEvent

    data class FocusTapped(
        val point: Offset
    ) : CameraEvent

    data class FocusFinished(
        val success: Boolean
    ) : CameraEvent

    object ClearFocus : CameraEvent

    data class PermissionChanged(
        val granted: Boolean
    ) : CameraEvent

    data object GalleryProcessingStarted : CameraEvent

    data object GalleryProcessingFinished : CameraEvent

    object CaptureStarted : CameraEvent

    object CaptureFinished : CameraEvent
}