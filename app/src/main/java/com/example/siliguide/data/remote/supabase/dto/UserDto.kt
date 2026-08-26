package com.example.siliguide.data.remote.supabase.dto

import com.example.siliguide.data.local.Users
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class UserDto(
    @SerialName("id")
    val id: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("email")
    val email: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

fun Users.toDto(): UserDto {
    return UserDto(
        id = this.user_id,
        displayName = this.display_name,
        email = this.email,
        createdAt = Instant.ofEpochMilli(this.created_at).toString(),
        updatedAt = Instant.ofEpochMilli(this.updated_at).toString()
    )
}
