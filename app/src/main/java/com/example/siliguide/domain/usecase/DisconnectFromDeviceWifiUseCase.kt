package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class DisconnectFromDeviceWifiUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    operator fun invoke() {
        repository.disconnectFromWifi()
    }
}
