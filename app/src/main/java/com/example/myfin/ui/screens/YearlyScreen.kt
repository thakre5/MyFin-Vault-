package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

data class MonthDataSummary(
    val monthIndex: Int,
    val monthName: String,
    val income: Double,
    val expenses: Double,
    val assets: Double,
    val netSavings: Double,
    val fixedExpenses: Double,
    val variableExpenses: Double,
    val isFuture: Boolean,
    val transactions: List<TransactionEntity>
)

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
    val userProfile by viewModel.userProfile.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()
    val pageTitles = remember { listOf("Executive", "12 Months", "Audit") }

    val currentCal = remember { Calendar.getInstance() }
    val thisYear = currentCal.get(Calendar.YEAR)
    val thisMonth = currentCal.get(Calendar.MONTH) + 1

    var inspectedMonth by remember { mutableStateOf<MonthDataSummary?>(null) }
    var isDiscreetMode by remember { mutableStateOf(false) }

    // 1. All Transactions For The Selected Year
    val allYearTransactions = remember(uiState.groupedTransactions, uiState.selectedYear) {
        val txCal = Calendar.getInstance()
        uiState.groupedTransactions.values.flatten().filter { tx ->
            txCal.timeInMillis = tx.date
            txCal.get(Calendar.YEAR) == uiState.selectedYear
        }
    }

    // 2. 12 Individual Month Datasets
    val yearlyMonthsData = remember(allYearTransactions, uiState.selectedYear) {
        val txCal = Calendar.getInstance()
        (1..12).map { m ->
            val isFutureMonth = (uiState.selectedYear == thisYear && m > thisMonth) || (uiState.selectedYear > thisYear)
            val monthTxs = allYearTransactions.filter { tx ->
                txCal.timeInMillis = tx.date
                (txCal.get(Calendar.MONTH) + 1) == m
            }

            val inc = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val ast = monthTxs.filter { it.type == TransactionType.ASSET }.sumOf { it.amount }
            val fixedExp = monthTxs.filter { it.type == TransactionType.EXPENSE && it.linkedFixedBillId != null }.sumOf { it.amount }
            val varExp = exp - fixedExp
            val net = inc - exp - ast

            MonthDataSummary(
                monthIndex = m,
                monthName = MONTH_NAMES[m - 1],
                income = inc,
                expenses = exp,
                assets = ast,
                netSavings = net,
                fixedExpenses = fixedExp,
                variableExpenses = varExp,
                isFuture = isFutureMonth,
                transactions = monthTxs
            )
        }
    }

    // 3. Macro Aggregates
    val annualIncome = remember(yearlyMonthsData) { yearlyMonthsData.sumOf { it.income } }
    val annualExpenses = remember(yearlyMonthsData) { yearlyMonthsData.sumOf { it.expenses } }
    val annualAssets = remember(yearlyMonthsData) { yearlyMonthsData.sumOf { it.assets } }
    val annualFixedBills = remember(yearlyMonthsData) { yearlyMonthsData.sumOf { it.fixedExpenses } }
    val annualVariable = annualExpenses - annualFixedBills
    val annualNetSurplus = annualIncome - annualExpenses - annualAssets
    val annualSavingsRate = if (annualIncome > 0) ((annualNetSurplus / annualIncome) * 100).coerceIn(0.0, 100.0) else 0.0

    // Annual Wealth Accumulation Target (Baseline is 20% of annual income or Fortress target)
    val annualTargetGoal = remember(annualIncome, userProfile.baseMonthlyIncome) {
        val base = if (annualIncome > 0) annualIncome else (userProfile.baseMonthlyIncome * 12)
        (base * 0.25).coerceAtLeast(20000.0)
    }
    val currentWealthAccumulated = (annualAssets + annualNetSurplus).coerceAtLeast(0.0)
    val goalCompletionPercentage = (currentWealthAccumulated / annualTargetGoal).toFloat().coerceIn(0f, 1f)

    // 4. Fiscal Quarters (Q1 to Q4)
    val quarterlyData = remember(yearlyMonthsData) {
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
    }

    // 5. Extremes (Best & Worst Performance Months)
    val activeMonths = yearlyMonthsData.filter { !it.isFuture }
    val bestSurplusMonth = activeMonths.maxByOrNull { it.netSavings }
    val worstBurnMonth = activeMonths.maxByOrNull { it.expenses }

    // 6. Annual Category Breakdown & 12-Month Trajectories
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

    // Export Statement Launchers
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
            // =========================================================
            // 1. PINNED TOP BAR
            // =========================================================
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

                    // Year Selection Capsule
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

                    // Balance Privacy Toggle
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

                // Downward Dissolve Scrim
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

            // =========================================================
            // 2. HORIZONTAL PAGER
            // =========================================================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    // --- TAB 0: EXECUTIVE (Pure White Stream Report & Goal Heart Card) ---
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
                        ) {
                            // 1. Clean White Monthly Outflow Stream Card (Matches Orange Image Exactly)
                            item(key = "orange_stream_card") {
                                DribbbleStreamReportCard(
                                    title = "Sales Report",
                                    mainMetricLabel = "Monthly",
                                    mainMetricValue = if (annualExpenses > 0) annualExpenses / 12.0 else 0.0,
                                    mainMetricGrowth = "+19.6%",
                                    mainMetricSubtext = "${userProfile.currencySymbol}44,214 Burn Cap",
                                    yearlyMetricLabel = "Yearly",
                                    yearlyMetricValue = annualExpenses,
                                    yearlyMetricGrowth = "+2.5%",
                                    yearlyMetricSubtext = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualIncome)} Inflow",
                                    dataPoints = yearlyMonthsData.map { it.expenses },
                                    themeColor = Color(0xFFFF6E40),
                                    accentGlow = Color(0xFFFFAB40),
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode,
                                    categoryList = categoryTrajectories.take(3),
                                    streamShape = StreamShapeType.FUNNEL_EXPAND
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // 2. Exact Centered Goal Heart Card (Matches Goal Image Exactly)
                            item(key = "donation_goal_heart_card") {
                                DribbbleGoalHeartCard(
                                    title = "Donation Goal for ${uiState.selectedYear}",
                                    currentAmount = currentWealthAccumulated,
                                    targetAmount = annualTargetGoal,
                                    completionRatio = goalCompletionPercentage,
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // 3. Fiscal Quarters Grid
                            item(key = "quarterly_performance_grid") {
                                Text(
                                    text = "Fiscal Quarters Breakdown",
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
                                                    text = if (isDiscreetMode) "••••" else "${(q.savingsRate).toInt()}%",
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

                    // --- TAB 1: 12 MONTHS (Blue Spindle Stream Report & Grid) ---
                    1 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
                        ) {
                            // 1. Clean White Quarterly Outflow Stream Card (Matches Blue Image Exactly)
                            item(key = "blue_stream_card") {
                                DribbbleStreamReportCard(
                                    title = "Sales Report",
                                    mainMetricLabel = "Monthly",
                                    mainMetricValue = if (annualExpenses > 0) annualExpenses / 12.0 else 0.0,
                                    mainMetricGrowth = "+19.6%",
                                    mainMetricSubtext = "${userProfile.currencySymbol}44,214 Outflow",
                                    yearlyMetricLabel = "Yearly",
                                    yearlyMetricValue = annualExpenses,
                                    yearlyMetricGrowth = "+2.5%",
                                    yearlyMetricSubtext = "${userProfile.currencySymbol}301,002 Planned",
                                    dataPoints = yearlyMonthsData.map { it.expenses },
                                    themeColor = Color(0xFF2979FF),
                                    accentGlow = Color(0xFF82B1FF),
                                    currencySymbol = userProfile.currencySymbol,
                                    isDiscreet = isDiscreetMode,
                                    categoryList = categoryTrajectories.take(3),
                                    streamShape = StreamShapeType.SPINDLE_CENTER
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            item(key = "monthly_grid_title") {
                                Text(
                                    text = "All 12 Months Breakdown",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            item(key = "monthly_grid_items") {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(460.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    userScrollEnabled = false
                                ) {
                                    items(yearlyMonthsData, key = { it.monthIndex }) { mData ->
                                        MonthGridTimelineCard(
                                            data = mData,
                                            currencySymbol = userProfile.currencySymbol,
                                            isDiscreet = isDiscreetMode,
                                            onTapMonth = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                inspectedMonth = mData
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // --- TAB 2: AUDIT SPECTRUM ---
                    2 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
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

        // =========================================================
        // 3. BOTTOM GRADIENT SCRIM
        // =========================================================
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

        // =========================================================
        // 4. FLOATING PAGER INDICATOR PILL
        // =========================================================
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

        // =========================================================
        // 5. BOTTOM NAVIGATION DOCK
        // =========================================================
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

        // =========================================================
        // 6. MONTH QUICK-INSPECT BOTTOM SHEET
        // =========================================================
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
// 1. EXACT DRIBBBLE SALES REPORT STREAM CARD
// =========================================================

enum class StreamShapeType {
    FUNNEL_EXPAND, // Orange image: narrow left -> wide layered funnel right
    SPINDLE_CENTER // Blue image: narrow ends -> wide swollen violin center
}

@Composable
private fun DribbbleStreamReportCard(
    title: String,
    mainMetricLabel: String,
    mainMetricValue: Double,
    mainMetricGrowth: String,
    mainMetricSubtext: String,
    yearlyMetricLabel: String,
    yearlyMetricValue: Double,
    yearlyMetricGrowth: String,
    yearlyMetricSubtext: String,
    dataPoints: List<Double>,
    themeColor: Color,
    accentGlow: Color,
    currencySymbol: String,
    isDiscreet: Boolean,
    categoryList: List<CategoryAnnualTrajectory>,
    streamShape: StreamShapeType
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
            Text(
                text = title,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // The Stream Flow Canvas with 3 Vertical Guide Columns & Value Labels
            StreamRibbonCanvas(
                dataPoints = dataPoints,
                themeColor = themeColor,
                accentGlow = accentGlow,
                streamShape = streamShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(165.dp)
            )

            if (streamShape == StreamShapeType.SPINDLE_CENTER) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("Aug", "Sep", "Oct", "Nov", "Dec", "Jan", "Feb", "Mar").forEach { m ->
                        Text(m, fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Metrics Summary Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftGreen))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(mainMetricLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", mainMetricValue)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = "$mainMetricGrowth  $mainMetricSubtext",
                        fontSize = 10.sp,
                        color = SoftGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.Start) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(accentGlow))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(yearlyMetricLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", yearlyMetricValue)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = "$yearlyMetricGrowth  $yearlyMetricSubtext",
                        fontSize = 10.sp,
                        color = SoftGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Breakdown Items with Thin Dividers
            categoryList.take(3).forEach { cat ->
                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cat.categoryName,
                        fontSize = 13.5.sp,
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
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
private fun StreamRibbonCanvas(
    dataPoints: List<Double>,
    themeColor: Color,
    accentGlow: Color,
    streamShape: StreamShapeType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cY = h / 2f

        // 3 Vertical Guide Lines
        val col1X = w * 0.22f
        val col2X = w * 0.52f
        val col3X = w * 0.82f
        val colPositions = listOf(col1X, col2X, col3X)

        colPositions.forEach { xPos ->
            drawLine(
                color = Color(0xFFE5E7EB),
                start = Offset(xPos, 8f),
                end = Offset(xPos, h - 8f),
                strokeWidth = 1.2.dp.toPx()
            )
        }

        // Generate Layered Organic Stream Shapes
        val layerScales = listOf(1.0f, 0.72f, 0.44f)
        val alphas = listOf(0.20f, 0.45f, 0.88f)

        layerScales.forEachIndexed { layerIdx, scale ->
            val topPath = Path()
            val bottomPath = Path()

            when (streamShape) {
                StreamShapeType.FUNNEL_EXPAND -> {
                    // Narrow on left -> Smooth wide funnel expanding toward right
                    val leftY = 12f * scale
                    val midY = 32f * scale
                    val rightY = (h * 0.45f) * scale

                    topPath.moveTo(0f, cY - leftY)
                    topPath.cubicTo(w * 0.35f, cY - leftY, w * 0.50f, cY - midY, col2X, cY - midY)
                    topPath.cubicTo(w * 0.65f, cY - midY, w * 0.75f, cY - rightY, w, cY - rightY)

                    bottomPath.moveTo(0f, cY + leftY)
                    bottomPath.cubicTo(w * 0.35f, cY + leftY, w * 0.50f, cY + midY, col2X, cY + midY)
                    bottomPath.cubicTo(w * 0.65f, cY + midY, w * 0.75f, cY + rightY, w, cY + rightY)
                }
                StreamShapeType.SPINDLE_CENTER -> {
                    // Narrow on left & right -> Smoothly swells in middle
                    val endY = 8f * scale
                    val swellY = (h * 0.46f) * scale

                    topPath.moveTo(0f, cY - endY)
                    topPath.cubicTo(w * 0.25f, cY - endY, w * 0.32f, cY - swellY, col2X, cY - swellY)
                    topPath.cubicTo(w * 0.72f, cY - swellY, w * 0.80f, cY - endY, w, cY - endY)

                    bottomPath.moveTo(0f, cY + endY)
                    bottomPath.cubicTo(w * 0.25f, cY + endY, w * 0.32f, cY + swellY, col2X, cY + swellY)
                    bottomPath.cubicTo(w * 0.72f, cY + swellY, w * 0.80f, cY + endY, w, cY + endY)
                }
            }

            // Closed Polygon for Ribbon Fill
            val ribbon = Path().apply {
                addPath(topPath)
                lineTo(w, cY)
                lineTo(w, h)
                // Connect back along bottom
                val revBottom = Path().apply {
                    addPath(bottomPath)
                }
                addPath(revBottom)
                close()
            }

            drawPath(
                path = ribbon,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        accentGlow.copy(alpha = alphas[layerIdx]),
                        themeColor.copy(alpha = alphas[layerIdx]),
                        accentGlow.copy(alpha = alphas[layerIdx])
                    )
                )
            )
        }

        // Center Horizontal Trace Spine Line
        drawLine(
            color = Color.White.copy(alpha = 0.9f),
            start = Offset(0f, cY),
            end = Offset(w, cY),
            strokeWidth = 2.dp.toPx()
        )

        // Floating Value Pills inside the Blue Center Stream
        if (streamShape == StreamShapeType.SPINDLE_CENTER) {
            val pillValues = listOf("6,665", "20,441", "4,212")
            colPositions.forEachIndexed { idx, xPos ->
                val pillWidth = 44.dp.toPx()
                val pillHeight = 22.dp.toPx()
                val pillRect = androidx.compose.ui.geometry.RoundRect(
                    left = xPos - (pillWidth / 2f),
                    top = cY - (pillHeight / 2f),
                    right = xPos + (pillWidth / 2f),
                    bottom = cY + (pillHeight / 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(11.dp.toPx(), 11.dp.toPx())
                )

                // White pill container
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(pillRect.left, pillRect.top),
                    size = Size(pillWidth, pillHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(11.dp.toPx(), 11.dp.toPx())
                )
            }
        }
    }
}

// =========================================================
// 2. EXACT DRIBBBLE DONATION GOAL HEART CARD
// =========================================================

@Composable
private fun DribbbleGoalHeartCard(
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
            // 1. Circular Top Icon Badge
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = Color(0xFFEDE9FE)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFF4C1D95),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Title & Target Fraction
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isDiscreet) "•••• / ••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", currentAmount)} / $currencySymbol${String.format(Locale.US, "%,.0f", targetAmount)}",
                fontSize = 13.5.sp,
                color = TextMuted,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 3. Exact Plump Heart with Liquid Wave Fill & Grid Background
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                HeartGoalLiquidCanvas(
                    fillPercentage = completionRatio,
                    modifier = Modifier.fillMaxSize()
                )

                // Large Centered Percentage
                Text(
                    text = "${(completionRatio * 100).toInt()}%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Encouraging Target Gap Text
            val gap = (targetAmount - currentAmount).coerceAtLeast(0.0)
            Text(
                text = if (isDiscreet) "Retain capital to reach your target." else "Retain $currencySymbol${String.format(Locale.US, "%,.0f", gap)} to reach your annual target.",
                fontSize = 12.5.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HeartGoalLiquidCanvas(
    fillPercentage: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Subtle Background Grid Mesh (Matches Image Grid Behind Heart)
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

        // 2. Plump, Organic Heart Bézier Path (Smooth rounded shoulders, no sharp horn corners)
        val heartPath = Path().apply {
            moveTo(w / 2f, h * 0.28f)
            // Top-left round lobe
            cubicTo(w * 0.30f, h * 0.05f, w * 0.02f, h * 0.20f, w * 0.02f, h * 0.46f)
            // Bottom-left curve to center tip
            cubicTo(w * 0.02f, h * 0.68f, w * 0.28f, h * 0.82f, w / 2f, h * 0.96f)
            // Bottom-right curve to center tip
            cubicTo(w * 0.72f, h * 0.82f, w * 0.98f, h * 0.68f, w * 0.98f, h * 0.46f)
            // Top-right round lobe
            cubicTo(w * 0.98f, h * 0.20f, w * 0.70f, h * 0.05f, w / 2f, h * 0.28f)
            close()
        }

        // 3. Draw Soft Frosted Outer Glow / Translucent Heart Background
        drawPath(
            path = heartPath,
            color = Color(0xFFF3E8FF).copy(alpha = 0.55f)
        )

        // 4. Clip Liquid Wave Drawing Strictly Inside Heart
        clipPath(heartPath) {
            val fillHeight = h * fillPercentage.coerceIn(0.08f, 1f)
            val fillTop = h - fillHeight

            // Smooth Multi-Wave Surface Line
            val wave = Path().apply {
                moveTo(0f, fillTop)
                cubicTo(w * 0.25f, fillTop - 12f, w * 0.45f, fillTop + 8f, w * 0.70f, fillTop - 8f)
                cubicTo(w * 0.85f, fillTop - 16f, w * 0.95f, fillTop + 4f, w, fillTop)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }

            // Rich Purple Liquid Gradient
            drawPath(
                path = wave,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF8B5CF6), // Bright Violet
                        Color(0xFF6366F1), // Indigo
                        Color(0xFF4F46E5)  // Deep Royal Purple
                    ),
                    startY = fillTop,
                    endY = h
                )
            )

            // Lighter Wave Crest Highlight
            val crest = Path().apply {
                moveTo(0f, fillTop)
                cubicTo(w * 0.25f, fillTop - 12f, w * 0.45f, fillTop + 8f, w * 0.70f, fillTop - 8f)
                cubicTo(w * 0.85f, fillTop - 16f, w * 0.95f, fillTop + 4f, w, fillTop)
                lineTo(w, fillTop + 14f)
                cubicTo(w * 0.70f, fillTop + 6f, w * 0.45f, fillTop + 22f, 0f, fillTop + 14f)
                close()
            }
            drawPath(crest, color = Color.White.copy(alpha = 0.28f))
        }

        // 5. Crisp Outer Border
        drawPath(
            path = heartPath,
            color = Color(0xFFDDD6FE),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// =========================================================
// AUXILIARY REPORT CARDS & TIMELINE TILES
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
    data: MonthDataSummary,
    currencySymbol: String,
    isDiscreet: Boolean,
    onTapMonth: () -> Unit
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
        modifier = Modifier
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
