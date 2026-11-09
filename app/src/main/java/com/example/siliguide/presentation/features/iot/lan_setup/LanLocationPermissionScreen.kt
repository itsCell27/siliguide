package com.example.siliguide.presentation.features.iot.lan_setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenLight
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.WizardCaptionStyle
import com.example.siliguide.core.ui.theme.WizardErrorBg
import com.example.siliguide.core.ui.theme.WizardErrorBorder
import com.example.siliguide.core.ui.theme.WizardErrorText
import com.example.siliguide.presentation.features.iot.lan_setup.components.CheckMarkIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.LanWizardScaffold
import com.example.siliguide.presentation.features.iot.lan_setup.components.LocationPinDeniedIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.LocationPinFilledIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.PinSmallIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.SuccessCheckIcon
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
 * Two runtime permissions gate a Wi-Fi scan: ACCESS_FINE_LOCATION on every
 * version, plus NEARBY_WIFI_DEVICES on Android 13+. They are granted through
 * separate system prompts and separate App Settings toggles, so the denied
 * state has to say which one is still missing — otherwise the user grants
 * Location, comes back, and the screen looks stuck for no visible reason.
 */
sealed interface LanPermissionUiState {

    /** Nothing granted yet, nothing permanently denied — show the grant CTA. */
    data object Requesting : LanPermissionUiState

    /** Every required permission is granted — show the continue CTA. */
    data object Granted : LanPermissionUiState

    /** At least one permission is permanently denied. */
    data class Denied(
        val missingLocation: Boolean = true,
        val missingNearbyDevices: Boolean = false,
        /** True when at least one missing permission can still be prompted. */
        val canRetryPrompt: Boolean = false
    ) : LanPermissionUiState
}

@Composable
fun LanLocationPermissionScreen(
    state: LanPermissionUiState,
    onBack: () -> Unit,
    onGrantPermission: () -> Unit,
    onContinue: () -> Unit,
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

                LanPermissionUiState.Granted -> {
                    WizardPrimaryButton(
                        text = "Continue",
                        onClick = onContinue
                    )
                }

                is LanPermissionUiState.Denied -> {
                    if (state.canRetryPrompt) {
                        // One of the two can still be asked for directly
                        WizardPrimaryButton(
                            text = "Try Again",
                            onClick = onGrantPermission
                        )
                        WizardTextLink(
                            text = "Open App Settings",
                            onClick = onOpenAppSettings
                        )
                    } else {
                        WizardDangerButton(
                            text = "Open App Settings",
                            onClick = onOpenAppSettings,
                            bgColor = WizardErrorBg,
                            contentColor = WizardErrorText
                        )
                    }
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
                    text = "Android requires location and nearby device access to scan for " +
                            "WiFi networks. Used only to find your IoT device — never stored " +
                            "or shared.",
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

            LanPermissionUiState.Granted -> {
                WizardHeroBadge(bgColor = GreenBgLight, borderColor = GreenLight) {
                    SuccessCheckIcon(size = 38.dp, color = GreenPrimary)
                }

                WizardHeading(
                    text = "Permissions granted",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                WizardBody(
                    text = "SiliGuide can now scan for WiFi networks and find your IoT " +
                            "device. Continue to choose the network your device will join.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                WizardCard {
                    WizardBulletRow(
                        text = "Location — allowed",
                        showDivider = true
                    ) { CheckMarkIcon() }

                    WizardBulletRow(
                        text = "Nearby devices — allowed",
                        showDivider = false
                    ) { CheckMarkIcon() }
                }
            }

            is LanPermissionUiState.Denied -> {
                WizardHeroBadge(bgColor = WizardErrorBg, borderColor = WizardErrorBorder) {
                    LocationPinDeniedIcon(color = WizardErrorText)
                }

                WizardHeading(
                    text = deniedHeading(state),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                WizardBody(
                    text = deniedBody(state),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                // Spells out both toggles, since App Settings lists them apart
                WizardCard {
                    if (state.missingLocation) {
                        WizardBulletRow(
                            text = "Location — still needs to be allowed",
                            showDivider = state.missingNearbyDevices
                        ) { PinSmallIcon() }
                    }

                    if (state.missingNearbyDevices) {
                        WizardBulletRow(
                            text = "Nearby devices — still needs to be allowed",
                            showDivider = false
                        ) { WifiArcSmallIcon() }
                    }
                }

                WizardAlertBanner(
                    bgColor = WizardErrorBg,
                    borderColor = WizardErrorBorder,
                    accentColor = WizardErrorText,
                    cornerRadius = 11.dp
                ) {
                    Text(
                        text = deniedBanner(state),
                        style = WizardCaptionStyle,
                        color = WizardErrorText
                    )
                }
            }
        }
    }
}

private fun deniedHeading(state: LanPermissionUiState.Denied): String = when {
    state.missingLocation && state.missingNearbyDevices -> "Permissions required"
    state.missingNearbyDevices -> "Nearby devices access required"
    else -> "Location permission required"
}

private fun deniedBody(state: LanPermissionUiState.Denied): String = when {
    state.missingLocation && state.missingNearbyDevices ->
        "SiliGuide needs both Location and Nearby devices access to scan for WiFi " +
                "networks and find your IoT device."

    state.missingNearbyDevices ->
        "Location is allowed, but SiliGuide still needs Nearby devices access to find " +
                "your IoT device on WiFi."

    else ->
        "Location permission was denied. SiliGuide needs it to scan for WiFi networks " +
                "and find your IoT device."
}

private fun deniedBanner(state: LanPermissionUiState.Denied): String = when {
    state.missingLocation && state.missingNearbyDevices ->
        "In App Settings, open Permissions and allow both Location and Nearby devices."

    state.missingNearbyDevices ->
        "In App Settings, open Permissions and allow Nearby devices. Allowing Location " +
                "alone is not enough."

    else ->
        "In App Settings, open Permissions and allow Location. WiFi scanning cannot " +
                "proceed without it."
}

/* ── Previews ──────────────────────────────────────────────────────────── */

@Preview(
    name = "Requesting · Small 320x640",
    showSystemUi = true,
    device = "spec:width=320dp,height=640dp,dpi=320"
)
@Composable
private fun LanLocationPermissionRequestingSmallPreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Requesting,
            onBack = {},
            onGrantPermission = {},
            onContinue = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(
    name = "Requesting · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanLocationPermissionRequestingStandardPreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Requesting,
            onBack = {},
            onGrantPermission = {},
            onContinue = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(
    name = "Granted · Small 320x640",
    showSystemUi = true,
    device = "spec:width=320dp,height=640dp,dpi=320"
)
@Composable
private fun LanLocationPermissionGrantedSmallPreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Granted,
            onBack = {},
            onGrantPermission = {},
            onContinue = {},
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
            state = LanPermissionUiState.Granted,
            onBack = {},
            onGrantPermission = {},
            onContinue = {},
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
            state = LanPermissionUiState.Granted,
            onBack = {},
            onGrantPermission = {},
            onContinue = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(
    name = "Denied · Both missing 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanLocationPermissionDeniedBothPreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Denied(
                missingLocation = true,
                missingNearbyDevices = true
            ),
            onBack = {},
            onGrantPermission = {},
            onContinue = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(
    name = "Denied · Nearby only, retryable 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanLocationPermissionDeniedNearbyPreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Denied(
                missingLocation = false,
                missingNearbyDevices = true,
                canRetryPrompt = true
            ),
            onBack = {},
            onGrantPermission = {},
            onContinue = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}

@Preview(
    name = "Denied · Location only 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanLocationPermissionDeniedLocationLargePreview() {
    SiliGuideTheme {
        LanLocationPermissionScreen(
            state = LanPermissionUiState.Denied(
                missingLocation = true,
                missingNearbyDevices = false
            ),
            onBack = {},
            onGrantPermission = {},
            onContinue = {},
            onWhyNeeded = {},
            onOpenAppSettings = {}
        )
    }
}