package com.example.siliguide.presentation.features.iot

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.ConnectionMode
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.usecase.GetIoTDeviceByIdUseCase
import com.example.siliguide.presentation.features.iot.bluetooth_setup.state.BluetoothConnectionUiState
import kotlinx.coroutines.flow.asStateFlow
import android.bluetooth.BluetoothDevice
import com.example.siliguide.core.permission.PermissionManager

import com.example.siliguide.data.remote.iot.BluetoothApi
import com.example.siliguide.data.remote.iot.BluetoothDiscoveryManager
import com.example.siliguide.data.remote.iot.BluetoothManager
import com.example.siliguide.domain.usecase.ClaimIoTDeviceUseCase
import com.example.siliguide.presentation.features.iot.bluetooth_setup.PairedSiliGuideDevice
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

import android.Manifest
import android.os.Build
import com.example.siliguide.core.permission.DeviceSettingsChecker
import com.example.siliguide.core.permission.PermissionRequest
import com.example.siliguide.core.permission.PermissionStatus
import com.example.siliguide.core.permission.PermissionType
import com.example.siliguide.domain.usecase.CheckDeviceWiFiStatusUseCase
import com.example.siliguide.domain.usecase.ConnectDeviceToWiFUseCase
import com.example.siliguide.domain.usecase.ScanWifiNetworksUseCase
import com.example.siliguide.domain.usecase.UpdateDeviceIpUseCase
import com.example.siliguide.presentation.features.iot.lan_setup.LanApProvisioningUiState
import com.example.siliguide.presentation.features.iot.lan_setup.LanPermissionUiState
import com.example.siliguide.presentation.features.iot.lan_setup.LanPreflightAction
import com.example.siliguide.presentation.features.iot.lan_setup.LanPreflightResult
import com.example.siliguide.presentation.features.iot.lan_setup.LanWifiUiState
import com.example.siliguide.presentation.features.iot.lan_setup.LanProvisioningMethod
import com.example.siliguide.presentation.features.iot.lan_setup.LanSetupMethodUiState
import com.example.siliguide.data.remote.iot.dto.WiFiStatusDto
import com.example.siliguide.domain.usecase.UpdateDeviceConnectionModeUseCase
import com.example.siliguide.presentation.features.iot.lan_setup.LanSaveAction
import com.example.siliguide.presentation.features.iot.lan_setup.LanSaveDialogState
import com.example.siliguide.presentation.features.iot.lan_setup.LanSuccessStage
import com.example.siliguide.presentation.features.iot.lan_setup.LanBluetoothProvisioningUiState

data class ValidationDialogState(
    val title: String,
    val message: String
)

const val DEVICE_NAME_MAX_LENGTH = 12
const val DEFAULT_AP_IP = "192.168.4.1"

@HiltViewModel
class IoTViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val fetchSensorsUseCase: FetchSensorsUseCase,
    private val connectToDeviceWifiUseCase: ConnectToDeviceWifiUseCase,
    private val disconnectFromDeviceWifiUseCase: DisconnectFromDeviceWifiUseCase,
    private val verifyDeviceConnectionUseCase: VerifyDeviceConnectionUseCase,
    private val parseWifiQrUseCase: ParseWifiQrUseCase,
    private val saveIoTDeviceUseCase: SaveIoTDeviceUseCase,
    private val claimIoTDeviceUseCase: ClaimIoTDeviceUseCase,
    private val getIoTDeviceByIdUseCase: GetIoTDeviceByIdUseCase,
    private val bluetoothManager: BluetoothManager,
    private val bluetoothDiscoveryManager: BluetoothDiscoveryManager,
    private val bluetoothApi: BluetoothApi,
    @param:ApplicationContext private val context: Context,

    private val deviceSettingsChecker: DeviceSettingsChecker,
    private val scanWifiNetworksUseCase: ScanWifiNetworksUseCase,
    private val connectDeviceToWiFUseCase: ConnectDeviceToWiFUseCase,
    private val checkDeviceWiFiStatusUseCase: CheckDeviceWiFiStatusUseCase,
    private val updateDeviceIpUseCase: UpdateDeviceIpUseCase,
    private val updateDeviceConnectionModeUseCase: UpdateDeviceConnectionModeUseCase,
    val permissionManager: PermissionManager
) : ViewModel() {

    // Backing property for state updates
    private val _iotUiState = MutableStateFlow(IoTUiState())
    // Public read-only stream for the UI to observe
    val iotUiState: StateFlow<IoTUiState> = _iotUiState.asStateFlow()

    var isClaiming by mutableStateOf(false)
        private set

    var sensorData by mutableStateOf<SensorResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var validationDialog by mutableStateOf<ValidationDialogState?>(null)
        private set

    var connectedDeviceId by mutableStateOf<String?>(null)
        private set

    // NEW: Variable to store the pairing token from firmware
    var pairingToken by mutableStateOf<String?>(null)
        private set

    private val _apConnectionState = MutableStateFlow("IDLE")
    val apConnectionState: StateFlow<String> = _apConnectionState


    // =========================================================================
    // BLUETOOTH SETUP STATE
    // =========================================================================
    private val _bluetoothSetupState =
        MutableStateFlow<BluetoothConnectionUiState> (
            BluetoothConnectionUiState.Waiting
        )

    val bluetoothSetupState = _bluetoothSetupState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PairedSiliGuideDevice>>(emptyList())
    val pairedDevices: StateFlow<List<PairedSiliGuideDevice>> = _pairedDevices.asStateFlow()

    private val _navigateToDeviceSelection = MutableSharedFlow<Unit>()
    val navigateToDeviceSelection: SharedFlow<Unit> = _navigateToDeviceSelection.asSharedFlow()


    // SharedFlow to trigger navigation when saving is done
    private val _setupComplete = MutableSharedFlow<Unit>()
    val setupComplete: SharedFlow<Unit> = _setupComplete.asSharedFlow()

    // NEW: A reference to hold our polling loop so we can cancel it
    private var pollingJob: Job? = null

    // Bluetooth SharedFlow
    private val _bluetoothVerified = MutableSharedFlow<Unit>()
    val bluetoothVerified = _bluetoothVerified.asSharedFlow()

    // ========================================================================
    // BLUETOOTH DEVICE STATE
    // ========================================================================
    private var selectedBluetoothDevice: BluetoothDevice? = null

    var deviceName by mutableStateOf("")
        private set

    fun updateDeviceName(name: String) {
        deviceName = name
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

    private fun showValidationDialog(
        title: String,
        message: String
    ) {
        Log.d(
            "VALIDATION_DIALOG",
            "Showing dialog: $title"
        )

        validationDialog = ValidationDialogState(
            title = title,
            message = message
        )
    }

    fun dismissValidationDialog() {
        validationDialog = null
    }

    private suspend fun verifyEsp32ConnectionViaIp() = withContext(Dispatchers.IO) {
        Log.d("AP_DEBUG", "Pinging 192.168.4.1/info via Ktor...")

        if (!isWifiEnabled()) {
            _apConnectionState.value = "WIFI_OFF"
            error = "WiFi is turned off. Please enable WiFi to connect to the IoT device."
            showValidationDialog(
                title = "WiFi is off",
                message = "Please enable WiFi to connect to the IoT device."
            )
            return@withContext
        }

        repeat(3) { attempt ->
            val result = verifyDeviceConnectionUseCase(host = DEFAULT_AP_IP)

            result.onSuccess { info ->

                Log.d(
                    "AP_DEBUG",
                    "Verification Success! Connected to ${info.deviceId}"
                )

                withContext(Dispatchers.Main) {

                    // ----------------------------------------------------
                    // 1. Verify this is actually a SiliGuide device
                    // ----------------------------------------------------
                    if (info.device != "SiliGuide") {

                        showValidationDialog(
                            title = "Unsupported Device",
                            message = "The connected device is not a SiliGuide IoT device."
                        )

                        _apConnectionState.value = "FAILED"
                        return@withContext
                    }

                    // --- NEW LOGIC: Check if we are Reconfiguring ---
                    val reconfigId = apReconfigureDeviceId
                    if (reconfigId != null) {
                        if (info.deviceId != reconfigId) {
                            showValidationDialog(
                                title = "Wrong Device",
                                message = "You connected to a different SiliGuide device. Please connect to the correct device's hotspot."
                            )
                            _apConnectionState.value = "FAILED"
                            return@withContext
                        }

                        // Device matches! Bypass existing/claimed checks.
                        connectedDeviceId = info.deviceId
                        pairingToken = info.token
                        error = null
                        _apConnectionState.value = "CONNECTED"
                        return@withContext
                    }

                    // ----------------------------------------------------
                    // 2. Already exists in local database?
                    // ----------------------------------------------------
                    val existingDevice =
                        getIoTDeviceByIdUseCase(info.deviceId)

                    if (existingDevice != null) {

                        showValidationDialog(
                            title = "Device Already Registered",
                            message = "This SiliGuide device is already registered in your account."
                        )

                        _apConnectionState.value = "FAILED"
                        return@withContext
                    }

                    // ----------------------------------------------------
                    // 3. Device already claimed?
                    // ----------------------------------------------------
                    if (info.claimed) {

                        showValidationDialog(
                            title = "Device Already Claimed",
                            message = "This SiliGuide device has already been claimed and cannot be paired again unless you reset it manually."
                        )

                        _apConnectionState.value = "FAILED"
                        return@withContext
                    }

                    // ----------------------------------------------------
                    // Passed every validation
                    // ----------------------------------------------------
                    connectedDeviceId = info.deviceId
                    pairingToken = info.token

                    if (deviceName.isEmpty()) {
                        deviceName = "SiliGuide${info.deviceId.takeLast(3)}"
                    }

                    error = null
                    _apConnectionState.value = "CONNECTED"
                }

                return@withContext
            }

            result.onFailure { exception ->
                Log.d("AP_DEBUG", "Ping attempt ${attempt + 1} failed: ${exception.message}")
            }

            delay(500)
        }

        withContext(Dispatchers.Main) {

            _apConnectionState.value = "FAILED"

            error = "Could not reach the SILI-GUIDE sensor. Please ensure you are connected to its Hotspot."

            showValidationDialog(
                title = "Unable to Reach Device",
                message = "The app could not communicate with a SiliGuide device.\n\nPlease verify that your phone is connected to the correct SiliGuide_IoT Wi-Fi network, then tap Retry."
            )
        }
    }

    /**
     * FINAL STEP: Saves the device to the SQLDelight database
     */
    fun finalizeSetup(
        customName: String,
        connectionMode: ConnectionMode,
        ip: String = ""
    ) {
        viewModelScope.launch {

            Log.d("IoTViewModel", "Entered finalizeSetup()")

            Log.d("IoTViewModel", "connectedDeviceId = $connectedDeviceId")
            val id = connectedDeviceId ?: return@launch
            Log.d("IoTViewModel", "connectedDeviceId = $id")

            val token = pairingToken ?: ""
            Log.d("IoTViewModel", "pairingToken = $token")

            // Fetch the current userId
            val userId = authRepository.getUserId() ?: return@launch
            Log.d("IoTViewModel", "userId = $userId")

            // Check if exists
//            Log.d("IoTViewModel", "Checking existing device...")
//            val existingDevice = getIoTDeviceByIdUseCase(id)
//            if (existingDevice != null) {
//                if (existingDevice.userId == userId) {
//                    // Scenario A: Device already belongs to this user
//                    // Maybe just update the name?
//                    Log.d("IoTViewModel", "Device already registered to this user. Updating name.")
//                } else {
//                    // Scenario B: Device belongs to someone else (Security check)
//                    error = "This device is already registered to another account."
//                    return@launch
//                }
//            }
//            Log.d("IoTViewModel", "existingDevice = $existingDevice")

            val newDevice = IoTDevice(
                deviceId = id,
                userId = userId,
                deviceName = customName.trim().take(DEVICE_NAME_MAX_LENGTH),
                pairingToken = token,
                isActive = true,
                lastKnownIp = ip,
                connectionMode = connectionMode,
                dateAdded = System.currentTimeMillis()
            )

            try {
                isClaiming = true

                Log.d(
                    "IoTViewModel",
                    "Claiming ESP32..."
                )

                claimIoTDeviceUseCase(
                    host = ip,
                    token = token
                )

                Log.d(
                    "IoTViewModel",
                    "ESP32 successfully claimed."
                )

                saveIoTDeviceUseCase(newDevice)

                _setupComplete.emit(Unit)

            } catch (e: Exception) {

                Log.e(
                    "IoTViewModel",
                    "Claim request failed. Verifying actual device state...",
                    e
                )

                var recovered = false

                val verificationResult =
                    verifyDeviceConnectionUseCase(ip)

                verificationResult.onSuccess { info ->

                    // Same device and now marked as claimed?
                    if (
                        info.deviceId == id &&
                        info.claimed
                    ) {

                        recovered = true

                        Log.d(
                            "IoTViewModel",
                            "Claim appears to have succeeded despite missing HTTP response."
                        )

                        saveIoTDeviceUseCase(newDevice)

                        _setupComplete.emit(Unit)

                        return@onSuccess
                    }
                }

                // Only show the dialog if recovery failed
                if (!recovered) {
                    showValidationDialog(
                        title = "Unable to Claim Device",
                        message = "The SiliGuide device did not acknowledge ownership. Please make sure it is powered on and connected, then try again."
                    )
                }
            } finally {
                isClaiming = false
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
        validationDialog = null
    }

    // =========================================================================
    // BLUETOOTH SETUP STATE HELPERS
    // =========================================================================
    private fun resetBluetoothDeviceState() {
        selectedBluetoothDevice = null
        connectedDeviceId = null
        pairingToken = null
    }

    fun startBluetoothSetup() {

        resetBluetoothDeviceState()

        bluetoothManager.disconnect()

        _bluetoothSetupState.value =
            BluetoothConnectionUiState.Waiting

        error = null
    }

    fun connectToSelectedBluetoothDevice(selected: BluetoothDevice) {
        viewModelScope.launch {

            setBluetoothVerifying()
            _iotUiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    loadingMessage = "Connecting..."
                )
            }

            withContext(Dispatchers.IO) {
                try {

                    bluetoothManager.connect(selected)

                    withContext(Dispatchers.Main) {
                        _iotUiState.update { currentState ->
                            currentState.copy(
                                loadingMessage = "Verifying..."
                            )
                        }
                    }

                    val info = bluetoothApi.getDeviceInfo()

                    withContext(Dispatchers.Main) {

                        // ----------------------------------------------------
                        // 1. Verify this is actually a SiliGuide device
                        // ----------------------------------------------------
                        if (info.device != "SiliGuide") {
                            showValidationDialog(
                                title = "Unsupported Device",
                                message = "The connected device is not a SiliGuide IoT device."
                            )
                            setBluetoothUnsupported("Device is not recognized as SiliGuide.")
                            bluetoothManager.disconnect()
                            return@withContext
                        }

                        // ----------------------------------------------------
                        // 2. Already exists in local database?
                        // ----------------------------------------------------
                        val existingDevice = getIoTDeviceByIdUseCase(info.deviceId)

                        if (existingDevice != null) {
                            showValidationDialog(
                                title = "Device Already Registered",
                                message = "This SiliGuide device is already registered in your account."
                            )
                            setBluetoothUnsupported("Device already exists in your records.")
                            bluetoothManager.disconnect()
                            return@withContext
                        }

                        // ----------------------------------------------------
                        // 3. Device already claimed?
                        // ----------------------------------------------------
                        if (info.claimed) {
                            showValidationDialog(
                                title = "Device Already Claimed",
                                message = "This SiliGuide device has already been claimed and cannot be paired again unless you reset it manually."
                            )
                            setBluetoothUnsupported("Device is permanently locked to another user.")
                            bluetoothManager.disconnect()
                            return@withContext
                        }

                        // ----------------------------------------------------
                        // Passed every validation
                        // ----------------------------------------------------
                        connectedDeviceId = info.deviceId
                        pairingToken = info.token ?: ""

                        if (deviceName.isEmpty()) {
                            deviceName = "SiliGuide${info.deviceId.takeLast(3)}"
                        }

                        error = null
                        _bluetoothVerified.emit(Unit)
                    }

                } catch (e: SecurityException) {
                    Log.e("BluetoothSetup", "Missing Bluetooth permission", e)
                    withContext(Dispatchers.Main) {
                        setBluetoothUnsupported("Missing Bluetooth permissions. Please grant them in settings.")
                    }
                } catch (e: Exception) {
                    bluetoothManager.disconnect()
                    Log.e("BluetoothSetup", "Bluetooth verification failed", e)
                    withContext(Dispatchers.Main) {
                        showValidationDialog(
                            title = "Connection Failed",
                            message = "Could not reach the SILI-GUIDE sensor over Bluetooth. Ensure it is powered on, in range, and paired correctly."
                        )
                        resetBluetoothSetup()
                    }
                } finally {
                    _iotUiState.update { currentState ->
                        currentState.copy(
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun verifyPairedBluetoothDevices() {
        viewModelScope.launch {

            setBluetoothVerifying()
            _iotUiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    loadingMessage = "Checking Bluetooth..."
                )
            }

            withContext(Dispatchers.IO) {
                try {

                    if (!bluetoothManager.isBluetoothOn()) {
                        withContext(Dispatchers.Main) {
                            showValidationDialog(
                                title = "Bluetooth is Off",
                                message = "Please turn on Bluetooth in your phone's settings to scan for SiliGuide devices."
                            )
                            resetBluetoothSetup()
                            _iotUiState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    isBluetoothOn = false
                                )
                            }
                        }
                        return@withContext
                    }

                    _iotUiState.update { it.copy(
                        loadingMessage = "Checking paired devices...",
                        isBluetoothOn = true
                        )
                    }

                    val startTime = System.currentTimeMillis()

                    val pairedDevices = bluetoothDiscoveryManager.getPairedSiliGuideDevices()

                    if (pairedDevices.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            showValidationDialog(
                                title = "No Device Found",
                                message = "No paired SiliGuide devices were found. Please tap 'Open Settings' to pair the device first."
                            )
                            resetBluetoothSetup()
                            _iotUiState.update { currentState ->
                                currentState.copy(
                                    isLoading = false,
                                    hasPairedDevices = false
                                )
                            }
                        }
                        return@withContext
                    }

                    _iotUiState.update { it.copy(
                        loadingMessage = "Verifying active devices...",
                        hasPairedDevices = true
                        )
                    }

                    val mapped = pairedDevices.mapNotNull { device ->

                        val isOnline = try {
                            withTimeoutOrNull(500) {
                                bluetoothManager.connect(device)
                                bluetoothApi.ping()
                            } ?: false
                        } catch (e: Exception) {
                            Log.d("BluetoothSetup", "Ping failed for ${device.name}: ${e.message}")
                            false
                        } finally {
                            bluetoothManager.disconnect()
                        }

                        if (isOnline) {
                            PairedSiliGuideDevice(
                                name = device.name ?: "Unknown Device",
                                deviceId = device.address,
                                device = device
                            )
                        } else {
                            null
                        }
                    }

                    val duration = System.currentTimeMillis() - startTime
                    Log.d("PerformanceLog", "Getting paired Bluetooth devices took: ${duration}ms")

                    withContext(Dispatchers.Main) {
                        _pairedDevices.value = mapped
                        _navigateToDeviceSelection.emit(Unit)
                    }

                } catch (e: SecurityException) {
                    Log.e("BluetoothSetup", "Missing Bluetooth permission", e)
                    withContext(Dispatchers.Main) {
                        setBluetoothUnsupported("Missing Bluetooth permissions. Please grant them in settings.")
                    }
                } catch (e: Exception) {
                    bluetoothManager.disconnect()
                    Log.e("BluetoothSetup", "Bluetooth verification failed", e)
                    withContext(Dispatchers.Main) {
                        showValidationDialog(
                            title = "Connection Failed",
                            message = "Could not reach the SILI-GUIDE sensor over Bluetooth. Ensure it is powered on, in range, and paired correctly."
                        )
                        resetBluetoothSetup()
                    }
                } finally {
                    _iotUiState.update { currentState ->
                        currentState.copy(
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    fun finalizeBluetoothSetup(customName: String) {
        viewModelScope.launch {
            val id = connectedDeviceId ?: return@launch
            val token = pairingToken ?: ""
            val userId = authRepository.getUserId() ?: return@launch

            try {
                isClaiming = true

                // Lock Execution — claim first, over Bluetooth.
                val claimResult = withContext(Dispatchers.IO) {
                    bluetoothApi.claimDevice(token)
                }

                Log.d("IoTViewModel", "Claim status=${claimResult.status}, error=${claimResult.error}")

                if (!claimResult.error.isNullOrBlank()) {
                    showValidationDialog(
                        title = "Claim Rejected",
                        message = claimResult.error ?: "The device rejected the claim request."
                    )
                    return@launch   // no save happen, device stay unsaved
                }

                // Local Commit — only reach here if claim succeed.
                val newDevice = IoTDevice(
                    deviceId = id,
                    userId = userId,
                    deviceName = customName.trim().take(DEVICE_NAME_MAX_LENGTH),
                    pairingToken = token,
                    isActive = true,
                    lastKnownIp = "",
                    connectionMode = ConnectionMode.BLUETOOTH,
                    dateAdded = System.currentTimeMillis()
                )

                saveIoTDeviceUseCase(newDevice)
                bluetoothManager.disconnect()
                resetBluetoothDeviceState()
                _setupComplete.emit(Unit)

            } catch (e: Exception) {
                Log.e("IoTViewModel", "Claim or save failed", e)
                showValidationDialog(
                    title = "Save Failed",
                    message = "Could not claim or save this device. Please try again."
                )
                bluetoothManager.disconnect()
            } finally {
                isClaiming = false
            }
        }
    }

    fun selectPairedDevice(paired: PairedSiliGuideDevice) {
        connectToSelectedBluetoothDevice(paired.device)
    }

    fun setBluetoothSearching() {
        _bluetoothSetupState.value = BluetoothConnectionUiState.Searching
    }

    fun setBluetoothVerifying() {
        _bluetoothSetupState.value = BluetoothConnectionUiState.Verifying
    }

    fun setBluetoothUnsupported(message: String) {
        _bluetoothSetupState.value =
            BluetoothConnectionUiState.Unsupported(message)
    }

    fun resetBluetoothSetup() {
        _bluetoothSetupState.value = BluetoothConnectionUiState.Waiting
        error = null
    }


    // =========================================================================
    // LAN SETUP FUNCTIONS
    // =========================================================================

    // ---- Step 1 of 4: Choose Setup Method ---------------------------------

    private val _lanSetupMethodState = MutableStateFlow(LanSetupMethodUiState())

    val lanSetupMethodState: StateFlow<LanSetupMethodUiState> =
        _lanSetupMethodState.asStateFlow()

    /** Emitted when the user commits to a provisioning transport. */
    private val _lanMethodChosen =
        MutableSharedFlow<LanProvisioningMethod>(extraBufferCapacity = 1)

    val lanMethodChosen: SharedFlow<LanProvisioningMethod> =
        _lanMethodChosen.asSharedFlow()

    /** Which transport the wizard is on — read by Step 3 to pick Step 4. */
    val lanProvisioningMethod: LanProvisioningMethod
        get() = _lanSetupMethodState.value.selectedMethod

    fun selectLanProvisioningMethod(method: LanProvisioningMethod) {
        _lanSetupMethodState.value =
            _lanSetupMethodState.value.copy(selectedMethod = method)
    }

    /**
     * Step 1 → Step 2.
     *
     * No gate here — choosing a method needs no permissions. Both transports
     * require the same ones, so the preflight runs on Step 2 either way.
     */
    fun continueFromLanSetupMethod() {
        viewModelScope.launch {
            _lanMethodChosen.emit(_lanSetupMethodState.value.selectedMethod)
        }
    }

    // ---- Step 2 of 4: Location Permission ---------------------------------

    private val _lanPermissionState =
        MutableStateFlow<LanPermissionUiState>(LanPermissionUiState.Requesting)

    val lanPermissionState: StateFlow<LanPermissionUiState> =
        _lanPermissionState.asStateFlow()

    /**
     * Emitted when permission, Location service and Wi-Fi are all ready.
     *
     * extraBufferCapacity keeps the event alive across the brief window where
     * the screen is resuming and the collector has not re-attached yet.
     */
    private val _lanPreflightPassed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lanPreflightPassed: SharedFlow<Unit> = _lanPreflightPassed.asSharedFlow()

    /** Whatever is currently blocking the wizard, or null when nothing is. */
    var lanBlocker by mutableStateOf<LanPreflightResult.Blocked?>(null)
        private set

    fun dismissLanBlocker() {
        lanBlocker = null
    }

    /**
     * Runtime permissions needed to scan for Wi-Fi networks.
     *
     * Android 13+ adds NEARBY_WIFI_DEVICES. It is a SEPARATE grant from
     * ACCESS_FINE_LOCATION with its own system prompt and its own App Settings
     * toggle, so both have to be tracked individually.
     */
    val lanPermissionRequests: List<PermissionRequest>
        get() = buildList {

            add(
                PermissionRequest(
                    permission = Manifest.permission.ACCESS_FINE_LOCATION,
                    type = PermissionType.LOCATION
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(
                    PermissionRequest(
                        permission = Manifest.permission.NEARBY_WIFI_DEVICES,
                        type = PermissionType.NEARBY_WIFI
                    )
                )
            }
        }

    /** Same list, shaped for RequestMultiplePermissions. */
    val lanPermissionArray: Array<String>
        get() = lanPermissionRequests
            .map { it.permission }
            .toTypedArray()

    /** Status of every LAN permission, keyed by permission string. */
    private suspend fun lanPermissionStatuses(
        context: Context
    ): Map<String, PermissionStatus> =
        lanPermissionRequests.associate { request ->
            request.permission to permissionManager.getPermissionStatus(
                context = context,
                permission = request.permission,
                type = request.type
            )
        }

    /**
     * Turns per-permission statuses into the Step 2 artwork state.
     *
     * Granted when nothing is missing. Denied only when something is
     * permanently denied — if a missing permission can still be prompted, the
     * screen stays in Requesting so the user is not sent to App Settings for
     * no reason.
     */
    private fun toLanPermissionUiState(
        statuses: Map<String, PermissionStatus>
    ): LanPermissionUiState {

        val missing = statuses.filterValues { it !is PermissionStatus.Granted }

        if (missing.isEmpty()) {
            return LanPermissionUiState.Granted
        }

        val anyPermanentlyDenied =
            missing.values.any { it is PermissionStatus.PermanentlyDenied }

        if (!anyPermanentlyDenied) {
            return LanPermissionUiState.Requesting
        }

        return LanPermissionUiState.Denied(
            missingLocation =
                missing.containsKey(Manifest.permission.ACCESS_FINE_LOCATION),

            missingNearbyDevices =
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        missing.containsKey(Manifest.permission.NEARBY_WIFI_DEVICES),

            canRetryPrompt = missing.values.any {
                it is PermissionStatus.RequestPermission ||
                        it is PermissionStatus.ShowRationale
            }
        )
    }

    /** Human-readable list of what is still missing, for dialog copy. */
    private fun missingLanPermissionLabels(
        statuses: Map<String, PermissionStatus>
    ): String {

        val labels = buildList {

            if (statuses[Manifest.permission.ACCESS_FINE_LOCATION]
                        !is PermissionStatus.Granted
            ) {
                add("Location")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                statuses[Manifest.permission.NEARBY_WIFI_DEVICES]
                        !is PermissionStatus.Granted
            ) {
                add("Nearby devices")
            }
        }

        return labels.joinToString(" and ")
    }

    /**
     * Single gate for Step 2 → Step 3.
     *
     * Checked in the order the user can act on them: permissions first, then
     * the Location service, then the Wi-Fi radio.
     *
     * [context] must be the Activity context, since PermissionManager needs it
     * to tell a rationale apart from a permanent denial.
     */
    private suspend fun evaluateLanPreflight(
        context: Context
    ): LanPreflightResult {

        // ---- 1. Location / nearby-devices permissions ---------------------
        val statuses = lanPermissionStatuses(context)

        _lanPermissionState.value = toLanPermissionUiState(statuses)

        val missing = statuses.filterValues { it !is PermissionStatus.Granted }

        if (missing.isNotEmpty()) {

            val labels = missingLanPermissionLabels(statuses)

            val canPrompt = missing.values.any {
                it is PermissionStatus.RequestPermission ||
                        it is PermissionStatus.ShowRationale
            }

            return if (canPrompt) {
                LanPreflightResult.Blocked(
                    title = "$labels Access Required",
                    message = "SiliGuide needs $labels access to scan for nearby WiFi " +
                            "networks and find your IoT device. It is never stored or shared.",
                    action = LanPreflightAction.REQUEST_PERMISSION
                )
            } else {
                LanPreflightResult.Blocked(
                    title = "$labels Access Required",
                    message = "Please open App Settings, go to Permissions, and allow " +
                            "$labels so SiliGuide can scan for WiFi networks.",
                    action = LanPreflightAction.OPEN_APP_SETTINGS
                )
            }
        }

        // ---- 2. Location service -----------------------------------------
        if (!deviceSettingsChecker.isLocationServiceEnabled()) {

            return LanPreflightResult.Blocked(
                title = "Location is Off",
                message = "Please turn on Location in your phone's settings. Android will " +
                        "not return WiFi scan results while Location is off.",
                action = LanPreflightAction.OPEN_LOCATION_SETTINGS
            )
        }

        // ---- 3. Wi-Fi radio ----------------------------------------------
        if (!deviceSettingsChecker.isWifiEnabled()) {

            return LanPreflightResult.Blocked(
                title = "WiFi is Off",
                message = "Please turn on WiFi so SiliGuide can scan for the network your " +
                        "IoT device will join.",
                action = LanPreflightAction.OPEN_WIFI_SETTINGS
            )
        }

        return LanPreflightResult.Ready
    }

    /**
     * Runs the gate and records the outcome.
     *
     * Backs both the Grant Permission and the Continue button — the gate is
     * the same either way, only the artwork differs.
     *
     * On success emits [lanPreflightPassed]. On failure writes [lanBlocker] —
     * every blocker, including the permission one, so the UI always has the
     * matching action to take.
     */
    fun continueFromLanPermission(context: Context) {
        viewModelScope.launch {

            when (val result = evaluateLanPreflight(context)) {

                LanPreflightResult.Ready -> {
                    error = null
                    lanBlocker = null
                    _lanPreflightPassed.emit(Unit)
                }

                is LanPreflightResult.Blocked -> {
                    error = result.message
                    lanBlocker = result
                }
            }
        }
    }

    /** Persist that the OS prompt was shown, before launching it. */
    fun markLanPermissionRequested() {
        viewModelScope.launch {
            permissionManager.markRequested(lanPermissionRequests)
        }
    }

    /** Result of the OS permission prompt. */
    fun onLanPermissionResult(
        context: Context,
        granted: Boolean
    ) {
        lanBlocker = null

        if (granted) {
            error = null
            // Re-run the gate — Location service and Wi-Fi still unchecked
            continueFromLanPermission(context)
        } else {
            error = "Permission was denied."
            refreshLanPermissionState(context)
        }
    }

    /**
     * Refresh only the Requesting / Granted / Denied artwork on ON_RESUME.
     *
     * Deliberately does NOT emit navigation or raise a blocker — otherwise it
     * races the settings launcher callback and can navigate twice.
     */
    fun refreshLanPermissionState(context: Context) {
        viewModelScope.launch {
            _lanPermissionState.value =
                toLanPermissionUiState(lanPermissionStatuses(context))
        }
    }

    /** Rationale shown by the "Why is this needed?" link on Step 2. */
    fun showLanPermissionRationale() {
        showValidationDialog(
            title = "Why These Permissions",
            message = "Android ties WiFi scanning to location access, and on Android 13 and " +
                    "newer it also requires Nearby devices access. Without both, the system " +
                    "returns an empty network list, so SiliGuide cannot show the WiFi network " +
                    "your IoT device should join. SiliGuide never stores or shares your location."
        )
    }

    // ---- Step 3 of 4: Wi-Fi scan and credentials --------------------------

    private val _lanWifiState =
        MutableStateFlow<LanWifiUiState>(LanWifiUiState.Scanning)

    val lanWifiState: StateFlow<LanWifiUiState> = _lanWifiState.asStateFlow()

    /** Emitted when a network is chosen and its password entered. */
    private val _lanCredentialsReady = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lanCredentialsReady: SharedFlow<Unit> = _lanCredentialsReady.asSharedFlow()

    /** Home Wi-Fi credentials handed to the ESP32 by whichever path runs. */
    var lanSelectedSsid: String? = null
        private set

    var lanSelectedPassword: String = ""
        private set

    /**
     * Re-checks the gate, then scans.
     *
     * The user can revoke permission or flip a radio off while sitting on this
     * screen, so the same preflight that guarded Step 2 runs again here rather
     * than trusting that it passed a moment ago.
     */
    fun startLanWifiScan(context: Context) {
        viewModelScope.launch {

            when (val result = evaluateLanPreflight(context)) {

                LanPreflightResult.Ready -> {
                    lanBlocker = null
                    error = null

                    _lanWifiState.value = LanWifiUiState.Scanning

                    val networks = runCatching { scanWifiNetworksUseCase() }
                        .getOrElse { emptyList() }

                    // Filter OUT SiliGuide devices and blank hidden networks
                    val filteredNetworks = networks.filter { network ->
                        val ssid = network.ssid

                        // 1. Must not be null or completely empty (hides ghost networks)
                        // 2. Must NOT contain the word "SiliGuide"
                        !ssid.isNullOrBlank() && !ssid.contains("SiliGuide", ignoreCase = true)
                    }

                    _lanWifiState.value =
                        if (filteredNetworks.isEmpty()) {
                            LanWifiUiState.Empty
                        } else {
                            LanWifiUiState.Ready(
                                networks = filteredNetworks,
                                // Keep the choice across a refresh
                                selectedSsid = lanSelectedSsid
                                    ?.takeIf { ssid -> networks.any { it.ssid == ssid } },
                                password = lanSelectedPassword,
                                passwordVisible = currentPasswordVisible()
                            )
                        }
                }

                is LanPreflightResult.Blocked -> {
                    error = result.message
                    lanBlocker = result

                    _lanWifiState.value =
                        LanWifiUiState.Unavailable(scanUnavailableMessage(result.action))
                }
            }
        }
    }

    fun finishDeviceWiFiSetupDialog() {
        showValidationDialog(
            title = "Finish WiFi Setup",
            message = "Your device is saved but not on a network yet. " +
                    "Choose a WiFi network and enter its password to finish."
        )
    }

    private fun currentPasswordVisible(): Boolean =
        (_lanWifiState.value as? LanWifiUiState.Ready)?.passwordVisible == true

    private fun scanUnavailableMessage(action: LanPreflightAction): String = when (action) {

        LanPreflightAction.OPEN_WIFI_SETTINGS ->
            "WiFi is turned off. Turn it on to scan for nearby networks."

        LanPreflightAction.OPEN_LOCATION_SETTINGS ->
            "Location is turned off. Turn it on to scan for nearby networks."

        LanPreflightAction.REQUEST_PERMISSION,
        LanPreflightAction.OPEN_APP_SETTINGS ->
            "Permission is required before SiliGuide can scan for nearby networks."
    }

    fun selectLanWifiNetwork(ssid: String) {

        val ready = _lanWifiState.value as? LanWifiUiState.Ready ?: return

        // Re-tapping the same row keeps whatever was typed
        val password = if (ready.selectedSsid == ssid) ready.password else ""

        lanSelectedSsid = ssid
        lanSelectedPassword = password

        _lanWifiState.value = ready.copy(
            selectedSsid = ssid,
            password = password,
            passwordVisible = false
        )
    }

    fun updateLanWifiPassword(password: String) {

        val ready = _lanWifiState.value as? LanWifiUiState.Ready ?: return

        lanSelectedPassword = password

        _lanWifiState.value = ready.copy(password = password)
    }

    fun toggleLanWifiPasswordVisibility() {

        val ready = _lanWifiState.value as? LanWifiUiState.Ready ?: return

        _lanWifiState.value = ready.copy(passwordVisible = !ready.passwordVisible)
    }

    /** Step 3 → Step 4, or straight back to /configure when re-provisioning. */
    fun continueFromLanWifi() {

        // Reusing Step 3 to fix a saved device — skip the rest of the wizard
        if (lanReconfigureDeviceId != null) {
            sendWifiToSavedDevice()
            return
        }

        val ready = _lanWifiState.value as? LanWifiUiState.Ready ?: return

        val selected = ready.networks.firstOrNull { it.ssid == ready.selectedSsid } ?: return

        if (!selected.isOpen && ready.password.isEmpty()) {
            showValidationDialog(
                title = "Password Required",
                message = "Enter the password for ${selected.ssid} to continue."
            )
            return
        }

        lanSelectedSsid = selected.ssid
        lanSelectedPassword = ready.password

        viewModelScope.launch { _lanCredentialsReady.emit(Unit) }
    }

    /**
     * Clears the network choice only.
     *
     * The provisioning method came from Step 1, so a rescan must not wipe it.
     */
    fun resetLanWifiSelection() {
        _lanWifiState.value = LanWifiUiState.Scanning
        lanSelectedSsid = null
        lanSelectedPassword = ""
    }

    // ---- Step 4a of 4: Device Hotspot (AP) provisioning -------------------

    /** SSID prefix the ESP32 broadcasts while unclaimed. */
    private val lanApDeviceSsid = "SiliGuide_IoT"

    private val _lanApProvisioningState = MutableStateFlow<LanApProvisioningUiState>(
        LanApProvisioningUiState.Waiting(lanApDeviceSsid)
    )

    val lanApProvisioningState: StateFlow<LanApProvisioningUiState> =
        _lanApProvisioningState.asStateFlow()

    /** Emitted once an unclaimed SiliGuide device answers on 192.168.4.1. */
    private val _lanApVerified = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lanApVerified: SharedFlow<Unit> = _lanApVerified.asSharedFlow()

    private var lanApWatchJob: Job? = null

    /**
     * Starts watching [apConnectionState] and mirrors it onto the wizard state.
     *
     * verifyEsp32ConnectionViaIp() already owns the real work — the ping, the
     * SiliGuide check, the already-registered check and the already-claimed
     * check — and raises its own validationDialog on each failure. This only
     * translates its status string into screen states and fires navigation.
     */
    fun startLanApProvisioning() {

        // Clear any verdict from a previous visit. apConnectionState is a
        // StateFlow, so a stale "CONNECTED" would replay the instant the
        // collector attaches and re-fire lanApVerified.
        _apConnectionState.value = "IDLE"

        _lanApProvisioningState.value =
            LanApProvisioningUiState.Waiting(lanApDeviceSsid)

        lanApWatchJob?.cancel()

        lanApWatchJob = viewModelScope.launch {
            apConnectionState.collect { status ->

                when (status) {

                    "CONNECTING" ->
                        _lanApProvisioningState.value =
                            LanApProvisioningUiState.SendingCredentials

                    "CONNECTED" -> {
                        _lanApProvisioningState.value =
                            LanApProvisioningUiState.SendingCredentials

                        _lanApVerified.emit(Unit)
                    }

                    "FAILED", "WIFI_OFF" ->
                        _lanApProvisioningState.value =
                            LanApProvisioningUiState.Error(lanApDeviceSsid)

                    else ->
                        _lanApProvisioningState.value =
                            LanApProvisioningUiState.Waiting(lanApDeviceSsid)
                }
            }
        }
    }

    /** Forget the last hotspot verdict so Step 4 re-verifies on return. */
    fun resetLanApVerification() {
        _apConnectionState.value = "IDLE"
        _lanApProvisioningState.value =
            LanApProvisioningUiState.Waiting(lanApDeviceSsid)
    }

    /** Re-pings the hotspot after the user fixes whatever was wrong. */
    fun retryLanApProvisioning() {
        dismissValidationDialog()
        forceCheckConnection()
    }

    fun stopLanApProvisioning() {
        lanApWatchJob?.cancel()
        lanApWatchJob = null
    }

    // ---- Success screen ---------------------------------------------------

    // ---- Success: claim, save, provision ----------------------------------

    /** Blocking message raised while claiming, saving or provisioning. */
    var lanSaveDialog by mutableStateOf<LanSaveDialogState?>(null)
        private set

    fun dismissLanSaveDialog() {
        lanSaveDialog = null
    }

    var isFinalizingLan by mutableStateOf(false)
        private set

    /** Which half of the Success screen is showing. */
    var lanSuccessStage by mutableStateOf(LanSuccessStage.NAMING)
        private set

    /** Network the device joined — the one the phone has to rejoin. */
    var lanDeviceIp by mutableStateOf("")
        private set

    /** A verification attempt already came back empty. */
    var lanSwitchFailed by mutableStateOf(false)
        private set

    /** Emitted when the device is claimed, saved and on the LAN. */
    private val _lanSetupFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lanSetupFinished: SharedFlow<Unit> = _lanSetupFinished.asSharedFlow()

    /**
     * Emitted when a device is provisioned and the phone hand-off starts.
     *
     * Separate from lanSetupFinished: that one means fully done, this one means
     * "go show the SWITCH_NETWORK screen". The re-provisioning path fires from
     * Step 3, which has to pop back to Success before the hand-off UI can show.
     */
    private val _lanHandoffReady = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lanHandoffReady: SharedFlow<Unit> = _lanHandoffReady.asSharedFlow()

    /** Emitted with a deviceId when the user has to pick a different network. */
    private val _lanReconfigureRequested = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val lanReconfigureRequested: SharedFlow<String> = _lanReconfigureRequested.asSharedFlow()

    /**
     * Set while Step 3 is being reused to re-provision an already saved device.
     *
     * When non-null, Continue on the Wi-Fi screen pushes credentials straight
     * to the device instead of walking forward through the wizard.
     */
    var lanReconfigureDeviceId by mutableStateOf<String?>(null)
        private set

    fun startLanWifiReconfigure(deviceId: String) {
        lanReconfigureDeviceId = deviceId
    }

    fun clearLanWifiReconfigure() {
        lanReconfigureDeviceId = null
    }

    /** Human label for whichever path delivered the device. */
    val lanProvisioningPathLabel: String
        get() = when (lanProvisioningMethod) {
            LanProvisioningMethod.ACCESS_POINT -> "AP Hotspot"
            LanProvisioningMethod.BLUETOOTH -> "Bluetooth"
        }

    /**
     * Save Device on the Success screen, AP path.
     *
     * Order matters and is dictated by the firmware:
     *
     *  1. POST /claim  — only answers while the device is unclaimed.
     *  2. Save locally — the row has to exist before we can update its IP.
     *  3. POST /configure — 403s unless the device is already claimed, which
     *     is why it cannot run before step 1.
     *  4. GET /wifi-status — the ESP32 runs WIFI_AP_STA, so its hotspot stays
     *     up while it joins the home network and we can keep polling it.
     *  5. Update last_known_ip, then move the phone over to the same network.
     */
    fun finalizeLanAPMode() {

        _iotUiState.update { currentState ->
            currentState.copy(
                isLoading = true,
                loadingMessage = "Saving device..."
            )
        }

        val deviceId = connectedDeviceId
        val token = pairingToken
        val ssid = lanSelectedSsid
        val name = deviceName.trim()

        if (deviceId == null || token.isNullOrBlank()) {
            _iotUiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    loadingMessage = "Loading..."
                )
            }
            lanSaveDialog = LanSaveDialogState(
                title = "Device Not Connected",
                message = "The device is no longer reachable. Go back and reconnect to its hotspot."
            )
            return
        }

        if (ssid.isNullOrBlank()) {
            _iotUiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    loadingMessage = "Loading..."
                )
            }
            lanSaveDialog = LanSaveDialogState(
                title = "No Network Selected",
                message = "Go back and choose the WiFi network this device should join.",
                action = LanSaveAction.RECONFIGURE_WIFI,
                confirmText = "Choose WiFi"
            )
            return
        }

        if (name.isBlank()) {
            _iotUiState.update { currentState ->
                currentState.copy(
                    isLoading = false,
                    loadingMessage = "Loading..."
                )
            }
            lanSaveDialog = LanSaveDialogState(
                title = "Name Required",
                message = "Give this device a name before saving it."
            )
            return
        }

        val password = lanSelectedPassword

        viewModelScope.launch {

            isFinalizingLan = true

            try {
                val userId = authRepository.getUserId()

                if (userId == null) {
                    lanSaveDialog = LanSaveDialogState(
                        title = "Not Signed In",
                        message = "Sign in again before adding a device."
                    )
                    return@launch
                }

                // ---- 1. Claim -------------------------------------------
                val claimed = runCatching {
                    claimIoTDeviceUseCase(host = DEFAULT_AP_IP, token = token)
                }

                if (claimed.isFailure) {
                    Log.e("IoTViewModel", "Claim failed", claimed.exceptionOrNull())

                    lanSaveDialog = LanSaveDialogState(
                        title = "Couldn't Claim Device",
                        message = "The device rejected the pairing request. Make sure your " +
                                "phone is still connected to its hotspot, then try again."
                    )
                    return@launch
                }

                // ---- 2. Save locally ------------------------------------
                val saved = runCatching {
                    saveIoTDeviceUseCase(
                        IoTDevice(
                            deviceId = deviceId,
                            userId = userId,
                            deviceName = name.take(DEVICE_NAME_MAX_LENGTH),
                            pairingToken = token,
                            isActive = true,
                            // Filled in once the device reports its LAN address
                            lastKnownIp = "",
                            // Set to AP for now since device is not yet connected to LAN
                            connectionMode = ConnectionMode.AP,
                            dateAdded = System.currentTimeMillis()
                        )
                    )
                }

                if (saved.isFailure) {
                    Log.e("IoTViewModel", "Save failed", saved.exceptionOrNull())

                    lanSaveDialog = LanSaveDialogState(
                        title = "Couldn't Save Device",
                        message = "The device was claimed but could not be saved to this " +
                                "device. Please try again."
                    )
                    return@launch
                }

                // ---- 3, 4, 5. Provision --------------------------------
                provisionSavedDevice(
                    deviceId = deviceId,
                    token = token,
                    ssid = ssid,
                    password = password
                )

            } finally {
                _iotUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        loadingMessage = "Loading..."
                    )
                }
                isFinalizingLan = false
            }
        }
    }

    /**
     * Continue on Step 3 when the wizard is re-provisioning a saved device.
     *
     * The device is already claimed and in the database, so only /configure
     * and the status poll are repeated.
     */
    fun sendWifiToSavedDevice() {

        // Continue on Step 3 isn't gated by isFinalizingLan
        if (isFinalizingLan) return

        val deviceId = lanReconfigureDeviceId
        val token = pairingToken
        val ready = _lanWifiState.value as? LanWifiUiState.Ready

        val selected = ready?.networks?.firstOrNull { it.ssid == ready.selectedSsid }

        if (deviceId == null || token.isNullOrBlank()) {
            lanSaveDialog = LanSaveDialogState(
                title = "Device Not Reachable",
                message = "Lost track of the device being set up. Restart the setup wizard " +
                        "and reconnect to its hotspot."
            )
            return
        }

        if (selected == null) {
            lanSaveDialog = LanSaveDialogState(
                title = "No Network Selected",
                message = "Choose a WiFi network and enter its password to continue."
            )
            return
        }

        if (!selected.isOpen && ready.password.isEmpty()) {
            showValidationDialog(
                title = "Password Required",
                message = "Enter the password for ${selected.ssid} to continue."
            )
            return
        }

        lanSelectedSsid = selected.ssid
        lanSelectedPassword = ready.password

        viewModelScope.launch {

            isFinalizingLan = true

            _iotUiState.update { currentState ->
                currentState.copy(
                    isLoading = true,
                    loadingMessage = "Connecting..."
                )
            }

            try {
                when (lanProvisioningMethod) {

                    LanProvisioningMethod.ACCESS_POINT ->
                        provisionSavedDevice(
                            deviceId = deviceId,
                            token = token,
                            ssid = selected.ssid,
                            password = ready.password
                        )

                    LanProvisioningMethod.BLUETOOTH ->
                        provisionSavedDeviceViaBluetooth(
                            deviceId = deviceId,
                            token = token,
                            ssid = selected.ssid,
                            password = ready.password
                        )
                }
            } finally {
                _iotUiState.update { currentState ->
                    currentState.copy(
                        isLoading = false
                    )
                }
                isFinalizingLan = false
            }
        }
    }

    /**
     * Pushes credentials to a claimed device, waits for it to join, records its
     * IP, then moves the phone onto the same network.
     */
    private suspend fun provisionSavedDevice(
        deviceId: String,
        token: String,
        ssid: String,
        password: String
    ) {
        // ---- Send credentials over the hotspot ----------------------------
        val configured = runCatching {
            connectDeviceToWiFUseCase(
                host = DEFAULT_AP_IP,
                token = token,
                ssid = ssid,
                password = password
            )
        }

        if (configured.isFailure) {
            Log.e("IoTViewModel", "/configure failed", configured.exceptionOrNull())

            lanSaveDialog = LanSaveDialogState(
                title = "Couldn't Send WiFi Details",
                message = "The device did not accept the WiFi details for \"$ssid\". " +
                        "Choose a network again and re-enter the password.",
                action = LanSaveAction.RECONFIGURE_WIFI,
                confirmText = "Choose WiFi"
            )
            return
        }

        // ---- Wait for the device to actually join -------------------------
        val status = awaitDeviceOnLan()

        if (status == null) {
            lanSaveDialog = LanSaveDialogState(
                title = "Device Couldn't Join \"$ssid\"",
                message = "The device could not connect to \"$ssid\". The password may be " +
                        "wrong, or the network may be out of range of the device. Choose a " +
                        "network again and re-enter the password.",
                action = LanSaveAction.RECONFIGURE_WIFI,
                confirmText = "Choose WiFi"
            )
            return
        }

        // ---- Record where it landed ---------------------------------------
        runCatching { updateDeviceIpUseCase(deviceId, status.ip) }
            .onFailure { Log.e("IoTViewModel", "updateDeviceIp failed", it) }

        // ---- Update connection mode to LAN ---------------------------------------
        runCatching { updateDeviceConnectionModeUseCase(deviceId, ConnectionMode.LAN) }
            .onFailure { Log.e("IoTViewModel", "updateConnectionMode failed", it) }


        clearLanWifiReconfigure()
        error = null
        lanSaveDialog = null

        // ---- Hand the phone over ------------------------------------------
        // Drop the process binding to the hotspot so a normal Wi-Fi switch in
        // Settings actually takes effect for our traffic.
        disconnectFromDeviceWifiUseCase()

        lanDeviceIp = status.ip
        lanSwitchFailed = false
        lanSuccessStage = LanSuccessStage.SWITCH_NETWORK

        // Step 3 has to pop back to Success before the hand-off UI is visible.
        // No-op when we are already on Success (the finalize path).
        _lanHandoffReady.emit(Unit)

        // Deliberately no _lanSetupFinished here. The device is on the LAN but
        // the phone is not, so navigating now would show a "Connected" card
        // the app cannot actually reach.
        verifyPhoneOnDeviceNetwork()
    }

    /**
     * Polls /wifi-status on the hotspot until the device reports a real IP.
     *
     * The firmware gives up after WIFI_TIMEOUT (10s) and then retries every 5s,
     * so this window covers one full attempt plus a retry.
     */
    private suspend fun awaitDeviceOnLan(): WiFiStatusDto? {

        repeat(WIFI_STATUS_POLL_ATTEMPTS) { attempt ->

            delay(WIFI_STATUS_POLL_DELAY_MS)

            val status = runCatching {
                checkDeviceWiFiStatusUseCase(DEFAULT_AP_IP)
            }.getOrNull()

            Log.d(
                "AP_DEBUG",
                "wifi-status attempt ${attempt + 1}: connected=${status?.connected} ip=${status?.ip}"
            )

            if (status != null &&
                status.connected &&
                status.ip.isNotBlank() &&
                status.ip != "0.0.0.0"
            ) {
                return status
            }
        }

        return null
    }

    /**
     * Pings the device at its LAN address to prove the phone followed it over.
     *
     * Called on entry to SWITCH_NETWORK, on every ON_RESUME, and from the
     * "I'm Connected" button. Only a real answer advances the wizard.
     */
    fun verifyPhoneOnDeviceNetwork() {

        val ip = lanDeviceIp

        if (ip.isBlank()) return

        // Already checking, or already done
        if (lanSuccessStage != LanSuccessStage.SWITCH_NETWORK) return

        viewModelScope.launch {

            lanSuccessStage = LanSuccessStage.VERIFYING

            var reached = false

            repeat(PHONE_SWITCH_POLL_ATTEMPTS) { attempt ->

                if (reached) return@repeat

                val result = verifyDeviceConnectionUseCase(host = ip)

                result.onSuccess { info ->
                    Log.d("AP_DEBUG", "Reached ${info.deviceId} at $ip")
                    reached = true
                }

                result.onFailure {
                    Log.d(
                        "AP_DEBUG",
                        "Phone not on device network yet (attempt ${attempt + 1})"
                    )
                }

                if (!reached) delay(PHONE_SWITCH_POLL_DELAY_MS)
            }

            if (reached) {
                lanSwitchFailed = false
                lanSuccessStage = LanSuccessStage.SWITCH_NETWORK
                _lanSetupFinished.emit(Unit)
            } else {
                lanSwitchFailed = true
                lanSuccessStage = LanSuccessStage.SWITCH_NETWORK
            }
        }
    }

    fun connectToSameWiFiDialog() {
        showValidationDialog(
            title = "Finish WiFi Setup",
            message = "Reconnect this phone to \"${lanSelectedSsid}\" " +
                    "so SiliGuide can reach your device and finish setup."
        )
    }

    private companion object {
        const val WIFI_STATUS_POLL_ATTEMPTS = 12
        const val WIFI_STATUS_POLL_DELAY_MS = 1_500L

        const val PHONE_SWITCH_POLL_ATTEMPTS = 3
        const val PHONE_SWITCH_POLL_DELAY_MS = 1_200L
    }


    // ---- Step 4b of 4: Bluetooth provisioning -----------------------------

    private var isVerifyingLanBluetooth = false

    private val _lanBluetoothState =
        MutableStateFlow<LanBluetoothProvisioningUiState>(
            LanBluetoothProvisioningUiState.Pairing
        )

    val lanBluetoothState: StateFlow<LanBluetoothProvisioningUiState> =
        _lanBluetoothState.asStateFlow()

    /** Emitted once a paired, unclaimed SiliGuide device answers over SPP. */
    private val _lanBluetoothVerified = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lanBluetoothVerified: SharedFlow<Unit> = _lanBluetoothVerified.asSharedFlow()

    /** Emitted when BLUETOOTH_CONNECT has to be requested before anything else. */
    private val _lanBluetoothPermissionNeeded =
        MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val lanBluetoothPermissionNeeded: SharedFlow<Unit> =
        _lanBluetoothPermissionNeeded.asSharedFlow()

    /** The device we verified — kept so finalize can reopen a dropped socket. */
    private var lanBluetoothDevice: BluetoothDevice? = null

    /**
     * BLUETOOTH_CONNECT gates bondedDevices and createRfcommSocket on API 31+.
     * Below that the manifest install-time grants are enough.
     */
    val lanBluetoothPermissions: List<PermissionRequest>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                PermissionRequest(
                    permission = Manifest.permission.BLUETOOTH_CONNECT,
                    type = PermissionType.BLUETOOTH
                ),
                PermissionRequest(
                    permission = Manifest.permission.BLUETOOTH_SCAN,
                    type = PermissionType.BLUETOOTH
                )
            )
        } else {
            emptyList()
        }

    val lanBluetoothPermissionArray: Array<String>
        get() = lanBluetoothPermissions
            .map { it.permission }
            .toTypedArray()

    fun markLanBluetoothPermissionRequested() {
        viewModelScope.launch {
            permissionManager.markRequested(lanBluetoothPermissions)
        }
    }

    /**
     * device.name requires BLUETOOTH_CONNECT on API 31+. Suppression on
     * BluetoothDiscoveryManager doesn't cover reads made from here, so this
     * wraps every read site with an explicit catch instead of a blanket
     * @SuppressLint the linter can't verify.
     */
    private fun BluetoothDevice.safeName(): String =
        try {
            name ?: "SiliGuide device"
        } catch (_: SecurityException) {
            "SiliGuide device"
        }

    /**
     * Entry point for Step 4b.
     *
     * Runs on screen entry, on ON_RESUME (the user comes back from Bluetooth
     * Settings after pairing), and from Retry.
     */
    fun startLanBluetoothProvisioning(context: Context) {

        if (isVerifyingLanBluetooth ||
            _lanBluetoothState.value is LanBluetoothProvisioningUiState.SendingCredentials
        ) return

        viewModelScope.launch {

            isVerifyingLanBluetooth = true

            try {
                // ---- 1. Permission ------------------------------------------
                val status = permissionManager.getCombinedPermissionStatus(
                    context = context,
                    requests = lanBluetoothPermissions
                )

                if (status !is PermissionStatus.Granted) {

                    _lanBluetoothState.value = LanBluetoothProvisioningUiState.Pairing

                    if (status is PermissionStatus.PermanentlyDenied) {
                        showValidationDialog(
                            title = "Bluetooth Permission Required",
                            message = "Open App Settings and allow Nearby devices so SiliGuide " +
                                    "can talk to your IoT device over Bluetooth."
                        )
                    } else {
                        _lanBluetoothPermissionNeeded.emit(Unit)
                    }

                    return@launch
                }

                // ---- 2. Radio -----------------------------------------------
                if (!bluetoothManager.isBluetoothOn()) {

                    _lanBluetoothState.value = LanBluetoothProvisioningUiState.Error

                    showValidationDialog(
                        title = "Bluetooth is Off",
                        message = "Turn on Bluetooth in your phone's settings, then tap Retry."
                    )

                    return@launch
                }

                // ---- 3. Paired devices --------------------------------------
                val paired = withContext(Dispatchers.IO) {
                    runCatching { bluetoothDiscoveryManager.getPairedSiliGuideDevices() }
                        .getOrElse { emptyList() }
                }

                if (paired.isEmpty()) {
                    // Not an error — the user simply has not paired yet
                    _lanBluetoothState.value = LanBluetoothProvisioningUiState.Pairing
                    return@launch
                }

                // ---- 4. One device: go. Several: let the user choose ---------
                if (paired.size == 1) {
                    verifyLanBluetoothDevice(listOf(paired.first()))
                    return@launch
                }

                // Don't hold a selection screen open over a live socket
                withContext(Dispatchers.IO) { bluetoothManager.disconnect() }

                _lanBluetoothState.value = LanBluetoothProvisioningUiState.SelectDevice(
                    devices = paired.map { device ->
                        PairedSiliGuideDevice(
                            name = device.safeName(),
                            deviceId = device.address,
                            device = device
                        )
                    }
                )
            } finally {
                isVerifyingLanBluetooth = false
            }
        }
    }

    /** A row on the SelectDevice list was tapped. */
    fun selectLanBluetoothDevice(paired: PairedSiliGuideDevice) {
        viewModelScope.launch {
            verifyLanBluetoothDevice(listOf(paired.device))
        }
    }

    /**
     * Where to land after a device fails verification.
     *
     * With several bonded devices the list is more useful than a dead Error
     * screen — the user probably just tapped the wrong one.
     */
    private suspend fun lanBluetoothFallbackState(): LanBluetoothProvisioningUiState {

        val bonded = withContext(Dispatchers.IO) {
            runCatching { bluetoothDiscoveryManager.getPairedSiliGuideDevices() }
                .getOrElse { emptyList() }
        }

        return if (bonded.size > 1) {
            LanBluetoothProvisioningUiState.SelectDevice(
                devices = bonded.map { device ->
                    PairedSiliGuideDevice(
                        name = device.safeName(),
                        deviceId = device.address,
                        device = device
                    )
                }
            )
        } else {
            LanBluetoothProvisioningUiState.Error
        }
    }

    /**
     * Connects to the first paired SiliGuide device that answers, then runs the
     * same three checks the AP path runs: is it SiliGuide, is it already in the
     * database, is it already claimed.
     */
    private suspend fun verifyLanBluetoothDevice(candidates: List<BluetoothDevice>) {

        _iotUiState.update {
            it.copy(isLoading = true, loadingMessage = "Connecting…")
        }

        try {
            for (device in candidates) {

                val info = withContext(Dispatchers.IO) {
                    try {
                        bluetoothManager.connect(device)
                        Log.d("BT_DEBUG", "Socket open, asking GET_INFO")
                        bluetoothApi.getDeviceInfo()
                    } catch (e: Exception) {
                        Log.e(
                            "BT_DEBUG",
                            "Verify failed for ${device.safeName()}: ${e.javaClass.simpleName}: ${e.message}",
                            e
                        )
                        null
                    }
                }

//                val info = withContext(Dispatchers.IO) {
//                    runCatching {
//                        bluetoothManager.connect(device)
//                        bluetoothApi.getDeviceInfo()
//                    }.getOrNull()
//                }

                if (info == null) {
                    withContext(Dispatchers.IO) { bluetoothManager.disconnect() }
                    continue
                }

                if (info.device != "SiliGuide") {
                    withContext(Dispatchers.IO) { bluetoothManager.disconnect() }
                    continue
                }

                if (getIoTDeviceByIdUseCase(info.deviceId) != null) {
                    withContext(Dispatchers.IO) { bluetoothManager.disconnect() }

                    _lanBluetoothState.value = lanBluetoothFallbackState()

                    showValidationDialog(
                        title = "Device Already Registered",
                        message = "This SiliGuide device is already registered in your account."
                    )
                    return
                }

                if (info.claimed) {
                    withContext(Dispatchers.IO) { bluetoothManager.disconnect() }

                    _lanBluetoothState.value = lanBluetoothFallbackState()

                    showValidationDialog(
                        title = "Device Already Claimed",
                        message = "This SiliGuide device has already been claimed and cannot " +
                                "be paired again unless it is factory reset."
                    )
                    return
                }

                // Passed everything — socket stays open for finalize
                lanBluetoothDevice = device
                connectedDeviceId = info.deviceId
                pairingToken = info.token

                if (deviceName.isEmpty()) {
                    deviceName = "SiliGuide${info.deviceId.takeLast(3)}"
                }

                error = null

                _lanBluetoothState.value =
                    LanBluetoothProvisioningUiState.SendingCredentials(
                        deviceName = device.safeName()
                    )

                _lanBluetoothVerified.emit(Unit)
                return
            }

            // Nothing answered. With several devices bonded, drop back to the
            // list so the user can try a different one.
            val bonded = withContext(Dispatchers.IO) {
                runCatching { bluetoothDiscoveryManager.getPairedSiliGuideDevices() }
                    .getOrElse { emptyList() }
            }

            _lanBluetoothState.value =
                if (bonded.size > 1) {
                    LanBluetoothProvisioningUiState.SelectDevice(
                        devices = bonded.map { device ->
                            PairedSiliGuideDevice(
                                name = device.safeName(),
                                deviceId = device.address,
                                device = device
                            )
                        }
                    )
                } else {
                    LanBluetoothProvisioningUiState.Error
                }

            showValidationDialog(
                title = "Couldn't Reach the Device",
                message = "That SiliGuide device didn't answer. Make sure it is powered on " +
                        "and nearby, then try again."
            )

        } finally {
            _iotUiState.update { it.copy(isLoading = false) }
        }
    }

    fun retryLanBluetoothProvisioning(context: Context) {
        dismissValidationDialog()
        _lanBluetoothState.value = LanBluetoothProvisioningUiState.Pairing
        startLanBluetoothProvisioning(context)
    }

    /**
     * Reopens a socket that dropped while the user was naming the device.
     *
     * socket.isConnected only reflects local state — Android never clears it
     * when the remote end goes away, so a PING is the only honest check. The
     * ESP32 drops RFCOMM fairly often during WiFi retries, since Bluedroid and
     * WiFi share the same radio.
     */
    private suspend fun ensureLanBluetoothConnected(): Boolean {

        if (bluetoothManager.isConnected()) {
            val alive = withContext(Dispatchers.IO) {
                runCatching { bluetoothApi.ping() }.getOrDefault(false)
            }

            if (alive) return true

            Log.d("BT_DEBUG", "Socket looked connected but PING failed — reopening")

            withContext(Dispatchers.IO) { bluetoothManager.disconnect() }
        }

        val device = lanBluetoothDevice ?: return false

        return withContext(Dispatchers.IO) {
            runCatching { bluetoothManager.connect(device) }.isSuccess
        }
    }

    /**
     * Save Device on the Success screen, Bluetooth path.
     *
     * Same firmware ordering as AP — CLAIM before CONFIGURE, since the device
     * answers "unauthorized_bluetooth_access" until it is claimed.
     */
    fun finalizeLanBluetoothMode() {

        val deviceId = connectedDeviceId
        val token = pairingToken
        val ssid = lanSelectedSsid
        val name = deviceName.trim()

        if (deviceId == null || token.isNullOrBlank()) {
            lanSaveDialog = LanSaveDialogState(
                title = "Device Not Connected",
                message = "Lost the Bluetooth link to the device. Go back and reconnect."
            )
            return
        }

        if (ssid.isNullOrBlank()) {
            lanSaveDialog = LanSaveDialogState(
                title = "No Network Selected",
                message = "Go back and choose the WiFi network this device should join.",
                action = LanSaveAction.RECONFIGURE_WIFI,
                confirmText = "Choose WiFi"
            )
            return
        }

        if (name.isBlank()) {
            lanSaveDialog = LanSaveDialogState(
                title = "Name Required",
                message = "Give this device a name before saving it."
            )
            return
        }

        val password = lanSelectedPassword

        viewModelScope.launch {

            isFinalizingLan = true

            try {
                val userId = authRepository.getUserId()

                if (userId == null) {
                    lanSaveDialog = LanSaveDialogState(
                        title = "Not Signed In",
                        message = "Sign in again before adding a device."
                    )
                    return@launch
                }

                if (!ensureLanBluetoothConnected()) {
                    lanSaveDialog = LanSaveDialogState(
                        title = "Bluetooth Disconnected",
                        message = "Lost the Bluetooth link. Make sure the device is powered " +
                                "on and nearby, then try again."
                    )
                    return@launch
                }

                // ---- 1. Claim over SPP -----------------------------------
                val claim = withContext(Dispatchers.IO) {
                    runCatching { bluetoothApi.claimDevice(token) }
                }

                val claimResult = claim.getOrNull()

                if (claim.isFailure || !claimResult?.error.isNullOrBlank()) {
                    Log.e("IoTViewModel", "BT claim failed", claim.exceptionOrNull())

                    lanSaveDialog = LanSaveDialogState(
                        title = "Couldn't Claim Device",
                        message = claimResult?.error
                            ?: "The device rejected the pairing request. Try again."
                    )
                    return@launch
                }

                // ---- 2. Save locally ------------------------------------
                val saved = runCatching {
                    saveIoTDeviceUseCase(
                        IoTDevice(
                            deviceId = deviceId,
                            userId = userId,
                            deviceName = name.take(DEVICE_NAME_MAX_LENGTH),
                            pairingToken = token,
                            isActive = true,
                            // Filled in once the device reports its LAN address
                            lastKnownIp = "",
                            connectionMode = ConnectionMode.LAN,
                            dateAdded = System.currentTimeMillis()
                        )
                    )
                }

                if (saved.isFailure) {
                    Log.e("IoTViewModel", "Save failed", saved.exceptionOrNull())

                    lanSaveDialog = LanSaveDialogState(
                        title = "Couldn't Save Device",
                        message = "The device was claimed but could not be saved. Try again."
                    )
                    return@launch
                }

                // ---- 3, 4, 5. Provision --------------------------------
                provisionSavedDeviceViaBluetooth(
                    deviceId = deviceId,
                    token = token,
                    ssid = ssid,
                    password = password
                )

            } finally {
                isFinalizingLan = false
            }
        }
    }

    /**
     * Pushes credentials over SPP, waits for the device to join, records its IP.
     *
     * No phone hand-off: the phone was never on the device hotspot, so once the
     * IP is stored the wizard is finished.
     */
    private suspend fun provisionSavedDeviceViaBluetooth(
        deviceId: String,
        token: String,
        ssid: String,
        password: String
    ) {
        if (!ensureLanBluetoothConnected()) {
            lanSaveDialog = LanSaveDialogState(
                title = "Bluetooth Disconnected",
                message = "Lost the Bluetooth link. Make sure the device is powered on and " +
                        "nearby, then try again."
            )
            return
        }

        // ---- Send credentials ---------------------------------------------
        val configure = withContext(Dispatchers.IO) {
            runCatching {
                bluetoothApi.configureWiFi(
                    token = token,
                    ssid = ssid,
                    password = password
                )
            }
        }

        val configureResult = configure.getOrNull()

        if (configure.isFailure || !configureResult?.error.isNullOrBlank()) {
            Log.e("IoTViewModel", "BT /configure failed", configure.exceptionOrNull())

            lanSaveDialog = LanSaveDialogState(
                title = "Couldn't Send WiFi Details",
                message = "The device did not accept the WiFi details for \"$ssid\". " +
                        "Choose a network again and re-enter the password.",
                action = LanSaveAction.RECONFIGURE_WIFI,
                confirmText = "Choose WiFi"
            )
            return
        }

        // ---- Wait for it to join, over the same socket ---------------------
        val status = awaitDeviceOnLanViaBluetooth()

        if (status == null) {
            lanSaveDialog = LanSaveDialogState(
                title = "Device Couldn't Join \"$ssid\"",
                message = "The device could not connect to \"$ssid\". The password may be " +
                        "wrong, or the network may be out of range of the device. Choose a " +
                        "network again and re-enter the password.",
                action = LanSaveAction.RECONFIGURE_WIFI,
                confirmText = "Choose WiFi"
            )
            return
        }

        runCatching { updateDeviceIpUseCase(deviceId, status.ip) }
            .onFailure { Log.e("IoTViewModel", "updateDeviceIp failed", it) }

        // Bluetooth was only the delivery channel — LAN mode takes over now
        withContext(Dispatchers.IO) { bluetoothManager.disconnect() }

        clearLanWifiReconfigure()
        error = null
        lanSaveDialog = null

        // The phone was never moved, but that does NOT mean it is on the same
        // network the device just joined — it could be on another AP, on mobile
        // data, or on the 5GHz twin of a 2.4GHz-only SSID. Run the same
        // verification the AP path runs; if the phone is already there the ping
        // succeeds on the first try and this is invisible.
        lanDeviceIp = status.ip
        lanSwitchFailed = false
        lanSuccessStage = LanSuccessStage.SWITCH_NETWORK

        _lanHandoffReady.emit(Unit)

        verifyPhoneOnDeviceNetwork()
    }

    private suspend fun awaitDeviceOnLanViaBluetooth(): WiFiStatusDto? {

        repeat(WIFI_STATUS_POLL_ATTEMPTS) { attempt ->

            delay(WIFI_STATUS_POLL_DELAY_MS)

            val status = withContext(Dispatchers.IO) {
                runCatching { bluetoothApi.getWifiStatus() }.getOrNull()
            }

            if (status == null && !ensureLanBluetoothConnected()) {
                Log.d("BT_DEBUG", "Lost Bluetooth link mid-poll, giving up")
                return null
            }

            Log.d(
                "BT_DEBUG",
                "wifi-status attempt ${attempt + 1}: connected=${status?.connected} ip=${status?.ip}"
            )

            if (status != null &&
                status.connected &&
                status.ip.isNotBlank() &&
                status.ip != "0.0.0.0"
            ) {
                return status
            }
        }

        return null
    }

    fun stopLanBluetoothProvisioning() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { bluetoothManager.disconnect() }
        }
    }


    // ---- Wizard teardown --------------------------------------------------

    /**
     * Wipes every scrap of wizard state.
     *
     * lanReconfigureDeviceId matters most — leave it set and the NEXT wizard
     * run enters Step 3 already in reconfigure mode, so Continue forks to
     * sendWifiToSavedDevice() against a stale device instead of Step 4.
     */
    fun resetLanSetup() {
        _lanSetupMethodState.value = LanSetupMethodUiState()
        _lanPermissionState.value = LanPermissionUiState.Requesting
        _lanWifiState.value = LanWifiUiState.Scanning
        _lanApProvisioningState.value =
            LanApProvisioningUiState.Waiting(lanApDeviceSsid)

        lanSelectedSsid = null
        lanSelectedPassword = ""
        lanReconfigureDeviceId = null

        lanSuccessStage = LanSuccessStage.NAMING
        lanDeviceIp = ""
        lanSwitchFailed = false

        lanSaveDialog = null
        lanBlocker = null
        error = null
        validationDialog = null

        _lanBluetoothState.value = LanBluetoothProvisioningUiState.Pairing
        lanBluetoothDevice = null
    }



    // --- AP RECONFIGURATION STATE ---
    var apReconfigureDeviceId by mutableStateOf<String?>(null)
        private set

    fun startApReconfigure(deviceId: String) {
        apReconfigureDeviceId = deviceId
    }

    fun clearApReconfigure() {
        apReconfigureDeviceId = null
    }

    fun finalizeApModeSwitch() {
        viewModelScope.launch {
            val deviceId = apReconfigureDeviceId ?: return@launch

            _iotUiState.update {
                it.copy(isLoading = true, loadingMessage = "Switching mode...")
            }

            try {
                // Update connection mode to AP
                updateDeviceConnectionModeUseCase(deviceId, ConnectionMode.AP)
                // Ensure the IP points to the default AP IP
                updateDeviceIpUseCase(deviceId, DEFAULT_AP_IP)

                _setupComplete.emit(Unit)
            } catch (e: Exception) {
                Log.e("IoTViewModel", "Failed to switch to AP mode", e)
                showValidationDialog(
                    title = "Switch Failed",
                    message = "Could not update connection mode. Please try again."
                )
            } finally {
                _iotUiState.update { it.copy(isLoading = false) }
            }
        }
    }

}
