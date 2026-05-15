package com.example.siliguide.domain.usecase

import android.util.Log
import com.example.siliguide.domain.model.WifiCredentials
import javax.inject.Inject

class ParseWifiQrUseCase @Inject constructor() {

    operator fun invoke(raw: String): WifiCredentials? {

        return try {

            // STANDARD WIFI QR FORMAT
            // WIFI:T:WPA;S:SiliGuide_IoT;P:12345678;;

            if (raw.startsWith("WIFI:")) {

                val cleaned = raw.removePrefix("WIFI:")
                val parts = cleaned.split(";")

                var ssid = ""
                var password = ""
                var security = "WPA"

                parts.forEach { part ->

                    when {

                        part.startsWith("S:") -> {
                            ssid = part.removePrefix("S:")
                        }

                        part.startsWith("P:") -> {
                            password = part.removePrefix("P:")
                        }

                        part.startsWith("T:") -> {
                            security = part.removePrefix("T:")
                        }
                    }
                }

                if (ssid.isBlank()) return null

                WifiCredentials(
                    ssid = ssid,
                    password = password,
                    security = security
                )

            } else {

                // LEGACY FORMAT SUPPORT
                // SSID=SiliGuide_0001;PASS=12345678

                val parts = raw.split(";")
                    .mapNotNull {

                        val index = it.indexOf("=")

                        if (index == -1) {
                            null
                        } else {
                            it.substring(0, index) to
                                    it.substring(index + 1)
                        }
                    }
                    .toMap()

                val ssid = parts["SSID"] ?: return null
                val password = parts["PASS"] ?: ""

                WifiCredentials(
                    ssid = ssid,
                    password = password,
                    security = "WPA"
                )
            }

        } catch (e: Exception) {
            Log.e("ParseWifiQrUseCase", "Error parsing QR code", e)
            null
        }
    }
}