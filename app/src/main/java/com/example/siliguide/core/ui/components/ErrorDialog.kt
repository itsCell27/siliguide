package com.example.siliguide.core.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.siliguide.core.ui.theme.*

/**
 * Shared "something went wrong" dialog — same visual language as
 * DangerConfirmDialog/MessageDialog, single acknowledgment button instead
 * of a confirm/cancel pair. For network/auth errors that should interrupt
 * the user rather than sit as inline text.
 */
@Composable
fun ErrorDialog(message: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(18.dp), color = White, modifier = Modifier.widthIn(max = 300.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Something went wrong",
                    style = TextStyle(
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                    color = GrayBody,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary, contentColor = White)
                ) {
                    Text("OK", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp))
                }
            }
        }
    }
}