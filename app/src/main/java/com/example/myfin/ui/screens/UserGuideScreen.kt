package com.example.myfin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.*

@Composable
fun UserGuideScreen(
    onBack: () -> Unit
) {
    val bottomNavPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CardWhite)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                }

                Text("3-Bank Architecture Guide", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)

                Spacer(modifier = Modifier.size(38.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp + bottomNavPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    GuideCard(
                        title = "1. Operating Vault (Day-to-Day)",
                        description = "Houses your active spending allowance. Safe-to-Spend metrics are derived directly from this buffer to prevent lifestyle creep and accidental overdrafts."
                    )
                }

                item {
                    GuideCard(
                        title = "2. Commitments Vault (AutoPay & Bills)",
                        description = "Quarantines recurring fixed liabilities (Rent, EMIs, Utilities, Subscriptions, Insurance) on salary day so mandatory obligations are always 100% pre-funded."
                    )
                }

                item {
                    GuideCard(
                        title = "3. Fortress Vault (Emergency Reserves & Wealth)",
                        description = "Shields long-term capital, mutual funds, gold, and multi-month emergency safety nets. Untouchable for daily discretionary spending."
                    )
                }

                item {
                    GuideCard(
                        title = "4. Safe to Spend Formula",
                        description = "Safe to Spend = Planned Inflow - (Fixed Commitments + Asset/SIP Allocations) - Actual Discretionary Outflow."
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideCard(title: String, description: String) {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(1.5.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AccentPurple)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, fontSize = 12.sp, color = TextDark, lineHeight = 18.sp)
        }
    }
}
