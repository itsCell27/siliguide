package com.example.siliguide.presentation.features.plants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlantScreen(
    viewModel: PlantViewModel = hiltViewModel()
) {
    // Collect the live list of plants from the ViewModel
    val plants by viewModel.plants.collectAsStateWithLifecycle()

    // Hold the text typed into the input field
    var plantName by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Siling Labuyo Plants") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // --- INPUT SECTION ---
            OutlinedTextField(
                value = plantName,
                onValueChange = { plantName = it },
                label = { Text("Plant Name (e.g., Farm Bed A)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (plantName.isNotBlank()) {
                        viewModel.addPlant(plantName) // Saves to DB!
                        plantName = "" // Clear the text field
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Plant")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // --- LIST SECTION ---
            Text("Saved Plants:", style = MaterialTheme.typography.titleMedium)

            if (plants.isEmpty()) {
                Text("No plants saved yet.", modifier = Modifier.padding(top = 16.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(plants) { plant ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = plant.name, style = MaterialTheme.typography.titleMedium)
                                Text(text = "Added: ${plant.dateCreated}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}