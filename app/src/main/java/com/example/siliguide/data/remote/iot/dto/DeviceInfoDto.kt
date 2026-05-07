package com.example.siliguide.data.remote.iot.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfoDto(
    val device: String,
    val deviceId: String,
    val token: String? = null,
    val mode: String
)