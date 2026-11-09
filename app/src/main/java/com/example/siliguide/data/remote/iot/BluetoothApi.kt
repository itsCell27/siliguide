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
     * Plaintext:   {"ssid":"...","pass":"..."}  (JSON-escaped, see wifiCredentialsJson)
     */
    @Throws(Exception::class)
    suspend fun configureWiFi(
        token: String,
        ssid: String,
        password: String
    ): ConfigureWiFiResponseDto {

        val plainToken = plainTokenOf(token)

        // Field names must stay "ssid" and "pass" — firmware looks for exactly these
        val plainPayload = wifiCredentialsJson(ssid, password)

        val encryptedBase64 = crypto.encrypt(plainPayload, plainToken)

        // Base64 (NO_WRAP) contains no colon and no newline, so the single
        // split on the device and the readResponse() line terminator both hold
        val secureCommand = "${BluetoothCommands.CONFIGURE}:$encryptedBase64"

        val response = bluetoothManager.executeCommand(secureCommand)

        Log.d("BluetoothApi", "Configure Response: $response")

        return json.decodeFromString(response.trim())
    }

    /** DB tokens are encrypted; a raw 64-hex token is used as-is. */
    private fun plainTokenOf(token: String): String =
        if (token.length == 64 && token.all { it in "0123456789ABCDEFabcdef" }) {
            token
        } else {
            pairingTokenCrypto.decrypt(token)
        }

    /**
     * LOCKED GATEWAY: Rejoin the WiFi saved in device memory (firmware 1.1.0+
     * keeps the last WiFi that actually worked there). Cancels a CONFIGURE
     * attempt still running. Wire format: CONNECT_WIFI:base64(...)
     */
    @Throws(Exception::class)
    suspend fun reconnectSavedWiFi(token: String) {
        val encryptedBase64 = crypto.encrypt(commandJson("connect_wifi"), plainTokenOf(token))

        val response = bluetoothManager.executeCommand(
            "${BluetoothCommands.CONNECT_WIFI}:$encryptedBase64"
        )
        Log.d("BluetoothApi", "Connect-WiFi Response: $response")

        if (!response.contains("connecting") && !response.contains("already_connected")) {
            throw IllegalStateException("Device refused connect-wifi: $response")
        }
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
     * Finds a specific paired device, opens a connection if needed, executes CLAIM,
     * and cleanly disconnects.
     */
    suspend fun claimSpecificDevice(hardwareMac: String, token: String): ClaimResponseDto {
        if (!bluetoothManager.isBluetoothOn()) {
            return ClaimResponseDto(status = "error", error = "Bluetooth is turned off.")
        }
        val device = bluetoothDiscoveryManager.getSpecificPairedDevice(hardwareMac)
            ?: return ClaimResponseDto(
                status = "error",
                error = "Device not found in paired Bluetooth devices. Please pair it in Android Bluetooth settings."
            )

        return try {
            bluetoothManager.connect(device)
            claimDevice(token)
        } catch (e: Exception) {
            Log.e("BluetoothApi", "Failed to claim specific device over Bluetooth", e)
            ClaimResponseDto(status = "error", error = e.message ?: "Failed to claim device.")
        } finally {
            bluetoothManager.disconnect()
        }
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

        val rawResponse = response.trim()

        if (rawResponse.isBlank()) {
            throw java.io.IOException("Empty response received from IoT device over Bluetooth")
        }

        if (rawResponse.startsWith("{\"error\"")) {
            throw java.io.IOException("IoT device returned Bluetooth error: $rawResponse")
        }

        // 1. If a token IS provided, attempt to decrypt the Base64 AES-GCM payload locally
        if (token != null) {
            val plainToken = pairingTokenCrypto.decrypt(token)
            val decryptedJson = crypto.decrypt(rawResponse, plainToken)
            if (decryptedJson != null) {
                return json.decodeFromString(decryptedJson)
            }
        }

        // 2. If decryption failed or no token was provided, check if the response
        //    contains unencrypted raw sensor data. If so, the device is not claimed.
        val isRawSensorData = runCatching {
            json.decodeFromString<SensorResponseDto>(rawResponse)
        }.isSuccess

        if (isRawSensorData) {
            Log.d("BluetoothApi", "IoT device is not claimed.")
            throw SecurityException("IoT device is not claimed.")
        }

        if (token == null) {
            throw SecurityException("Pairing token is required to decrypt sensor data.")
        }

        // 3. Verify if the payload is a complete, well-formed Base64 payload.
        // A genuine encrypted sensor payload consists of IV(12) + ~230B JSON + Tag(16) -> Base64 >= 200 chars.
        // If truncated by connection loss or RF noise, treat as a transmission error (IOException).
        val isValidBase64Format = rawResponse.length >= 200 && runCatching {
            android.util.Base64.decode(rawResponse, android.util.Base64.NO_WRAP)
        }.isSuccess

        if (!isValidBase64Format) {
            throw java.io.IOException("Incomplete or corrupted Bluetooth payload received (${rawResponse.length} chars)")
        }

        // 4. Intact Base64 payload failed AES-GCM authentication (token mismatch or tampered data)
        throw SecurityException("Failed to decrypt sensor payload. Invalid token or tampered data.")
    }

    /**
     * Specific function for getting device info in IoTRepositoryImpl to check
     * if the device is online via Bluetooth.
     */
    suspend fun getSpecificDeviceInfo(hardwareMac: String): DeviceInfoDto? {
        // check if bluetooth is on
        if (!bluetoothManager.isBluetoothOn()) return null

        // find the bluetooth device
        val device = bluetoothDiscoveryManager.getSpecificPairedDevice(hardwareMac) ?: return null

        // connect to the device
        return try {
            bluetoothManager.connect(device)
            getDeviceInfo()
        } catch (e: Exception) {
            null
        } finally {
            bluetoothManager.disconnect()
        }
    }

    suspend fun getSpecificDeviceSensorData(hardwareMac: String, token: String): SensorResponseDto? {
        // check if bluetooth is on
        if (!bluetoothManager.isBluetoothOn()) return null

        // find the bluetooth device
        val device = bluetoothDiscoveryManager.getSpecificPairedDevice(hardwareMac) ?: return null

        // connect to the device
        return try {
            bluetoothManager.connect(device)
            getSensorData(token)
        } catch (e: SecurityException) {
            throw e   // decrypt/token failure — let it propagate, don't collapse to null
        } catch (e: Exception) {
            null      // everything else (real connection failures) unchanged
        } finally {
            bluetoothManager.disconnect()
        }
    }


    /**
     * LOCKED GATEWAY: Sends a secure wipe/reset command to the IoT Device over Bluetooth.
     * Firmware validates the AES-GCM mathematical tag before wiping the NVS preferences.
     */
    @Throws(Exception::class)
    suspend fun resetDevice(token: String) {
        // 1. Decrypt token from SQLDelight DB first
        val plainToken = pairingTokenCrypto.decrypt(token)

        // 2. Dummy JSON payload (firmware only cares about mathematical decryption success)
        val unencryptedPayload = """{"command":"factory_reset"}"""

        // 3. Encrypt the JSON into a Base64 string using the pairing token
        val encryptedBase64 = crypto.encrypt(unencryptedPayload, plainToken)
            ?: throw SecurityException("Failed to encrypt reset payload.")

        // 4. Construct the Bluetooth command string: "RESET:iv+ciphertext+tag"
        val secureCommand = "${BluetoothCommands.RESET}:$encryptedBase64"

        // 5. Send over the RFCOMM socket
        val response = bluetoothManager.executeCommand(secureCommand)
        Log.d("BluetoothApi", "Reset Response: $response")

        // 6. Validate the device acknowledged the reset
        if (!response.contains("resetting")) {
            throw IllegalStateException("Failed to reset device via Bluetooth. Firmware rejected. Response: $response")
        }
    }

    /**
     * Specific function for the Repository to find, connect, reset, and disconnect
     * a specific SiliGuide device automatically.
     */
    suspend fun resetSpecificDevice(hardwareMac: String, token: String): Boolean {
        // 1. Check if Bluetooth is enabled
        if (!bluetoothManager.isBluetoothOn()) return false

        // 2. Find the specific paired hardware
        val device = bluetoothDiscoveryManager.getSpecificPairedDevice(hardwareMac) ?: return false

        // 3. Connect, execute wipe, and cleanly disconnect
        return try {
            bluetoothManager.connect(device)
            resetDevice(token)
            true
        } catch (e: Exception) {
            Log.e("BluetoothApi", "Failed to reset specific device over Bluetooth", e)
            false
        } finally {
            // Even if the ESP32 restarts instantly and drops the connection,
            // we must cleanly close the Android socket to prevent memory leaks.
            bluetoothManager.disconnect()
        }
    }
}