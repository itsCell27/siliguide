package com.example.siliguide.presentation.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.siliguide.R
import com.example.siliguide.core.ui.theme.*
import com.example.siliguide.core.ui.components.ErrorDialog

private const val DISPLAY_NAME_MAX_LENGTH = 20


/**
 * Which identity provider the current session came from. Drives which rows
 * show (guests get no "Account Settings" group / no "Delete Account") and
 * which log-out dialog fires (guest log-out wipes local data).
 *
 * TODO: if a domain-level auth/provider enum already exists, prefer that one
 * and delete this — kept local for now since this screen was built standalone.
 */
enum class ProfileAuthProvider { EMAIL, GOOGLE, GUEST }

/**
 * The actual Profile UI — top bar, scrollable settings body, and all dialogs.
 * No Scaffold, no bottom nav, no SiliGuideTheme wrapper: this is meant to be
 * embedded as a tab inside DashboardScreen's shared shell (see DashboardTab
 * in DashboardScreen.kt), which owns the bottom nav and the Add-menu sheet.
 *
 * [ProfileScreen] below is a thin standalone wrapper around this, kept for
 * previews and any direct/deep-link entry outside the Dashboard tab shell.
 */
@Composable
fun ProfileContent(
    displayName: String,
    email: String?,
    authProvider: ProfileAuthProvider,
    appVersion: String = "v1.0.0",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onSaveDisplayName: (String) -> Unit = {},
    onConfirmLogOut: () -> Unit = {},
    onConfirmGuestLogOut: () -> Unit = {},
    onConfirmDeleteAccount: () -> Unit = {},
    onCreateAccountWithEmail: () -> Unit = {},
    onCreateAccountWithGoogle: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    var showChangeNameDialog by remember { mutableStateOf(false) }
    var showLogOutDialog by remember { mutableStateOf(false) }
    var showGuestLogOutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showCreateAccountDialog by remember { mutableStateOf(false) }

    val isGuest = authProvider == ProfileAuthProvider.GUEST

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Top bar ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 22.dp, vertical = 14.dp)
        ) {
            Text(
                "Profile & Settings",
                style = MaterialTheme.typography.headlineLarge,
                color = Black
            )
        }
        HorizontalDivider(color = Border, thickness = 1.dp)

        // ── Scrollable body ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            AccountCard(displayName = displayName, email = email, authProvider = authProvider)

            if (isGuest) {
                GuestBanner(
                    onCreateAccountClick = { showCreateAccountDialog = true },
                    isLoading = isLoading,
                )
            } else {
                SectionLabel("ACCOUNT SETTINGS")
                SettingsGroup {
                    SettingsRow(
                        icon = { PersonIcon(modifier = Modifier.size(14.dp), color = GreenPrimary) },
                        title = "Change Display Name",
                        showChevron = true,
                        onClick = { showChangeNameDialog = true }
                    )
                }
            }

            SectionLabel("APP SETTINGS")
            SettingsGroup {
                SettingsRow(
                    icon = { ClockIcon(modifier = Modifier.size(14.dp), color = GreenPrimary) },
                    title = "App Version",
                    trailingText = appVersion,
                    showChevron = false,
                    onClick = null
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { if (isGuest) showGuestLogOutDialog = true else showLogOutDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Danger),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Danger,
                        containerColor = White
                    )
                ) {
                    Text("Log Out", style = MaterialTheme.typography.titleSmall)
                }

                // Guests have no formal account to delete — mockup 15C omits this row.
                if (!isGuest) {
                    Text(
                        "Delete Account",
                        style = MaterialTheme.typography.titleSmall,
                        color = Danger,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { showDeleteAccountDialog = true }
                            .padding(vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    // ── Dialogs — each wired to the row/button that opens it ─────────
    if (showChangeNameDialog) {
        ChangeDisplayNameDialog(
            currentName = displayName,
            onCancel = { showChangeNameDialog = false },
            onSave = { newName ->
                onSaveDisplayName(newName)
                showChangeNameDialog = false
            }
        )
    }

    if (showLogOutDialog) {
        DangerConfirmDialog(
            icon = { LogOutIcon(modifier = Modifier.size(26.dp)) },
            iconBg = DangerBg,
            title = "Log Out?",
            message = "You will be returned to the Welcome screen.",
            confirmLabel = "Log Out",
            onCancel = { showLogOutDialog = false },
            onConfirm = {
                showLogOutDialog = false
                onConfirmLogOut()
            }
        )
    }

    if (showGuestLogOutDialog) {
        DangerConfirmDialog(
            icon = { WarningTriangleIcon(modifier = Modifier.size(26.dp)) },
            iconBg = WarningBg,
            title = "Log out of guest account?",
            message = "Because you're using a guest account, all guest data — plants, IoT devices, and analyses — will be permanently deleted from this device. This cannot be undone.",
            confirmLabel = "Log Out & Delete",
            onCancel = { showGuestLogOutDialog = false },
            onConfirm = {
                showGuestLogOutDialog = false
                onConfirmGuestLogOut()
            }
        )
    }

    if (showDeleteAccountDialog) {
        DangerConfirmDialog(
            icon = { TrashIcon(modifier = Modifier.size(26.dp)) },
            iconBg = DangerBg,
            title = "Delete Account?",
            message = "This will permanently delete your account and all associated plant and analysis data. This cannot be undone.",
            confirmLabel = "Delete Permanently",
            onCancel = { showDeleteAccountDialog = false },
            onConfirm = {
                showDeleteAccountDialog = false
                onConfirmDeleteAccount()
            }
        )
    }

    if (showCreateAccountDialog) {
        CreateAccountDialog(
            onCancel = { showCreateAccountDialog = false },
            onContinueWithEmail = {
                showCreateAccountDialog = false
                onCreateAccountWithEmail()
            },
            onContinueWithGoogle = {
                showCreateAccountDialog = false
                onCreateAccountWithGoogle()
            }
        )
    }

    if (errorMessage != null) {
        ErrorDialog(message = errorMessage, onDismiss = onDismissError)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Standalone wrapper — own Scaffold + bottom nav + theme. The normal in-app
// path is DashboardScreen embedding [ProfileContent] as a tab; this wrapper
// exists for previews and any direct/deep-link entry to Profile outside that
// shell.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun ProfileScreen(
    displayName: String,
    email: String?,
    authProvider: ProfileAuthProvider,
    appVersion: String = "v1.0.0",
    onNavigateHome: () -> Unit,
    onAddClick: () -> Unit,
    onSaveDisplayName: (String) -> Unit = {},
    onConfirmLogOut: () -> Unit = {},
    onConfirmGuestLogOut: () -> Unit = {},
    onConfirmDeleteAccount: () -> Unit = {},
    onCreateAccountWithEmail: () -> Unit = {},
    onCreateAccountWithGoogle: () -> Unit = {}
) {
    SiliGuideTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = { ProfileBottomNav(onHomeClick = onNavigateHome, onAddClick = onAddClick) }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                ProfileContent(
                    displayName = displayName,
                    email = email,
                    authProvider = authProvider,
                    appVersion = appVersion,
                    onSaveDisplayName = onSaveDisplayName,
                    onConfirmLogOut = onConfirmLogOut,
                    onConfirmGuestLogOut = onConfirmGuestLogOut,
                    onConfirmDeleteAccount = onConfirmDeleteAccount,
                    onCreateAccountWithEmail = onCreateAccountWithEmail,
                    onCreateAccountWithGoogle = onCreateAccountWithGoogle
                )
            }
        }
    }
}

// ── Account card (green gradient normally, amber gradient for guest) ────────
@Composable
private fun AccountCard(displayName: String, email: String?, authProvider: ProfileAuthProvider) {
    val isGuest = authProvider == ProfileAuthProvider.GUEST
    val topColor = if (isGuest) WarningBg else GreenBgLight
    val borderColor = if (isGuest) WarningBorder else GreenLight

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(topColor, White)), RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val initials = if (isGuest) "G" else initialsOf(displayName)
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(if (isGuest) GrayLight else GreenPrimary, CircleShape)
                .border(2.5.dp, White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initials,
                style = TextStyle(fontFamily = Fraunces, fontWeight = FontWeight.Bold, fontSize = 22.sp),
                color = if (isGuest) GrayBody else White
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                if (isGuest) "Guest" else displayName,
                style = TextStyle(
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = (-0.2).sp
                ),
                color = Black
            )
            Text(
                if (isGuest) "Not signed in" else (email ?: ""),
                style = MaterialTheme.typography.bodySmall,
                color = if (isGuest) GrayHint else GrayMedium
            )
            if (!isGuest) {
                Spacer(Modifier.height(5.dp))
//                AuthBadge(authProvider)
            }
        }
    }
}

private fun initialsOf(name: String): String =
    name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }

//@Composable
//private fun AuthBadge(authProvider: ProfileAuthProvider) {
//    val bg: Color
//    val border: Color
//    val text: Color
//    val label: String
//    when (authProvider) {
//        ProfileAuthProvider.EMAIL -> { bg = GreenBgLight; border = GreenLight; text = GreenDark; label = "EMAIL" }
//        ProfileAuthProvider.GOOGLE -> { bg = InfoBg; border = InfoBorder; text = Info; label = "GOOGLE" }
//        ProfileAuthProvider.GUEST -> { bg = WarningBg; border = WarningBorder; text = Warning; label = "GUEST" }
//    }
//    Box(
//        modifier = Modifier
//            .background(bg, RoundedCornerShape(20.dp))
//            .border(1.dp, border, RoundedCornerShape(20.dp))
//            .padding(horizontal = 9.dp, vertical = 2.dp)
//    ) {
//        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = text)
//    }
//}

// ── Guest warning banner + Create Account CTA ────────────────────────────────
@Composable
private fun GuestBanner(
    onCreateAccountClick: () -> Unit,
    isLoading: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WarningBg, RoundedCornerShape(12.dp))
            .border(1.dp, WarningBorder, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "⚠ You're using a guest account.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = Warning
        )
        Text(
            "Create an account to back up your data and access it from any device.",
            style = MaterialTheme.typography.bodySmall,
            color = Warning
        )
        Button(
            onClick = onCreateAccountClick,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Warning, contentColor = White),
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
            } else {
                Text("Create Account", style = MaterialTheme.typography.titleSmall)
            }
        }
//        if (errorMessage != null) {
//            Text(
//                errorMessage,
//                style = MaterialTheme.typography.bodySmall,
//                color = Danger,
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth()
//            )
//        }
    }
}

// ── Settings list building blocks ────────────────────────────────────────────
@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = GrayMedium,
        modifier = Modifier.padding(horizontal = 3.dp)
    )
}

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsRow(
    icon: @Composable () -> Unit,
    title: String,
    trailingText: String? = null,
    showChevron: Boolean,
    onClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(GreenBgLight, RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) { icon() }

        Text(
            title,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp),
            color = Black,
            modifier = Modifier.weight(1f)
        )

        trailingText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = GrayMedium)
        }

        if (showChevron) {
            Text("›", style = MaterialTheme.typography.titleLarge, color = GrayHint)
        }
    }
}

// ── Bottom nav — Profile tab active. Reuses Dashboard's icon set for visual
// parity with the shared nav bar (ic_home / ic_add / ic_profile). ────────────
@Composable
private fun ProfileBottomNav(onHomeClick: () -> Unit, onAddClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), color = White, shadowElevation = 8.dp, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(68.dp)
                .padding(horizontal = 28.dp)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onHomeClick
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_home),
                    contentDescription = "Home",
                    tint = GrayLight,
                    modifier = Modifier.size(20.dp)
                )
                Text("Home", style = NavLabelStyle, color = GrayHint)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(GreenPrimary)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAddClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add",
                    tint = White,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_profile),
                    contentDescription = "Profile",
                    tint = GreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text("Profile", style = NavLabelActiveStyle, color = GreenPrimary)
            }
        }
    }
}

// ── Dialogs ───────────────────────────────────────────────────────────────

@Composable
private fun ChangeDisplayNameDialog(
    currentName: String,
    onCancel: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }

    val trimmed = name.trim()
    val canSave = trimmed.isNotEmpty() && trimmed != currentName

    Dialog(onDismissRequest = onCancel) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(White)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Text(
                text = "Change Display Name",
                style = TextStyle(
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.2).sp
                ),
                color = Black
            )

            Text(
                text = "This is the name shown across your account and plant details.",
                fontSize = 12.sp,
                fontFamily = DMSans,
                color = GrayBody,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(White)
                    .border(
                        width = 1.5.dp,
                        color = if (trimmed.isEmpty()) GreenLight else GreenPrimary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {

                BasicTextField(
                    value = name,
                    onValueChange = { typed ->
                        if (typed.length <= DISPLAY_NAME_MAX_LENGTH) name = typed
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = DMSans,
                        fontSize = 12.sp,
                        color = Black
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    cursorBrush = SolidColor(GreenPrimary),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (name.isEmpty()) {
                                Text(
                                    text = "Your name",
                                    fontSize = 12.sp,
                                    fontFamily = DMSans,
                                    color = GrayHint
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Text(
                    text = "${name.length}/$DISPLAY_NAME_MAX_LENGTH",
                    fontSize = 10.sp,
                    fontFamily = DMSans,
                    color = GreenPrimary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OffWhite)
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onCancel)
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = GrayBody
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (canSave) GreenPrimary else GreenLight)
                        .then(
                            if (canSave) {
                                Modifier.clickable { onSave(trimmed) }
                            } else {
                                Modifier
                            }
                        )
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Save",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = DMSans,
                        color = White
                    )
                }
            }
        }
    }
}

@Composable
private fun DangerConfirmDialog(
    icon: @Composable () -> Unit,
    iconBg: Color,
    title: String,
    message: String,
    confirmLabel: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = White, modifier = Modifier.widthIn(max = 300.dp)) {
            Column(
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 22.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(iconBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) { icon() }
                Spacer(Modifier.height(10.dp))
                Text(
                    title,
                    style = TextStyle(
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                    color = GrayBody,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(1.5.dp, Border),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = GrayBody, containerColor = White)
                    ) {
                        Text("Cancel", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp))
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(11.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = White)
                    ) {
                        Text(confirmLabel, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateAccountDialog(
    onCancel: () -> Unit,
    onContinueWithEmail: () -> Unit,
    onContinueWithGoogle: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), color = White, modifier = Modifier.widthIn(max = 300.dp)) {
            Column(modifier = Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Create Your Account",
                    style = TextStyle(
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.2).sp
                    ),
                    color = Black,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Your guest plants and analyses will carry over.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GrayMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onContinueWithEmail,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Black, containerColor = White)
                ) {
                    ProfileEnvelopeIcon(modifier = Modifier.size(15.dp), color = GreenPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("Continue with Email", style = MaterialTheme.typography.titleSmall)
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = onContinueWithGoogle,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Black, containerColor = White)
                ) {
                    GoogleIcon(modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Continue with Google", style = MaterialTheme.typography.titleSmall)
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    "Cancel",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = GrayMedium,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onCancel() }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

// ── Small Canvas icons (per icon convention: no drawable XML) ────────────────
@Composable
private fun PersonIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = color,
            radius = w * 0.17f,
            center = Offset(w * 0.5f, h * 0.36f),
            style = Stroke(width = w * 0.13f)
        )
        val shoulders = Path().apply {
            moveTo(w * 0.18f, h * 0.86f)
            cubicTo(w * 0.28f, h * 0.60f, w * 0.72f, h * 0.60f, w * 0.82f, h * 0.86f)
        }
        drawPath(shoulders, color = color, style = Stroke(width = w * 0.13f, cap = StrokeCap.Round))
    }
}

@Composable
private fun ClockIcon(modifier: Modifier = Modifier, color: Color) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = color,
            radius = w * 0.4f,
            center = Offset(w / 2f, h / 2f),
            style = Stroke(width = w * 0.1f)
        )
        val hands = Path().apply {
            moveTo(w * 0.5f, h * 0.28f)
            lineTo(w * 0.5f, h * 0.5f)
            lineTo(w * 0.68f, h * 0.62f)
        }
        drawPath(hands, color = color, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun LogOutIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.075f
        val doorPath = Path().apply {
            moveTo(w * 0.42f, h * 0.15f)
            lineTo(w * 0.22f, h * 0.15f)
            cubicTo(w * 0.17f, h * 0.15f, w * 0.14f, h * 0.18f, w * 0.14f, h * 0.23f)
            lineTo(w * 0.14f, h * 0.77f)
            cubicTo(w * 0.14f, h * 0.82f, w * 0.17f, h * 0.85f, w * 0.22f, h * 0.85f)
            lineTo(w * 0.42f, h * 0.85f)
        }
        drawPath(doorPath, color = Danger, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = Danger, start = Offset(w * 0.38f, h * 0.5f), end = Offset(w * 0.86f, h * 0.5f), strokeWidth = strokeW, cap = StrokeCap.Round)
        val arrow = Path().apply {
            moveTo(w * 0.68f, h * 0.32f)
            lineTo(w * 0.88f, h * 0.5f)
            lineTo(w * 0.68f, h * 0.68f)
        }
        drawPath(arrow, color = Danger, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun WarningTriangleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.075f
        val tri = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.92f, h * 0.85f)
            lineTo(w * 0.08f, h * 0.85f)
            close()
        }
        drawPath(tri, color = Warning, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = Warning, start = Offset(w * 0.5f, h * 0.38f), end = Offset(w * 0.5f, h * 0.62f), strokeWidth = strokeW, cap = StrokeCap.Round)
        drawCircle(color = Warning, radius = w * 0.035f, center = Offset(w * 0.5f, h * 0.72f))
    }
}

@Composable
private fun TrashIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.07f
        drawLine(color = Danger, start = Offset(w * 0.15f, h * 0.27f), end = Offset(w * 0.85f, h * 0.27f), strokeWidth = strokeW, cap = StrokeCap.Round)
        val lid = Path().apply {
            moveTo(w * 0.35f, h * 0.27f)
            lineTo(w * 0.38f, h * 0.16f)
            cubicTo(w * 0.38f, h * 0.13f, w * 0.40f, h * 0.11f, w * 0.44f, h * 0.11f)
            lineTo(w * 0.56f, h * 0.11f)
            cubicTo(w * 0.60f, h * 0.11f, w * 0.62f, h * 0.13f, w * 0.62f, h * 0.16f)
            lineTo(w * 0.65f, h * 0.27f)
        }
        drawPath(lid, color = Danger, style = Stroke(width = strokeW * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        val body = Path().apply {
            moveTo(w * 0.22f, h * 0.32f)
            lineTo(w * 0.28f, h * 0.86f)
            cubicTo(w * 0.28f, h * 0.9f, w * 0.31f, h * 0.92f, w * 0.35f, h * 0.92f)
            lineTo(w * 0.65f, h * 0.92f)
            cubicTo(w * 0.69f, h * 0.92f, w * 0.72f, h * 0.9f, w * 0.72f, h * 0.86f)
            lineTo(w * 0.78f, h * 0.32f)
        }
        drawPath(body, color = Danger, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(color = Danger, start = Offset(w * 0.42f, h * 0.45f), end = Offset(w * 0.42f, h * 0.75f), strokeWidth = strokeW * 0.7f, cap = StrokeCap.Round)
        drawLine(color = Danger, start = Offset(w * 0.58f, h * 0.45f), end = Offset(w * 0.58f, h * 0.75f), strokeWidth = strokeW * 0.7f, cap = StrokeCap.Round)
    }
}

@Composable
private fun ProfileEnvelopeIcon(modifier: Modifier = Modifier, color: Color) {
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
        drawPath(flap, color = color, style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun ProfileGoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val d = size.minDimension
        val strokeW = d * 0.24f
        val topLeft = Offset(strokeW / 2f, strokeW / 2f)
        val arcSize = androidx.compose.ui.geometry.Size(d - strokeW, d - strokeW)

        drawArc(Color(0xFF4285F4), -45f, 90f, false, topLeft, arcSize, style = Stroke(width = strokeW))
        drawArc(Color(0xFF34A853), 45f, 90f, false, topLeft, arcSize, style = Stroke(width = strokeW))
        drawArc(Color(0xFFFBBC05), 135f, 90f, false, topLeft, arcSize, style = Stroke(width = strokeW))
        drawArc(Color(0xFFEA4335), 225f, 90f, false, topLeft, arcSize, style = Stroke(width = strokeW))
        drawRect(
            color = Color(0xFF4285F4),
            topLeft = Offset(d * 0.5f, d * 0.42f),
            size = androidx.compose.ui.geometry.Size(d * 0.42f, d * 0.16f)
        )
    }
}

@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 360, heightDp = 740)
@Composable
fun ProfileScreenPreview() {
    SiliGuideTheme {
        ProfileScreen(
            displayName = "Juan Dela Cruz",
            email = "juan.delacruz@email.com",
            authProvider = ProfileAuthProvider.EMAIL,
            onNavigateHome = {},
            onAddClick = {}
        )
    }
}

// Small phone preview
@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 320, heightDp = 640)
@Composable
fun ProfileScreenSmallPreview() {
    SiliGuideTheme {
        ProfileScreen(
            displayName = "Juan Dela Cruz",
            email = "juan.delacruz@email.com",
            authProvider = ProfileAuthProvider.EMAIL,
            onNavigateHome = {},
            onAddClick = {}
        )
    }
}

// Large phone preview
@Preview(showSystemUi = true, backgroundColor = 0xFFFFFFFF, widthDp = 412, heightDp = 915)
@Composable
fun ProfileScreenLargePreview() {
    SiliGuideTheme {
        ProfileScreen(
            displayName = "Juan Dela Cruz",
            email = "juan.delacruz@email.com",
            authProvider = ProfileAuthProvider.EMAIL,
            onNavigateHome = {},
            onAddClick = {}
        )
    }
}