package com.example.siliguide.presentation.features.dashboard

import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.presentation.features.iot.IoTViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ── Layout constants ──────────────────────────────────────────────────────────
private val BottomNavHeight  = 68.dp
private val IotCardWidth     = 200.dp
private val AddDeviceCardWidth = 100.dp

// ── Status color tokens ───────────────────────────────────────────────────────
object StatusColors {
    val HealthyBg   = GreenBgLight
    val HealthyText = GreenDark
    val WarningText = Warning
    val DangerText  = Danger
    val NoneBg      = OffWhite
    val NoneText    = GrayMedium
}

// ── UI data models ────────────────────────────────────────────────────────────
data class PlantCondition(
    val status: String,
    val label: String = "",
    val deviceName: String?,
    val isDeviceConnected: Boolean = false,
    val lastDiagnosedDate: String
)

data class PlantItem(
    val id: String,
    val name: String,
    val condition: PlantCondition
)

// ─────────────────────────────────────────────────────────────────────────────
// DASHBOARD SCREEN  (entry point — Hilt lives here only)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToAddPlant: () -> Unit,
    onNavigateToAddIoT: () -> Unit,
    onNavigateToPlantDetail: (plantId: String) -> Unit,
    onNavigateToSeeAll: () -> Unit,
    onNavigateToProfile: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isAddMenuOpen by remember { mutableStateOf(false) }

    val displayPlants = uiState.plants.map { domainPlant ->
        val linkedDevice = uiState.devices.find { it.deviceId == domainPlant.deviceId }
        PlantItem(
            id = domainPlant.plantId,
            name = domainPlant.name,
            condition = PlantCondition(
                status = "none",
                label = "",
                deviceName = linkedDevice?.deviceName,
                isDeviceConnected = linkedDevice?.isActive ?: false,
                lastDiagnosedDate = DateFormat.format("MMM dd, yyyy", domainPlant.dateCreated).toString()
            )
        )
    }

    DashboardContent(
        plants              = displayPlants,
        devices             = uiState.devices,
        isAddMenuOpen       = isAddMenuOpen,
        onAddMenuToggle     = { isAddMenuOpen = it },
        onNavigateToCamera  = onNavigateToCamera,
        onNavigateToAddPlant= onNavigateToAddPlant,
        onNavigateToAddIoT  = onNavigateToAddIoT,
        onNavigateToPlantDetail = onNavigateToPlantDetail,
        onNavigateToSeeAll  = onNavigateToSeeAll,
        onNavigateToProfile = onNavigateToProfile,
        testIotContent      = { TestIoTScreen() }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DASHBOARD CONTENT  (Hilt-free, previewable)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DashboardContent(
    plants: List<PlantItem>,
    devices: List<IoTDevice>,
    isAddMenuOpen: Boolean,
    onAddMenuToggle: (Boolean) -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToAddPlant: () -> Unit,
    onNavigateToAddIoT: () -> Unit,
    onNavigateToPlantDetail: (String) -> Unit,
    onNavigateToSeeAll: () -> Unit,
    onNavigateToProfile: () -> Unit,
    testIotContent: @Composable () -> Unit = {}
) {
    // Dynamically compute how much bottom padding the scrollable body needs:
    // system nav bar inset + our custom bottom nav bar height.
    val systemNavBarInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    val scrollBodyBottomPadding = BottomNavHeight + systemNavBarInset

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OffWhite)
    ) {
        // ── Scrollable body ───────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {
            TopBarSection()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // fills remaining space, enabling correct scroll
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = scrollBodyBottomPadding)
            ) {
                IoTDevicesSection(devices = devices, onAddDevice = onNavigateToAddIoT)
                PlantsSection(
                    plants = plants,
                    onSeeAll = onNavigateToSeeAll,
                    onNavigateToPlantDetail = onNavigateToPlantDetail
                )
                testIotContent()
            }
        }

        // ── Scrim overlay ─────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = isAddMenuOpen,
            enter    = fadeIn(),
            exit     = fadeOut(),
            modifier = Modifier.zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAddMenuToggle(false) }
            )
        }

        // ── Bottom sheet ──────────────────────────────────────────────────
        AnimatedVisibility(
            visible  = isAddMenuOpen,
            enter    = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
            exit     = slideOutVertically(targetOffsetY  = { it }, animationSpec = tween(250)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .zIndex(10f)
        ) {
            BottomSheetContent(
                onAddPlant = { onAddMenuToggle(false); onNavigateToAddPlant() },
                onAddIoT   = { onAddMenuToggle(false); onNavigateToAddIoT()   },
                onCancel   = { onAddMenuToggle(false) }
            )
        }

        // ── Bottom navigation ─────────────────────────────────────────────
        BottomNavSection(
            modifier       = Modifier.align(Alignment.BottomCenter),
            isAddMenuOpen  = isAddMenuOpen,
            onAddClick     = { onAddMenuToggle(!isAddMenuOpen) },
            onHomeClick    = { /* already on Home */ },
            onProfileClick = onNavigateToProfile
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TopBarSection(userInitial: String = "J") {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .statusBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Text(text = "Sili",  style = MaterialTheme.typography.headlineLarge, color = Black)
                Text(text = "Guide", style = MaterialTheme.typography.headlineLarge, color = GreenPrimary)
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GreenBgLight)
                    .border(1.5.dp, GreenLight, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitial.uppercase(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSans,
                    color = GreenDark
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Border)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IOT DEVICES SECTION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun IoTDevicesSection(devices: List<IoTDevice>, onAddDevice: () -> Unit) {
    Column(modifier = Modifier.padding(top = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "IOT DEVICES",
                style = SectionLabelStyle,
                color = GrayMedium
            )
//            if (devices.isNotEmpty()) {
//                Text(
//                    text = "See all",
//                    style = SeeAllStyle,
//                    color = GreenPrimary,
//                    modifier = Modifier.clickable { onAddDevice() }
//                )
//            }
        }

        if (devices.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 14.dp)
                    .border(BorderStroke(1.dp, GrayLight), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .padding(horizontal = 3.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_iot_device),
                        contentDescription = "Add IoT",
                        modifier = Modifier.size(16.dp),
                        tint = GrayLight
                    )
                }
                Text(text = "No devices added — ", style = SeeAllStyle, color = GrayHint)
                Text(
                    text = "Add one",
                    style = SeeAllStyle,
                    color = GreenPrimary,
                    modifier = Modifier.clickable { onAddDevice() }
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                devices.forEach { device ->
                    IoTDeviceCard(
                        device = device,
                        modifier = Modifier.width(IotCardWidth)
                    )
                }

                // Add device card — width fixed, height wraps content
//                Box(
//                    modifier = Modifier
//                        .width(AddDeviceCardWidth)         // width only — height wraps
//                        .defaultMinSize(minHeight = 90.dp) // keeps it proportional
//                        .background(OffWhite, RoundedCornerShape(12.dp))
//                        .border(1.dp, GrayLight, RoundedCornerShape(12.dp))
//                        .clickable { onAddDevice() },
//                    contentAlignment = Alignment.Center
//                ) {
//                    Column(
//                        horizontalAlignment = Alignment.CenterHorizontally,
//                        verticalArrangement = Arrangement.spacedBy(4.dp),
//                        modifier = Modifier.padding(12.dp)
//                    ) {
//                        Box(
//                            modifier = Modifier
//                                .size(26.dp)
//                                .clip(CircleShape)
//                                .border(1.5.dp, GrayLight, CircleShape),
//                            contentAlignment = Alignment.Center
//                        ) {
//                            Text("+", fontSize = 14.sp, color = GrayHint, fontFamily = DMSans)
//                        }
//                        Text(
//                            text = "Add device",
//                            fontSize = 10.sp,
//                            fontFamily = DMSans,
//                            color = GrayHint,
//                            textAlign = TextAlign.Center
//                        )
//                    }
//                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// IOT DEVICE CARD  — width fixed, height wraps content
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun IoTDeviceCard(
    device: IoTDevice,
    modeLabel: String = device.connectionMode.name,
    modifier: Modifier = Modifier // The parent will pass the width here
) {
    Card(
        modifier = modifier.wrapContentHeight(), // Let the parent dictate width, but height wraps to content
        colors = CardDefaults.cardColors(containerColor = White),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(10.dp, 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), // Inner elements fill the card's width
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(GreenBgLight, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_iot_device),
                        contentDescription = "IoT Device",
                        modifier = Modifier.size(16.dp),
                        tint = GreenDark
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (device.isActive) GreenPrimary else GrayLight)
                )
            }

            Text(
                text = device.deviceName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis // Adds "..." if the name is too long!
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = modeLabel,
                    fontSize = 10.sp,
                    fontFamily = DMSans,
                    color = GrayMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = if (device.isActive) "Connected" else "Offline",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSans,
                    color = if (device.isActive) GreenPrimary else GrayHint
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PLANTS SECTION
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlantsSection(
    plants: List<PlantItem>,
    onSeeAll: () -> Unit,
    onNavigateToPlantDetail: (plantId: String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 14.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "MY PLANTS", style = SectionLabelStyle, color = GrayMedium)
            if (plants.isNotEmpty()) {
                Text(
                    text = "See all",
                    style = SeeAllStyle,
                    color = GreenPrimary,
                    modifier = Modifier.clickable { onSeeAll() }
                )
            }
        }

        if (plants.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PageFrameBg)
                        .border(1.5.dp, Border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_leaf_outlined),
                        contentDescription = "Leaf",
                        tint = GrayLight
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "No plants yet", style = MaterialTheme.typography.titleSmall, color = GrayBody)
                Text(
                    text = "Tap the + button below to add your\nfirst Siling Labuyo plant",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayHint,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )
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

// ─────────────────────────────────────────────────────────────────────────────
// PLANT CARD
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun PlantCard(plant: PlantItem, onNavigateToDetail: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetail() },
        colors = CardDefaults.cardColors(containerColor = White),
        shape  = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(GreenBgLight)
                    .border(1.dp, GreenLight, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_leaf),
                    contentDescription = "Leaf",
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = plant.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Black,
                    maxLines = 1
                )
                if (plant.condition.status == "none") {
                    Text(
                        text = "Not yet analyzed",
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayHint,
                        fontStyle = FontStyle.Italic
                    )
                } else {
                    Text(
                        text = plant.condition.lastDiagnosedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayBody
                    )
                }
                DeviceBadge(
                    deviceName  = plant.condition.deviceName,
                    isConnected = plant.condition.isDeviceConnected
                )
            }

            ConditionBadge(status = plant.condition.status, label = plant.condition.label)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DEVICE BADGE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DeviceBadge(deviceName: String?, isConnected: Boolean) {
    val sharedTextStyle = LocalTextStyle.current.copy(
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
    if (deviceName != null) {
        Row(
            modifier = Modifier
                .background(
                    color = if (isConnected) GreenBgLight else OffWhite,
                    shape = RoundedCornerShape(5.dp)
                )
                .padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) GreenPrimary else GrayLight)
            )
            Text(
                text = deviceName,
                fontSize = 9.sp,
                lineHeight = 9.sp,
                style = sharedTextStyle,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = if (isConnected) GreenDark else GrayHint
            )
        }
    } else {
        Box(
            modifier = Modifier
                .background(OffWhite, RoundedCornerShape(5.dp))
                .padding(horizontal = 6.dp, vertical = 1.5.dp)
        ) {
            Text(
                text = "No device",
                fontSize = 9.sp,
                lineHeight = 9.sp,
                style = sharedTextStyle,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = GrayHint
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CONDITION BADGE
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ConditionBadge(status: String, label: String = "") {
    val (bgColor, textColor) = when (status) {
        "healthy" -> GreenBgLight to GreenDark
        "warning" -> WarningBg   to Warning
        "danger"  -> DangerBg    to Danger
        else      -> OffWhite    to GrayMedium
    }
    val displayLabel = when {
        label.isNotEmpty()  -> label
        status == "healthy" -> "Healthy"
        status == "warning" -> "Warning"
        status == "danger"  -> "Danger"
        else                -> "—"
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(text = displayLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = DMSans, color = textColor)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM NAV  — height wraps content, insets handled internally
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BottomNavSection(
    modifier: Modifier = Modifier,
    isAddMenuOpen: Boolean,
    onAddClick: () -> Unit,
    onHomeClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    Surface(
        modifier       = modifier.fillMaxWidth(),
        color          = White,
        shadowElevation = 8.dp,
        tonalElevation  = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()          // system gesture bar
                .height(BottomNavHeight)          // fixed design height above inset
                .padding(horizontal = 28.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Home (active)
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onHomeClick
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(painter = painterResource(R.drawable.ic_home), contentDescription = "Home", tint = GreenPrimary, modifier = Modifier.size(20.dp))
                Text(text = "Home", style = NavLabelActiveStyle, color = GreenPrimary)
            }

            // Add button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isAddMenuOpen) GreenDark else GreenPrimary)
                    .then(
                        if (isAddMenuOpen)
                            Modifier.border(4.dp, GreenPrimary.copy(alpha = 0.18f), CircleShape)
                        else Modifier
                    )
                    .clickable { onAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = if (isAddMenuOpen) {
                        painterResource(R.drawable.ic_close)
                    } else {
                        painterResource(R.drawable.ic_add)
                    },
                    contentDescription = if (isAddMenuOpen) "Close" else "Add",
                    tint = White,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.Center)
                )
            }

            // Profile (inactive)
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onProfileClick
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(painter = painterResource(R.drawable.ic_profile), contentDescription = "Profile", tint = GrayLight, modifier = Modifier.size(20.dp))
                Text(text = "Profile", style = NavLabelStyle, color = GrayHint)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM SHEET CONTENT
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BottomSheetContent(onAddPlant: () -> Unit, onAddIoT: () -> Unit, onCancel: () -> Unit) {
    val offsetY      = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (offsetY.value > 150f) {
                            onCancel()
                        } else {
                            coroutineScope.launch {
                                offsetY.animateTo(0f, animationSpec = tween(300))
                            }
                        }
                    }
                ) { change, dragAmount ->
                    change.consume()
                    if (offsetY.value + dragAmount >= 0) {
                        coroutineScope.launch { offsetY.snapTo(offsetY.value + dragAmount) }
                    }
                }
            },
        colors = CardDefaults.cardColors(containerColor = White),
        shape  = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(GrayLight, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(text = "What would you like to add?", style = MaterialTheme.typography.titleLarge, color = Black, modifier = Modifier.padding(bottom = 4.dp))
            Text(text = "Choose an option below to get started.", style = MaterialTheme.typography.bodySmall, color = GrayMedium, lineHeight = 18.sp, modifier = Modifier.padding(bottom = 18.dp))
            SheetOptionItem(icon = "🌱", title = "Add New Plant", description = "Create a new Siling Labuyo plant entry and optionally link a sensor device", onClick = onAddPlant)
            Spacer(modifier = Modifier.height(10.dp))
            SheetOptionItem(icon = "📡", title = "Add IoT Device", description = "Connect a new ESP32 sensor device via AP hotspot or home WiFi network", onClick = onAddIoT)
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text(text = "Cancel", style = MaterialTheme.typography.titleSmall, color = GrayBody)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHEET OPTION ITEM
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun SheetOptionItem(icon: String, title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors   = CardDefaults.cardColors(containerColor = PageFrameBg),
        shape    = RoundedCornerShape(14.dp),
        border   = BorderStroke(1.dp, Border)
    ) {
        Row(
            modifier = Modifier.padding(16.dp, 14.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(44.dp).background(GreenBgLight, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Text(icon, fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = Black)
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = GrayMedium, lineHeight = 15.4.sp)
            }
            Text("›", fontSize = 18.sp, color = GrayHint, fontFamily = DMSans)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TEST IOT SCREEN  (dev / debug — replace with real monitoring UI)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TestIoTScreen() {
    val viewModel: IoTViewModel = hiltViewModel()
    DisposableEffect(Unit) {
        viewModel.startLiveMonitoring()
        onDispose { viewModel.stopLiveMonitoring() }
    }
    when {
        viewModel.isLoading          -> Text("Loading...")
        viewModel.error != null      -> Text(viewModel.error!!)
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

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────
@Preview(showBackground = true, name = "Dashboard - Populated")
@Composable
fun DashboardPreview() {
    SiliGuideTheme {
        DashboardContent(
            plants = listOf(
                PlantItem("1", "Siling Labuyo 1", PlantCondition("healthy", "Healthy", "Sensor A", true, "Oct 24, 2023")),
                PlantItem("2", "Garden Bed 2",    PlantCondition("none",    "",        null,       false, "Oct 25, 2023"))
            ),
            devices = listOf(
                IoTDevice("1","1","Sensor A","AAA",true, "10.1.1.1", ConnectionMode.LAN,false, 24052026),
                IoTDevice("2","1","Sensor B","BBB",false,"10.1.1.1", ConnectionMode.BLUETOOTH, false, 23052026)
            ),
            isAddMenuOpen = false, onAddMenuToggle = {},
            onNavigateToCamera = {}, onNavigateToAddPlant = {}, onNavigateToAddIoT = {},
            onNavigateToPlantDetail = {}, onNavigateToSeeAll = {}, onNavigateToProfile = {}
        )
    }
}

@Preview(showBackground = true, name = "Dashboard - Empty")
@Composable
fun DashboardEmptyPreview() {
    SiliGuideTheme {
        DashboardContent(
            plants = emptyList(), devices = emptyList(),
            isAddMenuOpen = false, onAddMenuToggle = {},
            onNavigateToCamera = {}, onNavigateToAddPlant = {}, onNavigateToAddIoT = {},
            onNavigateToPlantDetail = {}, onNavigateToSeeAll = {}, onNavigateToProfile = {}
        )
    }
}