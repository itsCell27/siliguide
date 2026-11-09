package com.example.siliguide.domain.usecase

import android.util.Log
import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.repository.IoTRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class GuestLogOutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val ioTRepository: IoTRepository,
    private val userPreferences: UserPreferences
) {
    companion object {
        private const val TAG = "GuestLogOutUseCase"
        private const val RESET_ATTEMPT_TIMEOUT_MS = 3_000L
    }

    suspend operator fun invoke() {
        val session = userPreferences.sessionFlow.first()
        if (session != null && session.isGuest) {
            try {
                val userDevices = ioTRepository.getAllDevices(session.userId).first()
                for (device in userDevices) {
                    withTimeoutOrNull(RESET_ATTEMPT_TIMEOUT_MS) {
                        try {
                            if (ioTRepository.isDeviceOnline(device.deviceId)) {
                                Log.d(TAG, "Guest device ${device.deviceId} is online. Resetting...")
                                ioTRepository.resetDevice(device.deviceId)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Physical reset failed for guest device ${device.deviceId}", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed during guest ESP32 reset loop", e)
            }
        }
        authRepository.deleteGuestSession()
    }
}