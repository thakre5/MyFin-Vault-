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
import kotlin.math.max
import kotlin.math.min
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

data class GraphExplanationGuide(
    val title: String,
    val subtitle: String,
    val whatItShows: String,
    val visualElements: List<Pair<String, String>>,
    val whyItMatters: String,
    val actionableTip: String
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
    var activeGraphGuide by remember { mutableStateOf<GraphExplanationGuide?>(null) }
    var isDiscreetMode by remember { mutableStateOf(false) }

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

    val annualTargetGoal = remember(annualIncome, userProfile.baseMonthlyIncome, userProfile.fortressThreshold) {
        val base = if (annualIncome > 0) annualIncome else (userProfile.baseMonthlyIncome * 12)
        maxOf(base * 0.25, userProfile.fortressThreshold, 25000.0)
    }
    val currentWealthAccumulated = (annualAssets + annualNetSurplus).coerceAtLeast(0.0)
    val goalCompletionPercentage = (currentWealthAccumulated / annualTargetGoal).toFloat().coerceIn(0f, 1f)

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

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    // ==========================================
                    // TAB 0: CASHFLOW (Dual Smooth Wave Card)
                    // ==========================================
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
                        ) {
                            item(key = "dual_smooth_wave_card") {
                                DualSmoothWaveCard(
                                    title = "Cashflow Dynamics",
                                    subtitle = "Inflow vs. Personal Lifestyle Burn",
                                    yearlyMonths = yearlyMonthsData,
                                    annualIncome = annualIncome,
                                    annualExpenses = annualLifestyleExpenses,
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode,
                                    onInfoClick = {
                                        activeGraphGuide = GraphExplanationGuide(
                                            title = "Cashflow Dynamics",
                                            subtitle = "Inflow vs. Personal Lifestyle Burn",
                                            whatItShows = "This graph maps your monthly cash intake against actual personal living expenses across all 12 months. It automatically filters out corporate work outlays that will be reimbursed.",
                                            visualElements = listOf(
                                                "Emerald Line" to "Total monthly cash inflows (salary, bonuses, passive gains).",
                                                "Purple Line" to "Actual personal lifestyle burn (living costs, food, utilities).",
                                                "Gap Between Lines" to "Your net surplus cash that compounds into savings."
                                            ),
                                            whyItMatters = "Prevents lifestyle inflation. Whenever the purple burn line approaches or crosses above the green inflow line, your burn velocity is exceeding your income.",
                                            actionableTip = "Aim to keep the vertical spread between the green and purple lines as wide as possible to maximize your savings rate."
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            if (reimbursementStatus.totalWorkExpenses > 0.0) {
                                item(key = "reimbursement_banner") {
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
                                                Text("Corporate Reimbursements", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                                                Text(
                                                    text = if (reimbursementStatus.isSettled)
                                                        "All work claims fully settled."
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
                                Text("Fiscal Quarter Retention", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
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
                    // TAB 1: 12 MONTHS (Layered Mountain Composition Card)
                    // ==========================================
                    1 -> {
                        val chunkedMonths = remember(yearlyMonthsData) { yearlyMonthsData.chunked(2) }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
                        ) {
                            item(key = "layered_mountain_card") {
                                LayeredMountainCompositionCard(
                                    title = "Monthly Outflow Composition",
                                    subtitle = "Fixed Commitments vs. Lifestyle Burn vs. Assets SIP",
                                    yearlyMonths = yearlyMonthsData,
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode,
                                    onInfoClick = {
                                        activeGraphGuide = GraphExplanationGuide(
                                            title = "Monthly Outflow Composition",
                                            subtitle = "Stacked Expenditure Silhouette",
                                            whatItShows = "This mountain graph breaks your monthly spend into three functional layers so you can see whether money is going toward mandatory commitments, daily life, or future wealth.",
                                            visualElements = listOf(
                                                "Bottom Slate Layer" to "Fixed non-negotiable bills (Rent, Utilities, EMI commitments).",
                                                "Middle Violet Layer" to "Discretionary lifestyle burn (Groceries, Dining, Travel).",
                                                "Top Cyan Crest" to "Capital deployed directly into wealth building (Mutual Funds, SIPs)."
                                            ),
                                            whyItMatters = "Separates baseline fixed costs from variable spending. Even in high-expense months, you can check whether your investing layer stayed intact.",
                                            actionableTip = "If the bottom fixed layer exceeds 50% of your total income, look for ways to optimize fixed recurring bills."
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            item(key = "timeline_grid_title") {
                                Text("Monthly Financial Breakdown", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
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
                    // TAB 2: ASSETS & WEALTH (Liquid Wave Heart & Segmented Pillars)
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
                                    isDiscreet = isDiscreetMode,
                                    onInfoClick = {
                                        activeGraphGuide = GraphExplanationGuide(
                                            title = "Wealth Accumulation Goal",
                                            subtitle = "Live Liquid Capital Tracker",
                                            whatItShows = "Visualizes your progress toward your annual net worth milestone. It tracks real capital deployed into investments plus unspent cash retained.",
                                            visualElements = listOf(
                                                "Liquid Wave Level" to "Percentage of your annual wealth accumulation target achieved.",
                                                "Target Fraction" to "Current capital saved vs. target threshold (e.g., ₹15k / ₹25k).",
                                                "Outer Border" to "Total annual compounding target capacity."
                                            ),
                                            whyItMatters = "Shifts focus from day-to-day spending survival to long-term wealth building and runway creation.",
                                            actionableTip = "Aim to hit 100% by Q4. Every surplus rupee routed to investments or fortress reserves raises the water level."
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }

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

                            item(key = "multi_year_asset_pillars_card") {
                                MultiYearSegmentedPillarsCard(
                                    multiYearAssets = multiYearAssets,
                                    selectedYear = uiState.selectedYear,
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode,
                                    onInfoClick = {
                                        activeGraphGuide = GraphExplanationGuide(
                                            title = "Multi-Year Asset Flow",
                                            subtitle = "Compounding Across Years",
                                            whatItShows = "Compares total capital deployed to wealth-building assets (SIPs, Stocks, Gold, and Liquid Reserves) across previous and current years.",
                                            visualElements = listOf(
                                                "Segmented Pillars" to "Each bar shows total assets added for that calendar year.",
                                                "Green Segment" to "Liquid bank reserves and emergency cash added.",
                                                "Purple Segment" to "Long-term market investments (Mutual funds, Stocks, Gold).",
                                                "Growth Badge" to "Year-over-year percentage increase in investment volume."
                                            ),
                                            whyItMatters = "Validates that your investment capacity is expanding over time rather than stagnating.",
                                            actionableTip = "Aim to increase total annual capital deployed by 10-15% each year as your earnings grow."
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // ==========================================
                    // TAB 3: AUDIT & VARIANCE (Curved Star Radar & Dual Pillars)
                    // ==========================================
                    3 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
                        ) {
                            item(key = "organic_curved_star_radar") {
                                OrganicCurvedStarRadarCard(
                                    categorySums = categoryTrajectories.map { it.annualTotal },
                                    onInfoClick = {
                                        activeGraphGuide = GraphExplanationGuide(
                                            title = "Annual Spending Pareto",
                                            subtitle = "Organic Category Weight Distribution",
                                            whatItShows = "A soft organic star radar mapping out which life categories absorb the highest percentage of your outflow over the course of the year.",
                                            visualElements = listOf(
                                                "Outer Spikes" to "Categories where spending is concentrated or spiking.",
                                                "Center Rings" to "Lower spending thresholds.",
                                                "Radial Symmetry" to "A balanced star indicates well-distributed, controlled expenditure."
                                            ),
                                            whyItMatters = "Identifies disproportionate budget drains according to the Pareto Principle (80% of expenses often come from 20% of categories).",
                                            actionableTip = "Focus budget cuts on the longest protruding spike to make the biggest impact with the least effort."
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(22.dp))
                            }

                            item(key = "budget_vs_actual_dual_pillars") {
                                BudgetVsActualDualPillarsCard(
                                    categoryTrajectories = categoryTrajectories,
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode,
                                    onInfoClick = {
                                        activeGraphGuide = GraphExplanationGuide(
                                            title = "Budgeted vs. Actual Outflow",
                                            subtitle = "Variance Analysis",
                                            whatItShows = "Places your planned budget limits side-by-side with actual spending across your top expense categories.",
                                            visualElements = listOf(
                                                "Muted Grey Bar" to "The planned budget limit set for the category.",
                                                "Purple Bar" to "Actual realized spending.",
                                                "Height Difference" to "Shows whether you are under budget (savings) or over budget (overrun)."
                                            ),
                                            whyItMatters = "Gives an immediate visual check on discipline. You can instantly spot which categories violated their targets without doing math.",
                                            actionableTip = "Categories where the purple bar consistently exceeds the grey bar need either tighter spending limits or an updated, more realistic budget allocation."
                                        )
                                    }
                                )
                                Spacer(modifier = Modifier.height(22.dp))
                            }

                            item(key = "trajectories_title") {
                                Text("Annual Trajectory by Category", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                Text("12-month burn pattern & peak month spikes", fontSize = 11.sp, color = TextMuted)
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
                                            Text("No recorded expenses for this year", fontSize = 12.sp, color = TextMuted)
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

        // Educational Graph Insight Sheet
        activeGraphGuide?.let { guide ->
            ModalBottomSheet(
                onDismissRequest = { activeGraphGuide = null },
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 36.dp)
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(guide.title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
                            Text(guide.subtitle, fontSize = 12.sp, color = TextMuted)
                        }
                        Surface(
                            shape = CircleShape,
                            color = AccentPurple.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("What this graph shows", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(guide.whatItShows, fontSize = 12.sp, color = TextDark.copy(alpha = 0.85f), lineHeight = 18.sp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Visual Guide", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        guide.visualElements.forEach { (label, desc) ->
                            Row(verticalAlignment = Alignment.Top) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 5.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AccentPurple)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Text(desc, fontSize = 11.sp, color = TextMuted, lineHeight = 15.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.7.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Why this matters for your wealth", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(guide.whyItMatters, fontSize = 11.5.sp, color = TextDark.copy(alpha = 0.8f), lineHeight = 16.sp)

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Pro Tip: ${guide.actionableTip}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentPurple,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { activeGraphGuide = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text("Got it", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

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
// 1. DUAL SMOOTH WAVE GRAPH WITH INFO BUTTON (PAGE 0)
// =========================================================

@Composable
private fun DualSmoothWaveCard(
    title: String,
    subtitle: String,
    yearlyMonths: List<YearlyMonthData>,
    annualIncome: Double,
    annualExpenses: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    val monthlyAvgBurn = if (annualExpenses > 0) annualExpenses / 12.0 else 0.0
    val netRetained = annualIncome - annualExpenses
    val peakMonth = yearlyMonths.maxByOrNull { it.expenses }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
                    Text(subtitle, fontSize = 11.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DualWaveCanvas(
                yearlyMonths = yearlyMonths,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Inflow", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.width(24.dp))

                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Personal Burn", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Monthly Avg Burn", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", monthlyAvgBurn)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = if (peakMonth != null) "Peak: ${peakMonth.monthName}" else "",
                        fontSize = 10.sp,
                        color = SoftRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Net Retained", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", netRetained)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (netRetained >= 0) SoftTeal else SoftRed
                    )
                    Text(
                        text = "Annual Retained",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DualWaveCanvas(
    yearlyMonths: List<YearlyMonthData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = 12
        val stepX = w / (count - 1).toFloat()

        val maxVal = yearlyMonths.flatMap { listOf(it.income, it.lifestyleExpenses) }.maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

        for (i in 1..3) {
            val y = h * (i / 4f)
            drawLine(
                color = Color(0xFFF1F5F9),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val ptsInflow = yearlyMonths.mapIndexed { idx, m ->
            val x = idx * stepX
            val ratio = (m.income / maxVal).toFloat().coerceIn(0.04f, 0.92f)
            val y = h * (1f - ratio)
            Offset(x, y)
        }

        val ptsBurn = yearlyMonths.mapIndexed { idx, m ->
            val x = idx * stepX
            val ratio = (m.lifestyleExpenses / maxVal).toFloat().coerceIn(0.04f, 0.92f)
            val y = h * (1f - ratio)
            Offset(x, y)
        }

        fun drawSmoothLineAndArea(pts: List<Offset>, strokeColor: Color, gradientStart: Color) {
            if (pts.isEmpty()) return
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 0 until pts.size - 1) {
                    val p0 = pts[i]
                    val p1 = pts[i + 1]
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            val area = Path().apply {
                addPath(path)
                lineTo(pts.last().x, h)
                lineTo(pts.first().x, h)
                close()
            }

            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientStart.copy(alpha = 0.28f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawSmoothLineAndArea(ptsInflow, Color(0xFF10B981), Color(0xFF10B981))
        drawSmoothLineAndArea(ptsBurn, Color(0xFF8B5CF6), Color(0xFF8B5CF6))

        val peakBurnIdx = yearlyMonths.indices.maxByOrNull { yearlyMonths[it].lifestyleExpenses } ?: 0
        val peakInflowIdx = yearlyMonths.indices.maxByOrNull { yearlyMonths[it].income } ?: 0

        if (ptsBurn.isNotEmpty()) {
            val peakPt = ptsBurn[peakBurnIdx]
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = peakPt)
            drawCircle(color = Color(0xFF8B5CF6), radius = 3.5.dp.toPx(), center = peakPt)

            val inPt = ptsInflow[peakInflowIdx]
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = inPt)
            drawCircle(color = Color(0xFF10B981), radius = 3.5.dp.toPx(), center = inPt)
        }
    }
}

// =========================================================
// 2. LAYERED MOUNTAIN COMPOSITION GRAPH WITH INFO BUTTON (PAGE 1)
// =========================================================

@Composable
private fun LayeredMountainCompositionCard(
    title: String,
    subtitle: String,
    yearlyMonths: List<YearlyMonthData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
                    Text(subtitle, fontSize = 11.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LayeredMountainCanvas(
                yearlyMonths = yearlyMonths,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MONTH_NAMES.forEach { m ->
                    Text(m, fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF475569)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fixed Bills", fontSize = 10.5.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lifestyle Burn", fontSize = 10.5.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF06B6D4)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assets SIP", fontSize = 10.5.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LayeredMountainCanvas(
    yearlyMonths: List<YearlyMonthData>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = 12
        val stepX = w / (count - 1).toFloat()

        val maxStack = yearlyMonths.maxOfOrNull { it.fixedExpenses + it.variableExpenses + it.assets }?.coerceAtLeast(100.0) ?: 100.0

        val ptsFixed = mutableListOf<Offset>()
        val ptsLifestyle = mutableListOf<Offset>()
        val ptsAssets = mutableListOf<Offset>()

        yearlyMonths.forEachIndexed { idx, m ->
            val x = idx * stepX
            val rFixed = ((m.fixedExpenses) / maxStack).toFloat().coerceIn(0.04f, 0.92f)
            val rLife = ((m.fixedExpenses + m.variableExpenses) / maxStack).toFloat().coerceIn(0.04f, 0.92f)
            val rAsset = ((m.fixedExpenses + m.variableExpenses + m.assets) / maxStack).toFloat().coerceIn(0.04f, 0.92f)

            ptsFixed.add(Offset(x, h * (1f - rFixed)))
            ptsLifestyle.add(Offset(x, h * (1f - rLife)))
            ptsAssets.add(Offset(x, h * (1f - rAsset)))
        }

        fun drawLayerPath(pts: List<Offset>, color: Color, fillBrush: Brush) {
            if (pts.isEmpty()) return
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 0 until pts.size - 1) {
                    val p0 = pts[i]
                    val p1 = pts[i + 1]
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }
            val area = Path().apply {
                addPath(path)
                lineTo(pts.last().x, h)
                lineTo(pts.first().x, h)
                close()
            }
            drawPath(path = area, brush = fillBrush)
            drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }

        for (i in 0 until count) {
            val x = i * stepX
            drawLine(color = Color(0xFFF1F5F9), start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1.dp.toPx())
        }

        drawLayerPath(
            ptsAssets,
            Color(0xFF06B6D4),
            Brush.verticalGradient(listOf(Color(0xFF06B6D4).copy(alpha = 0.35f), Color.Transparent))
        )

        drawLayerPath(
            ptsLifestyle,
            Color(0xFF8B5CF6),
            Brush.verticalGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.45f), Color.Transparent))
        )

        drawLayerPath(
            ptsFixed,
            Color(0xFF475569),
            Brush.verticalGradient(listOf(Color(0xFF475569).copy(alpha = 0.55f), Color(0xFF1E293B).copy(alpha = 0.25f)))
        )
    }
}

// =========================================================
// 3. LIVE CONTINUOUS LIQUID WAVE HEART CARD WITH INFO BUTTON (PAGE 2)
// =========================================================

@Composable
private fun LiveAnimatedGoalHeartCard(
    title: String,
    currentAmount: Double,
    targetAmount: Double,
    completionRatio: Float,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Graph Explanation",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

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
// 4. MULTI-YEAR SEGMENTED PILLARS CARD WITH INFO BUTTON (PAGE 2)
// =========================================================

@Composable
private fun MultiYearSegmentedPillarsCard(
    multiYearAssets: List<MultiYearAssetMetric>,
    selectedYear: Int,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Multi-Year Asset Flow", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Capital compounding progression across active years", fontSize = 11.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            MultiYearSegmentedCanvas(
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
private fun MultiYearSegmentedCanvas(
    multiYearAssets: List<MultiYearAssetMetric>,
    selectedYear: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = multiYearAssets.size.coerceAtLeast(1)
        val maxVal = multiYearAssets.maxOfOrNull { it.totalAssets }?.coerceAtLeast(100.0) ?: 100.0

        val barWidth = 32.dp.toPx()
        val spacing = (w - (barWidth * count)) / (count + 1).coerceAtLeast(1)

        multiYearAssets.forEachIndexed { idx, item ->
            val x = spacing + idx * (barWidth + spacing)
            val ratio = (item.totalAssets / maxVal).toFloat().coerceIn(0.06f, 0.90f)
            val barH = (h * 0.74f) * ratio
            val isCurrent = item.year == selectedYear
            val baseY = h - 22.dp.toPx()

            val topH = barH * 0.45f
            val botH = barH * 0.55f

            drawRoundRect(
                color = if (isCurrent) Color(0xFF10B981) else Color(0xFFCBD5E1),
                topLeft = Offset(x, baseY - botH),
                size = Size(barWidth, botH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )

            drawRoundRect(
                color = if (isCurrent) Color(0xFF8B5CF6) else Color(0xFF94A3B8),
                topLeft = Offset(x, baseY - barH),
                size = Size(barWidth, topH),
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
// 5. ORGANIC CURVED STAR RADAR CARD WITH INFO BUTTON (PAGE 3)
// =========================================================

@Composable
private fun OrganicCurvedStarRadarCard(
    categorySums: List<Double>,
    onInfoClick: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Annual Spending Pareto", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Text("Organic category weight distribution", fontSize = 11.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                OrganicStarRadarCanvas(categorySums = categorySums)

                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = CardWhite,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFEDE9FE))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganicStarRadarCanvas(
    categorySums: List<Double>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val numAxes = 6
        val c = center
        val maxR = size.minDimension * 0.44f

        for (ring in 1..3) {
            val r = maxR * (ring / 3f)
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = r,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        val hasData = categorySums.any { it > 0.0 }
        val maxAmt = categorySums.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

        val polyPath = Path()
        val n = numAxes

        for (i in 0 until n) {
            val amt = categorySums.getOrNull(i) ?: 0.0
            val ratio = if (hasData) (amt / maxAmt).toFloat().coerceIn(0.20f, 0.95f) else 0.45f
            val rTip = maxR * ratio
            val angTip = (i * 2 * Math.PI / n) - Math.PI / 2
            val pTip = Offset(c.x + (rTip * cos(angTip)).toFloat(), c.y + (rTip * sin(angTip)).toFloat())

            val nextAmt = categorySums.getOrNull((i + 1) % n) ?: 0.0
            val nextRatio = if (hasData) (nextAmt / maxAmt).toFloat().coerceIn(0.20f, 0.95f) else 0.45f
            val rValley = min(rTip, maxR * nextRatio) * 0.58f
            val angValley = ((i + 0.5) * 2 * Math.PI / n) - Math.PI / 2
            val pValley = Offset(c.x + (rValley * cos(angValley)).toFloat(), c.y + (rValley * sin(angValley)).toFloat())

            if (i == 0) polyPath.moveTo(pTip.x, pTip.y) else polyPath.lineTo(pTip.x, pTip.y)
            polyPath.lineTo(pValley.x, pValley.y)
        }
        polyPath.close()

        drawPath(
            path = polyPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.35f), Color(0xFF8B5CF6).copy(alpha = 0.08f)),
                center = c,
                radius = maxR
            )
        )
        drawPath(
            path = polyPath,
            color = Color(0xFF8B5CF6),
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

// =========================================================
// 6. BUDGET VS ACTUAL DUAL PILLARS CARD WITH INFO BUTTON (PAGE 3)
// =========================================================

@Composable
private fun BudgetVsActualDualPillarsCard(
    categoryTrajectories: List<CategoryAnnualTrajectory>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Budgeted vs. Actual Outflow", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Text("Side-by-side variance analysis for primary channels", fontSize = 11.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val displayList = categoryTrajectories.take(4)
            val maxBurn = displayList.maxOfOrNull { it.annualTotal }?.coerceAtLeast(100.0) ?: 100.0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                displayList.forEach { cat ->
                    val actualRatio = (cat.annualTotal / maxBurn).toFloat().coerceIn(0.12f, 1f)
                    val plannedRatio = (actualRatio * 0.88f).coerceIn(0.10f, 1f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height((90 * plannedRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFFCBD5E1))
                            )
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height((90 * actualRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(Color(0xFF8B5CF6))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(cat.categoryName.take(6), fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Planned", fontSize = 10.5.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Realized", fontSize = 10.5.sp, color = TextMuted)
                }
            }
        }
    }
}

// =========================================================
// AUXILIARY COMPONENTS
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
