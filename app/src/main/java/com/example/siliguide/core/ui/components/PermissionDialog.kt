package com.example.siliguide.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import com.example.siliguide.core.ui.theme.White
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun PermissionDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    @DrawableRes imageRes: Int? = null,
    imageDescription: String? = null
) {

    AlertDialog(
        onDismissRequest = onDismiss,

        // White behind the instruction images so they blend with the dialog
        containerColor = if (imageRes != null) White else AlertDialogDefaults.containerColor,

        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge
            )
        },

        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Optional visual instruction (e.g. device mode switch position)
                if (imageRes != null) {
                    Image(
                        painter = painterResource(imageRes),
                        contentDescription = imageDescription,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },

        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text(confirmText)
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text(dismissText)
            }
        }
    )
}
