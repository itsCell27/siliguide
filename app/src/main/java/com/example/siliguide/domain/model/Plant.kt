package com.example.siliguide.domain.model

data class Plant(
    val plantId: String,
    val userId: String,
    val name: String,
    val dateCreated: String,
    val lastAnalyzedAt: String?,
    val isSynced: Boolean,
    val deviceId: String?
)