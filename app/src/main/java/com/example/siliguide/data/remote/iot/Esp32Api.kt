package com.example.siliguide.data.remote.iot

import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.data.remote.iot.dto.SensorResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import io.ktor.client.request.post

class Esp32Api {

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
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    // Pass the token so the ESP32 doesn't block you with a 401 Unauthorized
    suspend fun getSensorData(host: String, token: String? = null): SensorResponseDto {
        return client.get("http://$host/data") {
            // If we have a token (device is claimed), append it securely to the request
            if (token != null) {
                parameter("token", token)
            }
        }.body()
    }

    // get device /info for discovery
    suspend fun getDeviceInfo(host: String): DeviceInfoDto {
        return client.get("http://$host/info").body()
    }

    // Add the Handshake lock endpoint so your repository can claim the device
    suspend fun claimDevice(
        host: String,
        token: String
    ) {
        client.post("http://$host/claim") {
            parameter("token", token)
        }
    }
}