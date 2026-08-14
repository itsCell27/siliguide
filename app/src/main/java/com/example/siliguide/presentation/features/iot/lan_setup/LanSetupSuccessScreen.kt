package com.example.siliguide.presentation.features.iot.lan_setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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
import com.example.siliguide.core.ui.theme.GreenLight
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.core.ui.theme.WizardCaptionStyle
import com.example.siliguide.core.ui.theme.WizardSubCaptionStyle
import com.example.siliguide.core.ui.theme.WizardSuccessTitleStyle
import com.example.siliguide.core.ui.theme.WizardWarningBg
import com.example.siliguide.core.ui.theme.WizardWarningBorder
import com.example.siliguide.core.ui.theme.WizardWarningText
import com.example.siliguide.presentation.features.iot.lan_setup.components.ChevronDownIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.DeviceChipIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.LanWizardScaffold
import com.example.siliguide.presentation.features.iot.lan_setup.components.PlantLeafIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.SuccessCheckIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WifiBroadcastIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WifiSettingsIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardActionRow
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardAlertBanner
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardBody
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardCard
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardFieldLabel
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardNumberedRow
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardOutlinedButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPill
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPrimaryButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardStatusPanel
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardWaitingButton

/**
 * Which half of the Success screen is showing.
 *
 * The AP path leaves the phone on the device's hotspot, and Android will not
 * let an app join a network on the user's behalf — WifiNetworkSpecifier always
 * raises a system consent sheet the user can decline. So instead of navigating
 * away and hoping, the wizard holds here until the phone is verifiably back on
 * the same network as the device.
 */
enum class LanSuccessStage {

    /** Name the device and save it. */
    NAMING,

    /** Device is on the LAN; the phone still has to follow it over. */
    SWITCH_NETWORK,

    /** Pinging the device at its LAN address. */
    VERIFYING
}

/**
 * Success — reached from either provisioning path.
 */
data class LanSetupSuccessUiState(
    val stage: LanSuccessStage = LanSuccessStage.NAMING,
    /** Which transport delivered the device: "AP Hotspot" or "Bluetooth". */
    val provisioningPath: String = "AP Hotspot",
    val deviceName: String = "",
    val deviceNameMaxLength: Int = 12,
    val linkedPlantName: String? = null,
    val isSaving: Boolean = false,
    /** Network the device joined — the one the phone now has to rejoin. */
    val targetSsid: String = "",
    /** Address the device reported on that network. */
    val deviceIp: String = "",
    /** A verification attempt already failed. */
    val switchFailed: Boolean = false,

    val isModeSwitch: Boolean = false
)

@Composable
fun LanSetupSuccessScreen(
    state: LanSetupSuccessUiState,
    onBack: () -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onPickPlant: () -> Unit,
    onSaveDevice: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onCheckConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Bypass name validation if it's a mode switch
    val canSave = (state.deviceName.isNotBlank() || state.isModeSwitch) && !state.isSaving

    val hideBack = state.stage == LanSuccessStage.NAMING

    val primaryButtonText = if (state.isModeSwitch) "Apply Network Switch" else "Save Device"
    val loadingButtonText = if (state.isModeSwitch) "Applying…" else "Saving…"

    LanWizardScaffold(
        flowLabel = if (state.isModeSwitch) "Switch to LAN" else "LAN Setup",
        stepLabel = "Step 4 of 4",
        progress = 1f,
        onBack = onBack,
        showBack = hideBack,
        enableBack = !state.isSaving,
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(13.dp),
        bottomBar = {
            when (state.stage) {

                LanSuccessStage.NAMING -> {
                    if (canSave) {
                        WizardPrimaryButton(
                            text = if (state.isModeSwitch) "Apply Network Switch" else "Save Device",
                            onClick = onSaveDevice)
                    } else {
                        WizardWaitingButton(
                            text = if (state.isSaving) loadingButtonText else primaryButtonText
                        )
                    }
                }

                LanSuccessStage.SWITCH_NETWORK -> {
                    WizardPrimaryButton(
                        text = "Open WiFi Settings",
                        onClick = onOpenWifiSettings
                    )
                    WizardOutlinedButton(
                        text = "I'm Connected",
                        onClick = onCheckConnection
                    )
                }

                LanSuccessStage.VERIFYING ->
                    WizardWaitingButton(text = "Checking…")
            }
        }
    ) {
        when (state.stage) {

            LanSuccessStage.NAMING -> NamingContent(
                state = state,
                onDeviceNameChange = onDeviceNameChange,
                onPickPlant = onPickPlant
            )

            LanSuccessStage.SWITCH_NETWORK,
            LanSuccessStage.VERIFYING -> SwitchNetworkContent(
                state = state,
                onOpenWifiSettings = onOpenWifiSettings
            )
        }
    }
}

/* ── Stage 1: name and save ────────────────────────────────────────────── */

@Composable
private fun ColumnScope.NamingContent(
    state: LanSetupSuccessUiState,
    onDeviceNameChange: (String) -> Unit,
    onPickPlant: () -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(66.dp)
            .clip(RoundedCornerShape(50))
            .background(GreenPrimary),
        contentAlignment = Alignment.Center
    ) { SuccessCheckIcon() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (state.isModeSwitch) "Mode Switch Ready!" else "Device connected!",
            style = WizardSuccessTitleStyle,
            color = Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        WizardBody(
            text = if (state.isModeSwitch) {
                "Your device's new network details are ready to be pushed."
            } else {
                "Your Device is connected and is ready to be added to your account."
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    // Names the path that actually delivered the device
    WizardPill(
        text = "Device Connected via ${state.provisioningPath}",
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )

    // --- ONLY SHOW IF IT IS A NEW DEVICE ---
    if (!state.isModeSwitch) {
        // ── Device name ─────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            WizardFieldLabel(text = "Device name")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(White)
                    .border(
                        width = 1.5.dp,
                        color = if (state.deviceName.isBlank()) GreenLight else GreenPrimary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                DeviceChipIcon()

                BasicTextField(
                    value = state.deviceName,
                    onValueChange = { typed ->
                        if (typed.length <= state.deviceNameMaxLength) {
                            onDeviceNameChange(typed)
                        }
                    },
                    singleLine = true,
                    enabled = !state.isSaving,
                    textStyle = TextStyle(
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = Black
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(GreenPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (state.deviceName.isEmpty()) {
                                Text(
                                    text = "Name this device",
                                    fontSize = 12.sp,
                                    color = GrayHint
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Text(
                    text = "${state.deviceName.length}/${state.deviceNameMaxLength}",
                    style = WizardSubCaptionStyle,
                    color = GreenPrimary
                )
            }
        }

        // ── Link to plant ───────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            WizardFieldLabel(text = "Link to a plant", optional = true)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(White)
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onPickPlant)
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                PlantLeafIcon()

                Text(
                    text = state.linkedPlantName ?: "Select a plant",
                    fontSize = 12.sp,
                    color = GrayMedium,
                    modifier = Modifier.weight(1f)
                )

                ChevronDownIcon()
            }
        }
    }


}

/* ── Stage 2: move the phone off the hotspot ───────────────────────────── */

@Composable
private fun ColumnScope.SwitchNetworkContent(
    state: LanSetupSuccessUiState,
    onOpenWifiSettings: () -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(66.dp)
            .clip(RoundedCornerShape(50))
            .background(GreenPrimary),
        contentAlignment = Alignment.Center
    ) { SuccessCheckIcon() }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Device saved!",
            style = WizardSuccessTitleStyle,
            color = Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        WizardBody(
            text = if (state.provisioningPath == "Bluetooth") {
                "Your device joined the network. Connect this phone to the same " +
                        "network so the app can reach it."
            } else {
                "Your device joined the network. Your phone is still on the device " +
                        "hotspot — switch it back so the app can reach the device."
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }

    WizardPill(
        text = "${state.targetSsid} · ${state.deviceIp}",
        modifier = Modifier.align(Alignment.CenterHorizontally)
    )

    WizardCard(cornerRadius = 13.dp) {
        WizardNumberedRow(number = 1, showDivider = true) {
            Text(
                text = "Open WiFi Settings",
                style = WizardCaptionStyle,
                color = GrayBody
            )
        }

        WizardNumberedRow(number = 2, showDivider = true) {
            Text(
                text = buildAnnotatedString {
                    append("Connect to ")
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            background = GreenBgLight,
                            color = GreenDark
                        )
                    ) { append(" ${state.targetSsid} ") }
                },
                style = WizardCaptionStyle,
                color = GrayBody
            )
        }

        WizardNumberedRow(number = 3, showDivider = false) {
            Text(
                text = "Return to SiliGuide",
                style = WizardCaptionStyle,
                color = GrayBody
            )
        }
    }

    WizardActionRow(
        title = "Open WiFi Settings",
        subtitle = "Reconnect to ${state.targetSsid}",
        tileColor = GreenBgLight,
        onClick = onOpenWifiSettings
    ) { WifiSettingsIcon() }

    if (state.switchFailed && state.stage != LanSuccessStage.VERIFYING) {
        WizardAlertBanner(
            bgColor = WizardWarningBg,
            borderColor = WizardWarningBorder,
            accentColor = WizardWarningText
        ) {
            Text(
                text = "Still couldn't reach the device. Make sure this phone is on " +
                        "\"${state.targetSsid}\" and not the device hotspot, then check again.",
                style = WizardCaptionStyle,
                color = WizardWarningText
            )
        }
    } else {
        WizardStatusPanel(
            title = if (state.stage == LanSuccessStage.VERIFYING) {
                "Looking for the device…"
            } else {
                "Waiting for your phone to rejoin…"
            },
            subtitle = "We'll detect this automatically"
        ) { WifiBroadcastIcon() }
    }
}

/* ── Previews ──────────────────────────────────────────────────────────── */

private val previewNaming = LanSetupSuccessUiState(
    stage = LanSuccessStage.NAMING,
    provisioningPath = "AP Hotspot",
    deviceName = "SiliGuide8BC",
    linkedPlantName = "Labuyo Plant #2"
)

private val previewSwitch = LanSetupSuccessUiState(
    stage = LanSuccessStage.SWITCH_NETWORK,
    deviceName = "SiliGuide8BC",
    targetSsid = "master'sBED_2ndF",
    deviceIp = "192.168.1.42"
)

@Preview(
    name = "Naming · Small 320x640",
    showSystemUi = true,
    device = "spec:width=320dp,height=640dp,dpi=320"
)
@Composable
private fun LanSetupSuccessNamingSmallPreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewNaming,
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {},
            onOpenWifiSettings = {},
            onCheckConnection = {}
        )
    }
}

@Preview(
    name = "Naming · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanSetupSuccessNamingStandardPreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewNaming,
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {},
            onOpenWifiSettings = {},
            onCheckConnection = {}
        )
    }
}

@Preview(
    name = "Switch network · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanSetupSuccessSwitchPreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewSwitch,
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {},
            onOpenWifiSettings = {},
            onCheckConnection = {}
        )
    }
}

@Preview(
    name = "Switch network · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanSetupSuccessSwitchLargePreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewSwitch,
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {},
            onOpenWifiSettings = {},
            onCheckConnection = {}
        )
    }
}

@Preview(
    name = "Switch failed 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanSetupSuccessSwitchFailedPreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewSwitch.copy(switchFailed = true),
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {},
            onOpenWifiSettings = {},
            onCheckConnection = {}
        )
    }
}

@Preview(
    name = "Verifying 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanSetupSuccessVerifyingPreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewSwitch.copy(stage = LanSuccessStage.VERIFYING),
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {},
            onOpenWifiSettings = {},
            onCheckConnection = {}
        )
    }
}