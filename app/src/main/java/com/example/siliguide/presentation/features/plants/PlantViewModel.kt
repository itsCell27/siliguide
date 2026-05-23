package com.example.siliguide.presentation.features.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.usecase.AddPlantUseCase
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import com.example.siliguide.domain.usecase.GetAnalysisSessionsByPlantUseCase
import com.example.siliguide.domain.usecase.GetPlantByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlantViewModel @Inject constructor(
    private val authRepository: AuthRepository, // Inject this to get current userId
    private val addPlantUseCase: AddPlantUseCase,
    private val getPlantsUseCase: GetPlantsUseCase,
    private val getPlantByIdUseCase: GetPlantByIdUseCase,
    private val getAnalysisSessionsByPlantUseCase: GetAnalysisSessionsByPlantUseCase
) : ViewModel() {

    // 1. Reactive Plant Stream: Automatically scoped to the current userId
    @OptIn(ExperimentalCoroutinesApi::class)
    val plants: StateFlow<List<Plant>> = authRepository.getUserIdFlow()
        .flatMapLatest { userId ->
            if (userId != null) {
                getPlantsUseCase(userId) // Now takes userId
            } else {
                flowOf(emptyList())
            }
        }
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

    // 2. Updated addPlant to include userId from the repository
    fun addPlant(name: String, deviceId: String? = null) {
        viewModelScope.launch {
            val userId = authRepository.getUserIdFlow().first()
            if (userId != null) {
                addPlantUseCase(
                    userId = userId, // Pass the ID
                    name = name,
                    deviceId = deviceId
                )
            }
        }
    }
}