package com.example.siliguide.presentation.features.iot

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.siliguide.domain.model.SensorResponse
import com.example.siliguide.domain.usecase.FetchSensorsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IoTViewModel @Inject constructor(
    // We now inject the UseCase, NOT the Repository!
    private val fetchSensorsUseCase: FetchSensorsUseCase
) : ViewModel() {

    var sensorData by mutableStateOf<SensorResponse?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun fetchSensors() {
        viewModelScope.launch {
            isLoading = true
            error = null

            try {
                // Call the UseCase as if it were a function
                sensorData = fetchSensorsUseCase()
            } catch (e: Exception) {
                error = "ESP32 not connected: ${e.message}"
                sensorData = null
            }

            isLoading = false
        }
    }
}