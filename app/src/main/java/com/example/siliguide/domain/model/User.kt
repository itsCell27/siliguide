package com.example.siliguide.domain.model

data class User(
    val userId: String,
    val displayName: String,
    val email: String?,
    val authProvider: String,
    val isGuest: Boolean
)