package com.example.siliguide.data.remote.iot.dto

import kotlinx.serialization.Serializable

/**
 * Defaults keep parsing alive if a firmware build drops or renames a field.
 *
 * - statusCode mirrors Arduino's wl_status_t; -1 means "not reported".
 * - ssid is the network the device is on (or trying). Blank on old firmware.
 * - result / reason / resultSsid describe the last CONFIGURE attempt
 *   (firmware 1.1.0+). Blank on old firmware.
 *     result: "idle" | "trying" | "ok" | "failed"
 *     reason: "wrong_password" | "not_found" | "timeout" | "cancelled"
 */
@Serializable
data class WiFiStatusDto (
    val connected: Boolean = false,
    val statusCode: Int = -1,
    val ip: String = "",
    val status: String = "",
    val ssid: String = "",
    val result: String = "",
    val reason: String = "",
    val resultSsid: String = ""
)
