package com.example.myfin.ui.onboarding.steps

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.onboarding.components.OnboardingDateVisualTransformation
import com.example.myfin.ui.onboarding.components.SecurityRadarPulseCanvas
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.BorderLight
import com.example.myfin.ui.theme.CardWhite
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted

@Composable
fun OnboardingStep2PinSecurity(
    pinEntryPhase: Int,
    masterPin: String,
    confirmPin: String,
    rawDobDigits: String,
    isBiometricEnabled: Boolean,
    onDigitPress: (String) -> Unit,
    onDeleteDigit: () -> Unit,
    onDobChange: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onProceedToConfirm: () -> Unit,
    onValidateAndNext: () -> Unit
) {
    val currentPinString = if (pinEntryPhase == 1) masterPin else confirmPin
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SecurityRadarPulseCanvas(
            modifier = Modifier
                .size(68.dp)
                .padding(bottom = 6.dp)
        )

        Text(
            text = if (pinEntryPhase == 1) "Create Master PIN" else "Confirm PIN & Recovery",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = TextDark
        )
        Text(
            text = if (pinEntryPhase == 1) "4-digit offline lock protecting your personal vault" else "Re-enter PIN and set local recovery credentials",
            fontSize = 11.5.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { idx ->
                val isFilled = idx < currentPinString.length
                val scale by animateFloatAsState(if (isFilled) 1.25f else 1.0f, spring(dampingRatio = 0.6f), label = "dotScale")
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(if (isFilled) AccentPurple else BorderLight)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pinEntryPhase == 2) {
            OutlinedTextField(
                value = rawDobDigits,
                onValueChange = onDobChange,
                label = { Text("Recovery Date of Birth (DD / MM / YYYY)", fontSize = 11.sp) },
                visualTransformation = OnboardingDateVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CardWhite,
                border = BorderStroke(0.6.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Biometric Unlock", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = onBiometricToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (key.isNotBlank()) CardWhite else Color.Transparent)
                                .clickable(enabled = key.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (key == "DEL") onDeleteDigit() else onDigitPress(key)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "DEL") {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = TextDark, modifier = Modifier.size(18.dp))
                            } else {
                                Text(key, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextDark)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { if (pinEntryPhase == 1) onProceedToConfirm() else onValidateAndNext() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text(if (pinEntryPhase == 1) "Confirm PIN" else "Save Security Lock", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}
