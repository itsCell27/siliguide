package com.example.siliguide.presentation.features.plants

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.R

// ─────────────────────────────────────────────────────────────────────────────
// SAMPLE DATA — previews only. Real screen loads from PlantViewModel below.
// ─────────────────────────────────────────────────────────────────────────────

private val dummyAnalysisHistory = listOf(
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

/** Fallback thumbnail gradient by predicted symptom class, used when a session has no saved image. */
private fun gradientForSymptom(symptom: String): Pair<Color, Color> = when (symptom.lowercase()) {
    "healthy"   -> Color(0xFF3B6D11) to Color(0xFF1F3909)
    "curling"   -> Color(0xFF7BA34C) to Color(0xFF3B6D11)
    "spots"     -> Color(0xFFB04040) to Color(0xFF6A1818)
    "yellowing" -> Color(0xFFA8B460) to Color(0xFF6A8030)
    else        -> Color(0xFF5E8C2E) to Color(0xFF3E6418)
}

// ─────────────────────────────────────────────────────────────────────────────
// SCREEN — wired to PlantViewModel (shares state with PlantDetailScreen)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AnalysisHistoryScreen(
    plantId: String,
    onBack: () -> Unit = {},
    onNavigateToAnalysisDetail: (sessionId: String) -> Unit = {},
    viewModel: PlantViewModel = hiltViewModel()
) {
    LaunchedEffect(plantId) { viewModel.loadPlant(plantId) }

    val plant by viewModel.plantState.collectAsState()
    val analysisHistory by viewModel.analysisHistoryState.collectAsState()

    val plantName = plant?.name ?: "Plant"

    val items = remember(analysisHistory) {
        analysisHistory.map { session ->
            val (gradStart, gradEnd) = gradientForSymptom(session.predictedClass)
            AnalysisHistoryItem(
                sessionId      = session.sessionId,
                symptom        = session.predictedClass,
                condition      = session.plantCondition ?: "No issues detected",
                confidence     = (session.confidence * 100).toInt(),
                isFullAnalysis = !session.isPartial,
                timestamp      = formatTimestamp(session.sessionTimestamp),
                imagePath      = session.imagePath,
                gradientStart  = gradStart,
                gradientEnd    = gradEnd
            )
        }
    }

    AnalysisHistoryContent(
        plantName                  = plantName,
        items                       = items,
        onBack                      = onBack,
        onNavigateToAnalysisDetail  = onNavigateToAnalysisDetail
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CONTENT — screen 11F/11G: top bar + plant line + search + result list
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalysisHistoryContent(
    plantName: String,
    items: List<AnalysisHistoryItem>,
    onBack: () -> Unit,
    onNavigateToAnalysisDetail: (sessionId: String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val isSearching = searchQuery.isNotBlank()

    val filteredItems = remember(searchQuery, items) {
        if (!isSearching) items
        else items.filter {
            it.symptom.contains(searchQuery, ignoreCase = true) ||
                    it.condition.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // ── Top bar ───────────────────────────────────────────────────────
        AnalysisHistoryTopBar(resultCount = filteredItems.size, onBack = onBack)

        // ── Plant subtitle row ────────────────────────────────────────────
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(start = 18.dp, top = 7.dp, end = 18.dp, bottom = 5.dp)
            ) {
                Text("Plant: ", fontSize = 11.sp, fontFamily = DMSans, color = GrayMedium)
                Text(plantName, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = GreenDark)
            }
            HorizontalDivider(color = Border, thickness = 1.dp)
        }

        // ── Search bar ────────────────────────────────────────────────────
        HistorySearchBar(query = searchQuery, onQueryChange = { searchQuery = it })

        // ── Results ───────────────────────────────────────────────────────
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
                Text(
                    if (isSearching) "No results" else "No analysis history yet",
                    style = MaterialTheme.typography.titleSmall,
                    color = GrayBody
                )
                Text(
                    if (isSearching) "No entries match \"$searchQuery\"." else "Analyze this plant to start building its history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayHint,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            if (isSearching) {
                Text(
                    text = "${filteredItems.size} result${if (filteredItems.size != 1) "s" else ""} for \"$searchQuery\"",
                    fontSize = 10.sp,
                    fontFamily = DMSans,
                    color = GrayMedium,
                    modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 2.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = if (isSearching) 0.dp else 12.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                items(filteredItems, key = { it.sessionId }) { item ->
                    AnalysisHistoryRow(
                        item    = item,
                        compact = false,
                        onClick = { onNavigateToAnalysisDetail(item.sessionId) }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalysisHistoryTopBar(
    resultCount: Int,
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

            // Count badge — always green per mockup (screen 11F & 11G)
            Box(
                modifier = Modifier
                    .background(GreenBgLight, RoundedCornerShape(20.dp))
                    .border(1.dp, GreenLight, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text       = "$resultCount",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSans,
                    color      = GreenDark
                )
            }
        }
        HorizontalDivider(color = Border, thickness = 1.dp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SEARCH BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun HistorySearchBar(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(OffWhite, RoundedCornerShape(10.dp))
                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 11.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    colorFilter = ColorFilter.tint(GrayMedium)
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            "Search analysis history",
                            fontSize = 12.sp,
                            fontFamily = DMSans,
                            color = GrayMedium
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp, fontFamily = DMSans, color = Black),
                        cursorBrush = SolidColor(GreenPrimary),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        HorizontalDivider(color = Border, thickness = 1.dp)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "11F — All results", showSystemUi = true)
@Composable
fun AnalysisHistory11FPreview() {
    SiliGuideTheme {
        AnalysisHistoryContent(
            plantName = "Labuyo Plant #2",
            items = dummyAnalysisHistory,
            onBack = {},
            onNavigateToAnalysisDetail = {}
        )
    }
}

@Preview(showBackground = true, name = "11G — Search \"leaf\"", showSystemUi = true)
@Composable
fun AnalysisHistory11GPreview() {
    SiliGuideTheme {
        AnalysisHistoryContent(
            plantName = "Labuyo Plant #2",
            items = dummyAnalysisHistory.filter {
                it.symptom.contains("leaf", ignoreCase = true) ||
                        it.condition.contains("leaf", ignoreCase = true)
            },
            onBack = {},
            onNavigateToAnalysisDetail = {}
        )
    }
}

@Preview(showBackground = true, name = "History — Empty", showSystemUi = true)
@Composable
fun AnalysisHistoryEmptyPreview() {
    SiliGuideTheme {
        AnalysisHistoryContent(
            plantName = "Labuyo Plant #2",
            items = emptyList(),
            onBack = {},
            onNavigateToAnalysisDetail = {}
        )
    }
}