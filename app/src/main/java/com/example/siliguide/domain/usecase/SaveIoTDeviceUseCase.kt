package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

/**
 * Persists a validated SiliGuide IoT device to the local database.
 */
class SaveIoTDeviceUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    suspend operator fun invoke(device: IoTDevice) {
        repository.saveDevice(device)
    }
}