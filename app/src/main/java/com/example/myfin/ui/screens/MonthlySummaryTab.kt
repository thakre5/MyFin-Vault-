package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionType
import com.example.myfin.data.UserProfile
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.CategoryPerformance
import com.example.myfin.ui.FilterCriteria
import com.example.myfin.ui.MonthlyUiState
import com.example.myfin.ui.components.SpendingSparkline
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs
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

    val activeMatrix = remember(uiState.categories, selectedMatrixType) {
        uiState.categories.filter { it.type == selectedMatrixType && it.category.isNotBlank() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
    ) {
        if (uiState.isRolloverBannerVisible || showWaterfallPrompt || showMonthEndSweepPrompt || uiState.commitmentsShortfall.isShortfall) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (uiState.commitmentsShortfall.isShortfall) {
                        val shortfall = uiState.commitmentsShortfall
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SoftRed.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.WarningAmber,
                                        contentDescription = null,
                                        tint = SoftRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Commitments Shortfall Warning",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val dueText = if (shortfall.earliestDueDay != null) " by ${shortfall.earliestDueDay}th" else ""
                                    Text(
                                        text = "Transfer ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", shortfall.shortfallAmount)}$dueText to ${shortfall.affectedAccountName} to protect MAB & avoid bill bounce.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Button(
                                    onClick = onOpenTransferSheet,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(text = "Transfer", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (uiState.isRolloverBannerVisible) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.28f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(AccentPurple.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SyncAlt,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Recurring Commitments Scheduled",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = uiState.rolloverBannerMessage.ifBlank {
                                            "Recurring AutoPay bills and budget limits have been scheduled for next cycle."
                                        },
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Button(
                                    onClick = { viewModel.dismissRolloverBanner() },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(text = "Dismiss", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    if (showWaterfallPrompt && paydayPlan != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, SoftTeal.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SoftTeal.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = SoftTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Payday Allocation Ready",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val planParts = buildList {
                                        if (paydayPlan.toCommitments > 0.0) {
                                            add("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", paydayPlan.toCommitments)} to Commitments")
                                        }
                                        if (paydayPlan.totalToFortress > 0.0) {
                                            add("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", paydayPlan.totalToFortress)} to Fortress")
                                        }
                                    }
                                    Text(
                                        text = if (planParts.isNotEmpty()) "Allocate ${planParts.joinToString(" & ")}." else "Living cushion preserved in Operating.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.applyPaydayAllocation(
                                                plan = paydayPlan,
                                                operatingAccount = operatingAccountName,
                                                commitmentsAccount = commitmentsAccountName,
                                                fortressAccount = fortressAccountName
                                            )
                                            onDismissWaterfall()
                                            Toast.makeText(context, "Payday allocation executed!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(text = "Allocate", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    TextButton(
                                        onClick = onDismissWaterfall,
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier.height(22.dp)
                                    ) {
                                        Text(text = "Dismiss", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }

                    if (showMonthEndSweepPrompt && monthEndSweepPlan != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, SoftGreen.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(SoftGreen.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = SoftGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Month-End Wealth Sweep",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Sweep ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", monthEndSweepPlan.sweepAmount)} unspent surplus into Fortress Extra.",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        lineHeight = 15.sp,
                                        maxLines = 2
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.applyMonthEndSweep(
                                                plan = monthEndSweepPlan,
                                                operatingAccount = operatingAccountName,
                                                fortressAccount = fortressAccountName
                                            )
                                            onDismissSweep()
                                            Toast.makeText(context, "Surplus swept to Fortress Extra!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SoftGreen),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text(text = "Sweep", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    TextButton(
                                        onClick = onDismissSweep,
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier.height(22.dp)
                                    ) {
                                        Text(text = "Dismiss", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 3. HORIZONTAL CAROUSEL: SAFE TO SPEND, 3-PILLAR TARGET, & FORTRESS CARDS
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Card 1: Liquid Safe to Spend
                item {
                    val statusColor = if (isHealthy) SoftGreen else SoftRed

                    Surface(
                        modifier = Modifier
                            .width(320.dp)
                            .height(290.dp)
                            .shadow(3.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFFCFAFF),
                                            AccentPurple.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
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
                                            .padding(vertical = 2.dp, horizontal = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(statusColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "LIQUID SAFE TO SPEND",
                                            color = TextMuted,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.7.sp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.HelpOutline,
                                            contentDescription = "Explain Safe to Spend",
                                            tint = AccentPurple,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isHealthy) AccentPurple.copy(alpha = 0.1f) else statusColor.copy(alpha = 0.12f),
                                        border = BorderStroke(0.6.dp, if (isHealthy) AccentPurple.copy(alpha = 0.25f) else statusColor.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "${uiState.metrics.safeToSpendPercentage}% Capacity",
                                            color = if (isHealthy) AccentPurple else statusColor,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = if (isDiscreetMode) "••••••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.safeToSpend)}",
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isHealthy) TextDark else SoftRed,
                                    letterSpacing = (-0.5).sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = when {
                                        isPastMonth -> "Month closed: final remaining balance"
                                        uiState.metrics.isSalaryDelayed -> "Salary expected (${uiState.metrics.nextPaydayDay}th) • 1-day runway reserved"
                                        isCurrentMonth && isHealthy -> if (isDiscreetMode) "Guilt-free surplus protected" else "Pure surplus • Runway reserved for ${uiState.metrics.daysUntilPayday}d (until ${uiState.metrics.nextPaydayDay}th)"
                                        isCurrentMonth -> "Runway deficit: spending exceeds safe allowance"
                                        else -> "Projected surplus for ${uiState.metrics.daysUntilPayday} days until payday"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (uiState.metrics.isSalaryDelayed) Color(0xFFE57A28) else if (isHealthy) TextMuted else SoftRed,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                SpendingSparkline(
                                    points = uiState.metrics.dailyExpensePoints,
                                    lineColor = if (isHealthy) AccentPurple else SoftRed,
                                    gradientStartColor = (if (isHealthy) AccentPurple else SoftRed).copy(alpha = 0.32f),
                                    gradientEndColor = (if (isHealthy) AccentPurple else SoftRed).copy(alpha = 0.0f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val displayInflow = if (uiState.metrics.plannedIncome > 0) uiState.metrics.plannedIncome else uiState.metrics.personalIncome
                                val displayAssets = if (uiState.metrics.plannedAssets > 0) uiState.metrics.plannedAssets else uiState.metrics.actualAssets

                                PillarMetricCard(
                                    title = "Inflow",
                                    amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayInflow)}",
                                    tintColor = SoftGreen,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToLedgerWithFilter(TransactionType.INCOME) }
                                )
                                PillarMetricCard(
                                    title = "Fixed Bills",
                                    amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.fixedCommitmentsTotal)}",
                                    tintColor = SoftRed,
                                    modifier = Modifier.weight(1f),
                                    onClick = onNavigateToCommitments
                                )
                                PillarMetricCard(
                                    title = "SIP Assets",
                                    amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayAssets)}",
                                    tintColor = SoftTeal,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigateToLedgerWithFilter(TransactionType.ASSET) }
                                )
                            }
                        }
                    }
                }

                // Card 2: 3-Pillar Target Execution
                item {
                    val plannedExpenses = uiState.metrics.plannedExpenses
                    val actualExpenses = uiState.metrics.actualExpenses
                    val expDiff = actualExpenses - plannedExpenses
                    val expFraction = if (plannedExpenses > 0) (actualExpenses / plannedExpenses).toFloat().coerceIn(0f, 1f) else if (actualExpenses > 0) 1f else 0f

                    val plannedIncome = uiState.metrics.plannedIncome
                    val actualIncome = uiState.metrics.actualIncome
                    val incDiff = actualIncome - plannedIncome
                    val incFraction = if (plannedIncome > 0) (actualIncome / plannedIncome).toFloat().coerceIn(0f, 1f) else if (actualIncome > 0) 1f else 0f

                    val plannedAssets = uiState.metrics.plannedAssets
                    val actualAssets = uiState.metrics.actualAssets
                    val astDiff = actualAssets - plannedAssets
                    val astFraction = if (plannedAssets > 0) (actualAssets / plannedAssets).toFloat().coerceIn(0f, 1f) else if (actualAssets > 0) 1f else 0f

                    val isExpenseOverBudget = plannedExpenses > 0 && actualExpenses > plannedExpenses
                    val budgetStatusLabel = when {
                        plannedExpenses <= 0 -> "Tracking"
                        isExpenseOverBudget -> "Over Budget"
                        else -> "On Track"
                    }
                    val budgetStatusColor = when {
                        plannedExpenses <= 0 -> TextMuted
                        isExpenseOverBudget -> SoftRed
                        else -> SoftGreen
                    }

                    Surface(
                        modifier = Modifier
                            .width(320.dp)
                            .height(290.dp)
                            .shadow(3.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFFCFAFF),
                                            AccentPurple.copy(alpha = 0.04f)
                                        )
                                    )
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(AccentPurple)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "3-PILLAR TARGET EXECUTION",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextMuted,
                                        letterSpacing = 0.7.sp
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = budgetStatusColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = budgetStatusLabel,
                                        color = budgetStatusColor,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            // Expenses pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CanvasLight.copy(alpha = 0.6f),
                                border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SoftRed.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = SoftRed, modifier = Modifier.size(13.dp))
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text("Expenses", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isExpenseOverBudget) SoftRed.copy(alpha = 0.12f) else CardWhite
                                            ) {
                                                Text(
                                                    text = if (plannedExpenses > 0) {
                                                        if (isDiscreetMode) "Tracked"
                                                        else if (expDiff > 0) "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expDiff)} Over"
                                                        else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(expDiff))} Left"
                                                    } else "No Cap",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isExpenseOverBudget) SoftRed else TextMuted,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualExpenses)}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp,
                                                color = if (isExpenseOverBudget) SoftRed else TextDark
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "Target: ••••" else "Target: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", plannedExpenses)}",
                                                fontSize = 9.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { expFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = SoftRed,
                                        trackColor = SoftRed.copy(alpha = 0.15f)
                                    )
                                }
                            }

                            // Income pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CanvasLight.copy(alpha = 0.6f),
                                border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SoftGreen.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SoftGreen, modifier = Modifier.size(13.dp))
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text("Income", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = CardWhite
                                            ) {
                                                Text(
                                                    text = if (plannedIncome > 0) {
                                                        if (incDiff >= 0) "Target Met"
                                                        else if (isDiscreetMode) "Short"
                                                        else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(incDiff))} Short"
                                                    } else "Recorded",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextMuted,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualIncome)}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp,
                                                color = TextDark
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "Target: ••••" else "Target: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", plannedIncome)}",
                                                fontSize = 9.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { incFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = SoftGreen,
                                        trackColor = SoftGreen.copy(alpha = 0.15f)
                                    )
                                }
                            }

                            // Assets pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CanvasLight.copy(alpha = 0.6f),
                                border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(SoftTeal.copy(alpha = 0.12f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Savings, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(13.dp))
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))
                                            Text("Assets / SIP", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = CardWhite
                                            ) {
                                                Text(
                                                    text = if (plannedAssets > 0) {
                                                        if (astDiff >= 0) "Target Met"
                                                        else if (isDiscreetMode) "Short"
                                                        else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(astDiff))} Short"
                                                    } else "Recorded",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextMuted,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualAssets)}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 12.5.sp,
                                                color = TextDark
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "Target: ••••" else "Target: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", plannedAssets)}",
                                                fontSize = 9.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { astFraction },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = SoftTeal,
                                        trackColor = SoftTeal.copy(alpha = 0.15f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 3: Fortress Emergency Net
                item {
                    val fortressGoal = uiState.fortressTarget
                    val currentFds = uiState.fortressFdBalance
                    val sweepFloor = userProfile.fortressSweepThreshold
                    val progressPct = uiState.fortressProgressPercentage
                    val progressFraction = (progressPct / 100f).coerceIn(0f, 1f)

                    Surface(
                        modifier = Modifier
                            .width(320.dp)
                            .height(290.dp)
                            .shadow(3.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFF7FCFB),
                                            Color(0xFF0D9488).copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF0D9488))
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "FORTRESS SAFETY NET & SWEEP",
                                            color = TextMuted,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.7.sp
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF0D9488).copy(alpha = 0.12f),
                                        border = BorderStroke(0.6.dp, Color(0xFF0D9488).copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = "$progressPct% Funded",
                                            color = Color(0xFF0D9488),
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = if (isDiscreetMode) "••••••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", currentFds)}",
                                    fontSize = 27.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark,
                                    letterSpacing = (-0.5).sp
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = if (isDiscreetMode) "Emergency Corpus Protected" else "Corpus in Sweep FDs (Goal: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortressGoal)})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                LinearProgressIndicator(
                                    progress = { progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF0D9488),
                                    trackColor = Color(0xFF0D9488).copy(alpha = 0.15f)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CanvasLight.copy(alpha = 0.7f),
                                border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Liquid Savings Floor", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = TextMuted)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", sweepFloor)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .height(26.dp)
                                            .width(1.dp)
                                            .background(BorderLight.copy(alpha = 0.7f))
                                    )

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("Runway Target", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = TextMuted)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${userProfile.fortressEmergencyMonths} Months Burn",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0D9488)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. Balance Flow & Net Savings Delta Card
        item {
            val startBalance = uiState.metrics.startLiquidBalance
            val endBalance = uiState.metrics.endLiquidBalance
            val savedBeforeInvest = uiState.metrics.netSavedBeforeInvest
            val savedAfterInvest = uiState.metrics.netSavedAfterInvest

            val incomeBase = uiState.metrics.personalIncome.takeIf { it > 0.0 } ?: uiState.metrics.actualIncome
            val wealthRetentionRatePct = if (incomeBase > 0) {
                round((savedBeforeInvest / incomeBase) * 100.0).toInt()
            } else 0

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "START BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.4.sp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", startBalance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextDark
                            )
                            Text(text = "Opening Liquid", fontSize = 10.sp, color = TextMuted)
                        }

                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .width(1.dp)
                                .background(BorderLight.copy(alpha = 0.6f))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp)
                        ) {
                            Text(text = "END BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.4.sp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", endBalance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (endBalance >= 0) TextDark else SoftRed
                            )
                            Text(text = "Active Liquid", fontSize = 10.sp, color = TextMuted)
                        }

                        Box(
                            modifier = Modifier
                                .height(34.dp)
                                .width(1.dp)
                                .background(BorderLight.copy(alpha = 0.6f))
                        )

                        Column(
                            modifier = Modifier
                                .weight(1.2f)
                                .padding(start = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(text = "SAVINGS (PRE-SIP)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.4.sp)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isDiscreetMode) "••••" else "${if (savedBeforeInvest >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", savedBeforeInvest)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = if (savedBeforeInvest >= 0) SoftTeal else SoftRed
                            )
                            Text(
                                text = "${if (savedBeforeInvest >= 0) "+" else ""}$wealthRetentionRatePct% Retained",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (savedBeforeInvest >= 0) SoftTeal else SoftRed
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentPurple))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Net Cash Added (Post-SIP / Assets):",
                                fontSize = 11.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Text(
                            text = if (isDiscreetMode) "••••" else "${if (savedAfterInvest >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", savedAfterInvest)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (savedAfterInvest >= 0) SoftGreen else SoftRed
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }

        // 5. Category Matrix Header & Switcher
        item {
            Text(text = "Category Matrix", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(10.dp))

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
            Spacer(modifier = Modifier.height(12.dp))
        }

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

@Composable
private fun PillarMetricCard(
    title: String,
    amount: String,
    tintColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(11.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(tintColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = amount,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
        }
    }
}
