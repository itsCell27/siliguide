package com.example.siliguide.data.remote.supabase.datasource

import com.example.siliguide.data.remote.supabase.dto.PlantDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabasePlantRemoteSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val table by lazy {
        supabaseClient.postgrest.from("plants")
    }

    suspend fun upsertPlants(plants: List<PlantDto>) = withContext(Dispatchers.IO) {
        if (plants.isEmpty()) return@withContext
        table.upsert(plants)
    }

    suspend fun fetchPlantsByUser(userId: String): List<PlantDto> = withContext(Dispatchers.IO) {
        table.select {
            filter {
                eq("user_id", userId)
                eq("is_deleted", false)
            }
        }.decodeList<PlantDto>()
    }

    suspend fun deletePlant(plantId: String) = withContext(Dispatchers.IO) {
        table.delete {
            filter {
                eq("plant_id", plantId)
            }
        }
    }
}
