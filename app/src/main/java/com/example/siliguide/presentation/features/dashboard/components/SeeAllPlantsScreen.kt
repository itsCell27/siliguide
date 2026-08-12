package com.example.siliguide.presentation.features.dashboard.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.DMSans
import com.example.siliguide.core.ui.theme.DangerBg
import com.example.siliguide.core.ui.theme.Danger
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GrayHint
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenLight
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.Warning
import com.example.siliguide.core.ui.theme.WarningBg
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.presentation.features.dashboard.formatLastAnalyzed
import java.io.File

/** One row in the See All plant list. */
data class SeeAllPlantItem(
    val plantId: String,
    val name: String,
    /** "healthy" | "warning" | "danger" | "none" — drives the badge colors. */
    val status: String,
    val conditionLabel: String,
    /** Epoch millis of the newest analysis, or null when never analyzed. */
    val lastAnalyzedAt: Long? = null,
    val lastAnalyzedImagePath: String? = null
) {
    /** Anything other than healthy, and never-analyzed, needs a look. */
    val needsAttention: Boolean
        get() = status == "warning" || status == "danger"
}

/**
 * See All — My Plants.
 *
 * Stateless. Filtering lives in the ViewModel so the count badge, the summary
 * line and the rendered list can never disagree.
 */
@Composable
fun SeeAllPlantsScreen(
    plants: List<SeeAllPlantItem>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onPlantClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val attentionCount = plants.count { it.needsAttention }

    SeeAllScaffold(
        title = "My Plants",
        visibleCount = plants.size,
        query = query,
        searchPlaceholder = "Search plants...",
        onQueryChange = onQueryChange,
        onBack = onBack,
        modifier = modifier
    ) {
        if (plants.isEmpty()) {

            if (query.isBlank()) {
                SeeAllEmptyState(
                    title = "No plants yet",
                    message = "Add your first Siling Labuyo plant from the dashboard to " +
                            "start tracking its condition."
                )
            } else {
                SeeAllEmptyState(
                    title = "No plants found",
                    message = "No results for \"$query\". Try a different name or clear " +
                            "the search.",
                    onClearSearch = { onQueryChange("") }
                )
            }

        } else {

            // Search wins over the attention count — the user asked a question
            // and the summary should answer it
            val summary = when {
                query.isNotBlank() -> resultSummaryText(
                    query = query,
                    count = plants.size,
                    singular = "plant",
                    plural = "plants"
                )

                attentionCount > 0 ->
                    "$attentionCount ${if (attentionCount == 1) "plant" else "plants"} " +
                            "need attention"

                else -> null
            }

//            if (summary != null) {
//                ResultSummaryRow(text = summary)
//            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 22.dp,
                    end = 22.dp,
                    top = if (summary == null) 12.dp else 8.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = plants,
                    key = { it.plantId }
                ) { plant ->
                    PlantListItem(
                        plant = plant,
                        onClick = { onPlantClick(plant.plantId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlantListItem(
    plant: SeeAllPlantItem,
    onClick: () -> Unit,
    compact: Boolean = false
) {
    val thumbSize = if (compact) 55.dp else 60.dp
    val thumbRadius = if (compact) 10.dp else 11.dp

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick },
        colors = CardDefaults.cardColors(containerColor = White),
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!plant.lastAnalyzedImagePath.isNullOrEmpty() && File(plant.lastAnalyzedImagePath).exists()) {
                AsyncImage(
                    model = File(plant.lastAnalyzedImagePath),
                    contentDescription = "Leaf Image",
                    modifier = Modifier
                        .size(thumbSize)
                        .clip(RoundedCornerShape(thumbRadius)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(thumbSize)
                        .clip(RoundedCornerShape(thumbRadius))
                        .background(GreenBgLight)
                        .border(1.dp, GreenLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(R.drawable.ic_leaf),
                        contentDescription = "Leaf",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val analyzedAt = plant.lastAnalyzedAt

                if (analyzedAt == null) {
                    Text(
                        text = "Not yet analyzed",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayHint,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    Text(
                        text = formatLastAnalyzed(analyzedAt),
                        fontSize = 10.sp,
                        fontFamily = DMSans,
                        color = GrayHint
                    )
                    Text(
                        text = plant.conditionLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayBody
                    )
                }
            }
        }
    }
}

/**
 * Condition badge.
 *
 * The mockup annotation says "Condition badge always green", but the severity
 * tokens exist and carry more information, so warning and danger keep their
 * own colors. Pass status = "healthy" if you want the flat green look.
 */
@Composable
private fun PlantStatusBadge(status: String, label: String) {

    val (bgColor, textColor) = when (status) {
        "healthy" -> GreenBgLight to GreenDark
        "warning" -> WarningBg to Warning
        "danger" -> DangerBg to Danger
        else -> OffWhite to GrayMedium
    }

    val displayLabel = when {
        label.isNotBlank() -> label
        status == "healthy" -> "Healthy"
        status == "warning" -> "Warning"
        status == "danger" -> "Danger"
        else -> "—"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = displayLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color = textColor,
            maxLines = 1
        )
    }
}

/* ── Previews ──────────────────────────────────────────────────────────── */

private val previewPlants = listOf(
    SeeAllPlantItem(
        "1", "Labuyo Plant #1", "healthy", "Healthy",
        System.currentTimeMillis() - 12 * 60_000
    ),
    SeeAllPlantItem(
        "2", "Backyard Sili", "warning", "Underwatering",
        System.currentTimeMillis() - 30L * 86_400_000
    ),
    SeeAllPlantItem(
        "3", "Pot #3", "warning", "Leaf Spots",
        System.currentTimeMillis() - 34L * 86_400_000
    ),
    SeeAllPlantItem("4", "Window Sili", "none", "", null)
)

@Preview(name = "Default list", showSystemUi = true, widthDp = 360, heightDp = 740)
@Composable
private fun SeeAllPlantsDefaultPreview() {
    SiliGuideTheme {
        SeeAllPlantsScreen(
            plants = previewPlants,
            query = "",
            onQueryChange = {},
            onBack = {},
            onPlantClick = {}
        )
    }
}

@Preview(name = "Needs attention", showSystemUi = true, widthDp = 360, heightDp = 740)
@Composable
private fun SeeAllPlantsAttentionPreview() {
    SiliGuideTheme {
        SeeAllPlantsScreen(
            plants = previewPlants.filter { it.needsAttention },
            query = "",
            onQueryChange = {},
            onBack = {},
            onPlantClick = {}
        )
    }
}

@Preview(name = "Active search", showSystemUi = true, widthDp = 360, heightDp = 740)
@Composable
private fun SeeAllPlantsSearchPreview() {
    SiliGuideTheme {
        SeeAllPlantsScreen(
            plants = previewPlants.take(1),
            query = "labuyo",
            onQueryChange = {},
            onBack = {},
            onPlantClick = {}
        )
    }
}

@Preview(name = "No results", showSystemUi = true, widthDp = 360, heightDp = 740)
@Composable
private fun SeeAllPlantsNoResultsPreview() {
    SiliGuideTheme {
        SeeAllPlantsScreen(
            plants = emptyList(),
            query = "xyz123",
            onQueryChange = {},
            onBack = {},
            onPlantClick = {}
        )
    }
}