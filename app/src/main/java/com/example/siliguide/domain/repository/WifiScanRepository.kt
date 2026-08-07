package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.WifiNetwork

/**
 * Scans for nearby Wi-Fi access points.
 *
 * The caller is responsible for making sure location permission, the Location
 * service and the Wi-Fi radio are all in place — this only reports what the
 * scan returned.
 */
interface WifiScanRepository {

    /** Returns nearby networks, strongest first, deduplicated by SSID. */
    suspend fun scanNetworks(): List<WifiNetwork>
}