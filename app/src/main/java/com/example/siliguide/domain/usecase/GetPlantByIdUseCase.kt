package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.PlantRepository
import javax.inject.Inject

class GetPlantByIdUseCase @Inject constructor(
    private val plantRepository: PlantRepository
) {
    suspend operator fun invoke(plantId: String): Plant {
        return plantRepository.getPlantById(plantId)
            ?: throw Exception("Plant not found")
    }
}