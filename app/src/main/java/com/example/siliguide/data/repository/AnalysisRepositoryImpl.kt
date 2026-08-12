package com.example.siliguide.data.repository

import com.example.siliguide.data.local.mapper.JsonMapper
import com.example.siliguide.data.local.sqldelight.SiliGuideDatabaseManager
import com.example.siliguide.data.local.mapper.toDomainModel
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.repository.AnalysisRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

class AnalysisRepositoryImpl @Inject constructor(
    private val dbManager: SiliGuideDatabaseManager
) : AnalysisRepository {

    private val queries = dbManager.queries

    override suspend fun saveSession(session: AnalysisSession) {
        withContext(Dispatchers.IO) {
            queries.insertSession(
                session_id = session.sessionId,
                plant_id = session.plantId,
                device_id = session.deviceId,
                user_id = session.userId,
                session_timestamp = session.sessionTimestamp,
                image_path = session.imagePath,

                predicted_class = session.predictedClass,
                confidence = session.confidence,

                segmentation_result = JsonMapper.encodeSegmentationResult(session.segmentationResult),

                plant_condition = session.plantCondition,
                plant_condition_description = session.plantConditionDescription,
                possible_causes = session.possibleCauses,
                characteristics = session.characteristics,
                recommended_actions = session.recommendedActions,
                is_partial = if (session.isPartial) 1L else 0L,
                soil_moisture = session.soilMoisture,
                soil_ph = session.soilPh,
                soil_temp = session.soilTemp,
                electrical_conductivity = session.electricalConductivity?.toLong(),
                nitrogen = session.nitrogen?.toLong(),
                phosphorus = session.phosphorus?.toLong(),
                potassium = session.potassium?.toLong(),
                light_intensity = session.lightIntensity?.toLong(),
                air_temp = session.airTemp
            )
        }
    }

    override suspend fun getSessionsByPlantId(plantId: String): List<AnalysisSession> {
        return withContext(Dispatchers.IO) {
            queries.getSessionsByPlant(plantId)
                .executeAsList()
                .map { it.toDomainModel() }
        }
    }

    override suspend fun getSessionsByUser(userId: String): List<AnalysisSession> {
        return withContext(Dispatchers.IO) {
            queries.getSessionsByUser(userId)
                .executeAsList()
                .map { it.toDomainModel() }
        }
    }

    override suspend fun getSessionById(sessionId: String): AnalysisSession? {
        return withContext(Dispatchers.IO) {
            queries.getSessionById(sessionId)
                .executeAsOneOrNull()
                ?.toDomainModel()
        }
    }

    override suspend fun getLatestSessionByPlantId(plantId: String): AnalysisSession? {
        return withContext(Dispatchers.IO) {
            queries.getLatestSessionForPlant(plantId)
                .executeAsOneOrNull()
                ?.toDomainModel()
        }
    }

    override suspend fun getPartialSessionsByUser(userId: String): List<AnalysisSession> {
        return withContext(Dispatchers.IO) {
            queries.getPartialAnalysisSessionsByUser(userId)
                .executeAsList()
                .map { it.toDomainModel() }
        }
    }

    override suspend fun getCompleteSessionsByUser(userId: String): List<AnalysisSession> {
        return withContext(Dispatchers.IO) {
            queries.getCompleteAnalysisSessionsByUser(userId)
                .executeAsList()
                .map { it.toDomainModel() }
        }
    }

    override suspend fun markSessionSynced(sessionId: String) {
        withContext(Dispatchers.IO) {
            queries.markSessionSynced(sessionId)
        }
    }

    override suspend fun deleteSession(sessionId: String) {
        withContext(Dispatchers.IO) {
            queries.deleteSession(sessionId)
        }
    }
}