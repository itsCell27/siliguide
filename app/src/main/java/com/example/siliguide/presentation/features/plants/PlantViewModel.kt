package com.example.siliguide.presentation.features.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.usecase.AddPlantUseCase
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val addPlantUseCase: AddPlantUseCase,
    getPlantsUseCase: GetPlantsUseCase
) : ViewModel() {

    // 1. A continuous stream of plants from the database!
    // If the database changes, this updates the UI automatically.
    // Replace "user-123" with your actual user ID logic later when Auth is done.
    val plants: StateFlow<List<Plant>> = getPlantsUseCase("user-123")
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. Function to add a plant from the UI
    fun addPlant(name: String) {
        viewModelScope.launch {
            // Hardcoding a dummy user ID for offline testing
            addPlantUseCase(name = name, userId = "user-123")
        }
    }
}