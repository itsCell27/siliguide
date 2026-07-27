package com.example.siliguide.domain.model

data class IoTDevice(
    val deviceId: String,
    val userId: String,
    val deviceName: String,
    val deviceIdentifier: String, // Storing the pairing token here
    val isActive: Boolean,
    val lastKnownIp: String,      // Will be 192.168.4.1 for AP mode
    val connectionMode: ConnectionMode,    // "AP"
    val isSynced: Boolean = false,
    val dateAdded: Long
)