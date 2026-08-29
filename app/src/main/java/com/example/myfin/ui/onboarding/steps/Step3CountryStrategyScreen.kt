package com.example.myfin.ui.onboarding.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.onboarding.CountryCurrencyMapping
import com.example.myfin.ui.onboarding.SupportedCountries
import com.example.myfin.ui.onboarding.TealPrimary
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.BorderLight
import com.example.myfin.ui.theme.CardWhite
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted

@Composable
fun OnboardingStep3CountryStrategy(
    selectedCountry: CountryCurrencyMapping,
    selectedStrategy: String,
    onSelectCountry: (CountryCurrencyMapping) -> Unit,
    onSelectStrategy: (String) -> Unit,
    onContinue: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text("Country & Currency Mapping", fontSize = 19.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Currencies and denomination formats are auto-bound by region", fontSize = 11.5.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SupportedCountries) { item ->
                    val isSelected = selectedCountry.countryName == item.countryName
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectCountry(item) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) AccentPurple.copy(alpha = 0.12f) else CardWhite,
                        border = BorderStroke(0.8.dp, if (isSelected) AccentPurple else BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.flagEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(item.countryName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = TextDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = if (isSelected) AccentPurple else BorderLight.copy(alpha = 0.5f)) {
                                Text(item.currencySymbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else TextDark, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Select Vault Architecture", fontSize = 17.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Choose how your capital is structured across accounts", fontSize = 11.5.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(12.dp))

            val is3Vault = selectedStrategy == "3-VAULT"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectStrategy("3-VAULT") },
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(1.2.dp, if (is3Vault) AccentPurple else BorderLight.copy(alpha = 0.6f)),
                shadowElevation = if (is3Vault) 4.dp else 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(AccentPurple.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("3-Vault Strategy", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextDark)
                        }
                        if (is3Vault) {
                            Surface(shape = RoundedCornerShape(6.dp), color = AccentPurple) {
                                Text("Recommended", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Partitions funds into Operating (daily spend), Commitments (AutoPay & fixed bills), and Fortress (untouchable emergency reserves).",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val isSimple = selectedStrategy == "SIMPLE"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectStrategy("SIMPLE") },
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(1.2.dp, if (isSimple) AccentPurple else BorderLight.copy(alpha = 0.6f)),
                shadowElevation = if (isSimple) 4.dp else 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(TealPrimary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Simple Mode", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextDark)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tracks all accounts and cards in a unified, flat liquidity balance without role restrictions or sweeps.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Confirm Architecture", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }
}
