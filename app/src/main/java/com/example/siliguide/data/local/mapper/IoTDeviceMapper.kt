package com.example.siliguide.data.local.mapper

import com.example.siliguide.domain.model.IoTDevice
// FIX: Point this to your actual generated package
import com.example.siliguide.data.local.Iot_devices
import com.example.siliguide.domain.model.ConnectionMode

fun Iot_devices.toIotDomain(): IoTDevice {
    return IoTDevice(
        deviceId = device_id,
        userId = user_id, // Add this
        deviceName = device_name,
        pairingToken = pairing_token,
        isActive = is_active == 1L,
        lastKnownIp = last_known_ip ?: "192.168.4.1",
        connectionMode = ConnectionMode.valueOf(connection_mode),
        firmware = firmware,
        isSynced = is_synced == 1L, // Add this: Converts SQLite Long to Boolean
        dateAdded = date_added
    )
}