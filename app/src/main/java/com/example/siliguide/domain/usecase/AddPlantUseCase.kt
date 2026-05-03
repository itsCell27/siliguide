package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.PlantRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

class AddPlantUseCase @Inject constructor(
    private val repository: PlantRepository
) {
    suspend operator fun invoke(name: String, userId: String, deviceId: String? = null) {
        // Use SimpleDateFormat which is supported on all Android versions
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val currentDate = formatter.format(Date())

        val newPlant = Plant(
            plantId = UUID.randomUUID().toString(), // Generate unique ID
            userId = userId,
            name = name,
            dateCreated = currentDate,
            lastAnalyzedAt = null,
            isSynced = false,
            deviceId = deviceId
        )
        repository.insertPlant(newPlant)
    }
}