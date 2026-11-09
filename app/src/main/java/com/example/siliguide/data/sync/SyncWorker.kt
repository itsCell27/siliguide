package com.example.siliguide.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.siliguide.data.local.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun getSyncEngine(): SyncEngine
        fun getUserPreferences(): UserPreferences
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val userPreferences = entryPoint.getUserPreferences()
        val syncEngine = entryPoint.getSyncEngine()

        val session = userPreferences.sessionFlow.first()
        if (session == null || session.isGuest) {
            Log.d(TAG, "Skipping sync: No user logged in or user is guest")
            return Result.success()
        }

        return when (val result = syncEngine.sync(session.userId)) {
            is SyncResult.Success -> {
                Log.d(TAG, "Background sync successful for user: ${session.userId}")
                Result.success()
            }
            is SyncResult.Error -> {
                Log.e(TAG, "Background sync failed: ${result.message}", result.throwable)
                if (runAttemptCount < 3) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }
}
