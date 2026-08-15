package com.example.siliguide.data.remote.iot

import android.util.Log
import com.example.siliguide.core.security.AesGcmCrypto
import com.example.siliguide.core.security.PairingTokenCrypto
import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.data.remote.iot.dto.SensorResponseDto
import com.example.siliguide.data.remote.iot.dto.ClaimResponseDto // Make sure to create this simple DTO
import com.example.siliguide.data.remote.iot.dto.ConfigureWiFiResponseDto
import com.example.siliguide.data.remote.iot.dto.WiFiStatusDto
import kotlinx.serialization.json.Json
import javax.inject.Inject

class BluetoothApi @Inject constructor(
    private val bluetoothManager: BluetoothManager,
    private val bluetoothDiscoveryManager: BluetoothDiscoveryManager,
    private val crypto: AesGcmCrypto,
    private val pairingTokenCrypto: PairingTokenCrypto
) {

    init {
        Log.d(
            "BluetoothApi",
            "BluetoothManager=${System.identityHashCode(bluetoothManager)}"
        )
    }

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    /**
     * UNLOCKED: Pings the ESP32 to verify active RFCOMM socket connectivity.
     * Expects a raw "PONG" string back from firmware.
     */
    @Throws(Exception::class)
    suspend fun ping(): Boolean {
        val response = bluetoothManager.executeCommand(BluetoothCommands.PING)
        return response.trim() == "PONG"
    }

    /**
     * UNLOCKED: Fetches the public device identification frame.
     * Safe to call without a token; if claimed, the ESP32 masks the token asset.
     */
    @Throws(Exception::class)
    suspend fun getDeviceInfo(): DeviceInfoDto {
        val response = bluetoothManager.executeCommand(BluetoothCommands.GET_INFO)
        Log.d("BluetoothApi", "Device Info Response: $response")
        return json.decodeFromString(response)
    }

    /**
     * UNLOCKED: Fetches the current WiFi connection status.
     */
    suspend fun getWifiStatus(): WiFiStatusDto {
        val response = bluetoothManager.executeCommand(BluetoothCommands.GET_WIFI_STATUS)
        Log.d("BluetoothApi", "WiFi Status Response: $response")
        return json.decodeFromString(response)
    }

    /**
     * LOCKED GATEWAY: Hands the device its home Wi-Fi credentials.
     *
     * Firmware rejects this with "unauthorized_bluetooth_access" unless the
     * device is already claimed, so claimDevice() has to run first.
     *
     * Wire format: CONFIGURE:base64( IV[12] + ciphertext + tag[16] )
     * Plaintext:   {"ssid":"...","pass":"..."}
     *
     * The device parses that JSON with indexOf, not a real parser, so a quote
     * or backslash in either value would corrupt the extraction.
     */
    @Throws(Exception::class)
    suspend fun configureWiFi(
        token: String,
        ssid: String,
        password: String
    ): ConfigureWiFiResponseDto {

        require(!ssid.contains('"') && !ssid.contains('\\')) {
            "SSID cannot contain quotes or backslashes"
        }
        require(!password.contains('"') && !password.contains('\\')) {
            "Password cannot contain quotes or backslashes"
        }

        // decrypt token from SQLDelight DB first
        val plainToken = pairingTokenCrypto.decrypt(token)

        // Field names must stay "ssid" and "pass" — firmware looks for exactly these
        val plainPayload = """{"ssid":"$ssid","pass":"$password"}"""

        val encryptedBase64 = crypto.encrypt(plainPayload, plainToken)

        // Base64 (NO_WRAP) contains no colon and no newline, so the single
        // split on the device and the readResponse() line terminator both hold
        val secureCommand = "${BluetoothCommands.CONFIGURE}:$encryptedBase64"

        val response = bluetoothManager.executeCommand(secureCommand)

        Log.d("BluetoothApi", "Configure Response: $response")

        return json.decodeFromString(response.trim())
    }

    /**
     * SECURE HANDSHAKE: Finalizes edge provisioning by locking the token.
     * Format Sent: "CLAIM:abc123xyz"
     */
    @Throws(Exception::class)
    suspend fun claimDevice(token: String): ClaimResponseDto {
        val secureCommand = "${BluetoothCommands.CLAIM}:$token"
        val response = bluetoothManager.executeCommand(secureCommand)

        Log.d("BluetoothApi", "Claim Response: $response")
        return json.decodeFromString(response)
    }

    /**
     * LOCKED GATEWAY: Pulls raw sensor data.
     * Required once deviceClaimed == true.
     */
    @Throws(Exception::class)
    suspend fun getSensorData(token: String? = null): SensorResponseDto {
        // Zero-Knowledge Architecture: We DO NOT send the token over the air.
        // We only send the exact command string.
        val command = BluetoothCommands.GET_DATA
        val response = bluetoothManager.executeCommand(command)

        Log.d("BluetoothApi", "Sensor Response received")

        // 1. Setup Mode (Unclaimed): Parse the raw JSON directly
        if (token == null) {
            return json.decodeFromString(response.trim())
        }

        // decrypt token from SQLDelight DB first
        val plainToken = pairingTokenCrypto.decrypt(token)

        // 2. Secure Mode (Claimed): Decrypt the Base64 AES-GCM payload locally
        val decryptedJson = crypto.decrypt(response.trim(), plainToken)

        // 3. Fail-Safe: Catch bit-flipping attacks or incorrect tokens
        if (decryptedJson == null) {
            throw SecurityException("Failed to decrypt Bluetooth payload. Invalid token or tampered data.")
        }

        return json.decodeFromString(decryptedJson)
    }

    /**
     * Specific function for getting device info in IoTRepositoryImpl to check
     * if the device is online via Bluetooth.
     */
    suspend fun getSpecificDeviceInfo(deviceId: String): DeviceInfoDto? {
        // check if bluetooth is on
        if (!bluetoothManager.isBluetoothOn()) return null

        // find the bluetooth device
        val device = bluetoothDiscoveryManager.getSpecificPairedDevice(deviceId) ?: return null

        // connect to the device
        try {
            bluetoothManager.connect(device)
        } catch (e: Exception) {
            return null
        }

        val info = getDeviceInfo()
        bluetoothManager.disconnect()

        return info
    }

    suspend fun getSpecificDeviceSensorData(deviceId: String, token: String): SensorResponseDto? {
        // check if bluetooth is on
        if (!bluetoothManager.isBluetoothOn()) return null

        // find the bluetooth device
        val device = bluetoothDiscoveryManager.getSpecificPairedDevice(deviceId) ?: return null

        // connect to the device
        try {
            bluetoothManager.connect(device)
        } catch (e: Exception) {
            return null
        }

        val sensorData = getSensorData(token)
        bluetoothManager.disconnect()

        return sensorData
    }
}