package com.example.siliguide.domain.usecase

import androidx.navigation.NavHost
import com.example.siliguide.data.remote.iot.dto.WiFiStatusDto
import com.example.siliguide.domain.repository.IoTRepository
import com.google.mlkit.vision.barcode.common.Barcode
import javax.inject.Inject

class CheckDeviceWiFiStatusUseCase @Inject constructor(
    private val ioTRepository: IoTRepository
) {
    suspend operator fun invoke(host: String): WiFiStatusDto {
        return ioTRepository.checkWiFiStatus(host)
    }
}