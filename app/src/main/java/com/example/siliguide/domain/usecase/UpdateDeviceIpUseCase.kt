package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class UpdateDeviceIpUseCase @Inject constructor(
    private val ioTRepository: IoTRepository
) {
    private val currentTimestamp = System.currentTimeMillis()
    suspend operator fun invoke(ip: String, deviceId: String) {
        ioTRepository.updateDeviceIp(deviceId, currentTimestamp, ip)
    }
}