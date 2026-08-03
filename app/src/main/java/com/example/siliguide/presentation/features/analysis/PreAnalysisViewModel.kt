package com.example.siliguide.presentation.features.analysis

import android.Manifest
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.core.permission.PermissionManager
import com.example.siliguide.core.permission.PermissionStatus
import com.example.siliguide.core.permission.PermissionType
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.usecase.CheckDeviceConnectionUseCase
import com.example.siliguide.domain.usecase.GetIoTDevicesUseCase
import com.example.siliguide.domain.usecase.GetPlantByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PreAnalysisViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getIoTDevicesUseCase: GetIoTDevicesUseCase,
    private val getPlantByIdUseCase: GetPlantByIdUseCase,
    private val permissionManager: PermissionManager,
    private val checkDeviceConnectionUseCase: CheckDeviceConnectionUseCase
) : ViewModel() {

    private val _state = MutableStateFlow<PreAnalysisState>(PreAnalysisState.NoDevice())
    val state: StateFlow<PreAnalysisState> = _state.asStateFlow()

    var permissionStatus by mutableStateOf<PermissionStatus>(PermissionStatus.RequestPermission)
        private set

    var shouldShowPermissionDialog by mutableStateOf(false)
        private set

    fun loadData(plantId: String) {
        viewModelScope.launch {
            val userId = authRepository.getUserId() ?: return@launch
            val plant = getPlantByIdUseCase(plantId) ?: return@launch

            getIoTDevicesUseCase(userId).collect { allDevices ->
                // BUG FIX: Replaced standard buildList { } to allow suspended operations
                val onlineDevices = mutableListOf<IoTDevice>()
                for (device in allDevices) {
                    if (checkDeviceConnectionUseCase(device.deviceId)) {
                        onlineDevices.add(device)
                    }
                }

                _state.value = buildState(
                    plant = plant,
                    allDevices = allDevices,
                    onlineDevices = onlineDevices
                )
            }
        }
    }

    private fun buildState(
        plant: Plant,
        allDevices: List<IoTDevice>,
        onlineDevices: List<IoTDevice>
    ): PreAnalysisState {

        val availableDevices = onlineDevices.map {
            OnTheGoDevice(deviceId = it.deviceId, name = it.deviceName, meta = "Connected")
        }

        // Plant has NO linked device
        if (plant.deviceId == null) {
            return PreAnalysisState.NoDevice(availableDevices = availableDevices)
        }

        // Find linked device
        val linkedDevice = allDevices.find { it.deviceId == plant.deviceId }
            ?: return PreAnalysisState.NoDevice(availableDevices = availableDevices)

        // Is linked device online?
        val linkedDeviceOnline = onlineDevices.any { it.deviceId == linkedDevice.deviceId }

        if (linkedDeviceOnline) {
            return PreAnalysisState.LinkedConnected(
                deviceId = linkedDevice.deviceId,
                deviceName = linkedDevice.deviceName,
                deviceMeta = "Connected",
                sensors = SensorPreviewData()
            )
        }

        // Linked device offline
        val otherOnlineDevices = onlineDevices
            .filter { it.deviceId != linkedDevice.deviceId }
            .map { OnTheGoDevice(deviceId = it.deviceId, name = it.deviceName, meta = "Connected") }

        return PreAnalysisState.LinkedDisconnected(
            deviceName = linkedDevice.deviceName,
            lastSeenLabel = "Disconnected",
            onTheGoDevices = otherOnlineDevices
        )
    }

    fun refreshCameraPermission(context: Context) {
        viewModelScope.launch {
            val status = permissionManager.getPermissionStatus(
                context = context,
                permission = Manifest.permission.CAMERA,
                type = PermissionType.CAMERA
            )
            permissionStatus = status

            if (status == PermissionStatus.Granted) {
                permissionManager.clearRequested(PermissionType.CAMERA)
            }
        }
    }

    fun markCameraPermissionRequested() {
        viewModelScope.launch { permissionManager.markRequested(PermissionType.CAMERA) }
    }

    fun clearCameraPermissionRequested() {
        viewModelScope.launch { permissionManager.clearRequested(PermissionType.CAMERA) }
    }

    fun showPermissionDialog() { shouldShowPermissionDialog = true }

    fun hidePermissionDialog() { shouldShowPermissionDialog = false }
}