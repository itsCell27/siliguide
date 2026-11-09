package com.example.siliguide.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.data.local.UserPreferences
import com.example.siliguide.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferences: UserPreferences
) : ViewModel() {

    // Reads DataStore instantly on app launch to decide which screen to show
    val startDestination: StateFlow<String?> = userPreferences.sessionFlow
        .map { session ->
            if (session?.userId == null) {
                Screen.Welcome.route // No user? Show Welcome screen
            } else {
                Screen.Dashboard.route // User exists? Skip to Dashboard
            }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null // null means "loading"
        )
}