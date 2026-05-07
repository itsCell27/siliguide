package com.example.siliguide.presentation.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.data.local.session.UserSessionManager
import com.example.siliguide.domain.usecase.CreateGuestUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val createGuestUserUseCase: CreateGuestUserUseCase,
    private val sessionManager: UserSessionManager
) : ViewModel() {

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            val userId = createGuestUserUseCase()
            sessionManager.saveUserId(userId)
            onSuccess()
        }
    }
}