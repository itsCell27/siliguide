package com.example.siliguide.data.remote.supabase.datasource

import com.example.siliguide.data.remote.supabase.dto.AnalysisSessionDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAnalysisRemoteSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val table by lazy {
        supabaseClient.postgrest.from("analysis_sessions")
    }

    suspend fun upsertSessions(sessions: List<AnalysisSessionDto>) = withContext(Dispatchers.IO) {
        if (sessions.isEmpty()) return@withContext
        table.upsert(sessions)
    }

    suspend fun fetchSessionsByUser(userId: String): List<AnalysisSessionDto> = withContext(Dispatchers.IO) {
        table.select {
            filter {
                eq("user_id", userId)
                eq("is_deleted", false)
            }
        }.decodeList<AnalysisSessionDto>()
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        table.delete {
            filter {
                eq("session_id", sessionId)
            }
        }
    }
}
