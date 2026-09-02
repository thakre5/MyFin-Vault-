package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import coil.compose.SubcomposeAsyncImage
import com.example.myfin.data.SecurityManager
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun PinLockScreen(
    profileName: String = "Vault User",
    profileImageUri: String? = null,
    recoveryDob: String = "",
    isBiometricEnabled: Boolean = false,
    onUnlockSuccess: () -> Unit,
    onResetPasswordOnly: (String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val activity = context as? FragmentActivity
    val securityManager = remember { SecurityManager(context) }

    var enteredPin by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    // Multi-Step In-Place Password Recovery State
    var recoveryStep by remember { mutableIntStateOf(1) } // 1 = Verify DOB, 2 = Set New PIN
    var recoveryDobInput by remember { mutableStateOf("") }
    var newMasterPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }

    fun triggerBiometricPrompt() {
        if (activity != null && isBiometricEnabled && securityManager.canAuthenticateWithBiometrics(context)) {
            securityManager.showBiometricPrompt(
                activity = activity,
                onSuccess = { onUnlockSuccess() },
                onError = { /* Fallback to keypad */ }
            )
        }
    }

    // Auto-launch biometric fingerprint scanner on screen entry
    LaunchedEffect(Unit) {
        delay(300L)
        triggerBiometricPrompt()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        // Ambient Purple Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AccentPurple.copy(alpha = 0.14f),
                            AccentPurple.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Profile & Lock Emblem Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                Surface(
                    modifier = Modifier.size(76.dp),
                    shape = CircleShape,
                    color = CardWhite,
                    border = BorderStroke(2.dp, AccentPurple.copy(alpha = 0.35f)),
                    shadowElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModel = remember(profileImageUri) {
                            if (!profileImageUri.isNullOrBlank()) {
                                File(profileImageUri).takeIf { it.exists() } ?: profileImageUri
                            } else null
                        }

                        if (imageModel != null) {
                            SubcomposeAsyncImage(
                                model = imageModel,
                                contentDescription = "Profile Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = {
                                    Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        shape = CircleShape,
                                        color = AccentPurple.copy(alpha = 0.12f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = profileName.take(1).uppercase().ifBlank { "M" },
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.Black,
                                                color = AccentPurple
                                            )
                                        }
                                    }
                                }
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = AccentPurple.copy(alpha = 0.12f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "Vault Locked",
                                        tint = AccentPurple,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = profileName.ifBlank { "MyFin Vault" },
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = TextDark,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Enter Master PIN to decrypt ledger",
                    fontSize = 12.5.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Animated PIN Dot Indicators (Standard 4 to 6 dot layout)
                val dotCount = if (enteredPin.length > 4) 6 else 4
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(dotCount) { index ->
                        val isFilled = index < enteredPin.length
                        val dotSize by animateDpAsState(
                            targetValue = if (isFilled) 15.dp else 13.dp,
                            animationSpec = tween(150),
                            label = "dotSize"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isFilled) AccentPurple else BorderLight.copy(alpha = 0.9f),
                            animationSpec = tween(150),
                            label = "dotColor"
                        )

                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }

            // Keypad Grid
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "DEL" -> {
                                    IconButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                        },
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                                            contentDescription = "Delete",
                                            tint = TextDark,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                "BIO" -> {
                                    if (isBiometricEnabled) {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                triggerBiometricPrompt()
                                            },
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Fingerprint,
                                                contentDescription = "Unlock with Biometrics",
                                                tint = AccentPurple,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(64.dp))
                                    }
                                }
                                else -> {
                                    Surface(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .shadow(2.dp, CircleShape)
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                if (enteredPin.length < 6) {
                                                    val newPin = enteredPin + key
                                                    enteredPin = newPin

                                                    // Test PIN validity once length reaches 4 or higher
                                                    if (newPin.length >= 4 && securityManager.verifyPin(newPin)) {
                                                        enteredPin = ""
                                                        onUnlockSuccess()
                                                    } else if (newPin.length >= 6) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
                                                        enteredPin = ""
                                                    }
                                                }
                                            },
                                        shape = CircleShape,
                                        color = CardWhite,
                                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = key,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 22.sp,
                                                color = TextDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Forgot PIN Recovery
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    recoveryStep = 1
                    recoveryDobInput = ""
                    newMasterPinInput = ""
                    confirmNewPinInput = ""
                    showResetDialog = true
                },
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Text(
                    text = "Forgot Master PIN?",
                    fontSize = 13.sp,
                    color = AccentPurple,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Non-destructive in-place password reset dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = {
                    showResetDialog = false
                    enteredPin = ""
                },
                icon = {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = if (recoveryStep == 1) "Verify Recovery Key" else "Set New Master PIN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .imePadding()
                    ) {
                        if (recoveryStep == 1) {
                            Text(
                                text = "Enter the Date of Birth bound during setup to verify ownership. Your account data will remain 100% intact.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                            OutlinedTextField(
                                value = recoveryDobInput,
                                onValueChange = { recoveryDobInput = it },
                                placeholder = { Text("DD/MM/YYYY or YYYY-MM-DD", fontSize = 12.5.sp) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                        } else {
                            Text(
                                text = "Choose a new Master PIN to secure your vault.",
                                fontSize = 12.sp,
                                color = TextMuted,
                                lineHeight = 16.sp
                            )
                            OutlinedTextField(
                                value = newMasterPinInput,
                                onValueChange = { if (it.length <= 6) newMasterPinInput = it.filter { ch -> ch.isDigit() } },
                                placeholder = { Text("New Master PIN (4-6 digits)", fontSize = 12.5.sp) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                            OutlinedTextField(
                                value = confirmNewPinInput,
                                onValueChange = { if (it.length <= 6) confirmNewPinInput = it.filter { ch -> ch.isDigit() } },
                                placeholder = { Text("Confirm New Master PIN", fontSize = 12.5.sp) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (recoveryStep == 1) {
                                val isDobMatched = securityManager.verifyRecoveryDob(recoveryDobInput) ||
                                        (recoveryDob.isNotBlank() && recoveryDobInput.replace("[^0-9]".toRegex(), "") == recoveryDob.replace("[^0-9]".toRegex(), ""))

                                if (isDobMatched) {
                                    recoveryStep = 2
                                } else {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    Toast.makeText(context, "Recovery Date of Birth does not match", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                if (newMasterPinInput.length < 4) {
                                    Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                                } else if (newMasterPinInput != confirmNewPinInput) {
                                    Toast.makeText(context, "PINs do not match", Toast.LENGTH_SHORT).show()
                                } else {
                                    securityManager.setPin(newMasterPinInput)
                                    onResetPasswordOnly(newMasterPinInput)
                                    Toast.makeText(context, "Master PIN updated successfully", Toast.LENGTH_SHORT).show()
                                    showResetDialog = false
                                    enteredPin = ""
                                    onUnlockSuccess()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (recoveryStep == 1) "Verify DOB" else "Update PIN & Unlock",
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showResetDialog = false
                        enteredPin = ""
                    }) {
                        Text("Cancel", color = TextMuted)
                    }
                }
            )
        }
    }
}
