package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.PlantRepository
import java.util.UUID
import javax.inject.Inject

class AddPlantUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    // 1. Removed userId parameter
    suspend operator fun invoke(name: String, deviceId: String? = null) {

        // 2. Get standard Unix Epoch time in milliseconds (Long)
        val currentTimestamp = System.currentTimeMillis()

        val newPlant = Plant(
            plantId = UUID.randomUUID().toString(), // Generate unique ID
            name = name,
            dateCreated = currentTimestamp,         // 3. Pass the Long directly
            lastAnalyzedAt = null,
            deviceId = deviceId
        )
        repository.insertPlant(newPlant)
    }
}