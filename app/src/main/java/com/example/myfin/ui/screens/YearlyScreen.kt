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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.ExcelExportManager
import com.example.myfin.data.TransactionType
import com.example.myfin.data.YearlyCategoryRollup
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

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
    val isFuture: Boolean
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

    val monthNames = remember {
        listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    }

    // Relative Calendar Timeline Checks
    val currentCal = remember { Calendar.getInstance() }
    val thisYear = currentCal.get(Calendar.YEAR)
    val thisMonth = currentCal.get(Calendar.MONTH) + 1

    val allTransactions = remember(uiState.groupedTransactions, uiState.selectedYear) {
        val txCal = Calendar.getInstance()
        uiState.groupedTransactions.values.flatten().filter { tx ->
            txCal.timeInMillis = tx.date
            txCal.get(Calendar.YEAR) == uiState.selectedYear
        }
    }

    val yearlyMonthsData = remember(allTransactions, uiState.selectedYear) {
        val txCal = Calendar.getInstance()
        (1..12).map { m ->
            val isFutureMonth = (uiState.selectedYear == thisYear && m > thisMonth) || (uiState.selectedYear > thisYear)
            val monthTxs = allTransactions.filter { tx ->
                txCal.timeInMillis = tx.date
                (txCal.get(Calendar.MONTH) + 1) == m
            }

            val inc = monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
            val exp = monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
            val ast = monthTxs.filter { it.type == TransactionType.ASSET }.sumOf { it.amount }
            val net = inc - exp - ast

            MonthDataSummary(
                monthIndex = m,
                monthName = monthNames[m - 1],
                income = inc,
                expenses = exp,
                assets = ast,
                netSavings = net,
                isFuture = isFutureMonth
            )
        }
    }

    val annualIncome = yearlyMonthsData.sumOf { it.income }
    val annualExpenses = yearlyMonthsData.sumOf { it.expenses }
    val annualAssets = yearlyMonthsData.sumOf { it.assets }
    val annualNetSurplus = annualIncome - annualExpenses - annualAssets
    val annualSavingsRate = if (annualIncome > 0) ((annualNetSurplus / annualIncome) * 100).coerceIn(0.0, 100.0) else 0.0

    val expenseBreakdown = remember(allTransactions) {
        allTransactions.filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .map { (cat, txs) -> YearlyCategoryRollup(cat, TransactionType.EXPENSE, txs.sumOf { it.amount }) }
            .sortedByDescending { it.totalActualAmount }
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
            // 1. PINNED TOP BAR (WITH YEAR PICKER CAPSULE & DISSOLVE)
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
                            imageVector = Icons.Default.ChevronLeft,
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
                }

                // Smooth Downward Dissolve Shelf
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CanvasLight,
                                    CanvasLight.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }

            // =========================================================
            // 2. FULL-SCREEN HORIZONTAL PAGER
            // =========================================================
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    // --- SUB-SCREEN 0: EXECUTIVE SUMMARY ---
                    0 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
                        ) {
                            // Dark Spline Topography Hero Card
                            item {
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
                                                    text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", annualNetSurplus)}",
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
                                                        text = "${annualSavingsRate.toInt()}% Saved",
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
                                                Text(text = "Inflow ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualIncome)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SoftTeal))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "Assets ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualAssets)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentPurple))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(text = "Burn ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualExpenses)}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            // Wealth Goal Execution Capsule
                            item {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(3.dp, RoundedCornerShape(22.dp)),
                                    shape = RoundedCornerShape(22.dp),
                                    color = CardWhite,
                                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(18.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier.size(86.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            LiquidGoalPillCanvas(
                                                fillPercentage = (annualSavingsRate / 100f).toFloat().coerceIn(0.1f, 1f),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                            Text(
                                                text = "${annualSavingsRate.toInt()}%",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                color = Color.White
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(16.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "Annual Wealth Accumulation", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = "Accumulated ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualAssets + annualNetSurplus)} towards annual compounding reserves.",
                                                fontSize = 11.5.sp,
                                                color = TextMuted,
                                                lineHeight = 16.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))
                            }

                            // Macro Metrics Row
                            item {
                                val activeMonthsCount = if (uiState.selectedYear == thisYear) thisMonth.coerceAtLeast(1) else 12
                                val avgMonthlyBurn = annualExpenses / activeMonthsCount
                                val avgDailyBurn = annualExpenses / (activeMonthsCount * 30).coerceAtLeast(1)

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    YearlyPillarPill(
                                        modifier = Modifier.weight(1f),
                                        title = "Avg Monthly Burn",
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", avgMonthlyBurn)}",
                                        tint = AccentPurple
                                    )
                                    YearlyPillarPill(
                                        modifier = Modifier.weight(1f),
                                        title = "Avg Daily Spend",
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", avgDailyBurn)}",
                                        tint = SoftRed
                                    )
                                    YearlyPillarPill(
                                        modifier = Modifier.weight(1f),
                                        title = "SIP Investment",
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", annualAssets)}",
                                        tint = SoftTeal
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }

                    // --- SUB-SCREEN 1: 12 MONTHS TIMELINE GRID ---
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
                                    onTapMonth = {
                                        viewModel.selectMonth(mData.monthIndex)
                                        onNavigateToMonth(uiState.selectedYear, mData.monthIndex)
                                    }
                                )
                            }
                        }
                    }

                    // --- SUB-SCREEN 2: ANNUAL AUDIT & CATEGORY SPECTRUM ---
                    2 -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
                        ) {
                            item {
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
                                            categorySums = expenseBreakdown.map { it.totalActualAmount }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            item {
                                Text(text = "Highest Outflow Segments", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                Spacer(modifier = Modifier.height(10.dp))
                            }

                            if (expenseBreakdown.isEmpty()) {
                                item {
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
                                items(expenseBreakdown.size) { idx ->
                                    val item = expenseBreakdown[idx]
                                    val ratio = if (annualExpenses > 0) (item.totalActualAmount / annualExpenses).toFloat() else 0f

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        color = CardWhite,
                                        border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = item.category, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                                                Text(
                                                    text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", item.totalActualAmount)} (${(ratio * 100).toInt()}%)",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 13.sp,
                                                    color = AccentPurple
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LinearProgressIndicator(
                                                progress = { ratio.coerceIn(0.04f, 1f) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = AccentPurple,
                                                trackColor = BorderLight.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================
        // 3. BOTTOM GRADIENT SCRIM (DISSOLVES CONTENT BEFORE DOCK)
        // =========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CanvasLight.copy(alpha = 0.85f),
                            CanvasLight
                        )
                    )
                )
                .zIndex(2.5f)
        )

        // =========================================================
        // 4. FLOATING PAGER INDICATOR PILL (ANCHORED BOTTOM LEFT)
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
        // 5. STANDARDIZED FLOATING BOTTOM NAVIGATION DOCK WITH FAB
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
    }
}

// =========================================================
// VECTOR CANVASES & TIMELINE COMPONENTS
// =========================================================

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

        // Income Underfill
        val incomeFill = Path().apply {
            addPath(incomePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(incomeFill, brush = Brush.verticalGradient(listOf(SoftGreen.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(incomePath, color = SoftGreen, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Expense Underfill
        val expenseFill = Path().apply {
            addPath(expensePath)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(expenseFill, brush = Brush.verticalGradient(listOf(AccentPurple.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(expensePath, color = AccentPurple, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

        // Assets Line
        drawPath(assetsPath, color = SoftTeal, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun LiquidGoalPillCanvas(
    fillPercentage: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerRadius = CornerRadius(w / 2f, w / 2f)

        drawRoundRect(
            color = BorderLight.copy(alpha = 0.35f),
            size = Size(w, h),
            cornerRadius = cornerRadius
        )

        val fillH = h * fillPercentage.coerceIn(0.08f, 1f)
        val fillTop = h - fillH

        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(AccentPurple, Color(0xFF8B5CF6))
            ),
            topLeft = Offset(0f, fillTop),
            size = Size(w, fillH),
            cornerRadius = cornerRadius
        )
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

        val maxAmount = categorySums.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val polyPath = Path()
        for (i in 0 until numAxes) {
            val amt = categorySums.getOrNull(i) ?: 0.0
            val ratio = if (categorySums.isNotEmpty()) (amt / maxAmount).toFloat().coerceIn(0.15f, 0.95f) else 0.2f
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

@Composable
private fun MonthGridTimelineCard(
    data: MonthDataSummary,
    currencySymbol: String,
    onTapMonth: () -> Unit
) {
    val isSurplus = data.netSavings >= 0
    val statusColor = if (data.isFuture) TextMuted else if (isSurplus) SoftGreen else SoftRed

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
                        text = if (data.isFuture) "Planned" else if (isSurplus) "+${currencySymbol}${(data.netSavings / 1000).toInt()}k" else "-${currencySymbol}${(data.netSavings / 1000).toInt()}k",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${currencySymbol}${String.format(Locale.US, "%,.0f", data.expenses)}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = if (data.isFuture) TextMuted else TextDark
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
                Box(modifier = Modifier.weight((data.income / total).toFloat().coerceIn(0.05f, 0.9f)).fillMaxHeight().background(SoftGreen))
                Box(modifier = Modifier.weight((data.expenses / total).toFloat().coerceIn(0.05f, 0.9f)).fillMaxHeight().background(AccentPurple))
                Box(modifier = Modifier.weight((data.assets / total).toFloat().coerceIn(0.05f, 0.9f)).fillMaxHeight().background(SoftTeal))
            }
        }
    }
}

@Composable
private fun YearlyPillarPill(
    title: String,
    amount: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = amount, fontSize = 14.sp, fontWeight = FontWeight.Black, color = tint)
        }
    }
}
