package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.PlantRepository
import java.util.UUID
import javax.inject.Inject

class AddPlantUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    // Re-added userId requirement to establish relational context safely
    suspend operator fun invoke(userId: String, name: String, deviceId: String? = null) {

        val currentTimestamp = System.currentTimeMillis()

        val newPlant = Plant(
            plantId = UUID.randomUUID().toString(),
            userId = userId, // Relates this plant to the active local account profile
            name = name,
            dateCreated = currentTimestamp,
            lastAnalyzedAt = null,
            isSynced = false, // Instantiated as un-synced until WorkManager fires online
            deviceId = deviceId
        )
        repository.insertPlant(newPlant)
    }
}