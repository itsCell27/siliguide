package com.example.siliguide.domain.usecase

import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class VerifyDeviceConnectionUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    suspend operator fun invoke(): Result<DeviceInfoDto> {
        return repository.verifyDeviceConnection()
    }
}