package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.repository.AuthRepository
import javax.inject.Inject

class UpdateDisplayNameUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(newName: String) = authRepository.updateDisplayName(newName)
}