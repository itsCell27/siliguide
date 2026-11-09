package com.example.siliguide.domain.usecase

import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.data.local.storage.InternalImageStorage
import com.example.siliguide.domain.repository.AnalysisRepository
import com.example.siliguide.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteAnalysisSessionUseCase @Inject constructor(
    private val analysisRepository: AnalysisRepository,
    private val userPreferences: UserPreferences,
    private val syncRepository: SyncRepository,
    private val internalImageStorage: InternalImageStorage
) {
    suspend operator fun invoke(sessionId: String): Result<Unit> {
        val session = userPreferences.sessionFlow.first()
        val isGuest = session == null || session.isGuest

        return try {
            val currentTimestamp = System.currentTimeMillis()
            if (isGuest) {
                // Guest: hard delete locally immediately
                val localSession = analysisRepository.getSessionById(sessionId)
                analysisRepository.deleteSession(sessionId)
                localSession?.imagePath?.let { path ->
                    internalImageStorage.deleteImage(path)
                }
            } else {
                // Authenticated: mark deleted locally (instant offline UI update)
                analysisRepository.markAnalysisSessionAsDeleted(currentTimestamp, sessionId)
                syncRepository.enqueueOneTimeSync()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}