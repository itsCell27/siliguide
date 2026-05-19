package com.example.siliguide.presentation.features.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.siliguide.core.ui.theme.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.animation.core.Animatable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.presentation.features.iot.IoTViewModel
import kotlinx.coroutines.launch
import android.text.format.DateFormat
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.Plant

// Status colors (extend theme)
object StatusColors {
    val HealthyBg = GreenBgLight
    val HealthyText = GreenDark
    val WarningBg = Color(0xFFFAEEDA)
    val WarningText = Color(0xFF633806)
    val DangerBg = Color(0xFFFCEBEB)
    val DangerText = Danger
    val NoneBg = OffWhite
    val NoneText = GrayMedium
}

data class PlantCondition(
    val status: String, // healthy, warning, danger, none
    val deviceName: String?, // null if no device
    val isDeviceConnected: Boolean = false,
    val lastUpdated: String
)

data class PlantItem(
    val id: String,
    val name: String,
    val condition: PlantCondition
)

@Composable
fun DashboardScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToAddPlant: () -> Unit,
    onNavigateToAddIoT: () -> Unit,
    onNavigateToPlantDetail: (plantId: String) -> Unit,
    onNavigateToSeeAll: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isAddMenuOpen by remember { mutableStateOf(false) }

    // 1. Map Domain Plants to UI PlantItems
    val displayPlants = uiState.plants.map { domainPlant ->
        // This find logic now correctly looks at Domain models
        val linkedDevice = uiState.devices.find { it.deviceId == domainPlant.deviceId }

        PlantItem(
            id = domainPlant.plantId,
            name = domainPlant.name,
            condition = PlantCondition(
                status = "none", // Rule 6: Initial state
                deviceName = linkedDevice?.deviceName,
                isDeviceConnected = linkedDevice?.isActive ?: false,
                lastUpdated = DateFormat.format("MMM dd", domainPlant.dateCreated).toString()
            )
        )
    }

    // 2. Use domain devices directly - no mapping needed
    val displayDevices = uiState.devices

    Box(modifier = Modifier.fillMaxSize().background(OffWhite)) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding() // FIX: Adds dynamic padding so bottom items aren't hidden by the new taller nav bar
                .padding(bottom = 80.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // TOP BAR
            TopBarSection()

            // Pass the REAL data from the database
            IoTDevicesSection(devices = displayDevices, onAddDevice = onNavigateToAddIoT)

            // Pass the REAL data from the database
            PlantsSection(
                plants = displayPlants,
                onSeeAll = onNavigateToSeeAll,
                onNavigateToPlantDetail = onNavigateToPlantDetail
            )

            TestIoTScreen()
        }

        // SCRIM OVERLAY
        AnimatedVisibility(
            visible = isAddMenuOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isAddMenuOpen = false }
            )
        }

        // BOTTOM SHEET
        AnimatedVisibility(
            visible = isAddMenuOpen,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding() // FIX: Pushes the bottom sheet perfectly above the system nav bar
                .zIndex(10f)
        ) {
            BottomSheetContent(
                onAddPlant = {
                    isAddMenuOpen = false
                    onNavigateToAddPlant()
                },
                onAddIoT = {
                    isAddMenuOpen = false
                    onNavigateToAddIoT()
                },
                onCancel = { isAddMenuOpen = false }
            )
        }

        // BOTTOM NAVIGATION
        BottomNavSection(
            modifier = Modifier.align(Alignment.BottomCenter),
            isAddMenuOpen = isAddMenuOpen,
            onAddClick = { isAddMenuOpen = !isAddMenuOpen },
        )
    }
}

@Composable
fun TopBarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .statusBarsPadding() // FIX: Pushes the TopBar content safely below the notification bar
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // SiliGuide Logo
        Row {
            Text(
                text = "Sili",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = Black,
                letterSpacing = (-0.3f).sp
            )
            Text(
                text = "Guide",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = GreenPrimary,
                letterSpacing = (-0.3f).sp
            )
        }
    }
}

@Composable
fun IoTDevicesSection(devices: List<IoTDevice>, onAddDevice: () -> Unit) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IoT Devices",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = GrayHint,
                letterSpacing = 0.5.sp
            )
        }

        if (devices.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 10.dp)
                    .border(1.dp, GrayLight, RoundedCornerShape(12.dp))
                    .padding(12.dp, 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row {
                    Text(
                        text = "No devices connected. ",
                        fontSize = 12.sp,
                        fontFamily = DMSans,
                        color = GrayHint
                    )
                    Text(
                        text = "Add one",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = GreenPrimary,
                        modifier = Modifier.clickable { onAddDevice() }
                    )
                }
            }
        } else {
            // Horizontal scroll of devices
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                devices.forEach { device ->
                    IoTDeviceCard(device = device)
                }

                // Add device button
                Box(
                    modifier = Modifier
                        .size(width = 130.dp, height = 70.dp)
                        .background(White, RoundedCornerShape(12.dp))
                        .border(1.dp, GrayLight, RoundedCornerShape(12.dp))
                        .clickable { onAddDevice() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+ Add",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = GreenPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun IoTDeviceCard(device: IoTDevice) {
    Card(
        modifier = Modifier
            .size(width = 130.dp, height = 70.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(10.dp, 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Top: Icon + Status Dot
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(GreenBgLight, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // IoT Icon placeholder
                    Text("◇", fontSize = 14.sp, color = GreenPrimary)
                }

                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (device.isActive) GreenPrimary
                            else GrayLight
                        )
                )
            }

            // Device Name
            Text(
                text = device.deviceName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = Black,
                maxLines = 1
            )

            // Status text
            Text(
                text = if (device.isActive) "Connected" else "Offline",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = if (device.isActive) GreenPrimary
                else GrayHint
            )
        }
    }
}

@Composable
fun PlantsSection(
    plants: List<PlantItem>,
    onSeeAll: () -> Unit,
    onNavigateToPlantDetail: (plantId: String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        // Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Plants",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = GrayHint,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "See All",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = GreenPrimary,
                modifier = Modifier.clickable { onSeeAll() }
            )
        }

        if (plants.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(OffWhite)
                            .border(1.5.dp, Border, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🌱", fontSize = 36.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No plants yet",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = GrayMedium
                    )
                    Text(
                        text = "Add your first Siling Labuyo plant to get started",
                        fontSize = 12.sp,
                        fontFamily = DMSans,
                        color = GrayHint,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                plants.forEach { plant ->
                    PlantCard(
                        plant = plant,
                        onNavigateToDetail = { onNavigateToPlantDetail(plant.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun PlantCard(
    plant: PlantItem,
    onNavigateToDetail: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetail() },
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp, 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Plant Thumbnail
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenBgLight)
                    .border(1.dp, GreenLight, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🌿", fontSize = 20.sp)
            }

            // Plant Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = plant.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSans,
                    color = Black
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Device Badge
                    if (plant.condition.deviceName != null) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (plant.condition.isDeviceConnected)
                                        GreenBgLight
                                    else
                                        OffWhite,
                                    RoundedCornerShape(5.dp)
                                )
                                .padding(2.dp, 3.dp, 6.dp, 3.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (plant.condition.isDeviceConnected)
                                                GreenPrimary
                                            else
                                                GrayLight
                                        )
                                )
                                Text(
                                    text = plant.condition.deviceName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = DMSans,
                                    color = if (plant.condition.isDeviceConnected)
                                        StatusColors.HealthyText
                                    else
                                        GrayHint
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .background(OffWhite, RoundedCornerShape(5.dp))
                                .padding(2.dp, 3.dp, 6.dp, 3.dp)
                        ) {
                            Text(
                                text = "No device",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = DMSans,
                                color = GrayHint
                            )
                        }
                    }

                    // Separator dot
                    Box(
                        modifier = Modifier
                            .size(3.dp)
                            .clip(CircleShape)
                            .background(GrayLight)
                    )

                    // Date
                    Text(
                        text = plant.condition.lastUpdated,
                        fontSize = 10.sp,
                        fontFamily = DMSans,
                        color = GrayMedium
                    )
                }
            }

            // Status Badge
            ConditionBadge(status = plant.condition.status)
        }
    }
}

@Composable
fun ConditionBadge(status: String) {
    val (bgColor, textColor) = when (status) {
        "healthy" -> StatusColors.HealthyBg to StatusColors.HealthyText
        "warning" -> StatusColors.WarningBg to StatusColors.WarningText
        "danger" -> StatusColors.DangerBg to StatusColors.DangerText
        else -> OffWhite to GrayMedium
    }

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(3.dp, 4.dp, 8.dp, 4.dp)
    ) {
        Text(
            text = when (status) {
                "healthy" -> "Healthy"
                "warning" -> "Underwatering"
                "danger" -> "Leaf Spots"
                else -> "Not analyzed"
            },
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = DMSans,
            color = textColor
        )
    }
}

@Composable
fun BottomSheetContent(
    onAddPlant: () -> Unit,
    onAddIoT: () -> Unit,
    onCancel: () -> Unit
) {
    // 1. Upgrade to Animatable for smooth spring-back physics
    val offsetY = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        // If they dragged it down more than 150 pixels, close the menu!
                        if (offsetY.value > 150f) {
                            onCancel()
                            // FIX: We deliberately DO NOT reset offsetY to 0 here!
                            // We leave it exactly where the user's finger left it,
                            // so AnimatedVisibility seamlessly takes over the downward slide.
                        } else {
                            // If they didn't drag far enough, smoothly bounce it back up
                            coroutineScope.launch {
                                offsetY.animateTo(0f, animationSpec = tween(300))
                            }
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    // Only allow dragging downwards
                    if (offsetY.value + dragAmount >= 0) {
                        coroutineScope.launch {
                            offsetY.snapTo(offsetY.value + dragAmount)
                        }
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = White),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 22.dp, bottom = 42.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 36.dp, height = 4.dp)
                    .background(GrayLight, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(18.dp))

            // Title
            Text(
                text = "What would you like to add?",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = Black,
                letterSpacing = (-0.3f).sp,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            // Subtitle
            Text(
                text = "Choose an option below to get started.",
                fontSize = 12.sp,
                fontFamily = DMSans,
                color = GrayMedium,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 18.dp)
            )

            // Option 1: Add Plant
            SheetOptionItem(
                icon = "🌱",
                title = "Add New Plant",
                description = "Create a new Siling Labuyo plant entry and optionally link a sensor device",
                onClick = onAddPlant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Option 2: Add IoT
            SheetOptionItem(
                icon = "📡",
                title = "Add IoT Device",
                description = "Connect a new IoT device via AP hotspot or home WiFi network",
                onClick = onAddIoT
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Cancel button
            TextButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Cancel",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSans,
                    color = GrayHint
                )
            }
        }
    }
}

@Composable
fun SheetOptionItem(
    icon: String,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = OffWhite),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp, 14.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(GreenBgLight, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }

            // Text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSans,
                    color = Black
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    fontFamily = DMSans,
                    color = GrayMedium,
                    lineHeight = 16.sp
                )
            }

            // Chevron
            Text("›", fontSize = 16.sp, color = GrayHint)
        }
    }
}

@Composable
fun BottomNavSection(
    modifier: Modifier = Modifier,
    isAddMenuOpen: Boolean,
    onAddClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 16.dp)
            .background(White)
            .navigationBarsPadding() // FIX: Adds padding to keep content above the device's navigation buttons/gesture bar
            .padding(horizontal = 28.dp, vertical = 12.dp)
            .zIndex(3f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Add Button (Center/Floating)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isAddMenuOpen) GreenDark
                        else GreenPrimary
                    )
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isAddMenuOpen) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "Add",
                    tint = White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun TestIoTScreen() {
    val viewModel: IoTViewModel = hiltViewModel()

    // Use DisposableEffect instead of LaunchedEffect
    DisposableEffect(Unit) {
        // Starts the 3-second loop when the screen appears
        viewModel.startLiveMonitoring()

        onDispose {
            // Kills the loop immediately when the user navigates away
            viewModel.stopLiveMonitoring()
        }
    }

    when {
        viewModel.isLoading -> Text("Loading...")
        viewModel.error != null -> Text(viewModel.error!!)
        viewModel.sensorData != null -> {

            val data = viewModel.sensorData!!

            Column {
                Text("Soil Moisture: ${data.soil.moisture}")
                Text("Soil Temp: ${data.soil.temperature}")
                Text("pH: ${data.soil.ph}")

                Text("EC: ${data.soil.ec}")
                Text("Nitrogen: ${data.soil.nitrogen}")
                Text("Phosphorus: ${data.soil.phosphorus}")
                Text("Potassium: ${data.soil.potassium}")

                Text("Light: ${data.environment.light}")
                Text("Air Temp: ${data.environment.airTemperature}")
            }
        }
    }
}