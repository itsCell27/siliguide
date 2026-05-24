package com.example.siliguide.presentation.features.iot

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.usecase.ConnectToDeviceWifiUseCase
import com.example.siliguide.domain.usecase.DisconnectFromDeviceWifiUseCase
import com.example.siliguide.domain.usecase.FetchSensorsUseCase
import com.example.siliguide.domain.usecase.ParseWifiQrUseCase
import com.example.siliguide.domain.usecase.SaveIoTDeviceUseCase
import com.example.siliguide.domain.usecase.VerifyDeviceConnectionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.example.siliguide.domain.repository.AuthRepository

@HiltViewModel
class IoTViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val fetchSensorsUseCase: FetchSensorsUseCase,
    private val connectToDeviceWifiUseCase: ConnectToDeviceWifiUseCase,
    private val disconnectFromDeviceWifiUseCase: DisconnectFromDeviceWifiUseCase,
    private val verifyDeviceConnectionUseCase: VerifyDeviceConnectionUseCase,
    private val parseWifiQrUseCase: ParseWifiQrUseCase,
    private val saveIoTDeviceUseCase: SaveIoTDeviceUseCase,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    var sensorData by mutableStateOf<SensorResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var connectedDeviceId by mutableStateOf<String?>(null)
        private set

    // NEW: Variable to store the pairing token from firmware
    var pairingToken by mutableStateOf<String?>(null)
        private set

    private val _apConnectionState = MutableStateFlow("IDLE")
    val apConnectionState: StateFlow<String> = _apConnectionState

    // SharedFlow to trigger navigation when saving is done
    private val _setupComplete = MutableSharedFlow<Unit>()
    val setupComplete: SharedFlow<Unit> = _setupComplete.asSharedFlow()

    // NEW: A reference to hold our polling loop so we can cancel it
    private var pollingJob: Job? = null

    var deviceName by mutableStateOf("")
        private set

    fun updateDeviceName(name: String) {
        deviceName = name
    }

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

    private fun isWifiEnabled(): Boolean {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
        return wifiManager.isWifiEnabled
    }

    fun connectToDeviceFromQr(raw: String) {

        val credentials = parseWifiQrUseCase(raw)

        if (credentials != null) {

            if (!isWifiEnabled()) {
                _apConnectionState.value = "WIFI_OFF"
                error = "WiFi is turned off. Please enable WiFi to connect to the IoT device."
                return
            }

            // security validation
            if (!credentials.ssid.startsWith("SiliGuide")) {
                _apConnectionState.value = "INVALID_QR"
                error = "This is not a SiliGuide device."
                return
            }

            _apConnectionState.value = "CONNECTING"

            connectToDeviceWifiUseCase(
                credentials.ssid,
                credentials.password,

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

    private suspend fun verifyEsp32ConnectionViaIp() = withContext(Dispatchers.IO) {
        Log.d("AP_DEBUG", "Pinging 192.168.4.1/info via Ktor...")

        repeat(5) { attempt ->
            val result = verifyDeviceConnectionUseCase()

            result.onSuccess { info ->
                Log.d("AP_DEBUG", "Verification Success! Connected to ${info.deviceId}")
                withContext(Dispatchers.Main) {
                    connectedDeviceId = info.deviceId
                    pairingToken = info.token

                    // Set default name automatically if user hasn't typed one
                    if (deviceName.isEmpty()) {
                        deviceName = "SiliGuide Node ${info.deviceId}"
                    }

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

    /**
     * FINAL STEP: Saves the device to the SQLDelight database
     */
    fun finalizeSetup(customName: String) {
        viewModelScope.launch {
            val id = connectedDeviceId ?: return@launch
            val token = pairingToken ?: ""

            // 2. Fetch the current userId
            val userId = authRepository.getUserId() ?: return@launch

            val newDevice = IoTDevice(
                deviceId = id,
                userId = userId, // 3. Pass the userId here!
                deviceName = customName,
                deviceIdentifier = token,
                isActive = true,
                lastKnownIp = "192.168.4.1",
                connectionMode = "AP",
                isSynced = false, // Initial state
                dateAdded = System.currentTimeMillis()
            )

            try {
                saveIoTDeviceUseCase(newDevice)
                _setupComplete.emit(Unit)
            } catch (e: Exception) {
                error = "Database Error: Could not save device."
            }
        }
    }

    fun forceCheckConnection() {
        Log.d("AP_DEBUG", "Force checking connection (user initiated)")
        _apConnectionState.value = "CONNECTING"

        viewModelScope.launch {
            verifyEsp32ConnectionViaIp()
        }
    }

    fun disconnectFromDeviceWifi() {
        Log.d("AP_DEBUG", "Disconnecting from device WiFi using UseCase...")

        // Use the UseCase to properly unbind and unregister the network callback
        disconnectFromDeviceWifiUseCase()

        // Reset UI state
        _apConnectionState.value = "IDLE"
        connectedDeviceId = null
        pairingToken = null
        deviceName = ""
        error = null
    }

}
