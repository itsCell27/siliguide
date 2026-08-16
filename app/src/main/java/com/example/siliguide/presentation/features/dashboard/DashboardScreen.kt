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
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.presentation.features.auth.ProfileAuthProvider
import com.example.siliguide.presentation.features.auth.ProfileContent
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

// ── Layout constants ──────────────────────────────────────────────────────────
private val BottomNavHeight  = 68.dp
private val IotCardWidth     = 150.dp
private val AddDeviceCardWidth = 100.dp

// Dashboard is a summary, not a full list — "See all" carries the rest
private const val DashboardDeviceLimit = 2
private const val DashboardPlantLimit  = 3

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
    /** "healthy" | "warning" | "danger" | "none" — drives the badge colors. */
    val status: String,
    val label: String = "",
    val deviceName: String?,
    val isDeviceConnected: Boolean = false,
    /** Epoch millis of the newest analysis, or null when never analyzed. */
    val lastAnalyzedAt: Long? = null,
    /** True when the analysis ran without sensor data. */
    val isPartial: Boolean = false,
    val imagePath: String? = null
)

data class PlantItem(
    val id: String,
    val name: String,
    val condition: PlantCondition
)

/**
 * Which body the shared shell (top bar + scroll area) currently shows.
 * Home <-> Profile is a pure state flip now — no NavGraph round trip, no
 * back-stack growth, and the Add-button bottom sheet is shared by both
 * since it lives in this same Box regardless of tab.
 */
enum class DashboardTab { HOME, PROFILE }

// ─────────────────────────────────────────────────────────────────────────────
// DASHBOARD SCREEN  (entry point — Hilt lives here only)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    onNavigateToCamera: () -> Unit,
    onNavigateToAddPlant: () -> Unit,
    onNavigateToAddIoT: () -> Unit,
    onNavigateToPlantDetail: (plantId: String) -> Unit,
    onNavigateToDeviceDetail: (deviceId: String) -> Unit = {},
    onNavigateToSeeAllPlants: () -> Unit,
    onNavigateToSeeAllDevices: () -> Unit = {},
    /** Log Out, Guest Log Out, and Delete Account all resolve to the same
     *  place from the nav graph's point of view: leave the app shell,
     *  land on Welcome with the stack cleared. The three confirm actions
     *  below stay separate so their (currently TODO) business logic can
     *  diverge later without touching the caller again. */
    onSignedOut: () -> Unit = {},
    onCreateAccountWithEmail: () -> Unit = {},
    onCreateAccountWithGoogle: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var isAddMenuOpen by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(DashboardTab.HOME) }

    // TODO: swap these placeholder fields for AuthViewModel's real
    // session/profile state (display name, email, provider) once exposed
    val profileDisplayName = "Juan Dela Cruz"
    val profileEmail: String? = "juan.delacruz@email.com"
    val profileAuthProvider = ProfileAuthProvider.EMAIL

    val displayPlants = uiState.plants.map { domainPlant ->

        val linkedDevice = uiState.devices.find { it.deviceId == domainPlant.deviceId }
        val session = uiState.latestSessions[domainPlant.plantId]

        PlantItem(
            id = domainPlant.plantId,
            name = domainPlant.name,
            condition = PlantCondition(
                status = session.toStatus(),
                label = session.toLabel(),
                deviceName = linkedDevice?.deviceName,
                isDeviceConnected = linkedDevice
                    ?.let { uiState.deviceOnline[it.deviceId] } ?: false,
                lastAnalyzedAt = session?.sessionTimestamp,
                isPartial = session?.isPartial ?: false,
                imagePath = session?.imagePath
            )
        )
    }

    DashboardContent(
        plants              = displayPlants,
        devices             = uiState.devices,
        deviceOnline        = uiState.deviceOnline,
        isRefreshing        = isRefreshing,
        onRefresh           = { viewModel.refresh() },
        isAddMenuOpen       = isAddMenuOpen,
        onAddMenuToggle     = { isAddMenuOpen = it },
        selectedTab         = selectedTab,
        onTabChange         = { selectedTab = it },
        onNavigateToCamera  = onNavigateToCamera,
        onNavigateToAddPlant= onNavigateToAddPlant,
        onNavigateToAddIoT  = onNavigateToAddIoT,
        onNavigateToPlantDetail = onNavigateToPlantDetail,
        onNavigateToDeviceDetail = onNavigateToDeviceDetail,
        onNavigateToSeeAllPlants  = onNavigateToSeeAllPlants,
        onNavigateToSeeAllDevices = onNavigateToSeeAllDevices,
        profileDisplayName  = profileDisplayName,
        profileEmail        = profileEmail,
        profileAuthProvider = profileAuthProvider,
        onSaveDisplayName = {
            /* TODO: AuthViewModel — persist new display name */
        },
        onConfirmLogOut = {
            /* TODO: AuthViewModel — sign out */
            onSignedOut()
        },
        onConfirmGuestLogOut = {
            /* TODO: AuthViewModel — wipe guest data, sign out */
            onSignedOut()
        },
        onConfirmDeleteAccount = {
            /* TODO: AuthViewModel — delete account */
            onSignedOut()
        },
        onCreateAccountWithEmail = onCreateAccountWithEmail,
        onCreateAccountWithGoogle = onCreateAccountWithGoogle
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DASHBOARD CONTENT  (Hilt-free, previewable)
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    plants: List<PlantItem>,
    devices: List<IoTDevice>,
    deviceOnline: Map<String, Boolean>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    isAddMenuOpen: Boolean,
    onAddMenuToggle: (Boolean) -> Unit,
    selectedTab: DashboardTab,
    onTabChange: (DashboardTab) -> Unit,
    onNavigateToCamera: () -> Unit,
    onNavigateToAddPlant: () -> Unit,
    onNavigateToAddIoT: () -> Unit,
    onNavigateToPlantDetail: (String) -> Unit,
    onNavigateToDeviceDetail: (String) -> Unit = {},
    onNavigateToSeeAllPlants: () -> Unit,
    onNavigateToSeeAllDevices: () -> Unit,
    profileDisplayName: String = "",
    profileEmail: String? = null,
    profileAuthProvider: ProfileAuthProvider = ProfileAuthProvider.EMAIL,
    profileAppVersion: String = "v1.0.0",
    onSaveDisplayName: (String) -> Unit = {},
    onConfirmLogOut: () -> Unit = {},
    onConfirmGuestLogOut: () -> Unit = {},
    onConfirmDeleteAccount: () -> Unit = {},
    onCreateAccountWithEmail: () -> Unit = {},
    onCreateAccountWithGoogle: () -> Unit = {}
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
            .safeDrawingPadding()
    ) {
        // ── Scrollable body — swaps per tab, shell (scrim/sheet/nav) stays put ──
        Column(modifier = Modifier.fillMaxSize()) {
            when (selectedTab) {
                DashboardTab.HOME -> {
                    TopBarSection(onAvatarClick = { onTabChange(DashboardTab.PROFILE) })

                    // Wraps only the scrolling area, so the top bar stays put while
                    // the indicator pulls down over the content
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = scrollBodyBottomPadding)
                        ) {
                            IoTDevicesSection(
                                devices = devices,
                                deviceOnline = deviceOnline,
                                onAddDevice = onNavigateToAddIoT,
                                onDeviceClick = onNavigateToDeviceDetail,
                                onSeeAllDevices = onNavigateToSeeAllDevices
                            )
                            PlantsSection(
                                plants = plants,
                                onSeeAll = onNavigateToSeeAllPlants,
                                onNavigateToPlantDetail = onNavigateToPlantDetail
                            )
                        }
                    }
                }

                DashboardTab.PROFILE -> {
                    ProfileContent(
                        displayName = profileDisplayName,
                        email = profileEmail,
                        authProvider = profileAuthProvider,
                        appVersion = profileAppVersion,
                        onSaveDisplayName = onSaveDisplayName,
                        onConfirmLogOut = onConfirmLogOut,
                        onConfirmGuestLogOut = onConfirmGuestLogOut,
                        onConfirmDeleteAccount = onConfirmDeleteAccount,
                        onCreateAccountWithEmail = onCreateAccountWithEmail,
                        onCreateAccountWithGoogle = onCreateAccountWithGoogle
                    )
                }
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

        // ── Bottom sheet — same plant-vs-device chooser regardless of tab ──
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

        // ── Bottom navigation — Home/Profile are a state flip, not a route ──
        BottomNavSection(
            modifier       = Modifier.align(Alignment.BottomCenter),
            isAddMenuOpen  = isAddMenuOpen,
            activeTab      = selectedTab,
            onAddClick     = { onAddMenuToggle(!isAddMenuOpen) },
            onHomeClick    = { onTabChange(DashboardTab.HOME) },
            onProfileClick = { onTabChange(DashboardTab.PROFILE) }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun TopBarSection(
    userInitial: String = "J",
    onAvatarClick: () -> Unit = {}
) {
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
                Text(
                    text = "Sili",
                    style = MaterialTheme.typography.headlineLarge,
                    color = Black
                )
                Text(
                    text = "Guide",
                    style = MaterialTheme.typography.headlineLarge,
                    color = GreenPrimary
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(GreenBgLight)
                    .border(1.5.dp, GreenLight, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAvatarClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userInitial,
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
fun IoTDevicesSection(
    devices: List<IoTDevice>,
    deviceOnline: Map<String, Boolean>,
    onAddDevice: () -> Unit,
    onDeviceClick: (String) -> Unit = {},
    onSeeAllDevices: () -> Unit = {}
) {
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
            if (devices.isNotEmpty()) {
                Text(
                    text = "See all",
                    style = SeeAllStyle,
                    color = GreenPrimary,
                    modifier = Modifier.clickable { onSeeAllDevices() }
                )
            }
        }

        if (devices.isEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 14.dp)
                    .background(White, RoundedCornerShape(12.dp))
                    .dashedBorder(GrayLight, 12.dp)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_iot_device),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = GrayLight
                )
                Text(text = "No devices added —", style = SeeAllStyle, color = GrayHint)
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
                devices.take(DashboardDeviceLimit).forEach { device ->
                    IoTDeviceCard(
                        device = device,
                        isOnline = deviceOnline[device.deviceId] ?: device.isActive,
                        onClick = { onDeviceClick(device.deviceId) },
                        modifier = Modifier.width(IotCardWidth)
                    )
                }

                AddDeviceCard(onClick = onAddDevice)
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
    isOnline: Boolean,
    onClick: () -> Unit = {},
    modeLabel: String = device.connectionMode.displayLabel(),
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .wrapContentHeight()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = White),
        shape  = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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
                    Icon(
                        painter = painterResource(R.drawable.ic_iot_device),
                        contentDescription = "IoT Device",
                        modifier = Modifier.size(16.dp),
                        tint = GreenPrimary
                    )
                }
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isOnline) GreenPrimary else GrayLight)
                )
            }

            Text(
                text = device.deviceName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = DMSans,
                color = Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
                    text = if (isOnline) "Connected" else "Offline",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = DMSans,
                    color = if (isOnline) GreenPrimary else GrayHint
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ADD DEVICE CARD  — dashed tile pinned to the end of the IoT row
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun AddDeviceCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(AddDeviceCardWidth)
            .defaultMinSize(minHeight = 90.dp)
            .background(OffWhite, RoundedCornerShape(12.dp))
            .dashedBorder(GrayLight, 12.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .dashedBorder(GrayLight, 13.dp, strokeWidth = 1.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    tint = GrayHint,
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = "Add device",
                fontSize = 10.sp,
                fontFamily = DMSans,
                color = GrayHint,
                textAlign = TextAlign.Center
            )
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
                        tint = GrayLight,
                        modifier = Modifier.size(40.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "No plants yet",
                    style = MaterialTheme.typography.titleSmall,
                    color = GrayBody
                )
                Text(
                    text = "Tap the + button below to add your\nfirst Siling Labuyo plant",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayHint,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(horizontal = 22.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                plants.take(DashboardPlantLimit).forEach { plant ->
                    PlantCard(
                        plant = plant,
                        onNavigateToDetail = { onNavigateToPlantDetail(plant.id) },
                        compact = true
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
fun PlantCard(plant: PlantItem, onNavigateToDetail: () -> Unit, compact: Boolean = false) {

    val thumbSize = if (compact) 55.dp else 60.dp
    val thumbRadius = if (compact) 10.dp else 11.dp

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
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!plant.condition.imagePath.isNullOrEmpty() && File(plant.condition.imagePath).exists()) {
                AsyncImage(
                    model = File(plant.condition.imagePath),
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

                val analyzedAt = plant.condition.lastAnalyzedAt

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
                        text = plant.condition.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = GrayBody
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DEVICE BADGE
//
// No longer used by PlantCard — the updated mockup drops the linked-device chip
// from the plant row. Kept for the plant detail screen.
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
        "healthy" -> StatusColors.HealthyBg to StatusColors.HealthyText
        "warning" -> WarningBg              to StatusColors.WarningText
        "danger"  -> DangerBg               to StatusColors.DangerText
        else      -> StatusColors.NoneBg    to StatusColors.NoneText
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
            .width(35.dp)
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

// ─────────────────────────────────────────────────────────────────────────────
// BOTTOM NAV  — height wraps content, insets handled internally
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun BottomNavSection(
    modifier: Modifier = Modifier,
    isAddMenuOpen: Boolean,
    activeTab: DashboardTab,
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
                .navigationBarsPadding()
                .height(BottomNavHeight)
                .padding(horizontal = 28.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isHomeActive = activeTab == DashboardTab.HOME
            val isProfileActive = activeTab == DashboardTab.PROFILE

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
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = "Home",
                    tint = if (isHomeActive) GreenPrimary else GrayLight,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Home",
                    style = if (isHomeActive) NavLabelActiveStyle else NavLabelStyle,
                    color = if (isHomeActive) GreenPrimary else GrayHint
                )
            }

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
                Icon(
                    painter = painterResource(R.drawable.ic_profile),
                    contentDescription = "Profile",
                    tint = if (isProfileActive) GreenPrimary else GrayLight,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Profile",
                    style = if (isProfileActive) NavLabelActiveStyle else NavLabelStyle,
                    color = if (isProfileActive) GreenPrimary else GrayHint
                )
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
            Text(
                text = "What would you like to add?",
                style = MaterialTheme.typography.titleLarge,
                color = Black,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Choose an option below to get started.",
                style = MaterialTheme.typography.bodySmall,
                color = GrayMedium,
                lineHeight = 18.sp,
                modifier = Modifier.padding(bottom = 18.dp)
            )
            SheetOptionItem(
                icon = "🌱",
                title = "Add New Plant",
                description = "Create a new Siling Labuyo plant entry and optionally link a sensor device",
                onClick = onAddPlant
            )
            Spacer(modifier = Modifier.height(10.dp))
            SheetOptionItem(
                icon = "📡",
                title = "Add IoT Device",
                description = "Connect a new ESP32 sensor device via AP hotspot or home WiFi network",
                onClick = onAddIoT
            )
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
            Box(
                modifier = Modifier.size(44.dp).background(GreenBgLight, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall, color = Black)
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMedium,
                    lineHeight = 15.4.sp
                )
            }
            Text("›", fontSize = 18.sp, color = GrayHint, fontFamily = DMSans)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Badge severity for a plant's newest analysis.
 *
 * A partial analysis ran without sensor readings, so the rule engine never
 * produced a plantCondition — the leaf symptom alone is all there is to show.
 */
private fun AnalysisSession?.toStatus(): String {

    if (this == null) return "none"

    return if (predictedClass.equals("healthy", ignoreCase = true)) {
        "healthy"
    } else {
        "warning"
    }
}

/** Rule-engine condition when available, otherwise the leaf symptom. */
private fun AnalysisSession?.toLabel(): String {

    if (this == null) return ""

    plantCondition?.takeIf { it.isNotBlank() }?.let { return it }

    return predictedClass.humanizeSymptom()
}

/** "leaf_curling" → "Leaf Curling" */
private fun String.humanizeSymptom(): String =
    split('_', '-', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }

/**
 * Relative for anything from the last week, absolute date beyond that.
 */
fun formatLastAnalyzed(millis: Long): String {

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

/** "LAN Mode" / "AP Mode" / "Bluetooth Mode", as the card shows it. */
private fun ConnectionMode.displayLabel(): String = when (this) {
    ConnectionMode.LAN -> "LAN Mode"
    ConnectionMode.AP -> "AP Mode"
    ConnectionMode.BLUETOOTH -> "BT Mode"
}

/**
 * Dashed rounded outline.
 *
 * Compose has no dashed variant of Modifier.border, so the stroke is drawn
 * manually. Used by the empty-devices row and the Add device tile.
 */
private fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp,
    strokeWidth: Dp = 1.dp,
    dashLength: Dp = 4.dp,
    gapLength: Dp = 4.dp
): Modifier = this.drawBehind {

    val stroke = strokeWidth.toPx()
    val inset = stroke / 2f

    drawRoundRect(
        color = color,
        topLeft = Offset(inset, inset),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = stroke,
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(dashLength.toPx(), gapLength.toPx()),
                0f
            )
        )
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PREVIEWS
// ─────────────────────────────────────────────────────────────────────────────
private val previewDevices = listOf(
    IoTDevice("1", "1", "Garden Sensor", "AAA", true, "192.168.1.42", ConnectionMode.LAN, "1.0.0", false, 24052026),
    IoTDevice("2", "1", "Pot Sensor 2", "BBB", false, "192.168.4.1", ConnectionMode.AP, "1.0.0", false, 23052026)
)

private val previewOnline = mapOf("1" to true, "2" to false)

@Preview(showBackground = true, name = "5C — Full state", widthDp = 360, heightDp = 740)
@Composable
fun DashboardPreview() {
    SiliGuideTheme {
        DashboardContent(
            plants = listOf(
                PlantItem(
                    "1", "Labuyo Plant #1",
                    PlantCondition(
                        status = "healthy",
                        label = "Healthy",
                        deviceName = "Garden Sensor",
                        isDeviceConnected = true,
                        lastAnalyzedAt = System.currentTimeMillis() - 12 * 60_000
                    )
                ),
                PlantItem(
                    "2", "Backyard Sili",
                    PlantCondition(
                        status = "warning",
                        label = "Underwatering",
                        deviceName = null,
                        isDeviceConnected = false,
                        lastAnalyzedAt = System.currentTimeMillis() - 30L * 86_400_000
                    )
                ),
                PlantItem(
                    "3", "Pot #3",
                    PlantCondition(
                        status = "warning",
                        label = "Leaf Spots",
                        deviceName = null,
                        isDeviceConnected = false,
                        lastAnalyzedAt = System.currentTimeMillis() - 3 * 3_600_000,
                        isPartial = true
                    )
                )
            ),
            devices = previewDevices,
            deviceOnline = previewOnline,
            isRefreshing = false,
            onRefresh = {},
            isAddMenuOpen = false, onAddMenuToggle = {},
            selectedTab = DashboardTab.HOME, onTabChange = {},
            onNavigateToCamera = {}, onNavigateToAddPlant = {}, onNavigateToAddIoT = {},
            onNavigateToPlantDetail = {}, onNavigateToSeeAllPlants = {},
            onNavigateToSeeAllDevices = {}
        )
    }
}

@Preview(showBackground = true, name = "5B — Plants only", widthDp = 360, heightDp = 740)
@Composable
fun DashboardPlantsOnlyPreview() {
    SiliGuideTheme {
        DashboardContent(
            plants = listOf(
                PlantItem("1", "Labuyo Plant #1", PlantCondition("none", "", null, false, null)),
                PlantItem(
                    "2", "Backyard Sili",
                    PlantCondition(
                        "healthy", "Healthy", null, false,
                        System.currentTimeMillis() - 3 * 3_600_000
                    )
                )
            ),
            devices = emptyList(),
            deviceOnline = emptyMap(),
            isRefreshing = false,
            onRefresh = {},
            isAddMenuOpen = false, onAddMenuToggle = {},
            selectedTab = DashboardTab.HOME, onTabChange = {},
            onNavigateToCamera = {}, onNavigateToAddPlant = {}, onNavigateToAddIoT = {},
            onNavigateToPlantDetail = {}, onNavigateToSeeAllPlants = {},
            onNavigateToSeeAllDevices = {}
        )
    }
}

@Preview(showBackground = true, name = "5A — Empty state", widthDp = 360, heightDp = 740)
@Composable
fun DashboardEmptyPreview() {
    SiliGuideTheme {
        DashboardContent(
            plants = emptyList(), devices = emptyList(),
            deviceOnline = emptyMap(),
            isRefreshing = false,
            onRefresh = {},
            isAddMenuOpen = false, onAddMenuToggle = {},
            selectedTab = DashboardTab.HOME, onTabChange = {},
            onNavigateToCamera = {}, onNavigateToAddPlant = {}, onNavigateToAddIoT = {},
            onNavigateToPlantDetail = {}, onNavigateToSeeAllPlants = {},
            onNavigateToSeeAllDevices = {}
        )
    }
}

@Preview(showBackground = true, name = "Refreshing", widthDp = 360, heightDp = 740)
@Composable
fun DashboardRefreshingPreview() {
    SiliGuideTheme {
        DashboardContent(
            plants = listOf(
                PlantItem(
                    "1", "Labuyo Plant #1",
                    PlantCondition(
                        "healthy", "Healthy", null, false,
                        System.currentTimeMillis() - 12 * 60_000
                    )
                )
            ),
            devices = previewDevices,
            deviceOnline = previewOnline,
            isRefreshing = true,
            onRefresh = {},
            isAddMenuOpen = false, onAddMenuToggle = {},
            selectedTab = DashboardTab.HOME, onTabChange = {},
            onNavigateToCamera = {}, onNavigateToAddPlant = {}, onNavigateToAddIoT = {},
            onNavigateToPlantDetail = {}, onNavigateToSeeAllPlants = {},
            onNavigateToSeeAllDevices = {}
        )
    }
}