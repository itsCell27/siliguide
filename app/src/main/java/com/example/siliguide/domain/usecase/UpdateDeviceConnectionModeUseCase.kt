package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class UpdateDeviceConnectionModeUseCase @Inject constructor(
    private val ioTRepository: IoTRepository
) {
    private val currentTimestamp = System.currentTimeMillis()
    suspend operator fun invoke(connectionMode: ConnectionMode, deviceId: String) {
        ioTRepository.updateDeviceConnectionMode(connectionMode, currentTimestamp, deviceId)
    }
}