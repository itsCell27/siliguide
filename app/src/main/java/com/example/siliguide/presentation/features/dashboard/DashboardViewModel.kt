package com.example.siliguide.presentation.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.data.local.session.UserSessionManager
import com.example.siliguide.domain.usecase.GetUserByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    sessionManager: UserSessionManager,
    private val getUserByIdUseCase: GetUserByIdUseCase
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val userName: StateFlow<String> =
        sessionManager.userIdFlow
            .filterNotNull()
            .flatMapLatest { userId ->
                flow {
                    val user = getUserByIdUseCase(userId)
                    emit(user?.displayName ?: "Guest")
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = "Guest"
            )
}