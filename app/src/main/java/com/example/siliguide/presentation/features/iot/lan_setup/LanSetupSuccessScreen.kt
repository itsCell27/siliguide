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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.core.ui.theme.WizardSubCaptionStyle
import com.example.siliguide.core.ui.theme.WizardSuccessTitleStyle
import com.example.siliguide.presentation.features.iot.lan_setup.components.ChevronDownIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.DeviceChipIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.LanWizardScaffold
import com.example.siliguide.presentation.features.iot.lan_setup.components.PlantLeafIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.SuccessCheckIcon
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardBody
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardFieldLabel
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPill
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPrimaryButton

/**
 * Success — reached from either provisioning path.
 *
 * The ESP32 is on the LAN. The user names the device and may optionally link
 * it to a Siling Labuyo plant for continuous monitoring.
 */
data class LanSetupSuccessUiState(
    val ipAddress: String = "192.168.1.42",
    val deviceName: String = "",
    val deviceNameMaxLength: Int = 30,
    val linkedPlantName: String? = null
)

@Composable
fun LanSetupSuccessScreen(
    state: LanSetupSuccessUiState,
    onBack: () -> Unit,
    onDeviceNameChange: (String) -> Unit,
    onPickPlant: () -> Unit,
    onSaveDevice: () -> Unit,
    modifier: Modifier = Modifier
) {
    LanWizardScaffold(
        flowLabel = "LAN Setup",
        stepLabel = "Step 4 of 4",
        progress = 1f,
        onBack = onBack,
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(13.dp),
        bottomBar = {
            WizardPrimaryButton(text = "Save Device", onClick = onSaveDevice)
        }
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
                text = "Device connected!",
                style = WizardSuccessTitleStyle,
                color = Black,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(3.dp))
            WizardBody(
                text = "Your ESP32 has joined your WiFi network and is ready to use.",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        WizardPill(
            text = "\uD83C\uDFE0 LAN Mode · ${state.ipAddress}",
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        // ── Device name ─────────────────────────────────────────────────
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            WizardFieldLabel(text = "Device name")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(White)
                    .border(1.5.dp, GreenPrimary, RoundedCornerShape(12.dp))
                    .clickable { onDeviceNameChange(state.deviceName) }
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                DeviceChipIcon()

                Text(
                    text = state.deviceName,
                    fontSize = 12.sp,
                    color = Black,
                    modifier = Modifier.weight(1f)
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

/* ── Previews ──────────────────────────────────────────────────────────── */

private val previewSuccess = LanSetupSuccessUiState(
    ipAddress = "192.168.1.42",
    deviceName = "Garden Sensor",
    linkedPlantName = "Labuyo Plant #2"
)

@Preview(
    name = "Success · Small 320x640",
    showSystemUi = true,
    device = "spec:width=320dp,height=640dp,dpi=320"
)
@Composable
private fun LanSetupSuccessSmallPreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewSuccess,
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {}
        )
    }
}

@Preview(
    name = "Success · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanSetupSuccessStandardPreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewSuccess,
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {}
        )
    }
}

@Preview(
    name = "Success · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanSetupSuccessLargePreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewSuccess,
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {}
        )
    }
}

@Preview(
    name = "Success · No plant linked 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanSetupSuccessNoPlantPreview() {
    SiliGuideTheme {
        LanSetupSuccessScreen(
            state = previewSuccess.copy(linkedPlantName = null),
            onBack = {},
            onDeviceNameChange = {},
            onPickPlant = {},
            onSaveDevice = {}
        )
    }
}