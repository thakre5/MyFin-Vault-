package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.ExcelExportManager
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.*
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

data class QuarterlyMetrics(
    val quarterLabel: String,
    val quarterIndex: Int,
    val totalIncome: Double,
    val totalExpenses: Double,
    val totalAssets: Double,
    val netSurplus: Double,
    val savingsRate: Double
)

data class CategoryAnnualTrajectory(
    val categoryName: String,
    val annualTotal: Double,
    val percentageOfTotal: Double,
    val monthlyAmounts: List<Double>,
    val peakMonthIndex: Int,
    val peakMonthAmount: Double
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun YearlyScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToMonth: (year: Int, month: Int) -> Unit = { _, _ -> },
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToTaxonomy: () -> Unit = {},
    onNavigateToVaults: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.monthlyUiState.collectAsState()
    val yearlyState by viewModel.yearlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 4 })
    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()
    val pageTitles = remember { listOf("Cashflow", "12 Months", "Assets & Wealth", "Audit") }

    var inspectedMonth by remember { mutableStateOf<YearlyMonthData?>(null) }
    var isDiscreetMode by remember { mutableStateOf(false) }

    // Live data from ViewModel
    val yearlyMonthsData = yearlyState.yearlyMonths
    val annualIncome = yearlyState.totalYearlyIncome
    val annualExpenses = yearlyState.totalYearlyExpense
    val annualAssets = yearlyState.totalYearlyAssets
    val annualNetSurplus = yearlyState.annualNetSurplus
    val annualLifestyleExpenses = yearlyState.annualLifestyleExpenses
    val reimbursementStatus = yearlyState.reimbursementStatus
    val wealthMetrics = yearlyState.assetWealthMetrics
    val multiYearAssets = yearlyState.multiYearAssets
    val allYearTransactions = yearlyState.allYearTransactions

    val annualSavingsRate = if (annualIncome > 0) ((annualNetSurplus / annualIncome) * 100).coerceIn(0.0, 100.0) else 0.0

    // Wealth Goal Target
    val annualTargetGoal = remember(annualIncome, userProfile.baseMonthlyIncome, userProfile.fortressThreshold) {
        val base = if (annualIncome > 0) annualIncome else (userProfile.baseMonthlyIncome * 12)
        maxOf(base * 0.25, userProfile.fortressThreshold, 25000.0)
    }
    val currentWealthAccumulated = (annualAssets + annualNetSurplus).coerceAtLeast(0.0)
    val goalCompletionPercentage = (currentWealthAccumulated / annualTargetGoal).toFloat().coerceIn(0f, 1f)

    // Fiscal Quarters (Q1 to Q4)
    val quarterlyData = remember(yearlyMonthsData) {
        if (yearlyMonthsData.size >= 12) {
            listOf(
                "Q1" to yearlyMonthsData.subList(0, 3),
                "Q2" to yearlyMonthsData.subList(3, 6),
                "Q3" to yearlyMonthsData.subList(6, 9),
                "Q4" to yearlyMonthsData.subList(9, 12)
            ).mapIndexed { qIdx, (label, months) ->
                val qInc = months.sumOf { it.income }
                val qExp = months.sumOf { it.expenses }
                val qAst = months.sumOf { it.assets }
                val qNet = qInc - qExp - qAst
                val qRate = if (qInc > 0) ((qNet / qInc) * 100).coerceIn(0.0, 100.0) else 0.0
                QuarterlyMetrics(
                    quarterLabel = label,
                    quarterIndex = qIdx + 1,
                    totalIncome = qInc,
                    totalExpenses = qExp,
                    totalAssets = qAst,
                    netSurplus = qNet,
                    savingsRate = qRate
                )
            }
        } else emptyList()
    }

    // Category Breakdown & Trajectories
    val categoryTrajectories = remember(allYearTransactions, annualExpenses) {
        val txCal = Calendar.getInstance()
        val expenseTxs = allYearTransactions.filter { it.type == TransactionType.EXPENSE }
        val grouped = expenseTxs.groupBy { it.category }

        grouped.map { (cat, txs) ->
            val total = txs.sumOf { it.amount }
            val monthlySums = DoubleArray(12) { 0.0 }
            for (tx in txs) {
                txCal.timeInMillis = tx.date
                val mIdx = txCal.get(Calendar.MONTH).coerceIn(0, 11)
                monthlySums[mIdx] += tx.amount
            }
            val peakMonth = monthlySums.indices.maxByOrNull { monthlySums[it] } ?: 0
            CategoryAnnualTrajectory(
                categoryName = cat,
                annualTotal = total,
                percentageOfTotal = if (annualExpenses > 0) (total / annualExpenses) * 100.0 else 0.0,
                monthlyAmounts = monthlySums.toList(),
                peakMonthIndex = peakMonth,
                peakMonthAmount = monthlySums[peakMonth]
            )
        }.sortedByDescending { it.annualTotal }
    }

    val xlsxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val ok = ExcelExportManager.exportToUri(context, it, userProfile.currencySymbol)
                Toast.makeText(context, if (ok) "Annual Statement (.xlsx) saved!" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val ok = viewModel.exportCsvToUri(context, it)
                Toast.makeText(context, if (ok) "Annual Tax Ledger (.csv) exported!" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val fabActions = remember(uiState.selectedYear) {
        listOf(
            DockFabAction(
                icon = Icons.Default.TableChart,
                label = "Export Statement (.xlsx)",
                onClick = {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    xlsxExportLauncher.launch("MyFin_Annual_${uiState.selectedYear}_$timeStamp.xlsx")
                }
            ),
            DockFabAction(
                icon = Icons.Default.ReceiptLong,
                label = "Tax Ledger (.csv)",
                onClick = {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    csvExportLauncher.launch("MyFin_Tax_Ledger_${uiState.selectedYear}_$timeStamp.csv")
                }
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
            .nestedScroll(scrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pinned Top Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasLight)
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(38.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Drawer",
                            tint = TextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier.align(Alignment.Center),
                        shape = RoundedCornerShape(20.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectYear(uiState.selectedYear - 1)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Year", modifier = Modifier.size(18.dp), tint = TextDark)
                            }

                            Text(
                                text = "Year ${uiState.selectedYear}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = TextDark,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )

                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    viewModel.selectYear(uiState.selectedYear + 1)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Year", modifier = Modifier.size(18.dp), tint = TextDark)
                            }
                        }
                    }

                    IconButton(
                        onClick = { isDiscreetMode = !isDiscreetMode },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isDiscreetMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Privacy Toggle",
                            tint = if (isDiscreetMode) AccentPurple else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(CanvasLight, CanvasLight.copy(alpha = 0f))
                            )
                        )
                )
            }

            // 4-Page Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    // ==========================================
                    // TAB 0: CASHFLOW (Orange Funnel Ribbon Card)
                    // ==========================================
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
                        ) {
                            item(key = "orange_cashflow_card") {
                                CashflowFunnelStreamCard(
                                    title = "Cashflow Velocity",
                                    subtitle = "Annual Outflow Swell & Burn Acceleration",
                                    yearlyMonths = yearlyMonthsData,
                                    annualIncome = annualIncome,
                                    annualExpenses = annualExpenses,
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode,
                                    topCategories = categoryTrajectories.take(3)
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            // Reimbursable Work Spends Callout
                            if (reimbursementStatus.totalWorkExpenses > 0.0) {
                                item(key = "reimbursement_offset_banner") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = CardWhite,
                                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
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
                                                    Icons.Default.WorkOutline,
                                                    contentDescription = null,
                                                    tint = AccentPurple,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(14.dp))

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Corporate Reimbursements",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = TextDark
                                                )
                                                Text(
                                                    text = if (reimbursementStatus.isSettled)
                                                        "All work expenses are fully settled."
                                                    else
                                                        "Pending claim recovery of ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", reimbursementStatus.pendingReimbursement)}",
                                                    fontSize = 11.sp,
                                                    color = if (reimbursementStatus.isSettled) SoftGreen else SoftRed
                                                )
                                            }

                                            Column(horizontalAlignment = Alignment.End) {
                                                Text(
                                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualLifestyleExpenses)}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 14.sp,
                                                    color = TextDark
                                                )
                                                Text("True Lifestyle", fontSize = 9.5.sp, color = TextMuted)
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(18.dp))
                                }
                            }

                            item(key = "cashflow_quarterly_grid") {
                                Text(
                                    text = "Fiscal Quarter Retention",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    quarterlyData.forEach { q ->
                                        Surface(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(14.dp),
                                            color = CardWhite,
                                            border = BorderStroke(0.7.dp, BorderLight)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text(q.quarterLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (isDiscreetMode) "••••" else "${q.savingsRate.toInt()}%",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (q.netSurplus >= 0) SoftTeal else SoftRed
                                                )
                                                Text(
                                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${(q.netSurplus / 1000).toInt()}k",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextDark
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // ==========================================
                    // TAB 1: 12 MONTHS (Blue Spindle Rhythm Card & Fully Scrollable Months)
                    // ==========================================
                    1 -> {
                        val chunkedMonths = remember(yearlyMonthsData) { yearlyMonthsData.chunked(2) }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
                        ) {
                            item(key = "blue_seasonal_rhythm_card") {
                                SeasonalRhythmSpindleCard(
                                    title = "Monthly Outflow Rhythm",
                                    subtitle = "Seasonal Burn Variation Around Annual Mean",
                                    yearlyMonths = yearlyMonthsData,
                                    quarterlyData = quarterlyData,
                                    annualExpenses = annualExpenses,
                                    annualNetSurplus = annualNetSurplus,
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode,
                                    topCategories = categoryTrajectories.take(3)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            item(key = "timeline_grid_title") {
                                Text(
                                    text = "Monthly Financial Breakdown",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            items(chunkedMonths, key = { it.first().monthIndex }) { rowPair ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    MonthGridTimelineCard(
                                        data = rowPair[0],
                                        currencySymbol = userProfile.currencySymbol,
                                        isDiscreet = isDiscreetMode,
                                        onTapMonth = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            inspectedMonth = rowPair[0]
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (rowPair.size > 1) {
                                        MonthGridTimelineCard(
                                            data = rowPair[1],
                                            currencySymbol = userProfile.currencySymbol,
                                            isDiscreet = isDiscreetMode,
                                            onTapMonth = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                inspectedMonth = rowPair[1]
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // ==========================================
                    // TAB 2: ASSETS & WEALTH (Live Wave Heart & Multi-Year Flow)
                    // ==========================================
                    2 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
                        ) {
                            item(key = "live_heart_goal_card") {
                                LiveAnimatedGoalHeartCard(
                                    title = "Annual Wealth Accumulation Goal",
                                    currentAmount = currentWealthAccumulated,
                                    targetAmount = annualTargetGoal,
                                    completionRatio = goalCompletionPercentage,
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // Asset Wealth & NPA Provisioning Card
                            item(key = "asset_wealth_breakdown_card") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(4.dp, RoundedCornerShape(22.dp)),
                                    shape = RoundedCornerShape(22.dp),
                                    color = CardWhite,
                                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
                                ) {
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Realizable Net Worth", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.realizableNetWorth)}",
                                                    fontSize = 22.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = AccentPurple
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = SoftTeal.copy(alpha = 0.14f)
                                            ) {
                                                Text(
                                                    text = "Gross ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.grossWealth)}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SoftTeal,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            QuickMetricTile(
                                                modifier = Modifier.weight(1f),
                                                label = "Investments",
                                                value = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.totalInvestments)}",
                                                tint = SoftTeal
                                            )
                                            QuickMetricTile(
                                                modifier = Modifier.weight(1f),
                                                label = "Liquid Cash",
                                                value = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.liquidReserves)}",
                                                tint = SoftGreen
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            QuickMetricTile(
                                                modifier = Modifier.weight(1f),
                                                label = "Active Loans Out",
                                                value = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.activeReceivables)}",
                                                tint = AccentPurple
                                            )
                                            QuickMetricTile(
                                                modifier = Modifier.weight(1f),
                                                label = "NPA Bad Debt",
                                                value = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.npaWrittenOff)}",
                                                tint = SoftRed
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            item(key = "multi_year_asset_flow_card") {
                                MultiYearAssetFlowCard(
                                    multiYearAssets = multiYearAssets,
                                    selectedYear = uiState.selectedYear,
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // ==========================================
                    // TAB 3: AUDIT & CATEGORY SPECTRUM
                    // ==========================================
                    3 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
                        ) {
                            item(key = "radar_header") {
                                Text(text = "Annual Category Pareto", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Text(text = "Cumulative spend distribution across entire year", fontSize = 11.5.sp, color = TextMuted)
                                Spacer(modifier = Modifier.height(14.dp))

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(3.dp, RoundedCornerShape(22.dp)),
                                    shape = RoundedCornerShape(22.dp),
                                    color = CardWhite,
                                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AnnualRadarSpiderCanvas(
                                            categorySums = categoryTrajectories.map { it.annualTotal }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            item(key = "trajectories_title") {
                                Text(text = "Annual Trajectory by Category", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                Text(text = "12-month burn pattern & peak month spikes", fontSize = 11.sp, color = TextMuted)
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (categoryTrajectories.isEmpty()) {
                                item(key = "empty_trajectories") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = CardWhite
                                    ) {
                                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                            Text(text = "No recorded expenses for this year", fontSize = 12.sp, color = TextMuted)
                                        }
                                    }
                                }
                            } else {
                                items(categoryTrajectories, key = { it.categoryName }) { item ->
                                    CategoryTrajectoryRowCard(
                                        item = item,
                                        currencySymbol = userProfile.currencySymbol,
                                        isDiscreet = isDiscreetMode
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Dissolve Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, CanvasLight.copy(alpha = 0.85f), CanvasLight)
                    )
                )
                .zIndex(2.5f)
        )

        // Floating Pager Indicator
        FloatingPagerIndicator(
            pagerState = pagerState,
            pageTitles = pageTitles,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 22.dp, bottom = 78.dp)
                .zIndex(3.5f)
        )

        // Bottom Navigation Dock
        AppBottomDock(
            currentSelection = NavigationTarget.YEARLY_VIEW,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.MONTHLY_VIEW -> onNavigateToDashboard()
                    NavigationTarget.BUDGET_PLANNER -> onNavigateToPlanner()
                    NavigationTarget.VAULT_ACCOUNTS -> onNavigateToVaults()
                    NavigationTarget.REPORTS_ANALYTICS -> onNavigateToAnalytics()
                    NavigationTarget.DATA_SET -> onNavigateToTaxonomy()
                    else -> {}
                }
            },
            fabActions = fabActions,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        )

        // Month Quick-Inspect Bottom Sheet
        inspectedMonth?.let { mData ->
            ModalBottomSheet(
                onDismissRequest = { inspectedMonth = null },
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
                        Column {
                            Text("${mData.monthName} ${uiState.selectedYear}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
                            Text(if (mData.isFuture) "Planned Cycle" else "Completed Accounting Cycle", fontSize = 11.5.sp, color = TextMuted)
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (mData.netSavings >= 0) SoftGreen.copy(alpha = 0.14f) else SoftRed.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = if (isDiscreetMode) "••••" else if (mData.netSavings >= 0) "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", mData.netSavings)}" else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(mData.netSavings))}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = if (mData.netSavings >= 0) SoftGreen else SoftRed,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Inflow",
                            value = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", mData.income)}",
                            tint = SoftGreen
                        )
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Outflow",
                            value = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", mData.expenses)}",
                            tint = AccentPurple
                        )
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Assets SIP",
                            value = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", mData.assets)}",
                            tint = SoftTeal
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text("Top Expenses in ${mData.monthName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(8.dp))

                    val topMonthCategories = mData.transactions
                        .filter { it.type == TransactionType.EXPENSE }
                        .groupBy { it.category }
                        .mapValues { it.value.sumOf { tx -> tx.amount } }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(3)

                    if (topMonthCategories.isEmpty()) {
                        Text("No recorded expenses for this month.", fontSize = 11.5.sp, color = TextMuted)
                    } else {
                        topMonthCategories.forEach { (cat, amt) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat, fontSize = 12.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                Text(
                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", amt)}",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPurple
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            inspectedMonth = null
                            viewModel.selectMonth(mData.monthIndex)
                            onNavigateToMonth(uiState.selectedYear, mData.monthIndex)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Open ${mData.monthName} Monthly Dashboard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// 1. RE-ENGINEERED DYNAMIC CASHFLOW FUNNEL CARD (ORANGE)
// =========================================================

@Composable
private fun CashflowFunnelStreamCard(
    title: String,
    subtitle: String,
    yearlyMonths: List<YearlyMonthData>,
    annualIncome: Double,
    annualExpenses: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    topCategories: List<CategoryAnnualTrajectory>
) {
    var cumulativeBurn = 0.0
    val cumulativeExpenses = yearlyMonths.map { m ->
        cumulativeBurn += m.expenses
        cumulativeBurn
    }

    val monthlyAvgBurn = if (annualExpenses > 0) annualExpenses / 12.0 else 0.0
    val savingsRate = if (annualIncome > 0) (((annualIncome - annualExpenses) / annualIncome) * 100).toInt() else 0

    val q1Burn = cumulativeExpenses.getOrElse(2) { 0.0 }
    val midBurn = cumulativeExpenses.getOrElse(5) { 0.0 }
    val endBurn = cumulativeExpenses.lastOrNull() ?: 0.0

    val q1Inflow = annualIncome * 0.25
    val midInflow = annualIncome * 0.50
    val endInflow = annualIncome

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
            Text(subtitle, fontSize = 11.5.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(14.dp))

            // Checkpoint Numbers Row (Top: Inflow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(q1Inflow, midInflow, endInflow).forEach { amt ->
                    Text(
                        text = if (isDiscreet) "••••" else String.format(Locale.US, "%,.0f", amt),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            SmoothFunnelCanvas(
                cumulativeExpenses = cumulativeExpenses,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Checkpoint Numbers Row (Bottom: Actual Outflow Burn)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(q1Burn, midBurn, endBurn).forEach { amt ->
                    Text(
                        text = if (isDiscreet) "••••" else String.format(Locale.US, "%,.0f", amt),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftGreen))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Monthly Avg Burn", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", monthlyAvgBurn)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = "↑ $savingsRate% Net Saved",
                        fontSize = 10.sp,
                        color = SoftGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFFAB40)))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Annual Outflow", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", annualExpenses)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = if (isDiscreet) "••••" else "of $currencySymbol${String.format(Locale.US, "%,.0f", annualIncome)} Inflow",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            topCategories.take(3).forEach { cat ->
                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat.categoryName, fontSize = 13.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", cat.annualTotal)}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }
        }
    }
}

@Composable
private fun SmoothFunnelCanvas(
    cumulativeExpenses: List<Double>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cY = h / 2f

        val col1X = w * 0.22f
        val col2X = w * 0.52f
        val col3X = w * 0.82f
        listOf(col1X, col2X, col3X).forEach { xPos ->
            drawLine(
                color = Color(0xFFEEEEEE),
                start = Offset(xPos, 4f),
                end = Offset(xPos, h - 4f),
                strokeWidth = 1.dp.toPx()
            )
        }

        val maxBurn = cumulativeExpenses.lastOrNull()?.coerceAtLeast(10.0) ?: 10.0

        val layers = listOf(1.0f to 0.22f, 0.70f to 0.50f, 0.42f to 0.90f)
        layers.forEach { (scale, alpha) ->
            val topP = Path()
            val bottomP = Path()

            val stepX = w / 11f
            for (i in 0..11) {
                val x = i * stepX
                val burn = cumulativeExpenses.getOrElse(i) { 0.0 }
                val ratio = (burn / maxBurn).toFloat().coerceIn(0f, 1f)
                val curveT = i / 11f
                val baselineShape = 0.12f + 0.88f * (curveT * curveT)
                val thickness = (h * 0.44f) * (baselineShape * (0.6f + 0.4f * ratio)) * scale

                val yTop = cY - thickness
                val yBottom = cY + thickness

                if (i == 0) {
                    topP.moveTo(x, yTop)
                    bottomP.moveTo(x, yBottom)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevBurn = cumulativeExpenses.getOrElse(i - 1) { 0.0 }
                    val prevRatio = (prevBurn / maxBurn).toFloat().coerceIn(0f, 1f)
                    val prevT = (i - 1) / 11f
                    val prevBase = 0.12f + 0.88f * (prevT * prevT)
                    val prevThickness = (h * 0.44f) * (prevBase * (0.6f + 0.4f * prevRatio)) * scale

                    val cX = (prevX + x) / 2
                    topP.cubicTo(cX, cY - prevThickness, cX, yTop, x, yTop)
                    bottomP.cubicTo(cX, cY + prevThickness, cX, yBottom, x, yBottom)
                }
            }

            val ribbon = Path().apply {
                addPath(topP)
                lineTo(w, cY)
                lineTo(w, h)
                addPath(bottomP)
                close()
            }

            drawPath(
                path = ribbon,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFFAB40).copy(alpha = alpha),
                        Color(0xFFFF6E40).copy(alpha = alpha),
                        Color(0xFFFF3D00).copy(alpha = alpha)
                    )
                )
            )
        }
    }
}

// =========================================================
// 2. RE-ENGINEERED SEASONAL RHYTHM SPINDLE CARD (BLUE)
// =========================================================

@Composable
private fun SeasonalRhythmSpindleCard(
    title: String,
    subtitle: String,
    yearlyMonths: List<YearlyMonthData>,
    quarterlyData: List<QuarterlyMetrics>,
    annualExpenses: Double,
    annualNetSurplus: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    topCategories: List<CategoryAnnualTrajectory>
) {
    val quarterlyAvg = if (annualExpenses > 0) annualExpenses / 4.0 else 0.0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
            Text(subtitle, fontSize = 11.5.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp),
                contentAlignment = Alignment.Center
            ) {
                SpindleFlowCanvas(
                    monthlyExpenses = yearlyMonths.map { it.expenses },
                    modifier = Modifier.fillMaxSize()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0, 1, 2).forEach { qIdx ->
                        val qAmt = quarterlyData.getOrNull(qIdx)?.totalExpenses ?: 0.0
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardWhite,
                            shadowElevation = 3.dp,
                            border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
                        ) {
                            Text(
                                text = if (isDiscreet) "••••" else String.format(Locale.US, "%,.0f", qAmt),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Jan", "Mar", "May", "Jul", "Sep", "Nov", "Dec").forEach { m ->
                    Text(m, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF2979FF)))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Quarterly Avg Outflow", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", quarterlyAvg)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = "Seasonality Wave",
                        fontSize = 10.sp,
                        color = Color(0xFF2979FF),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Net Retained", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", annualNetSurplus)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = if (annualNetSurplus >= 0) SoftTeal else SoftRed
                    )
                    Text(
                        text = "Total Annual Surplus",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            topCategories.take(3).forEach { cat ->
                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat.categoryName, fontSize = 13.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", cat.annualTotal)}",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }
            }
        }
    }
}

@Composable
private fun SpindleFlowCanvas(
    monthlyExpenses: List<Double>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cY = h / 2f

        val col1X = w * 0.22f
        val col2X = w * 0.52f
        val col3X = w * 0.82f
        listOf(col1X, col2X, col3X).forEach { xPos ->
            drawLine(
                color = Color(0xFFEEEEEE),
                start = Offset(xPos, 8f),
                end = Offset(xPos, h - 8f),
                strokeWidth = 1.dp.toPx()
            )
        }

        val maxExp = monthlyExpenses.maxOrNull()?.coerceAtLeast(10.0) ?: 10.0

        val layers = listOf(1.0f to 0.22f, 0.70f to 0.50f, 0.44f to 0.90f)
        layers.forEach { (scale, alpha) ->
            val topP = Path()
            val bottomP = Path()

            val stepX = w / 11f
            for (i in 0..11) {
                val x = i * stepX
                val exp = monthlyExpenses.getOrElse(i) { 0.0 }
                val ratio = (exp / maxExp).toFloat().coerceIn(0f, 1f)

                val t = i / 11f
                val spindleEnvelope = 0.10f + 0.90f * (4f * t * (1f - t))
                val thickness = (h * 0.44f) * (spindleEnvelope * (0.6f + 0.4f * ratio)) * scale

                val yTop = cY - thickness
                val yBottom = cY + thickness

                if (i == 0) {
                    topP.moveTo(x, yTop)
                    bottomP.moveTo(x, yBottom)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevExp = monthlyExpenses.getOrElse(i - 1) { 0.0 }
                    val prevRatio = (prevExp / maxExp).toFloat().coerceIn(0f, 1f)
                    val prevT = (i - 1) / 11f
                    val prevEnvelope = 0.10f + 0.90f * (4f * prevT * (1f - prevT))
                    val prevThickness = (h * 0.44f) * (prevEnvelope * (0.6f + 0.4f * prevRatio)) * scale

                    val cX = (prevX + x) / 2
                    topP.cubicTo(cX, cY - prevThickness, cX, yTop, x, yTop)
                    bottomP.cubicTo(cX, cY + prevThickness, cX, yBottom, x, yBottom)
                }
            }

            val ribbon = Path().apply {
                addPath(topP)
                lineTo(w, cY)
                lineTo(w, h)
                addPath(bottomP)
                close()
            }

            drawPath(
                path = ribbon,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF82B1FF).copy(alpha = alpha),
                        Color(0xFF2979FF).copy(alpha = alpha),
                        Color(0xFF82B1FF).copy(alpha = alpha)
                    )
                )
            )
        }
    }
}

// =========================================================
// 3. LIVE CONTINUOUS LIQUID WAVE HEART CARD
// =========================================================

@Composable
private fun LiveAnimatedGoalHeartCard(
    title: String,
    currentAmount: Double,
    targetAmount: Double,
    completionRatio: Float,
    currencySymbol: String,
    isDiscreet: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFFEDE9FE)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isDiscreet) "•••• / ••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", currentAmount)} / $currencySymbol${String.format(Locale.US, "%,.0f", targetAmount)}",
                fontSize = 13.5.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(220.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                CleanLivingHeartCanvas(
                    fillPercentage = completionRatio,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = "${(completionRatio * 100).toInt()}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            val gap = (targetAmount - currentAmount).coerceAtLeast(0.0)
            Text(
                text = if (isDiscreet) "Accumulate assets to reach your annual target." else "Deploy $currencySymbol${String.format(Locale.US, "%,.0f", gap)} more to hit your compounding milestone.",
                fontSize = 12.5.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CleanLivingHeartCanvas(
    fillPercentage: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeartWaveTransition")

    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase1"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase2"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val gridStep = 24.dp.toPx()
        var currentX = 0f
        while (currentX < w) {
            drawLine(color = Color(0xFFF3F4F6), start = Offset(currentX, 0f), end = Offset(currentX, h), strokeWidth = 1.dp.toPx())
            currentX += gridStep
        }
        var currentY = 0f
        while (currentY < h) {
            drawLine(color = Color(0xFFF3F4F6), start = Offset(0f, currentY), end = Offset(w, currentY), strokeWidth = 1.dp.toPx())
            currentY += gridStep
        }

        val heartPath = Path().apply {
            moveTo(w / 2f, h * 0.28f)
            cubicTo(w * 0.28f, h * 0.04f, w * 0.02f, h * 0.22f, w * 0.02f, h * 0.48f)
            cubicTo(w * 0.02f, h * 0.70f, w * 0.26f, h * 0.84f, w / 2f, h * 0.98f)
            cubicTo(w * 0.74f, h * 0.84f, w * 0.98f, h * 0.70f, w * 0.98f, h * 0.48f)
            cubicTo(w * 0.98f, h * 0.22f, w * 0.72f, h * 0.04f, w / 2f, h * 0.28f)
            close()
        }

        drawPath(path = heartPath, color = Color.White)
        drawPath(path = heartPath, color = Color(0xFFF3E8FF).copy(alpha = 0.65f))

        clipPath(heartPath) {
            val fillHeight = h * fillPercentage.coerceIn(0.06f, 0.96f)
            val fillTop = (h * 0.98f) - fillHeight
            val amplitude = 7.dp.toPx()
            val wavelength = w * 0.85f

            val backWave = Path().apply {
                val startY = fillTop + amplitude * sin(wavePhase1)
                moveTo(0f, startY)
                var x = 0f
                while (x <= w) {
                    val y = fillTop + amplitude * sin((2 * Math.PI * (x / wavelength) + wavePhase1).toFloat())
                    lineTo(x, y)
                    x += 3f
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = backWave,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.65f), Color(0xFF6D28D9)),
                    startY = fillTop - 10f,
                    endY = h
                )
            )

            val frontSurface = Path()
            val frontWave = Path().apply {
                val startY = fillTop + (amplitude * 0.85f) * sin(-wavePhase2)
                moveTo(0f, startY)
                frontSurface.moveTo(0f, startY)
                var x = 0f
                while (x <= w) {
                    val y = fillTop + (amplitude * 0.85f) * sin((2 * Math.PI * (x / (wavelength * 0.92f)) - wavePhase2).toFloat())
                    lineTo(x, y)
                    frontSurface.lineTo(x, y)
                    x += 3f
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = frontWave,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFA78BFA), Color(0xFF4C1D95)),
                    startY = fillTop - 10f,
                    endY = h
                )
            )

            drawPath(
                path = frontSurface,
                color = Color.White.copy(alpha = 0.45f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawPath(
            path = heartPath,
            color = Color(0xFFDDD6FE),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// =========================================================
// 4. MULTI-YEAR ASSET FLOW PROGRESSION CARD
// =========================================================

@Composable
private fun MultiYearAssetFlowCard(
    multiYearAssets: List<MultiYearAssetMetric>,
    selectedYear: Int,
    currencySymbol: String,
    isDiscreet: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Multi-Year Asset Flow",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Text(
                text = "Capital deployed to wealth-building assets across years",
                fontSize = 11.5.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(18.dp))

            MultiYearAssetCanvas(
                multiYearAssets = multiYearAssets,
                selectedYear = selectedYear,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                multiYearAssets.forEach { item ->
                    val isCurrent = item.year == selectedYear
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) AccentPurple.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Year ${item.year}",
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isCurrent) AccentPurple else TextDark
                            )
                            if (item.growthPercent != 0.0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.growthPercent >= 0) SoftGreen.copy(alpha = 0.12f) else SoftRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (item.growthPercent >= 0) "+${item.growthPercent.toInt()}%" else "${item.growthPercent.toInt()}%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.growthPercent >= 0) SoftGreen else SoftRed,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", item.totalAssets)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = if (isCurrent) AccentPurple else TextDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiYearAssetCanvas(
    multiYearAssets: List<MultiYearAssetMetric>,
    selectedYear: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = multiYearAssets.size.coerceAtLeast(1)
        val maxVal = multiYearAssets.maxOfOrNull { it.totalAssets }?.coerceAtLeast(100.0) ?: 100.0

        val barWidth = 28.dp.toPx()
        val spacing = (w - (barWidth * count)) / (count + 1).coerceAtLeast(1)

        multiYearAssets.forEachIndexed { idx, item ->
            val x = spacing + idx * (barWidth + spacing)
            val ratio = (item.totalAssets / maxVal).toFloat().coerceIn(0.04f, 0.92f)
            val barH = (h * 0.75f) * ratio
            val isCurrent = item.year == selectedYear

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = if (isCurrent) {
                        listOf(AccentPurple, Color(0xFF6366F1))
                    } else {
                        listOf(BorderLight, BorderLight.copy(alpha = 0.5f))
                    }
                ),
                topLeft = Offset(x, h - barH - 20.dp.toPx()),
                size = Size(barWidth, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }

        drawLine(
            color = Color(0xFFE5E7EB),
            start = Offset(0f, h - 18.dp.toPx()),
            end = Offset(w, h - 18.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// =========================================================
// AUXILIARY COMPONENTS & SPIDER WEBS
// =========================================================

@Composable
private fun QuickMetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    tint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = tint)
        }
    }
}

@Composable
private fun CategoryTrajectoryRowCard(
    item: CategoryAnnualTrajectory,
    currencySymbol: String,
    isDiscreet: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        border = BorderStroke(0.6.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(item.categoryName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                    Text("${String.format(Locale.US, "%.1f", item.percentageOfTotal)}% of annual outflow", fontSize = 10.5.sp, color = TextMuted)
                }

                Text(
                    text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", item.annualTotal)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = AccentPurple
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val maxMonth = item.monthlyAmounts.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val pts = item.monthlyAmounts.mapIndexed { idx, amt ->
                    val x = (idx.toFloat() / 11f) * size.width
                    val y = size.height * (1f - (amt / maxMonth).toFloat().coerceIn(0.1f, 0.9f))
                    Offset(x, y)
                }

                val path = Path()
                pts.forEachIndexed { idx, pt ->
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(path, color = AccentPurple, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))

                val peakPt = pts[item.peakMonthIndex]
                drawCircle(color = SoftRed, radius = 3.dp.toPx(), center = peakPt)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Jan", fontSize = 9.sp, color = TextMuted)
                Text("Peak: ${MONTH_NAMES[item.peakMonthIndex]}", fontSize = 9.5.sp, color = SoftRed, fontWeight = FontWeight.Bold)
                Text("Dec", fontSize = 9.sp, color = TextMuted)
            }
        }
    }
}

@Composable
private fun AnnualRadarSpiderCanvas(
    categorySums: List<Double>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val numAxes = 6
        val c = center
        val maxR = size.minDimension * 0.44f

        for (ring in 1..3) {
            val r = maxR * (ring / 3f)
            val ringPath = Path()
            for (i in 0 until numAxes) {
                val angle = (i * 2 * Math.PI / numAxes) - Math.PI / 2
                val x = c.x + (r * cos(angle)).toFloat()
                val y = c.y + (r * sin(angle)).toFloat()
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            drawPath(ringPath, color = BorderLight.copy(alpha = 0.5f), style = Stroke(width = 1.dp.toPx()))
        }

        for (i in 0 until numAxes) {
            val angle = (i * 2 * Math.PI / numAxes) - Math.PI / 2
            val x = c.x + (maxR * cos(angle)).toFloat()
            val y = c.y + (maxR * sin(angle)).toFloat()
            drawLine(color = BorderLight.copy(alpha = 0.6f), start = c, end = Offset(x, y), strokeWidth = 1.dp.toPx())
        }

        val hasData = categorySums.any { it > 0.0 }
        if (hasData) {
            val maxAmount = categorySums.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
            val polyPath = Path()
            for (i in 0 until numAxes) {
                val amt = categorySums.getOrNull(i) ?: 0.0
                val ratio = (amt / maxAmount).toFloat().coerceIn(0.12f, 0.95f)
                val r = maxR * ratio
                val angle = (i * 2 * Math.PI / numAxes) - Math.PI / 2
                val x = c.x + (r * cos(angle)).toFloat()
                val y = c.y + (r * sin(angle)).toFloat()
                if (i == 0) polyPath.moveTo(x, y) else polyPath.lineTo(x, y)
            }
            polyPath.close()

            drawPath(polyPath, color = AccentPurple.copy(alpha = 0.22f))
            drawPath(polyPath, color = AccentPurple, style = Stroke(width = 2.dp.toPx()))
        }
    }
}

@Composable
private fun MonthGridTimelineCard(
    data: YearlyMonthData,
    currencySymbol: String,
    isDiscreet: Boolean,
    onTapMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSurplus = data.netSavings >= 0
    val hasActivity = data.income > 0 || data.expenses > 0 || data.assets > 0
    val statusColor = if (data.isFuture) TextMuted else if (isSurplus) SoftGreen else SoftRed

    val absSavings = abs(data.netSavings)
    val formattedSavings = if (absSavings >= 1000) "${(absSavings / 1000).toInt()}k" else "${absSavings.toInt()}"
    val badgeText = when {
        data.isFuture -> "Planned"
        !hasActivity -> "No Data"
        isSurplus -> "+$currencySymbol$formattedSavings"
        else -> "-$currencySymbol$formattedSavings"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onTapMonth),
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = data.monthName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isDiscreet) "••••" else badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", data.expenses)}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = if (data.isFuture || !hasActivity) TextMuted else TextDark
            )
            Text(text = "Outflow Burn", fontSize = 10.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(8.dp))

            val total = (data.income + data.expenses + data.assets).coerceAtLeast(1.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BorderLight.copy(alpha = 0.4f))
            ) {
                if (hasActivity) {
                    Box(modifier = Modifier.weight((data.income / total).toFloat().coerceIn(0.05f, 0.9f)).fillMaxHeight().background(SoftGreen))
                    Box(modifier = Modifier.weight((data.expenses / total).toFloat().coerceIn(0.05f, 0.9f)).fillMaxHeight().background(AccentPurple))
                    Box(modifier = Modifier.weight((data.assets / total).toFloat().coerceIn(0.05f, 0.9f)).fillMaxHeight().background(SoftTeal))
                }
            }
        }
    }
}
