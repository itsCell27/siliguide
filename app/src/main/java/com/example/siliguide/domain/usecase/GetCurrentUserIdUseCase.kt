package com.example.siliguide.domain.usecase

import com.example.siliguide.data.local.session.UserSessionManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCurrentUserIdUseCase @Inject constructor(
    private val sessionManager: UserSessionManager
) {
    operator fun invoke(): Flow<String?> {
        return sessionManager.userIdFlow
    }
}