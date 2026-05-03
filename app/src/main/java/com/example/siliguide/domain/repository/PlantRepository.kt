package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.Plant
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    // We use Flow so the UI updates instantly when the database changes!
    fun getPlantsByUser(userId: String): Flow<List<Plant>>

    suspend fun insertPlant(plant: Plant)
    suspend fun deletePlant(plantId: String)
}