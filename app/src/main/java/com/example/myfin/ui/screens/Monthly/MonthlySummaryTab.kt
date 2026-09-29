package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionType
import com.example.myfin.data.UserProfile
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.CategoryPerformance
import com.example.myfin.ui.FilterCriteria
import com.example.myfin.ui.MonthlyUiState
import com.example.myfin.ui.components.NowBarAlert
import com.example.myfin.ui.components.SpendingSparkline
import com.example.myfin.ui.components.VaultNowBar
import com.example.myfin.ui.theme.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round

@Composable
fun MonthlySummaryTab(
    viewModel: BudgetViewModel,
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    filterCriteria: FilterCriteria,
    isDiscreetMode: Boolean,
    isCurrentMonth: Boolean,
    isPastMonth: Boolean,
    dismissedWaterfallMonth: Int,
    dismissedSweepMonth: Int,
    operatingAccountName: String,
    commitmentsAccountName: String,
    fortressAccountName: String,
    onOpenStsInfo: () -> Unit,
    onOpenThreePillarInfo: () -> Unit,
    onOpenFortressInfo: () -> Unit,
    onOpenBalanceFlowInfo: (Int) -> Unit,
    onOpenTransferSheet: () -> Unit,
    onDismissWaterfall: () -> Unit,
    onDismissSweep: () -> Unit,
    onNavigateToLedgerWithFilter: (TransactionType) -> Unit,
    onNavigateToCommitments: () -> Unit
) {
    val context = LocalContext.current
    var selectedMatrixType by remember { mutableStateOf(TransactionType.EXPENSE) }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    val isHealthy = uiState.metrics.safeToSpend > 0

    val paydayPlan = uiState.paydaySuggestion
    val showWaterfallPrompt = remember(paydayPlan, uiState.selectedMonth, dismissedWaterfallMonth, isCurrentMonth) {
        isCurrentMonth && paydayPlan != null && (paydayPlan.toCommitments > 0.0 || paydayPlan.totalToFortress > 0.0) && dismissedWaterfallMonth != uiState.selectedMonth
    }

    val monthEndSweepPlan = uiState.monthEndSweepSuggestion
    val showMonthEndSweepPrompt = remember(monthEndSweepPlan, uiState.selectedMonth, dismissedSweepMonth, isCurrentMonth) {
        isCurrentMonth && monthEndSweepPlan != null && monthEndSweepPlan.sweepAmount > 0.0 && dismissedSweepMonth != uiState.selectedMonth
    }

    // Consolidated Samsung Now Bar Alerts (Shortfall, Rollover, Waterfall, Month-End Sweep)
    val nowBarAlerts = remember(
        uiState.commitmentsShortfall,
        uiState.isRolloverBannerVisible,
        uiState.rolloverBannerMessage,
        showWaterfallPrompt,
        paydayPlan,
        showMonthEndSweepPrompt,
        monthEndSweepPlan,
        userProfile.currencySymbol
    ) {
        buildList {
            // 1. Critical: Commitments Shortfall
            if (uiState.commitmentsShortfall.isShortfall) {
                val shortfall = uiState.commitmentsShortfall
                val dueText = if (shortfall.earliestDueDay != null) " by ${shortfall.earliestDueDay}th" else ""
                add(
                    NowBarAlert(
                        id = "shortfall",
                        icon = Icons.Default.WarningAmber,
                        iconTint = SoftRed,
                        iconBg = SoftRed.copy(alpha = 0.12f),
                        title = "Shortfall • ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", shortfall.shortfallAmount)}",
                        subtitle = "Transfer to ${shortfall.affectedAccountName}$dueText to protect MAB & avoid AutoPay bounce.",
                        actionLabel = "Transfer",
                        actionColor = SoftRed,
                        onAction = onOpenTransferSheet
                    )
                )
            }

            // 2. High: Scheduled Recurring Commitments
            if (uiState.isRolloverBannerVisible) {
                add(
                    NowBarAlert(
                        id = "rollover",
                        icon = Icons.Default.SyncAlt,
                        iconTint = AccentPurple,
                        iconBg = AccentPurple.copy(alpha = 0.12f),
                        title = "AutoPay Bills Scheduled",
                        subtitle = uiState.rolloverBannerMessage.ifBlank { "Recurring AutoPay bills and budget limits scheduled for next cycle." },
                        actionLabel = "Dismiss",
                        actionColor = AccentPurple,
                        onAction = { viewModel.dismissRolloverBanner() },
                        onDismiss = { viewModel.dismissRolloverBanner() }
                    )
                )
            }

            // 3. Medium: Payday Allocation
            if (showWaterfallPrompt && paydayPlan != null) {
                val parts = buildList {
                    if (paydayPlan.toCommitments > 0.0) add("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", paydayPlan.toCommitments)} to Bills")
                    if (paydayPlan.totalToFortress > 0.0) add("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", paydayPlan.totalToFortress)} to Fortress")
                }.joinToString(" & ")

                add(
                    NowBarAlert(
                        id = "payday",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = SoftTeal,
                        iconBg = SoftTeal.copy(alpha = 0.12f),
                        title = "Payday Allocation Ready",
                        subtitle = if (parts.isNotBlank()) "Allocate $parts." else "Living cushion preserved in Operating.",
                        actionLabel = "Allocate",
                        actionColor = SoftTeal,
                        onAction = {
                            viewModel.applyPaydayAllocation(
                                plan = paydayPlan,
                                operatingAccount = operatingAccountName,
                                commitmentsAccount = commitmentsAccountName,
                                fortressAccount = fortressAccountName
                            )
                            onDismissWaterfall()
                            Toast.makeText(context, "Payday allocation executed!", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = onDismissWaterfall
                    )
                )
            }

            // 4. Low: Month-End Wealth Sweep
            if (showMonthEndSweepPrompt && monthEndSweepPlan != null) {
                add(
                    NowBarAlert(
                        id = "sweep",
                        icon = Icons.Default.Savings,
                        iconTint = SoftGreen,
                        iconBg = SoftGreen.copy(alpha = 0.12f),
                        title = "Wealth Sweep • ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", monthEndSweepPlan.sweepAmount)}",
                        subtitle = "Sweep unspent surplus to Fortress Extra.",
                        actionLabel = "Sweep",
                        actionColor = SoftGreen,
                        onAction = {
                            viewModel.applyMonthEndSweep(
                                plan = monthEndSweepPlan,
                                operatingAccount = operatingAccountName,
                                fortressAccount = fortressAccountName
                            )
                            onDismissSweep()
                            Toast.makeText(context, "Surplus swept to Fortress Extra!", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = onDismissSweep
                    )
                )
            }
        }
    }

    val activeMatrix = remember(uiState.categories, selectedMatrixType) {
        uiState.categories.filter { it.type == selectedMatrixType && it.category.isNotBlank() }
    }

    // Baseline metrics calculation
    val todayDay = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    val daysUntilSalary = uiState.metrics.daysUntilPayday.coerceAtLeast(1)
    val safeDailyVelocity = if (uiState.metrics.safeToSpend > 0) {
        uiState.metrics.safeToSpend / daysUntilSalary
    } else 0.0

    val incomeBase = uiState.metrics.personalIncome.takeIf { it > 0.0 } ?: uiState.metrics.actualIncome
    val lifestyleExp = uiState.metrics.lifestyleExpenses
    val preSipSaved = uiState.metrics.netSavedBeforeInvest
    val retentionPct = if (incomeBase > 0) round((preSipSaved / incomeBase) * 100.0).toInt().coerceIn(0, 100) else 0

    val fortressFd = uiState.fortressFdBalance
    val fortTotal = remember(uiState.activeAccounts) {
        uiState.activeAccounts
            .filter { acc -> acc.accountType.equals("Fortress", ignoreCase = true) }
            .sumOf { it.currentBalance }
    }
    val fortressSavings = remember(fortTotal, fortressFd) { (fortTotal - fortressFd).coerceAtLeast(0.0) }
    val fortressCushionDeficit = remember(fortressSavings, userProfile.fortressSweepThreshold) {
        if (userProfile.fortressSweepThreshold > 0.0) (userProfile.fortressSweepThreshold - fortressSavings).coerceAtLeast(0.0) else 0.0
    }

    // Macro outflow distribution metrics
    val committedFixed = uiState.metrics.fixedCommitmentsTotal
    val actualLivingBurn = (lifestyleExp - committedFixed).coerceAtLeast(0.0)
    val actualAssets = uiState.metrics.actualAssets
    val totalOutflows = committedFixed + actualLivingBurn + actualAssets

    val fixedOutflowFraction = if (totalOutflows > 0) (committedFixed / totalOutflows).toFloat().coerceIn(0f, 1f) else 0.33f
    val livingOutflowFraction = if (totalOutflows > 0) (actualLivingBurn / totalOutflows).toFloat().coerceIn(0f, 1f) else 0.33f
    val assetOutflowFraction = if (totalOutflows > 0) (actualAssets / totalOutflows).toFloat().coerceIn(0f, 1f) else 0.34f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
    ) {
        // 1. SAMSUNG NOW BAR DECK
        if (nowBarAlerts.isNotEmpty()) {
            item(key = "now_bar_capsule") {
                VaultNowBar(
                    alerts = nowBarAlerts,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // 2. HERO CARD: ACTIVE TONAL PULSE WITH PACING & HORIZON SPARKLINE
        item(key = "hero_active_pulse") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF1E1B4B),
                border = BorderStroke(1.dp, Color(0xFF3730A3).copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E1B4B),
                                    Color(0xFF151336),
                                    Color(0xFF0F0E24)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    // Header Row: Status Badge + Title + Capacity Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenStsInfo)
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isHealthy) Color(0xFF34D399) else Color(0xFFF87171))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIQUID SAFE TO SPEND",
                                color = Color(0xFFA5B4FC),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Explain Safe to Spend",
                                tint = Color(0xFFA5B4FC).copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(7.dp),
                            color = (if (isHealthy) Color(0xFF6366F1) else Color(0xFFEF4444)).copy(alpha = 0.22f),
                            border = BorderStroke(0.6.dp, (if (isHealthy) Color(0xFF818CF8) else Color(0xFFF87171)).copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = "${uiState.metrics.safeToSpendPercentage}% Capacity",
                                color = if (isHealthy) Color(0xFFC7D2FE) else Color(0xFFFCA5A5),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Main Figure
                    if (isDiscreetMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "  ● ● ● ● ●  ",
                                fontSize = 18.sp,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.safeToSpend)}",
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isHealthy) Color(0xFFFFFFFF) else Color(0xFFFCA5A5),
                            letterSpacing = (-0.6).sp
                        )
                    }

                    // Pacing Velocity Subtitle
                    Text(
                        text = when {
                            isPastMonth -> "Month closed • Final liquid ledger reconciled"
                            uiState.metrics.isSalaryDelayed -> "Salary expected (${uiState.metrics.nextPaydayDay}th) • 1-day safety runway reserved"
                            isCurrentMonth && isHealthy -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", safeDailyVelocity)}/day safe pace • $daysUntilSalary days until payday (${uiState.metrics.nextPaydayDay}th)"
                            isCurrentMonth -> "Runway deficit: Spending exceeds safe buffer before next payday"
                            else -> "Projected runway reserved for $daysUntilSalary days"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.metrics.isSalaryDelayed) Color(0xFFFBBF24) else if (isHealthy) Color(0xFFCBD5E1) else Color(0xFFFCA5A5)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Integrated Sparkline
                    SpendingSparkline(
                        points = uiState.metrics.dailyExpensePoints,
                        lineColor = if (isHealthy) Color(0xFF818CF8) else Color(0xFFF87171),
                        gradientStartColor = (if (isHealthy) Color(0xFF818CF8) else Color(0xFFF87171)).copy(alpha = 0.35f),
                        gradientEndColor = Color.Transparent
                    )

                    // Payday Horizon Milestones Timeline
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1st Start", fontSize = 8.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                        Text("Today (${todayDay}th)", fontSize = 8.5.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.Bold)
                        Text("Payday (${uiState.metrics.nextPaydayDay}th)", fontSize = 8.sp, color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Translucent Bottom Pillar Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val displayInflow = if (uiState.metrics.plannedIncome > 0) uiState.metrics.plannedIncome else uiState.metrics.personalIncome
                        val displayAssets = if (uiState.metrics.plannedAssets > 0) uiState.metrics.plannedAssets else uiState.metrics.actualAssets

                        DarkPillarMetricCard(
                            title = "Inflow",
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayInflow)}",
                            tintColor = Color(0xFF34D399),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToLedgerWithFilter(TransactionType.INCOME) }
                        )
                        DarkPillarMetricCard(
                            title = "Fixed Bills",
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.fixedCommitmentsTotal)}",
                            tintColor = Color(0xFFF87171),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCommitments
                        )
                        DarkPillarMetricCard(
                            title = "SIP Assets",
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayAssets)}",
                            tintColor = Color(0xFF2DD4BF),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToLedgerWithFilter(TransactionType.ASSET) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        // 3. 3-ITEM QUICK PULSE STRIP (Replaces 108dp carousel)
        item(key = "quick_pulse_strip") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Column 1: Wealth Retained (Pre-SIP)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenBalanceFlowInfo(1) }
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "RETAINED",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (isDiscreetMode) "••••" else "$retentionPct% Saved",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = SoftTeal
                        )
                        Text(
                            text = if (isDiscreetMode) "••••" else "${if (preSipSaved >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", preSipSaved)}",
                            fontSize = 9.5.sp,
                            color = TextMuted
                        )
                    }

                    Box(modifier = Modifier.height(30.dp).width(0.8.dp).background(BorderLight.copy(alpha = 0.7f)))

                    // Column 2: Lifestyle Burn (Total Living Spent)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenBalanceFlowInfo(0) }
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "MONTH BURN",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", lifestyleExp)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = SoftRed
                        )
                        val avgBurnDaily = if (todayDay > 0) lifestyleExp / todayDay else 0.0
                        Text(
                            text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", avgBurnDaily)}/day",
                            fontSize = 9.5.sp,
                            color = TextMuted
                        )
                    }

                    Box(modifier = Modifier.height(30.dp).width(0.8.dp).background(BorderLight.copy(alpha = 0.7f)))

                    // Column 3: Fortress Safety Net (Emergency Fund Status)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onOpenFortressInfo() }
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "FORTRESS NET",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortressFd)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0D9488)
                        )
                        Text(
                            text = if (isDiscreetMode) "••••" else if (fortressCushionDeficit > 0) "● Filling" else "● Protected",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (fortressCushionDeficit > 0) SoftAmber else SoftGreen
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        // 4. MACRO OUTFLOW DISTRIBUTION STRIP
        item(key = "macro_outflow_strip") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                color = CardWhite,
                border = BorderStroke(0.7.dp, BorderLight.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "OUTFLOW ALLOCATION",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = if (isDiscreetMode) "••••" else "Total: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalOutflows)}",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Multi-segmented distribution bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(CanvasLight)
                    ) {
                        if (fixedOutflowFraction > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .weight(fixedOutflowFraction.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(SoftRed)
                            )
                        }
                        if (livingOutflowFraction > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .weight(livingOutflowFraction.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(SoftAmber)
                            )
                        }
                        if (assetOutflowFraction > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .weight(assetOutflowFraction.coerceAtLeast(0.01f))
                                    .fillMaxHeight()
                                    .background(SoftTeal)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Distribution Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(SoftRed))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Fixed: ${(fixedOutflowFraction * 100).toInt()}%",
                                fontSize = 9.5.sp,
                                color = TextDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(SoftAmber))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Living: ${(livingOutflowFraction * 100).toInt()}%",
                                fontSize = 9.5.sp,
                                color = TextDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(SoftTeal))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Wealth: ${(assetOutflowFraction * 100).toInt()}%",
                                fontSize = 9.5.sp,
                                color = TextDark,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        // 5. CATEGORY MATRIX HEADER WITH LIVE HEADROOM READOUT
        item(key = "matrix_header_and_switcher") {
            val plannedExp = uiState.metrics.plannedExpenses
            val actualExp = uiState.metrics.actualExpenses
            val expDiff = plannedExp - actualExp

            val plannedAst = uiState.metrics.plannedAssets
            val actualAst = uiState.metrics.actualAssets
            val astDiff = plannedAst - actualAst

            val pendingClaims = uiState.reimbursementStatus.pendingReimbursement

            val liveHeadroomLabel = when (selectedMatrixType) {
                TransactionType.EXPENSE -> when {
                    plannedExp <= 0 -> "No Target Cap"
                    expDiff >= 0 -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expDiff)} Headroom"
                    else -> "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(expDiff))} Over Budget"
                }
                TransactionType.INCOME -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualIncome)} Received"
                TransactionType.ASSET -> when {
                    plannedAst <= 0 -> "No Target Set"
                    astDiff > 0 -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", astDiff)} to Fund"
                    else -> "100% Funded"
                }
                TransactionType.CORPORATE -> when {
                    pendingClaims > 0 -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", pendingClaims)} Pending"
                    else -> "Reimbursed"
                }
                TransactionType.TRANSFER -> "Transfers"
            }

            val headroomColor = when (selectedMatrixType) {
                TransactionType.EXPENSE -> if (plannedExp > 0 && expDiff < 0) SoftRed else SoftGreen
                TransactionType.INCOME -> SoftGreen
                TransactionType.ASSET -> SoftTeal
                TransactionType.CORPORATE -> if (pendingClaims > 0) Color(0xFFE57A28) else SoftGreen
                TransactionType.TRANSFER -> AccentPurple
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Matrix",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark
                )

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = headroomColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.6.dp, headroomColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = if (isDiscreetMode) "••••" else liveHeadroomLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = headroomColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 4-Way Segment Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(BorderLight.copy(alpha = 0.5f))
                    .padding(4.dp)
            ) {
                listOf(
                    Triple(TransactionType.EXPENSE, "Expenses", SoftRed),
                    Triple(TransactionType.INCOME, "Income", SoftGreen),
                    Triple(TransactionType.ASSET, "Assets / SIP", SoftTeal),
                    Triple(TransactionType.CORPORATE, "Corporate", Color(0xFFE57A28))
                ).forEach { (type, label, color) ->
                    val isSelected = selectedMatrixType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CardWhite else Color.Transparent)
                            .clickable { selectedMatrixType = type }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.sp,
                            color = if (isSelected) color else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 6. CATEGORY MATRIX ROWS
        if (activeMatrix.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No active entries in this segment", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        activeMatrix.forEachIndexed { index, cat ->
                            val isLegacy = remember(uiState.masterCategories, cat) {
                                uiState.masterCategories.any { it.name.equals(cat.category, ignoreCase = true) && it.type == cat.type && it.isLegacy }
                            }
                            val isNew = remember(uiState.masterCategories, cat) {
                                uiState.masterCategories.any { it.name.equals(cat.category, ignoreCase = true) && it.type == cat.type && it.isNew }
                            }

                            CategoryMatrixRow(
                                cat = cat,
                                isExpanded = expandedCategories[cat.category] ?: false,
                                onToggleExpand = {
                                    expandedCategories[cat.category] = !(expandedCategories[cat.category] ?: false)
                                },
                                currencySymbol = userProfile.currencySymbol,
                                isDiscreetMode = isDiscreetMode,
                                isLegacy = isLegacy,
                                isNew = isNew
                            )
                            if (index < activeMatrix.lastIndex) {
                                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DarkPillarMetricCard(
    title: String,
    amount: String,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(tintColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, fontSize = 8.5.sp, color = Color(0xFFA5B4FC), fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun CategoryMatrixRow(
    cat: CategoryPerformance,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isLegacy: Boolean = false,
    isNew: Boolean = false
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "arrowRotation"
    )

    val isTargetOriented = cat.type == TransactionType.INCOME || cat.type == TransactionType.ASSET

    val progressFraction = if (cat.plannedAmount > 0) {
        (cat.actualAmount / cat.plannedAmount).toFloat().coerceIn(0f, 1f)
    } else 1f

    val utilizationPercentage = if (cat.plannedAmount > 0) {
        ((cat.actualAmount / cat.plannedAmount) * 100).toInt()
    } else 100

    val progressColor = when {
        cat.isOverBudget && !isTargetOriented -> SoftRed
        cat.type == TransactionType.INCOME -> SoftGreen
        cat.type == TransactionType.ASSET -> SoftTeal
        cat.type == TransactionType.CORPORATE -> Color(0xFFE57A28)
        utilizationPercentage >= 85 -> SoftAmber
        else -> AccentPurple
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggleExpand)
            .padding(vertical = 8.dp, horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLegacy) Color(0xFFFFF3E0) else progressColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.category.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (isLegacy) Color(0xFFE65100) else progressColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cat.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (isLegacy) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFF3E0),
                                border = BorderStroke(0.6.dp, Color(0xFFFFB74D))
                            ) {
                                Text(
                                    text = "Legacy",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (isNew) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF4CAF50))
                            )
                        }

                        if (cat.isOverBudget && !isTargetOriented) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SoftRed.copy(alpha = 0.12f),
                                border = BorderStroke(0.5.dp, SoftRed.copy(alpha = 0.35f)),
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Text(
                                    text = "Over",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftRed,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val statusText = if (cat.plannedAmount > 0) {
                        val diff = cat.actualAmount - cat.plannedAmount
                        if (isDiscreetMode) {
                            "Target configured"
                        } else if (isTargetOriented) {
                            if (diff >= 0) {
                                "Target achieved (+${currencySymbol}${String.format(Locale.US, "%,.0f", diff)})"
                            } else {
                                "${currencySymbol}${String.format(Locale.US, "%,.0f", abs(diff))} needed to reach target"
                            }
                        } else {
                            val remaining = cat.plannedAmount - cat.actualAmount
                            if (remaining >= 0) {
                                "$currencySymbol${String.format(Locale.US, "%,.0f", remaining)} left of $currencySymbol${String.format(Locale.US, "%,.0f", cat.plannedAmount)}"
                            } else {
                                "Exceeded by $currencySymbol${String.format(Locale.US, "%,.0f", abs(remaining))}"
                            }
                        }
                    } else {
                        if (isLegacy) {
                            "Retiring next month • Logged: $currencySymbol${String.format(Locale.US, "%,.0f", cat.actualAmount)}"
                        } else if (cat.type == TransactionType.CORPORATE) {
                            "Logged actual: $currencySymbol${String.format(Locale.US, "%,.0f", cat.actualAmount)}"
                        } else {
                            "No target limit configured"
                        }
                    }

                    val statusColor = when {
                        cat.isOverBudget && !isTargetOriented -> SoftRed
                        isTargetOriented && cat.plannedAmount > 0 && cat.actualAmount >= cat.plannedAmount -> SoftGreen
                        else -> TextMuted
                    }

                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = if (cat.isOverBudget && !isTargetOriented) FontWeight.SemiBold else FontWeight.Normal,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", cat.actualAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (cat.isOverBudget && !isTargetOriented) SoftRed else TextDark
                    )
                    if (cat.plannedAmount > 0) {
                        Text(
                            text = "$utilizationPercentage%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = progressColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation)
                )
            }
        }

        if (cat.plannedAmount > 0) {
            Spacer(modifier = Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = BorderLight.copy(alpha = 0.5f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CanvasLight)
                    .padding(10.dp)
            ) {
                if (cat.activeSubcategories.isEmpty()) {
                    Text(text = "No logged transactions in subcategories", fontSize = 11.sp, color = TextMuted)
                } else {
                    Text(
                        text = "SUBCATEGORY CONTRIBUTIONS",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    cat.activeSubcategories.forEach { sub ->
                        val subPercentage = if (cat.actualAmount > 0) {
                            ((sub.amount / cat.actualAmount) * 100).toInt()
                        } else 0

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(progressColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = sub.name, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                }

                                Text(
                                    text = if (isDiscreetMode) "•••• ($subPercentage%)" else "$currencySymbol${String.format(Locale.US, "%,.2f", sub.amount)} ($subPercentage%)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Spacer(modifier = Modifier.height(2.5.dp))
                            LinearProgressIndicator(
                                progress = { (subPercentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp)),
                                color = progressColor.copy(alpha = 0.65f),
                                trackColor = BorderLight.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
