package com.example.siliguide.presentation.features.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.example.siliguide.core.ui.theme.*

@Composable
fun WelcomeScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onGuestClick: () -> Unit
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
            val iconSize      = (logoSize * 0.54f).coerceIn(32.dp, 52.dp)
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
                            // Logo circle
                            Box(
                                modifier = Modifier
                                    .size(logoSize)
                                    .background(GreenBgLight, CircleShape)
                                    .border(1.5.dp, GreenLight, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                SiliLeafIcon(modifier = Modifier.size(iconSize))
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
                                    "DECISION SUPPORT SYSTEM",
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

                            // Login — filled
                            Button(
                                onClick = onLoginClick,
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
                                Text(
                                    "Login",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Sign Up — outlined
                            OutlinedButton(
                                onClick = onSignUpClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(btnHeight),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, GreenPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = White,
                                    contentColor = GreenPrimary
                                )
                            ) {
                                Text(
                                    "Sign Up",
                                    style = MaterialTheme.typography.titleSmall
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

// ── Leaf icon ─────────────────────────────────────────────────────────────────
@Composable
private fun SiliLeafIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f

        // Leaf body
        val leafPath = Path().apply {
            moveTo(cx, h * 0.13f)
            cubicTo(cx + w * 0.40f, h * 0.34f, cx + w * 0.38f, h * 0.68f, cx, h * 0.90f)
            cubicTo(cx - w * 0.38f, h * 0.68f, cx - w * 0.40f, h * 0.34f, cx, h * 0.13f)
            close()
        }
        drawPath(leafPath, GreenPrimary)

        // Center vein
        drawLine(
            color = GreenBgLight,
            start = Offset(cx, h * 0.88f),
            end = Offset(cx, h * 0.38f),
            strokeWidth = w * 0.044f,
            cap = StrokeCap.Round
        )

        // Left vein
        drawLine(
            color = GreenMid,
            start = Offset(cx, h * 0.62f),
            end = Offset(cx - w * 0.28f, h * 0.44f),
            strokeWidth = w * 0.030f,
            cap = StrokeCap.Round
        )

        // Right vein
        drawLine(
            color = GreenMid,
            start = Offset(cx, h * 0.52f),
            end = Offset(cx + w * 0.28f, h * 0.34f),
            strokeWidth = w * 0.030f,
            cap = StrokeCap.Round
        )

        // Tip dot
        drawCircle(
            color = GreenLight,
            radius = w * 0.054f,
            center = Offset(cx, h * 0.13f)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 740)
@Composable
fun WelcomeScreenPreview() {
    SiliGuideTheme {
        WelcomeScreen(
            onLoginClick = {},
            onSignUpClick = {},
            onGuestClick = {}
        )
    }
}

// Small phone preview
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 320, heightDp = 640)
@Composable
fun WelcomeScreenSmallPreview() {
    SiliGuideTheme {
        WelcomeScreen(
            onLoginClick = {},
            onSignUpClick = {},
            onGuestClick = {}
        )
    }
}

// Large phone preview
@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, widthDp = 412, heightDp = 915)
@Composable
fun WelcomeScreenLargePreview() {
    SiliGuideTheme {
        WelcomeScreen(
            onLoginClick = {},
            onSignUpClick = {},
            onGuestClick = {}
        )
    }
}