package com.example.siliguide.domain.model

sealed class SensorFetchResult {
    data class Success(val response: SensorResponse) : SensorFetchResult()
    data object Unreachable : SensorFetchResult()   // no response — timeout, socket error, powered off
    data object TokenRejected : SensorFetchResult() // device is unclaimed or broadcasting new token — needs re-pairing
    data object DecryptionFailed : SensorFetchResult() // payload decryption failed with saved token — needs reset
}
