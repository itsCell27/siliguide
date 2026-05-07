package com.example.siliguide.domain.usecase

import com.example.siliguide.domain.model.User
import com.example.siliguide.domain.repository.AuthRepository
import javax.inject.Inject

class GetUserByIdUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(userId: String): User? {
        return repository.getUserById(userId)
    }
}