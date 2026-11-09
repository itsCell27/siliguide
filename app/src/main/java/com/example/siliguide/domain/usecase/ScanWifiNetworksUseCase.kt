package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.WifiNetwork
import com.example.siliguide.domain.repository.WifiScanRepository
import javax.inject.Inject

class ScanWifiNetworksUseCase @Inject constructor(
    private val wifiScanRepository: WifiScanRepository
) {
    suspend operator fun invoke(): List<WifiNetwork> =
        wifiScanRepository.scanNetworks()
}