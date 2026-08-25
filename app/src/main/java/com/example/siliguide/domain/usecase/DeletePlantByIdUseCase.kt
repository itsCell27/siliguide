package com.example.siliguide.domain.usecase

import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.data.local.storage.InternalImageStorage
import com.example.siliguide.domain.repository.AnalysisRepository
import com.example.siliguide.domain.repository.PlantRepository
import com.example.siliguide.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeletePlantByIdUseCase @Inject constructor(
    private val plantRepository: PlantRepository,
    private val analysisRepository: AnalysisRepository,
    private val userPreferences: UserPreferences,
    private val syncRepository: SyncRepository,
    private val internalImageStorage: InternalImageStorage
) {
    suspend operator fun invoke(plantId: String): Result<Unit> {
        val session = userPreferences.sessionFlow.first()
        val isGuest = session == null || session.isGuest

        return try {
            val currentTimestamp = System.currentTimeMillis()
            if (isGuest) {
                // Guest: hard delete locally immediately
                val associatedSessions = analysisRepository.getSessionsByPlantId(plantId)
                plantRepository.deletePlant(plantId)
                associatedSessions.forEach { s ->
                    internalImageStorage.deleteImage(s.imagePath)
                }
            } else {
                // Authenticated: mark deleted locally (instant offline UI update)
                val associatedSessions = analysisRepository.getSessionsByPlantId(plantId)
                associatedSessions.forEach { s ->
                    analysisRepository.markAnalysisSessionAsDeleted(currentTimestamp, s.sessionId)
                }
                plantRepository.markPlantAsDeletedById(currentTimestamp, plantId)
                syncRepository.enqueueOneTimeSync()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}