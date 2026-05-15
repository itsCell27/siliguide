package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.repository.AnalysisRepository
import javax.inject.Inject

class GetAnalysisSessionsByPlantUseCase @Inject constructor(
    private val analysisRepository: AnalysisRepository
) {
    suspend operator fun invoke(plantId: String): List<AnalysisSession> {
        return analysisRepository.getSessionsByPlantId(plantId)
    }
}