package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.PlantRepository
import javax.inject.Inject

class DeletePlantByIdUseCase @Inject constructor(
    private val plantRepository: PlantRepository
) {
    suspend operator fun invoke(plantId: String) {
        plantRepository.deletePlant(plantId)
    }
}