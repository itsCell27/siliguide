package com.example.siliguide.presentation.features.iot.bluetooth_setup.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.DangerBg
import com.example.siliguide.core.ui.theme.DangerBorder
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GrayHint
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.Info
import com.example.siliguide.core.ui.theme.InfoBg
import com.example.siliguide.core.ui.theme.InfoBlue
import com.example.siliguide.core.ui.theme.InfoBorder
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.White
import com.example.siliguide.presentation.features.iot.bluetooth_setup.state.VerificationStage
import com.example.siliguide.presentation.features.iot.bluetooth_setup.state.VerificationStageStatus
import com.example.siliguide.presentation.features.iot.lan_setup.components.PhoneOutlineIcon

private const val DEVICE_NAME_MAX_LENGTH = 12
private val CardCornerRadius = 14.dp
private val CardBorderWidth = 1.dp
private val IconBadgeSize = 76.dp
private val NodeSize = 46.dp
private val PulseCircleSize = 58.dp
private val StageDotSize = 19.dp

// ─────────────────────────── Top bar & progress ───────────────────────────

@Composable
fun BluetoothSetupTopBar(
    stepLabel: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(OffWhite)
                    .border(1.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chevron_left),
                    contentDescription = "Back",
                    tint = GrayBody,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "Bluetooth Setup",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        if (stepLabel != null) {
            Text(
                text = stepLabel,
                style = MaterialTheme.typography.labelSmall,
                color = GreenPrimary
            )
        }
    }
}

@Composable
fun ProgressHeader(
    progress: Float,
    modifier: Modifier = Modifier
) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp),
        color = GreenPrimary,
        trackColor = Border,
    )
}

// ─────────────────────────── Illustration ───────────────────────────

@Composable
fun BluetoothIllustrationCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(InfoBlue, White)
                ),
                RoundedCornerShape(CardCornerRadius)
            )
            .border(CardBorderWidth, InfoBorder, RoundedCornerShape(CardCornerRadius))
            .padding(vertical = 18.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Surface(
            shape = CircleShape,
            color = White,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, InfoBorder),
            modifier = Modifier.size(NodeSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                PhoneOutlineIcon(color = Info)
            }
        }
        Row(
            modifier = Modifier.width(60.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Info.copy(alpha = 0.5f), CircleShape)
                )
            }
        }
        Surface(
            shape = CircleShape,
            color = Info,
            modifier = Modifier.size(NodeSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = White
                )
            }
        }
    }
}

/** Circular badge icon used at the top of intro / unsupported / details screens. */
@Composable
fun CircularIconBadge(
    background: Color,
    borderColor: Color,
    iconTint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = CircleShape,
        color = background,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
        modifier = modifier.size(IconBadgeSize)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        }
    }
}

// ─────────────────────────── Requirements checklist ───────────────────────────

@Composable
fun RequirementChecklist(
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .background(GreenBgLight, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (index != items.lastIndex) {
                HorizontalDivider(color = Border)
            }
        }
    }
}

// ─────────────────────────── Open settings action row ───────────────────────────

@Composable
fun OpenBluetoothSettingsCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(11.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(InfoBg, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Info,
                    modifier = Modifier.size(14.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Open Bluetooth Settings",
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Pair your SiliGuide device, then come back",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMedium
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = GrayHint,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// ─────────────────────────── Waiting (pulsing) card ───────────────────────────

@Composable
fun WaitingCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(InfoBg, RoundedCornerShape(CardCornerRadius))
            .border(CardBorderWidth, InfoBorder, RoundedCornerShape(CardCornerRadius))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = White,
            border = androidx.compose.foundation.BorderStroke(2.dp, InfoBorder),
            modifier = Modifier.size(PulseCircleSize)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = Info
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = GrayMedium,
            textAlign = TextAlign.Center
        )
    }
}

// ─────────────────────────── Verification stages card ───────────────────────────

@Composable
fun VerificationCard(
    stages: List<VerificationStage>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        stages.forEachIndexed { index, stage ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                when (stage.status) {
                    VerificationStageStatus.Done -> {
                        Box(
                            modifier = Modifier
                                .size(StageDotSize)
                                .background(GreenPrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = White,
                                modifier = Modifier.size(9.dp)
                            )
                        }
                    }
                    VerificationStageStatus.Pending -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(StageDotSize),
                            strokeWidth = 2.dp,
                            color = Info,
                            trackColor = Border
                        )
                    }
                    VerificationStageStatus.Waiting -> {
                        Box(
                            modifier = Modifier
                                .size(StageDotSize)
                                .border(2.dp, Border, CircleShape)
                        )
                    }
                }
                Text(
                    text = stage.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = when (stage.status) {
                        VerificationStageStatus.Done -> MaterialTheme.colorScheme.onSurface
                        VerificationStageStatus.Pending -> Info
                        VerificationStageStatus.Waiting -> GrayHint
                    },
                    fontWeight = if (stage.status == VerificationStageStatus.Done) FontWeight.Medium else FontWeight.Normal
                )
            }
            if (index != stages.lastIndex) {
                HorizontalDivider(color = Border)
            }
        }
    }
}

// ─────────────────────────── Unsupported device card ───────────────────────────

@Composable
fun UnsupportedDeviceCard(
    message: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DangerBg, RoundedCornerShape(11.dp))
            .border(1.dp, DangerBorder, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

// ─────────────────────────── Bluetooth mode badge ───────────────────────────

@Composable
fun BluetoothModeBadge(
    connected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(7.dp),
        color = InfoBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, InfoBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Bluetooth,
                contentDescription = null,
                tint = Info,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = if (connected) "Bluetooth Mode · Connected" else "Bluetooth Mode",
                style = MaterialTheme.typography.labelSmall,
                color = Info
            )
        }
    }
}

// ─────────────────────────── Info card ───────────────────────────

@Composable
fun InfoCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(11.dp))
            .border(1.dp, Border, RoundedCornerShape(11.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = Info,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────── Device details form ───────────────────────────

@Composable
fun DeviceDetailsForm(
    deviceName: String,
    onDeviceNameChanged: (String) -> Unit,
    availablePlants: List<String>,
    selectedPlant: String?,
    onPlantSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(13.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = "Device name",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = deviceName,
                onValueChange = {
                    if (it.length <= DEVICE_NAME_MAX_LENGTH) onDeviceNameChanged(it)
                },
                placeholder = { Text("Enter device name…", color = GrayHint) },
                singleLine = true,
                supportingText = {
                    Text(
                        text = "${deviceName.length}/$DEVICE_NAME_MAX_LENGTH",
                        style = MaterialTheme.typography.labelSmall,
                        color = GreenPrimary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = GreenPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Link to a plant",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = OffWhite,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                ) {
                    Text(
                        text = "Optional",
                        style = MaterialTheme.typography.labelSmall,
                        color = GrayHint,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            PlantDropdown(
                plants = availablePlants,
                selectedPlant = selectedPlant,
                onPlantSelected = onPlantSelected
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantDropdown(
    plants: List<String>,
    selectedPlant: String?,
    onPlantSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedPlant ?: "Select a plant to link",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedTextColor = if (selectedPlant == null) GrayMedium else MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable) // Called directly from the scope
                .fillMaxWidth()
        )

        // Removed the hardcoded 'androidx.compose.material3.' prefix
        // This is now properly recognized as a scoped function of ExposedDropdownMenuBox
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            plants.forEach { plant ->
                DropdownMenuItem(
                    text = { Text(plant) },
                    onClick = {
                        onPlantSelected(plant)
                        expanded = false
                    }
                )
            }
        }
    }
}

// ─────────────────────────── Bottom actions ───────────────────────────

@Composable
fun WizardBottomActions(
    primaryLabel: String,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean = true,
    secondaryLabel: String? = null,
    onSecondaryClick: (() -> Unit)? = null,
    secondaryIsLink: Boolean = false,
    tertiaryLinkLabel: String? = null,
    onTertiaryLinkClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Button(
            onClick = onPrimaryClick,
            enabled = primaryEnabled,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(primaryLabel, style = MaterialTheme.typography.titleSmall, color = White)
        }
        if (secondaryLabel != null && onSecondaryClick != null) {
            if (secondaryIsLink) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = GreenPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickableText(onSecondaryClick)
                )
            } else {
                OutlinedButton(
                    onClick = onSecondaryClick,
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text(secondaryLabel, style = MaterialTheme.typography.titleSmall)
                }
            }
        }
        if (tertiaryLinkLabel != null && onTertiaryLinkClick != null) {
            Text(
                text = tertiaryLinkLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = GreenPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickableText(onTertiaryLinkClick)
            )
        }
    }
}

@Composable
private fun Modifier.clickableText(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}