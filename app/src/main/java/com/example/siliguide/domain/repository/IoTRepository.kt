package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.SensorResponse

// The Domain layer defines WHAT it needs, not HOW to get it.
interface IoTRepository {
    suspend fun getSensors(): SensorResponse
}