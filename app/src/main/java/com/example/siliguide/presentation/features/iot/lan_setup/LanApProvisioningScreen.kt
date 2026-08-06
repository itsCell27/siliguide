package com.example.siliguide.presentation.features.iot.lan_setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.WizardCaptionStyle
import com.example.siliguide.core.ui.theme.WizardWarningBg
import com.example.siliguide.core.ui.theme.WizardWarningBorder
import com.example.siliguide.core.ui.theme.WizardWarningText
import com.example.siliguide.presentation.features.iot.lan_setup.components.LanWizardScaffold
import com.example.siliguide.presentation.features.iot.lan_setup.components.LocationPinMediumIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WifiBroadcastIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WifiSettingsIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardActionRow
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardAlertBanner
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardBody
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardCard
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardHeading
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardHeroBadge
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardNumberedRow
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardOutlinedButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPrimaryButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardStatusPanel
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardWaitingButton

/**
 * Step 4 of 4 — Device Hotspot (AP) Provisioning Path.
 *
 * The phone temporarily joins the ESP32's own hotspot, hands over the Home
 * Wi-Fi credentials collected in step 2, then the ESP32 drops its hotspot and
 * joins the LAN.
 */
sealed interface LanApProvisioningUiState {
    /** Instructions shown; app polls until the phone lands on the device hotspot. */
    data class Waiting(val deviceSsid: String = "SiliGuide_IoT_XXXX") : LanApProvisioningUiState

    /** Hotspot detected; credentials are being pushed to the ESP32. */
    data object SendingCredentials : LanApProvisioningUiState

    /** Hotspot never appeared — offer retry or fall back to Bluetooth. */
    data class Error(val deviceSsid: String = "SiliGuide_IoT_XXXX") : LanApProvisioningUiState
}

@Composable
fun LanApProvisioningScreen(
    state: LanApProvisioningUiState,
    onBack: () -> Unit,
    onOpenWifiSettings: () -> Unit,
    onRetry: () -> Unit,
    onSwitchToBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    LanWizardScaffold(
        flowLabel = "AP Setup",
        stepLabel = "Step 4 of 4",
        progress = 1f,
        onBack = onBack,
        modifier = modifier,
        contentArrangement = when (state) {
            is LanApProvisioningUiState.SendingCredentials ->
                Arrangement.spacedBy(10.dp, Alignment.CenterVertically)

            else -> Arrangement.spacedBy(9.dp)
        },
        contentAlignment = when (state) {
            is LanApProvisioningUiState.SendingCredentials -> Alignment.CenterHorizontally
            else -> Alignment.Start
        },
        bottomBar = {
            when (state) {
                is LanApProvisioningUiState.Waiting -> WizardWaitingButton(text = "Waiting…")

                LanApProvisioningUiState.SendingCredentials ->
                    WizardWaitingButton(text = "Please wait…")

                is LanApProvisioningUiState.Error -> {
                    WizardPrimaryButton(text = "Try Again", onClick = onRetry)
                    WizardOutlinedButton(
                        text = "Switch to Bluetooth",
                        onClick = onSwitchToBluetooth
                    )
                }
            }
        }
    ) {
        when (state) {
            is LanApProvisioningUiState.Waiting -> {
                WizardHeading(text = "Connect to Device Hotspot")

                WizardBody(
                    text = "Temporarily join the device's hotspot so we can send your Wi-Fi " +
                            "credentials to it."
                )

                WizardCard(cornerRadius = 13.dp) {
                    WizardNumberedRow(number = 1, showDivider = true) {
                        Text(
                            text = "Open Wi-Fi Settings",
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
                                ) { append(" ${state.deviceSsid} ") }
                            },
                            style = WizardCaptionStyle,
                            color = GrayBody
                        )
                    }

                    WizardNumberedRow(number = 3, showDivider = false) {
                        Text(
                            text = "Return to the app",
                            style = WizardCaptionStyle,
                            color = GrayBody
                        )
                    }
                }

                WizardActionRow(
                    title = "Open Wi-Fi Settings",
                    subtitle = "Find ${state.deviceSsid}",
                    tileColor = GreenBgLight,
                    onClick = onOpenWifiSettings
                ) { WifiSettingsIcon() }

                WizardStatusPanel(
                    title = "Waiting for hotspot connection…",
                    subtitle = "We'll detect this automatically"
                ) { LocationPinMediumIcon() }
            }

            LanApProvisioningUiState.SendingCredentials -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WizardHeroBadge(
                        bgColor = com.example.siliguide.core.ui.theme.White,
                        borderColor = com.example.siliguide.core.ui.theme.GreenLight,
                        size = 58.dp,
                        borderWidth = 2.dp
                    ) { WifiBroadcastIcon() }

                    Text(
                        text = "Sending Wi-Fi credentials…",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Black,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "ESP32 connecting to Home Wi-Fi…",
                        style = WizardCaptionStyle,
                        color = GrayMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            is LanApProvisioningUiState.Error -> {
                WizardHeading(text = "Couldn't detect the device hotspot")

                WizardBody(
                    text = "We couldn't detect your IoT device's hotspot. Please make sure the " +
                            "device is powered on and connect to its Wi-Fi network."
                )

                WizardAlertBanner(
                    bgColor = WizardWarningBg,
                    borderColor = WizardWarningBorder,
                    accentColor = WizardWarningText
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("Make sure your phone is connected to ")
                            withStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    background = Color.Black.copy(alpha = 0.07f),
                                    color = WizardWarningText
                                )
                            ) { append(" ${state.deviceSsid} ") }
                            append(" in Wi-Fi settings, then tap Try Again.")
                        },
                        style = WizardCaptionStyle,
                        color = WizardWarningText
                    )
                }
            }
        }
    }
}

/* ── Previews ──────────────────────────────────────────────────────────── */

@Preview(
    name = "Waiting · Small 320x640",
    showSystemUi = true,
    device = "spec:width=320dp,height=640dp,dpi=320"
)
@Composable
private fun LanApWaitingSmallPreview() {
    SiliGuideTheme {
        LanApProvisioningScreen(
            state = LanApProvisioningUiState.Waiting(),
            onBack = {},
            onOpenWifiSettings = {},
            onRetry = {},
            onSwitchToBluetooth = {}
        )
    }
}

@Preview(
    name = "Waiting · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanApWaitingStandardPreview() {
    SiliGuideTheme {
        LanApProvisioningScreen(
            state = LanApProvisioningUiState.Waiting(),
            onBack = {},
            onOpenWifiSettings = {},
            onRetry = {},
            onSwitchToBluetooth = {}
        )
    }
}

@Preview(
    name = "Waiting · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanApWaitingLargePreview() {
    SiliGuideTheme {
        LanApProvisioningScreen(
            state = LanApProvisioningUiState.Waiting(),
            onBack = {},
            onOpenWifiSettings = {},
            onRetry = {},
            onSwitchToBluetooth = {}
        )
    }
}

@Preview(
    name = "Sending · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanApSendingPreview() {
    SiliGuideTheme {
        LanApProvisioningScreen(
            state = LanApProvisioningUiState.SendingCredentials,
            onBack = {},
            onOpenWifiSettings = {},
            onRetry = {},
            onSwitchToBluetooth = {}
        )
    }
}

@Preview(
    name = "Error · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanApErrorPreview() {
    SiliGuideTheme {
        LanApProvisioningScreen(
            state = LanApProvisioningUiState.Error(),
            onBack = {},
            onOpenWifiSettings = {},
            onRetry = {},
            onSwitchToBluetooth = {}
        )
    }
}

@Preview(
    name = "Error · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanApErrorLargePreview() {
    SiliGuideTheme {
        LanApProvisioningScreen(
            state = LanApProvisioningUiState.Error(),
            onBack = {},
            onOpenWifiSettings = {},
            onRetry = {},
            onSwitchToBluetooth = {}
        )
    }
}