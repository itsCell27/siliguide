package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.AuthRepository
import javax.inject.Inject

class VerifyEmailOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, token: String) = authRepository.verifyEmailOtp(email, token)
}