package com.example.siliguide.data.remote.supabase.dto

import com.example.siliguide.domain.model.Plant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class PlantDto(
    @SerialName("plant_id")
    val plantId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("plant_name")
    val plantName: String,
    @SerialName("date_created")
    val dateCreated: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("last_analyzed_at")
    val lastAnalyzedAt: String? = null,
    @SerialName("device_id")
    val deviceId: String? = null,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false
)

fun Plant.toDto(): PlantDto {
    return PlantDto(
        plantId = this.plantId,
        userId = this.userId,
        plantName = this.name,
        dateCreated = Instant.ofEpochMilli(this.dateCreated).toString(),
        updatedAt = Instant.ofEpochMilli(this.updatedAt).toString(),
        lastAnalyzedAt = this.lastAnalyzedAt?.let { Instant.ofEpochMilli(it).toString() },
        deviceId = this.deviceId,
        isDeleted = this.isDeleted
    )
}

fun PlantDto.toDomain(): Plant {
    val createdMillis = runCatching { Instant.parse(this.dateCreated).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())
    val updatedMillis = runCatching { Instant.parse(this.updatedAt).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())
    val lastAnalyzedMillis = this.lastAnalyzedAt?.let {
        runCatching { Instant.parse(it).toEpochMilli() }.getOrNull()
    }

    return Plant(
        plantId = this.plantId,
        userId = this.userId,
        name = this.plantName,
        dateCreated = createdMillis,
        updatedAt = updatedMillis,
        lastAnalyzedAt = lastAnalyzedMillis,
        deviceId = this.deviceId,
        isDeleted = this.isDeleted,
        isSynced = true
    )
}
