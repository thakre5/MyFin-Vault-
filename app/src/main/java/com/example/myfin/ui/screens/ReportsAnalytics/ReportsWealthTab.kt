package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.AccountBalanceResult
import com.example.myfin.ui.theme.*
import java.util.Locale

@Composable
fun ReportsWealthTab(
    userProfileCurrency: String,
    vaultMode: String,
    onOpenStrategyInfo: () -> Unit,
    totalInvestments: Double,
    realizableNetWorth: Double,
    monthlyBurnRate: Double,
    accounts: List<AccountBalanceResult>,
    isDiscreet: Boolean,
    onOpenMetricInfo: (ChartMetricInfo) -> Unit
) {
    val totalLiquid = remember(accounts) { accounts.sumOf { it.currentBalance } }
    val runwayMonths = remember(totalLiquid, monthlyBurnRate) {
        if (monthlyBurnRate > 0) (totalLiquid / monthlyBurnRate) else if (totalLiquid > 0) 99.0 else 0.0
    }
    val is3Vault = !vaultMode.equals("SIMPLE", ignoreCase = true)

    val bankAmount = remember(accounts) {
        accounts.filter { !it.accountType.equals("Cash", true) }.sumOf { it.currentBalance }
    }
    val cashAmount = remember(accounts) {
        accounts.filter { it.accountType.equals("Cash", true) }.sumOf { it.currentBalance }
    }

    // =========================================================
    // CANONICAL UNIFIED METRIC DEFINITIONS (GRAPH & TITLE SYNC)
    // =========================================================

    val netCapitalTrajectoryInfo = remember(userProfileCurrency, totalLiquid, totalInvestments, realizableNetWorth) {
        ChartMetricInfo(
            title = "Net Capital Trajectory & Silhouette",
            subtitle = "Liquid Reserves vs. Compounding Wealth Assets",
            formula = "Realizable Net Worth = Liquid Reserves + Active Investments + Receivables",
            breakdown = "Liquid Vaults: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalLiquid)}\nInvested Portfolio: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalInvestments)}\nRealizable Net Worth: $userProfileCurrency${String.format(Locale.US, "%,.0f", realizableNetWorth)}",
            visualElements = listOf(
                "Violet Silhouette Layer" to "Liquid defensive buffer held across bank and cash accounts.",
                "Teal Silhouette Layer" to "Compounding market portfolio stock (Mutual funds, gold, SIPs)."
            ),
            advice = "Visualizes your liquid defensive buffer alongside appreciating capital. Aim to grow the teal compounding layer faster than the liquid baseline."
        )
    }

    val capitalDistributionInfo = remember(userProfileCurrency, bankAmount, cashAmount, totalInvestments) {
        ChartMetricInfo(
            title = "Capital Allocation Bubbles",
            subtitle = "Three-Tier Wealth Balance Distribution",
            formula = "Total Capital = Bank Reserves + Cash Buffer + Invested Assets",
            breakdown = "Bank Reserves: $userProfileCurrency${String.format(Locale.US, "%,.0f", bankAmount)}\nCash Buffer: $userProfileCurrency${String.format(Locale.US, "%,.0f", cashAmount)}\nInvested Portfolio: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalInvestments)}",
            visualElements = listOf(
                "Purple Bubble" to "Bank balances held in operating and commitments accounts.",
                "Teal Bubble" to "Long-term investment assets, mutual funds, and wealth accounts.",
                "Green Bubble" to "Physical cash and petty expense buffers."
            ),
            advice = "Maintain small, focused cash reserves while routing excess bank liquidity into compounding investments."
        )
    }

    val emergencyRunwayInfo = remember(userProfileCurrency, totalLiquid, monthlyBurnRate, runwayMonths) {
        ChartMetricInfo(
            title = "Emergency Buffer Runway",
            subtitle = "Financial Survival Duration at Baseline Burn",
            formula = "Runway Months = Liquid Vaults ÷ Average Monthly Burn",
            breakdown = "Liquid Reserves: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalLiquid)}\nMonthly Burn Rate: $userProfileCurrency${String.format(Locale.US, "%,.0f", monthlyBurnRate)}/mo\nRunway Buffer: ${String.format(Locale.US, "%.1f", runwayMonths)} Months",
            visualElements = listOf(
                "Runway Counter" to "Months your liquid reserves can fund full living expenses without any new income."
            ),
            advice = "Maintaining at least a 6-month buffer covers unexpected career or business interruptions without forcing distress asset liquidations."
        )
    }

    val vaultsStatusInfo = remember(accounts.size, is3Vault) {
        ChartMetricInfo(
            title = if (is3Vault) "Strategic Vault Architecture" else "Vaults Liquidity Status",
            subtitle = if (is3Vault) "Operating, Commitments & Fortress Tiers" else "Universal Account Balances",
            formula = "Spendable Surplus = Current Balance - MAB Buffer",
            breakdown = "Monitoring ${accounts.size} active accounts across all liquidity tiers.",
            visualElements = listOf(
                "Tier Badge" to "Indicates whether the account funds daily living, bills, or emergency reserves.",
                "MAB Buffer" to "Minimum required account balance protected against overdraft charges."
            ),
            advice = "Keep minimum balance floors protected and sweep uncommitted surpluses to your Fortress tier."
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 140.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // 1. Net Capital Trajectory Header & Area Chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMetricInfo(netCapitalTrajectoryInfo) },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Net Capital Trajectory",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Liquid Reserves + Invested Portfolio",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenStrategyInfo() },
                shape = RoundedCornerShape(10.dp),
                color = if (is3Vault) AccentPurple.copy(alpha = 0.12f) else CanvasLight,
                border = BorderStroke(0.7.dp, if (is3Vault) AccentPurple else BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (is3Vault) Icons.Default.Layers else Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (is3Vault) AccentPurple else TextDark,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (is3Vault) "3-Vault" else "Simple",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (is3Vault) AccentPurple else TextDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LayeredMountainAreaChartCanvas(
            liquidTotal = totalLiquid,
            assetTotal = totalInvestments,
            currencySymbol = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = { onOpenMetricInfo(netCapitalTrajectoryInfo) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 2. Capital Distribution Header & Bubbles
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMetricInfo(capitalDistributionInfo) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Capital Distribution",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        ThreeBubbleAllocationCanvas(
            bankAmount = bankAmount,
            cashAmount = cashAmount,
            assetAmount = totalInvestments,
            currency = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = { onOpenMetricInfo(capitalDistributionInfo) }
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 3. Emergency Buffer Runway Header & Metric
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMetricInfo(emergencyRunwayInfo) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Emergency Buffer Runway",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpenMetricInfo(emergencyRunwayInfo) }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isDiscreet) "•• Months" else "${String.format(Locale.US, "%.1f", runwayMonths)} Months",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = SoftTeal
                )
                Text("Living expenses secured in vaults", fontSize = 11.5.sp, color = TextMuted)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftTeal.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (runwayMonths >= 6) "Healthy Cushion" else "Building Buffer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = SoftTeal
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 4. Strategic Vaults Status Header & Itemized List
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenMetricInfo(vaultsStatusInfo) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (is3Vault) "Strategic Vaults Status" else "Vaults Liquidity Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        accounts.forEach { acc ->
            val spendableSurplus = (acc.currentBalance - acc.minBalance).coerceAtLeast(0.0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "${acc.accountName} Vault Audit",
                                subtitle = "${acc.accountType} Tier Account Details",
                                formula = "Spendable Surplus = Current Balance - Minimum Account Balance",
                                breakdown = "Current Balance: $userProfileCurrency${String.format(Locale.US, "%,.0f", acc.currentBalance)}\nMAB Protected Buffer: $userProfileCurrency${String.format(Locale.US, "%,.0f", acc.minBalance)}\nFree Spendable Surplus: $userProfileCurrency${String.format(Locale.US, "%,.0f", spendableSurplus)}",
                                visualElements = listOf(
                                    "MAB Buffer" to "Required minimum balance protected against overdraft charges.",
                                    "Free Surplus" to "Liquid funds freely available for safe spending or sweeping."
                                ),
                                advice = "Only spendable surplus above the MAB floor is factored into Safe-to-Spend algorithms."
                            )
                        )
                    }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (acc.accountType.equals("Cash", true)) SoftTeal.copy(alpha = 0.12f) else AccentPurple.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (acc.accountType.equals("Cash", true)) Icons.Default.Payments else Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (acc.accountType.equals("Cash", true)) SoftTeal else AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(acc.accountName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (is3Vault) "${acc.accountType} Tier" else "${acc.accountType} Vault", fontSize = 11.sp, color = TextMuted)
                            if (acc.minBalance > 0.0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("• MAB: $userProfileCurrency${String.format(Locale.US, "%,.0f", acc.minBalance)}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isDiscreet) "••••" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", acc.currentBalance)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.5.sp,
                        color = TextDark
                    )
                    if (acc.minBalance > 0.0) {
                        Text(
                            text = if (isDiscreet) "Surplus: ••••" else "Surplus: $userProfileCurrency${String.format(Locale.US, "%,.0f", spendableSurplus)}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftTeal
                        )
                    } else {
                        Text(
                            text = if (isDiscreet) "Base: ••••" else "Base: $userProfileCurrency${acc.startingBalance.toInt()}",
                            fontSize = 10.5.sp,
                            color = TextMuted
                        )
                    }
                }
            }
            HorizontalDivider(color = BorderLight.copy(alpha = 0.35f), thickness = 0.7.dp)
        }
    }
}

@Composable
private fun LayeredMountainAreaChartCanvas(
    liquidTotal: Double,
    assetTotal: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().clickable { onOpenInfo() }) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val w = size.width
            val h = size.height

            val totalWealth = (liquidTotal + assetTotal).coerceAtLeast(1.0)
            val liquidShare = (liquidTotal / totalWealth).toFloat().coerceIn(0.2f, 0.8f)

            val p1 = Path().apply {
                moveTo(0f, h * (1f - (liquidShare * 0.6f + 0.1f)))
                cubicTo(w * 0.3f, h * (1f - (liquidShare * 0.7f + 0.05f)), w * 0.6f, h * (1f - (liquidShare * 0.85f)), w, h * (1f - liquidShare))
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                p1,
                brush = Brush.verticalGradient(listOf(AccentPurple.copy(alpha = 0.45f), AccentPurple.copy(alpha = 0.05f)))
            )

            val p2 = Path().apply {
                moveTo(0f, h * 0.85f)
                cubicTo(w * 0.35f, h * 0.70f, w * 0.7f, h * 0.60f, w, h * (1f - (liquidShare * 0.5f)))
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                p2,
                brush = Brush.verticalGradient(listOf(SoftTeal.copy(alpha = 0.55f), SoftTeal.copy(alpha = 0.05f)))
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentPurple))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Liquid Cash", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Investments", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ThreeBubbleAllocationCanvas(
    bankAmount: Double,
    cashAmount: Double,
    assetAmount: Double,
    currency: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onOpenInfo() },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(AccentPurple.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Banks", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                val label = if (isDiscreet) "••••" else if (bankAmount >= 1000) "$currency${(bankAmount / 1000).toInt()}k" else "$currency${bankAmount.toInt()}"
                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(SoftTeal.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Portfolio", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                val label = if (isDiscreet) "••••" else if (assetAmount >= 1000) "$currency${(assetAmount / 1000).toInt()}k" else "$currency${assetAmount.toInt()}"
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(SoftGreen.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cash", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                val label = if (isDiscreet) "••••" else if (cashAmount >= 1000) "$currency${(cashAmount / 1000).toInt()}k" else "$currency${cashAmount.toInt()}"
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
