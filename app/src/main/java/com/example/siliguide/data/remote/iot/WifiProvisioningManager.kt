package com.example.siliguide.data.remote.iot

import android.content.Context
import android.net.*
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build

class WifiProvisioningManager {

    fun connect(
        context: Context,
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            onFailure()
            return
        }

        val specifier = WifiNetworkSpecifier.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            .build()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .setNetworkSpecifier(specifier)
            .build()

        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivityManager.bindProcessToNetwork(network)
                onSuccess()
            }

            override fun onUnavailable() {
                onFailure()
            }
        }

        connectivityManager.requestNetwork(request, callback)
    }
}