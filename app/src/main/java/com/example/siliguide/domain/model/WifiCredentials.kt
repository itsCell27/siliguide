package com.example.siliguide.domain.model

data class WifiCredentials(
    val ssid: String,
    val password: String,
    val security: String
)