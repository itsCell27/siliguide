package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.PlantRepository
import com.example.siliguide.domain.repository.SyncRepository
import javax.inject.Inject

class LinkIoTDeviceToPlantUseCase @Inject constructor(
    private val plantRepository: PlantRepository,
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(deviceId: String, plantId: String) {
        val currentTimestamp = System.currentTimeMillis()
        plantRepository.linkDevice(
            deviceId = deviceId,
            updatedAt = currentTimestamp,
            plantId = plantId
        )
        syncRepository.enqueueOneTimeSync()
    }
}