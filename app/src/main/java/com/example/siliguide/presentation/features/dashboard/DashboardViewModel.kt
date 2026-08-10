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
import com.example.siliguide.domain.usecase.CheckDeviceConnectionUseCase
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import com.example.siliguide.domain.usecase.GetIoTDevicesUseCase
import com.example.siliguide.domain.usecase.GetLatestSessionByPlantId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
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
    private val getLatestSessionByPlantId: GetLatestSessionByPlantId
) : ViewModel() {

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

                            // Only enrich what the dashboard actually shows
                            val deviceOnline = pingDevices(devices.take(DEVICE_LIMIT))
                            val latestSessions = loadLatestSessions(plants.take(PLANT_LIMIT))

                            _isRefreshing.value = false

                            DashboardUiState(
                                plants = plants,
                                devices = devices,
                                deviceOnline = deviceOnline,
                                latestSessions = latestSessions,
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

    /** Pull-to-refresh. Re-pings every device and re-reads the latest sessions. */
    fun refresh() {
        _isRefreshing.value = true
        refreshTrigger.value = System.currentTimeMillis()
    }

    /**
     * Live reachability per device.
     *
     * Three guards, because a liveness check must never be able to stall the
     * dashboard:
     *
     *  1. Bluetooth devices are NOT pinged. getSpecificDeviceInfo() opens a
     *     full RFCOMM socket, and BluetoothSocket.connect() is blocking and
     *     ignores withTimeoutOrNull — so it cannot even be bounded. It would
     *     also steal the singleton socket from any setup flow in progress.
     *     Their stored flag is shown instead.
     *  2. Per-device deadline, so one unreachable IP cannot hold the batch.
     *  3. Batch deadline, so the dashboard settles within a fixed budget no
     *     matter how many devices are saved.
     */
    private suspend fun pingDevices(devices: List<IoTDevice>): Map<String, Boolean> {

        if (devices.isEmpty()) return emptyMap()

        val results = withTimeoutOrNull(PING_BATCH_TIMEOUT_MS) {
            coroutineScope {
                devices
                    .map { device ->
                        async {
                            val online = when (device.connectionMode) {

                                ConnectionMode.BLUETOOTH -> device.isActive

                                else -> withTimeoutOrNull(PING_TIMEOUT_MS) {
                                    runCatching {
                                        checkDeviceConnectionUseCase(device.deviceId)
                                    }.getOrDefault(false)
                                } ?: false
                            }

                            device.deviceId to online
                        }
                    }
                    .awaitAll()
                    .toMap()
            }
        }

        return results ?: emptyMap()
    }

    private companion object {
        const val DEVICE_LIMIT = 2
        const val PLANT_LIMIT = 3

        const val PING_TIMEOUT_MS = 2_000L
        const val PING_BATCH_TIMEOUT_MS = 4_000L
    }

    /** Newest analysis per plant, or null where a plant has never been analyzed. */
    private suspend fun loadLatestSessions(
        plants: List<Plant>
    ): Map<String, AnalysisSession?> {

        if (plants.isEmpty()) return emptyMap()

        return coroutineScope {
            plants
                .map { plant ->
                    async {
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