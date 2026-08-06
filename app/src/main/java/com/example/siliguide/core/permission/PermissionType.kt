package com.example.siliguide.core.permission

enum class PermissionType(
    val key: String
) {
    CAMERA("camera"),
    LOCATION("location"),
    NEARBY_WIFI("nearby_wifi"),
    BLUETOOTH("bluetooth"),
    NOTIFICATION("notification")
}

/**
 * One runtime permission paired with the [PermissionType] used to remember
 * whether it has already been requested.
 */
data class PermissionRequest(
    val permission: String,
    val type: PermissionType
)