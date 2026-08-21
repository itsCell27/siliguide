package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class UpdateDeviceNameUseCase @Inject constructor(
    private val ioTRepository: IoTRepository
) {
    private val currentTimestamp = System.currentTimeMillis()
    suspend operator fun invoke(deviceName: String, deviceId: String) {
        ioTRepository.updateDeviceName(deviceName, currentTimestamp, deviceId)
    }
}