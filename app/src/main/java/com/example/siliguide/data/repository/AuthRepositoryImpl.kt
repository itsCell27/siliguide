package com.example.siliguide.data.repository

import android.util.Log
import com.example.siliguide.data.local.sqldelight.SiliGuideDatabaseManager
import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.data.remote.supabase.datasource.SupabaseUserRemoteSource
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
import com.example.siliguide.domain.repository.SyncRepository
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

class AuthRepositoryImpl @Inject constructor(
    private val dbManagerProvider: dagger.Lazy<SiliGuideDatabaseManager>,
    private val userPreferences: UserPreferences,
    private val supabaseProvider: dagger.Lazy<SupabaseClient>,
    private val syncRepositoryProvider: dagger.Lazy<SyncRepository>,
    private val userRemoteSourceProvider: dagger.Lazy<SupabaseUserRemoteSource>
) : AuthRepository {

    private val dbManager get() = dbManagerProvider.get()
    private val supabase get() = supabaseProvider.get()
    private val syncRepository get() = syncRepositoryProvider.get()
    private val userRemoteSource get() = userRemoteSourceProvider.get()

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
        supabase.auth.signInWith(OTP) {
            this.email = email
            createUser = true
        }
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
        val session = supabase.auth.currentSessionOrNull()
            ?: throw IllegalStateException("Verification succeeded but no session was established.")
        val realUserId = session.user?.id
            ?: throw IllegalStateException("Authenticated user has no ID.")
        val derivedName = session.user?.userMetadata?.get("name")?.toString()?.trim('"')
            ?.takeIf { it.isNotBlank() }
            ?: email.substringBefore("@")
        handlePostSignIn(realUserId, email, derivedName)
        Result.success(Unit)
    } catch (e: Exception) {
        Log.d("VerifyEmailOTP", e.toString())
        Result.failure(Exception(e.toFriendlyAuthError()))
    }

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = try {
        supabase.auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
        }
        val session = supabase.auth.currentSessionOrNull()
            ?: throw IllegalStateException("Google sign-in succeeded but no session was established.")
        val realUserId = session.user?.id
            ?: throw IllegalStateException("Authenticated user has no ID.")
        val displayName = session.user?.userMetadata?.get("full_name")?.toString()?.trim('"')
            ?: session.user?.email?.substringBefore("@")
            ?: "User"
        handlePostSignIn(realUserId = realUserId, email = session.user?.email, displayName = displayName)
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

    // Handles the post-auth database logic: migrates data if the current
    // local session is a guest, otherwise just inserts a fresh user row.
    private suspend fun handlePostSignIn(realUserId: String, email: String?, displayName: String) {
        val current = userPreferences.sessionFlow.first()
        val now = System.currentTimeMillis()

        if (current != null && current.isGuest) {
            val user = supabase.auth.currentUserOrNull()
            val createdAt = user?.createdAt
            val lastSignIn = user?.lastSignInAt

            /*
            * Only problem in this if user sends OTP code past 2 mins (120secs)
            * this logic will assume account already exist
            * TODO: create a better logic to handle this
             */
            val isBrandNewAccount = createdAt != null && lastSignIn != null &&
                    (lastSignIn - createdAt) < 120.seconds


            // On a genuine new signup both timestamps are stamped to the same
            // instant — the account is created and signed-in in one motion.
            // If they differ, this account already existed before this call.

            if (!isBrandNewAccount) {
                supabase.auth.signOut()
                throw IllegalStateException(
                    "This email is already registered. Log in instead, or use a different email to create a new account."
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
        syncRepository.enqueueOneTimeSync()
    }

    override suspend fun updateDisplayName(newName: String): Result<Unit> = try {
        val session = userPreferences.sessionFlow.first()
            ?: throw IllegalStateException("No signed-in user")
        val userId = session.userId
        val now = System.currentTimeMillis()

        dbManager.queries.updateDisplayName(newName, now, userId)

        if (!session.isGuest) {
            try {
                userRemoteSource.updateDisplayName(userId, newName, Instant.ofEpochMilli(now).toString())
            } catch (e: Exception) {
                Log.w("AuthRepositoryImpl", "Direct update of display_name to Supabase failed, queued for sync", e)
            }
            syncRepository.enqueueOneTimeSync()
        }

        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(Exception(e.toFriendlyAuthError()))
    }
}

/**
 * Raw exception text (Ktor timeout URLs, IOException stack noise, Supabase JSON error bodies)
 * is mapped to clear, plain-language messages for the user.
 */
private fun Throwable.toFriendlyAuthError(): String {
    val rawMsg = (message ?: "").lowercase()

    return when {
        this is HttpRequestTimeoutException || rawMsg.contains("timeout") ->
            "Request timed out. Check your connection and try again."

        this is IOException || rawMsg.contains("unable to resolve host") || rawMsg.contains("failed to connect") || rawMsg.contains("network") ->
            "No internet connection. Check your network and try again."

        this is android.database.sqlite.SQLiteConstraintException ->
            "This email is already linked to an existing account on this device. Log in instead, or use a different email."

        rawMsg.contains("otp_expired") || rawMsg.contains("token has expired") || (rawMsg.contains("expired") && rawMsg.contains("token")) ->
            "The verification code has expired. Please request a new code."

        rawMsg.contains("invalid") && (rawMsg.contains("token") || rawMsg.contains("otp") || rawMsg.contains("code")) ->
            "Invalid verification code. Please check the code in your email and try again."

        rawMsg.contains("rate_limit") || rawMsg.contains("rate limit") || rawMsg.contains("429") || rawMsg.contains("once every") || rawMsg.contains("too many") || rawMsg.contains("security purposes") ->
            "Too many requests. Please wait a moment before requesting another code."

        rawMsg.contains("user not found") || rawMsg.contains("user_not_found") ->
            "No account found associated with this email address."

        rawMsg.contains("session") && (rawMsg.contains("expired") || rawMsg.contains("not found")) ->
            "Your session has expired. Please sign in again."

        else -> {
            val clean = message
                ?.replace(Regex("[{}\"'\\[\\]]"), "")
                ?.replace("error_code:", "")
                ?.replace("msg:", "")
                ?.trim()

            if (!clean.isNullOrBlank() && !clean.contains("io.github.jan") && !clean.contains("PostgrestRestException") && !clean.contains("AuthRestException") && !clean.contains("Exception")) {
                clean
            } else {
                "Something went wrong. Please try again."
            }
        }
    }
}