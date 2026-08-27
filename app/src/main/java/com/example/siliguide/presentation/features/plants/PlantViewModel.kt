package com.example.siliguide.presentation.features.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.model.LastDeviceSensorData
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.usecase.AddPlantUseCase
import com.example.siliguide.domain.usecase.DeletePlantByIdUseCase
import com.example.siliguide.domain.usecase.FetchSensorsUseCase
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import com.example.siliguide.domain.usecase.GetAnalysisSessionsByPlantUseCase
import com.example.siliguide.domain.usecase.GetPlantByIdUseCase
import com.example.siliguide.domain.usecase.LinkIoTDeviceToPlantUseCase
import com.example.siliguide.domain.usecase.MarkPlantAsDeletedByIdUseCase
import com.example.siliguide.domain.usecase.UnlinkIoTDeviceToPlantUseCase
import com.example.siliguide.domain.usecase.UpdatePlantNameUseCase
import com.example.siliguide.domain.engine.SiliGuideThresholds
import com.example.siliguide.domain.engine.ThresholdEngine
import com.example.siliguide.core.ui.components.monitoring.MonitoringReading
import com.example.siliguide.core.ui.components.monitoring.MonitoringStatus
import com.example.siliguide.domain.usecase.GetIoTDeviceByIdUseCase
import com.example.siliguide.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject


const val MAX_PLANT_NAME_LENGTH = 20


@HiltViewModel
class PlantViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getPlantsUseCase: GetPlantsUseCase,
    private val getPlantByIdUseCase: GetPlantByIdUseCase,
    private val getAnalysisSessionsByPlantUseCase: GetAnalysisSessionsByPlantUseCase,
    private val deletePlantByIdUseCase: DeletePlantByIdUseCase,
    private val linkIoTDeviceToPlantUseCase: LinkIoTDeviceToPlantUseCase,
    private val unlinkIoTDeviceToPlantUseCase: UnlinkIoTDeviceToPlantUseCase,
    private val updatePlantNameUseCase: UpdatePlantNameUseCase,
    private val markPlantAsDeletedByIdUseCase: MarkPlantAsDeletedByIdUseCase,
    private val fetchSensorsUseCase: FetchSensorsUseCase,
    private val ioTRepository: IoTRepository,
    private val getIoTDeviceByIdUseCase: GetIoTDeviceByIdUseCase,
    private val syncRepository: SyncRepository
) : ViewModel() {

    // Reactive Plant Stream: Automatically scoped to the current userId
    @OptIn(ExperimentalCoroutinesApi::class)
    val plants: StateFlow<List<Plant>> = authRepository.getUserIdFlow()
        .flatMapLatest { userId ->
            if (userId != null) {
                getPlantsUseCase(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _plantState = MutableStateFlow<Plant?>(null)
    val plantState: StateFlow<Plant?> = _plantState.asStateFlow()

    private val _analysisHistoryState = MutableStateFlow<List<AnalysisSession>>(emptyList())
    val analysisHistoryState: StateFlow<List<AnalysisSession>> = _analysisHistoryState.asStateFlow()

    private val _iotDeviceState = MutableStateFlow<IoTDevice?>(null)
    val iotDeviceState: StateFlow<IoTDevice?> = _iotDeviceState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPlant(plantId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val (loadedPlant, history) = withContext(Dispatchers.IO) {
                    val plant = getPlantByIdUseCase(plantId)
                    val historyList = getAnalysisSessionsByPlantUseCase(plantId)
                    plant to historyList
                }
                _plantState.value = loadedPlant
                _analysisHistoryState.value = history

                val deviceId = loadedPlant.deviceId
                _iotDeviceState.value = if (deviceId != null) {
                    withContext(Dispatchers.IO) {
                        getIoTDeviceByIdUseCase(deviceId)
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                _plantState.value = null
                _iotDeviceState.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markPlantAsDeleted(plantId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            val result = deletePlantByIdUseCase(plantId)
            result.fold(
                onSuccess = {
                    if (_plantState.value?.plantId == plantId) {
                        _plantState.value = null
                    }
                    onComplete()
                },
                onFailure = { e ->
                    e.printStackTrace()
                }
            )
        }
    }


    fun updatePlantName(plantId: String, newName: String) {
        viewModelScope.launch {
            try {
                updatePlantNameUseCase(plantId, newName)
                syncRepository.enqueueOneTimeSync()
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
                syncRepository.enqueueOneTimeSync()
                // Reload the plant to reflect the new state (no device linked)
                loadPlant(plantId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ── Live device monitoring (Plant Detail's "Live monitoring" card) ─────
    //
    // Same poll-every-3s / fall-back-to-last-known-reading shape as
    // IoTDeviceDetailViewModel, just scoped to whichever device is linked
    // to the plant currently on screen, and reporting straight into
    // MonitoringCard's own status/reading types.

    data class LiveMonitoringUiState(
        val status: MonitoringStatus = MonitoringStatus.RECONNECTING,
        val readings: List<MonitoringReading> = emptyList(),
        val lastSeenText: String? = null,
        val staleNote: String? = null
    )

    private val _liveMonitoring = MutableStateFlow(LiveMonitoringUiState())
    val liveMonitoring: StateFlow<LiveMonitoringUiState> = _liveMonitoring.asStateFlow()

    private var monitorJob: Job? = null
    private var monitoredDeviceId: String? = null

    /** Call when the screen has a linked deviceId to watch. Safe to call every recomposition. */
    fun startLiveMonitoring(deviceId: String) {

        if (monitoredDeviceId == deviceId && monitorJob?.isActive == true) return

        monitoredDeviceId = deviceId
        monitorJob?.cancel()

        _liveMonitoring.value = LiveMonitoringUiState(status = MonitoringStatus.RECONNECTING)

        monitorJob = viewModelScope.launch {

            while (currentCoroutineContext().isActive) {

                pollDeviceOnce(deviceId)

                delay(MONITOR_POLL_INTERVAL_MS)
            }
        }
    }

    /** Call when the screen no longer has a device to watch (unlinked / left screen). */
    fun stopLiveMonitoring() {

        monitorJob?.cancel()
        monitorJob = null
        monitoredDeviceId = null
    }

    /**
     * "Try to Reconnect" button. Unlike startLiveMonitoring (which is safe to
     * call every recomposition and no-ops if a job is already running),
     * this always cancels and restarts so the button reliably flips status
     * to RECONNECTING and shows the loading animation, same as
     * IoTDeviceDetailViewModel.retryNow().
     */
    fun retryReconnect() {

        val id = monitoredDeviceId ?: return

        monitorJob?.cancel()

        _liveMonitoring.value = LiveMonitoringUiState(status = MonitoringStatus.RECONNECTING)

        monitorJob = viewModelScope.launch {

            while (currentCoroutineContext().isActive) {

                pollDeviceOnce(id)

                delay(MONITOR_POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollDeviceOnce(deviceId: String) {

        val response = withContext(Dispatchers.IO) {
            runCatching {
                withTimeoutOrNull(MONITOR_FETCH_TIMEOUT_MS) {
                    fetchSensorsUseCase(deviceId)
                }
            }.getOrNull()
        }

        if (response != null) {

            onLiveDeviceReading(deviceId, response)
        } else {

            onDeviceOffline(deviceId)
        }
    }

    private suspend fun onLiveDeviceReading(deviceId: String, response: SensorResponse) {

        withContext(Dispatchers.IO) {
            runCatching {
                ioTRepository.updateLastDeviceSensorData(
                    deviceId = deviceId,
                    lastSensorData = LastDeviceSensorData(
                        lastSeenAt = System.currentTimeMillis(),
                        lastSoilMoisture = response.soil.moisture,
                        lastSoilTemp = response.soil.temperature,
                        lastSoilPh = response.soil.ph,
                        lastElectricalConductivity = response.soil.ec,
                        lastNitrogen = response.soil.nitrogen,
                        lastPhosphorus = response.soil.phosphorus,
                        lastPotassium = response.soil.potassium,
                        lastAirTemp = response.environment.airTemperature,
                        lastLightIntensity = response.environment.light
                    )
                )
            }
        }

        _liveMonitoring.value = LiveMonitoringUiState(
            status = MonitoringStatus.CONNECTED,
            readings = buildMonitoringReadings(
                moisture = response.soil.moisture,
                soilTemp = response.soil.temperature,
                ph = response.soil.ph,
                nitrogen = response.soil.nitrogen,
                phosphorus = response.soil.phosphorus,
                potassium = response.soil.potassium,
                ec = response.soil.ec,
                airTemp = response.environment.airTemperature,
                light = response.environment.light
            )
        )
    }

    private suspend fun onDeviceOffline(deviceId: String) {

        val last = withContext(Dispatchers.IO) {
            runCatching { ioTRepository.getLastDeviceSensorData(deviceId) }.getOrNull()
        }

        val ago = last?.lastSeenAt?.let { monitorTimeAgo(it) }

        _liveMonitoring.value = LiveMonitoringUiState(
            status = MonitoringStatus.OFFLINE,
            readings = if (last == null) emptyList() else buildMonitoringReadings(
                moisture = last.lastSoilMoisture,
                soilTemp = last.lastSoilTemp,
                ph = last.lastSoilPh,
                nitrogen = last.lastNitrogen,
                phosphorus = last.lastPhosphorus,
                potassium = last.lastPotassium,
                ec = last.lastElectricalConductivity,
                airTemp = last.lastAirTemp,
                light = last.lastLightIntensity
            ),
            lastSeenText = ago?.let { "Last seen $it" },
            staleNote = ago?.let { "Showing last known readings · $it" }
        )
    }

    private fun buildMonitoringReadings(
        moisture: Double?,
        soilTemp: Double?,
        ph: Double?,
        nitrogen: Int?,
        phosphorus: Int?,
        potassium: Int?,
        ec: Int?,
        airTemp: Double?,
        light: Int?
    ): List<MonitoringReading> = listOf(
        MonitoringReading(
            "Soil Moisture", moisture.percent(),
            ThresholdEngine.evaluate(moisture, SiliGuideThresholds.SOIL_MOISTURE)
        ),
        MonitoringReading(
            "Soil Temp", soilTemp.celsius(),
            ThresholdEngine.evaluate(soilTemp, SiliGuideThresholds.SOIL_TEMP)
        ),
        MonitoringReading(
            "Soil pH", ph.decimal(1),
            ThresholdEngine.evaluate(ph, SiliGuideThresholds.SOIL_PH)
        ),
        MonitoringReading(
            "Soil N", nitrogen.ppm(),
            ThresholdEngine.evaluate(nitrogen?.toDouble(), SiliGuideThresholds.NITROGEN)
        ),
        MonitoringReading(
            "Soil P", phosphorus.ppm(),
            ThresholdEngine.evaluate(phosphorus?.toDouble(), SiliGuideThresholds.PHOSPHORUS)
        ),
        MonitoringReading(
            "Soil K", potassium.ppm(),
            ThresholdEngine.evaluate(potassium?.toDouble(), SiliGuideThresholds.POTASSIUM)
        ),
        MonitoringReading(
            "Soil EC", ec?.toString() ?: MONITOR_DASH,
            ThresholdEngine.evaluate(ec?.toDouble(), SiliGuideThresholds.SOIL_EC)
        ),
        MonitoringReading(
            "Air Temp", airTemp.celsius(),
            ThresholdEngine.evaluate(airTemp, SiliGuideThresholds.AIR_TEMP)
        ),
        MonitoringReading(
            "Light", light.lux(),
            ThresholdEngine.evaluate(light?.toDouble(), SiliGuideThresholds.LIGHT_INTENSITY)
        )
    )

    private fun Double?.percent(): String =
        this?.let { "${monitorTrim(it, 0)}%" } ?: MONITOR_DASH

    private fun Double?.celsius(): String =
        this?.let { "${monitorTrim(it, 1)}°C" } ?: MONITOR_DASH

    private fun Double?.decimal(digits: Int): String =
        this?.let { monitorTrim(it, digits) } ?: MONITOR_DASH

    private fun Int?.ppm(): String =
        this?.let { "$it ppm" } ?: MONITOR_DASH

    private fun Int?.lux(): String = when {
        this == null -> MONITOR_DASH
        this >= 1000 -> "${monitorTrim(this / 1000.0, 1)}k lx"
        else -> "$this lx"
    }

    private fun monitorTrim(value: Double, digits: Int): String =
        String.format(Locale.US, "%.${digits}f", value)

    private fun monitorTimeAgo(epochMillis: Long): String {

        val minutes = (System.currentTimeMillis() - epochMillis) / 60_000

        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 1_440 -> "${minutes / 60} hr ago"
            else -> "${minutes / 1_440} d ago"
        }
    }

    override fun onCleared() {
        stopLiveMonitoring()
        super.onCleared()
    }

    private companion object {
        const val MONITOR_POLL_INTERVAL_MS = 1_000L
        const val MONITOR_FETCH_TIMEOUT_MS = 2_500L
        const val MONITOR_DASH = "—"
    }
}