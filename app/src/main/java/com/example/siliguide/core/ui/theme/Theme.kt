package com.example.siliguide.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(

    primary = GreenPrimary,
    onPrimary = White,

    background = White,
    onBackground = Black,

    surface = White,
    onSurface = Black,

    surfaceVariant = OffWhite,
    onSurfaceVariant = GrayBody,

    outline = Border
)

@Composable
fun SiliGuideTheme(
    darkTheme: Boolean = false, // FORCE LIGHT MODE
    content: @Composable () -> Unit
) {

    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}