package com.example.siliguide.presentation.features.iot.device_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.model.LastDeviceSensorData
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.usecase.FetchSensorsUseCase
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

@HiltViewModel
class IoTDeviceDetailViewModel @Inject constructor(
    private val ioTRepository: IoTRepository,
    private val fetchSensorsUseCase: FetchSensorsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        IoTDeviceDetailUiState(
            deviceName = "",
            status = DeviceConnectionStatus.RECONNECTING,
            sensors = emptyList(),
            deviceIdentifier = "",
            firmware = "—",
            dateRegistered = "—"
        )
    )
    val uiState: StateFlow<IoTDeviceDetailUiState> = _uiState.asStateFlow()

    private var pollJob: Job? = null
    private var deviceId: String? = null

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

        _uiState.update {
            it.copy(
                deviceName = device.deviceName,
                mode = device.connectionMode.toDetailMode(),
                deviceIdentifier = device.deviceId,
                firmware = "—",
                dateRegistered = formatDate(device.dateAdded)
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
        SensorReadingUi("Soil Moisture", moisture.percent()),
        SensorReadingUi("Soil Temp", soilTemp.celsius()),
        SensorReadingUi("Soil pH", moisture?.let { ph.decimal(1) } ?: ph.decimal(1)),
        SensorReadingUi("Soil N", nitrogen.ppm()),
        SensorReadingUi("Soil P", phosphorus.ppm()),
        SensorReadingUi("Soil K", potassium.ppm()),
        SensorReadingUi("Soil EC", ec?.toString() ?: DASH),
        SensorReadingUi("Air Temp", airTemp.celsius()),
        SensorReadingUi("Light", light.lux())
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

    private companion object {
        const val POLL_INTERVAL_MS = 3_000L
        const val FETCH_TIMEOUT_MS = 2_500L
        const val DASH = "—"
    }
}