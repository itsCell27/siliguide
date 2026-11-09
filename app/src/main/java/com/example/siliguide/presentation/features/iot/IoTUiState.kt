package com.example.siliguide.presentation.features.iot

data class IoTUiState (
    val isLoading: Boolean = false,
    val loadingMessage: String = "Loading...",
    val hasPairedDevices: Boolean = false,
    val isBluetoothOn: Boolean = false,
)