package com.example.siliguide.presentation.features.auth

import android.util.Patterns
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.core.ui.components.ErrorDialog
import kotlinx.coroutines.delay

private const val OTP_LENGTH = 6
private const val RESEND_WAIT_SECONDS = 60

private enum class EmailAuthStep { EMAIL_ENTRY, OTP_VERIFY }

@Composable
fun EmailAuthScreen(
    onBackClick: () -> Unit,
    onContinueClick: (email: String, onSuccess: () -> Unit) -> Unit = { _, _ -> },
    onVerifyOtpClick: (email: String, otp: String) -> Unit = { _, _ -> },
    onResendCodeClick: (email: String, onSuccess: () -> Unit) -> Unit = { _, _ -> },
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onDismissError: () -> Unit = {}
) {
    var step by rememberSaveable { mutableStateOf(EmailAuthStep.EMAIL_ENTRY) }
    var email by rememberSaveable { mutableStateOf("") }
    var otp by rememberSaveable { mutableStateOf("") }
    var targetResendTimestampMillis by rememberSaveable { mutableLongStateOf(0L) }
    var secondsLeft by rememberSaveable { mutableIntStateOf(0) }

    val isEmailValid = remember(email) { Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() }
    val isOtpComplete = otp.length == OTP_LENGTH

    // Wall-clock countdown: accurately tracks elapsed time across app switches and backgrounding.
    LaunchedEffect(step, targetResendTimestampMillis) {
        if (step == EmailAuthStep.OTP_VERIFY && targetResendTimestampMillis > 0L) {
            while (true) {
                val now = System.currentTimeMillis()
                val remaining = ((targetResendTimestampMillis - now) / 1000).toInt().coerceAtLeast(0)
                secondsLeft = remaining
                if (remaining <= 0) break
                delay(500)
            }
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
                            .background(OffWhite, CircleShape)
                            .border(1.dp, Border, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // Mockup rule: back from OTP step returns to email entry,
                                // only email entry step actually leaves the screen.
                                if (step == EmailAuthStep.OTP_VERIFY) {
                                    step = EmailAuthStep.EMAIL_ENTRY
                                    otp = ""
                                    targetResendTimestampMillis = 0L
                                    secondsLeft = 0
                                } else {
                                    onBackClick()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BackArrowIcon(modifier = Modifier.size(16.dp))
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
                        EmailAuthStep.EMAIL_ENTRY -> EmailEntryContent(
                            email = email,
                            onEmailChange = { email = it },
                            isEmailValid = isEmailValid,
                            onContinue = {
                                if (isEmailValid) {
                                    onContinueClick(email.trim()) {
                                        targetResendTimestampMillis = System.currentTimeMillis() + (RESEND_WAIT_SECONDS * 1000L)
                                        step = EmailAuthStep.OTP_VERIFY
                                    }
                                }
                            },
                            isLoading = isLoading
                        )

                        EmailAuthStep.OTP_VERIFY -> OtpVerifyContent(
                            email = email.trim(),
                            otp = otp,
                            onOtpChange = { otp = it },
                            isOtpComplete = isOtpComplete,
                            secondsLeft = secondsLeft,
                            onVerify = { onVerifyOtpClick(email.trim(), otp) },
                            onResend = {
                                onResendCodeClick(email.trim()) {
                                    targetResendTimestampMillis = System.currentTimeMillis() + (RESEND_WAIT_SECONDS * 1000L)
                                }
                            },
                            isLoading = isLoading
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

// ── Step 1: email entry ────────────────────────────────────────────────────
@Composable
private fun ColumnScope.EmailEntryContent(
    email: String,
    onEmailChange: (String) -> Unit,
    isEmailValid: Boolean,
    onContinue: () -> Unit,
    isLoading: Boolean = false
) {
    Column {
        Spacer(Modifier.height(4.dp))
        Text(
            "Continue with Email",
            style = MaterialTheme.typography.headlineSmall,
            color = Black
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Enter your email address",
            style = MaterialTheme.typography.bodySmall,
            color = GrayMedium
        )
        Spacer(Modifier.height(28.dp))

        Text(
            "EMAIL ADDRESS",
            style = MaterialTheme.typography.labelMedium,
            color = GrayBody
        )
        Spacer(Modifier.height(6.dp))

        val focused = remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(
                    if (focused.value) White else OffWhite,
                    RoundedCornerShape(12.dp)
                )
                .border(
                    1.dp,
                    if (focused.value) GreenPrimary else Border,
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            EnvelopeOutlineIcon(modifier = Modifier.size(16.dp), color = GrayHint)

            Box(modifier = Modifier.weight(1f)) {
                if (email.isEmpty()) {
                    Text(
                        "you@email.com",
                        style = MaterialTheme.typography.bodyMedium,
                        color = GrayHint
                    )
                }
                BasicTextField(
                    value = email,
                    onValueChange = onEmailChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = Black),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused.value = it.isFocused }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            "We'll send a 6-digit code to verify it's you. ",
            style = MaterialTheme.typography.bodySmall,
            color = GrayHint
        )
    }

    Button(
        onClick = onContinue,
        enabled = isEmailValid && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GreenPrimary,
            contentColor = White,
            disabledContainerColor = GrayLight,
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
            Text("Continue", style = MaterialTheme.typography.titleSmall)
        }
    }
}

// ── Step 2: OTP verification ───────────────────────────────────────────────
@Composable
private fun ColumnScope.OtpVerifyContent(
    email: String,
    otp: String,
    onOtpChange: (String) -> Unit,
    isOtpComplete: Boolean,
    secondsLeft: Int,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    isLoading: Boolean = false
) {
    Column {
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .size(74.dp)
                .background(GreenBgLight, CircleShape)
                .border(1.5.dp, GreenLight, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            MailCheckIcon(modifier = Modifier.size(36.dp))
        }
        Spacer(Modifier.height(20.dp))

        Text("Check your email", style = MaterialTheme.typography.headlineSmall, color = Black)
        Spacer(Modifier.height(4.dp))
        Text(
            "We sent a 6-digit verification code to:",
            style = MaterialTheme.typography.bodySmall,
            color = GrayMedium
        )
        Spacer(Modifier.height(18.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OffWhite, RoundedCornerShape(12.dp))
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            EnvelopeOutlineIcon(modifier = Modifier.size(15.dp), color = GreenPrimary)
            Text(
                email,
                style = MaterialTheme.typography.bodyMedium,
                color = Black
            )
        }

        Spacer(Modifier.height(18.dp))

        OtpInputField(
            otp = otp,
            onOtpChange = onOtpChange,
            length = OTP_LENGTH
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
                Text(
                    "Resend code in ${secondsLeft}s",
                    style = MaterialTheme.typography.titleSmall,
                    color = GrayHint
                )
            } else {
                Text(
                    "Resend code",
                    style = MaterialTheme.typography.titleSmall,
                    color = GreenPrimary,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onResend() }
                )
            }
        }
    }

    Button(
        onClick = onVerify,
        enabled = isOtpComplete && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = GreenPrimary,
            contentColor = White,
            disabledContainerColor = GrayLight,
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
            Text("Verify", style = MaterialTheme.typography.titleSmall)
        }
    }
}

// ── The 6-digit OTP field ────────────────────────────────────────────────────
// IMPORTANT: this is ONE real input (one BasicTextField / one cursor / one
// focus target). The 6 boxes are pure decoration drawn from the current text.
// Digits only, max 6 chars, numeric keypad only (KeyboardType.NumberPassword
// avoids autofill/predictive-text bars some IMEs show for KeyboardType.Number).
@Composable
private fun OtpInputField(
    otp: String,
    onOtpChange: (String) -> Unit,
    length: Int
) {
    BasicTextField(
        value = otp,
        onValueChange = { raw ->
            val digitsOnly = raw.filter { it.isDigit() }.take(length)
            onOtpChange(digitsOnly)
        },
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
                    val isNextToType = index == otp.length
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
                                if (isNextToType) GreenPrimary else Border,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            char,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                            color = Black
                        )
                    }
                }
            }
        }
    )
}

// ── Small Canvas icons (per icon convention: no drawable XML) ────────────────
@Composable
private fun BackArrowIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.62f, h * 0.16f)
            lineTo(w * 0.30f, h * 0.5f)
            lineTo(w * 0.62f, h * 0.84f)
        }
        drawPath(
            path = path,
            color = GrayBody,
            style = Stroke(width = w * 0.11f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}

@Composable
private fun EnvelopeOutlineIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.085f

        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.06f, h * 0.2f),
            size = androidx.compose.ui.geometry.Size(w * 0.88f, h * 0.66f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.1f, w * 0.1f),
            style = Stroke(width = strokeW)
        )
        val flap = Path().apply {
            moveTo(w * 0.11f, h * 0.27f)
            lineTo(w * 0.5f, h * 0.58f)
            lineTo(w * 0.89f, h * 0.27f)
        }
        drawPath(
            path = flap,
            color = color,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
    }
}

@Composable
private fun MailCheckIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.05f

        drawRoundRect(
            color = GreenPrimary,
            topLeft = Offset(w * 0.08f, h * 0.22f),
            size = androidx.compose.ui.geometry.Size(w * 0.84f, h * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.11f, w * 0.11f),
            style = Stroke(width = strokeW)
        )
        val flap = Path().apply {
            moveTo(w * 0.12f, h * 0.28f)
            lineTo(w * 0.5f, h * 0.56f)
            lineTo(w * 0.88f, h * 0.28f)
        }
        drawPath(
            path = flap,
            color = GreenPrimary,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        )
        // Little green "new" dot, top-right
        drawCircle(color = GreenLight, radius = w * 0.125f, center = Offset(w * 0.79f, h * 0.24f))
        drawCircle(color = White, radius = w * 0.125f, center = Offset(w * 0.79f, h * 0.24f), style = Stroke(width = strokeW * 0.7f))
    }
}

@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 740)
@Composable
fun EmailAuthScreenPreview() {
    SiliGuideTheme {
        EmailAuthScreen(onBackClick = {})
    }
}

// Small phone preview
@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 320, heightDp = 640)
@Composable
fun EmailAuthScreenSmallPreview() {
    SiliGuideTheme {
        EmailAuthScreen(onBackClick = {})
    }
}

// Large phone preview
@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 412, heightDp = 915)
@Composable
fun EmailAuthScreenLargePreview() {
    SiliGuideTheme {
        EmailAuthScreen(onBackClick = {})
    }
}