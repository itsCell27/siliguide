package com.example.siliguide.data.remote.iot

import android.util.Log
import com.example.siliguide.core.security.AesGcmCrypto
import com.example.siliguide.core.security.PairingTokenCrypto
import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.data.remote.iot.dto.SensorResponseDto
import com.example.siliguide.data.remote.iot.dto.WiFiStatusDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.client.plugins.timeout

class Esp32Api @Inject constructor(
    private val crypto: AesGcmCrypto,
    private val pairingTokenCrypto: PairingTokenCrypto
) {

    // Same Json config the ContentNegotiation plugin uses, reused for manual
    // decode after decryption (device sends plain text/plain body when claimed,
    // not application/json, so the plugin never touches it).
    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    private val client = HttpClient(OkHttp) {
        // 1. Configure the OkHttp Engine for IoT hardware
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }

        // 2. Strict timeouts to prevent socket freezing/buffer attacks
        install(HttpTimeout) {
            requestTimeoutMillis = 5000
            connectTimeoutMillis = 3000
        }

        // 3. Type-Safe Serialization gatekeeper
        install(ContentNegotiation) {
            json(json)
        }
    }

    // Device uses a Zero-Knowledge Architecture.
    // Unclaimed device -> /data returns raw JSON (setup mode).
    // Claimed device   -> /data returns a base64 AES-GCM blob (text/plain).
    // The App decrypts it locally; the token never traverses the network.
    suspend fun getSensorData(host: String, token: String? = null): SensorResponseDto {
        // 1. Fetch the payload. We do NOT pass the token in the request.
        val httpResponse = client.get("http://$host/data")
        if (httpResponse.status.value !in 200..299) {
            throw java.io.IOException("ESP32 HTTP returned status ${httpResponse.status.value}")
        }
        val rawResponse: String = httpResponse.body<String>().trim()

        if (rawResponse.isBlank()) {
            throw java.io.IOException("Empty response received from ESP32")
        }

        if (rawResponse.startsWith("{\"error\"")) {
            throw java.io.IOException("ESP32 returned error: $rawResponse")
        }

        // 2. If a token IS provided, attempt to decrypt the Base64 AES-GCM payload locally.
        if (token != null) {
            val plainToken = pairingTokenCrypto.decrypt(token)
            val decryptedJson = crypto.decrypt(rawResponse, plainToken)
            if (decryptedJson != null) {
                return json.decodeFromString(decryptedJson)
            }
        }

        // 3. If decryption failed or no token was provided, check if the response
        //    contains unencrypted raw sensor data. If it can be parsed as SensorResponseDto
        //    without a token, the IoT device is NOT claimed (e.g. factory reset or in setup mode).
        val isRawSensorData = runCatching {
            json.decodeFromString<SensorResponseDto>(rawResponse)
        }.isSuccess

        if (isRawSensorData) {
            Log.d("Esp32Api", "IoT device is not claimed.")
            throw SecurityException("IoT device is not claimed.")
        }

        // 4. If no token was provided and the payload is encrypted
        if (token == null) {
            throw SecurityException("Pairing token is required to decrypt sensor data.")
        }

        // 5. Verify if the payload is a complete, well-formed Base64 payload.
        // A genuine encrypted sensor payload consists of IV(12) + ~230B JSON + Tag(16) -> Base64 >= 200 chars.
        // If truncated by socket reset or timeout, treat as a transmission error (IOException).
        val isValidBase64Format = rawResponse.length >= 200 && runCatching {
            android.util.Base64.decode(rawResponse, android.util.Base64.NO_WRAP)
        }.isSuccess

        if (!isValidBase64Format) {
            throw java.io.IOException("Incomplete or corrupted HTTP payload received (${rawResponse.length} chars)")
        }

        // 6. Intact Base64 payload failed AES-GCM authentication (token mismatch or tampered data)
        throw SecurityException("Failed to decrypt sensor payload. Invalid token or tampered data.")
    }

    // Send WiFi credentials to IoT Device securely via AES-GCM
    suspend fun configureWiFi(
        host: String,
        token: String,
        ssid: String,
        password: String
    ) {
        // decrypt token from SQLDelight DB first
        val plainToken = pairingTokenCrypto.decrypt(token)

        // 1. Format credentials into the JSON string expected by ESP32 C++ parser
        val unencryptedPayload = """{"ssid":"$ssid","pass":"$password"}"""

        // 2. Encrypt the JSON into a Base64 string using the pairing token
        val encryptedBase64Payload = crypto.encrypt(unencryptedPayload, plainToken)
            ?: throw SecurityException("Failed to encrypt Wi-Fi payload.")

        // 3. POST the raw encrypted Base64 string to /configure
        val response = client.post("http://$host/configure") {
            contentType(ContentType.Text.Plain)
            setBody(encryptedBase64Payload)
        }

        // Optional: Validate response status code or throw on HTTP errors
        if (response.status.value !in 200..299) {
            throw IllegalStateException("Failed to configure Wi-Fi. HTTP Status: ${response.status.value}")
        }
    }

    // Check IoT device WiFi Status
    suspend fun checkWiFiStatus(host: String): WiFiStatusDto {
        return client.get("http://$host/wifi-status").body()
    }

    // get device /info for discovery — always unlocked, never encrypted.
    // Device masks the token field itself once claimed; nothing to decrypt here.
    suspend fun getDeviceInfo(host: String, quick: Boolean = false): DeviceInfoDto {
        return client.get("http://$host/info") {
            if (quick) {
                timeout {
                    connectTimeoutMillis = 800
                    requestTimeoutMillis = 1_200
                }
            }
        }.body()
    }

    // Add the Handshake lock endpoint so your repository can claim the device.
    // Token still sent as plain text query param — this IS the handshake,
    // happens before any AES key material is "in use" as a shared secret.
    suspend fun claimDevice(
        host: String,
        token: String
    ) {
        client.post("http://$host/claim") {
            parameter("token", token)
        }
    }


    // Send a secure wipe/reset command to the IoT Device via AES-GCM
    suspend fun resetDevice(
        host: String,
        token: String
    ) {
        // 1. Decrypt token from SQLDelight DB first
        val plainToken = pairingTokenCrypto.decrypt(token)

        // 2. The ESP32 handleReset() function only checks if decryption is mathematically successful
        // to prove authorization. We can send a simple dummy JSON payload.
        val unencryptedPayload = """{"command":"factory_reset"}"""

        // 3. Encrypt the JSON into a Base64 string using the pairing token
        val encryptedBase64Payload = crypto.encrypt(unencryptedPayload, plainToken)
            ?: throw SecurityException("Failed to encrypt reset payload.")

        // 4. POST the raw encrypted Base64 string to /reset
        val response = client.post("http://$host/reset") {
            contentType(ContentType.Text.Plain)
            setBody(encryptedBase64Payload)
        }

        // 5. Validate response status code
        if (response.status.value !in 200..299) {
            throw IllegalStateException("Failed to reset device. HTTP Status: ${response.status.value}")
        }
    }
}