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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
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
import androidx.compose.ui.text.style.TextOverflow
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

private val HeroDarkGradient = Brush.verticalGradient(
    colors = listOf(
        Color(0xFF1E1F30),
        Color(0xFF131422)
    )
)

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
                    // --- TAB 0: EXECUTIVE SUMMARY ---
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
                        ) {
                            // Dark Spline Topography Hero Card
                            item(key = "executive_hero_card") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(8.dp, RoundedCornerShape(26.dp)),
                                    shape = RoundedCornerShape(26.dp),
                                    color = Color(0xFF171827),
                                    border = BorderStroke(0.8.dp, Color(0xFF2E3048))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(HeroDarkGradient)
                                            .padding(20.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column {
                                                Text(
                                                    text = "ANNUAL NET RETAINED",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF9E9EBA),
                                                    letterSpacing = 0.8.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualNetSurplus)}",
                                                    fontSize = 30.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = if (annualNetSurplus >= 0) Color.White else SoftRed,
                                                    letterSpacing = (-0.5).sp
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = SoftTeal.copy(alpha = 0.18f),
                                                border = BorderStroke(0.6.dp, SoftTeal.copy(alpha = 0.4f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                        contentDescription = null,
                                                        tint = SoftTeal,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isDiscreetMode) "••%" else "${annualSavingsRate.toInt()}% Saved",
                                                        color = SoftTeal,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(18.dp))

                                        AnnualTopographyWaveCanvas(
                                            yearlyData = yearlyMonthsData,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp)
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SoftGreen))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = if (isDiscreetMode) "Inflow: ••••" else "Inflow ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualIncome)}", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SoftTeal))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = if (isDiscreetMode) "Assets: ••••" else "Assets ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualAssets)}", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentPurple))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = if (isDiscreetMode) "Burn: ••••" else "Burn ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualExpenses)}", fontSize = 10.5.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // Fiscal Quarter Performance Grid (Q1 to Q4)
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

                            // Commitments vs Discretionary Ratio
                            item(key = "commitments_vs_discretionary") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = CardWhite,
                                    border = BorderStroke(0.8.dp, BorderLight)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Annual Outflow Structure", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                        Text("Fixed AutoPay Obligations vs. Variable Lifestyle Spending", fontSize = 11.sp, color = TextMuted)

                                        Spacer(modifier = Modifier.height(12.dp))

                                        val fixedRatio = if (annualExpenses > 0) (annualFixedBills / annualExpenses).toFloat() else 0.5f
                                        val varRatio = 1f - fixedRatio

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(CircleShape)
                                                .background(BorderLight.copy(alpha = 0.4f))
                                        ) {
                                            Box(modifier = Modifier.weight(fixedRatio.coerceIn(0.05f, 0.95f)).fillMaxHeight().background(SoftRed))
                                            Box(modifier = Modifier.weight(varRatio.coerceIn(0.05f, 0.95f)).fillMaxHeight().background(AccentPurple))
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column {
                                                Text("Fixed Obligations", fontSize = 11.sp, color = TextMuted)
                                                Text(
                                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualFixedBills)} (${(fixedRatio * 100).toInt()}%)",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SoftRed
                                                )
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Variable Discretionary", fontSize = 11.sp, color = TextMuted)
                                                Text(
                                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualVariable)} (${(varRatio * 100).toInt()}%)",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentPurple
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // Peak Performance Extremes
                            item(key = "extremes_row") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = CardWhite,
                                        border = BorderStroke(0.7.dp, SoftTeal.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Star, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Peak Savings Month", fontSize = 10.5.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = bestSurplusMonth?.monthName ?: "N/A",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextDark
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", bestSurplusMonth?.netSavings ?: 0.0)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SoftTeal
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = CardWhite,
                                        border = BorderStroke(0.7.dp, SoftRed.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Whatshot, contentDescription = null, tint = SoftRed, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Peak Outflow Month", fontSize = 10.5.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = worstBurnMonth?.monthName ?: "N/A",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextDark
                                            )
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", worstBurnMonth?.expenses ?: 0.0)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SoftRed
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // --- TAB 1: 12 MONTHS TIMELINE GRID ---
                    1 -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
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

                    // --- TAB 2: AUDIT & TRAJECTORY SPECTRUM ---
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
// VECTOR CANVASES & AUXILIARY COMPONENTS
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

            // 12-Month Mini Sparkline
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

                // Highlight peak month
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
private fun AnnualTopographyWaveCanvas(
    yearlyData: List<MonthDataSummary>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val maxVal = yearlyData.maxOfOrNull { maxOf(it.income, it.expenses, it.assets) }?.coerceAtLeast(100.0) ?: 100.0
        val pointsCount = yearlyData.size

        fun createSplinePath(values: List<Double>): Path {
            val pts = values.mapIndexed { idx, v ->
                val x = (idx.toFloat() / (pointsCount - 1).coerceAtLeast(1)) * w
                val normalizedY = 1f - (v / maxVal).toFloat().coerceIn(0.1f, 0.95f)
                val y = (h * 0.1f) + (normalizedY * h * 0.75f)
                Offset(x, y)
            }
            return Path().apply {
                if (pts.isNotEmpty()) {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 0 until pts.size - 1) {
                        val p0 = pts[i]
                        val p1 = pts[i + 1]
                        val cx = (p0.x + p1.x) / 2
                        cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }
                }
            }
        }

        val incomePath = createSplinePath(yearlyData.map { it.income })
        val expensePath = createSplinePath(yearlyData.map { it.expenses })
        val assetsPath = createSplinePath(yearlyData.map { it.assets })

        val incomeFill = Path().apply {
            addPath(incomePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(incomeFill, brush = Brush.verticalGradient(listOf(SoftGreen.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(incomePath, color = SoftGreen, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        val expenseFill = Path().apply {
            addPath(expensePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(expenseFill, brush = Brush.verticalGradient(listOf(AccentPurple.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(expensePath, color = AccentPurple, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        drawPath(assetsPath, color = SoftTeal, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
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
