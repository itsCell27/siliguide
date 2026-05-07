package com.example.siliguide.presentation.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.presentation.features.auth.components.LimitCard
import com.example.siliguide.presentation.features.auth.components.SectionTag

@Composable
fun GuestInfoScreen(
    onContinueSuccess: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var isLoading by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .safeDrawingPadding()
    ) {

        // ================= SCROLLABLE =================
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(OffWhite)
                    .border(1.5.dp, Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("👤")
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Using SiliGuide as a Guest",
                fontFamily = Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your data will only be stored on this device and will not be backed up.",
                fontSize = 12.sp,
                color = GrayMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionTag("Limitations")

            LimitCard(
                items = listOf(
                    "No cloud backup" to "All data stored on this device only",
                    "Data lost on uninstall" to "Plants and analysis records deleted permanently",
                    "No multi-device access" to "Cannot access data from another device",
                    "No account migration" to "Guest data cannot be transferred"
                ),
                isPositive = false
            )

            Spacer(modifier = Modifier.height(12.dp))

            SectionTag("What you can still do")

            LimitCard(
                items = listOf(
                    "Full plant analysis — offline" to "Camera and ML work offline",
                    "IoT device connection" to "Connect ESP32 sensor"
                ),
                isPositive = true
            )
        }

        // ================= PINNED BUTTONS =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(0.5.dp, Border))
                .padding(24.dp)
        ) {

            Button(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        viewModel.continueAsGuest {
                            isLoading = false
                            onContinueSuccess()
                        }
                    }
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Continue as Guest")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onCreateAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = BorderStroke(1.5.dp, GreenPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Create a Free Account", color = GreenPrimary)
            }
        }
    }
}