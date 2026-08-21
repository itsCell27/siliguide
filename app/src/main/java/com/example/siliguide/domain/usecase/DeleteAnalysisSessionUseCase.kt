package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.AnalysisRepository
import javax.inject.Inject

class DeleteAnalysisSessionUseCase @Inject constructor(
    private val analysisRepository: AnalysisRepository
) {
    // temporary since no suspabase integration yet
    private val currentTimestamp = System.currentTimeMillis()
    suspend operator fun invoke(sessionId: String) {
        analysisRepository.markAnalysisSessionAsDeleted(updatedAt = currentTimestamp, sessionId = sessionId)
    }
}