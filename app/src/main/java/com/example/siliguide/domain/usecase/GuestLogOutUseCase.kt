package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.AuthRepository
import javax.inject.Inject

class GuestLogOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke() = authRepository.deleteGuestSession()
}