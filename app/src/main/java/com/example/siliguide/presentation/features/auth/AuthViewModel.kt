package com.example.siliguide.presentation.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.usecase.CreateGuestSessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val createGuestSessionUseCase: CreateGuestSessionUseCase
) : ViewModel() {

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            createGuestSessionUseCase() // Creates SQLite row and DataStore state
            onSuccess() // Triggers Navigation
        }
    }
}