package com.example.siliguide.domain.model

data class Plant(
    val plantId: String,
    val name: String,
    val dateCreated: Long,
    val lastAnalyzedAt: Long?,
    val deviceId: String?
)