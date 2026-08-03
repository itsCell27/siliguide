package com.example.siliguide.domain.usecase

import com.example.siliguide.data.remote.iot.dto.DeviceInfoDto
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class VerifyDeviceConnectionUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    // Pass the host (e.g., "192.168.4.1" for AP mode or an mDNS address for LAN mode) as a parameter
    suspend operator fun invoke(host: String): Result<DeviceInfoDto> {
        return repository.verifyDeviceConnection(host)
    }
}