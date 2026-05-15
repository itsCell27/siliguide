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
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.usecase.GetAnalysisSessionsByPlantUseCase
import com.example.siliguide.domain.usecase.GetPlantByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val addPlantUseCase: AddPlantUseCase,
    getPlantsUseCase: GetPlantsUseCase,
    private val getPlantByIdUseCase: GetPlantByIdUseCase,
    private val getAnalysisSessionsByPlantUseCase: GetAnalysisSessionsByPlantUseCase
) : ViewModel() {

    // 1. A continuous stream of ALL plants from the local database.
    // Since we removed user accounts, we no longer pass any ID here.
    val plants: StateFlow<List<Plant>> = getPlantsUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _plantState = MutableStateFlow<Plant?>(null)
    val plantState: StateFlow<Plant?> = _plantState.asStateFlow()

    private val _analysisHistoryState = MutableStateFlow<List<AnalysisSession>>(emptyList())
    val analysisHistoryState: StateFlow<List<AnalysisSession>> = _analysisHistoryState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPlant(plantId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _plantState.value = getPlantByIdUseCase(plantId)
                _analysisHistoryState.value = getAnalysisSessionsByPlantUseCase(plantId)
            } catch (e: Exception) {
                _plantState.value = null
            }
            _isLoading.value = false
        }
    }

    // 2. Function to add a plant from the UI.
    // Note: If you want to link a device immediately, you can add
    // an optional deviceId parameter here.
    fun addPlant(name: String, deviceId: String? = null) {
        viewModelScope.launch {
            // userId parameter is now completely gone to match AddPlantUseCase
            addPlantUseCase(name = name, deviceId = deviceId)
        }
    }
}