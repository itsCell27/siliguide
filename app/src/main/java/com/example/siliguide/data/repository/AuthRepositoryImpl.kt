package com.example.siliguide.data.repository

import com.example.siliguide.data.local.sqldelight.SiliGuideDatabaseManager
import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.domain.model.UserSession
import com.example.siliguide.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.ktor.client.plugins.HttpRequestTimeoutException
import com.example.siliguide.domain.model.AuthProvider
import com.example.siliguide.domain.model.UserProfile
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.UUID
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val dbManager: SiliGuideDatabaseManager,
    private val userPreferences: UserPreferences,
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun createGuestSession(): String {
        val guestId = UUID.randomUUID().toString()
        val guestName = "Guest #${(1000..9999).random()}"
        dbManager.queries.insertUser(
            user_id = guestId,
            display_name = guestName,
            email = null,
            is_guest = 1L,
            created_at = System.currentTimeMillis(),
            updated_at = System.currentTimeMillis()
        )
        userPreferences.saveSession(UserSession(guestId, true))
        return guestId
    }

    override fun getUserIdFlow(): Flow<String?> =
        userPreferences.sessionFlow.map { it?.userId }

    override suspend fun getUserId(): String? =
        userPreferences.sessionFlow.first()?.userId

    override suspend fun sendEmailOtp(email: String): Result<Unit> = try {
        supabase.auth.signInWith(OTP) { this.email = email }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(e.toFriendlyAuthError()))
    }

    override suspend fun verifyEmailOtp(email: String, token: String): Result<Unit> = try {
        supabase.auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email,
            token = token
        )
        val user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException("No user after verify")
        val displayName = user.email?.substringBefore("@") ?: "User"
        handlePostSignIn(realUserId = user.id, email = user.email, displayName = displayName)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(e.toFriendlyAuthError()))
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = try {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
        }
        val user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException("No user after google sign-in")
        val displayName = user.userMetadata?.get("full_name")?.toString()
            ?: user.email?.substringBefore("@") ?: "User"
        handlePostSignIn(realUserId = user.id, email = user.email, displayName = displayName)
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(e.toFriendlyAuthError()))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getUserProfileFlow(): Flow<UserProfile?> =
        getUserIdFlow().flatMapLatest { userId ->
            if (userId == null) {
                flowOf(null)
            } else {
                dbManager.queries.getUserById(userId)
                    .asFlow()
                    .mapToOneOrNull(Dispatchers.IO)
                    .map { row ->
                        row?.let {
                            val isGuest = it.is_guest == 1L
                            val provider = when {
                                isGuest -> AuthProvider.GUEST
                                supabase.auth.currentIdentitiesOrNull()
                                    ?.any { identity -> identity.provider == "google" } == true -> AuthProvider.GOOGLE
                                else -> AuthProvider.EMAIL
                            }
                            UserProfile(
                                userId = it.user_id,
                                displayName = it.display_name,
                                email = it.email,
                                provider = provider
                            )
                        }
                    }
            }
        }

    override suspend fun signOut() {
        supabase.auth.signOut()
        userPreferences.clearSession()
    }

    override suspend fun deleteGuestSession() {
        val current = userPreferences.sessionFlow.first()
        if (current != null && current.isGuest) {
            dbManager.queries.deleteUser(current.userId)
        }
        userPreferences.clearSession()
    }

    // Shared by both sign-in paths — migrates guest data if the current
    // local session is a guest, otherwise just inserts a fresh user row.
    private suspend fun handlePostSignIn(realUserId: String, email: String?, displayName: String) {
        val current = userPreferences.sessionFlow.first()
        val now = System.currentTimeMillis()

        if (current != null && current.isGuest) {
            // Refuse to merge this guest's local data onto an account that
            // already has its own local row on this device — that's not a
            // guest "upgrading," that's two unrelated data sets colliding on
            // the same primary key.
            val existingLocalRow = dbManager.queries.getUserById(realUserId).executeAsOneOrNull()
            if (existingLocalRow != null) {
                supabase.auth.signOut() // undo the Supabase-side login, stay a guest
                throw IllegalStateException(
                    "This email is already linked to an existing account on this device. " +
                            "Log in instead, or use a different email to create a new one."
                )
            }
            dbManager.queries.transaction {
                dbManager.queries.migrateGuestToAuthenticated(
                    newUserId = realUserId,
                    newDisplayName = displayName,
                    newEmail = email,
                    updated_at = now,
                    oldGuestId = current.userId
                )
            }
        } else {
            dbManager.queries.insertUser(
                user_id = realUserId,
                display_name = displayName,
                email = email,
                is_guest = 0L,
                created_at = now,
                updated_at = now
            )
        }
        userPreferences.saveSession(UserSession(realUserId, isGuest = false))
    }
}

/**
 * Raw exception text (Ktor timeout URLs, IOException stack noise) is not
 * something to show a user. Map the common network failure shapes to plain
 * language; anything unrecognized falls back to its own message.
 */
private fun Throwable.toFriendlyAuthError(): String = when (this) {
    is HttpRequestTimeoutException -> "Request timed out. Check your connection and try again."
    is IOException -> "No internet connection. Check your network and try again."
    is android.database.sqlite.SQLiteConstraintException -> "This email is already linked to an existing account on this device. Log in instead, or use a different email."
    else -> message ?: "Something went wrong. Please try again."
}