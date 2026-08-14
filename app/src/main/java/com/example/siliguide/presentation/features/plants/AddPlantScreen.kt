package com.example.siliguide.presentation.features.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantScreen(
    viewModel: PlantViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCamera: (String) -> Unit
) {
    // new created plant id
    val createdPlantId by viewModel.createdPlantId.collectAsState()

    var plantName by remember { mutableStateOf("") }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var selectedDeviceName by remember { mutableStateOf("") }
    var showNextSheet by remember { mutableStateOf(false) }

    // Creates a state that refuses to be dismissed via swipe or outside click
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    // Animates the blur smoothly from 0.dp to 12.dp
    val blurRadius by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (showNextSheet) 12.dp else 0.dp,
        label = "blurAnimation"
    )

    Scaffold(
        Modifier
            .safeDrawingPadding()
            // Applies the animated blur to the background
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier),
        topBar = {
            SiliGuideTopBar(title = "Add New Plant", onBack = onNavigateBack)
        },
        bottomBar = {
            // Pinned Bottom Button Section
            Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addPlant(plantName, selectedDeviceId)
                            showNextSheet = true
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        enabled = plantName.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            disabledContainerColor = GreenLight
                        )
                    ) {
                        Text("Next", color = White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(OffWhite)
                .padding(padding)
                .padding(horizontal = 22.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // --- PLANT NAME SECTION ---
            Text("Plant name", style = MaterialTheme.typography.labelSmall, color = GrayBody)
            Spacer(modifier = Modifier.height(6.dp))
            CustomInputField(
                value = plantName,
                onValueChange = { if (it.length <= 30) plantName = it },
                placeholder = "e.g. My Labuyo #1",
                charCount = "${plantName.length}/30"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- IOT DEVICE SECTION ---
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Text("Link an IoT Device", style = MaterialTheme.typography.labelSmall, color = GrayBody)
//                Spacer(modifier = Modifier.width(8.dp))
//                Box(modifier = Modifier
//                    .clip(RoundedCornerShape(4.dp))
//                    .background(White)
//                    .border(1.dp, Border, RoundedCornerShape(4.dp))
//                    .padding(horizontal = 6.dp, vertical = 1.dp)
//                ) {
//                    Text("Optional", fontSize = 10.sp, color = GrayHint, fontWeight = FontWeight.Medium)
//                }
//            }
//            Text("Linking a device enables live sensor monitoring for this plant",
//                fontSize = 10.sp, color = GrayHint, modifier = Modifier.padding(top = 2.dp))

//            Spacer(modifier = Modifier.height(10.dp))
//
//            // Toggle between Selector and Selected state
//            if (selectedDeviceId == null) {
//                DashedNoDeviceCard(onClick = { /* Future: Open Device Picker */ })
//            } else {
//                SelectedDeviceCard(
//                    name = selectedDeviceName,
//                    onRemove = { selectedDeviceId = null }
//                )
//            }
//
//            // Mock Device Info Note from design
//            if (selectedDeviceId != null) {
//                Spacer(modifier = Modifier.height(10.dp))
//                DeviceNote(text = "$selectedDeviceName is currently connected. It will start monitoring this plant immediately after saving.")
//            }
        }

        // --- POST-SAVE BOTTOM SHEET ---
        if (showNextSheet) {
            ModalBottomSheet(
                onDismissRequest = {  },
                sheetState = sheetState,
                containerColor = White,
                dragHandle = null, // disable drag handle so user cannot go back to add plant screen and add duplicate plant
                scrimColor = Color.Black.copy(alpha = 0.5f)
            ) {
                NextStepsSheetContent(
                    onAnalyze = {
                        showNextSheet = false
                        createdPlantId?.let { id ->
                            onNavigateToCamera(id)
                        }
                    },
                    onFinish = {
                        showNextSheet = false
                        onNavigateBack()
                    }
                )
            }
        }
    }
}

@Composable
fun CustomInputField(value: String, onValueChange: (String) -> Unit, placeholder: String, charCount: String) {
    val isActive = value.isNotEmpty()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(White, RoundedCornerShape(12.dp))
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = if (isActive) GreenPrimary else Border,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Spa, null, tint = if (isActive) GreenPrimary else GrayHint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Black),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) Text(placeholder, color = GrayHint, fontSize = 13.sp)
                innerTextField()
            }
        )
        Text(charCount, fontSize = 10.sp, color = if (isActive) GreenPrimary else GrayHint)
    }
}

@Composable
fun DashedNoDeviceCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GrayLight, RoundedCornerShape(12.dp)) // Design calls for dashed; solid used for stability
            .background(White, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Add, null, tint = GrayLight, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text("No IoT devices registered yet.", color = GrayHint, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text("Add one →", color = GreenPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SelectedDeviceCard(name: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(GreenBgLight, RoundedCornerShape(12.dp))
            .border(1.5.dp, GreenLight, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(28.dp).background(White, RoundedCornerShape(8.dp)).border(1.dp, GreenLight, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.SettingsInputAntenna, null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(name, color = GreenDark, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("LAN Mode · Connected", color = GreenPrimary, fontSize = 10.sp)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(22.dp).background(White, CircleShape).border(1.dp, GreenLight, CircleShape)) {
            Icon(Icons.Default.Close, null, modifier = Modifier.size(10.dp), tint = GrayMedium)
        }
    }
}

@Composable
fun NextStepsSheetContent(onAnalyze: () -> Unit, onFinish: () -> Unit) {
    var selectedOption by remember { mutableStateOf(0) } // 0 for Analyze, 1 for Later

    Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        Text("What's next?", style = MaterialTheme.typography.titleLarge)
        Text("Your plant has been saved. Would you like to analyze it now or come back later?",
            fontSize = 12.sp, color = GrayMedium, lineHeight = 18.sp, modifier = Modifier.padding(top = 4.dp, bottom = 18.dp))

        ChoiceCard(
            title = "Analyze Now",
            desc = "Capture a leaf image to get an instant plant condition report",
            icon = Icons.Default.CameraAlt,
            isActive = selectedOption == 0,
            onClick = { selectedOption = 0 }
        )
        ChoiceCard(
            title = "Do It Later",
            desc = "Skip for now — analyze anytime from the plant's page",
            icon = Icons.Default.History,
            isActive = selectedOption == 1,
            onClick = { selectedOption = 1 }
        )

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = { if (selectedOption == 0) onAnalyze() else onFinish() },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun ChoiceCard(title: String, desc: String, icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(if (isActive) 2.dp else 1.5.dp, if (isActive) GreenPrimary else Border, RoundedCornerShape(14.dp))
            .background(if (isActive) GreenBgLight else White, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).background(if (isActive) White else OffWhite, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (isActive) GreenPrimary else GrayMedium)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (isActive) GreenDark else Black, style = MaterialTheme.typography.titleMedium)
            Text(desc, fontSize = 11.sp, color = if (isActive) GreenPrimary else GrayMedium, lineHeight = 14.sp)
        }
        RadioButton(selected = isActive, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary))
    }
}

@Composable
fun DeviceNote(text: String) {
    Row(modifier = Modifier.fillMaxWidth().background(GreenBgLight, RoundedCornerShape(10.dp)).border(1.dp, GreenLight, RoundedCornerShape(10.dp)).padding(10.dp)) {
        Icon(Icons.Default.Info, null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 11.sp, color = GreenDark, lineHeight = 16.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiliGuideTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall, // Fraunces 18sp
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        navigationIcon = {
            // We use a Box instead of IconButton to ignore Material's 48dp min-size rule
            Box(
                modifier = Modifier
                    .padding(start = 22.dp) // Aligned with your horizontal padding
                    .size(32.dp)           // Fixed circle size from mockup
                    .clip(CircleShape)
                    .background(OffWhite)
                    .border(1.dp, Border, CircleShape)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(14.dp), // Scaled icon size
                    tint = GrayBody
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White),
        // Added this to ensure the TopAppBar doesn't add extra height
        windowInsets = TopAppBarDefaults.windowInsets
    )
}