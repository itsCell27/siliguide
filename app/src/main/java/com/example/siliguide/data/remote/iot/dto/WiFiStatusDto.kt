package com.example.siliguide.data.remote.iot.dto

import kotlinx.serialization.Serializable

@Serializable
data class WiFiStatusDto (
    val connected: Boolean,
    val statusCode: Int,
    val ip: String
)