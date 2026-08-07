package com.example.siliguide.presentation.features.iot.lan_setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.DMSans
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GrayHint
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.domain.model.WifiNetwork
import com.example.siliguide.domain.model.WifiSignal
import com.example.siliguide.presentation.features.iot.lan_setup.components.CheckCircleIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.EyeIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.LanWizardScaffold
import com.example.siliguide.presentation.features.iot.lan_setup.components.RefreshIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardBody
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardDashedDivider
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardHeading
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPrimaryButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardUppercaseLabel
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardWaitingButton

/**
 * Step 2 of 4 — Connect to Home Wi-Fi.
 *
 * Credentials are collected once here. Both provisioning paths (AP and
 * Bluetooth) later deliver these same credentials to the ESP32.
 */
sealed interface LanWifiUiState {

    /** Scan in flight. */
    data object Scanning : LanWifiUiState

    /** Scan returned nothing. */
    data object Empty : LanWifiUiState

    /**
     * Scanning is impossible right now — permission revoked, Location off, or
     * Wi-Fi off. The matching dialog is raised separately; this is the copy
     * shown inside the card so the screen never looks blank.
     */
    data class Unavailable(val message: String) : LanWifiUiState

    /** Networks available; one may be selected and its password entered. */
    data class Ready(
        val networks: List<WifiNetwork>,
        val selectedSsid: String? = null,
        val password: String = "",
        val passwordVisible: Boolean = false
    ) : LanWifiUiState
}

@Composable
fun LanWifiSelectionScreen(
    state: LanWifiUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSelectNetwork: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedNetwork = (state as? LanWifiUiState.Ready)
        ?.let { ready -> ready.networks.firstOrNull { it.ssid == ready.selectedSsid } }

    // Open networks need no password
    val canContinue = state is LanWifiUiState.Ready &&
            selectedNetwork != null &&
            (selectedNetwork.isOpen || state.password.isNotEmpty())

    LanWizardScaffold(
        flowLabel = "LAN Setup",
        stepLabel = "Step 2 of 4",
        progress = 0.5f,
        onBack = onBack,
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(9.dp),
        bottomBar = {
            if (canContinue) {
                WizardPrimaryButton(text = "Continue", onClick = onContinue)
            } else {
                WizardWaitingButton(text = "Continue")
            }
        }
    ) {
        WizardHeading(text = "Select your WiFi")
        WizardBody(text = "Choose the network your IoT device will join.")

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(13.dp))
                .background(White)
                .border(1.dp, Border, RoundedCornerShape(13.dp))
        ) {
            when (state) {

                LanWifiUiState.Scanning -> {
                    SectionHeader(label = "Available networks", onRefresh = onRefresh)
                    PlaceholderRow(text = "Scanning for networks…")
                }

                LanWifiUiState.Empty -> {
                    SectionHeader(label = "Available networks", onRefresh = onRefresh)
                    PlaceholderRow(text = "No networks found. Tap refresh to scan again.")
                }

                is LanWifiUiState.Unavailable -> {
                    SectionHeader(label = "Available networks", onRefresh = onRefresh)
                    PlaceholderRow(text = state.message)
                }

                is LanWifiUiState.Ready -> {
                    val selected = selectedNetwork
                    val others = state.networks.filter { it.ssid != state.selectedSsid }

                    SectionHeader(
                        label = if (selected != null) "Selected network" else "Available networks",
                        onRefresh = onRefresh
                    )

                    if (selected != null) {
                        SelectedNetworkRow(
                            ssid = selected.ssid,
                            onClick = { onSelectNetwork(selected.ssid) }
                        )

                        if (!selected.isOpen) {
                            PasswordPanel(
                                password = state.password,
                                passwordVisible = state.passwordVisible,
                                onPasswordChange = onPasswordChange,
                                onToggleVisibility = onTogglePasswordVisibility
                            )
                        }
                    }

                    if (others.isNotEmpty()) {
                        if (selected != null) {
                            SubSectionHeader(label = "Other available networks")
                        }

                        others.forEachIndexed { index, network ->
                            NetworkRow(
                                ssid = network.ssid,
                                showDivider = index != others.lastIndex,
                                onClick = { onSelectNetwork(network.ssid) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Card header — uppercase label plus a refresh affordance. */
@Composable
private fun SectionHeader(
    label: String,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OffWhite)
            .drawBehind {
                drawLine(
                    color = Border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WizardUppercaseLabel(text = label)
        Spacer(Modifier.weight(1f))
        Box(Modifier.clickable(onClick = onRefresh)) { RefreshIcon() }
    }
}

/** Divider header separating the selection from the rest of the scan. */
@Composable
private fun SubSectionHeader(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(OffWhite)
            .drawBehind {
                drawLine(
                    color = Border,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        WizardUppercaseLabel(text = label)
    }
}

@Composable
private fun SelectedNetworkRow(
    ssid: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenBgLight)
            .clickable(onClick = onClick)
            .drawBehind {
                drawLine(
                    color = Border,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = ssid,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Black,
            modifier = Modifier.weight(1f)
        )

        CheckCircleIcon()
    }
}

@Composable
private fun NetworkRow(
    ssid: String,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .clickable(onClick = onClick)
            .then(
                if (showDivider) Modifier.drawBehind {
                    drawLine(
                        color = Border,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = ssid,
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = GrayBody,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlaceholderRow(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .padding(horizontal = 16.dp, vertical = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = GrayMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PasswordPanel(
    password: String,
    passwordVisible: Boolean,
    onPasswordChange: (String) -> Unit,
    onToggleVisibility: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GreenBgLight)
    ) {
        WizardDashedDivider()

        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            WizardUppercaseLabel(text = "Password", color = GreenDark)

            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(White)
                    .border(1.5.dp, GreenPrimary, RoundedCornerShape(9.dp))
                    .padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = DMSans,
                        fontSize = 13.sp,
                        color = Black,
                        letterSpacing = if (passwordVisible) 0.sp else 2.sp
                    ),
                    visualTransformation =
                        if (passwordVisible) VisualTransformation.None
                        else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(GreenPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (password.isEmpty()) {
                                Text(
                                    text = "Enter WiFi password",
                                    fontSize = 13.sp,
                                    color = GrayHint
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Box(Modifier.clickable(onClick = onToggleVisibility)) {
                    EyeIcon(color = if (passwordVisible) GreenPrimary else GrayBody)
                }
            }
        }
    }
}

/* ── Previews ──────────────────────────────────────────────────────────── */

private val previewNetworks = listOf(
    WifiNetwork("Home_2.4GHz", "WPA2", WifiSignal.STRONG, -48),
    WifiNetwork("PLDT_HOME", "WPA2", WifiSignal.STRONG, -55),
    WifiNetwork("Neighbor_Net", null, WifiSignal.WEAK, -82),
    WifiNetwork("Home_5GHz", "WPA3", WifiSignal.MEDIUM, -66)
)

private val previewReady = LanWifiUiState.Ready(
    networks = previewNetworks,
    selectedSsid = "Home_2.4GHz",
    password = "12345678"
)

@Preview(
    name = "Select + password · Small 320x640",
    showSystemUi = true,
    device = "spec:width=320dp,height=640dp,dpi=320"
)
@Composable
private fun LanWifiSelectionSmallPreview() {
    SiliGuideTheme {
        LanWifiSelectionScreen(
            state = previewReady,
            onBack = {},
            onRefresh = {},
            onSelectNetwork = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "Select + password · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanWifiSelectionStandardPreview() {
    SiliGuideTheme {
        LanWifiSelectionScreen(
            state = previewReady,
            onBack = {},
            onRefresh = {},
            onSelectNetwork = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "Select + password · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanWifiSelectionLargePreview() {
    SiliGuideTheme {
        LanWifiSelectionScreen(
            state = previewReady,
            onBack = {},
            onRefresh = {},
            onSelectNetwork = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "Password visible 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanWifiSelectionPasswordVisiblePreview() {
    SiliGuideTheme {
        LanWifiSelectionScreen(
            state = previewReady.copy(passwordVisible = true),
            onBack = {},
            onRefresh = {},
            onSelectNetwork = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "No selection 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanWifiSelectionNoSelectionPreview() {
    SiliGuideTheme {
        LanWifiSelectionScreen(
            state = LanWifiUiState.Ready(networks = previewNetworks),
            onBack = {},
            onRefresh = {},
            onSelectNetwork = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "Scanning 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanWifiSelectionScanningPreview() {
    SiliGuideTheme {
        LanWifiSelectionScreen(
            state = LanWifiUiState.Scanning,
            onBack = {},
            onRefresh = {},
            onSelectNetwork = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "Unavailable 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanWifiSelectionUnavailablePreview() {
    SiliGuideTheme {
        LanWifiSelectionScreen(
            state = LanWifiUiState.Unavailable(
                "WiFi is turned off. Turn it on to scan for networks."
            ),
            onBack = {},
            onRefresh = {},
            onSelectNetwork = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "Empty 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanWifiSelectionEmptyPreview() {
    SiliGuideTheme {
        LanWifiSelectionScreen(
            state = LanWifiUiState.Empty,
            onBack = {},
            onRefresh = {},
            onSelectNetwork = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onContinue = {}
        )
    }
}