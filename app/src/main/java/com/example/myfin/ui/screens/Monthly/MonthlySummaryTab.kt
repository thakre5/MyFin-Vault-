package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.myfin.ui.components.NowBarAlert
import com.example.myfin.ui.components.VaultNowBar
import com.example.myfin.ui.theme.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

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
    onNavigateToCommitments: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    onOpenAddSheet: () -> Unit = {}
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

    // Samsung Now Bar Alerts
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

    val daysUntilSalary = uiState.metrics.daysUntilPayday.coerceAtLeast(1)
    val safeDailyVelocity = if (uiState.metrics.safeToSpend > 0) {
        uiState.metrics.safeToSpend / daysUntilSalary
    } else 0.0

    val timeGreeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    // Top Expense Categories for Donut Chart
    val expenseCategories = remember(uiState.categories) {
        uiState.categories
            .filter { it.type == TransactionType.EXPENSE && it.actualAmount > 0.0 }
            .sortedByDescending { it.actualAmount }
    }
    val totalExpenseSpent = remember(expenseCategories) {
        expenseCategories.sumOf { it.actualAmount }
    }

    val donutColors = remember {
        listOf(
            Color(0xFF4F46E5), // Indigo
            Color(0xFF38BDF8), // Cyan Sky
            Color(0xFFF43F5E), // Coral Pink
            Color(0xFFF59E0B), // Amber
            Color(0xFFA855F7)  // Violet
        )
    }

    val topDonutSlices = remember(expenseCategories, totalExpenseSpent) {
        if (totalExpenseSpent <= 0.0 || expenseCategories.isEmpty()) {
            emptyList()
        } else {
            val topFour = expenseCategories.take(4)
            val othersAmount = expenseCategories.drop(4).sumOf { it.actualAmount }

            val slices = topFour.mapIndexed { index, cat ->
                val pct = ((cat.actualAmount / totalExpenseSpent) * 100).toInt()
                DonutSliceData(
                    name = cat.category,
                    percentage = pct,
                    color = donutColors[index % donutColors.size]
                )
            }.toMutableList()

            if (othersAmount > 0.0) {
                val otherPct = ((othersAmount / totalExpenseSpent) * 100).toInt()
                slices.add(
                    DonutSliceData(
                        name = "Others",
                        percentage = otherPct,
                        color = donutColors.last()
                    )
                )
            }
            slices
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 2.dp, bottom = 140.dp)
    ) {
        // 1. WARM GREETING HEADER (Compact & Flush)
        item(key = "greeting_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$timeGreeting,",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    Text(
                        text = "${userProfile.displayName.ifBlank { "User" }} 👋",
                        fontSize = 18.5.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        letterSpacing = (-0.3).sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isHealthy) SoftGreen else SoftRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHealthy) "Safe Runway" else "Attention",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHealthy) SoftGreen else SoftRed
                        )
                    }
                }
            }
        }

        // 2. SAMSUNG NOW BAR DECK
        if (nowBarAlerts.isNotEmpty()) {
            item(key = "now_bar_capsule") {
                VaultNowBar(
                    alerts = nowBarAlerts,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
        }

        // 3. FINTECH HERO CARD
        item(key = "fintech_hero_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF3730A3),
                border = BorderStroke(1.dp, Color(0xFF4F46E5).copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4338CA),
                                    Color(0xFF3730A3),
                                    Color(0xFF312E81)
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 15.dp)
                ) {
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
                        ) {
                            Text(
                                text = "SAFE TO SPEND",
                                color = Color(0xFFC7D2FE),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Explain Safe to Spend",
                                tint = Color(0xFFC7D2FE).copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = "${uiState.metrics.safeToSpendPercentage}% Cap",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                )
                            }

                            Box(modifier = Modifier.size(24.dp, 16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444).copy(alpha = 0.85f))
                                        .align(Alignment.CenterStart)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.85f))
                                        .align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (isDiscreetMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "  ● ● ● ● ●  ",
                                fontSize = 20.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.safeToSpend)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.7).sp
                        )
                    }

                    Text(
                        text = when {
                            isPastMonth -> "Month closed • Final safe ledger balance"
                            uiState.metrics.isSalaryDelayed -> "Salary expected (${uiState.metrics.nextPaydayDay}th) • 1-day safety runway reserved"
                            isCurrentMonth && isHealthy -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", safeDailyVelocity)}/day safe pace • $daysUntilSalary days until payday (${uiState.metrics.nextPaydayDay}th)"
                            isCurrentMonth -> "Runway deficit: Spending exceeds safe buffer before next payday"
                            else -> "Projected runway reserved for $daysUntilSalary days"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.metrics.isSalaryDelayed) Color(0xFFFDE68A) else Color(0xFFE0E7FF)
                    )

                    Spacer(modifier = Modifier.height(13.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val displayInflow = if (uiState.metrics.plannedIncome > 0) uiState.metrics.plannedIncome else uiState.metrics.personalIncome
                        val displayAssets = if (uiState.metrics.plannedAssets > 0) uiState.metrics.plannedAssets else uiState.metrics.actualAssets

                        FrostedCardPill(
                            title = "Inflow",
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayInflow)}",
                            tintColor = Color(0xFF34D399),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToLedgerWithFilter(TransactionType.INCOME) }
                        )
                        FrostedCardPill(
                            title = "Fixed Bills",
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.fixedCommitmentsTotal)}",
                            tintColor = Color(0xFFFCA5A5),
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCommitments
                        )
                        FrostedCardPill(
                            title = "SIP Assets",
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayAssets)}",
                            tintColor = Color(0xFF67E8F9),
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToLedgerWithFilter(TransactionType.ASSET) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 4. QUICK ACTION STRIP
        item(key = "quick_action_strip") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuickActionButton(
                    icon = Icons.Default.Add,
                    label = "Add Entry",
                    containerColor = Color(0xFFEEF2FF),
                    contentColor = Color(0xFF4F46E5),
                    onClick = onOpenAddSheet
                )
                QuickActionButton(
                    icon = Icons.Default.SyncAlt,
                    label = "Transfer",
                    containerColor = Color(0xFFECFDF5),
                    contentColor = Color(0xFF059669),
                    onClick = onOpenTransferSheet
                )
                QuickActionButton(
                    icon = Icons.Default.Autorenew,
                    label = "AutoPay",
                    containerColor = Color(0xFFFFF1F2),
                    contentColor = Color(0xFFE11D48),
                    onClick = onNavigateToCommitments
                )
                QuickActionButton(
                    icon = Icons.Default.MoreHoriz,
                    label = "More",
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = Color(0xFF475569),
                    onClick = onOpenDrawer
                )
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

        // 5. SPENDING OVERVIEW (Donut Chart)
        item(key = "spending_overview_donut") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Spending Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.5.dp, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                ) {
                    if (totalExpenseSpent <= 0.0 || topDonutSlices.isEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SoftGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zero expenses logged this month yet.",
                                fontSize = 12.5.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(
                                modifier = Modifier.size(118.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.size(110.dp)) {
                                    val strokePx = 16.dp.toPx()
                                    var currentAngle = -90f

                                    topDonutSlices.forEach { slice ->
                                        val sweep = (slice.percentage / 100f) * 360f
                                        if (sweep > 0f) {
                                            drawArc(
                                                color = slice.color,
                                                startAngle = currentAngle,
                                                sweepAngle = (sweep - 3f).coerceAtLeast(1f),
                                                useCenter = false,
                                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                                            )
                                            currentAngle += sweep
                                        }
                                    }
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalExpenseSpent)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.5.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "This Month",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                topDonutSlices.forEach { slice ->
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
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(slice.color)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = slice.name,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextDark,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = "${slice.percentage}%",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 6. CATEGORY MATRIX HEADER & SWITCHER
        item(key = "matrix_header_and_switcher") {
            val plannedExp = uiState.metrics.plannedExpenses
            val actualExp = uiState.metrics.actualExpenses
            val expDiff = plannedExp - actualExp

            val liveHeadroomLabel = when (selectedMatrixType) {
                TransactionType.EXPENSE -> when {
                    plannedExp <= 0 -> "No Target Cap"
                    expDiff >= 0 -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expDiff)} Headroom"
                    else -> "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(expDiff))} Over Budget"
                }
                TransactionType.INCOME -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualIncome)} Received"
                TransactionType.ASSET -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualAssets)} Funded"
                TransactionType.CORPORATE -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.reimbursementStatus.pendingReimbursement)} Pending"
                TransactionType.TRANSFER -> "Transfers"
            }

            val headroomColor = when (selectedMatrixType) {
                TransactionType.EXPENSE -> if (plannedExp > 0 && expDiff < 0) SoftRed else SoftGreen
                TransactionType.INCOME -> SoftGreen
                TransactionType.ASSET -> SoftTeal
                TransactionType.CORPORATE -> Color(0xFFE57A28)
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

        // 7. CATEGORY MATRIX ROWS
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

private data class DonutSliceData(
    val name: String,
    val percentage: Int,
    val color: Color
)

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Surface(
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            border = BorderStroke(0.6.dp, contentColor.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextDark
        )
    }
}

@Composable
private fun FrostedCardPill(
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
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.18f))
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
                Text(text = title, fontSize = 8.5.sp, color = Color(0xFFC7D2FE), fontWeight = FontWeight.SemiBold)
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
