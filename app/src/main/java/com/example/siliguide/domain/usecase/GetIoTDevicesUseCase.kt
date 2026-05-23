package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.IoTDevice
import com.example.siliguide.domain.repository.IoTRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetIoTDevicesUseCase @Inject constructor(
    private val repository: IoTRepository
) {
    operator fun invoke(userId: String): Flow<List<IoTDevice>> = repository.getAllDevices(userId)
}