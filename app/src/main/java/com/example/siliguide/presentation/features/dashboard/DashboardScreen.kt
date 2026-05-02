package com.example.siliguide.presentation.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.DarkGreen
import com.example.siliguide.core.ui.theme.LightGreenBg
import com.example.siliguide.core.ui.theme.LightWhiteBg
import com.example.siliguide.presentation.features.iot.IoTViewModel

@Composable
fun DashboardScreen(
    onNavigateToCamera: () -> Unit // Passed in from NavGraph!
) {
    Scaffold(
        bottomBar = {
            // We pass the navigation trigger directly to your custom bottom bar!
            DashboardBottomBar(onCameraClick = onNavigateToCamera)
        },
        containerColor = LightWhiteBg
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                Text(
                    text = "SiliGuide",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkGreen,
                    modifier = Modifier.padding(24.dp)
                )

                TestIoTScreen()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(LightGreenBg)
                )
            }
        }
    }
}

@Composable
fun DashboardBottomBar(onCameraClick: () -> Unit) {
    Surface(
        color = Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 8.dp, horizontal = 32.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                painter = painterResource(id = R.drawable.house),
                label = "Home",
                isSelected = true
            )

            // Here is your custom camera button!
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkGreen)
                    .clickable { onCameraClick() }, // Triggers the navigation!
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.camera),
                    contentDescription = "Scan",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            BottomNavItem(
                painter = painterResource(id = R.drawable.user),
                label = "Profile",
                isSelected = false
            )
        }
    }
}

@Composable
fun BottomNavItem(painter: Painter, label: String, isSelected: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painter,
            contentDescription = label,
            tint = if (isSelected) DarkGreen else DarkGreen.copy(alpha = 0.6f),
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isSelected) DarkGreen else DarkGreen.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
fun TestIoTScreen() {
    val viewModel: IoTViewModel = hiltViewModel()

    LaunchedEffect(Unit) {
        viewModel.fetchSensors()
    }

    when {
        viewModel.isLoading -> Text("Loading...")
        viewModel.error != null -> Text(viewModel.error!!)
        viewModel.sensorData != null -> {

            val data = viewModel.sensorData!!

            Column {
                Text("Soil Moisture: ${data.soil.moisture}")
                Text("Soil Temp: ${data.soil.temperature}")
                Text("pH: ${data.soil.ph}")

                Text("EC: ${data.soil.ec}")
                Text("Nitrogen: ${data.soil.nitrogen}")
                Text("Phosphorus: ${data.soil.phosphorus}")
                Text("Potassium: ${data.soil.potassium}")

                Text("Light: ${data.environment.light}")
                Text("Air Temp: ${data.environment.airTemperature}")
            }
        }
    }
}
