package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.ConnectionMode
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class UpdateDeviceConnectionModeUseCase @Inject constructor(
    private val ioTRepository: IoTRepository
) {
    suspend operator fun invoke(deviceId: String, connectionMode: ConnectionMode) {
        ioTRepository.updateDeviceConnectionMode(deviceId, connectionMode)
    }
}