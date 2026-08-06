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
import com.example.siliguide.presentation.features.iot.lan_setup.LanPermissionUiState
import com.example.siliguide.presentation.features.iot.lan_setup.LanPreflightAction
import com.example.siliguide.presentation.features.iot.lan_setup.LanPreflightResult

data class ValidationDialogState(
    val title: String,
    val message: String
)

const val DEVICE_NAME_MAX_LENGTH = 12

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

        repeat(3) { attempt ->
            val result = verifyDeviceConnectionUseCase("192.168.4.1")

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
                            message = "This SiliGuide device has already been claimed and cannot be paired again unless it is factory reset."
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
                isSynced = false, // Initial state
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
                                message = "This SiliGuide device has already been claimed and cannot be paired again unless it is factory reset."
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
                    isSynced = false,
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

    /** Drives Step 1 of 4 — Location Permission. */
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

    /** Whatever is currently blocking Step 1, or null when nothing is. */
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
     * Turns per-permission statuses into the Step 1 artwork state.
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
     * Single gate for Step 1 → Step 2.
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

    /** Rationale shown by the "Why is this needed?" link on Step 1. */
    fun showLanPermissionRationale() {
        showValidationDialog(
            title = "Why These Permissions",
            message = "Android ties WiFi scanning to location access, and on Android 13 and " +
                    "newer it also requires Nearby devices access. Without both, the system " +
                    "returns an empty network list, so SiliGuide cannot show the WiFi network " +
                    "your IoT device should join. SiliGuide never stores or shares your location."
        )
    }

    fun resetLanSetup() {
        _lanPermissionState.value = LanPermissionUiState.Requesting
        lanBlocker = null
        error = null
        validationDialog = null
    }


}
