package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.AnalysisSession

interface AnalysisRepository {
    suspend fun getSessionsByPlantId(plantId: String): List<AnalysisSession>
    suspend fun getSessionsByUser(userId: String): List<AnalysisSession>

    suspend fun getSessionById(sessionId: String): AnalysisSession?
    suspend fun getLatestSessionByPlantId(plantId: String): AnalysisSession?

    suspend fun saveSession(session: AnalysisSession)

    suspend fun markAnalysisSessionAsDeleted(sessionId: String)
    suspend fun markSessionSynced(sessionId: String)
    suspend fun deleteSession(sessionId: String)

    suspend fun getPartialSessionsByUser(userId: String): List<AnalysisSession>
    suspend fun getCompleteSessionsByUser(userId: String): List<AnalysisSession>
}