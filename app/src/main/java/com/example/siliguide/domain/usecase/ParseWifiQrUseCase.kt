package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.WifiCredentials

class ParseWifiQrUseCase {

    operator fun invoke(raw: String): WifiCredentials? {
        if (!raw.startsWith("WIFI:")) return null

        return try {
            val parts = raw.removePrefix("WIFI:")
                .split(";")
                .mapNotNull {
                    val kv = it.split(":")
                    if (kv.size == 2) kv[0] to kv[1] else null
                }.toMap()

            WifiCredentials(
                ssid = parts["S"] ?: return null,
                password = parts["P"] ?: "",
                security = parts["T"] ?: "WPA"
            )
        } catch (e: Exception) {
            null
        }
    }
}