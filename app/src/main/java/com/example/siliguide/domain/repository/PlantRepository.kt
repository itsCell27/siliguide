package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.Plant
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    // We use Flow so the UI updates instantly when the database changes!
    fun getAllPlants(): Flow<List<Plant>>

    suspend fun insertPlant(plant: Plant)
    suspend fun deletePlant(plantId: String)

    suspend fun getPlantById(plantId: String): Plant?
    suspend fun updatePlant(plant: Plant)
    suspend fun linkDevice(plantId: String, deviceId: String)
    suspend fun unlinkDevice(plantId: String)
    suspend fun getPlantsByDeviceId(deviceId: String): List<Plant>
}