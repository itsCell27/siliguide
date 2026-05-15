package com.example.siliguide.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.sp
import com.example.siliguide.R

// Set of Material typography styles to start with
// DM Sans (main UI font)
val DMSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_bold, FontWeight.Bold)
)

// Fraunces (only for display)
val Fraunces = FontFamily(
    Font(R.font.fraunces_bold, FontWeight.Bold)
)

val Typography = Typography(

    // Used for "Add New Plant" top bar and "What's next?" sheet title
    headlineSmall = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        letterSpacing = (-0.2).sp
    ),
    // Standard UI fonts
    titleMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp
    )
)