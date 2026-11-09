package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class CheckDeviceConnectionUseCase @Inject constructor(
    private val repository: IoTRepository
) {

    suspend operator fun invoke(
        deviceId: String
    ): Boolean {

        return repository.isDeviceOnline(deviceId)
    }
}