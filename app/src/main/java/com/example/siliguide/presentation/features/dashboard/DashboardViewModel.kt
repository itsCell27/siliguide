package com.example.siliguide.presentation.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.PlantRepository
import com.example.siliguide.domain.repository.SyncRepository
import com.example.siliguide.domain.usecase.CheckDeviceConnectionUseCase
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import com.example.siliguide.domain.usecase.GetIoTDevicesUseCase
import com.example.siliguide.domain.usecase.GetLatestSessionByPlantId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val iotRepository: IoTRepository,
    private val getPlantsUseCase: GetPlantsUseCase,
    private val getIoTDevicesUseCase: GetIoTDevicesUseCase,
    private val authRepository: AuthRepository,
    private val checkDeviceConnectionUseCase: CheckDeviceConnectionUseCase,
    private val getLatestSessionByPlantId: GetLatestSessionByPlantId,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val refreshTrigger = MutableStateFlow(0L)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _deviceOnline = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    private val _latestSessions = MutableStateFlow<Map<String, AnalysisSession?>>(emptyMap())
    private val _isInitialLoading = MutableStateFlow(true)

    private var pingJob: Job? = null
    private var currentDevices: List<IoTDevice> = emptyList()

    init {
        syncRepository.schedulePeriodicSync()
        startPeriodicReachabilityCheck()
    }

    private var lastPushedDevices: List<IoTDevice>? = null
    private var lastPushedPlants: List<Plant>? = null

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> =
        authRepository.getUserIdFlow()
            .flatMapLatest { userId ->
                if (userId == null) {
                    _isRefreshing.value = false
                    _isInitialLoading.value = false
                    flowOf(DashboardUiState(error = "No user logged in", isLoading = false))
                } else {
                    combine(
                        getPlantsUseCase(userId),
                        getIoTDevicesUseCase(userId),
                        _deviceOnline,
                        _latestSessions,
                        _isInitialLoading
                    ) { plants, devices, onlineMap, sessions, isInitialLoading ->
                        if (devices != lastPushedDevices) {
                            lastPushedDevices = devices
                            currentDevices = devices
                            _isInitialLoading.value = false
                            triggerDevicePing(devices.take(DEVICE_LIMIT))
                        }

                        if (plants != lastPushedPlants) {
                            lastPushedPlants = plants
                            _isInitialLoading.value = false
                            viewModelScope.launch(Dispatchers.IO) {
                                val loadedSessions = loadLatestSessions(plants.take(PLANT_LIMIT))
                                _latestSessions.value = loadedSessions
                            }
                        }

                        DashboardUiState(
                            plants = plants,
                            devices = devices,
                            deviceOnline = onlineMap,
                            latestSessions = sessions,
                            isLoading = isInitialLoading
                        )
                    }
                }
            }
            .flowOn(Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DashboardUiState(isLoading = true)
            )

    /** Pull-to-refresh. Re-pings every device, re-reads the latest sessions, and triggers cloud sync. */
    fun refresh() {
        _isRefreshing.value = true
        refreshTrigger.value = System.currentTimeMillis()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncRepository.syncNow()
                if (currentDevices.isNotEmpty()) {
                    val online = pingDevices(currentDevices.take(DEVICE_LIMIT))
                    _deviceOnline.value = online
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun triggerDevicePing(devices: List<IoTDevice>) {
        pingJob?.cancel()
        pingJob = viewModelScope.launch(Dispatchers.IO) {
            val online = pingDevices(devices)
            _deviceOnline.value = online
        }
    }

    private fun startPeriodicReachabilityCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                delay(PERIODIC_PING_INTERVAL_MS)
                if (currentDevices.isNotEmpty() && !_isRefreshing.value) {
                    val online = pingDevices(currentDevices.take(DEVICE_LIMIT))
                    _deviceOnline.value = online
                }
            }
        }
    }

    /**
     * Live reachability per device.
     *
     * Executed strictly on Dispatchers.IO so blocking network and Bluetooth
     * RFCOMM socket connections never block the UI main thread.
     */
    private suspend fun pingDevices(devices: List<IoTDevice>): Map<String, Boolean> =
        withContext(Dispatchers.IO) {
            if (devices.isEmpty()) return@withContext emptyMap()

            val resultMap = mutableMapOf<String, Boolean>()
            withTimeoutOrNull(PING_BATCH_TIMEOUT_MS) {
                coroutineScope {
                    val deferreds = devices.map { device ->
                        async(Dispatchers.IO) {
                            val online = withTimeoutOrNull(PING_TIMEOUT_MS) {
                                runCatching {
                                    checkDeviceConnectionUseCase(device.deviceId)
                                }.getOrDefault(false)
                            } ?: false

                            device.deviceId to online
                        }
                    }

                    deferreds.forEach { deferred ->
                        try {
                            val (id, online) = deferred.await()
                            resultMap[id] = online
                        } catch (_: Exception) {
                            // Leave as false on error
                        }
                    }
                }
            }

            // Ensure all target devices have an explicit boolean value in the map
            devices.forEach { device ->
                if (!resultMap.containsKey(device.deviceId)) {
                    resultMap[device.deviceId] = false
                }
            }

            resultMap
        }

    /** Newest analysis per plant, or null where a plant has never been analyzed. */
    private suspend fun loadLatestSessions(
        plants: List<Plant>
    ): Map<String, AnalysisSession?> = withContext(Dispatchers.IO) {
        if (plants.isEmpty()) return@withContext emptyMap()

        coroutineScope {
            plants
                .map { plant ->
                    async(Dispatchers.IO) {
                        val session = runCatching {
                            getLatestSessionByPlantId(plant.plantId)
                        }.getOrNull()

                        plant.plantId to session
                    }
                }
                .awaitAll()
                .toMap()
        }
    }

    private companion object {
        const val DEVICE_LIMIT = 2
        const val PLANT_LIMIT = 5

        const val PING_TIMEOUT_MS = 2_000L
        const val PING_BATCH_TIMEOUT_MS = 4_000L
        const val PERIODIC_PING_INTERVAL_MS = 12_000L
    }
}

data class DashboardUiState(
    val plants: List<Plant> = emptyList(),
    val devices: List<IoTDevice> = emptyList(),
    /** Live reachability keyed by deviceId — not the stored is_active flag. */
    val deviceOnline: Map<String, Boolean> = emptyMap(),
    /** Newest analysis keyed by plantId; null value means never analyzed. */
    val latestSessions: Map<String, AnalysisSession?> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)