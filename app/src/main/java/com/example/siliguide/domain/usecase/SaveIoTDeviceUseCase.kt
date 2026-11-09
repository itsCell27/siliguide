package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.SyncRepository
import javax.inject.Inject

/**
 * Persists a validated SiliGuide IoT device to the local database and schedules sync.
 */
class SaveIoTDeviceUseCase @Inject constructor(
    private val repository: IoTRepository,
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(device: IoTDevice) {
        repository.saveDevice(device)
        syncRepository.enqueueOneTimeSync()
    }
}