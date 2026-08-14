package com.example.siliguide.presentation.features.dashboard.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.DMSans
import com.example.siliguide.core.ui.theme.GrayHint
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.domain.model.ConnectionMode

/** One row in the See All device list. */
data class SeeAllDeviceItem(
    val deviceId: String,
    val name: String,
    val mode: ConnectionMode,
    val isOnline: Boolean
)

/**
 * See All — IoT Devices.
 *
 * Stateless: the query, the filtered list and the navigation callbacks all come
 * from the caller. Filtering happens in the ViewModel so the count badge and
 * the summary line always agree with what is rendered.
 */
@Composable
fun SeeAllDevicesScreen(
    devices: List<SeeAllDeviceItem>,
    query: String,
    isLinkingMode: Boolean = false,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onDeviceClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    SeeAllScaffold(
        title = if (isLinkingMode) "Link Device" else "IoT Devices",
        visibleCount = devices.size,
        query = query,
        searchPlaceholder = "Search devices...",
        onQueryChange = onQueryChange,
        onBack = onBack,
        modifier = modifier
    ) {
        if (devices.isEmpty()) {

            if (query.isBlank()) {
                if (isLinkingMode) {
                    SeeAllEmptyState(
                        title = "No unlinked devices",
                        message = "All your IoT devices are currently linked to other plants, or you haven't added any yet."
                    )
                } else {
                    SeeAllEmptyState(
                        title = "No devices yet",
                        message = "Add an IoT device from the dashboard to start monitoring your Siling Labuyo plants."
                    )
                }
            } else {
                SeeAllEmptyState(
                    title = "No devices found",
                    message = "No results for \"$query\". Try a different name or clear " +
                            "the search.",
                    onClearSearch = { onQueryChange("") }
                )
            }

        } else {

            // Summary only appears once the user has typed — the default list
            // needs no explanation
            if (query.isNotBlank()) {
                ResultSummaryRow(
                    text = resultSummaryText(
                        query = query,
                        count = devices.size,
                        singular = "device",
                        plural = "devices"
                    )
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 22.dp,
                    end = 22.dp,
                    top = if (query.isBlank()) 12.dp else 8.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = devices,
                    key = { it.deviceId }
                ) { device ->
                    DeviceListItem(
                        device = device,
                        isLinkingMode = isLinkingMode,
                        onClick = { onDeviceClick(device.deviceId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DeviceListItem(
    device: SeeAllDeviceItem,
    isLinkingMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Tile and glyph both go gray when the device is unreachable
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (device.isOnline) GreenBgLight else OffWhite),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawDeviceGlyph(if (device.isOnline) GreenPrimary else GrayHint)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = device.name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = device.mode.seeAllLabel(),
                    fontSize = 10.sp,
                    fontFamily = DMSans,
                    color = GrayMedium
                )
            }
        }

        // SWAP UI: If linking mode, show "Link", else show standard green dot
        if (isLinkingMode) {
            Text(
                text = "Link",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = GreenPrimary,
                modifier = Modifier.padding(end = 4.dp)
            )
        } else {
            ConnectionStatusPill(isOnline = device.isOnline)
        }
    }
}

/** "LAN Mode" / "AP Mode" / "Bluetooth Mode". */
private fun ConnectionMode.seeAllLabel(): String = when (this) {
    ConnectionMode.LAN -> "LAN Mode"
    ConnectionMode.AP -> "AP Mode"
    ConnectionMode.BLUETOOTH -> "Bluetooth Mode"
}

/* ── Previews ──────────────────────────────────────────────────────────── */

private val previewDevices = listOf(
    SeeAllDeviceItem("1", "Garden Sensor", ConnectionMode.LAN, true),
    SeeAllDeviceItem("2", "Pot Sensor 2", ConnectionMode.AP, false),
    SeeAllDeviceItem("3", "Field Sensor 3", ConnectionMode.BLUETOOTH, false)
)

@Preview(name = "Default list", showSystemUi = true, widthDp = 360, heightDp = 740)
@Composable
private fun SeeAllDevicesDefaultPreview() {
    SiliGuideTheme {
        SeeAllDevicesScreen(
            devices = previewDevices,
            query = "",
            onQueryChange = {},
            onBack = {},
            onDeviceClick = {}
        )
    }
}

@Preview(name = "Active search", showSystemUi = true, widthDp = 360, heightDp = 740)
@Composable
private fun SeeAllDevicesSearchPreview() {
    SiliGuideTheme {
        SeeAllDevicesScreen(
            devices = previewDevices.take(1),
            query = "garden",
            onQueryChange = {},
            onBack = {},
            onDeviceClick = {}
        )
    }
}

@Preview(name = "No results", showSystemUi = true, widthDp = 360, heightDp = 740)
@Composable
private fun SeeAllDevicesNoResultsPreview() {
    SiliGuideTheme {
        SeeAllDevicesScreen(
            devices = emptyList(),
            query = "xyz123",
            onQueryChange = {},
            onBack = {},
            onDeviceClick = {}
        )
    }
}

@Preview(name = "Nothing added yet", showSystemUi = true, widthDp = 360, heightDp = 740)
@Composable
private fun SeeAllDevicesEmptyPreview() {
    SiliGuideTheme {
        SeeAllDevicesScreen(
            devices = emptyList(),
            query = "",
            onQueryChange = {},
            onBack = {},
            onDeviceClick = {}
        )
    }
}