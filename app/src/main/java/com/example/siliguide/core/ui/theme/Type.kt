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

    // Body (default UI)
    bodyMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        //color = Black
    ),

    titleMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        //color = Black
    ),

    labelSmall = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        //color = GrayMedium
    ),

    // Big headings (use Fraunces manually when needed)
    headlineSmall = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        //color = Black
    )
)