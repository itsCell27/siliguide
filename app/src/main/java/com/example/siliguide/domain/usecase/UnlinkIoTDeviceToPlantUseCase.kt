package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.PlantRepository
import com.google.android.play.core.assetpacks.cu
import javax.inject.Inject

class UnlinkIoTDeviceToPlantUseCase @Inject constructor(
    private val plantRepository: PlantRepository
) {
    suspend operator fun invoke(plantId: String) {
        val currentTimestamp = System.currentTimeMillis()
        plantRepository.unlinkDevice(updatedAt = currentTimestamp, plantId = plantId)
    }
}