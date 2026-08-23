package com.example.siliguide.domain.model

enum class AuthProvider { EMAIL, GOOGLE, GUEST }

data class UserProfile(
    val userId: String,
    val displayName: String,
    val email: String?,
    val provider: AuthProvider
)