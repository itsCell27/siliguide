package com.example.siliguide.core.permission

sealed interface PermissionStatus {

    data object Granted : PermissionStatus

    data object RequestPermission : PermissionStatus

    data object ShowRationale : PermissionStatus

    data object PermanentlyDenied : PermissionStatus
}