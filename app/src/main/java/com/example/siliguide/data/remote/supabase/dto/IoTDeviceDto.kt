package com.example.siliguide.data.remote.supabase.dto

import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.model.IoTDevice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class IoTDeviceDto(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("hardware_mac")
    val hardwareMac: String,
    @SerialName("device_name")
    val deviceName: String,
    @SerialName("pairing_token")
    val pairingToken: String,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("last_known_ip")
    val lastKnownIp: String? = null,
    @SerialName("connection_mode")
    val connectionMode: String,
    @SerialName("firmware")
    val firmware: String? = null,
    @SerialName("is_deleted")
    val isDeleted: Boolean = false,
    @SerialName("date_added")
    val dateAdded: String,
    @SerialName("updated_at")
    val updatedAt: String,
    @SerialName("last_seen_at")
    val lastSeenAt: String? = null,
    @SerialName("last_soil_moisture")
    val lastSoilMoisture: Double? = null,
    @SerialName("last_soil_temp")
    val lastSoilTemp: Double? = null,
    @SerialName("last_soil_ph")
    val lastSoilPh: Double? = null,
    @SerialName("last_nitrogen")
    val lastNitrogen: Int? = null,
    @SerialName("last_phosphorus")
    val lastPhosphorus: Int? = null,
    @SerialName("last_potassium")
    val lastPotassium: Int? = null,
    @SerialName("last_electrical_conductivity")
    val lastElectricalConductivity: Int? = null,
    @SerialName("last_air_temp")
    val lastAirTemp: Double? = null,
    @SerialName("last_light_intensity")
    val lastLightIntensity: Int? = null
)

fun IoTDevice.toDto(rawPairingToken: String): IoTDeviceDto {
    return IoTDeviceDto(
        deviceId = this.deviceId,
        userId = this.userId,
        hardwareMac = this.hardwareMac,
        deviceName = this.deviceName,
        pairingToken = rawPairingToken,
        isActive = this.isActive,
        lastKnownIp = this.lastKnownIp,
        connectionMode = this.connectionMode.name,
        firmware = this.firmware,
        isDeleted = this.isDeleted,
        dateAdded = Instant.ofEpochMilli(this.dateAdded).toString(),
        updatedAt = Instant.ofEpochMilli(this.updatedAt).toString()
    )
}

fun IoTDeviceDto.toDomain(localEncryptedPairingToken: String): IoTDevice {
    val dateAddedMillis = runCatching { Instant.parse(this.dateAdded).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())
    val updatedAtMillis = runCatching { Instant.parse(this.updatedAt).toEpochMilli() }
        .getOrDefault(System.currentTimeMillis())

    val mode = runCatching { ConnectionMode.valueOf(this.connectionMode) }
        .getOrDefault(ConnectionMode.AP)

    return IoTDevice(
        deviceId = this.deviceId,
        userId = this.userId,
        hardwareMac = this.hardwareMac,
        deviceName = this.deviceName,
        pairingToken = localEncryptedPairingToken,
        isActive = this.isActive,
        lastKnownIp = this.lastKnownIp ?: "192.168.4.1",
        connectionMode = mode,
        firmware = this.firmware,
        isDeleted = this.isDeleted,
        isSynced = true,
        dateAdded = dateAddedMillis,
        updatedAt = updatedAtMillis
    )
}
