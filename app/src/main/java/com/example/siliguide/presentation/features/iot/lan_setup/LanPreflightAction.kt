package com.example.siliguide.presentation.features.iot.lan_setup

/**
 * What the UI must do next when a LAN preflight check fails.
 *
 * The wizard cannot scan for Wi-Fi networks unless all three conditions hold:
 * location permission granted, location services on, Wi-Fi radio on.
 */
enum class LanPreflightAction {

    /** Ask the OS for the location / nearby-devices permissions. */
    REQUEST_PERMISSION,

    /** Permission is permanently denied — only app settings can fix it. */
    OPEN_APP_SETTINGS,

    /** Location services are off — send the user to location settings. */
    OPEN_LOCATION_SETTINGS,

    /** Wi-Fi radio is off — send the user to Wi-Fi settings. */
    OPEN_WIFI_SETTINGS
}

/** Outcome of \[com.example.siliguide.presentation.features.iot.IoTViewModel.checkLanPreflight]. */
sealed interface LanPreflightResult {

    /** Everything required for a Wi-Fi scan is in place. */
    data object Ready : LanPreflightResult

    /** Something is missing. [title] and [message] are user-facing. */
    data class Blocked(
        val title: String,
        val message: String,
        val action: LanPreflightAction
    ) : LanPreflightResult
}