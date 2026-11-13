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

private fun formatOrdinalDay(day: Int): String {
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

@Composable
fun MonthlySummaryTab(
    viewModel: BudgetViewModel,
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    filterCriteria: FilterCriteria,
    isDiscreetMode: Boolean,
    isMidnightTheme: Boolean = false,
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
                val dueText = shortfall.earliestDueDay?.let { " by ${formatOrdinalDay(it)}" } ?: ""
                add(
                    NowBarAlert(
                        id = "shortfall",
                        icon = Icons.Default.WarningAmber,
                        iconTint = Error500,
                        iconBg = Error100,
                        title = "Shortfall • ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", shortfall.shortfallAmount)}",
                        subtitle = "Transfer to ${shortfall.affectedAccountName}$dueText to protect MAB & avoid AutoPay bounce.",
                        actionLabel = "Transfer",
                        actionColor = Error500,
                        onAction = onOpenTransferSheet
                    )
                )
            }

            if (uiState.isRolloverBannerVisible) {
                add(
                    NowBarAlert(
                        id = "rollover",
                        icon = Icons.Default.SyncAlt,
                        iconTint = Primary600,
                        iconBg = Primary100,
                        title = "AutoPay Bills Scheduled",
                        subtitle = uiState.rolloverBannerMessage.ifBlank { "Recurring AutoPay bills and budget limits scheduled for next cycle." },
                        actionLabel = "Dismiss",
                        actionColor = Primary600,
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
                        iconBg = SoftTealLight,
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
                        iconTint = Success500,
                        iconBg = Success100,
                        title = "Wealth Sweep • ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", monthEndSweepPlan.sweepAmount)}",
                        subtitle = "Sweep unspent surplus to Fortress Extra.",
                        actionLabel = "Sweep",
                        actionColor = Success500,
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

    val expenseCategories = remember(uiState.categories) {
        uiState.categories
            .filter { it.type == TransactionType.EXPENSE && !it.isReimbursableFloat && it.actualAmount > 0.0 }
            .sortedByDescending { it.actualAmount }
    }
    val totalExpenseSpent = remember(expenseCategories) {
        expenseCategories.sumOf { it.actualAmount }
    }

    val donutColors = remember {
        listOf(
            Primary600,
            SoftTeal,
            Error500,
            Warning500,
            Primary400
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

    val headingTextColor = if (isMidnightTheme) Color.White else TextDark
    val subtitleTextColor = if (isMidnightTheme) Neutral400 else TextMuted

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
    ) {
        // 1. 2-LINE GREETING HEADER
        item(key = "greeting_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$timeGreeting,",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = subtitleTextColor
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "${userProfile.displayName.ifBlank { "User" }} 👋",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = headingTextColor,
                        letterSpacing = (-0.4).sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isHealthy) Success100 else Error100,
                    border = BorderStroke(0.8.dp, if (isHealthy) Success500.copy(alpha = 0.35f) else Error500.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isHealthy) Success700 else Error700)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHealthy) "Safe Runway" else "Attention",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHealthy) Success700 else Error700
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
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // 3. FINTECH HERO CARD (Royal Cobalt Blue 600 -> 900)
        item(key = "fintech_hero_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Primary800,
                border = BorderStroke(1.dp, Primary500.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Primary600,
                                    Primary800,
                                    Primary900
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
                                color = Primary200,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.6.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Explain Safe to Spend",
                                tint = Primary200.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.16f),
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
                                        .background(Error500.copy(alpha = 0.88f))
                                        .align(Alignment.CenterStart)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape)
                                        .background(Warning500.copy(alpha = 0.88f))
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

                    val nextPaydayOrd = formatOrdinalDay(uiState.metrics.nextPaydayDay)
                    Text(
                        text = when {
                            isPastMonth -> "Month closed • Final safe ledger balance"
                            uiState.metrics.isSalaryDelayed -> "Salary expected ($nextPaydayOrd) • 1-day safety runway reserved"
                            isCurrentMonth && isHealthy -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", safeDailyVelocity)}/day safe pace • $daysUntilSalary days until payday ($nextPaydayOrd)"
                            isCurrentMonth -> "Runway deficit: Spending exceeds safe buffer before next payday"
                            else -> "Projected runway reserved for $daysUntilSalary days"
                        },
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (uiState.metrics.isSalaryDelayed) Warning100 else Primary100
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
                            tintColor = Success100,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToLedgerWithFilter(TransactionType.INCOME) }
                        )
                        FrostedCardPill(
                            title = "Fixed Bills",
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.fixedCommitmentsTotal)}",
                            tintColor = Error100,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToCommitments
                        )
                        FrostedCardPill(
                            title = "SIP Assets",
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayAssets)}",
                            tintColor = Primary200,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToLedgerWithFilter(TransactionType.ASSET) }
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(14.dp)) }

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
                    containerColor = Primary50,
                    contentColor = Primary600,
                    labelColor = if (isMidnightTheme) Neutral200 else TextDark,
                    onClick = onOpenAddSheet
                )
                QuickActionButton(
                    icon = Icons.Default.SyncAlt,
                    label = "Transfer",
                    containerColor = Success100,
                    contentColor = Success700,
                    labelColor = if (isMidnightTheme) Neutral200 else TextDark,
                    onClick = onOpenTransferSheet
                )
                QuickActionButton(
                    icon = Icons.Default.Autorenew,
                    label = "AutoPay",
                    containerColor = Error100,
                    contentColor = Error700,
                    labelColor = if (isMidnightTheme) Neutral200 else TextDark,
                    onClick = onNavigateToCommitments
                )
                QuickActionButton(
                    icon = Icons.Default.MoreHoriz,
                    label = "More",
                    containerColor = Neutral100,
                    contentColor = Neutral600,
                    labelColor = if (isMidnightTheme) Neutral200 else TextDark,
                    onClick = onOpenDrawer
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 5. SPENDING OVERVIEW (Donut Chart - Floating White Card)
        item(key = "spending_overview_donut") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Spending Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = headingTextColor
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
                                tint = Success500,
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

            val (headroomBg, headroomText) = when (selectedMatrixType) {
                TransactionType.EXPENSE -> if (plannedExp > 0 && expDiff < 0) Error100 to Error700 else Success100 to Success700
                TransactionType.INCOME -> Success100 to Success700
                TransactionType.ASSET -> SoftTealLight to SoftTeal
                TransactionType.CORPORATE -> Warning100 to Warning700
                TransactionType.TRANSFER -> Primary100 to Primary700
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
                    color = headingTextColor
                )

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = headroomBg,
                    border = BorderStroke(0.6.dp, headroomText.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = if (isDiscreetMode) "••••" else liveHeadroomLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = headroomText,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isMidnightTheme) Color.White.copy(alpha = 0.10f)
                        else Neutral100
                    )
                    .padding(4.dp)
            ) {
                listOf(
                    Triple(TransactionType.EXPENSE, "Expenses", Error500),
                    Triple(TransactionType.INCOME, "Income", Success500),
                    Triple(TransactionType.ASSET, "Assets / SIP", SoftTeal),
                    Triple(TransactionType.CORPORATE, "Corporate", Warning700)
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
                            color = if (isSelected) color else if (isMidnightTheme) Neutral400 else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 7. CATEGORY MATRIX ROWS (Floating White Card)
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
                            val mapKey = "${cat.type.name}_${cat.category}"

                            CategoryMatrixRow(
                                cat = cat,
                                isExpanded = expandedCategories[mapKey] ?: false,
                                onToggleExpand = {
                                    expandedCategories[mapKey] = !(expandedCategories[mapKey] ?: false)
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
    labelColor: Color = TextDark,
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
            color = labelColor
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
                Text(text = title, fontSize = 8.5.sp, color = Primary200, fontWeight = FontWeight.SemiBold)
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

    val isTargetOriented = cat.type == TransactionType.INCOME ||
            cat.type == TransactionType.ASSET ||
            (cat.type == TransactionType.CORPORATE && cat.category.equals("Reimbursements & Claims", ignoreCase = true))

    val isOverBudgetRow = cat.plannedAmount > 0.0 &&
            cat.actualAmount > cat.plannedAmount &&
            !isTargetOriented

    val progressFraction = if (cat.plannedAmount > 0) {
        (cat.actualAmount / cat.plannedAmount).toFloat().coerceIn(0f, 1f)
    } else 1f

    val utilizationPercentage = if (cat.plannedAmount > 0) {
        ((cat.actualAmount / cat.plannedAmount) * 100).toInt()
    } else 100

    val progressColor = when {
        isOverBudgetRow -> Error500
        cat.type == TransactionType.INCOME -> Success500
        cat.type == TransactionType.ASSET -> SoftTeal
        cat.type == TransactionType.CORPORATE -> Warning500
        utilizationPercentage >= 85 -> Warning500
        else -> Primary600
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
                        .background(if (isLegacy) Warning100 else progressColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.category.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (isLegacy) Warning700 else progressColor
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
                                color = Warning100,
                                border = BorderStroke(0.6.dp, Warning500)
                            ) {
                                Text(
                                    text = "Legacy",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Warning700,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (isNew) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Success500)
                            )
                        }

                        if (isOverBudgetRow) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Error100,
                                border = BorderStroke(0.5.dp, Error500.copy(alpha = 0.35f)),
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Text(
                                    text = "Over",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Error700,
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
                        val maskedOrActual = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", cat.actualAmount)}"
                        if (isLegacy) {
                            "Retiring next month • Logged: $maskedOrActual"
                        } else if (cat.type == TransactionType.CORPORATE) {
                            "Logged actual: $maskedOrActual"
                        } else {
                            "No target limit configured"
                        }
                    }

                    val statusColor = when {
                        isOverBudgetRow -> Error700
                        isTargetOriented && cat.plannedAmount > 0 && cat.actualAmount >= cat.plannedAmount -> Success700
                        else -> TextMuted
                    }

                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = if (isOverBudgetRow) FontWeight.SemiBold else FontWeight.Normal,
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
                        color = if (isOverBudgetRow) Error700 else TextDark
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
