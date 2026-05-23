package com.example.siliguide.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    authRepository: AuthRepository
) : ViewModel() {

    // Reads DataStore instantly on app launch to decide which screen to show
    val startDestination: StateFlow<String?> = authRepository.getUserIdFlow().map { userId ->
        if (userId == null) {
            Screen.Welcome.route // No user? Show Welcome screen
        } else {
            Screen.Dashboard.route // User exists? Skip to Dashboard
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null // null means "loading"
    )
}