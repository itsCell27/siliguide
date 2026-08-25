package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.IoTRepository
import com.example.siliguide.domain.repository.SyncRepository
import javax.inject.Inject

class UpdateDeviceNameUseCase @Inject constructor(
    private val ioTRepository: IoTRepository,
    private val syncRepository: SyncRepository
) {
    suspend operator fun invoke(deviceName: String, deviceId: String) {
        val currentTimestamp = System.currentTimeMillis()
        ioTRepository.updateDeviceName(deviceName, currentTimestamp, deviceId)
        syncRepository.enqueueOneTimeSync()
    }
}