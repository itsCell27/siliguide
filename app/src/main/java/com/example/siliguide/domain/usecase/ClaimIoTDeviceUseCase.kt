package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.IoTRepository
import javax.inject.Inject

class ClaimIoTDeviceUseCase @Inject constructor(
    private val repository: IoTRepository
) {

    suspend operator fun invoke(
        host: String,
        token: String
    ) {
        repository.claimDevice(
            host = host,
            token = token
        )
    }
}