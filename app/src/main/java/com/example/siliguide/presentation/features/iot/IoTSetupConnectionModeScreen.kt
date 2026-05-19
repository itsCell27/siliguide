package com.example.siliguide.presentation.features.iot

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.R

// Define the two possible connection paths
enum class ConnectionMode {
    AP_MODE, LAN_MODE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IoTSetupConnectionModeScreen(
    onNavigateBack: () -> Unit,
    onNext: (ConnectionMode) -> Unit
) {
    // Tracks which card the user has selected
    var selectedMode by remember { mutableStateOf<ConnectionMode?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Add Device",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = DMSans,
                            color = Black
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.padding(start = 8.dp) // Pushes it slightly off the edge
                    ) {
                        // The custom bordered circle
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(White)
                                .border(1.dp, Border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.chevron_left),
                                contentDescription = "Back",
                                tint = Black,
                                modifier = Modifier.size(24.dp) // Slightly smaller than the box
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OffWhite
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(White)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Button(
                    // Only trigger the callback if a mode is selected
                    onClick = { selectedMode?.let { onNext(it) } },
                    enabled = selectedMode != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary,
                        disabledContainerColor = GrayLight
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Continue",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DMSans,
                        // Text turns white when active, gray when disabled
                        color = if (selectedMode != null) White else GrayMedium
                    )
                }
            }
        },
        containerColor = OffWhite
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp, vertical = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // Header Text
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "How do you want to connect?",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Fraunces,
                    color = Black,
                    lineHeight = 32.sp
                )
                Text(
                    text = "Choose how the app communicates with your ESP32 sensor.",
                    fontSize = 14.sp,
                    fontFamily = DMSans,
                    color = GrayMedium,
                    lineHeight = 20.sp
                )
            }

            // Selection Cards
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ConnectionModeCard(
                    icon = "📡",
                    title = "AP Mode (Direct)",
                    tag = "No internet needed",
                    description = "Phone connects directly to the ESP32's hotspot. Best for farms without WiFi.",
                    isSelected = selectedMode == ConnectionMode.AP_MODE,
                    onClick = { selectedMode = ConnectionMode.AP_MODE }
                )

                ConnectionModeCard(
                    icon = "🏠",
                    title = "LAN Mode (Local)",
                    tag = "Needs router",
                    description = "ESP32 connects to your home WiFi network. Phone must be on the same WiFi.",
                    isSelected = selectedMode == ConnectionMode.LAN_MODE,
                    onClick = { selectedMode = ConnectionMode.LAN_MODE }
                )
            }
        }
    }
}

@Composable
fun ConnectionModeCard(
    icon: String,
    title: String,
    tag: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Dynamic styling based on selection state
    val borderColor = if (isSelected) GreenPrimary else Border
    val bgColor = if (isSelected) GreenBgLight else White
    val iconBgColor = if (isSelected) White else OffWhite

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBgColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp)
            }

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DMSans,
                        color = Black
                    )

                    // Custom Radio Button Visual
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                color = if (isSelected) GreenPrimary else GrayLight,
                                shape = CircleShape
                            )
                            .background(if (isSelected) GreenPrimary else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(White)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tag Badge
                Surface(
                    color = if (isSelected) White.copy(alpha = 0.6f) else OffWhite,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = DMSans,
                        color = GrayMedium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = description,
                    fontSize = 13.sp,
                    fontFamily = DMSans,
                    color = GrayMedium,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "IoT Connection Mode Screen")
@Composable
fun IoTSetupConnectionModeScreenPreview() {
    // Wrap the preview in your custom theme!
    SiliGuideTheme {
        IoTSetupConnectionModeScreen(
            onNext = {},
            onNavigateBack = {}
        )
    }
}