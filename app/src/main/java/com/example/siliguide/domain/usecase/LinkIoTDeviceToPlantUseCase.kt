package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.PlantRepository
import javax.inject.Inject

class LinkIoTDeviceToPlantUseCase @Inject constructor(
    private val plantRepository: PlantRepository
) {
    suspend operator fun invoke(plantId: String, deviceId: String) {
        plantRepository.linkDevice(plantId, deviceId)
    }
}