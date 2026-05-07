package com.example.siliguide.presentation.features.iot

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.usecase.ConnectToDeviceWifiUseCase
import com.example.siliguide.domain.usecase.FetchSensorsUseCase
import com.example.siliguide.domain.usecase.VerifyDeviceConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class IoTViewModel @Inject constructor(
    private val fetchSensorsUseCase: FetchSensorsUseCase,
    private val connectToDeviceWifiUseCase: ConnectToDeviceWifiUseCase,
    private val verifyDeviceConnectionUseCase: VerifyDeviceConnectionUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    var sensorData by mutableStateOf<SensorResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var connectedDeviceId by mutableStateOf<String?>(null)
        private set

    private val _apConnectionState = MutableStateFlow("IDLE")
    val apConnectionState: StateFlow<String> = _apConnectionState

    // NEW: A reference to hold our polling loop so we can cancel it
    private var pollingJob: Job? = null

    // =========================================================================
    // LIVE DASHBOARD POLLING LOGIC
    // =========================================================================

    /**
     * Starts a continuous loop that fetches data every 3 seconds.
     */
    fun startLiveMonitoring() {
        // First, cancel any existing loop so we don't accidentally run multiples!
        stopLiveMonitoring()

        Log.d("AP_DEBUG", "Starting live sensor monitoring...")
        pollingJob = viewModelScope.launch {
            // isActive ensures the loop stops automatically if the ViewModel is destroyed
            while (isActive) {
                fetchSensors(isBackgroundRefresh = true)
                delay(3000) // Wait 3 seconds before pinging the ESP32 again
            }
        }
    }

    /**
     * Stops the continuous loop. Call this when the user leaves the Dashboard.
     */
    fun stopLiveMonitoring() {
        pollingJob?.cancel()
        pollingJob = null
        Log.d("AP_DEBUG", "Stopped live sensor monitoring.")
    }

    /**
     * Fetches sensor data once.
     * @param isBackgroundRefresh If true, it won't show the initial "Loading..." state.
     */
    fun fetchSensors(isBackgroundRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isBackgroundRefresh) {
                isLoading = true // Only show loading spinner on the very first manual fetch
            }
            error = null

            try {
                sensorData = fetchSensorsUseCase()
            } catch (e: Exception) {
                error = "ESP32 not connected: ${e.message}"
                // Optional: If the connection breaks completely, stop the loop to save battery
                // stopLiveMonitoring()
            }

            if (!isBackgroundRefresh) {
                isLoading = false
            }
        }
    }

    // =========================================================================
    // AP CONNECTION & VERIFICATION LOGIC
    // =========================================================================

    fun connectToDeviceFromQr(raw: String) {
        val result = parseDeviceQr(raw)

        if (result != null) {
            val (ssid, password) = result
            _apConnectionState.value = "CONNECTING"

            connectToDeviceWifiUseCase(
                ssid,
                password,
                onSuccess = {
                    viewModelScope.launch {
                        delay(1500)
                        verifyEsp32ConnectionViaIp()
                    }
                },
                onFailure = {
                    _apConnectionState.value = "FAILED"
                    error = "Failed to connect to the sensor's Wi-Fi network."
                }
            )
        } else {
            _apConnectionState.value = "INVALID_QR"
            error = "Invalid QR Code format."
        }
    }

    private fun parseDeviceQr(raw: String): Pair<String, String>? {
        return try {
            val parts = raw.split(";").associate {
                val (key, value) = it.split("=")
                key to value
            }
            val ssid = parts["SSID"] ?: return null
            val pass = parts["PASS"] ?: ""
            ssid to pass
        } catch (e: Exception) { null }
    }

    private suspend fun verifyEsp32ConnectionViaIp() = withContext(Dispatchers.IO) {
        Log.d("AP_DEBUG", "Pinging 192.168.4.1/info via Ktor...")

        repeat(5) { attempt ->
            val result = verifyDeviceConnectionUseCase()

            result.onSuccess { deviceId ->
                Log.d("AP_DEBUG", "Verification Success! Connected to $deviceId")
                withContext(Dispatchers.Main) {
                    connectedDeviceId = deviceId
                    _apConnectionState.value = "CONNECTED"
                    error = null
                }
                return@withContext
            }

            result.onFailure { exception ->
                Log.d("AP_DEBUG", "Ping attempt ${attempt + 1} failed: ${exception.message}")
            }

            delay(1000)
        }

        withContext(Dispatchers.Main) {
            _apConnectionState.value = "FAILED"
            error = "Could not reach the SILI-GUIDE sensor. Please ensure you are connected to its Hotspot."
        }
    }

    fun forceCheckConnection() {
        Log.d("AP_DEBUG", "Force checking connection (user initiated)")
        _apConnectionState.value = "CONNECTING"

        viewModelScope.launch {
            verifyEsp32ConnectionViaIp()
        }
    }
}