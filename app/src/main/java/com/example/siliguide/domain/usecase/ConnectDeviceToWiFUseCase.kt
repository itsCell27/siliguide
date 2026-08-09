package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class ConnectDeviceToWiFUseCase @Inject constructor(
    private val ioTRepository: IoTRepository
) {
    suspend operator fun invoke(
        host: String,
        token: String,
        ssid: String,
        password: String
    ) {
        ioTRepository.configureWiFi(
            host = host,
            token = token,
            ssid = ssid,
            password = password
        )
    }
}