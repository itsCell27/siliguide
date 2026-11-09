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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltViewModel
class PreAnalysisViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getIoTDevicesUseCase: GetIoTDevicesUseCase,
    private val getPlantByIdUseCase: GetPlantByIdUseCase,
    private val permissionManager: PermissionManager,
    private val checkDeviceConnectionUseCase: CheckDeviceConnectionUseCase
) : ViewModel() {

    // Backing property for state updates
    private val _preUiState = MutableStateFlow(PreAnalysisUiState())
    // Public read-only stream for the UI to observe
    val preUiState: StateFlow<PreAnalysisUiState> = _preUiState.asStateFlow()

    private val _state = MutableStateFlow<PreAnalysisState>(PreAnalysisState.NoDevice())
    val state: StateFlow<PreAnalysisState> = _state.asStateFlow()

    var permissionStatus by mutableStateOf<PermissionStatus>(PermissionStatus.RequestPermission)
        private set

    var shouldShowPermissionDialog by mutableStateOf(false)
        private set

    fun loadData(plantId: String) {
        viewModelScope.launch {
            // 1. Initial loading state
            _preUiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    loadingMessage = "Initializing..."
                )
            }

            try {
                // 2. Switch to the IO thread for all database and Bluetooth work
                val stateUpdate = withContext(Dispatchers.IO) {

                    _preUiState.update { it.copy(loadingMessage = "Loading user data...") }
                    val userId = authRepository.getUserId() ?: return@withContext null

                    _preUiState.update { it.copy(loadingMessage = "Fetching plant details...") }
                    val plant = getPlantByIdUseCase(plantId) ?: return@withContext null

                    _preUiState.update { it.copy(loadingMessage = "Finding paired devices...") }
                    val allDevices = getIoTDevicesUseCase(userId).first()

                    val onlineDevices = mutableListOf<IoTDevice>()
                    val totalDevices = allDevices.size

                    // Use a standard indexed loop to show progress (e.g., "Checking 1 of 3")
                    allDevices.forEachIndexed { index, device ->

                        _preUiState.update {
                            it.copy(loadingMessage = "Connecting to device ${index + 1} of $totalDevices...")
                        }

                        // This blocking Bluetooth check now runs safely in the background
                        if (checkDeviceConnectionUseCase(device.deviceId)) {
                            onlineDevices.add(device)
                        }
                    }

                    _preUiState.update { it.copy(loadingMessage = "Finalizing dashboard...") }

                    // Return the computed state back to the outer coroutine
                    buildState(
                        plant = plant,
                        allDevices = allDevices,
                        onlineDevices = onlineDevices
                    )
                }

                // 3. Update the UI state (automatically back on the Main thread)
                if (stateUpdate != null) {
                    _state.value = stateUpdate
                }

            } catch (e: Exception) {
                // Optional: Show an error message briefly before hiding the loader
                _preUiState.update { it.copy(loadingMessage = "Error loading data") }
            } finally {
                _preUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        loadingMessage = "" // Clear the message when done
                    )
                }
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