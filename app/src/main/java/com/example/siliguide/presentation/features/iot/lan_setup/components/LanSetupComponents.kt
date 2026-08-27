package com.example.siliguide.presentation.features.iot.lan_setup.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GrayHint
import com.example.siliguide.core.ui.theme.GrayLight
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenLight
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.core.ui.components.SiliGuideBackButton
import com.example.siliguide.core.ui.theme.WizardBodyStyle
import com.example.siliguide.core.ui.theme.WizardCaptionStyle
import com.example.siliguide.core.ui.theme.WizardMicroLabelStyle
import com.example.siliguide.core.ui.theme.WizardSubCaptionStyle
import com.example.siliguide.core.ui.theme.WizardTitleStyle

/* ══════════════════════════════════════════════════════════════════════════
   SCAFFOLD
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Shared chrome for every LAN setup wizard step:
 * back button + flow label + step counter + progress bar, scrollless content
 * area, and a white bottom action bar.
 */
@Composable
fun LanWizardScaffold(
    flowLabel: String,
    stepLabel: String,
    progress: Float,
    onBack: () -> Unit,
    showBack: Boolean = true,
    enableBack: Boolean = true,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment.Horizontal = Alignment.Start,
    contentArrangement: Arrangement.Vertical = Arrangement.spacedBy(13.dp),
    bottomBar: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .background(OffWhite)
    ) {
        // ── Header ──────────────────────────────────────────────────────
        Column(modifier = Modifier.background(White)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (showBack) {
                    SiliGuideBackButton(
                        onClick = onBack,
                        enabled = enableBack,
                        size = 30.dp,
                        iconSize = 16.dp,
                        tint = if (enableBack) GrayBody else GrayHint
                    )

                    Spacer(Modifier.width(10.dp))
                }

                Text(
                    text = flowLabel,
                    style = WizardCaptionStyle.copy(fontWeight = FontWeight.Medium),
                    color = GrayMedium
                )

                Spacer(Modifier.weight(1f))

                Text(
                    text = stepLabel,
                    style = WizardCaptionStyle.copy(fontWeight = FontWeight.Medium),
                    color = GreenDark
                )
            }

            // ── Progress bar ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Border)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(GreenPrimary)
                    )
                }
            }
        }

        // ── Content ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 13.dp, bottom = 8.dp),
            verticalArrangement = contentArrangement,
            horizontalAlignment = contentAlignment,
            content = content
        )

        // ── Bottom action bar ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .drawBehind {
                    drawLine(
                        color = Border,
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = bottomBar
        )
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   BUTTONS
   ══════════════════════════════════════════════════════════════════════════ */

@Composable
fun WizardPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GreenPrimary)
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WizardOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(White)
            .border(1.5.dp, GreenPrimary, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = GreenPrimary,
            textAlign = TextAlign.Center
        )
    }
}

/** Non-interactive "waiting" button — flat green-light fill. */
@Composable
fun WizardWaitingButton(
    text: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GreenLight)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WizardDangerButton(
    text: String,
    onClick: () -> Unit,
    bgColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.5.dp, contentColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun WizardTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = GreenPrimary,
        textAlign = TextAlign.Center,
        textDecoration = TextDecoration.Underline,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    )
}

/* ══════════════════════════════════════════════════════════════════════════
   BLOCKS
   ══════════════════════════════════════════════════════════════════════════ */

/** Round badge holding a hero icon — used on permission / status screens. */
@Composable
fun WizardHeroBadge(
    bgColor: Color,
    borderColor: Color,
    size: Dp = 76.dp,
    borderWidth: Dp = 1.5.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .border(borderWidth, borderColor, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
fun WizardHeading(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = WizardTitleStyle,
        color = Black,
        textAlign = textAlign,
        modifier = modifier
    )
}

@Composable
fun WizardBody(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = WizardBodyStyle,
        color = GrayBody,
        textAlign = textAlign,
        modifier = modifier
    )
}

/** White surface with hairline border — the wizard's generic list card. */
@Composable
fun WizardCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(cornerRadius)),
        content = content
    )
}

/** Row inside a WizardCard: small green tile + caption. */
@Composable
fun WizardBulletRow(
    text: String,
    showDivider: Boolean,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showDivider) Modifier.drawBehind {
                    drawLine(
                        color = Border,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(GreenBgLight),
            contentAlignment = Alignment.Center
        ) { icon() }

        Text(text = text, style = WizardCaptionStyle, color = GrayBody)
    }
}

/** Numbered instruction row (1 · 2 · 3). */
@Composable
fun WizardNumberedRow(
    number: Int,
    showDivider: Boolean,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (showDivider) Modifier.drawBehind {
                    drawLine(
                        color = Border,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(20.dp)
                .clip(RoundedCornerShape(50))
                .background(GreenBgLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = WizardSubCaptionStyle.copy(fontWeight = FontWeight.Medium),
                color = GreenDark
            )
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

/** Tappable "Open Settings" style row with trailing chevron. */
@Composable
fun WizardActionRow(
    title: String,
    subtitle: String? = null,
    tileColor: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tileColor),
            contentAlignment = Alignment.Center
        ) { icon() }

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Black
            )
            if (subtitle != null) {
                Text(text = subtitle, style = WizardSubCaptionStyle, color = GrayMedium)
            }
        }

        ChevronRightIcon()
    }
}

/** Green gradient status panel — waiting / sending states. */
@Composable
fun WizardStatusPanel(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(GreenBgLight, White)))
            .border(1.dp, GreenLight, RoundedCornerShape(14.dp))
            .padding(15.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(50))
                .background(White)
                .border(2.dp, GreenLight, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) { icon() }

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Black,
            textAlign = TextAlign.Center
        )
        Text(
            text = subtitle,
            style = WizardSubCaptionStyle,
            color = GrayMedium,
            textAlign = TextAlign.Center
        )
    }
}

/** Coloured alert strip with leading circle-info glyph. */
@Composable
fun WizardAlertBanner(
    bgColor: Color,
    borderColor: Color,
    accentColor: Color,
    cornerRadius: Dp = 12.dp,
    content: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoCircleIcon(color = accentColor)
        Box(Modifier.weight(1f)) { content() }
    }
}

/** Small pill chip, e.g. "Connected" or "🏠 LAN Mode · 192.168.1.42". */
@Composable
fun WizardPill(
    text: String,
    modifier: Modifier = Modifier,
    showDot: Boolean = false,
    cornerRadius: Dp = 7.dp
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(GreenBgLight)
            .border(1.dp, GreenLight, RoundedCornerShape(cornerRadius))
            .padding(horizontal = if (showDot) 9.dp else 11.dp, vertical = if (showDot) 3.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (showDot) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GreenPrimary)
            )
        }
        Text(
            text = text,
            fontSize = if (showDot) 10.sp else 11.sp,
            fontWeight = FontWeight.Medium,
            color = GreenDark
        )
    }
}

/** Field label above an input row. */
@Composable
fun WizardFieldLabel(
    text: String,
    optional: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = text,
            style = WizardCaptionStyle.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
            color = GrayBody
        )
        if (optional) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(OffWhite)
                    .border(1.dp, Border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
                Text(text = "Optional", style = WizardSubCaptionStyle, color = GrayHint)
            }
        }
    }
}

/** Inline monospace chip used for SSIDs inside body copy. */
@Composable
fun WizardMonoChip(
    text: String,
    bgColor: Color = GreenBgLight,
    textColor: Color = GreenDark
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = textColor
        )
    }
}

@Composable
fun WizardUppercaseLabel(
    text: String,
    color: Color = GrayMedium
) {
    Text(
        text = text.uppercase(),
        style = WizardMicroLabelStyle,
        color = color
    )
}

/** Horizontal dashed hairline. */
@Composable
fun WizardDashedDivider(color: Color = GreenLight) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f
                    )
                )
            }
    )
}

/** Radio dot used by the setup-method cards. */
@Composable
fun WizardRadioDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .padding(top = 3.dp)
            .size(17.dp)
            .clip(RoundedCornerShape(50))
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = if (selected) GreenPrimary else GrayLight,
                shape = RoundedCornerShape(50)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(RoundedCornerShape(50))
                    .background(GreenPrimary)
            )
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   ICONS — all drawn inline with Compose Canvas (no drawables)
   ══════════════════════════════════════════════════════════════════════════ */

private fun DrawScope.stroke(width: Float) = Stroke(
    width = width,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round
)

@Composable
fun BackChevronIcon(size: Dp = 13.dp, color: Color = GrayBody) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 13f
        val p = Path().apply {
            moveTo(8.5f * k, 10.5f * k)
            lineTo(4.5f * k, 6.5f * k)
            lineTo(8.5f * k, 2.5f * k)
        }
        drawPath(p, color, style = stroke(1.5f * k))
    }
}

@Composable
fun ChevronRightIcon(size: Dp = 12.dp, color: Color = GrayHint) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 12f
        val p = Path().apply {
            moveTo(4.5f * k, 3f * k)
            lineTo(8f * k, 6f * k)
            lineTo(4.5f * k, 9f * k)
        }
        drawPath(p, color, style = stroke(1.3f * k))
    }
}

@Composable
fun ChevronDownIcon(size: Dp = 11.dp, color: Color = GrayHint) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 11f
        val p = Path().apply {
            moveTo(2.5f * k, 4.5f * k)
            lineTo(5.5f * k, 7.5f * k)
            lineTo(8.5f * k, 4.5f * k)
        }
        drawPath(p, color, style = stroke(1.3f * k))
    }
}

/** Filled map pin with white centre dot. */
@Composable
fun LocationPinFilledIcon(
    size: Dp = 42.dp,
    color: Color = GreenPrimary,
    dotColor: Color = White
) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 42f
        val p = Path().apply {
            moveTo(21f * k, 3f * k)
            cubicTo(13f * k, 3f * k, 7f * k, 9f * k, 7f * k, 17f * k)
            cubicTo(7f * k, 27f * k, 21f * k, 40f * k, 21f * k, 40f * k)
            cubicTo(21f * k, 40f * k, 35f * k, 27f * k, 35f * k, 17f * k)
            cubicTo(35f * k, 9f * k, 29f * k, 3f * k, 21f * k, 3f * k)
            close()
        }
        drawPath(p, color)
        drawCircle(dotColor, radius = 5f * k, center = Offset(21f * k, 17f * k))
    }
}

/** Outlined map pin crossed out — permission denied. */
@Composable
fun LocationPinDeniedIcon(size: Dp = 42.dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 42f
        val p = Path().apply {
            moveTo(21f * k, 3f * k)
            cubicTo(13f * k, 3f * k, 7f * k, 9f * k, 7f * k, 17f * k)
            cubicTo(7f * k, 27f * k, 21f * k, 40f * k, 21f * k, 40f * k)
            cubicTo(21f * k, 40f * k, 35f * k, 27f * k, 35f * k, 17f * k)
            cubicTo(35f * k, 9f * k, 29f * k, 3f * k, 21f * k, 3f * k)
            close()
        }
        drawPath(p, color, style = Stroke(width = 2f * k))
        drawLine(color, Offset(16f * k, 12f * k), Offset(26f * k, 22f * k), 2.2f * k, StrokeCap.Round)
        drawLine(color, Offset(26f * k, 12f * k), Offset(16f * k, 22f * k), 2.2f * k, StrokeCap.Round)
    }
}

/** Small filled pin for list rows. */
@Composable
fun PinSmallIcon(size: Dp = 11.dp, color: Color = GreenPrimary) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 11f
        val p = Path().apply {
            moveTo(5.5f * k, 1f * k)
            cubicTo(3.5f * k, 1f * k, 2f * k, 2.5f * k, 2f * k, 4.5f * k)
            cubicTo(2f * k, 7f * k, 5.5f * k, 10f * k, 5.5f * k, 10f * k)
            cubicTo(5.5f * k, 10f * k, 9f * k, 7f * k, 9f * k, 4.5f * k)
            cubicTo(9f * k, 2.5f * k, 7.5f * k, 1f * k, 5.5f * k, 1f * k)
            close()
        }
        drawPath(p, color)
    }
}

/** Medium filled pin used in the "waiting for hotspot" panel. */
@Composable
fun LocationPinMediumIcon(size: Dp = 24.dp, color: Color = GreenPrimary) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 24f
        val p = Path().apply {
            moveTo(12f * k, 20f * k)
            cubicTo(12f * k, 20f * k, 6f * k, 15.5f * k, 6f * k, 10.5f * k)
            cubicTo(6f * k, 7.5f * k, 8.7f * k, 5f * k, 12f * k, 5f * k)
            cubicTo(15.3f * k, 5f * k, 18f * k, 7.5f * k, 18f * k, 10.5f * k)
            cubicTo(18f * k, 15.5f * k, 12f * k, 20f * k, 12f * k, 20f * k)
            close()
        }
        drawPath(p, color)
        drawCircle(White, radius = 2.2f * k, center = Offset(12f * k, 11f * k))
    }
}

/** Tiny single-arc wifi glyph (network discovery bullet). */
@Composable
fun WifiArcSmallIcon(size: Dp = 11.dp, color: Color = GreenPrimary) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 11f
        val p = Path().apply {
            moveTo(2f * k, 7f * k)
            quadraticTo(5.5f * k, 4f * k, 9f * k, 7f * k)
        }
        drawPath(p, color, style = stroke(1.4f * k))
        drawCircle(color, radius = 0.9f * k, center = Offset(5.5f * k, 8.5f * k))
    }
}

/** Two-arc wifi glyph used in the network list. */
@Composable
fun WifiListIcon(
    width: Dp = 15.dp,
    color: Color = GreenPrimary,
    strong: Boolean = true,
    alpha: Float = 1f
) {
    Canvas(
        Modifier
            .width(width)
            .height(width * 11f / 15f)
    ) {
        val k = this.size.width / 15f
        if (strong) {
            val a1 = Path().apply {
                moveTo(1.5f * k, 4.5f * k)
                quadraticTo(7.5f * k, 0f, 13.5f * k, 4.5f * k)
            }
            drawPath(a1, color, alpha = alpha, style = stroke(1.4f * k))
            val a2 = Path().apply {
                moveTo(3.5f * k, 7f * k)
                quadraticTo(7.5f * k, 3.5f * k, 11.5f * k, 7f * k)
            }
            drawPath(a2, color, alpha = alpha, style = stroke(1.4f * k))
        } else {
            val a1 = Path().apply {
                moveTo(1.5f * k, 4.5f * k)
                quadraticTo(7.5f * k, 0f, 13.5f * k, 4.5f * k)
            }
            drawPath(a1, color, alpha = alpha, style = stroke(1.4f * k))
        }
        drawCircle(color, radius = 1.3f * k, center = Offset(7.5f * k, 9.5f * k), alpha = alpha)
    }
}

/** Wifi glyph with only the dot — weakest signal. */
@Composable
fun WifiDotOnlyIcon(width: Dp = 15.dp, color: Color = GrayMedium) {
    Canvas(
        Modifier
            .width(width)
            .height(width * 11f / 15f)
    ) {
        val k = this.size.width / 15f
        drawCircle(color, radius = 1.3f * k, center = Offset(7.5f * k, 9.5f * k), alpha = 0.3f)
    }
}

/** Concentric wifi glyph for the "Open Wi-Fi Settings" tile. */
@Composable
fun WifiSettingsIcon(size: Dp = 14.dp, color: Color = GreenPrimary) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 14f
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(2f * k, 0f),
            size = Size(10f * k, 10f * k),
            style = stroke(1.3f * k)
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(4f * k, 4.5f * k),
            size = Size(6f * k, 6f * k),
            style = stroke(1.3f * k)
        )
        drawCircle(color, radius = 1.3f * k, center = Offset(7f * k, 10f * k))
    }
}

/** Large broadcasting wifi glyph — "Sending Wi-Fi credentials…". */
@Composable
fun WifiBroadcastIcon(size: Dp = 24.dp, color: Color = GreenPrimary) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 24f
        val a1 = Path().apply {
            moveTo(3f * k, 10f * k)
            quadraticTo(13f * k, 3f * k, 23f * k, 10f * k)
        }
        drawPath(a1, color, style = stroke(2f * k))
        val a2 = Path().apply {
            moveTo(6f * k, 14.5f * k)
            quadraticTo(13f * k, 9f * k, 20f * k, 14.5f * k)
        }
        drawPath(a2, color, style = stroke(2f * k))
        drawCircle(color, radius = 2f * k, center = Offset(13f * k, 19f * k))
    }
}

@Composable
fun RefreshIcon(size: Dp = 13.dp, color: Color = GreenPrimary) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 13f
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 315f,
            useCenter = false,
            topLeft = Offset(2f * k, 2f * k),
            size = Size(9f * k, 9f * k),
            style = stroke(1.3f * k)
        )
        val head = Path().apply {
            moveTo(11f * k, 1.5f * k)
            lineTo(11f * k, 4.5f * k)
            lineTo(8f * k, 4.5f * k)
        }
        drawPath(head, color, style = stroke(1.3f * k))
    }
}

@Composable
fun CheckCircleIcon(
    size: Dp = 17.dp,
    circleColor: Color = GreenPrimary,
    checkColor: Color = White
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(circleColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * 9f / 17f)) {
            val k = this.size.width / 9f
            val p = Path().apply {
                moveTo(2f * k, 4.5f * k)
                lineTo(3.5f * k, 6f * k)
                lineTo(7f * k, 2.5f * k)
            }
            drawPath(p, checkColor, style = stroke(1.5f * k))
        }
    }
}

@Composable
fun CheckMarkIcon(size: Dp = 12.dp, color: Color = GreenPrimary) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 12f
        val p = Path().apply {
            moveTo(2f * k, 6f * k)
            lineTo(4.5f * k, 8.5f * k)
            lineTo(10f * k, 3f * k)
        }
        drawPath(p, color, style = stroke(1.6f * k))
    }
}

/** Big success check on the final screen. */
@Composable
fun SuccessCheckIcon(size: Dp = 28.dp, color: Color = White) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 28f
        val p = Path().apply {
            moveTo(6f * k, 14.5f * k)
            lineTo(11.5f * k, 20f * k)
            lineTo(22f * k, 9f * k)
        }
        drawPath(p, color, style = stroke(2.2f * k))
    }
}

@Composable
fun LockIcon(size: Dp = 12.dp, color: Color = GreenPrimary) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 12f
        drawRoundRect(
            color = color,
            topLeft = Offset(1.5f * k, 5.5f * k),
            size = Size(9f * k, 6f * k),
            cornerRadius = CornerRadius(1.2f * k),
            style = Stroke(width = 1.2f * k)
        )
        val shackle = Path().apply {
            moveTo(3.5f * k, 5.5f * k)
            lineTo(3.5f * k, 4f * k)
            arcTo(
                rect = Rect(
                    Offset(3.5f * k, 1.5f * k),
                    Size(5f * k, 5f * k)
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(8.5f * k, 5.5f * k)
        }
        drawPath(shackle, color, style = Stroke(width = 1.2f * k))
    }
}

@Composable
fun EyeIcon(width: Dp = 13.dp, color: Color = GrayBody) {
    Canvas(
        Modifier
            .width(width)
            .height(width * 12f / 13f)
    ) {
        val k = this.size.width / 13f
        drawOval(
            color = color,
            topLeft = Offset(1.5f * k, 2.5f * k),
            size = Size(10f * k, 7f * k),
            style = Stroke(width = 1.2f * k)
        )
        drawCircle(color, radius = 1.5f * k, center = Offset(6.5f * k, 6f * k))
    }
}

@Composable
fun InfoCircleIcon(size: Dp = 14.dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 14f
        drawCircle(
            color = color,
            radius = 6f * k,
            center = Offset(7f * k, 7f * k),
            style = Stroke(width = 1.2f * k)
        )
        drawLine(
            color,
            Offset(7f * k, 4f * k),
            Offset(7f * k, 8f * k),
            1.3f * k,
            StrokeCap.Round
        )
        drawCircle(color, radius = 0.7f * k, center = Offset(7f * k, 10.5f * k))
    }
}

@Composable
fun BluetoothGlyphIcon(size: Dp = 14.dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 24f
        val p = Path().apply {
            moveTo(7f * k, 7f * k)
            lineTo(17f * k, 17f * k)
            lineTo(12f * k, 22f * k)
            lineTo(12f * k, 2f * k)
            lineTo(17f * k, 7f * k)
            lineTo(7f * k, 17f * k)
        }
        drawPath(p, color, style = stroke(2f * k))
    }
}

@Composable
fun PhoneOutlineIcon(size: Dp = 16.dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 16f
        drawRoundRect(
            color = color,
            topLeft = Offset(2f * k, 1f * k),
            size = Size(12f * k, 14f * k),
            cornerRadius = CornerRadius(2.5f * k),
            style = Stroke(width = 1.3f * k)
        )
        drawCircle(color, radius = 0.8f * k, center = Offset(8f * k, 12f * k))
    }
}

/** Small device / sensor-box glyph for the device-name field. */
@Composable
fun DeviceChipIcon(size: Dp = 13.dp, color: Color = GreenPrimary) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 13f
        drawRoundRect(
            color = color,
            topLeft = Offset(1.5f * k, 3f * k),
            size = Size(10f * k, 7f * k),
            cornerRadius = CornerRadius(1.8f * k),
            style = Stroke(width = 1.2f * k)
        )
        drawCircle(color, radius = 1.2f * k, center = Offset(6.5f * k, 6.5f * k))
    }
}

/** Leaf / plant glyph for the link-to-plant field. */
@Composable
fun PlantLeafIcon(
    size: Dp = 13.dp,
    color: Color = GreenPrimary,
    veinColor: Color = GreenBgLight
) {
    Canvas(Modifier.size(size)) {
        val k = this.size.width / 13f
        val p = Path().apply {
            moveTo(6.5f * k, 1.5f * k)
            cubicTo(6.5f * k, 1.5f * k, 10.5f * k, 4.2f * k, 10.5f * k, 7.3f * k)
            arcTo(
                rect = Rect(Offset(2.5f * k, 3.3f * k), Size(8f * k, 8f * k)),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            cubicTo(2.5f * k, 4.2f * k, 6.5f * k, 1.5f * k, 6.5f * k, 1.5f * k)
            close()
        }
        drawPath(p, color)
        drawLine(
            veinColor,
            Offset(6.5f * k, 11f * k),
            Offset(6.5f * k, 6f * k),
            1.2f * k,
            StrokeCap.Round
        )
    }
}

/** Three dim dots, phone → device transfer illustration. */
@Composable
fun TransferDots(color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) {
            Box(
                Modifier
                    .size(5.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.5f))
            )
        }
    }
}