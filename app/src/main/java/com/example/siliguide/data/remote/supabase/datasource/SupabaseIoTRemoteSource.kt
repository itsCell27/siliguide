package com.example.siliguide.data.remote.supabase.datasource

import com.example.siliguide.data.remote.supabase.dto.IoTDeviceDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseIoTRemoteSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val table by lazy {
        supabaseClient.postgrest.from("iot_devices")
    }

    suspend fun upsertDevices(devices: List<IoTDeviceDto>) = withContext(Dispatchers.IO) {
        if (devices.isEmpty()) return@withContext
        table.upsert(devices)
    }

    suspend fun fetchDevicesByUser(userId: String): List<IoTDeviceDto> = withContext(Dispatchers.IO) {
        table.select {
            filter {
                eq("user_id", userId)
                eq("is_deleted", false)
            }
        }.decodeList<IoTDeviceDto>()
    }

    suspend fun deleteDevice(deviceId: String) = withContext(Dispatchers.IO) {
        table.delete {
            filter {
                eq("device_id", deviceId)
            }
        }
    }
}
