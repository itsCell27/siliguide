package com.example.siliguide.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import com.example.siliguide.data.local.SiliGuideDatabase
import com.example.siliguide.data.local.mapper.toDomainModel
import com.example.siliguide.data.local.mapper.toSyncInt
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.PlantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PlantRepositoryImpl @Inject constructor(
    driver: SqlDriver
) : PlantRepository {

    private val database = SiliGuideDatabase(driver)
    private val queries = database.siliGuideDatabaseQueries

    override fun getPlantsByUser(userId: String): Flow<List<Plant>> {
        return queries.getPlantsByUser(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { sqlEntities ->
                // Using your new mapper makes this one line of code!
                sqlEntities.map { it.toDomainModel() }
            }
    }

    override suspend fun insertPlant(plant: Plant) {
        queries.insertPlant(
            plant_id = plant.plantId,
            user_id = plant.userId,
            plant_name = plant.name,
            date_created = plant.dateCreated,
            last_analyzed_at = plant.lastAnalyzedAt,
            is_synced = plant.toSyncInt(), // Using the helper!
            device_id = plant.deviceId
        )
    }

    override suspend fun deletePlant(plantId: String) {
        queries.deletePlant(plantId)
    }
}