package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.Plant
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    // Context Scoped: Only stream records belonging to the active operator
    fun getPlantsByUser(userId: String): Flow<List<Plant>>

    suspend fun insertPlant(plant: Plant)
    suspend fun deletePlant(plantId: String)
    suspend fun markPlantAsDeletedById(updatedAt: Long, plantId: String)
    suspend fun getPlantById(plantId: String): Plant?

    suspend fun updatePlantName(plantId: String, updatedAt: Long, name: String)

    suspend fun linkDevice(deviceId: String, updatedAt: Long, plantId: String)
    suspend fun getLinkedPlantsByDeviceId(deviceId: String): Plant?
    suspend fun unlinkDevice(updatedAt: Long, plantId: String)

    suspend fun getPlantByDeviceId(deviceId: String): List<Plant>
}