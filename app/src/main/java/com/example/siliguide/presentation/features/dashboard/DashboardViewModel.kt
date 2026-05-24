package com.example.siliguide.presentation.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.PlantRepository
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import com.example.siliguide.domain.usecase.GetIoTDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val iotRepository: IoTRepository,
    private val getPlantsUseCase: GetPlantsUseCase,
    private val getIoTDevicesUseCase: GetIoTDevicesUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {


    // Reactive StateFlow: Automatically re-triggers when userId changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> = authRepository.getUserIdFlow()
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(DashboardUiState(error = "No user logged in"))
            } else {
                // Combine the two flows using the dynamic userId
                combine(
                    getPlantsUseCase.invoke(userId),
                    getIoTDevicesUseCase.invoke(userId)
                ) { plants, devices ->
                    DashboardUiState(
                        plants = plants,
                        devices = devices,
                        isLoading = false
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState(isLoading = true)
        )
}

data class DashboardUiState(
    val plants: List<Plant> = emptyList(),
    val devices: List<IoTDevice> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)