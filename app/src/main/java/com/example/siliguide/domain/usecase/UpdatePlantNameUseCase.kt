package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.PlantRepository
import javax.inject.Inject

class UpdatePlantNameUseCase @Inject constructor(
    private val plantRepository: PlantRepository
) {
    suspend operator fun invoke(plantId: String, newName: String) {
        plantRepository.updatePlantName(plantId, newName)
    }
}