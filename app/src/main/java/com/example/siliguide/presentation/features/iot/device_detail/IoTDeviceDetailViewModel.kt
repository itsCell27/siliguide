package com.example.siliguide.presentation.features.iot.device_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.model.LastDeviceSensorData
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.usecase.FetchSensorsUseCase
import com.example.siliguide.domain.usecase.UpdateDeviceNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import android.util.Log
import com.example.siliguide.domain.usecase.GetLinkedPlantByDeviceIdUseCase
import com.example.siliguide.domain.usecase.UnlinkIoTDeviceToPlantUseCase
import com.example.siliguide.domain.engine.SiliGuideThresholds
import com.example.siliguide.domain.engine.ThresholdEngine
import com.example.siliguide.domain.usecase.DeleteDeviceByIdUseCase
import com.example.siliguide.presentation.features.iot.DEVICE_NAME_MAX_LENGTH

@HiltViewModel
class IoTDeviceDetailViewModel @Inject constructor(
    private val ioTRepository: IoTRepository,
    private val fetchSensorsUseCase: FetchSensorsUseCase,
    private val updateDeviceNameUseCase: UpdateDeviceNameUseCase,
    private val getLinkedPlantByDeviceIdUseCase: GetLinkedPlantByDeviceIdUseCase,
    private val unlinkIoTDeviceToPlantUseCase: UnlinkIoTDeviceToPlantUseCase,
    private val deleteDeviceByIdUseCase: DeleteDeviceByIdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        IoTDeviceDetailUiState(
            deviceName = "",
            status = DeviceConnectionStatus.RECONNECTING,
            sensors = emptyList(),
            deviceIdentifier = "",
            firmware = "—",
            dateRegistered = "—",
            connectionMode = ConnectionMode.AP
        )
    )
    val uiState: StateFlow<IoTDeviceDetailUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var deviceId: String? = null
    private var currentLinkedPlantId: String? = null

    // --- DIALOG STATE ---
    private val _offlineWarningMessage = MutableStateFlow<String?>(null)
    val offlineWarningMessage = _offlineWarningMessage.asStateFlow()

    fun dismissOfflineWarning() {
        _offlineWarningMessage.value = null
    }

    // --- REMOVE DEVICE RESULT DIALOG ---
    data class RemoveDeviceDialogUi(
        val title: String,
        val message: String,
        val shouldNavigateOnDismiss: Boolean = false
    )

    private val _removeDeviceDialog = MutableStateFlow<RemoveDeviceDialogUi?>(null)
    val removeDeviceDialog = _removeDeviceDialog.asStateFlow()

    fun dismissRemoveDeviceDialog() {
        _removeDeviceDialog.value = null
    }

    /** Called once the screen enters composition. Starts the 3s poll loop. */
    fun start(deviceId: String) {

        if (this.deviceId == deviceId && pollJob?.isActive == true) return

        this.deviceId = deviceId
        restartPolling(deviceId)
    }

    /** Called when the screen leaves composition. */
    fun stop() {

        pollJob?.cancel()
        pollJob = null
    }

    /** "Try to Reconnect" button. */
    fun retryNow() {

        val id = deviceId ?: return
        restartPolling(id)
    }

    private fun restartPolling(deviceId: String) {

        pollJob?.cancel()

        _uiState.update {
            it.copy(status = DeviceConnectionStatus.RECONNECTING)
        }

        pollJob = viewModelScope.launch {

            loadDeviceInfo(deviceId)

            while (currentCoroutineContext().isActive) {

                refreshOnce(deviceId)

                delay(POLL_INTERVAL_MS)
            }
        }
    }

    // ── Device info (static rows) ─────────────────────────────────────────

    private suspend fun loadDeviceInfo(deviceId: String) {

        val device = withContext(Dispatchers.IO) {
            runCatching { ioTRepository.getDeviceById(deviceId) }.getOrNull()
        } ?: return

        val plant = device.deviceId?.let { deviceId ->
            withContext(Dispatchers.IO) {
                runCatching { getLinkedPlantByDeviceIdUseCase(deviceId) }.getOrNull()
            }
        }

        currentLinkedPlantId = plant?.plantId

        val linkedPlantUi = plant?.let {
            LinkedPlantUi(
                name = it.name,
                meta = "Linked to device" // Format this based on your available plant data
            )
        }

        _uiState.update {
            it.copy(
                deviceName = device.deviceName,
                mode = device.connectionMode.toDetailMode(),
                deviceIdentifier = device.deviceId,
                firmware = device.firmware,
                dateRegistered = formatDate(device.dateAdded),
                linkedPlant = linkedPlantUi,
                connectionMode = device.connectionMode
            )
        }
    }

    // ── One poll tick ─────────────────────────────────────────────────────

    private suspend fun refreshOnce(deviceId: String) {

        val response = withContext(Dispatchers.IO) {
            runCatching {
                withTimeoutOrNull(FETCH_TIMEOUT_MS) {
                    fetchSensorsUseCase(deviceId)
                }
            }.getOrNull()
        }

        if (response != null) {

            onLiveReading(deviceId, response)
        } else {

            onOffline(deviceId)
        }
    }

    /** Device answered: show live values and persist them onto iot_devices. */
    private suspend fun onLiveReading(
        deviceId: String,
        response: SensorResponse
    ) {

        val now = System.currentTimeMillis()

        withContext(Dispatchers.IO) {
            runCatching {
                ioTRepository.updateLastDeviceSensorData(
                    deviceId = deviceId,
                    lastSensorData = LastDeviceSensorData(
                        lastSeenAt = now,
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

        _uiState.update {
            it.copy(
                status = DeviceConnectionStatus.CONNECTED,
                sensors = buildSensorCells(
                    moisture = response.soil.moisture,
                    soilTemp = response.soil.temperature,
                    ph = response.soil.ph,
                    nitrogen = response.soil.nitrogen,
                    phosphorus = response.soil.phosphorus,
                    potassium = response.soil.potassium,
                    ec = response.soil.ec,
                    airTemp = response.environment.airTemperature,
                    light = response.environment.light
                ),
                lastSeenText = null,
                staleNote = null
            )
        }
    }

    /** Device unreachable: fall back to the last values stored on iot_devices. */
    private suspend fun onOffline(deviceId: String) {

        val last = withContext(Dispatchers.IO) {
            runCatching { ioTRepository.getLastDeviceSensorData(deviceId) }.getOrNull()
        }

        val ago = last?.lastSeenAt?.let { timeAgo(it) }

        _uiState.update {
            it.copy(
                status = DeviceConnectionStatus.OFFLINE,
                sensors = if (last == null) emptyList() else buildSensorCells(
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
                lastSeenText = ago?.let { t -> "Last seen $t" },
                staleNote = ago?.let { t -> "Showing last known readings · $t" }
            )
        }
    }

    // ── Formatting ────────────────────────────────────────────────────────

    private fun buildSensorCells(
        moisture: Double?,
        soilTemp: Double?,
        ph: Double?,
        nitrogen: Int?,
        phosphorus: Int?,
        potassium: Int?,
        ec: Int?,
        airTemp: Double?,
        light: Int?
    ): List<SensorReadingUi> = listOf(
        SensorReadingUi(
            "Soil Moisture", moisture.percent(),
            ThresholdEngine.evaluate(moisture, SiliGuideThresholds.SOIL_MOISTURE)
        ),
        SensorReadingUi(
            "Soil Temp", soilTemp.celsius(),
            ThresholdEngine.evaluate(soilTemp, SiliGuideThresholds.SOIL_TEMP)
        ),
        SensorReadingUi(
            "Soil pH", ph.decimal(1),
            ThresholdEngine.evaluate(ph, SiliGuideThresholds.SOIL_PH)
        ),
        SensorReadingUi(
            "Soil N", nitrogen.ppm(),
            ThresholdEngine.evaluate(nitrogen?.toDouble(), SiliGuideThresholds.NITROGEN)
        ),
        SensorReadingUi(
            "Soil P", phosphorus.ppm(),
            ThresholdEngine.evaluate(phosphorus?.toDouble(), SiliGuideThresholds.PHOSPHORUS)
        ),
        SensorReadingUi(
            "Soil K", potassium.ppm(),
            ThresholdEngine.evaluate(potassium?.toDouble(), SiliGuideThresholds.POTASSIUM)
        ),
        SensorReadingUi(
            "Soil EC", ec?.toString() ?: DASH,
            ThresholdEngine.evaluate(ec?.toDouble(), SiliGuideThresholds.SOIL_EC)
        ),
        SensorReadingUi(
            "Air Temp", airTemp.celsius(),
            ThresholdEngine.evaluate(airTemp, SiliGuideThresholds.AIR_TEMP)
        ),
        SensorReadingUi(
            "Light", light.lux(),
            ThresholdEngine.evaluate(light?.toDouble(), SiliGuideThresholds.LIGHT_INTENSITY)
        )
    )

    private fun Double?.percent(): String =
        this?.let { "${trim(it, 0)}%" } ?: DASH

    private fun Double?.celsius(): String =
        this?.let { "${trim(it, 1)}°C" } ?: DASH

    private fun Double?.decimal(digits: Int): String =
        this?.let { trim(it, digits) } ?: DASH

    private fun Int?.ppm(): String =
        this?.let { "$it ppm" } ?: DASH

    private fun Int?.lux(): String = when {
        this == null -> DASH
        this >= 1000 -> "${trim(this / 1000.0, 1)}k lx"
        else -> "$this lx"
    }

    private fun trim(value: Double, digits: Int): String =
        String.format(Locale.US, "%.${digits}f", value)

    private fun formatDate(epochMillis: Long): String =
        SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(epochMillis))

    private fun timeAgo(epochMillis: Long): String {

        val minutes = (System.currentTimeMillis() - epochMillis) / 60_000

        return when {
            minutes < 1 -> "just now"
            minutes < 60 -> "$minutes min ago"
            minutes < 1_440 -> "${minutes / 60} hr ago"
            else -> "${minutes / 1_440} d ago"
        }
    }

    private fun ConnectionMode.toDetailMode(): DetailConnectionMode = when (this) {
        ConnectionMode.AP -> DetailConnectionMode.AP
        ConnectionMode.LAN -> DetailConnectionMode.LAN
        ConnectionMode.BLUETOOTH -> DetailConnectionMode.BLUETOOTH
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    /**
     * Rename from the ⋮ menu.
     *
     * uiState.deviceName is plain state, not a reactive query, so it has to be
     * updated by hand after the write. The dashboard picks the new name up on
     * its own — getAllDevices() is a SQLDelight flow and re-emits on the
     * iot_devices table change.
     */
    fun renameDevice(newName: String) {

        val id = deviceId ?: return

        val trimmed = newName.trim().take(DEVICE_NAME_MAX_LENGTH)

        // The dialog already blocks empty and unchanged names, but this is a
        // public entry point
        if (trimmed.isEmpty() || trimmed == _uiState.value.deviceName) return

        viewModelScope.launch {

            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    updateDeviceNameUseCase(
                        deviceName = trimmed,
                        deviceId = id
                    )
                }
            }

            if (saved.isFailure) {
                Log.e("IoTDeviceDetailVM", "Rename failed", saved.exceptionOrNull())
                return@launch
            }

            _uiState.update { it.copy(deviceName = trimmed) }
        }
    }

    fun refreshDeviceInfo() {
        val id = deviceId ?: return
        viewModelScope.launch {
            loadDeviceInfo(id)
        }
    }

    fun unlinkPlant() {
        val plantIdToUnlink = currentLinkedPlantId ?: return

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    unlinkIoTDeviceToPlantUseCase(plantIdToUnlink)
                }.onSuccess {
                    // Clear the reference and refresh the UI to remove the card
                    currentLinkedPlantId = null
                    refreshDeviceInfo()
                }.onFailure { e ->
                    Log.e("IoTDeviceDetailVM", "Failed to unlink plant", e)
                }
            }
        }
    }


    fun removeDevice(onSuccess: () -> Unit) {
        val id = deviceId ?: return

        viewModelScope.launch {
            stop()  // kill the poll loop before the row disappears out from under it

            val outerResult = withContext(Dispatchers.IO) {
                runCatching { deleteDeviceByIdUseCase(deviceId = id) }
            }

            val useCaseResult = outerResult.getOrElse { throwable ->
                Log.e("IoTDeviceDetailVM", "Failed to remove device", throwable)
                _removeDeviceDialog.value = RemoveDeviceDialogUi(
                    title = "Removal Failed",
                    message = throwable.message ?: "Something went wrong while removing the device."
                )
                return@launch
            }

            useCaseResult.fold(
                onSuccess = { onSuccess() },
                onFailure = { e ->
                    Log.e("IoTDeviceDetailVM", "Failed to remove device", e)
                    _removeDeviceDialog.value = mapRemoveDeviceError(e)
                }
            )
        }
    }

    private fun mapRemoveDeviceError(e: Throwable): RemoveDeviceDialogUi = when {
        e.message == "IoT device not found." -> RemoveDeviceDialogUi(
            title = "Device Not Found",
            message = "This device no longer exists in your account.",
            shouldNavigateOnDismiss = false  // nothing changed, device never touched
        )
        e.message == "Device deletion is pending synchronization." -> RemoveDeviceDialogUi(
            title = "Removal Pending",
            message = "Removal is queued. It will finish once your connection syncs with the cloud.",
            shouldNavigateOnDismiss = true  // markDeviceDeleted already ran
        )
        e.message?.contains("unreachable") == true -> RemoveDeviceDialogUi(
            title = "Device Removed",
            message = "Removed from your account, but the ESP32 could not be reached to reset. It may need a manual reset later.",
            shouldNavigateOnDismiss = true  // cloud + soft-delete already done
        )
        else -> RemoveDeviceDialogUi(
            title = "Reset Failed",
            message = "Device was removed from your account, but resetting the physical ESP32 failed. Try again if it's still reachable.",
            shouldNavigateOnDismiss = true  // soft-delete already done, reset just failed
        )
    }


    // --- MODE CHANGE SHEET LOGIC ---

    fun requestModeChange() {
        val currentStatus = _uiState.value.status

        if (currentStatus == DeviceConnectionStatus.OFFLINE ||
            currentStatus == DeviceConnectionStatus.RECONNECTING) {

            _offlineWarningMessage.value =
                "Cannot change connection mode right now. The device is currently offline or unreachable. Please ensure it is powered on and try again."
            return
        }

        // Device is online! Safe to open the sheet.
        _uiState.update { it.copy(isModeChangeSheetOpen = true) }
    }

    fun dismissModeChangeSheet() {
        _uiState.update { it.copy(isModeChangeSheetOpen = false) }
    }


    private companion object {
        const val POLL_INTERVAL_MS = 3_000L
        const val FETCH_TIMEOUT_MS = 2_500L
        const val DASH = "—"
    }
}