package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.AnalysisSession

interface AnalysisRepository {
    suspend fun getSessionsByPlantId(plantId: String): List<AnalysisSession>
    suspend fun getSessionById(sessionId: String): AnalysisSession?
    suspend fun getLatestSessionByPlantId(plantId: String): AnalysisSession?
    suspend fun getAllSessions(): List<AnalysisSession>
    suspend fun saveSession(session: AnalysisSession)
    suspend fun updateSession(
        sessionId: String,
        plantCondition: String?,
        possibleCauses: String?,
        characteristics: String?,
        recommendedActions: String?
    )
    suspend fun deleteSession(sessionId: String)
    suspend fun getPartialSessionsByPlantId(plantId: String): List<AnalysisSession>
    suspend fun getCompleteSessionsByPlantId(plantId: String): List<AnalysisSession>
}