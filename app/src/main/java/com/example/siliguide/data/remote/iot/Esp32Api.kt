package com.example.siliguide.data.remote.iot

import com.example.siliguide.data.remote.iot.dto.SensorResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class Esp32Api {

    private val client = HttpClient(OkHttp) {
        // 1. Configure the OkHttp Engine for IoT hardware
        engine {
            config {
                retryOnConnectionFailure(true)
            }
        }

        // 2. Set strict timeouts so the app doesn't freeze if the ESP32 disconnects
        install(HttpTimeout) {
            requestTimeoutMillis = 5000 // 5 seconds max waiting for sensor data
            connectTimeoutMillis = 3000 // 3 seconds max to establish connection
        }

        // 3. Configure JSON Parsing (Lenient mode is great for embedded hardware)
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true // Prevents crashes if you add/remove sensors on the ESP32 later
            })
        }
    }

    suspend fun getSensorData(): SensorResponseDto {
        // Note: In a later refactor, we will make "192.168.4.1" dynamic
        // based on your LAN/AP connection mode!
        return client
            .get("http://192.168.4.1/data")
            .body()
    }
}