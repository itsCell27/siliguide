package com.example.siliguide.presentation.features.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.usecase.DeleteAccountUseCase
import com.example.siliguide.domain.usecase.SendEmailOtpUseCase
import com.example.siliguide.domain.usecase.VerifyEmailOtpUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DeleteAccountUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Drives DeleteAccountScreen's two real steps: send a fresh OTP, then verify
 * it as a re-authentication check before the account is actually deleted.
 *
 * Deliberately its own ViewModel, not folded into AuthViewModel — this
 * screen's job (destructive re-verification) is conceptually different from
 * AuthViewModel's job (establishing a session in the first place), even
 * though both reuse the exact same OTP UseCases underneath.
 */
@HiltViewModel
class DeleteAccountViewModel @Inject constructor(
    private val sendEmailOtpUseCase: SendEmailOtpUseCase,
    private val verifyEmailOtpUseCase: VerifyEmailOtpUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeleteAccountUiState())
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    /** Sends the confirmation code. Same call for both "Send Confirmation
     *  Code" (step 1) and "Resend code" (step 2) — only the caller's
     *  onSuccess behavior differs (advance the step vs. restart the timer),
     *  same shared-call shape as AuthViewModel.sendEmailOtp already uses. */
    fun sendConfirmationCode(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = DeleteAccountUiState(isLoading = true)
            sendEmailOtpUseCase(email)
                .onSuccess {
                    _uiState.value = DeleteAccountUiState(isLoading = false)
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = DeleteAccountUiState(
                        isLoading = false,
                        errorMessage = formatUserFriendlyError(e)
                    )
                }
        }
    }

    /**
     * Verifies the fresh OTP code, then executes the permanent account deletion:
     * 1. Re-verifies identity via OTP.
     * 2. Runs best-effort physical reset for paired ESP32 devices.
     * 3. Invokes Supabase Edge Function 'delete-account' (service_role hard delete + Storage cleanup).
     * 4. Cleans local SQLite tables, files, and session upon confirmed server deletion.
     */
    fun confirmDeletion(email: String, otp: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = DeleteAccountUiState(isLoading = true)
            verifyEmailOtpUseCase(email, otp)
                .onSuccess {
                    deleteAccountUseCase()
                        .onSuccess {
                            _uiState.value = DeleteAccountUiState(isLoading = false)
                            onSuccess()
                        }
                        .onFailure { e ->
                            _uiState.value = DeleteAccountUiState(
                                isLoading = false,
                                errorMessage = formatUserFriendlyError(
                                    e,
                                    "Failed to delete account on server. Please check your connection and try again."
                                )
                            )
                        }
                }
                .onFailure { e ->
                    _uiState.value = DeleteAccountUiState(
                        isLoading = false,
                        errorMessage = formatUserFriendlyError(
                            e,
                            "Invalid verification code. Please check the code in your email and try again."
                        )
                    )
                }
        }
    }

    /** Same call as sendConfirmationCode — kept as its own name so the
     *  NavGraph wiring reads clearly at each call site (send vs. resend),
     *  even though the underlying behavior is identical. */
    fun resendConfirmationCode(email: String, onSuccess: () -> Unit) =
        sendConfirmationCode(email, onSuccess)

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun formatUserFriendlyError(
        throwable: Throwable,
        fallback: String = "Something went wrong. Please try again."
    ): String {
        val raw = (throwable.message ?: "").lowercase()
        return when {
            raw.contains("timeout") ->
                "Request timed out. Please check your connection and try again."

            raw.contains("unable to resolve host") || raw.contains("failed to connect") || raw.contains("network") || raw.contains("no address associated") ->
                "No internet connection. Please check your network and try again."

            raw.contains("otp_expired") || raw.contains("token has expired") || (raw.contains("expired") && raw.contains("token")) ->
                "The verification code has expired or is invalid."

            raw.contains("invalid") && (raw.contains("token") || raw.contains("otp") || raw.contains("code")) ->
                "Invalid verification code. Please check the code in your email and try again."

            raw.contains("rate_limit") || raw.contains("rate limit") || raw.contains("429") || raw.contains("once every") || raw.contains("too many") || raw.contains("security purposes") ->
                "Too many requests. Please wait a moment before requesting another code."

            else -> {
                val clean = throwable.message
                    ?.replace(Regex("[{}\"'\\[\\]]"), "")
                    ?.replace("error_code:", "")
                    ?.replace("msg:", "")
                    ?.trim()

                if (!clean.isNullOrBlank() && !clean.contains("io.github.jan") && !clean.contains("Exception")) {
                    clean
                } else {
                    fallback
                }
            }
        }
    }
}