package com.example.siliguide.presentation.features.iot.lan_setup

/** What the single dialog button on the Success screen should do. */
enum class LanSaveAction {

    /** Nothing to fix automatically — just close the dialog. */
    DISMISS,

    /**
     * The device could not join the chosen network. Send the user back to
     * Step 3 to pick a different one; the device is already claimed and saved,
     * so only /configure has to be repeated.
     */
    RECONFIGURE_WIFI
}

/**
 * A blocking message raised while claiming, saving or provisioning the device.
 *
 * [action] decides whether the dialog offers a way out or just closes.
 */
data class LanSaveDialogState(
    val title: String,
    val message: String,
    val action: LanSaveAction = LanSaveAction.DISMISS,
    val confirmText: String = "OK"
)