package com.example.siliguide.domain.repository

import com.example.siliguide.data.sync.SyncResult

interface SyncRepository {
    fun enqueueOneTimeSync()
    fun schedulePeriodicSync()
    suspend fun syncNow(): SyncResult
}
