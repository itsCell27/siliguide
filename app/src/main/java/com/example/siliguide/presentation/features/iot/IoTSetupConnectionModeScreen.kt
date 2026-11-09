package com.example.siliguide.presentation.features.iot

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import com.example.siliguide.R
import com.example.siliguide.core.ui.components.SiliGuideBackButton
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.domain.model.ConnectionMode

// Rebuilt against screen_14_iot_setup_wizard.html, "Step 0 — Connection Mode
// Selection". That mockup ships AP Mode pre-selected as the sensible default
// (see "Step 0 · AP Mode selected (default)" state) rather than requiring an
// explicit first tap, so Continue is always enabled here. Chrome (back
// circle + label + thin progress track) matches the same shell used by the
// AP/LAN wizard steps that follow this screen, just parked at 0%.

@Composable
fun IoTSetupConnectionModeScreen(
    onNavigateBack: () -> Unit,
    onNext: (ConnectionMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(ConnectionMode.AP) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        ModeSelectTopBar(onBack = onNavigateBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 13.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Text(
                text = "How will you connect?",
                style = WizardTitleStyle,
                color = Black
            )

            Text(
                text = "Choose how your phone communicates with your IoT sensor device.",
                style = WizardBodyStyle,
                color = GrayBody
            )

            ConnectionModeCard(
                title = "AP Mode — Direct hotspot",
                description = "Phone connects directly to the device's Wi-Fi hotspot. No home router or internet needed.",
                isSelected = selectedMode == ConnectionMode.AP,
                onClick = { selectedMode = ConnectionMode.AP },
                glyph = {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        drawApGlyph(
                            outerColor = if (selectedMode == ConnectionMode.AP) GreenBgLight else GrayMedium,
                            innerColor = if (selectedMode == ConnectionMode.AP) GreenMid else GrayMedium,
                            dotColor = if (selectedMode == ConnectionMode.AP) GreenMid else GrayMedium,
                            dotCenterColor = if (selectedMode == ConnectionMode.AP) White else OffWhite,
                            strokeWidthPx = 1.5.dp.toPx()
                        )
                    }
                }
            )

            ConnectionModeCard(
                title = "LAN Mode — Home network",
                description = "Device and phone connect to the same Wi-Fi network. Works locally without internet.",
                isSelected = selectedMode == ConnectionMode.LAN,
                onClick = { selectedMode = ConnectionMode.LAN },
                glyph = {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        drawLanGlyph(
                            color = if (selectedMode == ConnectionMode.LAN) GreenBgLight else GrayMedium,
                            strokeWidthPx = 1.4.dp.toPx()
                        )
                    }
                }
            )

            ConnectionModeCard(
                title = "Bluetooth Mode — Direct connection",
                description = "Phone connects directly to the device over Bluetooth. No Wi-Fi or internet needed.",
                isSelected = selectedMode == ConnectionMode.BLUETOOTH,
                onClick = { selectedMode = ConnectionMode.BLUETOOTH },
                glyph = {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        drawBluetoothGlyph(
                            color = if (selectedMode == ConnectionMode.BLUETOOTH) GreenBgLight else GrayMedium,
                            strokeWidthPx = 1.4.dp.toPx()
                        )
                    }
                }
            )

            NotSureBanner()
        }

        PinnedContinueBar(onContinue = { onNext(selectedMode) })
    }
}

// ── Top bar (matches the AP/LAN wizard step chrome, parked at 0%) ─────────

@Composable
private fun ModeSelectTopBar(onBack: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .statusBarsPadding()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            SiliGuideBackButton(
                onClick = onBack,
                size = 30.dp,
                iconSize = 20.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Setup",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = GrayMedium
            )
        }

        // Progress track — Step 0 always renders empty (0%); the wizard
        // steps that follow own the filled portion for their own path.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Border)
        )
    }
}

// ── Mode option card ────────────────────────────────────────────────────

@Composable
private fun ConnectionModeCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    glyph: @Composable () -> Unit
) {
    val borderColor = if (isSelected) GreenPrimary else Border
    val bgColor = if (isSelected) GreenBgLight else White
    val iconBg = if (isSelected) GreenPrimary else OffWhite

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(bgColor)
            .border(if (isSelected) 2.dp else 1.5.dp, borderColor, RoundedCornerShape(13.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.Top
    ) {

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBg)
                .then(if (!isSelected) Modifier.border(1.dp, Border, RoundedCornerShape(10.dp)) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            glyph()
        }

        Column(modifier = Modifier.weight(1f)) {

            Text(
                text = title,
                style = WizardBodyMediumStyle,
                color = if (isSelected) GreenDark else Black
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = description,
                style = WizardCaptionStyle,
                color = if (isSelected) GreenPrimary else GrayMedium
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 3.dp)
                .size(17.dp)
                .border(
                    width = if (isSelected) 2.dp else 1.5.dp,
                    color = if (isSelected) GreenPrimary else GrayLight,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(GreenPrimary)
                )
            }
        }
    }
}

// ── "Not sure?" info banner ─────────────────────────────────────────────

@Composable
private fun NotSureBanner() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Canvas(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(14.dp)
        ) {
            drawInfoGlyph(color = GreenPrimary, strokeWidthPx = 1.2.dp.toPx())
        }

        val text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = Black)) {
                append("Not sure? ")
            }
            append("Use ")
            withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = GreenPrimary)) {
                append("AP Mode")
            }
            append(" or ")
            withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = GreenPrimary)) {
                append("Bluetooth")
            }
            append(" for on-the-go field analysis. Use ")
            withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = GreenPrimary)) {
                append("LAN Mode")
            }
            append(" for a permanent home or greenhouse setup.")
        }

        Text(
            text = text,
            style = WizardCaptionStyle,
            color = GrayBody,
            modifier = Modifier.weight(1f)
        )
    }
}

// ── Pinned Continue bar ─────────────────────────────────────────────────

@Composable
private fun PinnedContinueBar(onContinue: () -> Unit) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .border(width = 1.dp, color = Border)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 8.dp, bottom = 20.dp)
    ) {

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(GreenPrimary)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onContinue
                )
                .padding(vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Continue",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = White
            )
        }
    }
}

// ── Mode glyphs (Canvas — no drawable resources) ────────────────────────

/** Symmetric signal brackets around a center dot — matches the mockup's AP/hotspot icon. */
private fun DrawScope.drawApGlyph(
    outerColor: Color,
    innerColor: Color,
    dotColor: Color,
    dotCenterColor: Color,
    strokeWidthPx: Float
) {
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val outerR = size.width * 0.36f
    val innerR = size.width * 0.18f
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    // Left bracket "(" — outer then inner arc
    drawArc(
        color = outerColor,
        startAngle = 115f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx - outerR, cy - outerR),
        size = Size(outerR * 2, outerR * 2),
        style = stroke
    )
    drawArc(
        color = innerColor,
        startAngle = 115f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx - innerR, cy - innerR),
        size = Size(innerR * 2, innerR * 2),
        style = stroke
    )

    // Right bracket ")" — outer then inner arc
    drawArc(
        color = outerColor,
        startAngle = -65f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx - outerR, cy - outerR),
        size = Size(outerR * 2, outerR * 2),
        style = stroke
    )
    drawArc(
        color = innerColor,
        startAngle = -65f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(cx - innerR, cy - innerR),
        size = Size(innerR * 2, innerR * 2),
        style = stroke
    )

    drawCircle(color = dotColor, radius = size.width * 0.10f, center = Offset(cx, cy))
    drawCircle(color = dotCenterColor, radius = size.width * 0.045f, center = Offset(cx, cy))
}

/** Router body + antenna hump + status dot — matches the mockup's LAN icon. */
private fun DrawScope.drawLanGlyph(color: Color, strokeWidthPx: Float) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = strokeWidthPx)

    val bodyLeft = w * (2f / 22f)
    val bodyTop = h * (6f / 22f)
    val bodyRight = w * (20f / 22f)
    val bodyBottom = h * (17f / 22f)

    drawRoundRect(
        color = color,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyRight - bodyLeft, bodyBottom - bodyTop),
        cornerRadius = CornerRadius(w * (2.5f / 22f), h * (2.5f / 22f)),
        style = stroke
    )

    val humpLeft = w * (7f / 22f)
    val humpTop = h * (4f / 22f)
    val humpRight = w * (15f / 22f)
    val humpBottom = h * (6f / 22f)

    drawRoundRect(
        color = color,
        topLeft = Offset(humpLeft, humpTop),
        size = Size(humpRight - humpLeft, humpBottom - humpTop),
        cornerRadius = CornerRadius(w * (1f / 22f), h * (1f / 22f)),
        style = stroke
    )

    drawCircle(
        color = color,
        radius = w * (2f / 22f),
        center = Offset(w * (11f / 22f), h * (11.5f / 22f))
    )
}

/** Exact zig-zag path of the classic Bluetooth rune, scaled from the mockup's 22x22 viewBox. */
private fun DrawScope.drawBluetoothGlyph(color: Color, strokeWidthPx: Float) {
    val s = size.width / 22f

    fun pt(x: Float, y: Float) = Offset(x * s, y * s)

    val path = Path().apply {
        val p0 = pt(8f, 4f)
        moveTo(p0.x, p0.y)
        val points = listOf(
            pt(14f, 8.5f),
            pt(11f, 11f),
            pt(14f, 13.5f),
            pt(8f, 18f),
            pt(8f, 11.5f),
            pt(5.5f, 13.5f),
            pt(4.5f, 12.5f),
            pt(8f, 10f),
            pt(4.5f, 9.5f),
            pt(5.5f, 8.5f),
            pt(8f, 10.5f)
        )
        points.forEach { lineTo(it.x, it.y) }
        close()
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawInfoGlyph(color: Color, strokeWidthPx: Float) {
    val w = size.width
    val h = size.height

    drawCircle(color = color, radius = w * 0.42f, style = Stroke(width = strokeWidthPx))
    drawLine(
        color = color,
        start = Offset(w * 0.5f, h * 0.30f),
        end = Offset(w * 0.5f, h * 0.53f),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )
    drawCircle(color = color, radius = w * 0.055f, center = Offset(w * 0.5f, h * 0.69f))
}

// ── Previews ─────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true, widthDp = 360, heightDp = 740, name = "IoT Mode Select - 360x740")
@Composable
private fun IoTSetupConnectionModeScreenPreview360() {
    SiliGuideTheme {
        IoTSetupConnectionModeScreen(onNext = {}, onNavigateBack = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 320, heightDp = 640, name = "IoT Mode Select - 320x640")
@Composable
private fun IoTSetupConnectionModeScreenPreview320() {
    SiliGuideTheme {
        IoTSetupConnectionModeScreen(onNext = {}, onNavigateBack = {})
    }
}

@Preview(showBackground = true, showSystemUi = true, widthDp = 412, heightDp = 915, name = "IoT Mode Select - 412x915")
@Composable
private fun IoTSetupConnectionModeScreenPreview412() {
    SiliGuideTheme {
        IoTSetupConnectionModeScreen(onNext = {}, onNavigateBack = {})
    }
}