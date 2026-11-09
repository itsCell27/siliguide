package com.example.siliguide.presentation.features.analysis.components

import androidx.camera.core.ImageCapture
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.FlashAuto
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.siliguide.core.ui.theme.GreenDark

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
            val boxSide = minOf(size.width, size.height) * 0.90f
            val left = (size.width - boxSide) / 2
            val top = (size.height - boxSide) / 2 - 40.dp.toPx()
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
