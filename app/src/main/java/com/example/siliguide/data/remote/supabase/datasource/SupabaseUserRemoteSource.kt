package com.example.siliguide.data.remote.supabase.datasource

import com.example.siliguide.data.remote.supabase.dto.UserDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseUserRemoteSource @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val table by lazy {
        supabaseClient.postgrest.from("users")
    }

    suspend fun fetchUserById(userId: String): UserDto? = withContext(Dispatchers.IO) {
        table.select {
            filter {
                eq("id", userId)
            }
        }.decodeSingleOrNull<UserDto>()
    }

    suspend fun updateDisplayName(userId: String, displayName: String, updatedAt: String) = withContext(Dispatchers.IO) {
        table.update(
            {
                set("display_name", displayName)
                set("updated_at", updatedAt)
            }
        ) {
            filter {
                eq("id", userId)
            }
        }
    }
}
