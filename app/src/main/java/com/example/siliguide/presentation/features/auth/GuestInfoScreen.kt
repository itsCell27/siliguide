package com.example.siliguide.presentation.features.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import com.example.siliguide.core.ui.theme.*

// ── Data models ───────────────────────────────────────────────────────────────

private data class InfoRow(
    val label: String,
    val desc: String,
    val isPositive: Boolean
)

private val limitations = listOf(
    InfoRow("No cloud backup",        "All data stored on this device only",                   false),
    InfoRow("Data lost on uninstall", "Plants and analysis records deleted permanently",       false),
    InfoRow("No multi-device access", "Cannot access data from another device",                false),
//    InfoRow("No account migration",   "Guest data cannot be transferred to a new account",    false),
)

private val canDo = listOf(
    InfoRow("Full plant analysis — offline", "Camera, ML models and rule engine work without internet", true),
    InfoRow("IoT device connection",          "Connect your ESP32 sensor for live readings",            true),
)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun GuestInfoScreen(
    onContinueAsGuest: () -> Unit,
    onCreateAccount: () -> Unit
) {
    SiliGuideTheme {
        // Scaffold handles status bar insets at top automatically.
        // We pass WindowInsets.safeDrawing so Scaffold respects
        // both status bar (top) and navigation bar (bottom).
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    // Consume scaffold inner padding so no double-padding
                    .consumeWindowInsets(innerPadding)
                    .padding(innerPadding)
            ) {
                val screenH = maxHeight
                val screenW = maxWidth

                // ── Adaptive sizing ───────────────────────────────────────
                val iconCircleSize  = (screenW * 0.17f).coerceIn(52.dp, 72.dp)
                val iconSize        = (iconCircleSize * 0.52f).coerceIn(26.dp, 38.dp)
                val headingFontSize = (screenW.value * 0.055f).coerceIn(17f, 22f).sp
                val rowPadV         = if (screenH < 680.dp) 7.dp  else 9.dp
                val rowPadH         = if (screenH < 680.dp) 10.dp else 12.dp
                val iconWrapSize    = if (screenH < 680.dp) 24.dp else 26.dp
                val sectionGap      = if (screenH < 680.dp) 6.dp  else 8.dp
                val btnHeight       = if (screenH < 680.dp) 46.dp else 50.dp
                val contentPadH     = if (screenW < 360.dp) 18.dp else 22.dp

                Column(modifier = Modifier.fillMaxSize()) {

                    // ── Scrollable content ────────────────────────────────
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(
                                horizontal = contentPadH,
                                vertical   = if (screenH < 680.dp) 12.dp else 16.dp
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Guest icon
                        Box(
                            modifier = Modifier
                                .size(iconCircleSize)
                                .background(OffWhite, CircleShape)
                                .border(1.5.dp, Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            GuestPersonIcon(modifier = Modifier.size(iconSize))
                        }

                        Spacer(Modifier.height(if (screenH < 680.dp) 10.dp else 14.dp))

                        // Heading
                        Text(
                            "Using SiliGuide as a Guest",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = headingFontSize
                            ),
                            color = Black,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(4.dp))

                        // Subtitle
                        Text(
                            "Your data will only be stored on this device\nand will not be backed up.",
                            style = MaterialTheme.typography.bodySmall,
                            color = GrayMedium,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(Modifier.height(if (screenH < 680.dp) 12.dp else 16.dp))

                        // ── Limitations card ──────────────────────────────
                        SectionTag("Limitations")
                        Spacer(Modifier.height(sectionGap))
                        InfoCard(
                            rows        = limitations,
                            rowPadV     = rowPadV,
                            rowPadH     = rowPadH,
                            iconWrapSize = iconWrapSize
                        )

                        Spacer(Modifier.height(if (screenH < 680.dp) 8.dp else 10.dp))

                        // ── Can-do card ───────────────────────────────────
                        SectionTag("What you can still do")
                        Spacer(Modifier.height(sectionGap))
                        InfoCard(
                            rows        = canDo,
                            rowPadV     = rowPadV,
                            rowPadH     = rowPadH,
                            iconWrapSize = iconWrapSize
                        )
                    }

                    // ── Pinned buttons — always visible ───────────────────
                    // navigationBarsPadding() adds space above phone gesture bar
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding(),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, Border),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(
                                horizontal = contentPadH,
                                vertical   = if (screenH < 680.dp) 10.dp else 14.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(9.dp)
                        ) {
                            // Continue as Guest — filled
                            Button(
                                onClick = onContinueAsGuest,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(btnHeight),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GreenPrimary,
                                    contentColor   = White
                                ),
                                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
                            ) {
                                Text(
                                    "Continue as Guest",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }

                            // Create account — outlined
                            OutlinedButton(
                                onClick = onCreateAccount,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(btnHeight),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.5.dp, GreenPrimary),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = White,
                                    contentColor   = GreenPrimary
                                )
                            ) {
                                Text(
                                    "Create an Account",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun SectionTag(text: String) {
    Text(
        text.uppercase(),
        style    = MaterialTheme.typography.labelMedium,
        color    = GrayMedium,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun InfoCard(
    rows: List<InfoRow>,
    rowPadV: Dp,
    rowPadH: Dp,
    iconWrapSize: Dp
) {
    Surface(
        modifier      = Modifier.fillMaxWidth(),
        shape         = RoundedCornerShape(12.dp),
        color         = OffWhite,
        border        = BorderStroke(1.dp, Border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column {
            rows.forEachIndexed { index, row ->
                if (index > 0) {
                    HorizontalDivider(color = Border, thickness = 1.dp)
                }
                InfoRowItem(
                    row          = row,
                    rowPadV      = rowPadV,
                    rowPadH      = rowPadH,
                    iconWrapSize = iconWrapSize
                )
            }
        }
    }
}

@Composable
private fun InfoRowItem(
    row: InfoRow,
    rowPadV: Dp,
    rowPadH: Dp,
    iconWrapSize: Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = rowPadH, vertical = rowPadV),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Icon badge
        Box(
            modifier = Modifier
                .size(iconWrapSize)
                .background(
                    if (row.isPositive) GreenBgLight else DangerBg,
                    RoundedCornerShape(7.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (row.isPositive) CheckIcon(size = iconWrapSize * 0.52f)
            else CrossIcon(size = iconWrapSize * 0.52f)
        }

        // Text
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                row.label,
                style    = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize   = 12.sp
                ),
                color    = if (row.isPositive) GreenDark else Black
            )
            Text(
                row.desc,
                style    = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color    = GrayMedium,
                lineHeight = 14.sp
            )
        }
    }
}

// ── Canvas icons ──────────────────────────────────────────────────────────────

@Composable
private fun GuestPersonIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w  = size.width
        val h  = size.height
        val sw = w * 0.048f
        val pc = GrayBody

        // Head circle
        drawCircle(
            color  = pc,
            radius = w * 0.18f,
            center = Offset(w * 0.5f, h * 0.35f),
            style  = Stroke(width = sw)
        )
        // Shoulder arc
        val shoulderPath = Path().apply {
            moveTo(w * 0.10f, h * 0.92f)
            cubicTo(w * 0.10f, h * 0.68f, w * 0.90f, h * 0.68f, w * 0.90f, h * 0.92f)
        }
        drawPath(shoulderPath, color = pc, style = Stroke(width = sw, cap = StrokeCap.Round))

        // Plus badge (top-right) — circle
        val bCx = w * 0.80f
        val bCy = h * 0.26f
        val bR  = w * 0.162f
        drawCircle(color = OffWhite, radius = bR, center = Offset(bCx, bCy))
        drawCircle(
            color  = GrayLight,
            radius = bR,
            center = Offset(bCx, bCy),
            style  = Stroke(width = w * 0.038f)
        )
        val arm = bR * 0.55f
        // horizontal
        drawLine(GrayMedium, Offset(bCx - arm, bCy), Offset(bCx + arm, bCy),
            strokeWidth = w * 0.044f, cap = StrokeCap.Round)
        // vertical
        drawLine(GrayMedium, Offset(bCx, bCy - arm), Offset(bCx, bCy + arm),
            strokeWidth = w * 0.044f, cap = StrokeCap.Round)
    }
}

@Composable
private fun CheckIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w  = size.toPx()
        val sw = w * 0.12f
        val path = Path().apply {
            moveTo(w * 0.12f, w * 0.52f)
            lineTo(w * 0.42f, w * 0.82f)
            lineTo(w * 0.88f, w * 0.22f)
        }
        drawPath(path, color = GreenPrimary,
            style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun CrossIcon(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w  = size.toPx()
        val sw = w * 0.12f
        val col = Danger
        drawLine(col, Offset(w * 0.22f, w * 0.22f), Offset(w * 0.78f, w * 0.78f),
            strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(col, Offset(w * 0.78f, w * 0.22f), Offset(w * 0.22f, w * 0.78f),
            strokeWidth = sw, cap = StrokeCap.Round)
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF,
    widthDp = 360, heightDp = 740, showSystemUi = true)
@Composable
fun GuestInfoPreview() {
    SiliGuideTheme {
        GuestInfoScreen(onContinueAsGuest = {}, onCreateAccount = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF,
    widthDp = 320, heightDp = 640, showSystemUi = true)
@Composable
fun GuestInfoSmallPreview() {
    SiliGuideTheme {
        GuestInfoScreen(onContinueAsGuest = {}, onCreateAccount = {})
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF,
    widthDp = 412, heightDp = 915, showSystemUi = true)
@Composable
fun GuestInfoLargePreview() {
    SiliGuideTheme {
        GuestInfoScreen(onContinueAsGuest = {}, onCreateAccount = {})
    }
}