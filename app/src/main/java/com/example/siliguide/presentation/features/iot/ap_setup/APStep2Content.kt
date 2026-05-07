package com.example.siliguide.presentation.features.iot.ap_setup

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.Black
import com.example.siliguide.core.ui.theme.Border
import com.example.siliguide.core.ui.theme.Fraunces
import com.example.siliguide.core.ui.theme.GrayBody
import com.example.siliguide.core.ui.theme.GrayHint
import com.example.siliguide.core.ui.theme.GrayMedium
import com.example.siliguide.core.ui.theme.GreenBgLight
import com.example.siliguide.core.ui.theme.GreenDark
import com.example.siliguide.core.ui.theme.GreenLight
import com.example.siliguide.core.ui.theme.GreenPrimary
import com.example.siliguide.core.ui.theme.OffWhite
import com.example.siliguide.core.ui.theme.White

@Composable
fun APStep2Content(
    qrData: String?,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
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
            // Device Name
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
                    Text("Field Sensor 1", fontSize = 14.sp, color = Black, modifier = Modifier.weight(1f))
                    Text("13/30", fontSize = 11.sp, color = GreenPrimary)
                }
            }

            // Link to a plant
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
                    // Leaf/Plant Icon placeholder
                    Box(modifier = Modifier.size(16.dp).background(GreenPrimary, CircleShape))
                    Spacer(modifier = Modifier.width(9.dp))
                    Text("Select a plant to link", fontSize = 14.sp, color = GrayMedium, modifier = Modifier.weight(1f))
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = GrayHint)
                }
            }
        }

        // Info Box (.ic)
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
                text = "In AP Mode your phone must stay connected to SiliGuide_IoTSensors to receive live sensor readings.",
                fontSize = 12.sp,
                color = GrayBody,
                lineHeight = 16.sp
            )
        }
    }
}

@Preview
@Composable
fun Step2Prev() {
    APStep2Content(
        onBack = {},
        qrData = "SiliGuide_IoT_XXXX"
    )
}