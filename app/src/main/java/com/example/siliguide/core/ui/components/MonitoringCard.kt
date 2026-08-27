package com.example.siliguide.core.ui.components.monitoring

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.DMSans
import com.example.siliguide.core.ui.theme.DangerBg
import com.example.siliguide.core.ui.theme.Danger
import com.example.siliguide.core.ui.theme.BadgeLabelStyle
import com.example.siliguide.core.ui.theme.DeviceNameStyle
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GrayHint
import com.example.siliguide.core.ui.theme.GrayLight
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenLight
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.InfoBg
import com.example.siliguide.core.ui.theme.Info
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.SensorCellLabelStyle
import com.example.siliguide.core.ui.theme.SensorCellValueStyle
import com.example.siliguide.core.ui.theme.Warning
import com.example.siliguide.core.ui.theme.WarningBg
import com.example.siliguide.core.ui.theme.WarningBorder
import com.example.siliguide.core.ui.theme.WizardCaptionStyle
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.domain.engine.ThresholdLevel
import com.example.siliguide.domain.model.ConnectionMode

/**
 * Pulled out of IoTDeviceDetailScreen's old StatusCard so any monitoring
 * surface (IoT device detail, a future plant-only view, etc.) can render the
 * same "connected / offline / reconnecting" hero card + live readings grid
 * without copy-pasting the layout. Caller owns the icon glyph and the badge
 * next to the title (e.g. a connection-mode chip) — this component only
 * owns the status shell and the readings grid.
 */
enum class MonitoringStatus { CONNECTED, OFFLINE, RECONNECTING, NEEDS_REPAIRING }

data class MonitoringReading(
    val label: String,
    val value: String,
    val level: ThresholdLevel = ThresholdLevel.UNKNOWN
)

@Composable
fun MonitoringCard(
    title: String,
    status: MonitoringStatus,
    readings: List<MonitoringReading>,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    badge: (@Composable () -> Unit)? = null,
    lastSeenText: String? = null,
    staleNote: String? = null,
    offlineNote: String = "Device not reachable. Make sure it is powered on and your phone is on the same network.",
    onReconnect: () -> Unit = {},
    onRepair: (() -> Unit)? = null,
    connectionMode: ConnectionMode
) {

    val isOffline = status == MonitoringStatus.OFFLINE || status == MonitoringStatus.NEEDS_REPAIRING

    val bg = if (isOffline) WarningBg else GreenBgLight
    val bd = if (isOffline) WarningBorder else GreenLight

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(width = 1.dp, color = bd, shape = RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(White)
                    .border(width = 1.dp, color = bd, shape = RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {

                icon()
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = title,
                    style = DeviceNameStyle,
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (badge != null) {

                    Spacer(modifier = Modifier.height(3.dp))
                    badge()
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            when (status) {
                MonitoringStatus.CONNECTED -> ConnectedPill()
                MonitoringStatus.OFFLINE -> OfflinePill()
                MonitoringStatus.NEEDS_REPAIRING -> NeedsRepairPill()
                MonitoringStatus.RECONNECTING -> Unit
            }
        }

        if (isOffline && lastSeenText != null) {

            Text(
                text = "⏱ $lastSeenText",
                style = BadgeLabelStyle,
                color = Warning,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val modeOfflineNote = when (connectionMode) {
            ConnectionMode.AP -> "Phone is not connected to the sensor's Wi-Fi hotspot (192.168.4.1)."
            ConnectionMode.LAN -> offlineNote
            ConnectionMode.BLUETOOTH -> "Bluetooth sensor is disconnected or out of range."
        }

        if (status == MonitoringStatus.NEEDS_REPAIRING) {
            MonitoringWarningNote(
                text = "This device may have been reset and is broadcasting a new pairing code. Re-pair it to reconnect.",
                background = White.copy(alpha = 0.6f)
            )
        } else if (isOffline) {
            MonitoringWarningNote(
                text = modeOfflineNote,
                background = White.copy(alpha = 0.6f)
            )
        }

        if (readings.isNotEmpty() && status != MonitoringStatus.RECONNECTING) {

            MonitoringReadingsGrid(readings = readings, isStale = isOffline)
        }

        if (isOffline && staleNote != null) {

            Text(
                text = staleNote,
                style = BadgeLabelStyle,
                color = Warning,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
        }

        when (status) {
            MonitoringStatus.OFFLINE -> MonitoringReconnectButton(onClick = onReconnect)
            MonitoringStatus.NEEDS_REPAIRING -> MonitoringRepairButton(onClick = onRepair ?: onReconnect)
            MonitoringStatus.RECONNECTING -> MonitoringReconnectingButton()
            MonitoringStatus.CONNECTED -> Unit
        }

        if (status == MonitoringStatus.RECONNECTING) {

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = GreenPrimary,
                trackColor = GreenLight
            )
        }
    }
}

// ── Threshold chip ───────────────────────────────────────────────────────

@Composable
fun ThresholdChip(level: ThresholdLevel, isStale: Boolean = false) {
    val (bg, fg, label) = when {
        isStale -> Triple(OffWhite, GrayHint, "Stale")
        level == ThresholdLevel.NORMAL -> Triple(GreenBgLight, GreenDark, "NORMAL")
        level == ThresholdLevel.LOW -> Triple(InfoBg, Info, "LOW")
        level == ThresholdLevel.HIGH -> Triple(DangerBg, Danger, "HIGH")
        else -> Triple(OffWhite, GrayHint, "—")
    }
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(4.dp))
            .then(
                if (isStale) Modifier.border(
                    1.dp,
                    Border,
                    RoundedCornerShape(4.dp)
                ) else Modifier
            )
            .padding(horizontal = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color = fg,
            letterSpacing = 0.3.sp
        )
    }
}

// ── Internal pieces ───────────────────────────────────────────────────────

@Composable
private fun ConnectedPill() {

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GreenPrimary)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(White)
        )

        Spacer(modifier = Modifier.width(5.dp))

        Text(
            text = "Connected",
            style = BadgeLabelStyle,
            color = White
        )
    }
}

@Composable
private fun OfflinePill() {

    Text(
        text = "Offline",
        style = BadgeLabelStyle,
        color = GrayBody,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GrayLight)
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun NeedsRepairPill() {

    Text(
        text = "Needs Re-pairing",
        style = BadgeLabelStyle,
        color = Warning,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(White)
            .border(1.dp, WarningBorder, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
private fun MonitoringReadingsGrid(
    readings: List<MonitoringReading>,
    isStale: Boolean
) {

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        readings.chunked(3).forEach { rowItems ->

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {

                rowItems.forEach { reading ->

                    MonitoringReadingCell(
                        reading = reading,
                        isStale = isStale,
                        modifier = Modifier.weight(1f)
                    )
                }

                repeat(3 - rowItems.size) {

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MonitoringReadingCell(
    reading: MonitoringReading,
    isStale: Boolean,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(White)
            .border(
                width = 1.dp,
                color = if (isStale) GrayLight else GreenLight,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {

        Text(
            text = reading.label.uppercase(),
            style = SensorCellLabelStyle,
            color = if (isStale) GrayHint else GrayMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = reading.value,
            style = SensorCellValueStyle,
            color = if (isStale) GrayHint else Black,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(3.dp))

        ThresholdChip(level = reading.level, isStale = isStale)
    }
}

@Composable
private fun MonitoringWarningNote(
    text: String,
    background: Color
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(background)
            .border(
                width = 1.dp,
                color = WarningBorder,
                shape = RoundedCornerShape(9.dp)
            )
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {

        Canvas(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(13.dp)
        ) {

            drawMonitoringInfoGlyph(
                color = Warning,
                strokeWidthPx = 1.2.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = text,
            style = WizardCaptionStyle,
            color = Warning,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MonitoringReconnectButton(onClick: () -> Unit) {

    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(White)
            .border(
                width = 1.5.dp,
                color = GreenPrimary,
                shape = RoundedCornerShape(11.dp)
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Canvas(modifier = Modifier.size(14.dp)) {

            drawMonitoringRefreshGlyph(
                color = GreenPrimary,
                strokeWidthPx = 1.4.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = "Try to Reconnect",
            style = SheetButtonStyleFallback,
            color = GreenPrimary
        )
    }
}

@Composable
private fun MonitoringRepairButton(onClick: () -> Unit) {

    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(GreenPrimary)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Canvas(modifier = Modifier.size(14.dp)) {

            drawMonitoringRefreshGlyph(
                color = White,
                strokeWidthPx = 1.4.dp.toPx()
            )
        }

        Spacer(modifier = Modifier.width(7.dp))

        Text(
            text = "Re-pair Device",
            style = SheetButtonStyleFallback,
            color = White
        )
    }
}

@Composable
private fun MonitoringReconnectingButton() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(GreenBgLight)
            .border(
                width = 1.5.dp,
                color = GreenLight,
                shape = RoundedCornerShape(11.dp)
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            color = GreenPrimary,
            trackColor = GreenLight,
            strokeWidth = 2.dp
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Reconnecting…",
            style = SheetButtonStyleFallback,
            color = GreenDark
        )
    }
}

private fun DrawScope.drawMonitoringInfoGlyph(color: Color, strokeWidthPx: Float) {

    val w = size.width
    val h = size.height

    drawCircle(
        color = color,
        radius = w * 0.42f,
        style = Stroke(width = strokeWidthPx)
    )

    drawLine(
        color = color,
        start = Offset(w * 0.5f, h * 0.30f),
        end = Offset(w * 0.5f, h * 0.53f),
        strokeWidth = strokeWidthPx,
        cap = StrokeCap.Round
    )

    drawCircle(
        color = color,
        radius = w * 0.055f,
        center = Offset(w * 0.5f, h * 0.69f)
    )
}

private fun DrawScope.drawMonitoringRefreshGlyph(color: Color, strokeWidthPx: Float) {

    val s = size.minDimension / 14f
    val stroke = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)

    drawArc(
        color = color,
        startAngle = -40f,
        sweepAngle = 290f,
        useCenter = false,
        topLeft = Offset(2f * s, 2f * s),
        size = Size(10f * s, 10f * s),
        style = stroke
    )

    drawPath(
        path = Path().apply {
            moveTo(2f * s, 4f * s)
            lineTo(2f * s, 7f * s)
            lineTo(5f * s, 7f * s)
        },
        color = color,
        style = stroke
    )
}

/** 13sp medium button label (matches M3 titleMedium in this theme). */
private val SheetButtonStyleFallback = TextStyle(
    fontFamily = DMSans,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp
)