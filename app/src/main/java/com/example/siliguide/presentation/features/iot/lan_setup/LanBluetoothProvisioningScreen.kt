package com.example.siliguide.presentation.features.iot.lan_setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.Info
import com.example.siliguide.core.ui.theme.InfoBlue
import com.example.siliguide.core.ui.theme.InfoBorder
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.core.ui.theme.WizardCaptionStyle
import com.example.siliguide.core.ui.theme.WizardSubCaptionStyle
import com.example.siliguide.core.ui.theme.WizardWarningBg
import com.example.siliguide.core.ui.theme.WizardWarningBorder
import com.example.siliguide.core.ui.theme.WizardWarningText
import com.example.siliguide.presentation.features.iot.lan_setup.components.BluetoothGlyphIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.CheckMarkIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.LanWizardScaffold
import com.example.siliguide.presentation.features.iot.lan_setup.components.TransferDots
import com.example.siliguide.presentation.features.iot.lan_setup.components.WifiBroadcastIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardActionRow
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardAlertBanner
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardBody
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardBulletRow
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardCard
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardHeading
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardOutlinedButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPill
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPrimaryButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardStatusPanel
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardWaitingButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.PhoneOutlineIcon

/**
 * Step 4 of 4 — Bluetooth Provisioning Path.
 *
 * Android owns pairing, so the app guides the user to Bluetooth Settings and
 * then auto-detects the paired SiliGuide device. Once connected it pushes the
 * same Home Wi-Fi credentials the AP path would have sent.
 */
sealed interface LanBluetoothProvisioningUiState {
    /** Sub-step 1 of 2 — guide the user into Android Bluetooth Settings. */
    data object Pairing : LanBluetoothProvisioningUiState

    /** Paired and connected; credentials are being pushed over SPP. */
    data class SendingCredentials(
        val deviceName: String = "SiliGuide_89BC"
    ) : LanBluetoothProvisioningUiState

    /** SPP link failed — offer retry or fall back to the AP path. */
    data object Error : LanBluetoothProvisioningUiState
}

@Composable
fun LanBluetoothProvisioningScreen(
    state: LanBluetoothProvisioningUiState,
    onBack: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRetry: () -> Unit,
    onSwitchToDeviceHotspot: () -> Unit,
    modifier: Modifier = Modifier
) {
    LanWizardScaffold(
        flowLabel = "Bluetooth Setup",
        stepLabel = if (state is LanBluetoothProvisioningUiState.Pairing) "Step 1 of 2" else "Step 4 of 4",
        progress = if (state is LanBluetoothProvisioningUiState.Pairing) 0.5f else 1f,
        onBack = onBack,
        modifier = modifier,
        contentArrangement = when (state) {
            LanBluetoothProvisioningUiState.Pairing -> Arrangement.spacedBy(13.dp)
            is LanBluetoothProvisioningUiState.SendingCredentials -> Arrangement.spacedBy(11.dp)
            LanBluetoothProvisioningUiState.Error -> Arrangement.spacedBy(9.dp)
        },
        bottomBar = {
            when (state) {
                LanBluetoothProvisioningUiState.Pairing ->
                    WizardPrimaryButton(
                        text = "Open Bluetooth Settings",
                        onClick = onOpenBluetoothSettings
                    )

                is LanBluetoothProvisioningUiState.SendingCredentials ->
                    WizardWaitingButton(text = "Please wait…")

                LanBluetoothProvisioningUiState.Error -> {
                    WizardPrimaryButton(text = "Retry Bluetooth", onClick = onRetry)
                    WizardOutlinedButton(
                        text = "Switch to Device Hotspot",
                        onClick = onSwitchToDeviceHotspot
                    )
                }
            }
        }
    ) {
        when (state) {
            LanBluetoothProvisioningUiState.Pairing -> {
                WizardHeading(text = "Pair your device")

                WizardBody(
                    text = "Pair your SiliGuide IoT device using Android Bluetooth Settings. " +
                            "After pairing, return to SiliGuide."
                )

                PairingIllustration()

                WizardActionRow(
                    title = "Open Bluetooth Settings",
                    subtitle = "Pair your SiliGuide device, then come back",
                    tileColor = InfoBlue,
                    onClick = onOpenBluetoothSettings
                ) { BluetoothGlyphIcon(color = Info) }

                WizardCard {
                    WizardBulletRow(
                        text = "Only pair once per device — SiliGuide remembers it after",
                        showDivider = true
                    ) { CheckMarkIcon() }

                    WizardBulletRow(
                        text = "Returning here shows every paired SiliGuide device to choose from",
                        showDivider = false
                    ) { CheckMarkIcon() }
                }
            }

            is LanBluetoothProvisioningUiState.SendingCredentials -> {
                WizardHeading(text = "Connect via Bluetooth")

                WizardBody(
                    text = "Stay on your current Wi-Fi network — no need to switch networks."
                )

                ConnectedDeviceCard(deviceName = state.deviceName)

                WizardStatusPanel(
                    title = "Sending Wi-Fi credentials…",
                    subtitle = "ESP32 connecting to Home Wi-Fi…"
                ) { WifiBroadcastIcon() }
            }

            LanBluetoothProvisioningUiState.Error -> {
                WizardHeading(text = "Bluetooth communication failed")

                WizardBody(
                    text = "We couldn't communicate with your IoT device over Bluetooth."
                )

                WizardAlertBanner(
                    bgColor = WizardWarningBg,
                    borderColor = WizardWarningBorder,
                    accentColor = WizardWarningText
                ) {
                    Text(
                        text = "Make sure Bluetooth is turned on and the device is powered on " +
                                "and nearby, then tap Retry.",
                        style = WizardCaptionStyle,
                        color = WizardWarningText
                    )
                }
            }
        }
    }
}

/** Phone ⟶ device transfer illustration shown during pairing. */
@Composable
private fun PairingIllustration() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(InfoBlue, White)))
            .border(1.dp, InfoBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(50))
                .background(White)
                .border(1.5.dp, InfoBorder, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) { PhoneOutlineIcon(color = Info) }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) { TransferDots(color = Info) }

        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(50))
                .background(Info),
            contentAlignment = Alignment.Center
        ) { BluetoothGlyphIcon(size = 18.dp, color = InfoBlue) }
    }
}

/** Paired device summary row with a live "Connected" pill. */
@Composable
private fun ConnectedDeviceCard(deviceName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(White)
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(GreenPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "\uD83D\uDD35", fontSize = 18.sp)
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = deviceName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Black
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = "Nearby Bluetooth device",
                style = WizardSubCaptionStyle,
                color = GrayMedium
            )
        }

        WizardPill(text = "Connected", showDot = true, cornerRadius = 20.dp)
    }
}

/* ── Previews ──────────────────────────────────────────────────────────── */

@Preview(
    name = "Pairing · Small 320x640",
    showSystemUi = true,
    device = "spec:width=320dp,height=640dp,dpi=320"
)
@Composable
private fun LanBtPairingSmallPreview() {
    SiliGuideTheme {
        LanBluetoothProvisioningScreen(
            state = LanBluetoothProvisioningUiState.Pairing,
            onBack = {},
            onOpenBluetoothSettings = {},
            onRetry = {},
            onSwitchToDeviceHotspot = {}
        )
    }
}

@Preview(
    name = "Pairing · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanBtPairingStandardPreview() {
    SiliGuideTheme {
        LanBluetoothProvisioningScreen(
            state = LanBluetoothProvisioningUiState.Pairing,
            onBack = {},
            onOpenBluetoothSettings = {},
            onRetry = {},
            onSwitchToDeviceHotspot = {}
        )
    }
}

@Preview(
    name = "Pairing · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanBtPairingLargePreview() {
    SiliGuideTheme {
        LanBluetoothProvisioningScreen(
            state = LanBluetoothProvisioningUiState.Pairing,
            onBack = {},
            onOpenBluetoothSettings = {},
            onRetry = {},
            onSwitchToDeviceHotspot = {}
        )
    }
}

@Preview(
    name = "Sending · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanBtSendingPreview() {
    SiliGuideTheme {
        LanBluetoothProvisioningScreen(
            state = LanBluetoothProvisioningUiState.SendingCredentials(),
            onBack = {},
            onOpenBluetoothSettings = {},
            onRetry = {},
            onSwitchToDeviceHotspot = {}
        )
    }
}

@Preview(
    name = "Error · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanBtErrorPreview() {
    SiliGuideTheme {
        LanBluetoothProvisioningScreen(
            state = LanBluetoothProvisioningUiState.Error,
            onBack = {},
            onOpenBluetoothSettings = {},
            onRetry = {},
            onSwitchToDeviceHotspot = {}
        )
    }
}

@Preview(
    name = "Error · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanBtErrorLargePreview() {
    SiliGuideTheme {
        LanBluetoothProvisioningScreen(
            state = LanBluetoothProvisioningUiState.Error,
            onBack = {},
            onOpenBluetoothSettings = {},
            onRetry = {},
            onSwitchToDeviceHotspot = {}
        )
    }
}