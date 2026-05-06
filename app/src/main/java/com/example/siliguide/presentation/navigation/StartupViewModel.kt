package com.example.siliguide.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.usecase.GetCurrentUserIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase
) : ViewModel() {

    fun checkSession(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val userId = getCurrentUserIdUseCase().first()
            onResult(userId != null)
        }
    }
}