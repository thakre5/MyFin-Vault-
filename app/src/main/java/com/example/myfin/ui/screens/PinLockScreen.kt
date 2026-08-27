package com.example.myfin.ui.screens

import android.widget.Toast
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
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AccentPurpleLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Vault Locked",
                        tint = AccentPurple,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MyFin Vault Locked",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter your Master PIN to decrypt ledger",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // PIN Dot Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) AccentPurple else BorderLight)
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
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "DEL" -> {
                                    IconButton(
                                        onClick = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) },
                                        modifier = Modifier.size(64.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Delete", tint = TextDark)
                                    }
                                }
                                "RESET" -> {
                                    TextButton(
                                        onClick = { showEmergencyResetDialog = true },
                                        modifier = Modifier.size(64.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Forgot", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
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
                                        color = CardWhite
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

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Emergency Reset Dialog via DOB Verification
        if (showEmergencyResetDialog) {
            AlertDialog(
                onDismissRequest = { showEmergencyResetDialog = false },
                title = { Text("Emergency Vault Recovery", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Enter your Date of Birth configured during onboarding to verify identity and reset your Master PIN.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                        OutlinedTextField(
                            value = recoveryDobInput,
                            onValueChange = { recoveryDobInput = it },
                            label = { Text("DOB (YYYY-MM-DD)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
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
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                    ) {
                        Text("Verify & Reset")
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
