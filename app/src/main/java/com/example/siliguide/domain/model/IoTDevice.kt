package com.example.siliguide.domain.model

data class IoTDevice(
    val deviceId: String,
    val userId: String,
    val hardwareMac: String,
    val deviceName: String,
    val pairingToken: String,
    val isActive: Boolean,
    val lastKnownIp: String,
    val connectionMode: ConnectionMode,
    val firmware: String? = null,

    // Synchronization state
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,

    val dateAdded: Long,
    val updatedAt: Long,
)