package com.example.siliguide.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.data.sync.SyncEngine
import com.example.siliguide.data.sync.SyncResult
import com.example.siliguide.data.sync.SyncWorker
import com.example.siliguide.domain.repository.SyncRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val syncEngine: SyncEngine,
    private val userPreferences: UserPreferences
) : SyncRepository {

    companion object {
        private const val ONE_TIME_SYNC_WORK_NAME = "sili_one_time_sync"
        private const val PERIODIC_SYNC_WORK_NAME = "sili_periodic_sync"
    }

    private val workManager by lazy { WorkManager.getInstance(context) }

    override fun enqueueOneTimeSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            ONE_TIME_SYNC_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    override fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicSyncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicSyncRequest
        )
    }

    override suspend fun syncNow(): SyncResult {
        val session = userPreferences.sessionFlow.first()
        if (session == null || session.isGuest) {
            return SyncResult.Error("Cannot sync: User is not authenticated")
        }
        return syncEngine.sync(session.userId)
    }

    override fun cancelAllSync() {
        workManager.cancelUniqueWork(ONE_TIME_SYNC_WORK_NAME)
        workManager.cancelUniqueWork(PERIODIC_SYNC_WORK_NAME)
    }
}
