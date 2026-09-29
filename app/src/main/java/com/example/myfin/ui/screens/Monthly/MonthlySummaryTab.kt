package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.example.myfin.ui.components.NowBarAlert
import com.example.myfin.ui.components.SpendingSparkline
import com.example.myfin.ui.components.VaultNowBar
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

@OptIn(ExperimentalFoundationApi::class)
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

    val paydayPlan = uiState.paydaySuggestion[span_1](start_span)[span_1](end_span)
    val showWaterfallPrompt = remember(paydayPlan, uiState.selectedMonth, dismissedWaterfallMonth, isCurrentMonth) {
        isCurrentMonth && paydayPlan != null && (paydayPlan.toCommitments > 0.0 || paydayPlan.totalToFortress > 0.0) && dismissedWaterfallMonth != uiState.selectedMonth[span_2](start_span)[span_2](end_span)
    }

    val monthEndSweepPlan = uiState.monthEndSweepSuggestion[span_3](start_span)[span_3](end_span)
    val showMonthEndSweepPrompt = remember(monthEndSweepPlan, uiState.selectedMonth, dismissedSweepMonth, isCurrentMonth) {
        isCurrentMonth && monthEndSweepPlan != null && monthEndSweepPlan.sweepAmount > 0.0 && dismissedSweepMonth != uiState.selectedMonth[span_4](start_span)[span_4](end_span)
    }

    // Consolidated Samsung Now Bar Alerts
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
        uiState.categories.filter { it.type == selectedMatrixType && it.category.isNotBlank() }[span_5](start_span)[span_5](end_span)
    }

    val topCardsPagerState = rememberPagerState(pageCount = { 3 })[span_6](start_span)[span_6](end_span)
    val flowPagerState = rememberPagerState(pageCount = { 4 })[span_7](start_span)[span_7](end_span)

    LaunchedEffect(flowPagerState) {
        while (true) {
            delay(4500)
            if (!flowPagerState.isScrollInProgress) {
                val next = (flowPagerState.currentPage + 1) % 4
                flowPagerState.animateScrollToPage(next, animationSpec = tween(600))[span_8](start_span)[span_8](end_span)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)[span_9](start_span)[span_9](end_span)
    ) {
        // 1. SAMSUNG NOW BAR CAPSULE (Sits directly above Hero card)
        if (nowBarAlerts.isNotEmpty()) {
            item(key = "now_bar_capsule") {
                VaultNowBar(
                    alerts = nowBarAlerts,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // 2. HORIZONTAL PAGER: SAFE TO SPEND, 3-PILLAR TARGET, & FORTRESS CARDS
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalPager(
                    state = topCardsPagerState,
                    contentPadding = PaddingValues(horizontal = 0.dp),
                    pageSpacing = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)[span_10](start_span)[span_10](end_span)
                ) { pageIndex ->
                    when (pageIndex) {
                        0 -> {
                            val statusColor = if (isHealthy) SoftGreen else SoftRed[span_11](start_span)[span_11](end_span)

                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(2.dp, RoundedCornerShape(18.dp)),[span_12](start_span)[span_12](end_span)
                                shape = RoundedCornerShape(18.dp),[span_13](start_span)[span_13](end_span)
                                color = CardWhite,[span_14](start_span)[span_14](end_span)
                                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))[span_15](start_span)[span_15](end_span)
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
                                        .padding(horizontal = 12.dp, vertical = 10.dp),[span_16](start_span)[span_16](end_span)
                                    verticalArrangement = Arrangement.SpaceBetween[span_17](start_span)[span_17](end_span)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),[span_18](start_span)[span_18](end_span)
                                            horizontalArrangement = Arrangement.SpaceBetween,[span_19](start_span)[span_19](end_span)
                                            verticalAlignment = Alignment.CenterVertically[span_20](start_span)[span_20](end_span)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,[span_21](start_span)[span_21](end_span)
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))[span_22](start_span)[span_22](end_span)
                                                    .clickable(onClick = onOpenStsInfo)[span_23](start_span)[span_23](end_span)
                                                    .padding(vertical = 1.dp, horizontal = 2.dp)[span_24](start_span)[span_24](end_span)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)[span_25](start_span)[span_25](end_span)
                                                        .clip(CircleShape)[span_26](start_span)[span_26](end_span)
                                                        .background(statusColor)[span_27](start_span)[span_27](end_span)
                                                )
                                                Spacer(modifier = Modifier.width(5.dp))[span_28](start_span)[span_28](end_span)
                                                Text(
                                                    text = "LIQUID SAFE TO SPEND",[span_29](start_span)[span_29](end_span)
                                                    color = TextMuted,[span_30](start_span)[span_30](end_span)
                                                    fontSize = 9.sp,[span_31](start_span)[span_31](end_span)
                                                    fontWeight = FontWeight.Black,[span_32](start_span)[span_32](end_span)
                                                    letterSpacing = 0.5.sp[span_33](start_span)[span_33](end_span)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))[span_34](start_span)[span_34](end_span)
                                                Icon(
                                                    imageVector = Icons.Default.HelpOutline,[span_35](start_span)[span_35](end_span)
                                                    contentDescription = "Explain Safe to Spend",[span_36](start_span)[span_36](end_span)
                                                    tint = AccentPurple,[span_37](start_span)[span_37](end_span)
                                                    modifier = Modifier.size(13.dp)[span_38](start_span)[span_38](end_span)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(6.dp),[span_39](start_span)[span_39](end_span)
                                                color = if (isHealthy) AccentPurple.copy(alpha = 0.1f) else statusColor.copy(alpha = 0.12f),[span_40](start_span)[span_40](end_span)
                                                border = BorderStroke(0.5.dp, if (isHealthy) AccentPurple.copy(alpha = 0.25f) else statusColor.copy(alpha = 0.3f))[span_41](start_span)[span_41](end_span)
                                            ) {
                                                Text(
                                                    text = "${uiState.metrics.safeToSpendPercentage}% Capacity",[span_42](start_span)[span_42](end_span)
                                                    color = if (isHealthy) AccentPurple else statusColor,[span_43](start_span)[span_43](end_span)
                                                    fontSize = 8.5.sp,[span_44](start_span)[span_44](end_span)
                                                    fontWeight = FontWeight.Bold,[span_45](start_span)[span_45](end_span)
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)[span_46](start_span)[span_46](end_span)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))[span_47](start_span)[span_47](end_span)

                                        Text(
                                            text = if (isDiscreetMode) "••••••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.safeToSpend)}",[span_48](start_span)[span_48](end_span)
                                            fontSize = 21.sp,[span_49](start_span)[span_49](end_span)
                                            fontWeight = FontWeight.Black,[span_50](start_span)[span_50](end_span)
                                            color = if (isHealthy) TextDark else SoftRed,[span_51](start_span)[span_51](end_span)
                                            letterSpacing = (-0.5).sp[span_52](start_span)[span_52](end_span)
                                        )

                                        Spacer(modifier = Modifier.height(1.dp))[span_53](start_span)[span_53](end_span)

                                        Text(
                                            text = when {
                                                isPastMonth -> "Month closed: final remaining balance[span_54](start_span)"[span_54](end_span)
                                                uiState.metrics.isSalaryDelayed -> "Salary expected (${uiState.metrics.nextPaydayDay}th) • 1-day runway reserved[span_55](start_span)"[span_55](end_span)
                                                isCurrentMonth && isHealthy -> if (isDiscreetMode) "Guilt-free surplus protected" else "Pure surplus • Runway reserved for ${uiState.metrics.daysUntilPayday}d (until ${uiState.metrics.nextPaydayDay}th)[span_56](start_span)"[span_56](end_span)
                                                isCurrentMonth -> "Runway deficit: spending exceeds safe allowance[span_57](start_span)"[span_57](end_span)
                                                else -> "Projected surplus for ${uiState.metrics.daysUntilPayday} days until payday[span_58](start_span)"[span_58](end_span)
                                            },
                                            fontSize = 9.5.sp,[span_59](start_span)[span_59](end_span)
                                            fontWeight = FontWeight.Medium,[span_60](start_span)[span_60](end_span)
                                            color = if (uiState.metrics.isSalaryDelayed) Color(0xFFE57A28) else if (isHealthy) TextMuted else SoftRed,[span_61](start_span)[span_61](end_span)
                                            maxLines = 1,[span_62](start_span)[span_62](end_span)
                                            overflow = TextOverflow.Ellipsis[span_63](start_span)[span_63](end_span)
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))[span_64](start_span)[span_64](end_span)

                                        SpendingSparkline(
                                            points = uiState.metrics.dailyExpensePoints,[span_65](start_span)[span_65](end_span)
                                            lineColor = if (isHealthy) AccentPurple else SoftRed,[span_66](start_span)[span_66](end_span)
                                            gradientStartColor = (if (isHealthy) AccentPurple else SoftRed).copy(alpha = 0.32f),[span_67](start_span)[span_67](end_span)
                                            gradientEndColor = (if (isHealthy) AccentPurple else SoftRed).copy(alpha = 0.0f)[span_68](start_span)[span_68](end_span)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_69](start_span)[span_69](end_span)
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)[span_70](start_span)[span_70](end_span)
                                    ) {
                                        val displayInflow = if (uiState.metrics.plannedIncome > 0) uiState.metrics.plannedIncome else uiState.metrics.personalIncome[span_71](start_span)[span_71](end_span)
                                        val displayAssets = if (uiState.metrics.plannedAssets > 0) uiState.metrics.plannedAssets else uiState.metrics.actualAssets[span_72](start_span)[span_72](end_span)

                                        PillarMetricCard(
                                            title = "Inflow",[span_73](start_span)[span_73](end_span)
                                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayInflow)}",[span_74](start_span)[span_74](end_span)
                                            tintColor = SoftGreen,[span_75](start_span)[span_75](end_span)
                                            modifier = Modifier.weight(1f),[span_76](start_span)[span_76](end_span)
                                            onClick = { onNavigateToLedgerWithFilter(TransactionType.INCOME) }[span_77](start_span)[span_77](end_span)
                                        )
                                        PillarMetricCard(
                                            title = "Fixed Bills",[span_78](start_span)[span_78](end_span)
                                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.fixedCommitmentsTotal)}",[span_79](start_span)[span_79](end_span)
                                            tintColor = SoftRed,[span_80](start_span)[span_80](end_span)
                                            modifier = Modifier.weight(1f),[span_81](start_span)[span_81](end_span)
                                            onClick = onNavigateToCommitments[span_82](start_span)[span_82](end_span)
                                        )
                                        PillarMetricCard(
                                            title = "SIP Assets",[span_83](start_span)[span_83](end_span)
                                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayAssets)}",[span_84](start_span)[span_84](end_span)
                                            tintColor = SoftTeal,[span_85](start_span)[span_85](end_span)
                                            modifier = Modifier.weight(1f),[span_86](start_span)[span_86](end_span)
                                            onClick = { onNavigateToLedgerWithFilter(TransactionType.ASSET) }[span_87](start_span)[span_87](end_span)
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            val plannedExpenses = uiState.metrics.plannedExpenses[span_88](start_span)[span_88](end_span)
                            val actualExpenses = uiState.metrics.actualExpenses[span_89](start_span)[span_89](end_span)
                            val expDiff = actualExpenses - plannedExpenses[span_90](start_span)[span_90](end_span)
                            val expFraction = if (plannedExpenses > 0) (actualExpenses / plannedExpenses).toFloat().coerceIn(0f, 1f) else if (actualExpenses > 0) 1f else 0f[span_91](start_span)[span_91](end_span)

                            val plannedIncome = uiState.metrics.plannedIncome[span_92](start_span)[span_92](end_span)
                            val actualIncome = uiState.metrics.actualIncome[span_93](start_span)[span_93](end_span)
                            val incDiff = actualIncome - plannedIncome[span_94](start_span)[span_94](end_span)
                            val incFraction = if (plannedIncome > 0) (actualIncome / plannedIncome).toFloat().coerceIn(0f, 1f) else if (actualIncome > 0) 1f else 0f[span_95](start_span)[span_95](end_span)

                            val plannedAssets = uiState.metrics.plannedAssets[span_96](start_span)[span_96](end_span)
                            val actualAssets = uiState.metrics.actualAssets[span_97](start_span)[span_97](end_span)
                            val astDiff = actualAssets - plannedAssets[span_98](start_span)[span_98](end_span)
                            val astFraction = if (plannedAssets > 0) (actualAssets / plannedAssets).toFloat().coerceIn(0f, 1f) else if (actualAssets > 0) 1f else 0f[span_99](start_span)[span_99](end_span)

                            val isExpenseOverBudget = plannedExpenses > 0 && actualExpenses > plannedExpenses[span_100](start_span)[span_100](end_span)
                            val budgetStatusLabel = when {
                                plannedExpenses <= 0 -> "Tracking"
                                isExpenseOverBudget -> "Over Budget"
                                else -> "On Track"
                            }[span_101](start_span)[span_101](end_span)
                            val budgetStatusColor = when {
                                plannedExpenses <= 0 -> TextMuted
                                isExpenseOverBudget -> SoftRed
                                else -> SoftGreen
                            }[span_102](start_span)[span_102](end_span)

                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(2.dp, RoundedCornerShape(18.dp)),[span_103](start_span)[span_103](end_span)
                                shape = RoundedCornerShape(18.dp),[span_104](start_span)[span_104](end_span)
                                color = CardWhite,[span_105](start_span)[span_105](end_span)
                                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))[span_106](start_span)[span_106](end_span)
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
                                        .padding(horizontal = 11.dp, vertical = 9.dp),[span_107](start_span)[span_107](end_span)
                                    verticalArrangement = Arrangement.SpaceBetween[span_108](start_span)[span_108](end_span)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_109](start_span)[span_109](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_110](start_span)[span_110](end_span)
                                        verticalAlignment = Alignment.CenterVertically[span_111](start_span)[span_111](end_span)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,[span_112](start_span)[span_112](end_span)
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))[span_113](start_span)[span_113](end_span)
                                                .clickable(onClick = onOpenThreePillarInfo)[span_114](start_span)[span_114](end_span)
                                                .padding(vertical = 1.dp, horizontal = 2.dp)[span_115](start_span)[span_115](end_span)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)[span_116](start_span)[span_116](end_span)
                                                    .clip(CircleShape)[span_117](start_span)[span_117](end_span)
                                                    .background(AccentPurple)[span_118](start_span)[span_118](end_span)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))[span_119](start_span)[span_119](end_span)
                                            Text(
                                                text = "3-PILLAR TARGET EXECUTION",[span_120](start_span)[span_120](end_span)
                                                fontSize = 9.sp,[span_121](start_span)[span_121](end_span)
                                                fontWeight = FontWeight.Black,[span_122](start_span)[span_122](end_span)
                                                color = TextMuted,[span_123](start_span)[span_123](end_span)
                                                letterSpacing = 0.5.sp[span_124](start_span)[span_124](end_span)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))[span_125](start_span)[span_125](end_span)
                                            Icon(
                                                imageVector = Icons.Default.HelpOutline,[span_126](start_span)[span_126](end_span)
                                                contentDescription = "Explain 3-Pillars",[span_127](start_span)[span_127](end_span)
                                                tint = AccentPurple,[span_128](start_span)[span_128](end_span)
                                                modifier = Modifier.size(13.dp)[span_129](start_span)[span_129](end_span)
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(5.dp),[span_130](start_span)[span_130](end_span)
                                            color = budgetStatusColor.copy(alpha = 0.12f)[span_131](start_span)[span_131](end_span)
                                        ) {
                                            Text(
                                                text = budgetStatusLabel,[span_132](start_span)[span_132](end_span)
                                                color = budgetStatusColor,[span_133](start_span)[span_133](end_span)
                                                fontSize = 8.5.sp,[span_134](start_span)[span_134](end_span)
                                                fontWeight = FontWeight.Bold,[span_135](start_span)[span_135](end_span)
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)[span_136](start_span)[span_136](end_span)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(9.dp),[span_137](start_span)[span_137](end_span)
                                        color = CanvasLight.copy(alpha = 0.6f),[span_138](start_span)[span_138](end_span)
                                        border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f)),[span_139](start_span)[span_139](end_span)
                                        modifier = Modifier.fillMaxWidth()[span_140](start_span)[span_140](end_span)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {[span_141](start_span)[span_141](end_span)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),[span_142](start_span)[span_142](end_span)
                                                horizontalArrangement = Arrangement.SpaceBetween,[span_143](start_span)[span_143](end_span)
                                                verticalAlignment = Alignment.CenterVertically[span_144](start_span)[span_144](end_span)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {[span_145](start_span)[span_145](end_span)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)[span_146](start_span)[span_146](end_span)
                                                            .clip(RoundedCornerShape(4.dp))[span_147](start_span)[span_147](end_span)
                                                            .background(SoftRed.copy(alpha = 0.12f)),[span_148](start_span)[span_148](end_span)
                                                        contentAlignment = Alignment.Center[span_149](start_span)[span_149](end_span)
                                                    ) {
                                                        Icon(Icons.Default.TrendingDown, contentDescription = null, tint = SoftRed, modifier = Modifier.size(10.dp))[span_150](start_span)[span_150](end_span)
                                                    }
                                                    Spacer(modifier = Modifier.width(5.dp))[span_151](start_span)[span_151](end_span)
                                                    Text("Expenses", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = TextDark)[span_152](start_span)[span_152](end_span)
                                                    Spacer(modifier = Modifier.width(4.dp))[span_153](start_span)[span_153](end_span)
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),[span_154](start_span)[span_154](end_span)
                                                        color = if (isExpenseOverBudget) SoftRed.copy(alpha = 0.12f) else CardWhite[span_155](start_span)[span_155](end_span)
                                                    ) {
                                                        Text(
                                                            text = if (plannedExpenses > 0) {
                                                                if (isDiscreetMode) "Tracked"
                                                                else if (expDiff > 0) "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expDiff)} Over"
                                                                else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(expDiff))} Left"
                                                            } else "No Cap",[span_156](start_span)[span_156](end_span)
                                                            fontSize = 7.5.sp,[span_157](start_span)[span_157](end_span)
                                                            fontWeight = FontWeight.Bold,[span_158](start_span)[span_158](end_span)
                                                            color = if (isExpenseOverBudget) SoftRed else TextMuted,[span_159](start_span)[span_159](end_span)
                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)[span_160](start_span)[span_160](end_span)
                                                        )
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {[span_161](start_span)[span_161](end_span)
                                                    Text(
                                                        text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualExpenses)}",[span_162](start_span)[span_162](end_span)
                                                        fontWeight = FontWeight.Black,[span_163](start_span)[span_163](end_span)
                                                        fontSize = 11.sp,[span_164](start_span)[span_164](end_span)
                                                        color = if (isExpenseOverBudget) SoftRed else TextDark[span_165](start_span)[span_165](end_span)
                                                    )
                                                    Text(
                                                        text = if (isDiscreetMode) "Target: ••••" else "Target: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", plannedExpenses)}",[span_166](start_span)[span_166](end_span)
                                                        fontSize = 8.sp,[span_167](start_span)[span_167](end_span)
                                                        color = TextMuted[span_168](start_span)[span_168](end_span)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.5.dp))[span_169](start_span)[span_169](end_span)
                                            LinearProgressIndicator(
                                                progress = { expFraction },[span_170](start_span)[span_170](end_span)
                                                modifier = Modifier
                                                    .fillMaxWidth()[span_171](start_span)[span_171](end_span)
                                                    .height(2.5.dp)[span_172](start_span)[span_172](end_span)
                                                    .clip(RoundedCornerShape(1.5.dp)),[span_173](start_span)[span_173](end_span)
                                                color = SoftRed,[span_174](start_span)[span_174](end_span)
                                                trackColor = SoftRed.copy(alpha = 0.15f)[span_175](start_span)[span_175](end_span)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(9.dp),[span_176](start_span)[span_176](end_span)
                                        color = CanvasLight.copy(alpha = 0.6f),[span_177](start_span)[span_177](end_span)
                                        border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f)),[span_178](start_span)[span_178](end_span)
                                        modifier = Modifier.fillMaxWidth()[span_179](start_span)[span_179](end_span)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {[span_180](start_span)[span_180](end_span)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),[span_181](start_span)[span_181](end_span)
                                                horizontalArrangement = Arrangement.SpaceBetween,[span_182](start_span)[span_182](end_span)
                                                verticalAlignment = Alignment.CenterVertically[span_183](start_span)[span_183](end_span)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {[span_184](start_span)[span_184](end_span)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)[span_185](start_span)[span_185](end_span)
                                                            .clip(RoundedCornerShape(4.dp))[span_186](start_span)[span_186](end_span)
                                                            .background(SoftGreen.copy(alpha = 0.12f)),[span_187](start_span)[span_187](end_span)
                                                        contentAlignment = Alignment.Center[span_188](start_span)[span_188](end_span)
                                                    ) {
                                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SoftGreen, modifier = Modifier.size(10.dp))[span_189](start_span)[span_189](end_span)
                                                    }
                                                    Spacer(modifier = Modifier.width(5.dp))[span_190](start_span)[span_190](end_span)
                                                    Text("Income", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = TextDark)[span_191](start_span)[span_191](end_span)
                                                    Spacer(modifier = Modifier.width(4.dp))[span_192](start_span)[span_192](end_span)
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),[span_193](start_span)[span_193](end_span)
                                                        color = CardWhite[span_194](start_span)[span_194](end_span)
                                                    ) {
                                                        Text(
                                                            text = if (plannedIncome > 0) {
                                                                if (incDiff >= 0) "Target Met"
                                                                else if (isDiscreetMode) "Short"
                                                                else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(incDiff))} Short"
                                                            } else "Recorded",[span_195](start_span)[span_195](end_span)
                                                            fontSize = 7.5.sp,[span_196](start_span)[span_196](end_span)
                                                            fontWeight = FontWeight.Bold,[span_197](start_span)[span_197](end_span)
                                                            color = TextMuted,[span_198](start_span)[span_198](end_span)
                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)[span_199](start_span)[span_199](end_span)
                                                        )
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {[span_200](start_span)[span_200](end_span)
                                                    Text(
                                                        text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualIncome)}",[span_201](start_span)[span_201](end_span)
                                                        fontWeight = FontWeight.Black,[span_202](start_span)[span_202](end_span)
                                                        fontSize = 11.sp,[span_203](start_span)[span_203](end_span)
                                                        color = TextDark[span_204](start_span)[span_204](end_span)
                                                    )
                                                    Text(
                                                        text = if (isDiscreetMode) "Target: ••••" else "Target: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", plannedIncome)}",[span_205](start_span)[span_205](end_span)
                                                        fontSize = 8.sp,[span_206](start_span)[span_206](end_span)
                                                        color = TextMuted[span_207](start_span)[span_207](end_span)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.5.dp))[span_208](start_span)[span_208](end_span)
                                            LinearProgressIndicator(
                                                progress = { incFraction },[span_209](start_span)[span_209](end_span)
                                                modifier = Modifier
                                                    .fillMaxWidth()[span_210](start_span)[span_210](end_span)
                                                    .height(2.5.dp)[span_211](start_span)[span_211](end_span)
                                                    .clip(RoundedCornerShape(1.5.dp)),[span_212](start_span)[span_212](end_span)
                                                color = SoftGreen,[span_213](start_span)[span_213](end_span)
                                                trackColor = SoftGreen.copy(alpha = 0.15f)[span_214](start_span)[span_214](end_span)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(9.dp),[span_215](start_span)[span_215](end_span)
                                        color = CanvasLight.copy(alpha = 0.6f),[span_216](start_span)[span_216](end_span)
                                        border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f)),[span_217](start_span)[span_217](end_span)
                                        modifier = Modifier.fillMaxWidth()[span_218](start_span)[span_218](end_span)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {[span_219](start_span)[span_219](end_span)
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),[span_220](start_span)[span_220](end_span)
                                                horizontalArrangement = Arrangement.SpaceBetween,[span_221](start_span)[span_221](end_span)
                                                verticalAlignment = Alignment.CenterVertically[span_222](start_span)[span_222](end_span)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {[span_223](start_span)[span_223](end_span)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)[span_224](start_span)[span_224](end_span)
                                                            .clip(RoundedCornerShape(4.dp))[span_225](start_span)[span_225](end_span)
                                                            .background(SoftTeal.copy(alpha = 0.12f)),[span_226](start_span)[span_226](end_span)
                                                        contentAlignment = Alignment.Center[span_227](start_span)[span_227](end_span)
                                                    ) {
                                                        Icon(Icons.Default.Savings, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(10.dp))[span_228](start_span)[span_228](end_span)
                                                    }
                                                    Spacer(modifier = Modifier.width(5.dp))[span_229](start_span)[span_229](end_span)
                                                    Text("Assets / SIP", fontWeight = FontWeight.Bold, fontSize = 10.5.sp, color = TextDark)[span_230](start_span)[span_230](end_span)
                                                    Spacer(modifier = Modifier.width(4.dp))[span_231](start_span)[span_231](end_span)
                                                    Surface(
                                                        shape = RoundedCornerShape(3.dp),[span_232](start_span)[span_232](end_span)
                                                        color = CardWhite[span_233](start_span)[span_233](end_span)
                                                    ) {
                                                        Text(
                                                            text = if (plannedAssets > 0) {
                                                                if (astDiff >= 0) "Target Met"
                                                                else if (isDiscreetMode) "Short"
                                                                else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(astDiff))} Short"
                                                            } else "Recorded",[span_234](start_span)[span_234](end_span)
                                                            fontSize = 7.5.sp,[span_235](start_span)[span_235](end_span)
                                                            fontWeight = FontWeight.Bold,[span_236](start_span)[span_236](end_span)
                                                            color = TextMuted,[span_237](start_span)[span_237](end_span)
                                                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)[span_238](start_span)[span_238](end_span)
                                                        )
                                                    }
                                                }

                                                Column(horizontalAlignment = Alignment.End) {[span_239](start_span)[span_239](end_span)
                                                    Text(
                                                        text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualAssets)}",[span_240](start_span)[span_240](end_span)
                                                        fontWeight = FontWeight.Black,[span_241](start_span)[span_241](end_span)
                                                        fontSize = 11.sp,[span_242](start_span)[span_242](end_span)
                                                        color = TextDark[span_243](start_span)[span_243](end_span)
                                                    )
                                                    Text(
                                                        text = if (isDiscreetMode) "Target: ••••" else "Target: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", plannedAssets)}",[span_244](start_span)[span_244](end_span)
                                                        fontSize = 8.sp,[span_245](start_span)[span_245](end_span)
                                                        color = TextMuted[span_246](start_span)[span_246](end_span)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.5.dp))[span_247](start_span)[span_247](end_span)
                                            LinearProgressIndicator(
                                                progress = { astFraction },[span_248](start_span)[span_248](end_span)
                                                modifier = Modifier
                                                    .fillMaxWidth()[span_249](start_span)[span_249](end_span)
                                                    .height(2.5.dp)[span_250](start_span)[span_250](end_span)
                                                    .clip(RoundedCornerShape(1.5.dp)),[span_251](start_span)[span_251](end_span)
                                                color = SoftTeal,[span_252](start_span)[span_252](end_span)
                                                trackColor = SoftTeal.copy(alpha = 0.15f)[span_253](start_span)[span_253](end_span)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            val fortTotal = remember(uiState.activeAccounts) {
                                uiState.activeAccounts
                                    .filter { acc -> acc.accountType.equals("Fortress", ignoreCase = true) }
                                    .sumOf { it.currentBalance }[span_254](start_span)[span_254](end_span)
                            }
                            val fortressFd = uiState.fortressFdBalance[span_255](start_span)[span_255](end_span)
                            val emergencyTarget = uiState.fortressTarget[span_256](start_span)[span_256](end_span)
                            val sweepThreshold = userProfile.fortressSweepThreshold[span_257](start_span)[span_257](end_span)
                            val fortressSavings = remember(fortTotal, fortressFd) {
                                (fortTotal - fortressFd).coerceAtLeast(0.0)[span_258](start_span)[span_258](end_span)
                            }
                            val fortressCushionDeficit = remember(fortressSavings, sweepThreshold) {
                                if (sweepThreshold > 0.0) (sweepThreshold - fortressSavings).coerceAtLeast(0.0) else 0.0[span_259](start_span)[span_259](end_span)
                            }
                            val fdDeficit = remember(fortressFd, emergencyTarget) {
                                if (emergencyTarget > 0.0) (emergencyTarget - fortressFd).coerceAtLeast(0.0) else 0.0[span_260](start_span)[span_260](end_span)
                            }
                            val targetLabel = if (userProfile.fortressManualTarget > 0.0) "Manual" else "${userProfile.fortressEmergencyMonths}M[span_261](start_span)"[span_261](end_span)

                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(2.dp, RoundedCornerShape(18.dp)),[span_262](start_span)[span_262](end_span)
                                shape = RoundedCornerShape(18.dp),[span_263](start_span)[span_263](end_span)
                                color = CardWhite,[span_264](start_span)[span_264](end_span)
                                border = BorderStroke(0.8.dp, SoftTeal.copy(alpha = 0.28f))[span_265](start_span)[span_265](end_span)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(
                                                    Color(0xFFFFFFFF),
                                                    SoftTeal.copy(alpha = 0.04f)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp),[span_266](start_span)[span_266](end_span)
                                    verticalArrangement = Arrangement.SpaceBetween[span_267](start_span)[span_267](end_span)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_268](start_span)[span_268](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_269](start_span)[span_269](end_span)
                                        verticalAlignment = Alignment.CenterVertically[span_270](start_span)[span_270](end_span)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,[span_271](start_span)[span_271](end_span)
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))[span_272](start_span)[span_272](end_span)
                                                .clickable(onClick = onOpenFortressInfo)[span_273](start_span)[span_273](end_span)
                                                .padding(vertical = 1.dp, horizontal = 2.dp)[span_274](start_span)[span_274](end_span)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)[span_275](start_span)[span_275](end_span)
                                                    .clip(CircleShape)[span_276](start_span)[span_276](end_span)
                                                    .background(SoftTeal.copy(alpha = 0.14f)),[span_277](start_span)[span_277](end_span)
                                                contentAlignment = Alignment.Center[span_278](start_span)[span_278](end_span)
                                            ) {
                                                Icon(
                                                    Icons.Default.Security,[span_279](start_span)[span_279](end_span)
                                                    contentDescription = null,[span_280](start_span)[span_280](end_span)
                                                    tint = SoftTeal,[span_281](start_span)[span_281](end_span)
                                                    modifier = Modifier.size(15.dp)[span_282](start_span)[span_282](end_span)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(7.dp))[span_283](start_span)[span_283](end_span)
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {[span_284](start_span)[span_284](end_span)
                                                    Text(
                                                        text = "Fortress Vault Split",[span_285](start_span)[span_285](end_span)
                                                        fontWeight = FontWeight.Bold,[span_286](start_span)[span_286](end_span)
                                                        fontSize = 12.5.sp,[span_287](start_span)[span_287](end_span)
                                                        color = TextDark[span_288](start_span)[span_288](end_span)
                                                    )
                                                    Spacer(modifier = Modifier.width(3.dp))[span_289](start_span)[span_289](end_span)
                                                    Icon(
                                                        imageVector = Icons.Default.HelpOutline,[span_290](start_span)[span_290](end_span)
                                                        contentDescription = "Explain Fortress",[span_291](start_span)[span_291](end_span)
                                                        tint = SoftTeal,[span_292](start_span)[span_292](end_span)
                                                        modifier = Modifier.size(13.dp)[span_293](start_span)[span_293](end_span)
                                                    )
                                                }
                                                Text(
                                                    text = "Liquid Cushion vs. Emergency FD",[span_294](start_span)[span_294](end_span)
                                                    fontSize = 9.5.sp,[span_295](start_span)[span_295](end_span)
                                                    color = TextMuted[span_296](start_span)[span_296](end_span)
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),[span_297](start_span)[span_297](end_span)
                                            color = when {
                                                fortressCushionDeficit > 0 -> SoftAmber.copy(alpha = 0.12f)
                                                emergencyTarget > 0 && fdDeficit <= 0 -> SoftTeal.copy(alpha = 0.14f)
                                                fortressFd > 0 -> SoftTeal.copy(alpha = 0.12f)
                                                else -> SoftGreen.copy(alpha = 0.12f)
                                            }[span_298](start_span)[span_298](end_span)
                                        ) {
                                            Text(
                                                text = when {
                                                    fortressCushionDeficit > 0 -> "Filling Cushion"
                                                    emergencyTarget > 0 && fdDeficit <= 0 -> "Goal 100%"
                                                    fortressFd > 0 -> "FD Active"
                                                    else -> "Cushion Full"
                                                },[span_299](start_span)[span_299](end_span)
                                                fontSize = 8.5.sp,[span_300](start_span)[span_300](end_span)
                                                fontWeight = FontWeight.Bold,[span_301](start_span)[span_301](end_span)
                                                color = when {
                                                    fortressCushionDeficit > 0 -> SoftAmber
                                                    emergencyTarget > 0 && fdDeficit <= 0 -> SoftTeal
                                                    fortressFd > 0 -> SoftTeal
                                                    else -> SoftGreen
                                                },[span_302](start_span)[span_302](end_span)
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)[span_303](start_span)[span_303](end_span)
                                            )
                                        }
                                    }

                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()[span_304](start_span)[span_304](end_span)
                                                .height(6.dp)[span_305](start_span)[span_305](end_span)
                                                .clip(RoundedCornerShape(3.dp))[span_306](start_span)[span_306](end_span)
                                                .background(CanvasLight)[span_307](start_span)[span_307](end_span)
                                        ) {
                                            if (fortTotal > 0.0) {
                                                val cushionRatio = (fortressSavings / fortTotal).toFloat().coerceIn(0f, 1f)[span_308](start_span)[span_308](end_span)
                                                val fdRatio = (fortressFd / fortTotal).toFloat().coerceIn(0f, 1f)[span_309](start_span)[span_309](end_span)
                                                if (cushionRatio > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(cushionRatio.coerceAtLeast(0.01f))[span_310](start_span)[span_310](end_span)
                                                            .fillMaxHeight()[span_311](start_span)[span_311](end_span)
                                                            .background(SoftTeal)[span_312](start_span)[span_312](end_span)
                                                    )
                                                }
                                                if (fdRatio > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(fdRatio.coerceAtLeast(0.01f))[span_313](start_span)[span_313](end_span)
                                                            .fillMaxHeight()[span_314](start_span)[span_314](end_span)
                                                            .background(Color(0xFF0D9488))[span_315](start_span)[span_315](end_span)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))[span_316](start_span)[span_316](end_span)

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),[span_317](start_span)[span_317](end_span)
                                            horizontalArrangement = Arrangement.SpaceBetween,[span_318](start_span)[span_318](end_span)
                                            verticalAlignment = Alignment.CenterVertically[span_319](start_span)[span_319](end_span)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {[span_320](start_span)[span_320](end_span)
                                                Row(verticalAlignment = Alignment.CenterVertically) {[span_321](start_span)[span_321](end_span)
                                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(SoftTeal))[span_322](start_span)[span_322](end_span)
                                                    Spacer(modifier = Modifier.width(4.dp))[span_323](start_span)[span_323](end_span)
                                                    Text("Liquid Cushion", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)[span_324](start_span)[span_324](end_span)
                                                }
                                                Spacer(modifier = Modifier.height(1.dp))[span_325](start_span)[span_325](end_span)
                                                Text(
                                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortressSavings)}",[span_326](start_span)[span_326](end_span)
                                                    fontSize = 13.5.sp,[span_327](start_span)[span_327](end_span)
                                                    fontWeight = FontWeight.Bold,[span_328](start_span)[span_328](end_span)
                                                    color = TextDark[span_329](start_span)[span_329](end_span)
                                                )
                                                Text(
                                                    text = if (isDiscreetMode) "Cap: ••••" else if (sweepThreshold > 0.0) "Cap: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", sweepThreshold)}" else "No Cap Set",[span_330](start_span)[span_330](end_span)
                                                    fontSize = 8.5.sp,[span_331](start_span)[span_331](end_span)
                                                    color = TextMuted[span_332](start_span)[span_332](end_span)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .height(26.dp)[span_333](start_span)[span_333](end_span)
                                                    .width(1.dp)[span_334](start_span)[span_334](end_span)
                                                    .background(BorderLight.copy(alpha = 0.7f))[span_335](start_span)[span_335](end_span)
                                            )

                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)[span_336](start_span)[span_336](end_span)
                                                    .padding(start = 10.dp)[span_337](start_span)[span_337](end_span)
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {[span_338](start_span)[span_338](end_span)
                                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF0D9488)))[span_339](start_span)[span_339](end_span)
                                                    Spacer(modifier = Modifier.width(4.dp))[span_340](start_span)[span_340](end_span)
                                                    Text("Emergency FD", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)[span_341](start_span)[span_341](end_span)
                                                }
                                                Spacer(modifier = Modifier.height(1.dp))[span_342](start_span)[span_342](end_span)
                                                Text(
                                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortressFd)}",[span_343](start_span)[span_343](end_span)
                                                    fontSize = 13.5.sp,[span_344](start_span)[span_344](end_span)
                                                    fontWeight = FontWeight.Bold,[span_345](start_span)[span_345](end_span)
                                                    color = if (fortressFd > 0) Color(0xFF0D9488) else TextDark[span_346](start_span)[span_346](end_span)
                                                )
                                                Text(
                                                    text = if (isDiscreetMode) "Goal: ••••" else if (emergencyTarget > 0.0) "Goal: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", emergencyTarget)} ($targetLabel)" else "Target Unset",[span_347](start_span)[span_347](end_span)
                                                    fontSize = 8.5.sp,[span_348](start_span)[span_348](end_span)
                                                    color = TextMuted[span_349](start_span)[span_349](end_span)
                                                )
                                            }
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),[span_350](start_span)[span_350](end_span)
                                        color = if (fortressCushionDeficit > 0) SoftAmber.copy(alpha = 0.10f) else SoftTeal.copy(alpha = 0.10f),[span_351](start_span)[span_351](end_span)
                                        modifier = Modifier.fillMaxWidth()[span_352](start_span)[span_352](end_span)
                                    ) {
                                        val targetDesc = if (userProfile.fortressManualTarget > 0.0) "manual" else "${userProfile.fortressEmergencyMonths}M[span_353](start_span)"[span_353](end_span)
                                        val statusNotice = when {
                                            fortressCushionDeficit > 0 ->
                                                "• Needs ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortressCushionDeficit)} to fill cushion before auto-booking FDs"
                                            emergencyTarget > 0.0 && fdDeficit > 0 ->
                                                "• Cushion full. FDs need ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fdDeficit)} for $targetDesc target."
                                            emergencyTarget > 0.0 && fdDeficit <= 0 ->
                                                "• Cushion full & $targetDesc Emergency FD target 100% funded!"
                                            else ->
                                                "• Liquid cushion full. Surplus actively sweeps to Emergency FDs."
                                        }[span_354](start_span)[span_354](end_span)
                                        Text(
                                            text = if (isDiscreetMode) "• Balance privacy enabled" else statusNotice,[span_355](start_span)[span_355](end_span)
                                            fontSize = 9.sp,[span_356](start_span)[span_356](end_span)
                                            color = if (fortressCushionDeficit > 0) SoftAmber else SoftTeal,[span_357](start_span)[span_357](end_span)
                                            fontWeight = FontWeight.Medium,[span_358](start_span)[span_358](end_span)
                                            maxLines = 1,[span_359](start_span)[span_359](end_span)
                                            overflow = TextOverflow.Ellipsis,[span_360](start_span)[span_360](end_span)
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)[span_361](start_span)[span_361](end_span)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))[span_362](start_span)[span_362](end_span)

                Row(
                    modifier = Modifier.fillMaxWidth(),[span_363](start_span)[span_363](end_span)
                    horizontalArrangement = Arrangement.Center,[span_364](start_span)[span_364](end_span)
                    verticalAlignment = Alignment.CenterVertically[span_365](start_span)[span_365](end_span)
                ) {
                    repeat(3) { idx ->
                        val isSelected = topCardsPagerState.currentPage == idx[span_366](start_span)[span_366](end_span)
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 14.dp else 4.dp,[span_367](start_span)[span_367](end_span)
                            animationSpec = tween(250),[span_368](start_span)[span_368](end_span)
                            label = "topDotWidth[span_369](start_span)"[span_369](end_span)
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)[span_370](start_span)[span_370](end_span)
                                .height(3.5.dp)[span_371](start_span)[span_371](end_span)
                                .width(dotWidth)[span_372](start_span)[span_372](end_span)
                                .clip(RoundedCornerShape(2.dp))[span_373](start_span)[span_373](end_span)
                                .background(if (isSelected) AccentPurple else BorderLight.copy(alpha = 0.7f))[span_374](start_span)[span_374](end_span)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))[span_375](start_span)[span_375](end_span)
        }

        // 3. AUTO-SCROLLING BALANCE FLOW CAROUSEL
        item {
            val startBalance = uiState.metrics.startLiquidBalance[span_376](start_span)[span_376](end_span)
            val endBalance = uiState.metrics.endLiquidBalance[span_377](start_span)[span_377](end_span)
            val preSipSaved = uiState.metrics.netSavedBeforeInvest[span_378](start_span)[span_378](end_span)
            val actualAssets = uiState.metrics.actualAssets[span_379](start_span)[span_379](end_span)
            val postSipSurplus = preSipSaved - actualAssets[span_380](start_span)[span_380](end_span)
            val bankCashMovement = endBalance - startBalance[span_381](start_span)[span_381](end_span)
            val relocatedGap = bankCashMovement - postSipSurplus[span_382](start_span)[span_382](end_span)

            val incomeBase = uiState.metrics.personalIncome.takeIf { it > 0.0 } ?: uiState.metrics.actualIncome[span_383](start_span)[span_383](end_span)
            val lifestyleExp = uiState.metrics.lifestyleExpenses[span_384](start_span)[span_384](end_span)
            val retentionPct = if (incomeBase > 0) round((preSipSaved / incomeBase) * 100.0).toInt() else 0[span_385](start_span)[span_385](end_span)

            Column(modifier = Modifier.fillMaxWidth()) {[span_386](start_span)[span_386](end_span)
                HorizontalPager(
                    state = flowPagerState,[span_387](start_span)[span_387](end_span)
                    pageSpacing = 10.dp,[span_388](start_span)[span_388](end_span)
                    contentPadding = PaddingValues(horizontal = 2.dp),[span_389](start_span)[span_389](end_span)
                    modifier = Modifier
                        .fillMaxWidth()[span_390](start_span)[span_390](end_span)
                        .height(108.dp)[span_391](start_span)[span_391](end_span)
                ) { pageIndex ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()[span_392](start_span)[span_392](end_span)
                            .shadow(2.dp, RoundedCornerShape(16.dp))[span_393](start_span)[span_393](end_span)
                            .clip(RoundedCornerShape(16.dp))[span_394](start_span)[span_394](end_span)
                            .clickable { onOpenBalanceFlowInfo(pageIndex) },[span_395](start_span)[span_395](end_span)
                        shape = RoundedCornerShape(16.dp),[span_396](start_span)[span_396](end_span)
                        color = CardWhite,[span_397](start_span)[span_397](end_span)
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))[span_398](start_span)[span_398](end_span)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()[span_399](start_span)[span_399](end_span)
                                .padding(horizontal = 13.dp, vertical = 9.dp),[span_400](start_span)[span_400](end_span)
                            verticalArrangement = Arrangement.SpaceBetween[span_401](start_span)[span_401](end_span)
                        ) {
                            when (pageIndex) {
                                0 -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_402](start_span)[span_402](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_403](start_span)[span_403](end_span)
                                        verticalAlignment = Alignment.CenterVertically[span_404](start_span)[span_404](end_span)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {[span_405](start_span)[span_405](end_span)
                                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(SoftGreen))[span_406](start_span)[span_406](end_span)
                                            Spacer(modifier = Modifier.width(5.dp))[span_407](start_span)[span_407](end_span)
                                            Text(
                                                text = "LIQUID BANK FLOW",[span_408](start_span)[span_408](end_span)
                                                fontSize = 9.sp,[span_409](start_span)[span_409](end_span)
                                                fontWeight = FontWeight.Black,[span_410](start_span)[span_410](end_span)
                                                color = TextMuted,[span_411](start_span)[span_411](end_span)
                                                letterSpacing = 0.5.sp[span_412](start_span)[span_412](end_span)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {[span_413](start_span)[span_413](end_span)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),[span_414](start_span)[span_414](end_span)
                                                color = (if (bankCashMovement >= 0) SoftGreen else SoftRed).copy(alpha = 0.12f)[span_415](start_span)[span_415](end_span)
                                            ) {
                                                Text(
                                                    text = if (bankCashMovement >= 0) "Cash Added" else "Cash Drawn",[span_416](start_span)[span_416](end_span)
                                                    color = if (bankCashMovement >= 0) SoftGreen else SoftRed,[span_417](start_span)[span_417](end_span)
                                                    fontSize = 8.5.sp,[span_418](start_span)[span_418](end_span)
                                                    fontWeight = FontWeight.Bold,[span_419](start_span)[span_419](end_span)
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)[span_420](start_span)[span_420](end_span)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(3.dp))[span_421](start_span)[span_421](end_span)
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(13.dp))[span_422](start_span)[span_422](end_span)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_423](start_span)[span_423](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_424](start_span)[span_424](end_span)
                                        verticalAlignment = Alignment.Bottom[span_425](start_span)[span_425](end_span)
                                    ) {
                                        Column {[span_426](start_span)[span_426](end_span)
                                            Text(
                                                text = if (isDiscreetMode) "••••••••" else "${if (bankCashMovement >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", bankCashMovement)}",[span_427](start_span)[span_427](end_span)
                                                fontSize = 18.sp,[span_428](start_span)[span_428](end_span)
                                                fontWeight = FontWeight.Black,[span_429](start_span)[span_429](end_span)
                                                color = if (bankCashMovement >= 0) SoftGreen else SoftRed[span_430](start_span)[span_430](end_span)
                                            )
                                            Text("Net Bank Growth", fontSize = 9.5.sp, color = TextMuted)[span_431](start_span)[span_431](end_span)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {[span_432](start_span)[span_432](end_span)
                                            Text(
                                                text = if (isDiscreetMode) "Opening: ••••" else "Opening: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", startBalance)}",[span_433](start_span)[span_433](end_span)
                                                fontSize = 9.5.sp,[span_434](start_span)[span_434](end_span)
                                                color = TextMuted[span_435](start_span)[span_435](end_span)
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "Active: ••••" else "Active: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", endBalance)}",[span_436](start_span)[span_436](end_span)
                                                fontSize = 10.5.sp,[span_437](start_span)[span_437](end_span)
                                                fontWeight = FontWeight.Bold,[span_438](start_span)[span_438](end_span)
                                                color = TextDark[span_439](start_span)[span_439](end_span)
                                            )
                                        }
                                    }
                                }

                                1 -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_440](start_span)[span_440](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_441](start_span)[span_441](end_span)
                                        verticalAlignment = Alignment.CenterVertically[span_442](start_span)[span_442](end_span)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {[span_443](start_span)[span_443](end_span)
                                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(SoftTeal))[span_444](start_span)[span_444](end_span)
                                            Spacer(modifier = Modifier.width(5.dp))[span_445](start_span)[span_445](end_span)
                                            Text(
                                                text = "WEALTH RETENTION",[span_446](start_span)[span_446](end_span)
                                                fontSize = 9.sp,[span_447](start_span)[span_447](end_span)
                                                fontWeight = FontWeight.Black,[span_448](start_span)[span_448](end_span)
                                                color = TextMuted,[span_449](start_span)[span_449](end_span)
                                                letterSpacing = 0.5.sp[span_450](start_span)[span_450](end_span)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {[span_451](start_span)[span_451](end_span)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),[span_452](start_span)[span_452](end_span)
                                                color = SoftTeal.copy(alpha = 0.12f)[span_453](start_span)[span_453](end_span)
                                            ) {
                                                Text(
                                                    text = "$retentionPct% Retained",[span_454](start_span)[span_454](end_span)
                                                    color = SoftTeal,[span_455](start_span)[span_455](end_span)
                                                    fontSize = 8.5.sp,[span_456](start_span)[span_456](end_span)
                                                    fontWeight = FontWeight.Bold,[span_457](start_span)[span_457](end_span)
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)[span_458](start_span)[span_458](end_span)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(3.dp))[span_459](start_span)[span_459](end_span)
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(13.dp))[span_460](start_span)[span_460](end_span)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_461](start_span)[span_461](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_462](start_span)[span_462](end_span)
                                        verticalAlignment = Alignment.Bottom[span_463](start_span)[span_463](end_span)
                                    ) {
                                        Column {[span_464](start_span)[span_464](end_span)
                                            Text(
                                                text = if (isDiscreetMode) "••••••••" else "${if (preSipSaved >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", preSipSaved)}",[span_465](start_span)[span_465](end_span)
                                                fontSize = 18.sp,[span_466](start_span)[span_466](end_span)
                                                fontWeight = FontWeight.Black,[span_467](start_span)[span_467](end_span)
                                                color = if (preSipSaved >= 0) SoftTeal else SoftRed[span_468](start_span)[span_468](end_span)
                                            )
                                            Text("Pre-SIP Saved", fontSize = 9.5.sp, color = TextMuted)[span_469](start_span)[span_469](end_span)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {[span_470](start_span)[span_470](end_span)
                                            Text(
                                                text = if (isDiscreetMode) "Inflow: ••••" else "Inflow: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", incomeBase)}",[span_471](start_span)[span_471](end_span)
                                                fontSize = 9.5.sp,[span_472](start_span)[span_472](end_span)
                                                color = TextMuted[span_473](start_span)[span_473](end_span)
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "Burn: ••••" else "Burn: -${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", lifestyleExp)}",[span_474](start_span)[span_474](end_span)
                                                fontSize = 10.5.sp,[span_475](start_span)[span_475](end_span)
                                                fontWeight = FontWeight.SemiBold,[span_476](start_span)[span_476](end_span)
                                                color = SoftRed[span_477](start_span)[span_477](end_span)
                                            )
                                        }
                                    }
                                }

                                2 -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_478](start_span)[span_478](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_479](start_span)[span_479](end_span)
                                        verticalAlignment = Alignment.CenterVertically[span_480](start_span)[span_480](end_span)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {[span_481](start_span)[span_481](end_span)
                                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(AccentPurple))[span_482](start_span)[span_482](end_span)
                                            Spacer(modifier = Modifier.width(5.dp))[span_483](start_span)[span_483](end_span)
                                            Text(
                                                text = "MONTHLY SURPLUS",[span_484](start_span)[span_484](end_span)
                                                fontSize = 9.sp,[span_485](start_span)[span_485](end_span)
                                                fontWeight = FontWeight.Black,[span_486](start_span)[span_486](end_span)
                                                color = TextMuted,[span_487](start_span)[span_487](end_span)
                                                letterSpacing = 0.5.sp[span_488](start_span)[span_488](end_span)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {[span_489](start_span)[span_489](end_span)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),[span_490](start_span)[span_490](end_span)
                                                color = (if (postSipSurplus >= 0) SoftGreen else SoftRed).copy(alpha = 0.12f)[span_491](start_span)[span_491](end_span)
                                            ) {
                                                Text(
                                                    text = if (postSipSurplus >= 0) "Surplus Safe" else "Deficit",[span_492](start_span)[span_492](end_span)
                                                    color = if (postSipSurplus >= 0) SoftGreen else SoftRed,[span_493](start_span)[span_493](end_span)
                                                    fontSize = 8.5.sp,[span_494](start_span)[span_494](end_span)
                                                    fontWeight = FontWeight.Bold,[span_495](start_span)[span_495](end_span)
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)[span_496](start_span)[span_496](end_span)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(3.dp))[span_497](start_span)[span_497](end_span)
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(13.dp))[span_498](start_span)[span_498](end_span)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_499](start_span)[span_499](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_500](start_span)[span_500](end_span)
                                        verticalAlignment = Alignment.Bottom[span_501](start_span)[span_501](end_span)
                                    ) {
                                        Column {[span_502](start_span)[span_502](end_span)
                                            Text(
                                                text = if (isDiscreetMode) "••••••••" else "${if (postSipSurplus >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", postSipSurplus)}",[span_503](start_span)[span_503](end_span)
                                                fontSize = 18.sp,[span_504](start_span)[span_504](end_span)
                                                fontWeight = FontWeight.Black,[span_505](start_span)[span_505](end_span)
                                                color = if (postSipSurplus >= 0) SoftGreen else SoftRed[span_506](start_span)[span_506](end_span)
                                            )
                                            Text("Unallocated (Post-SIP)", fontSize = 9.5.sp, color = TextMuted)[span_507](start_span)[span_507](end_span)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {[span_508](start_span)[span_508](end_span)
                                            Text(
                                                text = if (isDiscreetMode) "Pre-SIP: ••••" else "Pre-SIP: +${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", preSipSaved)}",[span_509](start_span)[span_509](end_span)
                                                fontSize = 9.5.sp,[span_510](start_span)[span_510](end_span)
                                                color = TextMuted[span_511](start_span)[span_511](end_span)
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "Assets: ••••" else "Assets: -${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualAssets)}",[span_512](start_span)[span_512](end_span)
                                                fontSize = 10.5.sp,[span_513](start_span)[span_513](end_span)
                                                fontWeight = FontWeight.SemiBold,[span_514](start_span)[span_514](end_span)
                                                color = SoftTeal[span_515](start_span)[span_515](end_span)
                                            )
                                        }
                                    }
                                }

                                3 -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_516](start_span)[span_516](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_517](start_span)[span_517](end_span)
                                        verticalAlignment = Alignment.CenterVertically[span_518](start_span)[span_518](end_span)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {[span_519](start_span)[span_519](end_span)
                                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFE57A28)))[span_520](start_span)[span_520](end_span)
                                            Spacer(modifier = Modifier.width(5.dp))[span_521](start_span)[span_521](end_span)
                                            Text(
                                                text = "RESERVES & FLOAT",[span_522](start_span)[span_522](end_span)
                                                fontSize = 9.sp,[span_523](start_span)[span_523](end_span)
                                                fontWeight = FontWeight.Black,[span_524](start_span)[span_524](end_span)
                                                color = TextMuted,[span_525](start_span)[span_525](end_span)
                                                letterSpacing = 0.5.sp[span_526](start_span)[span_526](end_span)
                                            )
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {[span_527](start_span)[span_527](end_span)
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),[span_528](start_span)[span_528](end_span)
                                                color = Color(0xFFE57A28).copy(alpha = 0.12f)[span_529](start_span)[span_529](end_span)
                                            ) {
                                                Text(
                                                    text = "In FDs & Claims",[span_530](start_span)[span_530](end_span)
                                                    color = Color(0xFFE57A28),[span_531](start_span)[span_531](end_span)
                                                    fontSize = 8.5.sp,[span_532](start_span)[span_532](end_span)
                                                    fontWeight = FontWeight.Bold,[span_533](start_span)[span_533](end_span)
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)[span_534](start_span)[span_534](end_span)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(3.dp))[span_535](start_span)[span_535](end_span)
                                            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(13.dp))[span_536](start_span)[span_536](end_span)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),[span_537](start_span)[span_537](end_span)
                                        horizontalArrangement = Arrangement.SpaceBetween,[span_538](start_span)[span_538](end_span)
                                        verticalAlignment = Alignment.Bottom[span_539](start_span)[span_539](end_span)
                                    ) {
                                        Column {[span_540](start_span)[span_540](end_span)
                                            Text(
                                                text = if (isDiscreetMode) "••••••••" else "${if (relocatedGap >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", relocatedGap)}",[span_541](start_span)[span_541](end_span)
                                                fontSize = 18.sp,[span_542](start_span)[span_542](end_span)
                                                fontWeight = FontWeight.Black,[span_543](start_span)[span_543](end_span)
                                                color = Color(0xFFE57A28)[span_544](start_span)[span_544](end_span)
                                            )
                                            Text("Capital Relocated", fontSize = 9.5.sp, color = TextMuted)[span_545](start_span)[span_545](end_span)
                                        }

                                        Column(horizontalAlignment = Alignment.End) {[span_546](start_span)[span_546](end_span)
                                            val expectedPool = startBalance + postSipSurplus[span_547](start_span)[span_547](end_span)
                                            Text(
                                                text = if (isDiscreetMode) "Expected: ••••" else "Expected: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expectedPool)}",[span_548](start_span)[span_548](end_span)
                                                fontSize = 9.5.sp,[span_549](start_span)[span_549](end_span)
                                                color = TextMuted[span_550](start_span)[span_550](end_span)
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "Active: ••••" else "Active: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", endBalance)}",[span_551](start_span)[span_551](end_span)
                                                fontSize = 10.5.sp,[span_552](start_span)[span_552](end_span)
                                                fontWeight = FontWeight.Bold,[span_553](start_span)[span_553](end_span)
                                                color = TextDark[span_554](start_span)[span_554](end_span)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))[span_555](start_span)[span_555](end_span)

                Row(
                    modifier = Modifier.fillMaxWidth(),[span_556](start_span)[span_556](end_span)
                    horizontalArrangement = Arrangement.Center,[span_557](start_span)[span_557](end_span)
                    verticalAlignment = Alignment.CenterVertically[span_558](start_span)[span_558](end_span)
                ) {
                    repeat(4) { idx ->
                        val isSelected = flowPagerState.currentPage == idx[span_559](start_span)[span_559](end_span)
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 14.dp else 4.dp,[span_560](start_span)[span_560](end_span)
                            animationSpec = tween(250),[span_561](start_span)[span_561](end_span)
                            label = "dotWidth[span_562](start_span)"[span_562](end_span)
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)[span_563](start_span)[span_563](end_span)
                                .height(3.5.dp)[span_564](start_span)[span_564](end_span)
                                .width(dotWidth)[span_565](start_span)[span_565](end_span)
                                .clip(RoundedCornerShape(2.dp))[span_566](start_span)[span_566](end_span)
                                .background(if (isSelected) AccentPurple else BorderLight.copy(alpha = 0.7f))[span_567](start_span)[span_567](end_span)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))[span_568](start_span)[span_568](end_span)
        }

        // 4. Category Matrix Header & Switcher
        item {
            Text(text = "Category Matrix", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)[span_569](start_span)[span_569](end_span)
            Spacer(modifier = Modifier.height(8.dp))[span_570](start_span)[span_570](end_span)

            Row(
                modifier = Modifier
                    .fillMaxWidth()[span_571](start_span)[span_571](end_span)
                    .clip(RoundedCornerShape(14.dp))[span_572](start_span)[span_572](end_span)
                    .background(BorderLight.copy(alpha = 0.5f))[span_573](start_span)[span_573](end_span)
                    .padding(4.dp)[span_574](start_span)[span_574](end_span)
            ) {
                listOf(
                    Triple(TransactionType.EXPENSE, "Expenses", SoftRed),
                    Triple(TransactionType.INCOME, "Income", SoftGreen),
                    Triple(TransactionType.ASSET, "Assets / SIP", SoftTeal),
                    Triple(TransactionType.CORPORATE, "Corporate", Color(0xFFE57A28))
                ).forEach { (type, label, color) ->
                    val isSelected = selectedMatrixType == type[span_575](start_span)[span_575](end_span)
                    Box(
                        modifier = Modifier
                            .weight(1f)[span_576](start_span)[span_576](end_span)
                            .clip(RoundedCornerShape(10.dp))[span_577](start_span)[span_577](end_span)
                            .background(if (isSelected) CardWhite else Color.Transparent)[span_578](start_span)[span_578](end_span)
                            .clickable { selectedMatrixType = type }[span_579](start_span)[span_579](end_span)
                            .padding(vertical = 8.dp),[span_580](start_span)[span_580](end_span)
                        contentAlignment = Alignment.Center[span_581](start_span)[span_581](end_span)
                    ) {
                        Text(
                            text = label,[span_582](start_span)[span_582](end_span)
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,[span_583](start_span)[span_583](end_span)
                            fontSize = 11.sp,[span_584](start_span)[span_584](end_span)
                            color = if (isSelected) color else TextMuted,[span_585](start_span)[span_585](end_span)
                            maxLines = 1,[span_586](start_span)[span_586](end_span)
                            overflow = TextOverflow.Ellipsis[span_587](start_span)[span_587](end_span)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))[span_588](start_span)[span_588](end_span)
        }

        if (activeMatrix.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),[span_589](start_span)[span_589](end_span)
                    shape = RoundedCornerShape(18.dp),[span_590](start_span)[span_590](end_span)
                    color = CardWhite[span_591](start_span)[span_591](end_span)
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {[span_592](start_span)[span_592](end_span)
                        Text(text = "No active entries in this segment", fontSize = 12.sp, color = TextMuted)[span_593](start_span)[span_593](end_span)
                    }
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()[span_594](start_span)[span_594](end_span)
                        .shadow(3.dp, RoundedCornerShape(22.dp)),[span_595](start_span)[span_595](end_span)
                    shape = RoundedCornerShape(22.dp),[span_596](start_span)[span_596](end_span)
                    color = CardWhite,[span_597](start_span)[span_597](end_span)
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))[span_598](start_span)[span_598](end_span)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {[span_599](start_span)[span_599](end_span)
                        activeMatrix.forEachIndexed { index, cat ->
                            val isLegacy = remember(uiState.masterCategories, cat) {
                                uiState.masterCategories.any { it.name.equals(cat.category, ignoreCase = true) && it.type == cat.type && it.isLegacy }[span_600](start_span)[span_600](end_span)
                            }
                            val isNew = remember(uiState.masterCategories, cat) {
                                uiState.masterCategories.any { it.name.equals(cat.category, ignoreCase = true) && it.type == cat.type && it.isNew }[span_601](start_span)[span_601](end_span)
                            }

                            CategoryMatrixRow(
                                cat = cat,[span_602](start_span)[span_602](end_span)
                                isExpanded = expandedCategories[cat.category] ?: false,[span_603](start_span)[span_603](end_span)
                                onToggleExpand = {
                                    expandedCategories[cat.category] = !(expandedCategories[cat.category] ?: false)[span_604](start_span)[span_604](end_span)
                                },
                                currencySymbol = userProfile.currencySymbol,[span_605](start_span)[span_605](end_span)
                                isDiscreetMode = isDiscreetMode,[span_606](start_span)[span_606](end_span)
                                isLegacy = isLegacy,[span_607](start_span)[span_607](end_span)
                                isNew = isNew[span_608](start_span)[span_608](end_span)
                            )
                            if (index < activeMatrix.lastIndex) {
                                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)[span_609](start_span)[span_609](end_span)
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
        targetValue = if (isExpanded) 180f else 0f,[span_610](start_span)[span_610](end_span)
        animationSpec = tween(200),[span_611](start_span)[span_611](end_span)
        label = "arrowRotation[span_612](start_span)"[span_612](end_span)
    )

    val isTargetOriented = cat.type == TransactionType.INCOME || cat.type == TransactionType.ASSET[span_613](start_span)[span_613](end_span)

    val progressFraction = if (cat.plannedAmount > 0) {
        (cat.actualAmount / cat.plannedAmount).toFloat().coerceIn(0f, 1f)[span_614](start_span)[span_614](end_span)
    } else 1f[span_615](start_span)[span_615](end_span)

    val utilizationPercentage = if (cat.plannedAmount > 0) {
        ((cat.actualAmount / cat.plannedAmount) * 100).toInt()[span_616](start_span)[span_616](end_span)
    } else 100[span_617](start_span)[span_617](end_span)

    val progressColor = when {
        cat.isOverBudget && !isTargetOriented -> SoftRed
        cat.type == TransactionType.INCOME -> SoftGreen
        cat.type == TransactionType.ASSET -> SoftTeal
        cat.type == TransactionType.CORPORATE -> Color(0xFFE57A28)
        utilizationPercentage >= 85 -> SoftAmber
        else -> AccentPurple
    }[span_618](start_span)[span_618](end_span)

    Column(
        modifier = Modifier
            .fillMaxWidth()[span_619](start_span)[span_619](end_span)
            .clip(RoundedCornerShape(10.dp))[span_620](start_span)[span_620](end_span)
            .clickable(onClick = onToggleExpand)[span_621](start_span)[span_621](end_span)
            .padding(vertical = 8.dp, horizontal = 2.dp)[span_622](start_span)[span_622](end_span)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),[span_623](start_span)[span_623](end_span)
            horizontalArrangement = Arrangement.SpaceBetween,[span_624](start_span)[span_624](end_span)
            verticalAlignment = Alignment.CenterVertically[span_625](start_span)[span_625](end_span)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,[span_626](start_span)[span_626](end_span)
                modifier = Modifier.weight(1f)[span_627](start_span)[span_627](end_span)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)[span_628](start_span)[span_628](end_span)
                        .clip(RoundedCornerShape(10.dp))[span_629](start_span)[span_629](end_span)
                        .background(if (isLegacy) Color(0xFFFFF3E0) else progressColor.copy(alpha = 0.12f)),[span_630](start_span)[span_630](end_span)
                    contentAlignment = Alignment.Center[span_631](start_span)[span_631](end_span)
                ) {
                    Text(
                        text = cat.category.take(1).uppercase(),[span_632](start_span)[span_632](end_span)
                        fontWeight = FontWeight.Black,[span_633](start_span)[span_633](end_span)
                        fontSize = 14.sp,[span_634](start_span)[span_634](end_span)
                        color = if (isLegacy) Color(0xFFE65100) else progressColor[span_635](start_span)[span_635](end_span)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))[span_636](start_span)[span_636](end_span)

                Column(modifier = Modifier.weight(1f, fill = false)) {[span_637](start_span)[span_637](end_span)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,[span_638](start_span)[span_638](end_span)
                        modifier = Modifier.fillMaxWidth()[span_639](start_span)[span_639](end_span)
                    ) {
                        Text(
                            text = cat.category,[span_640](start_span)[span_640](end_span)
                            fontWeight = FontWeight.Bold,[span_641](start_span)[span_641](end_span)
                            fontSize = 13.5.sp,[span_642](start_span)[span_642](end_span)
                            color = TextDark,[span_643](start_span)[span_643](end_span)
                            maxLines = 1,[span_644](start_span)[span_644](end_span)
                            overflow = TextOverflow.Ellipsis,[span_645](start_span)[span_645](end_span)
                            modifier = Modifier.weight(1f, fill = false)[span_646](start_span)[span_646](end_span)
                        )

                        if (isLegacy) {
                            Spacer(modifier = Modifier.width(5.dp))[span_647](start_span)[span_647](end_span)
                            Surface(
                                shape = RoundedCornerShape(4.dp),[span_648](start_span)[span_648](end_span)
                                color = Color(0xFFFFF3E0),[span_649](start_span)[span_649](end_span)
                                border = BorderStroke(0.6.dp, Color(0xFFFFB74D))[span_650](start_span)[span_650](end_span)
                            ) {
                                Text(
                                    text = "Legacy",[span_651](start_span)[span_651](end_span)
                                    fontSize = 8.5.sp,[span_652](start_span)[span_652](end_span)
                                    fontWeight = FontWeight.Bold,[span_653](start_span)[span_653](end_span)
                                    color = Color(0xFFE65100),[span_654](start_span)[span_654](end_span)
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)[span_655](start_span)[span_655](end_span)
                                )
                            }
                        } else if (isNew) {
                            Spacer(modifier = Modifier.width(5.dp))[span_656](start_span)[span_656](end_span)
                            Box(
                                modifier = Modifier
                                    .size(5.dp)[span_657](start_span)[span_657](end_span)
                                    .clip(CircleShape)[span_658](start_span)[span_658](end_span)
                                    .background(Color(0xFF4CAF50))[span_659](start_span)[span_659](end_span)
                            )
                        }

                        if (cat.isOverBudget && !isTargetOriented) {
                            Spacer(modifier = Modifier.width(5.dp))[span_660](start_span)[span_660](end_span)
                            Surface(
                                shape = RoundedCornerShape(4.dp),[span_661](start_span)[span_661](end_span)
                                color = SoftRed.copy(alpha = 0.12f),[span_662](start_span)[span_662](end_span)
                                border = BorderStroke(0.5.dp, SoftRed.copy(alpha = 0.35f)),[span_663](start_span)[span_663](end_span)
                                modifier = Modifier.wrapContentWidth()[span_664](start_span)[span_664](end_span)
                            ) {
                                Text(
                                    text = "Over",[span_665](start_span)[span_665](end_span)
                                    fontSize = 8.5.sp,[span_666](start_span)[span_666](end_span)
                                    fontWeight = FontWeight.Bold,[span_667](start_span)[span_667](end_span)
                                    color = SoftRed,[span_668](start_span)[span_668](end_span)
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)[span_669](start_span)[span_669](end_span)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))[span_670](start_span)[span_670](end_span)

                    val statusText = if (cat.plannedAmount > 0) {
                        val diff = cat.actualAmount - cat.plannedAmount[span_671](start_span)[span_671](end_span)
                        if (isDiscreetMode) {
                            "Target configured[span_672](start_span)"[span_672](end_span)
                        } else if (isTargetOriented) {
                            if (diff >= 0) {
                                "Target achieved (+${currencySymbol}${String.format(Locale.US, "%,.0f", diff)})[span_673](start_span)"[span_673](end_span)
                            } else {
                                "${currencySymbol}${String.format(Locale.US, "%,.0f", abs(diff))} needed to reach target[span_674](start_span)"[span_674](end_span)
                            }
                        } else {
                            val remaining = cat.plannedAmount - cat.actualAmount[span_675](start_span)[span_675](end_span)
                            if (remaining >= 0) {
                                "$currencySymbol${String.format(Locale.US, "%,.0f", remaining)} left of $currencySymbol${String.format(Locale.US, "%,.0f", cat.plannedAmount)}[span_676](start_span)"[span_676](end_span)
                            } else {
                                "Exceeded by $currencySymbol${String.format(Locale.US, "%,.0f", abs(remaining))}[span_677](start_span)"[span_677](end_span)
                            }
                        }
                    } else {
                        if (isLegacy) {
                            "Retiring next month • Logged: $currencySymbol${String.format(Locale.US, "%,.0f", cat.actualAmount)}[span_678](start_span)"[span_678](end_span)
                        } else if (cat.type == TransactionType.CORPORATE) {
                            "Logged actual: $currencySymbol${String.format(Locale.US, "%,.0f", cat.actualAmount)}[span_679](start_span)"[span_679](end_span)
                        } else {
                            "No target limit configured[span_680](start_span)"[span_680](end_span)
                        }
                    }[span_681](start_span)[span_681](end_span)

                    val statusColor = when {
                        cat.isOverBudget && !isTargetOriented -> SoftRed
                        isTargetOriented && cat.plannedAmount > 0 && cat.actualAmount >= cat.plannedAmount -> SoftGreen
                        else -> TextMuted
                    }[span_682](start_span)[span_682](end_span)

                    Text(
                        text = statusText,[span_683](start_span)[span_683](end_span)
                        fontSize = 11.sp,[span_684](start_span)[span_684](end_span)
                        fontWeight = if (cat.isOverBudget && !isTargetOriented) FontWeight.SemiBold else FontWeight.Normal,[span_685](start_span)[span_685](end_span)
                        color = statusColor,[span_686](start_span)[span_686](end_span)
                        maxLines = 1,[span_687](start_span)[span_687](end_span)
                        overflow = TextOverflow.Ellipsis[span_688](start_span)[span_688](end_span)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))[span_689](start_span)[span_689](end_span)

            Row(verticalAlignment = Alignment.CenterVertically) {[span_690](start_span)[span_690](end_span)
                Column(horizontalAlignment = Alignment.End) {[span_691](start_span)[span_691](end_span)
                    Text(
                        text = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", cat.actualAmount)}",[span_692](start_span)[span_692](end_span)
                        fontWeight = FontWeight.Black,[span_693](start_span)[span_693](end_span)
                        fontSize = 14.sp,[span_694](start_span)[span_694](end_span)
                        color = if (cat.isOverBudget && !isTargetOriented) SoftRed else TextDark[span_695](start_span)[span_695](end_span)
                    )
                    if (cat.plannedAmount > 0) {
                        Text(
                            text = "$utilizationPercentage%",[span_696](start_span)[span_696](end_span)
                            fontSize = 10.sp,[span_697](start_span)[span_697](end_span)
                            fontWeight = FontWeight.SemiBold,[span_698](start_span)[span_698](end_span)
                            color = progressColor[span_699](start_span)[span_699](end_span)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))[span_700](start_span)[span_700](end_span)

                Icon(
                    imageVector = Icons.Default.ExpandMore,[span_701](start_span)[span_701](end_span)
                    contentDescription = "Expand",[span_702](start_span)[span_702](end_span)
                    tint = TextMuted,[span_703](start_span)[span_703](end_span)
                    modifier = Modifier
                        .size(16.dp)[span_704](start_span)[span_704](end_span)
                        .rotate(rotation)[span_705](start_span)[span_705](end_span)
                )
            }
        }

        if (cat.plannedAmount > 0) {
            Spacer(modifier = Modifier.height(7.dp))[span_706](start_span)[span_706](end_span)
            LinearProgressIndicator(
                progress = { progressFraction },[span_707](start_span)[span_707](end_span)
                modifier = Modifier
                    .fillMaxWidth()[span_708](start_span)[span_708](end_span)
                    .height(3.5.dp)[span_709](start_span)[span_709](end_span)
                    .clip(RoundedCornerShape(2.dp)),[span_710](start_span)[span_710](end_span)
                color = progressColor,[span_711](start_span)[span_711](end_span)
                trackColor = BorderLight.copy(alpha = 0.5f)[span_712](start_span)[span_712](end_span)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,[span_713](start_span)[span_713](end_span)
            enter = expandVertically() + fadeIn(),[span_714](start_span)[span_714](end_span)
            exit = shrinkVertically() + fadeOut()[span_715](start_span)[span_715](end_span)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()[span_716](start_span)[span_716](end_span)
                    .padding(top = 10.dp)[span_717](start_span)[span_717](end_span)
                    .clip(RoundedCornerShape(12.dp))[span_718](start_span)[span_718](end_span)
                    .background(CanvasLight)[span_719](start_span)[span_719](end_span)
                    .padding(10.dp)[span_720](start_span)[span_720](end_span)
            ) {
                if (cat.activeSubcategories.isEmpty()) {
                    Text(text = "No logged transactions in subcategories", fontSize = 11.sp, color = TextMuted)[span_721](start_span)[span_721](end_span)
                } else {
                    Text(
                        text = "SUBCATEGORY CONTRIBUTIONS",[span_722](start_span)[span_722](end_span)
                        fontSize = 9.5.sp,[span_723](start_span)[span_723](end_span)
                        fontWeight = FontWeight.Black,[span_724](start_span)[span_724](end_span)
                        color = TextMuted,[span_725](start_span)[span_725](end_span)
                        letterSpacing = 0.5.sp,[span_726](start_span)[span_726](end_span)
                        modifier = Modifier.padding(bottom = 4.dp)[span_727](start_span)[span_727](end_span)
                    )

                    cat.activeSubcategories.forEach { sub ->
                        val subPercentage = if (cat.actualAmount > 0) {
                            ((sub.amount / cat.actualAmount) * 100).toInt()[span_728](start_span)[span_728](end_span)
                        } else 0[span_729](start_span)[span_729](end_span)

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {[span_730](start_span)[span_730](end_span)
                            Row(
                                modifier = Modifier.fillMaxWidth(),[span_731](start_span)[span_731](end_span)
                                horizontalArrangement = Arrangement.SpaceBetween,[span_732](start_span)[span_732](end_span)
                                verticalAlignment = Alignment.CenterVertically[span_733](start_span)[span_733](end_span)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {[span_734](start_span)[span_734](end_span)
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)[span_735](start_span)[span_735](end_span)
                                            .clip(CircleShape)[span_736](start_span)[span_736](end_span)
                                            .background(progressColor)[span_737](start_span)[span_737](end_span)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))[span_738](start_span)[span_738](end_span)
                                    Text(text = sub.name, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextDark)[span_739](start_span)[span_739](end_span)
                                }

                                Text(
                                    text = if (isDiscreetMode) "•••• ($subPercentage%)" else "$currencySymbol${String.format(Locale.US, "%,.2f", sub.amount)} ($subPercentage%)",[span_740](start_span)[span_740](end_span)
                                    fontSize = 11.5.sp,[span_741](start_span)[span_741](end_span)
                                    fontWeight = FontWeight.Bold,[span_742](start_span)[span_742](end_span)
                                    color = TextDark[span_743](start_span)[span_743](end_span)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.5.dp))[span_744](start_span)[span_744](end_span)
                            LinearProgressIndicator(
                                progress = { (subPercentage / 100f).coerceIn(0f, 1f) },[span_745](start_span)[span_745](end_span)
                                modifier = Modifier
                                    .fillMaxWidth()[span_746](start_span)[span_746](end_span)
                                    .height(3.dp)[span_747](start_span)[span_747](end_span)
                                    .clip(RoundedCornerShape(1.5.dp)),[span_748](start_span)[span_748](end_span)
                                color = progressColor.copy(alpha = 0.65f),[span_749](start_span)[span_749](end_span)
                                trackColor = BorderLight.copy(alpha = 0.5f)[span_750](start_span)[span_750](end_span)
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),[span_751](start_span)[span_751](end_span)
        shape = RoundedCornerShape(9.dp),[span_752](start_span)[span_752](end_span)
        color = CardWhite,[span_753](start_span)[span_753](end_span)
        border = BorderStroke(0.7.dp, BorderLight.copy(alpha = 0.6f))[span_754](start_span)[span_754](end_span)
    ) {
        Column(modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp)) {[span_755](start_span)[span_755](end_span)
            Row(verticalAlignment = Alignment.CenterVertically) {[span_756](start_span)[span_756](end_span)
                Box(
                    modifier = Modifier
                        .size(4.dp)[span_757](start_span)[span_757](end_span)
                        .clip(CircleShape)[span_758](start_span)[span_758](end_span)
                        .background(tintColor)[span_759](start_span)[span_759](end_span)
                )
                Spacer(modifier = Modifier.width(3.dp))[span_760](start_span)[span_760](end_span)
                Text(text = title, fontSize = 8.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)[span_761](start_span)[span_761](end_span)
            }
            Spacer(modifier = Modifier.height(1.dp))[span_762](start_span)[span_762](end_span)
            Text(
                text = amount,[span_763](start_span)[span_763](end_span)
                fontSize = 10.5.sp,[span_764](start_span)[span_764](end_span)
                fontWeight = FontWeight.Bold,[span_765](start_span)[span_765](end_span)
                color = tintColor[span_766](start_span)[span_766](end_span)
            )
        }
    }
}
