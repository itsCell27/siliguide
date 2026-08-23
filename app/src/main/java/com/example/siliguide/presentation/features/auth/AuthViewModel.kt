package com.example.siliguide.presentation.features.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.BuildConfig
import com.example.siliguide.domain.usecase.CreateGuestSessionUseCase
import com.example.siliguide.domain.usecase.GuestLogOutUseCase
import com.example.siliguide.domain.usecase.SendEmailOtpUseCase
import com.example.siliguide.domain.usecase.SignInWithGoogleUseCase
import com.example.siliguide.domain.usecase.SignOutUseCase
import com.example.siliguide.domain.usecase.VerifyEmailOtpUseCase
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.siliguide.domain.model.UserProfile
import com.example.siliguide.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val createGuestSessionUseCase: CreateGuestSessionUseCase,
    private val sendEmailOtpUseCase: SendEmailOtpUseCase,
    private val verifyEmailOtpUseCase: VerifyEmailOtpUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val guestLogOutUseCase: GuestLogOutUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = authRepository.getUserProfileFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            createGuestSessionUseCase()
            onSuccess()
        }
    }

    fun sendEmailOtp(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            sendEmailOtpUseCase(email)
                .onSuccess {
                    _uiState.value = AuthUiState(isLoading = false)
                    onSuccess()
                }
                .onFailure { e -> _uiState.value = AuthUiState(isLoading = false, errorMessage = e.message) }
        }
    }

    fun verifyEmailOtp(email: String, otp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            verifyEmailOtpUseCase(email, otp)
                .onSuccess {
                    _uiState.value = AuthUiState(isLoading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = AuthUiState(isLoading = false, errorMessage = e.message ?: "Invalid code")
                }
        }
    }

    fun signInWithGoogle(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val credentialManager = CredentialManager.create(context)
                val result = withTimeoutOrNull(15_000) {
                    credentialManager.getCredential(context, request)
                } ?: run {
                    _uiState.value = AuthUiState(
                        isLoading = false,
                        errorMessage = "Request timed out. Check your connection and try again."
                    )
                    return@launch
                }

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCred = GoogleIdTokenCredential.createFrom(credential.data)
                    signInWithGoogleUseCase(googleCred.idToken)
                        .onSuccess {
                            _uiState.value = AuthUiState(isLoading = false)
                            onSuccess()
                        }
                        .onFailure { e ->
                            _uiState.value = AuthUiState(isLoading = false, errorMessage = e.message)
                        }
                } else {
                    _uiState.value = AuthUiState(isLoading = false, errorMessage = "Not a Google credential")
                }
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the account picker on purpose — not an error, stay quiet
                _uiState.value = AuthUiState(isLoading = false)
            } catch (e: GetCredentialException) {
                _uiState.value = AuthUiState(
                    isLoading = false,
                    errorMessage = "Couldn't sign in with Google. Check your internet connection, or make sure a Google account is set up on this device."
                )
            } catch (e: Exception) {
                _uiState.value = AuthUiState(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            signOutUseCase()
            onSuccess()
        }
    }

    fun guestLogOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            guestLogOutUseCase()
            onSuccess()
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}