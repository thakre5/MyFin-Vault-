package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.*

enum class TimeRangeFilter(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year")
}

enum class VelocityRange(val label: String) {
    W("W"),
    M("M"),
    THREE_M("3 M"),
    SIX_M("6 M"),
    Y("Y")
}

data class DailySpendData(
    val dayLabel: String,
    val essentialAmount: Double,
    val discretionaryAmount: Double,
    val totalAmount: Double
)

data class TrajectoryPointData(
    val stepLabel: String,
    val actualCumulative: Double,
    val targetCumulative: Double
)

data class ChartMetricInfo(
    val title: String,
    val subtitle: String,
    val formula: String,
    val breakdown: String,
    val visualElements: List<Pair<String, String>> = emptyList(),
    val advice: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartMetricInfoBottomSheet(
    info: ChartMetricInfo,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(info.title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = TextDark)
                    Text(info.subtitle, fontSize = 11.5.sp, color = TextMuted)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Active Reading & Contribution", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(info.breakdown, fontSize = 12.5.sp, color = TextDark, lineHeight = 17.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Text("Mathematical Formula", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = AccentPurple.copy(alpha = 0.08f),
                border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.25f))
            ) {
                Text(
                    text = info.formula,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = AccentPurple,
                    modifier = Modifier.padding(10.dp)
                )
            }

            if (info.visualElements.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text("Visual Elements Explained", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    info.visualElements.forEach { (tag, desc) ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(AccentPurple))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(desc, fontSize = 10.5.sp, color = TextMuted, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CanvasLight,
                border = BorderStroke(0.6.dp, BorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(info.advice, fontSize = 11.sp, color = TextDark, lineHeight = 15.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StrategyArchitectureBottomSheet(
    vaultMode: String,
    onDismiss: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val is3Vault = !vaultMode.equals("SIMPLE", ignoreCase = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Vault Strategy Architecture", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (is3Vault) AccentPurple.copy(alpha = 0.12f) else TextDark.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = if (is3Vault) "3-Vault Active" else "Simple Mode Active",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (is3Vault) AccentPurple else TextDark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (is3Vault) {
                    "Your wealth is systematically partitioned across structured financial tiers to prevent accidental overspending."
                } else {
                    "Your wealth is managed as a unified, flat liquidity pool across all connected bank cards and wallets."
                },
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (is3Vault) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    StrategyTierInfoRow(
                        icon = Icons.Default.AccountBalance,
                        color = AccentPurple,
                        title = "Operating Vault Tier",
                        desc = "Covers everyday groceries and variable daily lifestyle spend."
                    )
                    StrategyTierInfoRow(
                        icon = Icons.Default.CreditCard,
                        color = SoftRed,
                        title = "Commitments Vault Tier",
                        desc = "Dedicated lockbox protecting AutoPay bills and EMI obligations."
                    )
                    StrategyTierInfoRow(
                        icon = Icons.Default.Security,
                        color = SoftTeal,
                        title = "Fortress Vault Tier",
                        desc = "Liquid emergency reserve safeguarding against unforeseen life events."
                    )
                    StrategyTierInfoRow(
                        icon = Icons.Default.Payments,
                        color = SoftGreen,
                        title = "Physical Cash Tier",
                        desc = "Physical wallet buffer for cash transactions and petty expenses."
                    )
                }
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = CanvasLight,
                    border = BorderStroke(0.6.dp, BorderLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Flat Liquidity Structure", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        Text(
                            text = "Simple Mode aggregates all accounts into a single total net liquidity figure without reserve rules, strategic sweeps, or role badges.",
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onDismiss()
                    onNavigateToSettings()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Customize Strategy in Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StrategyTierInfoRow(
    icon: ImageVector,
    color: Color,
    title: String,
    desc: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextDark)
                Text(desc, fontSize = 10.5.sp, color = TextMuted, lineHeight = 14.sp)
            }
        }
    }
}
