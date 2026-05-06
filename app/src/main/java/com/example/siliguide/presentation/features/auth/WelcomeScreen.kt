package com.example.siliguide.presentation.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.theme.*

@Composable
fun WelcomeScreen(
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onContinueGuest: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .safeDrawingPadding()
    ) {

        // ================= HERO =================
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .clip(CircleShape)
                    .background(GreenLight)
                    .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("🌱", fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "SiliGuide",
                fontFamily = Fraunces,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = Black
            )

            Text(
                text = "Plant care for Siling Labuyo",
                fontFamily = DMSans,
                fontSize = 12.sp,
                color = GrayMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(GreenLight)
                    .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Decision Support System",
                    fontFamily = DMSans,
                    fontSize = 10.sp,
                    color = GreenDark
                )
            }
        }

        // ================= BOTTOM SHEET =================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OffWhite)
                .border(BorderStroke(0.5.dp, Border))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "GET STARTED",
                fontFamily = DMSans,
                fontSize = 10.sp,
                color = GrayMedium
            )

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Login")
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onSignUp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                border = BorderStroke(1.5.dp, GreenPrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Sign Up", color = GreenPrimary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
                Text(
                    "or",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    fontSize = 11.sp,
                    color = GrayMedium
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = DividerDefaults.Thickness,
                    color = DividerDefaults.color
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(onClick = onContinueGuest) {
                Text("Continue as Guest  ›", color = GrayBody)
            }

            Text(
                text = "No cloud backup · Data lost on uninstall",
                fontSize = 10.sp,
                color = GrayMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview
@Composable
fun WelcomePrev () {
    WelcomeScreen(
        onLogin = {},
        onSignUp = {},
        onContinueGuest = {}
    )
}