package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.repository.AnalysisRepository
import javax.inject.Inject

class GetAnalysisSessionByIdUseCase @Inject constructor(
    private val analysisRepository: AnalysisRepository
) {
    suspend operator fun invoke(sessionId: String): AnalysisSession? {
        return analysisRepository.getSessionById(sessionId)
    }
}