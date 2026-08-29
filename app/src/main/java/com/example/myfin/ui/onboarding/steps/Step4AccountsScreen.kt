package com.example.myfin.ui.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.onboarding.InitialAccountSetup
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.BorderLight
import com.example.myfin.ui.theme.CardWhite
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted
import java.util.Locale

@Composable
fun OnboardingStep4Accounts(
    accounts: List<InitialAccountSetup>,
    currencySymbol: String,
    onUpdateAccountBalance: (Int, String) -> Unit,
    onContinue: () -> Unit
) {
    val totalOpening = accounts.sumOf { it.initialBalanceText.toDoubleOrNull() ?: 0.0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text("Opening Liquidity Balances", fontSize = 19.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Enter the current balances in your initial vault accounts", fontSize = 11.5.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TOTAL OPENING NET WORTH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$currencySymbol${String.format(Locale.US, "%,.2f", totalOpening)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextDark)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            accounts.forEachIndexed { idx, acc ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CardWhite,
                    border = BorderStroke(0.7.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                            Text(acc.defaultType, fontSize = 10.5.sp, color = AccentPurple)
                        }

                        OutlinedTextField(
                            value = acc.initialBalanceText,
                            onValueChange = { onUpdateAccountBalance(idx, it.filter { ch -> ch.isDigit() || ch == '.' }) },
                            label = { Text("Balance ($currencySymbol)", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(135.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
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
                Text("Lock Opening Balances", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }
}
