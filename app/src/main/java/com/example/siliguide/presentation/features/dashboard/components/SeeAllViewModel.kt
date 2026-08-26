package com.example.siliguide.presentation.features.dashboard.components

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.usecase.CheckDeviceConnectionUseCase
import com.example.siliguide.domain.usecase.GetIoTDevicesUseCase
import com.example.siliguide.domain.usecase.GetLatestSessionByPlantId
import com.example.siliguide.domain.usecase.GetPlantsUseCase
import com.example.siliguide.domain.usecase.GetUnlinkedDevicesUseCase
import com.example.siliguide.domain.usecase.LinkIoTDeviceToPlantUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * Backs both See All screens.
 *
 * Unlike the dashboard, nothing is capped here — that is the whole point of the
 * screen. Filtering happens in the ViewModel rather than the composable so the
 * count badge, the summary line and the rendered list are derived from one
 * source and cannot drift apart.
 */
@HiltViewModel
class SeeAllViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val getPlantsUseCase: GetPlantsUseCase,
    private val getIoTDevicesUseCase: GetIoTDevicesUseCase,
    private val checkDeviceConnectionUseCase: CheckDeviceConnectionUseCase,
    private val getLatestSessionByPlantId: GetLatestSessionByPlantId,
    private val linkIoTDeviceToPlantUseCase: LinkIoTDeviceToPlantUseCase,
    private val getUnlinkedDevicesUseCase: GetUnlinkedDevicesUseCase
) : ViewModel() {

    // Automatically check if we navigated here to link a device
    val plantIdToLink: String? = savedStateHandle.get<String>("plantId")
    val isLinkingMode: Boolean = plantIdToLink != null

    private val _deviceQuery = MutableStateFlow("")
    val deviceQuery: StateFlow<String> = _deviceQuery.asStateFlow()

    private val _plantQuery = MutableStateFlow("")
    val plantQuery: StateFlow<String> = _plantQuery.asStateFlow()

    fun onDeviceQueryChange(value: String) {
        _deviceQuery.value = value
    }

    fun onPlantQueryChange(value: String) {
        _plantQuery.value = value
    }

    // ── Devices ───────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allDevices: StateFlow<List<SeeAllDeviceItem>> =
        authRepository.getUserIdFlow()
            .flatMapLatest { userId ->

                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    // SWITCH: Use getUnlinkedDevicesUseCase if we are linking a plant
                    val devicesFlow = if (isLinkingMode) {
                        getUnlinkedDevicesUseCase(userId)
                    } else {
                        getIoTDevicesUseCase(userId)
                    }

                    devicesFlow
                        // mapLatest so a DB write during a ping round cancels it
                        // instead of queueing another
                        .mapLatest { devices ->

                            val online = pingDevices(devices)

                            devices.map { device ->
                                SeeAllDeviceItem(
                                    deviceId = device.deviceId,
                                    name = device.deviceName,
                                    mode = device.connectionMode,
                                    isOnline = online[device.deviceId] ?: device.isActive,
                                )
                            }
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val visibleDevices: StateFlow<List<SeeAllDeviceItem>> =
        combine(allDevices, _deviceQuery) { devices, query ->
            if (query.isBlank()) {
                devices
            } else {
                devices.filter { it.name.contains(query.trim(), ignoreCase = true) }
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ── Plants ────────────────────────────────────────────────────────────

    @OptIn(ExperimentalCoroutinesApi::class)
    private val allPlants: StateFlow<List<SeeAllPlantItem>> =
        authRepository.getUserIdFlow()
            .flatMapLatest { userId ->

                if (userId == null) {
                    flowOf(emptyList())
                } else {
                    getPlantsUseCase(userId)
                        .mapLatest { plants ->

                            val sessions = loadLatestSessions(plants)

                            plants.map { plant ->

                                val session = sessions[plant.plantId]

                                SeeAllPlantItem(
                                    plantId = plant.plantId,
                                    name = plant.name,
                                    status = session.toStatus(),
                                    conditionLabel = session.toLabel(),
                                    lastAnalyzedAt = session?.sessionTimestamp,
                                    lastAnalyzedImagePath = session?.imagePath
                                )
                            }
                        }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ── Actions ────────────────────────────────────────────────────────────

    // ADDED onSuccess callback so the UI knows exactly when to pop back to the plant detail screen
    fun linkPlantToDevice(deviceId: String, plantId: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching {
                linkIoTDeviceToPlantUseCase(
                    deviceId = deviceId,
                    plantId = plantId
                )
            }.onSuccess {
                onSuccess()
            }.onFailure { exception ->
                exception.printStackTrace()
            }
        }
    }

    val visiblePlants: StateFlow<List<SeeAllPlantItem>> =
        combine(allPlants, _plantQuery) { plants, query ->
            if (query.isBlank()) {
                plants
            } else {
                plants.filter { it.name.contains(query.trim(), ignoreCase = true) }
            }
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )


    // ── Enrichment ────────────────────────────────────────────────────────

    /**
     * Live reachability per device.
     *
     * Same three guards as the dashboard, and they matter more here because
     * this list is uncapped:
     *
     *  1. Bluetooth is not pinged — getSpecificDeviceInfo() opens a blocking
     *     RFCOMM socket that ignores withTimeoutOrNull, and it would steal the
     *     singleton socket from any setup flow in progress.
     *  2. Per-device deadline, so one dead IP cannot hold the batch.
     *  3. Batch deadline, so twenty saved devices still settle in seconds.
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

    private companion object {
        const val PING_TIMEOUT_MS = 2_000L
        const val PING_BATCH_TIMEOUT_MS = 6_000L
    }
}

/* ── Session mapping ───────────────────────────────────────────────────── */

/**
 * Badge severity for a plant's newest analysis.
 *
 * A partial analysis ran without sensor data, so the rule engine never produced
 * a plantCondition — the leaf symptom is all there is.
 */
private fun AnalysisSession?.toStatus(): String {

    if (this == null) return "none"

    return if (predictedClass.equals("healthy", ignoreCase = true)) {
        "healthy"
    } else {
        "warning"
    }
}

/** Rule-engine condition when available, otherwise the leaf symptom. */
private fun AnalysisSession?.toLabel(): String {

    if (this == null) return ""

    plantCondition?.takeIf { it.isNotBlank() }?.let { return it }

    return predictedClass.humanizeSymptom()
}

/** "leaf_curling" → "Leaf Curling" */
private fun String.humanizeSymptom(): String =
    split('_', '-', ' ')
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }