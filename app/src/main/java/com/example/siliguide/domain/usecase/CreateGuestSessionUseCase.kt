package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.AuthRepository
import javax.inject.Inject

class CreateGuestSessionUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): String {
        return repository.createGuestSession()
    }
}