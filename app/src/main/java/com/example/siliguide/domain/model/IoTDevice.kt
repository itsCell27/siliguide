package com.example.siliguide.domain.model

data class IoTDevice(
    val deviceId: String,
    val userId: String,
    val deviceName: String,
    val pairingToken: String,
    val isActive: Boolean,
    val lastKnownIp: String,      // Will be 192.168.4.1 for AP mode
    val connectionMode: ConnectionMode,
    val firmware: String? = null,
    val isSynced: Boolean = false,
    val dateAdded: Long
)