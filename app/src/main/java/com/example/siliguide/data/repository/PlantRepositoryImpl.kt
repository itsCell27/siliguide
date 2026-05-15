package com.example.siliguide.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.example.siliguide.data.local.SiliGuideDatabaseManager // Inject Manager
import com.example.siliguide.data.local.mapper.toDomainModel
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlantRepositoryImpl @Inject constructor(
    private val dbManager: SiliGuideDatabaseManager // Correct way
) : PlantRepository {

    // Access the shared queries from the manager
    private val queries = dbManager.queries

    override fun getAllPlants(): Flow<List<Plant>> {
        return queries.getAllPlants()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { sqlEntities ->
                sqlEntities.map { it.toDomainModel() }
            }
    }

    override suspend fun insertPlant(plant: Plant) {
        queries.insertPlant(
            plant_id = plant.plantId,
            plant_name = plant.name,
            date_created = plant.dateCreated,
            last_analyzed_at = plant.lastAnalyzedAt,
            device_id = plant.deviceId
        )
    }

    override suspend fun deletePlant(plantId: String) {
        queries.deletePlant(plantId)
    }

    override suspend fun getPlantById(plantId: String): Plant? {
        return queries.getPlantById(plantId)
            .executeAsOneOrNull()
            ?.toDomainModel()
    }

    override suspend fun updatePlant(plant: Plant) {
        queries.updatePlant(
            plant_name = plant.name,
            last_analyzed_at = plant.lastAnalyzedAt,
            device_id = plant.deviceId,
            plant_id = plant.plantId
        )
    }

    override suspend fun linkDevice(plantId: String, deviceId: String) {
        queries.updatePlantDevice(deviceId, plantId)
    }

    override suspend fun unlinkDevice(plantId: String) {
        queries.unlinkPlantDevice(plantId)
    }

    override suspend fun getPlantsByDeviceId(deviceId: String): List<Plant> {
        return queries.getPlantsByDeviceId(deviceId)
            .executeAsList()
            .map { it.toDomainModel() }
    }
}