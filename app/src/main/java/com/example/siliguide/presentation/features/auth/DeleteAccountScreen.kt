package com.example.siliguide.presentation.features.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.siliguide.core.ui.components.ErrorDialog
import com.example.siliguide.core.ui.theme.*
import kotlinx.coroutines.delay

private const val OTP_LENGTH = 6
private const val RESEND_WAIT_SECONDS = 60
private const val TOAST_VISIBLE_MS = 2500L

private enum class DeleteAccountStep { ENTRY, OTP_VERIFY }

/**
 * Screen 16 (mockup) — permanent account deletion, gated behind a fresh
 * OTP re-verification. Passwordless apps have no "re-enter your password"
 * checkpoint, so this screen IS that checkpoint: proves whoever is holding
 * the phone right now still controls this email, not just that a session
 * happens to still be open.
 *
 * Two internal steps (ENTRY -> OTP_VERIFY), same single-composable-with-
 * enum-state shape as [EmailAuthScreen] — not two separate nav destinations.
 */
@Composable
fun DeleteAccountScreen(
    userEmail: String,
    onBackClick: () -> Unit,
    onKeepAccountClick: () -> Unit,
    onSendCodeClick: (onSuccess: () -> Unit) -> Unit = {},
    onConfirmDeletionClick: (otp: String) -> Unit = {},
    onResendCodeClick: (onSuccess: () -> Unit) -> Unit = {},
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {}
) {
    // Prevent device hardware/gesture back press while deletion or code dispatch is running
    BackHandler(enabled = isLoading) { /* consume back press while loading */ }

    var step by rememberSaveable { mutableStateOf(DeleteAccountStep.ENTRY) }
    var otp by rememberSaveable { mutableStateOf("") }
    var targetResendTimestampMillis by rememberSaveable { mutableLongStateOf(0L) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(0) }
    var showResentToast by rememberSaveable { mutableStateOf(false) }

    val isOtpComplete = otp.length == OTP_LENGTH

    // Wall-clock countdown: accurately tracks elapsed time across app switches and backgrounding.
    LaunchedEffect(step, targetResendTimestampMillis) {
        if (step == DeleteAccountStep.OTP_VERIFY && targetResendTimestampMillis > 0L) {
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = ((targetResendTimestampMillis - now) / 1000).toInt().coerceAtLeast(0)
                secondsLeft = remaining
                if (remaining <= 0) break
                delay(500)
            }
        }
    }

    // Transient "New code sent" toast, auto-hides.
    LaunchedEffect(showResentToast) {
        if (showResentToast) {
            delay(TOAST_VISIBLE_MS)
            showResentToast = false
        }
    }

    SiliGuideTheme {
        Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // ── Top bar ──────────────────────────────────────────────
                Box(
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(if (isLoading) OffWhite.copy(alpha = 0.5f) else OffWhite, CircleShape)
                            .border(1.dp, if (isLoading) Border.copy(alpha = 0.5f) else Border, CircleShape)
                            .clickable(
                                enabled = !isLoading,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isLoading) {
                                    // Back from OTP step returns to the entry step,
                                    // only the entry step actually leaves the screen.
                                    if (step == DeleteAccountStep.OTP_VERIFY) {
                                        step = DeleteAccountStep.ENTRY
                                        otp = ""
                                        targetResendTimestampMillis = 0L
                                        secondsLeft = 0
                                    } else {
                                        onBackClick()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BackArrowIcon(
                            color = if (isLoading) GrayHint else GrayBody,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    when (step) {
                        DeleteAccountStep.ENTRY -> DeleteEntryContent(
                            isLoading = isLoading,
                            onSendCode = {
                                // Gated on real success — mirrors the fix already
                                // applied to EmailAuthScreen, not the mockup's
                                // own optimistic-advance JS.
                                onSendCodeClick {
                                    targetResendTimestampMillis = System.currentTimeMillis() + (RESEND_WAIT_SECONDS * 1000L)
                                    step = DeleteAccountStep.OTP_VERIFY
                                }
                            },
                            onKeepAccount = onKeepAccountClick
                        )

                        DeleteAccountStep.OTP_VERIFY -> DeleteOtpVerifyContent(
                            email = userEmail,
                            otp = otp,
                            onOtpChange = { otp = it },
                            isOtpComplete = isOtpComplete,
                            secondsLeft = secondsLeft,
                            showResentToast = showResentToast,
                            isLoading = isLoading,
                            onConfirm = { onConfirmDeletionClick(otp) },
                            onResend = {
                                onResendCodeClick {
                                    targetResendTimestampMillis = System.currentTimeMillis() + (RESEND_WAIT_SECONDS * 1000L)
                                    showResentToast = true
                                }
                            },
                            onCancel = onKeepAccountClick
                        )
                    }
                }
            }
        }
    }

    if (errorMessage != null) {
        ErrorDialog(message = errorMessage, onDismiss = onDismissError)
    }
}

// ── Step 1: what gets deleted + permanent warning ───────────────────────────
@Composable
private fun ColumnScope.DeleteEntryContent(
    isLoading: Boolean,
    onSendCode: () -> Unit,
    onKeepAccount: () -> Unit
) {
    Column {
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(74.dp)
                .background(DangerBg, CircleShape)
                .border(1.5.dp, DangerBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            WarningTriangleDangerIcon(modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))

        Text(
            "Delete Account",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
            color = Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Confirm with a code sent to your email.",
            style = MaterialTheme.typography.bodySmall,
            color = GrayMedium
        )
        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(OffWhite, RoundedCornerShape(12.dp))
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "WHAT GETS DELETED",
                style = MaterialTheme.typography.labelSmall,
                color = GrayBody
            )
            DeleteListItem(
                icon = { PersonDangerIcon(Modifier.size(14.dp)) },
                text = "Your profile and sign-in access"
            )
            DeleteListItem(
                icon = { PlantDangerIcon(Modifier.size(14.dp)) },
                text = "All saved plants and their analysis sessions"
            )
            DeleteListItem(
                icon = { DeviceDangerIcon(Modifier.size(14.dp)) },
                text = "Paired IoT devices and their settings"
            )
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DangerBg, RoundedCornerShape(12.dp))
                .border(1.dp, DangerBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(modifier = Modifier.padding(top = 1.dp)) {
                ExclaimCircleIcon(modifier = Modifier.size(14.dp))
            }
            Text(
                "Your account and all associated data will be permanently deleted. This cannot be undone.",
                style = MaterialTheme.typography.bodySmall,
                color = Danger
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onSendCode,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Danger,
                contentColor = White,
                disabledContainerColor = DangerBorder,
                disabledContentColor = White
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Send Confirmation Code", style = MaterialTheme.typography.titleSmall)
            }
        }
        Text(
            "Keep My Account",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (isLoading) GrayHint else GrayBody,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = !isLoading,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (!isLoading) onKeepAccount() }
                .padding(vertical = 6.dp)
        )
    }
}

@Composable
private fun DeleteListItem(icon: @Composable () -> Unit, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(modifier = Modifier.padding(top = 2.dp)) { icon() }
        Text(
            text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp, lineHeight = 17.sp),
            color = Black
        )
    }
}

// ── Step 2: OTP re-verification ─────────────────────────────────────────────
@Composable
private fun ColumnScope.DeleteOtpVerifyContent(
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    isOtpComplete: Boolean,
    secondsLeft: Int,
    showResentToast: Boolean,
    isLoading: Boolean,
    onConfirm: () -> Unit,
    onResend: () -> Unit,
    onCancel: () -> Unit
) {
    Column {
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(74.dp)
                .background(DangerBg, CircleShape)
                .border(1.5.dp, DangerBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            EnvelopeDangerIcon(modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(18.dp))

        Text(
            "Confirm Deletion",
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
            color = Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Enter the 6-digit code we sent to:",
            style = MaterialTheme.typography.bodySmall,
            color = GrayMedium
        )
        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OffWhite, RoundedCornerShape(12.dp))
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            EnvelopeDangerIcon(modifier = Modifier.size(15.dp))
            Text(
                email,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Black
            )
        }

        Spacer(Modifier.height(18.dp))

        DangerOtpInputField(
            otp = otp,
            onOtpChange = onOtpChange,
            length = OTP_LENGTH,
            enabled = !isLoading
        )

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Didn't receive a code?",
                style = MaterialTheme.typography.bodySmall,
                color = GrayMedium
            )
            Spacer(Modifier.height(4.dp))
            if (secondsLeft > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ClockWaitIcon(modifier = Modifier.size(13.dp))
                    Text(
                        "Resend code in ${secondsLeft}s",
                        style = MaterialTheme.typography.titleSmall,
                        color = GrayHint
                    )
                }
            } else {
                Text(
                    "Resend code",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isLoading) GrayHint else Danger,
                    modifier = Modifier.clickable(
                        enabled = !isLoading,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (!isLoading) onResend() }
                )
            }
            if (showResentToast) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .background(DangerBg, RoundedCornerShape(10.dp))
                        .border(1.dp, DangerBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 13.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    CheckCircleDangerIcon(modifier = Modifier.size(13.dp))
                    Text(
                        "New code sent. Check your inbox.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = Danger
                    )
                }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onConfirm,
            enabled = isOtpComplete && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Danger,
                contentColor = White,
                disabledContainerColor = DangerBorder,
                disabledContentColor = White
            ),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Confirm Deletion", style = MaterialTheme.typography.titleSmall)
            }
        }
        Text(
            "Cancel",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = if (isLoading) GrayHint else GrayBody,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = !isLoading,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (!isLoading) onCancel() }
                .padding(vertical = 6.dp)
        )
    }
}

// ── OTP field — same one-real-input-six-drawn-boxes trick as EmailAuthScreen,
// recolored to the destructive palette (Danger focus border, not GreenPrimary).
@Composable
private fun DangerOtpInputField(
    otp: String,
    onOtpChange: (String) -> Unit,
    length: Int,
    enabled: Boolean = true
) {
    BasicTextField(
        value = otp,
        onValueChange = { raw ->
            if (enabled) {
                onOtpChange(raw.filter { it.isDigit() }.take(length))
            }
        },
        readOnly = !enabled,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(length) { index ->
                    val char = otp.getOrNull(index)?.toString() ?: ""
                    val isNextToType = index == otp.length && enabled
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(
                                if (isNextToType) White else OffWhite,
                                RoundedCornerShape(12.dp)
                            )
                            .border(
                                1.5.dp,
                                if (isNextToType) Danger else Border,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            char,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            color = if (enabled) Black else GrayHint
                        )
                    }
                }
            }
        }
    )
}

// ── Small Canvas icons (per icon convention: no drawable XML) ────────────────
@Composable
private fun BackArrowIcon(
    color: androidx.compose.ui.graphics.Color = GrayBody,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val path = Path().apply {
            moveTo(w * 0.62f, h * 0.16f)
            lineTo(w * 0.30f, h * 0.5f)
            lineTo(w * 0.62f, h * 0.84f)
        }
        drawPath(path, color = color, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun WarningTriangleDangerIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val strokeW = w * 0.06f
        val tri = Path().apply {
            moveTo(w * 0.5f, h * 0.13f)
            lineTo(w * 0.93f, h * 0.87f)
            lineTo(w * 0.07f, h * 0.87f)
            close()
        }
        drawPath(tri, color = Danger, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = Danger, start = Offset(w * 0.5f, h * 0.38f), end = Offset(w * 0.5f, h * 0.61f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawCircle(color = Danger, radius = w * 0.03f, center = Offset(w * 0.5f, h * 0.72f))
    }
}

@Composable
private fun PersonDangerIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawCircle(color = Danger, radius = w * 0.17f, center = Offset(w * 0.5f, h * 0.36f), style = Stroke(width = w * 0.13f))
        val shoulders = Path().apply {
            moveTo(w * 0.18f, h * 0.86f)
            cubicTo(w * 0.28f, h * 0.60f, w * 0.72f, h * 0.60f, w * 0.82f, h * 0.86f)
        }
        drawPath(shoulders, color = Danger, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round))
    }
}

@Composable
private fun PlantDangerIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val strokeW = w * 0.09f
        val droplet = Path().apply {
            moveTo(w * 0.5f, h * 0.1f)
            cubicTo(w * 0.5f, h * 0.1f, w * 0.85f, h * 0.35f, w * 0.85f, h * 0.63f)
            cubicTo(w * 0.85f, h * 0.83f, w * 0.7f, h * 0.97f, w * 0.5f, h * 0.97f)
            cubicTo(w * 0.3f, h * 0.97f, w * 0.15f, h * 0.83f, w * 0.15f, h * 0.63f)
            cubicTo(w * 0.15f, h * 0.35f, w * 0.5f, h * 0.1f, w * 0.5f, h * 0.1f)
            close()
        }
        drawPath(droplet, color = Danger, style = Stroke(width = strokeW))
        drawLine(color = Danger, start = Offset(w * 0.5f, h * 0.97f), end = Offset(w * 0.5f, h * 0.5f), strokeWidth = strokeW, cap = StrokeCap.Round)
    }
}

@Composable
private fun DeviceDangerIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawRoundRect(
            color = Danger,
            topLeft = Offset(w * 0.13f, h * 0.3f),
            size = Size(w * 0.74f, h * 0.43f),
            cornerRadius = CornerRadius(w * 0.11f, w * 0.11f),
            style = Stroke(width = w * 0.09f)
        )
        drawCircle(color = Danger, radius = w * 0.07f, center = Offset(w * 0.31f, h * 0.515f))
        drawLine(color = Danger, start = Offset(w * 0.53f, h * 0.515f), end = Offset(w * 0.74f, h * 0.515f), strokeWidth = w * 0.09f, cap = StrokeCap.Round)
    }
}

@Composable
private fun ExclaimCircleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val strokeW = w * 0.09f
        drawCircle(color = Danger, radius = w * 0.41f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = strokeW))
        drawLine(color = Danger, start = Offset(w * 0.5f, h * 0.3f), end = Offset(w * 0.5f, h * 0.54f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawCircle(color = Danger, radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.7f))
    }
}

@Composable
private fun EnvelopeDangerIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val strokeW = w * 0.075f
        drawRoundRect(
            color = Danger,
            topLeft = Offset(w * 0.06f, h * 0.22f),
            size = Size(w * 0.88f, h * 0.58f),
            cornerRadius = CornerRadius(w * 0.11f, w * 0.11f),
            style = Stroke(width = strokeW)
        )
        val flap = Path().apply {
            moveTo(w * 0.10f, h * 0.29f)
            lineTo(w * 0.5f, h * 0.57f)
            lineTo(w * 0.90f, h * 0.29f)
        }
        drawPath(flap, color = Danger, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun ClockWaitIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        drawCircle(color = GrayHint, radius = w * 0.43f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = w * 0.085f))
        val hands = Path().apply {
            moveTo(w * 0.5f, h * 0.28f)
            lineTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.68f, h * 0.62f)
        }
        drawPath(hands, color = GrayHint, style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun CheckCircleDangerIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width; val h = size.height
        val strokeW = w * 0.09f
        drawCircle(color = Danger, radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.5f), style = Stroke(width = strokeW))
        val check = Path().apply {
            moveTo(w * 0.3f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.67f)
            lineTo(w * 0.71f, h * 0.35f)
        }
        drawPath(check, color = Danger, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 740)
@Composable
fun DeleteAccountScreenPreview() {
    SiliGuideTheme {
        DeleteAccountScreen(userEmail = "juan.delacruz@email.com", onBackClick = {}, onKeepAccountClick = {})
    }
}

@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 320, heightDp = 640)
@Composable
fun DeleteAccountScreenSmallPreview() {
    SiliGuideTheme {
        DeleteAccountScreen(userEmail = "juan.delacruz@email.com", onBackClick = {}, onKeepAccountClick = {})
    }
}

@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 412, heightDp = 915)
@Composable
fun DeleteAccountScreenLargePreview() {
    SiliGuideTheme {
        DeleteAccountScreen(userEmail = "juan.delacruz@email.com", onBackClick = {}, onKeepAccountClick = {})
    }
}