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

    suspend fun markRequested(
        permission: PermissionType
    ) {
        permissionPreferences.setRequested(permission)
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