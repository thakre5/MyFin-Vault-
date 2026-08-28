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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.SecurityManager
import com.example.myfin.ui.theme.*

@Composable
fun PinLockScreen(
    correctPin: String,
    recoveryDob: String,
    onUnlockSuccess: () -> Unit,
    onEmergencyReset: () -> Unit
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var showEmergencyResetDialog by remember { mutableStateOf(false) }
    var recoveryDobInput by remember { mutableStateOf("") }

    val securityManager = remember { SecurityManager(context) }

    fun checkPin(pin: String) {
        if (securityManager.verifyPin(pin)) {
            onUnlockSuccess()
        } else {
            Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show()
            enteredPin = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        // Subtle Top Purple Ambient Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AccentPurple.copy(alpha = 0.12f),
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
            // Hero Lock Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 36.dp)
            ) {
                Surface(
                    modifier = Modifier.size(70.dp),
                    shape = CircleShape,
                    color = AccentPurple.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, AccentPurple.copy(alpha = 0.25f)),
                    shadowElevation = 2.dp
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

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "MyFin Vault Locked",
                    fontWeight = FontWeight.Black,
                    fontSize = 21.sp,
                    color = TextDark,
                    letterSpacing = (-0.3).sp
                )

                Spacer(modifier = Modifier.height(5.dp))

                Text(
                    text = "Enter your Master PIN to decrypt ledger",
                    fontSize = 12.5.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Animated PIN Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
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

            // Numeric Keypad
            Column(
                modifier = Modifier.fillMaxWidth(0.85f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("RESET", "0", "DEL")
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
                                "RESET" -> {
                                    TextButton(
                                        onClick = { showEmergencyResetDialog = true },
                                        modifier = Modifier.size(64.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(
                                            text = "Forgot",
                                            fontSize = 11.5.sp,
                                            color = AccentPurple,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                else -> {
                                    Surface(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .shadow(2.dp, CircleShape)
                                            .clickable {
                                                if (enteredPin.length < 6) {
                                                    enteredPin += key
                                                    if (enteredPin.length >= 4 && enteredPin.length >= correctPin.length) {
                                                        checkPin(enteredPin)
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

            Spacer(modifier = Modifier.height(10.dp))
        }

        // Emergency Reset Dialog via DOB Verification
        if (showEmergencyResetDialog) {
            AlertDialog(
                onDismissRequest = { showEmergencyResetDialog = false },
                title = { Text("Emergency Vault Recovery", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Enter your Date of Birth configured during onboarding to verify identity and reset your Master PIN.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                        OutlinedTextField(
                            value = recoveryDobInput,
                            onValueChange = { recoveryDobInput = it },
                            label = { Text("DOB (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = BorderLight
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (securityManager.verifyDob(recoveryDobInput)) {
                                Toast.makeText(context, "Recovery verified. Resetting vault credentials...", Toast.LENGTH_LONG).show()
                                onEmergencyReset()
                                showEmergencyResetDialog = false
                            } else {
                                Toast.makeText(context, "Date of Birth verification failed", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Verify & Reset", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmergencyResetDialog = false }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }
    }
}
