package com.example.siliguide.core.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Message dialog with one required action and an optional second one.
 *
 * PermissionDialog always renders two buttons. Some flows only have one valid
 * way forward — a failed provisioning attempt, for example — and offering a
 * "Cancel" there would leave the user stuck on a dead screen.
 */
@Composable
fun MessageDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    dismissText: String? = null,
    onDismissRequest: () -> Unit = onConfirm
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,

        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },

        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },

        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },

        dismissButton = dismissText?.let { label ->
            {
                TextButton(onClick = onDismissRequest) {
                    Text(label)
                }
            }
        }
    )
}