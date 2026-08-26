package com.example.siliguide.presentation.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.model.ConnectionMode
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
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

    init {
        syncRepository.schedulePeriodicSync()
    }

    /**
     * Bumped by [refresh] to re-run the whole pipeline.
     *
     * The plant and device flows are SQLDelight-backed and already reactive, so
     * a refresh is only needed for the two things that are NOT reactive: the
     * live device ping and the latest-session lookup.
     */
    private val refreshTrigger = MutableStateFlow(0L)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<DashboardUiState> =
        combine(authRepository.getUserIdFlow(), refreshTrigger) { userId, _ -> userId }
            .flatMapLatest { userId ->

                if (userId == null) {
                    _isRefreshing.value = false
                    flowOf(DashboardUiState(error = "No user logged in"))
                } else {
                    combine(
                        getPlantsUseCase.invoke(userId),
                        getIoTDevicesUseCase.invoke(userId)
                    ) { plants, devices -> plants to devices }
                        // mapLatest so a fast second emission cancels an
                        // in-flight round of device pings instead of queueing
                        .mapLatest { (plants, devices) ->
                            withContext(kotlinx.coroutines.Dispatchers.IO) {
                                // Only enrich what the dashboard actually shows
                                val deviceOnline = pingDevices(devices.take(DEVICE_LIMIT))
                                val latestSessions = loadLatestSessions(plants.take(PLANT_LIMIT))

                                DashboardUiState(
                                    plants = plants,
                                    devices = devices,
                                    deviceOnline = deviceOnline,
                                    latestSessions = latestSessions,
                                    isLoading = false
                                )
                            }
                        }
                        .onEach { _isRefreshing.value = false }
                }
            }
            .flowOn(kotlinx.coroutines.Dispatchers.IO)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DashboardUiState(isLoading = true)
            )

    /** Pull-to-refresh. Re-pings every device, re-reads the latest sessions, and triggers cloud sync. */
    fun refresh() {
        _isRefreshing.value = true
        refreshTrigger.value = System.currentTimeMillis()
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            syncRepository.syncNow()
        }
    }

    /**
     * Live reachability per device.
     *
     * Executed strictly on Dispatchers.IO so blocking network and Bluetooth
     * RFCOMM socket connections never block the UI main thread.
     *
     *  1. Per-device deadline (PING_TIMEOUT_MS), so one unreachable device cannot hold the batch.
     *  2. Batch deadline (PING_BATCH_TIMEOUT_MS), so the dashboard settles within a fixed budget.
     */
    private suspend fun pingDevices(devices: List<IoTDevice>): Map<String, Boolean> =
        withContext(Dispatchers.IO) {
            if (devices.isEmpty()) return@withContext emptyMap()

            val results = withTimeoutOrNull(PING_BATCH_TIMEOUT_MS) {
                coroutineScope {
                    devices
                        .map { device ->
                            async(Dispatchers.IO) {
                                val online = withTimeoutOrNull(PING_TIMEOUT_MS) {
                                    runCatching {
                                        checkDeviceConnectionUseCase(device.deviceId)
                                    }.getOrDefault(false)
                                } ?: false

                                device.deviceId to online
                            }
                        }
                        .awaitAll()
                        .toMap()
                }
            }

            results ?: emptyMap()
        }

    private companion object {
        const val DEVICE_LIMIT = 2
        const val PLANT_LIMIT = 5

        const val PING_TIMEOUT_MS = 2_000L
        const val PING_BATCH_TIMEOUT_MS = 4_000L
    }

    /** Newest analysis per plant, or null where a plant has never been analyzed. */
    private suspend fun loadLatestSessions(
        plants: List<Plant>
    ): Map<String, AnalysisSession?> = withContext(kotlinx.coroutines.Dispatchers.IO) {
        if (plants.isEmpty()) return@withContext emptyMap()

        coroutineScope {
            plants
                .map { plant ->
                    async(kotlinx.coroutines.Dispatchers.IO) {
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