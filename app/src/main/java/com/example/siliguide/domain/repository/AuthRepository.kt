package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun createGuestSession(): String
    fun getUserIdFlow(): Flow<String?>

    suspend fun getUserId(): String?

    suspend fun sendEmailOtp(email: String): Result<Unit>
    suspend fun verifyEmailOtp(email: String, token: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>

    fun getUserProfileFlow(): Flow<UserProfile?>

    suspend fun signOut()
    suspend fun deleteGuestSession()
}