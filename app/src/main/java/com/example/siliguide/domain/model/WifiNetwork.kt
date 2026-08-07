package com.example.siliguide.domain.model

/** Rough signal bucket, derived from RSSI. */
enum class WifiSignal { STRONG, MEDIUM, WEAK }

/**
 * One access point returned by a Wi-Fi scan.
 *
 * Pure Kotlin — no android.net.wifi types leak into domain.
 */
data class WifiNetwork(
    val ssid: String,
    /** "WPA2", "WPA3", "WEP"… or null for an open network. */
    val security: String? = null,
    val signal: WifiSignal = WifiSignal.STRONG,
    val rssi: Int = 0
) {
    val isOpen: Boolean get() = security == null
}