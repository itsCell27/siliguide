package com.example.siliguide.presentation.features.iot.ap_setup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.presentation.features.iot.IoTViewModel

@Composable
fun APStep2Content(
    viewModel: IoTViewModel,
    deviceName: String,                      // ← RECEIVE
    onDeviceNameChange: (String) -> Unit,    // ← CALLBACK
    onBack: () -> Unit
) {

    val maxChar = 30

    Column(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Success Icon
        Box(
            modifier = Modifier.size(66.dp).background(GreenPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = White, modifier = Modifier.size(32.dp))
        }

        // Header
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Device connected!", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = Fraunces, color = Black)
            Text("Your ESP32 is reachable via AP hotspot.", fontSize = 13.sp, color = GrayMedium, modifier = Modifier.padding(top = 3.dp))
        }

        // Mode Badge
        Text(
            text = "📡 AP Mode · 192.168.4.1",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = GreenDark,
            modifier = Modifier
                .background(GreenBgLight, RoundedCornerShape(7.dp))
                .border(1.dp, GreenLight, RoundedCornerShape(7.dp))
                .padding(horizontal = 11.dp, vertical = 6.dp)
        )

        // Form Fields
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // --- EDITABLE DEVICE NAME FIELD ---
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Device name", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GrayBody)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(White, RoundedCornerShape(12.dp))
                        .border(1.5.dp, GreenPrimary, RoundedCornerShape(12.dp))
                        .padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Wifi, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(9.dp))

                    // BasicTextField for pure custom styling
                    BasicTextField(
                        value = deviceName,
                        onValueChange = {
                            if (it.length <= maxChar) onDeviceNameChange(it)  // ← CALL CALLBACK
                        },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(fontSize = 14.sp, color = Black, fontFamily = DMSans),
                        cursorBrush = SolidColor(GreenPrimary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        decorationBox = { innerTextField ->
                            if (deviceName.isEmpty()) {
                                Text("Enter device name...", fontSize = 14.sp, color = GrayHint)
                            }
                            innerTextField()
                        }
                    )

                    Text("${deviceName.length}/$maxChar", fontSize = 11.sp, color = GreenPrimary)
                }
            }

            // Link to a plant (Static for now as per your design)
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Link to a plant ", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GrayBody)
                    Text(
                        "Optional",
                        fontSize = 10.sp,
                        color = GrayHint,
                        modifier = Modifier
                            .background(OffWhite, RoundedCornerShape(4.dp))
                            .border(1.dp, Border, RoundedCornerShape(4.dp))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(White, RoundedCornerShape(12.dp))
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(16.dp).background(GreenPrimary, CircleShape))
                    Spacer(modifier = Modifier.width(9.dp))
                    Text("Select a plant to link", fontSize = 14.sp, color = GrayMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = GrayHint)
                }
            }
        }

        // Info Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White, RoundedCornerShape(11.dp))
                .border(1.dp, Border, RoundedCornerShape(11.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Rounded.Wifi, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "In AP Mode your phone must stay connected to the sensor hotspot to receive live readings.",
                fontSize = 12.sp,
                color = GrayBody,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}