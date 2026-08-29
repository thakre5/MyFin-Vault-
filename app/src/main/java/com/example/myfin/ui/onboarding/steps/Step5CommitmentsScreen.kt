package com.example.myfin.ui.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.onboarding.CoralAccent
import com.example.myfin.ui.onboarding.InitialCommitmentPreset
import com.example.myfin.ui.onboarding.components.OrbitalSyncClockCanvas
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.BorderLight
import com.example.myfin.ui.theme.CanvasLight
import com.example.myfin.ui.theme.CardWhite
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted
import java.util.Locale

@Composable
fun OnboardingStep5Commitments(
    commitments: List<InitialCommitmentPreset>,
    currencySymbol: String,
    onToggleCommitment: (Int) -> Unit,
    onUpdateAmount: (Int, String) -> Unit,
    onContinue: () -> Unit
) {
    val totalCommitted = commitments.filter { it.isSelected }.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text("Seed Fixed Commitments", fontSize = 19.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Pre-allocate recurring bills and SIPs bound to your master categories", fontSize = 11.5.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MONTHLY COMMITTED BUFFER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$currencySymbol${String.format(Locale.US, "%,.0f", totalCommitted)}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = CoralAccent)
                    }
                    OrbitalSyncClockCanvas(modifier = Modifier.size(42.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            commitments.forEachIndexed { idx, bill ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleCommitment(idx) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (bill.isSelected) CardWhite else CanvasLight,
                    border = BorderStroke(0.8.dp, if (bill.isSelected) AccentPurple else BorderLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Checkbox(
                                checked = bill.isSelected,
                                onCheckedChange = { onToggleCommitment(idx) },
                                colors = CheckboxDefaults.colors(checkedColor = AccentPurple)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(bill.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                Text("${bill.categoryName} • Due ${bill.defaultDueDay}th", fontSize = 10.5.sp, color = TextMuted)
                            }
                        }

                        if (bill.isSelected) {
                            OutlinedTextField(
                                value = bill.amountText,
                                onValueChange = { onUpdateAmount(idx, it.filter { ch -> ch.isDigit() }) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(110.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Complete & Seal Vault", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }
}
