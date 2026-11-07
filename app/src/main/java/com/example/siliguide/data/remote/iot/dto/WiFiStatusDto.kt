package com.example.siliguide.data.remote.iot.dto

import kotlinx.serialization.Serializable

/**
 * Defaults keep parsing alive if a firmware build drops or renames a field.
 * statusCode mirrors Arduino's wl_status_t; -1 means "not reported".
 */
@Serializable
data class WiFiStatusDto (
    val connected: Boolean = false,
    val statusCode: Int = -1,
    val ip: String = ""
)
