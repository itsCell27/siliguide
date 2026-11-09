package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class GetIoTDeviceByIdUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    suspend operator fun invoke(deviceId: String): IoTDevice? {
        return repository.getDeviceById(deviceId)
    }
}