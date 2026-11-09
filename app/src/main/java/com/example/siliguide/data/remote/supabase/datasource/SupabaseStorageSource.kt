package com.example.siliguide.data.remote.supabase.datasource

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class SupabaseStorageSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    companion object {
        const val BUCKET_ANALYSIS_IMAGES = "analysis-images"
    }

    private val bucket by lazy {
        supabaseClient.storage.from(BUCKET_ANALYSIS_IMAGES)
    }

    suspend fun uploadAnalysisImage(
        userId: String,
        sessionId: String,
        imageBytes: ByteArray
    ): String = withContext(Dispatchers.IO) {
        val path = "$userId/$sessionId.jpg"
        bucket.upload(
            path = path,
            data = imageBytes
        ) {
            upsert = true
        }
        path
    }

    suspend fun downloadAnalysisImage(
        storagePath: String
    ): ByteArray = withContext(Dispatchers.IO) {
        bucket.downloadAuthenticated(storagePath)
    }

    suspend fun createSignedUrl(
        storagePath: String,
        expiresInSeconds: Long = 3600L
    ): String = withContext(Dispatchers.IO) {
        bucket.createSignedUrl(
            path = storagePath,
            expiresIn = expiresInSeconds.seconds
        )
    }

    suspend fun deleteAnalysisImage(
        userId: String,
        sessionId: String
    ) = withContext(Dispatchers.IO) {
        val path = "$userId/$sessionId.jpg"
        runCatching {
            bucket.delete(path)
        }
    }
}
