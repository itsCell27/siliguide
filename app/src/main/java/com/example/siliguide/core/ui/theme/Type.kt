package com.example.siliguide.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.*
import com.example.siliguide.R

// DM Sans (main UI font)
val DMSans = FontFamily(
    Font(R.font.dmsans_regular, FontWeight.Normal),
    Font(R.font.dmsans_medium, FontWeight.Medium),
    Font(R.font.dmsans_bold, FontWeight.Bold)
)

// Fraunces (display - serif)
val Fraunces = FontFamily(
    Font(R.font.fraunces_bold, FontWeight.Bold)
)

// ── Custom standalone text styles (not in M3 Typography slots) ──────────────

/** Uppercase section label — "IOT DEVICES", "MY PLANTS" */
val SectionLabelStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.5.sp
)

/** Small "See all" / "Add one" link text */
val SeeAllStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.sp
)

/** Bottom nav label */
val NavLabelStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp,
    letterSpacing = 0.sp
)

/** Active bottom nav label */
val NavLabelActiveStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    letterSpacing = 0.sp
)

// ── Setup wizard text styles ───────────────────────────────────────────────

/** Wizard step heading — 17sp Fraunces */
val WizardTitleStyle = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Bold,
    fontSize = 17.sp,
    letterSpacing = (-0.3).sp
)

/** Wizard success heading — 15sp Fraunces */
val WizardSuccessTitleStyle = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Bold,
    fontSize = 15.sp,
    letterSpacing = (-0.3).sp
)

/** Wizard body copy — 12sp, 1.5 line height */
val WizardBodyStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 18.sp
)

/** Wizard body copy, medium weight — 12sp */
val WizardBodyMediumStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 18.sp
)

/** Wizard list / caption copy — 11sp, 1.5 line height */
val WizardCaptionStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 16.5.sp
)

/** Wizard sub-caption — 10sp */
val WizardSubCaptionStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp
)

/** Wizard uppercase micro label — 10sp */
val WizardMicroLabelStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    letterSpacing = 0.4.sp
)

/** Wizard inline monospace chip — SSID / device id */
val WizardMonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 10.sp
)

// ── Camera viewfinder text styles (screen 08) ──────────────────────────────

/** Floating instruction pill over the viewfinder — 11sp */
val CameraPillStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp
)

/** Uppercase micro hint under the framing box — 10sp */
val CameraHintStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp,
    letterSpacing = 0.4.sp
)

/** Camera bottom-sheet heading — 20sp Fraunces */
val CameraSheetTitleStyle = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    letterSpacing = (-0.3).sp
)

/** Camera bottom-sheet body — 12sp, 1.55 line height */
val CameraSheetBodyStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 18.6.sp
)

/** "How to take a better picture:" heading — 12sp */
val CameraTipHeadingStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp
)

/** Example thumbnail caption — 11sp */
val CameraExampleLabelStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp
)

/** Pill CTA inside camera sheet — 14sp */
val CameraSheetButtonStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp
)

/** Numbered badge inside the Capturing Tips dialog — 11sp bold */
val CameraTipNumberStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Bold,
    fontSize = 11.sp
)

/** Tip body line — 13sp, 1.5 line height */
val CameraTipTextStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 19.5.sp
)

// ── Crop / Adjust text styles (screen 09) ──────────────────────────────────

/** Top bar title — 16sp Fraunces */
val CropTopBarTitleStyle = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    letterSpacing = (-0.2).sp
)

/** "Pinch to zoom · Drag to reposition" — 11sp */
val CropHintStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.2.sp
)

/** Loading headline — 14sp */
val CropLoadingTitleStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp
)

/** Loading sub-line + step rows — 11sp */
val CropLoadingStepStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp
)


// ── IoT device detail styles (screen 12) ───────────────────────────────────

/** Device name in the status hero — 16sp Fraunces */
val DeviceNameStyle = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    letterSpacing = (-0.2).sp
)

/** Centered dialog title — 16sp Fraunces */
val DialogTitleStyle = TextStyle(
    fontFamily = Fraunces,
    fontWeight = FontWeight.Bold,
    fontSize = 16.sp,
    letterSpacing = (-0.2).sp
)

/** Mode badges, pills, last-seen meta — 10sp */
val BadgeLabelStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 10.sp
)

/** Sensor cell name — 8.5sp uppercase */
val SensorCellLabelStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 8.5.sp,
    letterSpacing = 0.3.sp
)

/** Sensor cell value — 11.5sp */
val SensorCellValueStyle = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 11.5.sp
)

/** Monospace info value — MAC, firmware */
val InfoValueMonoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp
)


val Typography = Typography(

    // Large headings - Fraunces serif
    displaySmall = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = (-0.3).sp
    ),

    displayMedium = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        letterSpacing = (-0.5).sp,
        lineHeight = 34.sp
    ),

    headlineLarge = TextStyle(
        fontFamily = Fraunces,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = (-0.3).sp
    ),

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

    // Body text
    titleMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp
    ),

    titleSmall = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),

    bodyMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),

    bodySmall = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp
    ),

    labelSmall = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.2.sp
    ),

    labelMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    )
)