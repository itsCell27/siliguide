package com.example.siliguide.presentation.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.PlantRepository
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import com.example.siliguide.domain.usecase.GetIoTDevicesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val iotRepository: IoTRepository, // Assuming you have this
    private val getPlantsUseCase: GetPlantsUseCase,
    private val getIoTDevicesUseCase: GetIoTDevicesUseCase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        getPlantsUseCase.invoke(),          // Flow<List<Plant>>
        getIoTDevicesUseCase.invoke()       // Flow<List<IoTDevice>>
    ) { plants, devices ->
        DashboardUiState(
            plants = plants,
            devices = devices,
            isLoading = false
        )
    }.stateIn(
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