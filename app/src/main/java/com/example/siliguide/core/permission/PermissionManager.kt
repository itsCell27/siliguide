package com.example.siliguide.core.permission

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.siliguide.data.local.PermissionPreferences
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class PermissionManager @Inject constructor(
    private val permissionPreferences: PermissionPreferences
) {

    suspend fun getPermissionStatus(
        context: Context,
        permission: String,
        type: PermissionType
    ): PermissionStatus {

        val granted =
            ContextCompat.checkSelfPermission(
                context,
                permission
            ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            return PermissionStatus.Granted
        }

        val activity = context.findActivity()

        val requestedBefore =
            permissionPreferences
                .wasRequested(type)
                .first()

        if (
            activity != null &&
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permission
            )
        ) {
            return PermissionStatus.ShowRationale
        }

        if (requestedBefore) {
            return PermissionStatus.PermanentlyDenied
        }

        return PermissionStatus.RequestPermission
    }

    /**
     * Status for a group of permissions that must all be granted together,
     * such as location plus nearby-devices for a Wi-Fi scan.
     *
     * Resolves to the most blocking status in the group, so the caller always
     * knows the strongest action it has to take.
     */
    suspend fun getCombinedPermissionStatus(
        context: Context,
        requests: List<PermissionRequest>
    ): PermissionStatus {

        if (requests.isEmpty()) {
            return PermissionStatus.Granted
        }

        val statuses = requests.map { request ->
            getPermissionStatus(
                context = context,
                permission = request.permission,
                type = request.type
            )
        }

        return when {

            statuses.all { it is PermissionStatus.Granted } ->
                PermissionStatus.Granted

            statuses.any { it is PermissionStatus.PermanentlyDenied } ->
                PermissionStatus.PermanentlyDenied

            statuses.any { it is PermissionStatus.ShowRationale } ->
                PermissionStatus.ShowRationale

            else -> PermissionStatus.RequestPermission
        }
    }

    suspend fun markRequested(
        permission: PermissionType
    ) {
        permissionPreferences.setRequested(permission)
    }

    /** Marks every permission in a group as requested. */
    suspend fun markRequested(
        requests: List<PermissionRequest>
    ) {
        requests.forEach { request ->
            permissionPreferences.setRequested(request.type)
        }
    }

    suspend fun clearRequested(
        permission: PermissionType
    ) {
        permissionPreferences.clearRequested(permission)
    }
}

private tailrec fun Context.findActivity(): Activity? {

    return when (this) {

        is Activity -> this

        is ContextWrapper -> baseContext.findActivity()

        else -> null
    }
}