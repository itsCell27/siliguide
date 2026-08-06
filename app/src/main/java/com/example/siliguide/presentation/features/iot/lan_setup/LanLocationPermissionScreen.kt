package com.example.siliguide.presentation.features.iot.lan_setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenLight
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.WizardCaptionStyle
import com.example.siliguide.core.ui.theme.WizardErrorBg
import com.example.siliguide.core.ui.theme.WizardErrorBorder
import com.example.siliguide.core.ui.theme.WizardErrorText
import androidx.compose.material3.Text
import com.example.siliguide.presentation.features.iot.lan_setup.components.LanWizardScaffold
import com.example.siliguide.presentation.features.iot.lan_setup.components.LocationPinDeniedIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.LocationPinFilledIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.PinSmallIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WifiArcSmallIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardAlertBanner
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardBody
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardBulletRow
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardCard
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardDangerButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardHeading
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardHeroBadge
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPrimaryButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardTextLink

/**
 * Step 1 of 4 — Location Permission.
 *
 * Android needs fine location before it will return WiFi scan results, so this
 * is the wizard's gate. Two states only: permission granted (ask), or denied
 * (send the user to app settings).
 */
sealed interface LanPermissionUiState {
    /** Permission not yet granted — show the rationale and the grant CTA. */
    data object Requesting : LanPermissionUiState

    /** Permission was denied — the only way forward is app settings. */
    data object Denied : LanPermissionUiState
}

@Composable
fun LanLocationPermissionScreen(
    state: LanPermissionUiState,
    onBack: () -> Unit,
    onGrantPermission: () -> Unit,
    onWhyNeeded: () -> Unit,
    onOpenAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    LanWizardScaffold(
        flowLabel = "LAN Setup",
        stepLabel = "Step 1 of 4",
        progress = 0.25f,
        onBack = onBack,
        modifier = modifier,
        contentAlignment = Alignment.CenterHorizontally,
        contentArrangement = Arrangement.spacedBy(13.dp),
        bottomBar = {
            when (state) {
                LanPermissionUiState.Requesting -> {
                    WizardPrimaryButton(
                        text = "Grant Permission",
                        onClick = onGrantPermission
                    )
                    WizardTextLink(
                        text = "Why is this needed?",
                        onClick = onWhyNeeded
                    )
                }

                LanPermissionUiState.Denied -> {
                    WizardDangerButton(
                        text = "Open App Settings",
                        onClick = onOpenAppSettings,
                        bgColor = WizardErrorBg,
                        contentColor = WizardErrorText
                    )
                }
            }
        }
    ) {
        when (state) {
            LanPermissionUiState.Requesting -> {
                WizardHeroBadge(bgColor = GreenBgLight, borderColor = GreenLight) {
                    LocationPinFilledIcon()
                }

                WizardHeading(
                    text = "Location permission",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                WizardBody(
                    text = "Android requires location access to scan for nearby WiFi networks. " +
                            "Used only to find your IoT device — never stored or shared.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                WizardCard {
                    WizardBulletRow(
                        text = "Scan for nearby WiFi networks",
                        showDivider = true
                    ) { PinSmallIcon() }

                    WizardBulletRow(
                        text = "Discover IoT device on local network",
                        showDivider = false
                    ) { WifiArcSmallIcon() }
                }
            }

            LanPermissionUiState.Denied -> {
                WizardHeroBadge(bgColor = WizardErrorBg, borderColor = WizardErrorBorder) {
                    LocationPinDeniedIcon(color = WizardErrorText)
                }

                WizardHeading(
                    text = "Permission required",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                WizardBody(
                    text = "Location permission was denied. SiliGuide needs it to scan for " +
                            "WiFi networks and find your IoT device.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                WizardAlertBanner(
                    bgColor = WizardErrorBg,
                    borderColor = WizardErrorBorder,
                    accentColor = WizardErrorText,
                    cornerRadius = 11.dp
                ) {
                    Text(
                        text = "Without location permission, WiFi scanning cannot proceed on this device.",
                        style = WizardCaptionStyle,
                        color = WizardErrorText
                    )
                }
            }
        }
    }
}

/* ── Previews ──────────────────────────────────────────────────────────── */

@Preview(
    name = "Granted · Small 320x640",
    showSystemUi = true,
    device = "spec:width=320dp,height=640dp,dpi=320"
)
@Composable
private fun LanLocationPermissionGrantedSmallPreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Requesting,
            onBack = {},
            onGrantPermission = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(
    name = "Granted · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanLocationPermissionGrantedStandardPreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Requesting,
            onBack = {},
            onGrantPermission = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(
    name = "Granted · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanLocationPermissionGrantedLargePreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Requesting,
            onBack = {},
            onGrantPermission = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(
    name = "Denied · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanLocationPermissionDeniedPreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Denied,
            onBack = {},
            onGrantPermission = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(
    name = "Denied · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanLocationPermissionDeniedLargePreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Denied,
            onBack = {},
            onGrantPermission = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}