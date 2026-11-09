package com.example.siliguide.data.remote.iot.dto

import kotlinx.serialization.Serializable

@Serializable
data class ClaimResponseDto(
    val status: String? = null,
    val error: String? = null
)