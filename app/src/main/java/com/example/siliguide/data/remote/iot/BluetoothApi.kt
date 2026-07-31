package com.example.siliguide.data.remote.iot

import android.util.Log
import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.data.remote.iot.dto.SensorResponseDto
import kotlinx.serialization.json.Json
import javax.inject.Inject

class BluetoothApi @Inject constructor(
    private val bluetoothManager: BluetoothManager
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

    @Throws(Exception::class)
    suspend fun getSensorData(): SensorResponseDto {

        val response = bluetoothManager.executeCommand(
            BluetoothCommands.GET_DATA
        )

        Log.d("BluetoothApi", "Response: $response")
        return json.decodeFromString(response)
    }

    @Throws(Exception::class)
    suspend fun getDeviceInfo(): DeviceInfoDto {

        val response = bluetoothManager.executeCommand(
            BluetoothCommands.GET_INFO
        )

        return json.decodeFromString(response)
    }
}