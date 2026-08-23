package com.example.siliguide.presentation.features.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.*

// NOTE (breaking change vs old mockup):
// Old buttons: Login (filled) + Sign Up (outlined) + Continue as Guest (ghost)
// New mockup:  Continue with Email (filled) + Continue with Google (outlined) + Continue as Guest (ghost)
// Callback names changed below. Update NavGraph call site to match.
@Composable
fun WelcomeScreen(
    onContinueWithEmailClick: () -> Unit,
    onContinueWithGoogleClick: () -> Unit,
    onGuestClick: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val logoAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "logoAlpha"
    )
    val logoSlide by animateFloatAsState(
        targetValue = if (visible) 0f else 40f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "logoSlide"
    )
    val sheetAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, delayMillis = 150),
        label = "sheetAlpha"
    )
    val sheetSlide by animateFloatAsState(
        targetValue = if (visible) 0f else 80f,
        animationSpec = tween(700, delayMillis = 150, easing = EaseOutCubic),
        label = "sheetSlide"
    )

    SiliGuideTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenW = maxWidth
            val screenH = maxHeight

            // ── Adaptive sizing ───────────────────────────────────────────────
            val logoSize      = (screenW * 0.22f).coerceIn(60.dp, 96.dp)
            val logoImgSize   = (logoSize * 0.72f).coerceIn(40.dp, 68.dp)
            val glowSize      = (screenW * 0.72f).coerceIn(180.dp, 300.dp)
            val titleFontSize = (screenW.value * 0.09f).coerceIn(26f, 36f).sp
            val spacerLg      = if (screenH < 680.dp) 10.dp else 18.dp
            val spacerSm      = if (screenH < 680.dp) 4.dp  else 5.dp
            val spacerBadge   = if (screenH < 680.dp) 8.dp  else 14.dp
            val sheetTopPad   = if (screenH < 680.dp) 18.dp else 24.dp
            val sheetBotPad   = if (screenH < 680.dp) 20.dp else 36.dp
            val btnHeight     = if (screenH < 680.dp) 46.dp else 50.dp

            Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {

                    // ── Hero ─────────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        // Background glow
                        Canvas(modifier = Modifier.size(glowSize)) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        GreenBgLight.copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                )
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.graphicsLayer {
                                alpha = logoAlpha
                                translationY = logoSlide
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(logoSize)
                                    .background(GreenBgLight, CircleShape)
                                    .border(1.5.dp, GreenLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.siliguide_logo1),
                                    contentDescription = "SiliGuide logo",
                                    modifier = Modifier.size(logoImgSize),
                                    contentScale = ContentScale.Fit
                                )
                            }

                            Spacer(Modifier.height(spacerLg))

                            // App title
                            Text(
                                buildAnnotatedString {
                                    withStyle(SpanStyle(color = Black)) { append("Sili") }
                                    withStyle(SpanStyle(color = GreenPrimary)) { append("Guide") }
                                },
                                style = MaterialTheme.typography.displayMedium.copy(
                                    fontSize = titleFontSize
                                )
                            )

                            Spacer(Modifier.height(spacerSm))

                            // Tagline
                            Text(
                                "Plant care for Siling Labuyo",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayMedium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(Modifier.height(spacerBadge))

                            // DSS badge
                            Box(
                                modifier = Modifier
                                    .background(GreenBgLight, RoundedCornerShape(20.dp))
                                    .border(1.dp, GreenLight, RoundedCornerShape(20.dp))
                                    .padding(horizontal = 14.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    "Decision Support System",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = GreenDark
                                )
                            }
                        }
                    }

                    // ── Bottom sheet ─────────────────────────────────────────
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                alpha = sheetAlpha
                                translationY = sheetSlide
                            },
                        color = OffWhite,
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        border = BorderStroke(1.dp, Border),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                start = 22.dp,
                                end = 22.dp,
                                top = sheetTopPad,
                                bottom = sheetBotPad
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Section label
                            Text(
                                "GET STARTED",
                                style = MaterialTheme.typography.labelMedium,
                                color = GrayMedium
                            )

                            // Continue with Email — filled
                            Button(
                                onClick = onContinueWithEmailClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(btnHeight),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenPrimary,
                                    contentColor = White
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 0.dp,
                                    pressedElevation = 0.dp
                                )
                            ) {
                                EnvelopeIcon(modifier = Modifier.size(15.dp), color = White)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Continue with Email",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Continue with Google — outlined
                            OutlinedButton(
                                onClick = onContinueWithGoogleClick,
                                enabled = !isLoading,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(btnHeight),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, GreenPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = White,
                                    contentColor = GreenPrimary,
                                    disabledContentColor = GreenPrimary.copy(alpha = 0.5f)
                                )
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = GreenPrimary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    GoogleIcon(modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Continue with Google",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                }
                            }

                            if (errorMessage != null) {
                                Text(
                                    errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Danger,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Divider
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = GrayLight,
                                    thickness = 0.5.dp
                                )
                                Text(
                                    "  or  ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GrayMedium
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    color = GrayLight,
                                    thickness = 0.5.dp
                                )
                            }

                            // Continue as Guest
                            TextButton(
                                onClick = onGuestClick,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = GrayBody
                                )
                            ) {
                                Text(
                                    "Continue as Guest  ›",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Guest disclaimer
                            Text(
                                "No cloud backup · Data lost on uninstall",
                                style = MaterialTheme.typography.bodySmall,
                                color = GrayHint,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Envelope icon (Canvas, no drawable XML per icon convention) ───────────────
@Composable
private fun EnvelopeIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.09f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.06f, h * 0.2f),
            size = Size(w * 0.88f, h * 0.66f),
            cornerRadius = CornerRadius(w * 0.1f, w * 0.1f),
            style = Stroke(width = strokeW)
        )

        val flapPath = Path().apply {
            moveTo(w * 0.11f, h * 0.27f)
            lineTo(w * 0.5f, h * 0.58f)
            lineTo(w * 0.89f, h * 0.27f)
        }
        drawPath(
            path = flapPath,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// ── Google "G" icon (real path data, traced from icons8 SVG) ────────────────
@Composable
fun GoogleIcon(modifier: Modifier = Modifier) {
    val googlePaths = remember {
        listOf(
            "M43.611,20.083H42V20H24v8h11.303c-1.649,4.657-6.08,8-11.303,8c-6.627,0-12-5.373-12-12c0-6.627,5.373-12,12-12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C12.955,4,4,12.955,4,24c0,11.045,8.955,20,20,20c11.045,0,20-8.955,20-20C44,22.659,43.862,21.35,43.611,20.083z" to Color(0xFFFFC107),
            "M6.306,14.691l6.571,4.819C14.655,15.108,18.961,12,24,12c3.059,0,5.842,1.154,7.961,3.039l5.657-5.657C34.046,6.053,29.268,4,24,4C16.318,4,9.656,8.337,6.306,14.691z" to Color(0xFFFF3D00),
            "M24,44c5.166,0,9.86-1.977,13.409-5.192l-6.19-5.238C29.211,35.091,26.715,36,24,36c-5.202,0-9.619-3.317-11.283-7.946l-6.522,5.025C9.505,39.556,16.227,44,24,44z" to Color(0xFF4CAF50),
            "M43.611,20.083H42V20H24v8h11.303c-0.792,2.237-2.231,4.166-4.087,5.571c0.001-0.001,0.002-0.001,0.003-0.002l6.19,5.238C36.971,39.205,44,34,44,24C44,22.659,43.862,21.35,43.611,20.083z" to Color(0xFF1976D2)
        )
    }

    Canvas(modifier = modifier) {
        // SVG viewBox is 48x48 — scale that space to whatever size this
        // Canvas is actually given, anchored at the same top-left origin
        // the path coordinates were written against.
        val scaleFactor = size.width / 48f
        scale(scaleFactor, scaleFactor, pivot = Offset.Zero) {
            googlePaths.forEach { (pathData, color) ->
                drawPath(
                    path = PathParser().parsePathString(pathData).toPath(),
                    color = color
                )
            }
        }
    }
}

@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 740)
@Composable
fun WelcomeScreenPreview() {
    SiliGuideTheme {
        WelcomeScreen(
            onContinueWithEmailClick = {},
            onContinueWithGoogleClick = {},
            onGuestClick = {}
        )
    }
}

// Small phone preview
@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 320, heightDp = 640)
@Composable
fun WelcomeScreenSmallPreview() {
    SiliGuideTheme {
        WelcomeScreen(
            onContinueWithEmailClick = {},
            onContinueWithGoogleClick = {},
            onGuestClick = {}
        )
    }
}

// Large phone preview
@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 412, heightDp = 915)
@Composable
fun WelcomeScreenLargePreview() {
    SiliGuideTheme {
        WelcomeScreen(
            onContinueWithEmailClick = {},
            onContinueWithGoogleClick = {},
            onGuestClick = {}
        )
    }
}