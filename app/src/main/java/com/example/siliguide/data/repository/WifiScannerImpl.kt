package com.example.siliguide.data.repository

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.example.siliguide.domain.model.WifiNetwork
import com.example.siliguide.domain.model.WifiSignal
import com.example.siliguide.domain.repository.WifiScanRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Wi-Fi scanning through WifiManager.
 *
 * Android throttles startScan() to a handful of calls every couple of minutes,
 * so a refused scan falls back to the last cached results rather than showing
 * the user an empty list.
 */
class WifiScannerImpl @Inject constructor(
    @param:ApplicationContext private val context: Context
) : WifiScanRepository {

    private val wifiManager: WifiManager
        get() = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager

    override suspend fun scanNetworks(): List<WifiNetwork> {

        if (!wifiManager.isWifiEnabled) {
            return emptyList()
        }

        // Wait for a fresh scan, but never hang the wizard on it
        withTimeoutOrNull(SCAN_TIMEOUT_MS) { awaitScan() }

        return readResults()
            .mapNotNull { it.toWifiNetwork() }
            .groupBy { it.ssid }
            // One row per SSID — keep the strongest band
            .map { (_, duplicates) -> duplicates.maxBy { it.rssi } }
            .sortedByDescending { it.rssi }
    }

    /** Kicks off a scan and suspends until the system broadcasts results. */
    private suspend fun awaitScan(): Boolean =
        suspendCancellableCoroutine { continuation ->

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context?, intent: Intent?) {

                    runCatching { context.unregisterReceiver(this) }

                    if (continuation.isActive) {
                        continuation.resume(
                            intent?.getBooleanExtra(
                                WifiManager.EXTRA_RESULTS_UPDATED,
                                false
                            ) ?: false
                        )
                    }
                }
            }

            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED
            )

            continuation.invokeOnCancellation {
                runCatching { context.unregisterReceiver(receiver) }
            }

            @Suppress("DEPRECATION")
            val started = runCatching { wifiManager.startScan() }.getOrDefault(false)

            // Throttled or refused — fall straight through to cached results
            if (!started) {
                runCatching { context.unregisterReceiver(receiver) }

                if (continuation.isActive) {
                    continuation.resume(false)
                }
            }
        }

    /**
     * Reading scan results is permission-gated.
     *
     * The permission is checked up front, and SecurityException is still
     * caught because the grant can be revoked between the check and the call.
     */
    private fun readResults(): List<ScanResult> {

        if (!hasScanPermission()) {
            return emptyList()
        }

        return try {
            wifiManager.scanResults
        } catch (securityException: SecurityException) {
            emptyList()
        }
    }

    private fun hasScanPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun ScanResult.toWifiNetwork(): WifiNetwork? {

        @Suppress("DEPRECATION")
        val name = SSID

        if (name.isNullOrBlank()) {
            // Hidden network — nothing useful to show the user
            return null
        }

        return WifiNetwork(
            ssid = name,
            security = securityLabel(capabilities.orEmpty()),
            signal = signalOf(level),
            rssi = level
        )
    }

    private fun securityLabel(capabilities: String): String? = when {
        capabilities.contains("WPA3") || capabilities.contains("SAE") -> "WPA3"
        capabilities.contains("WPA2") || capabilities.contains("RSN") -> "WPA2"
        capabilities.contains("WPA") -> "WPA"
        capabilities.contains("WEP") -> "WEP"
        else -> null
    }

    private fun signalOf(rssi: Int): WifiSignal = when {
        rssi >= STRONG_RSSI -> WifiSignal.STRONG
        rssi >= MEDIUM_RSSI -> WifiSignal.MEDIUM
        else -> WifiSignal.WEAK
    }

    private companion object {
        const val SCAN_TIMEOUT_MS = 8_000L
        const val STRONG_RSSI = -60
        const val MEDIUM_RSSI = -70
    }
}