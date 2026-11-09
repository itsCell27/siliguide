package com.example.siliguide.core.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Greens
val GreenPrimary = Color(0xFF3B6D11)
val GreenDark = Color(0xFF27500A)
val GreenMid = Color(0xFF97C459)
val GreenLight = Color(0xFFC0DD97)
val GreenBgLight = Color(0xFFEAF3DE)

val LightGreen = Color(0xFF97C459)

// Neutrals
val White = Color(0xFFFFFFFF)
val OffWhite = Color(0xFFF7F7F5)
val Border = Color(0xFFEBEBEB)
val GrayLight = Color(0xFFD8D8D8)
val GrayHint = Color(0xFFB4B2A9)
val GrayMedium = Color(0xFF888780)
val GrayBody = Color(0xFF5F5E5A)
val Black = Color(0xFF1A1A1A)

// Status - Danger (Red)
val Danger = Color(0xFF791F1F)
val DangerBg = Color(0xFFFCEBEB)
val DangerBorder = Color(0xFFE9B7B7)

// Status - Warning (Amber)
val Warning = Color(0xFF633806)
val WarningBg = Color(0xFFFAEEDA)
val WarningBorder = Color(0xFFE9C589)

// Status - Info (Blue)
val Info = Color(0xff2f6fed)
val InfoBg = Color(0xFFE6F0F7)
val InfoBorder = Color(0xffbfd6fb)
val InfoBlue = Color(0xffeaf1fe)

// Status - Success (Green)
val Success = Color(0xFF3B6D11)
val SuccessBg = Color(0xFFEAF3DE)

// Page backgrounds
val PageBg = Color(0xFFF4F3EE)
val PageFrameBg = Color(0xFFF7F7F5)

// Leaf backgrounds
val LeafBgDark = Color(0xFF1F3909)
val LeafBgMedium = Color(0xFF2A3D10)
val LeafBgYellow = Color(0xFF3A4408)

// ── Setup wizard status tokens (IoT provisioning flows) ────────────────────

// Wizard - Warning (amber, softer than global Warning)
val WizardWarningBg = Color(0xFFFEF5E7)
val WizardWarningBorder = Color(0xFFF5CBA7)
val WizardWarningText = Color(0xFF784212)

// Wizard - Error (red, softer than global Danger)
val WizardErrorBg = Color(0xFFFCEBEB)
val WizardErrorBorder = Color(0xFFF7C1C1)
val WizardErrorText = Color(0xFFA32D2D)

// ── Camera viewfinder tokens (screen 08) ───────────────────────────────────

/** Dim outside the framing box (~45% black). */
val CameraScrim = Color(0x73000000)

/** Floating chip / round button surface (~55% black). */
val CameraChipBg = Color(0x8C000000)
val CameraChipBorder = Color(0x1FFFFFFF)

/** Frosted-glass square buttons (gallery / tips). */
val CameraGlassBg = Color(0x1FFFFFFF)
val CameraGlassBorder = Color(0x38FFFFFF)

/** Uppercase hint pill under the box. */
val CameraHintBg = Color(0x59000000)
val CameraHintBorder = Color(0x1AFFFFFF)
val CameraHintText = Color(0xC7FFFFFF)

/** Bottom controls gradient stops. */
val CameraControlsGradientMid = Color(0x8C000000)
val CameraControlsGradientEnd = Color(0xB3000000)

/** Glow ring around the shutter (~15% white). */
val CameraShutterGlow = Color(0x26FFFFFF)

/** Corner bracket colors. */
val CameraBracket = Color(0xFFFFFFFF)
val CameraBracketError = Color(0xFFFF8A8A)

/** Tap-to-focus ring states. */
val CameraFocusRing = Color(0xCCFFFFFF)
val CameraFocusOk = Color(0xFF97C459)
val CameraFocusFail = Color(0xFFFF8A8A)

/** Example thumbnail borders / marks in the guide sheet. */
val ExampleBadBorder = Color(0xFFD96B6B)
val ExampleBadMark = Color(0xFFC9302C)
val ExampleGoodBorder = Color(0xFF97C459)
val ExampleGoodMark = Color(0xFF3B6D11)

/** Sheet grabber. */
val SheetHandle = Color(0xFFD8D8D8)

/** Backdrop behind a camera bottom sheet. */
val CameraSheetBackdrop = Color(0x59000000)

/** Full-screen dim behind the Capturing Tips dialog (~60% black). */
val CameraDialogBackdrop = Color(0x99000000)

/** Glass square button while its dialog is open (~22% white). */
val CameraGlassBgActive = Color(0x38FFFFFF)

// ── Crop / Adjust tokens (screen 09) ───────────────────────────────────────

/** Dim outside the crop box (~55% black). */
val CropScrim = Color(0x8C000000)

/** Gesture hint pill. */
val CropHintBg = Color(0x66000000)
val CropHintText = Color(0xD9FFFFFF)

/** Rule-of-thirds grid + slider/step track (~18% white). */
val CropGridLine = Color(0x2EFFFFFF)

/** Pipeline loading overlay (~66% black). */
val CropLoadingScrim = Color(0xA8000000)

/** Pipeline step states. */
val CropStepDone = Color(0xFF97C459)
val CropStepActive = Color(0xFFFFFFFF)
val CropStepPending = Color(0x80FFFFFF)

/** Scrim behind centered dialogs (~45% black). */
val ModalScrim = Color(0x73000000)