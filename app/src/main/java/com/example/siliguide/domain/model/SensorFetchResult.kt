package com.example.siliguide.domain.model

sealed class SensorFetchResult {
    data class Success(val response: SensorResponse) : SensorFetchResult()
    data object Unreachable : SensorFetchResult()   // no response — timeout, socket error, powered off
    data object TokenRejected : SensorFetchResult() // response arrived, but decrypt/auth failed — likely reset
}
