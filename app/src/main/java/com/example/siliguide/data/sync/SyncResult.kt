package com.example.siliguide.data.sync

sealed interface SyncResult {
    data object Success : SyncResult
    data class Error(val message: String, val throwable: Throwable? = null) : SyncResult
}
