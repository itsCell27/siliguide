package com.example.siliguide.presentation.features.plants

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.repository.AuthRepository
import com.example.siliguide.domain.repository.SyncRepository
import com.example.siliguide.domain.usecase.AddPlantUseCase
import com.example.siliguide.domain.usecase.GetPlantByIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AddPlantViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val addPlantUseCase: AddPlantUseCase,
    private val getPlantByIdUseCase: GetPlantByIdUseCase,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _createdPlantId = MutableStateFlow<String?>(null)
    val createdPlantId: StateFlow<String?> = _createdPlantId.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun addPlant(name: String, deviceId: String? = null) {
        if (_isSaving.value) return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _isSaving.value = true
            try {
                val userId = withContext(Dispatchers.IO) { authRepository.getUserId() }
                if (userId != null) {
                    val newPlantId = withContext(Dispatchers.IO) {
                        addPlantUseCase(userId, trimmed, deviceId)
                    }
                    val verifiedPlant = withContext(Dispatchers.IO) {
                        getPlantByIdUseCase(newPlantId)
                    }
                    _createdPlantId.value = verifiedPlant.plantId
                    syncRepository.enqueueOneTimeSync()
                }
            } finally {
                _isSaving.value = false
            }
        }
    }
}
