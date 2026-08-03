package com.example.siliguide.data.remote.iot.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfoDto(
    val device: String,
    val deviceId: String,
    val claimed: Boolean,
    val token: String? = null,
    val mode: String
)