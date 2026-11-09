package com.example.siliguide.presentation.features.dashboard.components

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.safeDrawingPadding
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.DMSans
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

/* ══════════════════════════════════════════════════════════════════════════
   SCAFFOLD
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Shared chrome for both See All screens.
 *
 * White header block (back chevron + title + live count badge + search bar),
 * hairline divider, then an OffWhite list area. The count badge shows the
 * number of VISIBLE results, not the total — it moves as the user types.
 */
@Composable
fun SeeAllScaffold(
    title: String,
    visibleCount: Int,
    query: String,
    searchPlaceholder: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OffWhite)
            .safeDrawingPadding()
    ) {

        // ── Header ──────────────────────────────────────────────────────
        Column(modifier = Modifier.background(White)) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OffWhite)
                        .border(1.dp, Border, CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) { drawBackChevron(GrayBody) }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Black,
                    modifier = Modifier.weight(1f)
                )

                // Reflects currently visible results
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(GreenBgLight)
                        .border(1.dp, GreenLight, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = visibleCount.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = GreenDark
                    )
                }
            }

            // ── Search bar ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 8.dp)
            ) {
                SeeAllSearchBar(
                    query = query,
                    placeholder = searchPlaceholder,
                    onQueryChange = onQueryChange
                )
            }

            HorizontalDivider(thickness = 1.dp, color = Border)
        }

        // ── List area ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            content = content
        )
    }
}

/**
 * Search field.
 *
 * Idle: OffWhite fill, hairline border, gray magnifier. Active (non-empty):
 * white fill, 1.5dp green border, green magnifier, and a clear button.
 */
@Composable
private fun SeeAllSearchBar(
    query: String,
    placeholder: String,
    onQueryChange: (String) -> Unit
) {
    val isActive = query.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) White else OffWhite)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) GreenPrimary else Border,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Canvas(modifier = Modifier.size(14.dp)) {
            drawMagnifier(if (isActive) GreenPrimary else GrayHint)
        }

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = DMSans,
                fontSize = 12.sp,
                color = Black
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            cursorBrush = SolidColor(GreenPrimary),
            modifier = Modifier.weight(1f),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 12.sp,
                            fontFamily = DMSans,
                            color = GrayHint
                        )
                    }
                    innerTextField()
                }
            }
        )

        if (isActive) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(14.dp)) { drawClearButton() }
            }
        }
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   SHARED LIST PIECES
   ══════════════════════════════════════════════════════════════════════════ */

/** Small gray line above the list telling the user what they are looking at. */
@Composable
fun ResultSummaryRow(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontFamily = DMSans,
        color = GrayMedium,
        modifier = Modifier.padding(
            start = 22.dp,
            end = 22.dp,
            top = 12.dp,
            bottom = 2.dp
        )
    )
}

/**
 * No-results state — never a blank screen.
 *
 * [onClearSearch] is null when the list is empty because nothing exists yet,
 * rather than because the query matched nothing; the clear link is hidden then.
 */
@Composable
fun ColumnScope.SeeAllEmptyState(
    title: String,
    message: String,
    onClearSearch: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(OffWhite)
                .border(1.5.dp, Border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(28.dp)) { drawEmptySearchGlyph() }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color = GrayBody,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            fontSize = 12.sp,
            fontFamily = DMSans,
            color = GrayHint,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        if (onClearSearch != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Clear search",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = GreenPrimary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClearSearch
                )
            )
        }
    }
}

/** Status pill: green dot + "Connected", or gray dot + "Offline". */
@Composable
fun ConnectionStatusPill(isOnline: Boolean) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isOnline) GreenBgLight else OffWhite)
            .then(
                if (isOnline) Modifier
                else Modifier.border(1.dp, Border, RoundedCornerShape(6.dp))
            )
            .padding(horizontal = 8.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (isOnline) GreenPrimary else GrayLight)
        )
        Text(
            text = if (isOnline) "Connected" else "Offline",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color = if (isOnline) GreenDark else GrayHint
        )
    }
}

/* ══════════════════════════════════════════════════════════════════════════
   CANVAS ICONS — drawn inline, no drawable files
   ══════════════════════════════════════════════════════════════════════════ */

private fun DrawScope.roundStroke(width: Float) = Stroke(
    width = width,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round
)

/** "‹" back chevron, 14×14 viewBox. */
fun DrawScope.drawBackChevron(color: Color) {
    val k = size.width / 14f
    val path = Path().apply {
        moveTo(9f * k, 11f * k)
        lineTo(5f * k, 7f * k)
        lineTo(9f * k, 3f * k)
    }
    drawPath(path, color, style = roundStroke(1.5f * k))
}

/** Magnifying glass, 14×14 viewBox. */
fun DrawScope.drawMagnifier(color: Color) {
    val k = size.width / 14f
    drawCircle(
        color = color,
        radius = 4.5f * k,
        center = Offset(6f * k, 6f * k),
        style = Stroke(width = 1.2f * k)
    )
    drawLine(
        color = color,
        start = Offset(9.5f * k, 9.5f * k),
        end = Offset(12.5f * k, 12.5f * k),
        strokeWidth = 1.2f * k,
        cap = StrokeCap.Round
    )
}

/** Filled circle with an ✕ — the search clear button, 14×14 viewBox. */
fun DrawScope.drawClearButton() {
    val k = size.width / 14f
    drawCircle(
        color = Border,
        radius = 5.5f * k,
        center = Offset(7f * k, 7f * k)
    )
    drawLine(
        color = GrayMedium,
        start = Offset(5f * k, 5f * k),
        end = Offset(9f * k, 9f * k),
        strokeWidth = 1.2f * k,
        cap = StrokeCap.Round
    )
    drawLine(
        color = GrayMedium,
        start = Offset(9f * k, 5f * k),
        end = Offset(5f * k, 9f * k),
        strokeWidth = 1.2f * k,
        cap = StrokeCap.Round
    )
}

/** Magnifier with a faint plus inside — the empty state glyph, 28×28. */
fun DrawScope.drawEmptySearchGlyph() {
    val k = size.width / 28f

    drawCircle(
        color = GrayLight,
        radius = 8f * k,
        center = Offset(12f * k, 12f * k),
        style = Stroke(width = 1.5f * k)
    )
    drawLine(
        color = GrayLight,
        start = Offset(18.5f * k, 18.5f * k),
        end = Offset(24f * k, 24f * k),
        strokeWidth = 1.5f * k,
        cap = StrokeCap.Round
    )
    drawLine(
        color = GrayLight,
        start = Offset(9f * k, 12f * k),
        end = Offset(15f * k, 12f * k),
        strokeWidth = 1.3f * k,
        cap = StrokeCap.Round,
        alpha = 0.5f
    )
    drawLine(
        color = GrayLight,
        start = Offset(12f * k, 9f * k),
        end = Offset(12f * k, 15f * k),
        strokeWidth = 1.3f * k,
        cap = StrokeCap.Round,
        alpha = 0.5f
    )
}

/** Sensor box glyph, 20×20 viewBox. Tinted per online state. */
fun DrawScope.drawDeviceGlyph(color: Color) {
    val k = size.width / 20f

    drawRoundRect(
        color = color,
        topLeft = Offset(1f * k, 5f * k),
        size = androidx.compose.ui.geometry.Size(18f * k, 11f * k),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f * k),
        style = Stroke(width = 1.3f * k)
    )

    val handle = Path().apply {
        moveTo(5f * k, 5f * k)
        lineTo(5f * k, 4f * k)
        cubicTo(5f * k, 3.45f * k, 5.45f * k, 3f * k, 6f * k, 3f * k)
        lineTo(14f * k, 3f * k)
        cubicTo(14.55f * k, 3f * k, 15f * k, 3.45f * k, 15f * k, 4f * k)
        lineTo(15f * k, 5f * k)
    }
    drawPath(handle, color, style = Stroke(width = 1.3f * k))

    drawCircle(color, radius = 2f * k, center = Offset(10f * k, 10.5f * k))
}

/** Chili leaf with midrib and one vein, 24×24 viewBox. */
fun DrawScope.drawLeafGlyph() {
    val k = size.width / 24f

    val leaf = Path().apply {
        moveTo(12f * k, 4f * k)
        cubicTo(12f * k, 4f * k, 18f * k, 8f * k, 18f * k, 13f * k)
        cubicTo(18f * k, 16.31f * k, 15.31f * k, 19f * k, 12f * k, 19f * k)
        cubicTo(8.69f * k, 19f * k, 6f * k, 16.31f * k, 6f * k, 13f * k)
        cubicTo(6f * k, 8f * k, 12f * k, 4f * k, 12f * k, 4f * k)
        close()
    }
    drawPath(leaf, GreenPrimary)

    drawLine(
        color = GreenBgLight,
        start = Offset(12f * k, 19f * k),
        end = Offset(12f * k, 11f * k),
        strokeWidth = 1.3f * k,
        cap = StrokeCap.Round
    )

    val vein = Path().apply {
        moveTo(12f * k, 15f * k)
        cubicTo(12f * k, 15f * k, 9f * k, 13f * k, 8f * k, 11f * k)
    }
    drawPath(
        vein,
        GreenLight,
        style = Stroke(width = 1f * k, cap = StrokeCap.Round)
    )
}

/* ══════════════════════════════════════════════════════════════════════════
   FORMATTING
   ══════════════════════════════════════════════════════════════════════════ */

/**
 * Relative for the last week, absolute date beyond that.
 *
 * Matches the dashboard's formatting so a plant reads the same in both places.
 */
fun formatAnalyzedAt(millis: Long): String {

    val elapsed = System.currentTimeMillis() - millis

    val minutes = elapsed / 60_000
    val hours = elapsed / 3_600_000
    val days = elapsed / 86_400_000

    return when {
        elapsed < 0L -> DateFormat.format("MMM dd, yyyy", millis).toString()
        minutes < 1 -> "Just now"
        minutes < 60 -> "$minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
        hours < 24 -> "$hours ${if (hours == 1L) "hour" else "hours"} ago"
        days < 7 -> "$days ${if (days == 1L) "day" else "days"} ago"
        else -> DateFormat.format("MMM dd, yyyy", millis).toString()
    }
}

/** "1 result for "garden"" / "3 devices" — the summary line copy. */
fun resultSummaryText(
    query: String,
    count: Int,
    singular: String,
    plural: String
): String = when {
    query.isNotBlank() ->
        "$count ${if (count == 1) "result" else "results"} for \"$query\""

    count == 1 -> "1 $singular"

    else -> "$count $plural"
}