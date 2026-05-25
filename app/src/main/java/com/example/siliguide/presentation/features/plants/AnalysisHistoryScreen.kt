package com.example.siliguide.presentation.features.plants

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.R

// ─────────────────────────────────────────────────────────────────────────────
// DUMMY DATA
// ─────────────────────────────────────────────────────────────────────────────

val dummyAnalysisHistory = listOf(
    AnalysisHistoryItem(
        sessionId      = "s1",
        symptom        = "Leaf Curling",
        condition      = "Underwatering / Drought stress",
        confidence     = 89,
        isFullAnalysis = true,
        timestamp      = "Apr 28, 2026 · 9:41 AM",
        imagePath      = null,
        gradientStart  = Color(0xFF7BA34C),
        gradientEnd    = Color(0xFF3B6D11)
    ),
    AnalysisHistoryItem(
        sessionId      = "s2",
        symptom        = "Healthy",
        condition      = "No issues detected",
        confidence     = 94,
        isFullAnalysis = true,
        timestamp      = "Apr 24, 2026 · 2:14 PM",
        imagePath      = null,
        gradientStart  = Color(0xFF3B6D11),
        gradientEnd    = Color(0xFF1F3909)
    ),
    AnalysisHistoryItem(
        sessionId      = "s3",
        symptom        = "Yellowing Leaves",
        condition      = "Possible: Nitrogen deficiency",
        confidence     = 62,
        isFullAnalysis = false,
        timestamp      = "Apr 20, 2026 · 10:30 AM",
        imagePath      = null,
        gradientStart  = Color(0xFFA8B460),
        gradientEnd    = Color(0xFF6A8030)
    ),
    AnalysisHistoryItem(
        sessionId      = "s4",
        symptom        = "Healthy",
        condition      = "No issues detected",
        confidence     = 91,
        isFullAnalysis = true,
        timestamp      = "Apr 14, 2026 · 8:05 AM",
        imagePath      = null,
        gradientStart  = Color(0xFF4A6E22),
        gradientEnd    = Color(0xFF2E4812)
    ),
    AnalysisHistoryItem(
        sessionId      = "s5",
        symptom        = "Leaf Spots",
        condition      = "Possible disease infection",
        confidence     = 44,
        isFullAnalysis = false,
        timestamp      = "Apr 8, 2026 · 4:20 PM",
        imagePath      = null,
        gradientStart  = Color(0xFFB04040),
        gradientEnd    = Color(0xFF6A1818)
    ),
    AnalysisHistoryItem(
        sessionId      = "s6",
        symptom        = "Healthy",
        condition      = "No issues detected",
        confidence     = 88,
        isFullAnalysis = true,
        timestamp      = "Mar 28, 2026 · 11:00 AM",
        imagePath      = null,
        gradientStart  = Color(0xFF5E8C2E),
        gradientEnd    = Color(0xFF3E6418)
    )
)

// ─────────────────────────────────────────────────────────────────────────────
// FILTER CHIP MODEL
// ─────────────────────────────────────────────────────────────────────────────

enum class HistoryFilter(val label: String) {
    ALL("All"),
    HEALTHY("Healthy"),
    WARNING("Warning"),
    FULL("Full"),
    PARTIAL("Partial")
}

private fun List<AnalysisHistoryItem>.applyFilter(filter: HistoryFilter): List<AnalysisHistoryItem> =
    when (filter) {
        HistoryFilter.ALL     -> this
        HistoryFilter.HEALTHY -> filter { it.symptom.lowercase() == "healthy" }
        HistoryFilter.WARNING -> filter { it.symptom.lowercase() != "healthy" }
        HistoryFilter.FULL    -> filter { it.isFullAnalysis }
        HistoryFilter.PARTIAL -> filter { !it.isFullAnalysis }
    }

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnalysisHistoryScreen(
    plantId: String,
    plantName: String,
    // TODO: replace with ViewModel + real data
    items: List<AnalysisHistoryItem> = dummyAnalysisHistory,
    onBack: () -> Unit = {},
    onNavigateToAnalysisDetail: (sessionId: String) -> Unit = {}
) {
    var activeFilter by remember { mutableStateOf(HistoryFilter.ALL) }

    val filteredItems = remember(activeFilter, items) {
        items.applyFilter(activeFilter)
    }

    // Badge color: amber when warning filter active, green otherwise
    val badgeIsWarning = activeFilter == HistoryFilter.WARNING

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        AnalysisHistoryTopBar(
            resultCount    = filteredItems.size,
            badgeIsWarning = badgeIsWarning,
            onBack         = onBack
        )

        // ── Plant subtitle row ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .border(
                    width = 1.dp,
                    color = Border,
                    shape = RoundedCornerShape(0.dp)
                )
                .padding(horizontal = 18.dp, vertical = 7.dp)
        ) {
            Text("Plant: ", fontSize = 11.sp, fontFamily = DMSans, color = GrayMedium)
            Text(plantName, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = GreenDark)
        }

        // ── Filter chips ──────────────────────────────────────────────────
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .border(width = 1.dp, color = Border, shape = RoundedCornerShape(0.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            items(HistoryFilter.entries) { filter ->
                FilterChip(
                    filter   = filter,
                    isActive = filter == activeFilter,
                    onClick  = { activeFilter = filter }
                )
            }
        }

        // ── Results list ──────────────────────────────────────────────────
        if (filteredItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(PageFrameBg)
                        .border(1.5.dp, Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painterResource(R.drawable.ic_search),
                        contentDescription = "Search",
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text("No results", style = MaterialTheme.typography.titleSmall, color = GrayBody)
                Text(
                    "No entries match the selected filter.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayHint
                )
            }
        } else {
            // Result count label
            Text(
                text     = "${filteredItems.size} result${if (filteredItems.size != 1) "s" else ""}${if (activeFilter != HistoryFilter.ALL) " showing issues" else ""}",
                fontSize = 10.sp,
                fontFamily = DMSans,
                color    = GrayMedium,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(filteredItems, key = { it.sessionId }) { item ->
                    AnalysisHistoryRow(
                        item    = item,
                        compact = false,
                        onClick = { onNavigateToAnalysisDetail(item.sessionId) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ANALYSIS HISTORY TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalysisHistoryTopBar(
    resultCount: Int,
    badgeIsWarning: Boolean,
    onBack: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(OffWhite)
                    .border(1.dp, Border, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                    contentDescription = "Back",
                    tint     = GrayBody,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text     = "Analysis History",
                style    = MaterialTheme.typography.headlineSmall,
                color    = Black,
                modifier = Modifier.weight(1f)
            )

            // Count badge
            Box(
                modifier = Modifier
                    .background(
                        if (badgeIsWarning) WarningBg else GreenBgLight,
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        1.dp,
                        if (badgeIsWarning) WarningBorder else GreenLight,
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text      = "$resultCount",
                    fontSize  = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSans,
                    color     = if (badgeIsWarning) Warning else GreenDark
                )
            }
        }
        HorizontalDivider(color = Border, thickness = 1.dp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// FILTER CHIP
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FilterChip(
    filter: HistoryFilter,
    isActive: Boolean,
    onClick: () -> Unit
) {
    // Active warning filter gets amber styling
    val isActiveWarning = isActive && filter == HistoryFilter.WARNING

    val bg      = when {
        isActiveWarning -> WarningBg
        isActive        -> GreenBgLight
        else            -> OffWhite
    }
    val border  = when {
        isActiveWarning -> WarningBorder
        isActive        -> GreenPrimary
        else            -> Border
    }
    val borderW = if (isActive) 1.5.dp else 1.dp
    val textColor = when {
        isActiveWarning -> Warning
        isActive        -> GreenDark
        else            -> GrayBody
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(borderW, border, RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(horizontal = 11.dp, vertical = 5.dp)
    ) {
        Text(
            text      = filter.label,
            fontSize  = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color     = textColor
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "11F — All results", showSystemUi = true)
@Composable
fun AnalysisHistory11FPreview() {
    SiliGuideTheme {
        AnalysisHistoryScreen(
            plantId   = "p1",
            plantName = "Labuyo Plant #2"
        )
    }
}

@Preview(showBackground = true, name = "11G — Warning filter", showSystemUi = true)
@Composable
fun AnalysisHistory11GPreview() {
    SiliGuideTheme {
        AnalysisHistoryScreen(
            plantId   = "p1",
            plantName = "Labuyo Plant #2"
        )
    }
}

@Preview(showBackground = true, name = "History — Empty list", showSystemUi = true)
@Composable
fun AnalysisHistoryEmptyPreview() {
    SiliGuideTheme {
        AnalysisHistoryScreen(
            plantId   = "p1",
            plantName = "Labuyo Plant #2",
            items     = emptyList()
        )
    }
}