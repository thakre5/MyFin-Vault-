package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.*

@Composable
fun MultiStepOnboardingFlow(
    viewModel: BudgetViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }

    var displayName by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("2000-03-21") }
    var baseIncomeText by remember { mutableStateOf("") }
    var fortressThresholdText by remember { mutableStateOf("100000") }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isBiometricEnabled by remember { mutableStateOf(false) }

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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Header & Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MyFin Vault Setup",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = TextDark
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AccentPurpleLight
                    ) {
                        Text(
                            text = "Step $currentStep of 3",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = AccentPurple,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                AnimatedContent(targetState = currentStep, label = "onboardingStep") { step ->
                    when (step) {
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Personal Profile", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Text("Let's personalize your offline ledger with your identity and recovery baseline.", fontSize = 12.sp, color = TextMuted)

                                OutlinedTextField(
                                    value = displayName,
                                    onValueChange = { displayName = it },
                                    label = { Text("Display Name") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = AccentPurple) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                OutlinedTextField(
                                    value = dob,
                                    onValueChange = { dob = it },
                                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }

                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Cashflow Targets", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Text("Configure your baseline recurring inflow and emergency fortress reserve.", fontSize = 12.sp, color = TextMuted)

                                OutlinedTextField(
                                    value = baseIncomeText,
                                    onValueChange = { baseIncomeText = it },
                                    label = { Text("Expected Monthly Inflow (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                OutlinedTextField(
                                    value = fortressThresholdText,
                                    onValueChange = { fortressThresholdText = it },
                                    label = { Text("Fortress Liquidity Target (₹)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )
                            }
                        }

                        3 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Text("Hardware Security", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Text("Create your 4-6 digit vault PIN and enable optional biometric hardware protection.", fontSize = 12.sp, color = TextMuted)

                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = { if (it.length <= 6) pin = it },
                                    label = { Text("Master PIN (4-6 digits)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentPurple) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                OutlinedTextField(
                                    value = confirmPin,
                                    onValueChange = { if (it.length <= 6) confirmPin = it },
                                    label = { Text("Confirm PIN") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = AccentPurple) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = CardWhite,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Enable Fingerprint/Face Unlock", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                        Switch(
                                            checked = isBiometricEnabled,
                                            onCheckedChange = { isBiometricEnabled = it },
                                            colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    TextButton(onClick = { currentStep-- }) {
                        Text("Back", color = TextDark, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Button(
                    onClick = {
                        when (currentStep) {
                            1 -> {
                                if (displayName.isBlank()) {
                                    Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                                } else {
                                    currentStep = 2
                                }
                            }
                            2 -> {
                                currentStep = 3
                            }
                            3 -> {
                                if (pin.length < 4) {
                                    Toast.makeText(context, "PIN must be at least 4 digits", Toast.LENGTH_SHORT).show()
                                } else if (pin != confirmPin) {
                                    Toast.makeText(context, "PINs do not match", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.completeOnboarding(
                                        displayName = displayName.trim(),
                                        dob = dob.trim(),
                                        baseIncome = baseIncomeText.toDoubleOrNull() ?: 0.0,
                                        fortressThreshold = fortressThresholdText.toDoubleOrNull() ?: 100000.0,
                                        masterPin = pin.trim(),
                                        isBiometricEnabled = isBiometricEnabled
                                    )
                                    onComplete()
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = if (currentStep == 3) "Complete Setup" else "Continue",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (currentStep == 3) Icons.Default.Check else Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
