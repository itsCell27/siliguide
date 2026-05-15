package com.example.siliguide.presentation.features.iot.ap_setup

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.provider.Settings
import com.example.siliguide.core.ui.theme.*

@Composable
fun APStep1Content(
    onScanQrClick: () -> Unit,
    isChecking: Boolean = false,
    onManualRetry: () -> Unit = {},
) {
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Connect to device",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Fraunces,
                color = Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Find the sticker on the back of your SiliGuide sensor for your unique network details.",
                fontSize = 13.sp,
                color = GrayMedium,
                lineHeight = 18.sp
            )
        }

        // Hotspot Info Card (.apc)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(GreenBgLight, White)), RoundedCornerShape(14.dp))
                .border(1.dp, GreenLight, RoundedCornerShape(14.dp))
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(GreenPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Wifi, contentDescription = null, tint = GreenBgLight, modifier = Modifier.size(26.dp))
            }

            // DEVICE STICKER DASHED BOX
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    // Custom modifier to draw a dashed border around the sticker
                    .drawBehind {
                        drawRoundRect(
                            color = GreenLight,
                            style = Stroke(
                                width = 4f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                            ),
                            cornerRadius = CornerRadius(24f, 24f)
                        )
                    }
                    .background(White, RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "DEVICE STICKER",
                    fontSize = 10.sp,
                    color = GrayMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                // Dynamic SSID Placeholder
                Text(
                    text = "SiliGuide_IoT_XXXX",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = GreenBgLight,
                    modifier = Modifier
                        .background(GreenDark, RoundedCornerShape(7.dp))
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                )

                // Hidden Password
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Password:", fontSize = 13.sp, color = GrayMedium)
                    Text(
                        text = "••••••••",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = Black,
                        modifier = Modifier
                            .background(OffWhite, RoundedCornerShape(5.dp))
                            .border(1.dp, Border, RoundedCornerShape(5.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                HorizontalDivider(color = Border, modifier = Modifier.padding(vertical = 4.dp))

                // QR Scan Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(GreenBgLight, RoundedCornerShape(8.dp))
                        .border(1.5.dp, GreenLight, RoundedCornerShape(8.dp))
                        .clickable { onScanQrClick() }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = GreenDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Scan QR to Auto-Connect",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GreenDark
                    )
                }
            }

            Text(
                text = "Or manually join the network shown on your sticker via WiFi settings.",
                fontSize = 10.sp,
                color = GreenDark,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        // Open WiFi Settings Row (.owr)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White, RoundedCornerShape(11.dp))
                .border(1.dp, Border, RoundedCornerShape(11.dp))
                .clickable {
                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    context.startActivity(intent)
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(GreenBgLight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    contentDescription = null,
                    tint = GreenPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(9.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Open WiFi Settings",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black
                )
                Text(
                    "Find your SiliGuide_IoT network",
                    fontSize = 11.sp,
                    color = GrayMedium
                )
            }

            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = GrayHint
            )
        }

        // Checking or Waiting Box (.pc)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (isChecking) GreenBgLight else GreenBgLight,
                            White
                        )
                    ),
                    RoundedCornerShape(14.dp)
                )
                .border(
                    1.dp,
                    if (isChecking) GreenLight else GreenLight,
                    RoundedCornerShape(14.dp)
                )
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .border(2.dp, GreenLight, CircleShape)
                    .background(White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isChecking) {
                    // Spinning refresh icon when checking
                    Icon(
                        Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(45f)  // You can animate this for spinning effect
                    )
                } else {
                    Icon(
                        Icons.Rounded.Wifi,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (isChecking) {
                Text(
                    "Checking connection…",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black
                )
                Text(
                    "Make sure you're connected to the SiliGuide_IoT network",
                    fontSize = 11.sp,
                    color = GrayMedium,
                    textAlign = TextAlign.Center
                )

                // Manual retry button when checking
                Button(
                    onClick = onManualRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .padding(top = 4.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "Retry Detection",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = White
                    )
                }
            } else {
                Text(
                    "Waiting for connection…",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Black
                )
                Text(
                    "App detects you automatically",
                    fontSize = 11.sp,
                    color = GrayMedium
                )
            }
        }

        // Tip Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White, RoundedCornerShape(11.dp))
                .border(1.dp, Border, RoundedCornerShape(11.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text("💡", fontSize = 16.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Tip: Your phone won't be able to find the SiliGuide hotspot if the device is unplugged or powered off.",
                fontSize = 12.sp,
                color = GrayBody,
                lineHeight = 16.sp
            )
        }
    }
}