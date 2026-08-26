package com.example.siliguide.domain.usecase

import android.content.Context
import android.util.Log
import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.data.local.sqldelight.SiliGuideDatabaseManager
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.SyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

class DeleteAccountUseCase @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val authRepository: AuthRepository,
    private val ioTRepository: IoTRepository,
    private val syncRepository: SyncRepository,
    private val dbManager: SiliGuideDatabaseManager,
    private val userPreferences: UserPreferences,
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "DeleteAccountUseCase"
        private const val RESET_ATTEMPT_TIMEOUT_MS = 3_000L
    }

    suspend operator fun invoke(): Result<Unit> = withContext(Dispatchers.IO) {
        val session = userPreferences.sessionFlow.first()
            ?: return@withContext Result.failure(IllegalStateException("No active user session found."))

        val userId = session.userId

        // 1. Best-effort ESP32 reset for all user devices (runs on both Guest & Authenticated accounts)
        resetAllUserDevicesBestEffort(userId)

        // 2. Guest-specific flow
        if (session.isGuest) {
            authRepository.deleteGuestSession()
            cleanLocalCachedFiles()
            return@withContext Result.success(Unit)
        }

        // 3. Authenticated cloud deletion: Call Supabase Edge Function 'delete-account'
        try {
            supabaseClient.functions.invoke("delete-account")
        } catch (e: Exception) {
            Log.e(TAG, "Edge Function 'delete-account' invocation failed", e)
            return@withContext Result.failure(
                IllegalStateException("Failed to delete account on server: ${e.message ?: "Network error"}", e)
            )
        }

        // 4. Local Cleanup after confirmed server deletion
        try {
            // Cancel background sync tasks
            syncRepository.cancelAllSync()

            // Delete SQLite user row (cascades to plants, iot_devices, analysis_sessions)
            dbManager.queries.deleteUser(userId)

            // Clear local cached analysis files
            cleanLocalCachedFiles()

            // Clear Supabase auth session
            try {
                supabaseClient.auth.clearSession()
            } catch (e: Exception) {
                Log.w(TAG, "Supabase clearSession failed", e)
            }

            // Clear local DataStore preferences session
            userPreferences.clearSession()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning local data after server account deletion", e)
            // Even if local cleanup encounters an issue, ensure session is cleared
            userPreferences.clearSession()
            Result.success(Unit)
        }
    }

    private suspend fun resetAllUserDevicesBestEffort(userId: String) {
        try {
            val userDevices = ioTRepository.getAllDevices(userId).first()
            for (device in userDevices) {
                withTimeoutOrNull(RESET_ATTEMPT_TIMEOUT_MS) {
                    try {
                        if (ioTRepository.isDeviceOnline(device.deviceId)) {
                            Log.d(TAG, "Device ${device.deviceId} is online. Performing physical reset...")
                            ioTRepository.resetDevice(device.deviceId)
                        } else {
                            Log.d(TAG, "Device ${device.deviceId} is offline or unreachable. Skipping reset.")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Physical reset failed for device ${device.deviceId}", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed during ESP32 reset loop for user $userId", e)
        }
    }

    private fun cleanLocalCachedFiles() {
        try {
            val analysisDir = File(context.filesDir, "analysis_images")
            if (analysisDir.exists()) {
                analysisDir.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clean local analysis images", e)
        }
    }
}
