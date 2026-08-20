package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.Plant
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.PlantRepository
import javax.inject.Inject

class DeleteDeviceByIdUseCase @Inject constructor(
    private val ioTRepository: IoTRepository,
    private val plantRepository: PlantRepository
) {

    suspend operator fun invoke(
        deviceId: String
    ): Result<Unit> {

        // ---------------------------------------------------------
        // 1. Get the device
        // ---------------------------------------------------------
        val device = ioTRepository.getDeviceById(deviceId)
            ?: return Result.failure(
                IllegalArgumentException("IoT device not found.")
            )
        // Get linked plant if any
        val linkedPlant: Plant? = plantRepository.getLinkedPlantsByDeviceId(deviceId)

        // Unlink the plant
        if (linkedPlant != null) {
            plantRepository.unlinkDevice(
                updatedAt = System.currentTimeMillis(),
                plantId = linkedPlant.plantId
            )
        }

        // ---------------------------------------------------------
        // 2. Mark local deletion
        // ---------------------------------------------------------
        //
        // DO NOT reset the ESP32 yet.
        //
        // The local database now remembers that the user requested
        // deletion, even if the app loses internet immediately.
        //
        ioTRepository.markDeviceDeleted(
            updatedAt = System.currentTimeMillis(),
            deviceId = deviceId
        )

        // ---------------------------------------------------------
        // 3. TEMPORARY SUPABASE SIMULATION
        // ---------------------------------------------------------
        //
        // Later this will be replaced with the actual Supabase
        // deletion/synchronization operation.
        //
        val cloudDeletionSuccessful = true

        if (!cloudDeletionSuccessful) {
            // IMPORTANT:
            // Do NOT reset the ESP32.
            //
            // Keep the local tombstone so the deletion can be retried.
            return Result.failure(
                IllegalStateException(
                    "Device deletion is pending synchronization."
                )
            )
        }

        // ---------------------------------------------------------
        // 4. Supabase deletion succeeded.
        // ---------------------------------------------------------
        //
        // NOW we are allowed to reset the physical ESP32.
        //
        val deviceReachable = ioTRepository.isDeviceOnline(deviceId)

        if (!deviceReachable) {

            // Cloud ownership has already been removed.
            //
            // The ESP32 is unreachable, so the app cannot reset it.
            //
            // DO NOT call resetDevice().
            //
            // The physical device may need to be manually reset later.
            return Result.failure(
                IllegalStateException(
                    "Device was removed from the account, " +
                            "but the ESP32 is currently unreachable. " +
                            "The device may require a physical reset."
                )
            )
        }

        // ---------------------------------------------------------
        // 5. Reset the physical ESP32
        // ---------------------------------------------------------
        return try {

            ioTRepository.resetDevice(deviceId)

            // -----------------------------------------------------
            // 6. Reset succeeded.
            // Now remove the local record permanently.
            // -----------------------------------------------------
            ioTRepository.deleteDevice(deviceId)

            Result.success(Unit)

        } catch (e: Exception) {

            // Cloud deletion succeeded, but physical reset failed.
            //
            // Keep the local state so you can later implement a
            // recovery/pending-reset workflow.
            Result.failure(e)
        }
    }
}