package com.example.siliguide.presentation.features.iot.bluetooth_setup.state

/**
 * Represents every visual state BluetoothConnectionScreen can be in.
 * The screen renders differently based on which state is active instead of
 * being split into separate composables per state.
 */
sealed interface BluetoothConnectionUiState {

    /** Waiting for the user to pair via Android Bluetooth Settings. */
    data object Waiting : BluetoothConnectionUiState

    /** A paired Bluetooth device was detected; about to verify it. */
    data object Searching : BluetoothConnectionUiState

    /** Verifying the paired device is a SiliGuide IoT device. */
    data object Verifying : BluetoothConnectionUiState

    /** The paired device is not a SiliGuide IoT device. */
    data class Unsupported(val message: String) : BluetoothConnectionUiState

}

/** Status of a single row inside VerificationCard. */
enum class VerificationStageStatus {
    Done,
    Pending,
    Waiting
}

/** One row shown inside VerificationCard. */
data class VerificationStage(
    val label: String,
    val status: VerificationStageStatus
)