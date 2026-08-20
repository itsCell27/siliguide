package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.PlantRepository
import javax.inject.Inject

class GetLinkedPlantByDeviceIdUseCase @Inject constructor(
    private val plantRepository: PlantRepository
) {
    suspend operator fun invoke(deviceId: String): Plant? {
        return plantRepository.getLinkedPlantsByDeviceId(deviceId)
    }
}