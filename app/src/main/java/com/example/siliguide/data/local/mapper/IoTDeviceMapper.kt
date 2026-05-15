package com.example.siliguide.data.local.mapper

import com.example.siliguide.domain.model.IoTDevice
// FIX: Point this to your actual generated package
import com.example.siliguide.data.local.Iot_devices

fun Iot_devices.toIotDomain(): IoTDevice {
    return IoTDevice(
        deviceId = device_id,
        deviceName = device_name,
        deviceIdentifier = device_identifier,
        isActive = is_active == 1L, // Converts the SQLite Integer to Boolean
        lastKnownIp = last_known_ip ?: "192.168.4.1",
        connectionMode = connection_mode
    )
}