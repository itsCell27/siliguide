package com.example.siliguide.data.repository

import com.example.siliguide.data.local.SiliGuideDatabaseManager
import com.example.siliguide.data.local.mapper.toDomainModel
import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.repository.AnalysisRepository
import javax.inject.Inject

class AnalysisRepositoryImpl @Inject constructor(
    private val dbManager: SiliGuideDatabaseManager
) : AnalysisRepository {

    private val queries = dbManager.queries

    override suspend fun getSessionsByPlantId(plantId: String): List<AnalysisSession> {
        return queries.getSessionsByPlantId(plantId)
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun getSessionById(sessionId: String): AnalysisSession? {
        return queries.getSessionById(sessionId)
            .executeAsOneOrNull()
            ?.toDomainModel()
    }

    override suspend fun getLatestSessionByPlantId(plantId: String): AnalysisSession? {
        return queries.getLatestSessionByPlantId(plantId)
            .executeAsOneOrNull()
            ?.toDomainModel()
    }

    override suspend fun getAllSessions(): List<AnalysisSession> {
        return queries.getAllSessions()
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun saveSession(session: AnalysisSession) {
        queries.insertSession(
            session_id = session.sessionId,
            plant_id = session.plantId,
            device_id = session.deviceId,
            session_timestamp = session.sessionTimestamp,
            image_path = session.imagePath,
            predicted_class = session.predictedClass,
            confidence = session.confidence,
            air_temp = session.airTemp,
            light_intensity = session.lightIntensity,
            nitrogen = session.nitrogen?.toLong(),
            phosphorus = session.phosphorus?.toLong(),
            potassium = session.potassium?.toLong(),
            electrical_conductivity = session.electricalConductivity?.toLong(),
            soil_moisture = session.soilMoisture,
            soil_temp = session.soilTemp,
            soil_ph = session.soilPh,
            plant_condition = session.plantCondition,
            possible_causes = session.possibleCauses,
            symptoms = session.symptoms,
            recommended_actions = session.recommendedActions,
            is_partial = if (session.isPartial) 1L else 0L
        )
    }

    override suspend fun updateSession(
        sessionId: String,
        plantCondition: String?,
        possibleCauses: String?,
        symptoms: String?,
        recommendedActions: String?
    ) {
        queries.updateSession(
            plant_condition = plantCondition,
            possible_causes = possibleCauses,
            symptoms = symptoms,
            recommended_actions = recommendedActions,
            session_id = sessionId
        )
    }

    override suspend fun deleteSession(sessionId: String) {
        queries.deleteSession(sessionId)
    }

    override suspend fun getPartialSessionsByPlantId(plantId: String): List<AnalysisSession> {
        return queries.getPartialDiagnosisSessions(plantId)
            .executeAsList()
            .map { it.toDomainModel() }
    }

    override suspend fun getCompleteSessionsByPlantId(plantId: String): List<AnalysisSession> {
        return queries.getCompleteDiagnosisSessions(plantId)
            .executeAsList()
            .map { it.toDomainModel() }
    }
}