package com.example.siliguide.presentation.features.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.example.siliguide.core.ui.components.SiliGuideBackButton
import com.example.siliguide.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantScreen(
    viewModel: AddPlantViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToCamera: (String) -> Unit
) {
    val createdPlantId by viewModel.createdPlantId.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()

    val initialName = rememberSaveable {
        "Labuyo Plant#${('A'..'Z').random()}${(1..99).random().toString().padStart(2, '0')}"
    }

    var plantName by rememberSaveable { mutableStateOf(initialName) }
    var showNextSheet by rememberSaveable { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    val isSaveEnabled = plantName.isNotBlank() && !isSaving

    Scaffold(
        modifier = Modifier.safeDrawingPadding(),
        topBar = {
            SiliGuideTopBar(title = "Add New Plant", onBack = onNavigateBack)
        },
        bottomBar = {
            Surface(tonalElevation = 2.dp, shadowElevation = 8.dp) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White)
                        .padding(horizontal = 22.dp, vertical = 20.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.addPlant(plantName)
                            showNextSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = isSaveEnabled,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenPrimary,
                            disabledContainerColor = GreenLight
                        )
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Next", color = White, style = MaterialTheme.typography.titleMedium)
                        }
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

            Text("Plant name", style = MaterialTheme.typography.labelSmall, color = GrayBody)
            Spacer(modifier = Modifier.height(6.dp))

            CustomInputField(
                value = plantName,
                onValueChange = { if (it.length <= MAX_PLANT_NAME_LENGTH) plantName = it },
                placeholder = "e.g. My Labuyo #1",
                onDone = {
                    if (isSaveEnabled) {
                        viewModel.addPlant(plantName)
                        showNextSheet = true
                    }
                }
            )
        }

        if (showNextSheet) {
            ModalBottomSheet(
                onDismissRequest = { },
                sheetState = sheetState,
                containerColor = White,
                dragHandle = null,
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
fun CustomInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onDone: () -> Unit = {}
) {
    val isActive = value.isNotEmpty()
    val focusManager = LocalFocusManager.current

    Row(
        modifier = modifier
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
        Icon(
            imageVector = Icons.Default.Spa,
            contentDescription = null,
            tint = if (isActive) GreenPrimary else GrayHint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = Black),
            cursorBrush = SolidColor(GreenPrimary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    onDone()
                }
            ),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(placeholder, color = GrayHint, fontSize = 13.sp)
                }
                innerTextField()
            }
        )
        Text(
            text = "${value.length}/$MAX_PLANT_NAME_LENGTH",
            fontSize = 10.sp,
            color = if (isActive) GreenPrimary else GrayHint
        )
    }
}

@Composable
fun NextStepsSheetContent(onAnalyze: () -> Unit, onFinish: () -> Unit) {
    var selectedOption by rememberSaveable { mutableIntStateOf(0) }

    Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        Text("What's next?", style = MaterialTheme.typography.titleLarge)
        Text(
            text = "Your plant has been saved. Would you like to analyze it now or come back later?",
            fontSize = 12.sp,
            color = GrayMedium,
            lineHeight = 18.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
        )

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
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
        ) {
            Text("Continue", style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
fun ChoiceCard(
    title: String,
    desc: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(
                if (isActive) 2.dp else 1.5.dp,
                if (isActive) GreenPrimary else Border,
                RoundedCornerShape(14.dp)
            )
            .background(if (isActive) GreenBgLight else White, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isActive) White else OffWhite, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (isActive) GreenPrimary else GrayMedium)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (isActive) GreenDark else Black, style = MaterialTheme.typography.titleMedium)
            Text(desc, fontSize = 11.sp, color = if (isActive) GreenPrimary else GrayMedium, lineHeight = 14.sp)
        }
        RadioButton(
            selected = isActive,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = GreenPrimary),
            interactionSource = interactionSource
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SiliGuideTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp)
            )
        },
        navigationIcon = {
            SiliGuideBackButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 22.dp)
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = White),
        windowInsets = TopAppBarDefaults.windowInsets
    )
}