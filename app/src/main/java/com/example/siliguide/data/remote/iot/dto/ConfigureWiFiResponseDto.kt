package com.example.siliguide.data.remote.iot.dto

import kotlinx.serialization.Serializable

/**
 * Reply to CONFIGURE / POST /configure.
 *
 * Firmware answers {"status":"connecting"} on accept, or one of
 * {"error":"unauthorized_bluetooth_access" | "decryption_failed" |
 * "invalid_json_format"} on reject.
 */
@Serializable
data class ConfigureWiFiResponseDto(
    val status: String? = null,
    val error: String? = null
)