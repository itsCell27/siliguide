package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.PlantRepository
import javax.inject.Inject

class UnlinkIoTDeviceToPlantUseCase @Inject constructor(
    private val plantRepository: PlantRepository
) {
    suspend operator fun invoke(plantId: String) {
        plantRepository.unlinkDevice(plantId)
    }
}