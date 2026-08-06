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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.SiliGuideTheme
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.core.ui.theme.WizardCaptionStyle
import com.example.siliguide.presentation.features.iot.lan_setup.components.LanWizardScaffold
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardBody
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardHeading
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardPrimaryButton
import com.example.siliguide.presentation.features.iot.lan_setup.components.WizardRadioDot

/** The two temporary transports that can carry Wi-Fi credentials to the ESP32. */
enum class LanProvisioningMethod { ACCESS_POINT, BLUETOOTH }

/**
 * Step 3 of 4 — Choose Setup Method.
 *
 * Neither option is a communication mode. Both only deliver the credentials
 * captured in step 2; afterwards everything runs over LAN.
 */
data class LanSetupMethodUiState(
    val selectedMethod: LanProvisioningMethod = LanProvisioningMethod.ACCESS_POINT
)

@Composable
fun LanSetupMethodScreen(
    state: LanSetupMethodUiState,
    onBack: () -> Unit,
    onSelectMethod: (LanProvisioningMethod) -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    LanWizardScaffold(
        flowLabel = "LAN Setup",
        stepLabel = "Step 3 of 4",
        progress = 0.75f,
        onBack = onBack,
        modifier = modifier,
        contentArrangement = Arrangement.spacedBy(11.dp),
        bottomBar = {
            WizardPrimaryButton(text = "Continue", onClick = onContinue)
        }
    ) {
        WizardHeading(text = "Choose Setup Method")

        WizardBody(
            text = "Choose how you want to securely send your Wi-Fi credentials to your IoT device."
        )

        MethodCard(
            emoji = "\uD83D\uDCE1",
            title = "Device Hotspot (AP)",
            description = "Temporarily connect to your IoT device's Wi-Fi hotspot to securely " +
                    "transfer the selected Wi-Fi credentials.",
            selected = state.selectedMethod == LanProvisioningMethod.ACCESS_POINT,
            onClick = { onSelectMethod(LanProvisioningMethod.ACCESS_POINT) }
        )

        MethodCard(
            emoji = "\uD83D\uDD35",
            title = "Bluetooth",
            description = "Use Bluetooth to securely send the selected Wi-Fi credentials without " +
                    "leaving your current Wi-Fi network.",
            selected = state.selectedMethod == LanProvisioningMethod.BLUETOOTH,
            onClick = { onSelectMethod(LanProvisioningMethod.BLUETOOTH) }
        )
    }
}

@Composable
private fun MethodCard(
    emoji: String,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) GreenBgLight else White)
            .border(
                width = if (selected) 2.dp else 1.5.dp,
                color = if (selected) GreenPrimary else Border,
                shape = RoundedCornerShape(13.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(top = 1.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) GreenPrimary else OffWhite)
                .then(
                    if (selected) Modifier
                    else Modifier.border(1.dp, Border, RoundedCornerShape(10.dp))
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (selected) GreenDark else Black
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                style = WizardCaptionStyle.copy(lineHeight = 15.4.sp),
                color = if (selected) GreenPrimary else GrayMedium
            )
        }

        WizardRadioDot(selected = selected)
    }
}

/* ── Previews ──────────────────────────────────────────────────────────── */

@Preview(
    name = "AP selected · Small 320x640",
    showSystemUi = true,
    device = "spec:width=320dp,height=640dp,dpi=320"
)
@Composable
private fun LanSetupMethodApSmallPreview() {
    SiliGuideTheme {
        LanSetupMethodScreen(
            state = LanSetupMethodUiState(LanProvisioningMethod.ACCESS_POINT),
            onBack = {},
            onSelectMethod = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "AP selected · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanSetupMethodApStandardPreview() {
    SiliGuideTheme {
        LanSetupMethodScreen(
            state = LanSetupMethodUiState(LanProvisioningMethod.ACCESS_POINT),
            onBack = {},
            onSelectMethod = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "AP selected · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanSetupMethodApLargePreview() {
    SiliGuideTheme {
        LanSetupMethodScreen(
            state = LanSetupMethodUiState(LanProvisioningMethod.ACCESS_POINT),
            onBack = {},
            onSelectMethod = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "Bluetooth selected · Standard 360x740",
    showSystemUi = true,
    device = "spec:width=360dp,height=740dp,dpi=420"
)
@Composable
private fun LanSetupMethodBluetoothPreview() {
    SiliGuideTheme {
        LanSetupMethodScreen(
            state = LanSetupMethodUiState(LanProvisioningMethod.BLUETOOTH),
            onBack = {},
            onSelectMethod = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "Bluetooth selected · Large 412x915",
    showSystemUi = true,
    device = "spec:width=412dp,height=915dp,dpi=420"
)
@Composable
private fun LanSetupMethodBluetoothLargePreview() {
    SiliGuideTheme {
        LanSetupMethodScreen(
            state = LanSetupMethodUiState(LanProvisioningMethod.BLUETOOTH),
            onBack = {},
            onSelectMethod = {},
            onContinue = {}
        )
    }
}