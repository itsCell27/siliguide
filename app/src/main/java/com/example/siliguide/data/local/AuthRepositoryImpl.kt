package com.example.siliguide.data.local

import app.cash.sqldelight.db.SqlDriver
import com.example.siliguide.data.local.SiliGuideDatabase
import com.example.siliguide.domain.model.User
import com.example.siliguide.domain.repository.AuthRepository
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

class AuthRepositoryImpl @Inject constructor(
    driver: SqlDriver
) : AuthRepository {

    private val database = SiliGuideDatabase(driver)
    private val queries = database.siliGuideDatabaseQueries

    override suspend fun createGuestUser(): String {
        val userId = "guest_${UUID.randomUUID()}"
        val guestName = generateGuestName()

        queries.insertUser(
            user_id = userId,
            display_name = guestName,
            email = null,
            auth_provider = "GUEST",
            is_guest = 1
        )

        return userId
    }

    private fun generateGuestName(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val suffix = (1..4)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")

        return "Guest_$suffix"
    }

    override suspend fun getUserById(userId: String): User? {
        val result = queries.getUserById(userId).executeAsOneOrNull()

        return result?.let {
            User(
                userId = it.user_id,
                displayName = it.display_name,
                email = it.email,
                authProvider = it.auth_provider,
                isGuest = it.is_guest == 1L
            )
        }
    }
}