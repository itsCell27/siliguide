package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class GetDeviceByHardwareMacUseCase @Inject constructor(
    private val ioTRepository: IoTRepository
) {
    suspend operator fun invoke(hardwareMac: String): IoTDevice? {
        return ioTRepository.getDeviceByHardwareMac(hardwareMac)
    }
}