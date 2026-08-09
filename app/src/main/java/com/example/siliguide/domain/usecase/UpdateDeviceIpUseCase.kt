package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class UpdateDeviceIpUseCase @Inject constructor(
    private val ioTRepository: IoTRepository
) {
    suspend operator fun invoke(deviceId: String, ip: String) {
        ioTRepository.updateDeviceIp(deviceId, ip)
    }
}