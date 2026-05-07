package com.example.siliguide.domain.repository

import com.example.siliguide.domain.model.User

interface AuthRepository {
    suspend fun createGuestUser(): String
    suspend fun getUserById(userId: String): User?
}