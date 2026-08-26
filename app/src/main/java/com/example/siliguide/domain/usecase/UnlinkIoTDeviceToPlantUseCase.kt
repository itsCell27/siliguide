package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.PlantRepository
import com.example.siliguide.domain.repository.SyncRepository
import javax.inject.Inject

class UnlinkIoTDeviceToPlantUseCase @Inject constructor(
    private val plantRepository: PlantRepository,
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(plantId: String) {
        val currentTimestamp = System.currentTimeMillis()
        plantRepository.unlinkDevice(updatedAt = currentTimestamp, plantId = plantId)
        syncRepository.enqueueOneTimeSync()
    }
}