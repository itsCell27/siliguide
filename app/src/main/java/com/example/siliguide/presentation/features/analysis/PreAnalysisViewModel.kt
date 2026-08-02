package com.example.siliguide.presentation.features.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.usecase.GetIoTDevicesUseCase
import com.example.siliguide.domain.usecase.GetPlantByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.Manifest
import android.content.Context
import com.example.siliguide.core.permission.PermissionManager
import com.example.siliguide.core.permission.PermissionStatus
import com.example.siliguide.core.permission.PermissionType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@HiltViewModel
class PreAnalysisViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getIoTDevicesUseCase: GetIoTDevicesUseCase,
    private val getPlantByIdUseCase: GetPlantByIdUseCase,
    private val permissionManager: PermissionManager
) : ViewModel() {

    private val _state = MutableStateFlow<PreAnalysisState>(PreAnalysisState.NoDevice)
    val state: StateFlow<PreAnalysisState> = _state.asStateFlow()

    var permissionStatus by mutableStateOf<PermissionStatus>(
        PermissionStatus.RequestPermission
    )
        private set

    var shouldShowPermissionDialog by mutableStateOf(false)
        private set

    fun loadData(plantId: String) {
        viewModelScope.launch {
            val userId = authRepository.getUserId() ?: return@launch
            val plant = getPlantByIdUseCase(plantId) ?: return@launch

            // Observe devices for this user
            getIoTDevicesUseCase(userId).collect { allDevices ->
                // Filter only devices that are currently active/online
                val onlineDevices = allDevices.filter { it.isActive }

                // Determine the UI state based on linked device status and other available devices
                _state.value = buildState(plant, onlineDevices)
            }
        }
    }

    private fun buildState(plant: Plant, onlineDevices: List<IoTDevice>): PreAnalysisState {
        // Check if the specific device linked to this plant is online
        val linkedDevice = onlineDevices.find { it.deviceId == plant.deviceId }

        return if (linkedDevice != null) {
            // State 7A: Linked device is connected
            PreAnalysisState.LinkedConnected(
                deviceName = linkedDevice.deviceName,
                deviceMeta = "Connected",
                sensors = SensorPreviewData(), // Placeholder: Real sensor data fetched via separate logic
                deviceId = "dev_1"
            )
        } else if (onlineDevices.isNotEmpty()) {
            // State 7B: Linked device is off, but other devices are online
            PreAnalysisState.LinkedDisconnected(
                deviceName = "Main Sensor",
                lastSeenLabel = "Disconnected",
                onTheGoDevices = onlineDevices.map {
                    OnTheGoDevice(it.deviceId, it.deviceName, "Online")
                }
            )
        } else {
            // State 7C: No connected devices found
            PreAnalysisState.NoDevice
        }
    }

    fun refreshCameraPermission(
        context: Context
    ) {

        viewModelScope.launch {

            val status =
                permissionManager.getPermissionStatus(
                    context = context,
                    permission = Manifest.permission.CAMERA,
                    type = PermissionType.CAMERA
                )

            permissionStatus = status

            if (status == PermissionStatus.Granted) {

                permissionManager.clearRequested(
                    PermissionType.CAMERA
                )
            }
        }
    }

    fun markCameraPermissionRequested() {

        viewModelScope.launch {

            permissionManager.markRequested(
                PermissionType.CAMERA
            )
        }
    }

    fun clearCameraPermissionRequested() {

        viewModelScope.launch {

            permissionManager.clearRequested(
                PermissionType.CAMERA
            )
        }
    }

    fun showPermissionDialog() {

        shouldShowPermissionDialog = true
    }

    fun hidePermissionDialog() {

        shouldShowPermissionDialog = false
    }
}