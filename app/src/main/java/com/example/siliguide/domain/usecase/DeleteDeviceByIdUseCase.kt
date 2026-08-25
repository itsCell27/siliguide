package com.example.siliguide.domain.usecase

import android.util.Log
import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.PlantRepository
import com.example.siliguide.domain.repository.SyncRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

class DeleteDeviceByIdUseCase @Inject constructor(
    private val ioTRepository: IoTRepository,
    private val plantRepository: PlantRepository,
    private val userPreferences: UserPreferences,
    private val syncRepository: SyncRepository
) {
    private companion object {
        const val TAG = "DeleteDeviceByIdUseCase"
        const val RESET_ATTEMPT_TIMEOUT_MS = 3_000L
    }

    suspend operator fun invoke(deviceId: String): Result<Unit> {
        val session = userPreferences.sessionFlow.first()
        val isGuest = session == null || session.isGuest
        val now = System.currentTimeMillis()

        // 1. Validate device exists — the only point getDeviceById can still
        // see the row, before any local deletion below.
        val device = ioTRepository.getDeviceById(deviceId)
            ?: return Result.failure(
                IllegalArgumentException("IoT device not found.")
            )

        // 2. Unlink linked plant
        val linkedPlant: Plant? = plantRepository.getLinkedPlantsByDeviceId(deviceId)
        if (linkedPlant != null) {
            plantRepository.unlinkDevice(updatedAt = now, plantId = linkedPlant.plantId)
        }

        // 3. Best-effort physical reset — must run before local deletion,
        // or the row can never be found again either way. Bounded by a
        // timeout so an unreachable device can't stall the delete itself.
        val resetSucceeded = withTimeoutOrNull(RESET_ATTEMPT_TIMEOUT_MS) {
            try {
                if (ioTRepository.isDeviceOnline(deviceId)) {
                    ioTRepository.resetDevice(deviceId)
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.w(TAG, "Physical reset failed for $deviceId", e)
                false
            }
        } ?: false

        // 4. Offline-First Deletion — happens regardless of step 3's outcome.
        if (isGuest) {
            ioTRepository.deleteDevice(deviceId)
        } else {
            ioTRepository.markDeviceDeleted(updatedAt = now, deviceId = deviceId)
            syncRepository.enqueueOneTimeSync()
        }

        return if (resetSucceeded) {
            Result.success(Unit)
        } else {
            Result.failure(
                IllegalStateException(
                    "Device was removed from the account, " +
                            "but the ESP32 is currently unreachable. " +
                            "The device may require a physical reset."
                )
            )
        }
    }
}