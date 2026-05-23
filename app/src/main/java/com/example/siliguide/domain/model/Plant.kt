package com.example.siliguide.domain.model

data class Plant(
    val plantId: String,
    val userId: String,          // REQUIRED: For multi-user workspace isolation
    val name: String,
    val dateCreated: Long,
    val lastAnalyzedAt: Long?,
    val isSynced: Boolean = false, // REQUIRED: For WorkManager offline sync tracking
    val deviceId: String?
)