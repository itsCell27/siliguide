package com.example.siliguide.data.repository

import com.example.siliguide.data.local.sqldelight.SiliGuideDatabaseManager
import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.domain.model.UserSession
import com.example.siliguide.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val dbManager: SiliGuideDatabaseManager,
    private val userPreferences: UserPreferences
) : AuthRepository {

    override suspend fun createGuestSession(): String {
        val guestId = UUID.randomUUID().toString()
        val guestName = "Guest #${(1000..9999).random()}"

        // 1. Database
        dbManager.queries.insertUser(
            user_id = guestId,
            display_name = guestName,
            email = null,
            auth_provider = "GUEST",
            is_guest = 1L,
            created_at = System.currentTimeMillis()
        )

        // 2. Session (Uses the model!)
        userPreferences.saveSession(UserSession(guestId, true))

        return guestId
    }

    override fun getUserIdFlow(): Flow<String?> {
        return userPreferences.sessionFlow.map { it?.userId }
    }

    override suspend fun getUserId(): String? {
        return userPreferences.sessionFlow.first()?.userId
    }
}
