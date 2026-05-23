package com.example.siliguide.domain.repository

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun createGuestSession(): String
    fun getUserIdFlow(): Flow<String?>

    suspend fun getUserId(): String?
}