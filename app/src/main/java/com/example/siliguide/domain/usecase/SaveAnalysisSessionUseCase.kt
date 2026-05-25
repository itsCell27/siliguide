package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.AnalysisSession
import com.example.siliguide.domain.repository.AnalysisRepository
import javax.inject.Inject

class SaveAnalysisSessionUseCase @Inject constructor(
    private val analysisRepository: AnalysisRepository
){

    suspend operator fun invoke(session: AnalysisSession) {
        analysisRepository.saveSession(session)
    }
}