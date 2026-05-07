package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class ConnectToDeviceWifiUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    operator fun invoke(
        ssid: String,
        password: String,
        onSuccess: () -> Unit,
        onFailure: () -> Unit
    ) {
        repository.connectToDeviceWifi(
            ssid,
            password,
            onSuccess,
            onFailure
        )
    }
}