package com.example.siliguide.data.remote.iot

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
        val rawResponse: String = client.get("http://$host/data").body()

        // 2. If no token is provided by the repository, assume we are in Setup Mode
        //    and parse the raw JSON directly.
        if (token == null) {
            return json.decodeFromString(rawResponse)
        }

        // decrypt token from SQLDelight DB first
        val plainToken = pairingTokenCrypto.decrypt(token)

        // 3. If a token IS provided, we assume the payload is Base64 AES-GCM encrypted.
        //    Attempt to decrypt it locally.
        val decryptedJson = crypto.decrypt(rawResponse, plainToken)

        // 4. Fallback check: If decryption fails (e.g., wrong token, tampered data),
        //    throw an exception so the Repository can catch it and show an "Offline" state.
        if (decryptedJson == null) {
            throw SecurityException("Failed to decrypt sensor payload. Invalid token or tampered data.")
        }

        return json.decodeFromString(decryptedJson)
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
}