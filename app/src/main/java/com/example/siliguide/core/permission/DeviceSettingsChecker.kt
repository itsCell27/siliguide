package com.example.siliguide.core.permission

import android.content.Context
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Reads OS toggles that a runtime permission cannot cover.
 *
 * Granting location permission is not enough to scan for Wi-Fi networks —
 * the Location service and the Wi-Fi radio must also be switched on. Kept in
 * core/ because it needs a Context and must never reach domain/.
 */
class DeviceSettingsChecker @Inject constructor(
    @param:ApplicationContext private val context: Context
) {

    fun isWifiEnabled(): Boolean {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        return wifiManager.isWifiEnabled
    }

    fun isLocationServiceEnabled(): Boolean {
        val locationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            locationManager.isLocationEnabled
        } else {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }
}