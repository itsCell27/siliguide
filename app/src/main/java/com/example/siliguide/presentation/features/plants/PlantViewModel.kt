package com.example.siliguide.presentation.features.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.usecase.AddPlantUseCase
import com.example.siliguide.domain.usecase.DeletePlantByIdUseCase
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import com.example.siliguide.domain.usecase.GetAnalysisSessionsByPlantUseCase
import com.example.siliguide.domain.usecase.GetPlantByIdUseCase
import com.example.siliguide.domain.usecase.LinkIoTDeviceToPlantUseCase
import com.example.siliguide.domain.usecase.MarkPlantAsDeletedByIdUseCase
import com.example.siliguide.domain.usecase.UnlinkIoTDeviceToPlantUseCase
import com.example.siliguide.domain.usecase.UpdatePlantNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject


const val MAX_PLANT_NAME_LENGTH = 20


@HiltViewModel
class PlantViewModel @Inject constructor(
    private val authRepository: AuthRepository, // Inject this to get current userId
    private val addPlantUseCase: AddPlantUseCase,
    private val getPlantsUseCase: GetPlantsUseCase,
    private val getPlantByIdUseCase: GetPlantByIdUseCase,
    private val getAnalysisSessionsByPlantUseCase: GetAnalysisSessionsByPlantUseCase,
    private val deletePlantByIdUseCase: DeletePlantByIdUseCase,
    private val linkIoTDeviceToPlantUseCase: LinkIoTDeviceToPlantUseCase,
    private val unlinkIoTDeviceToPlantUseCase: UnlinkIoTDeviceToPlantUseCase,
    private val updatePlantNameUseCase: UpdatePlantNameUseCase,
    private val markPlantAsDeletedByIdUseCase: MarkPlantAsDeletedByIdUseCase
) : ViewModel() {

    // Add this state to store the ID of the newly created plant
    private val _createdPlantId = MutableStateFlow<String?>(null)
    val createdPlantId: StateFlow<String?> = _createdPlantId.asStateFlow()

    // Reactive Plant Stream: Automatically scoped to the current userId
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

    // Updated addPlant to include userId from the repository
    fun addPlant(name: String, deviceId: String? = null) {
        viewModelScope.launch {
            val userId = authRepository.getUserId()
            if (userId != null) {
                // 1. Insert the plant
                val newPlantId = addPlantUseCase(userId, name, deviceId)

                // 2. Verify it exists in the DB before updating the state
                // This ensures that when the UI sees 'createdPlantId', the record is 100% there.
                val verifiedPlant = getPlantByIdUseCase(newPlantId)

                if (verifiedPlant != null) {
                    _createdPlantId.value = newPlantId
                } else {
                    // Optional: handle retry or error if DB is extremely slow
                }
            }
        }
    }

    fun markPlantAsDeleted(plantId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                markPlantAsDeletedByIdUseCase(plantId)
                // If the marked deleted plant was the one currently loaded, clear the state
                if (_plantState.value?.plantId == plantId) {
                    _plantState.value = null
                }
                onComplete() // Callback to handle navigation (e.g., pop back to Dashboard)
            } catch (e: Exception) {
                // Handle error if necessary
                e.printStackTrace()
            }
        }
    }


    fun updatePlantName(plantId: String, newName: String) {
        viewModelScope.launch {
            try {
                updatePlantNameUseCase(plantId, newName)
                // Reload the plant to reflect the new name in the UI
                loadPlant(plantId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unlinkDevice(plantId: String) {
        viewModelScope.launch {
            try {
                unlinkIoTDeviceToPlantUseCase(plantId)
                // Reload the plant to reflect the new state (no device linked)
                loadPlant(plantId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}