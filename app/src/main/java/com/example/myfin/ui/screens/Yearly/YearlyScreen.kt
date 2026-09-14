package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.ExcelExportManager
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.YearlyMonthData
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
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
    val annualExpenses = yearlyState.totalYearlyExpense
    val annualAssets = yearlyState.totalYearlyAssets
    val annualNetSurplus = yearlyState.annualNetSurplus
    val annualPersonalIncome = yearlyState.annualPersonalIncome
    val wealthMetrics = yearlyState.assetWealthMetrics
    val multiYearAssets = yearlyState.multiYearAssets
    val allYearTransactions = yearlyState.allYearTransactions

    // Dynamic wealth goal bound to fortress target
    val annualTargetGoal = remember(annualPersonalIncome, userProfile.baseMonthlyIncome, uiState.fortressTarget) {
        val base = if (annualPersonalIncome > 0) annualPersonalIncome else (userProfile.baseMonthlyIncome * 12)
        val target = maxOf(base * 0.25, uiState.fortressTarget)
        if (target > 0.0) target else 1.0
    }
    val currentWealthAccumulated = (annualAssets + annualNetSurplus).coerceAtLeast(0.0)
    val goalCompletionPercentage = if (annualTargetGoal > 0.0) (currentWealthAccumulated / annualTargetGoal).toFloat().coerceIn(0f, 1f) else 0f

    val quarterlyData = remember(yearlyMonthsData) {
        if (yearlyMonthsData.size >= 12) {
            listOf(
                "Q1" to yearlyMonthsData.subList(0, 3),
                "Q2" to yearlyMonthsData.subList(3, 6),
                "Q3" to yearlyMonthsData.subList(6, 9),
                "Q4" to yearlyMonthsData.subList(9, 12)
            ).mapIndexed { qIdx, (label, months) ->
                val qInc = months.sumOf { it.netSavings + it.lifestyleExpenses + it.assets }
                val qExp = months.sumOf { it.lifestyleExpenses }
                val qAst = months.sumOf { it.assets }
                val qNet = months.sumOf { it.netSavings }
                val qRate = if (qInc > 0) ((qNet / qInc) * 100).coerceIn(-100.0, 100.0) else 0.0
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

    val plannedCategoryCeilings = remember(uiState.categories) {
        uiState.categories.associate { it.category to (it.plannedAmount * 12.0) }
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
                    0 -> YearlyCashflowTab(
                        yearlyState = yearlyState,
                        quarterlyData = quarterlyData,
                        currencySymbol = userProfile.currencySymbol,
                        isDiscreetMode = isDiscreetMode,
                        onOpenGraphGuide = { activeGraphGuide = it }
                    )

                    1 -> YearlyMonthsTab(
                        yearlyMonthsData = yearlyMonthsData,
                        currencySymbol = userProfile.currencySymbol,
                        isDiscreetMode = isDiscreetMode,
                        onOpenGraphGuide = { activeGraphGuide = it },
                        onInspectMonth = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            inspectedMonth = it
                        }
                    )

                    2 -> YearlyAssetsTab(
                        currentWealthAccumulated = currentWealthAccumulated,
                        annualTargetGoal = annualTargetGoal,
                        goalCompletionPercentage = goalCompletionPercentage,
                        wealthMetrics = wealthMetrics,
                        multiYearAssets = multiYearAssets,
                        selectedYear = uiState.selectedYear,
                        currencySymbol = userProfile.currencySymbol,
                        isDiscreetMode = isDiscreetMode,
                        onOpenGraphGuide = { activeGraphGuide = it }
                    )

                    3 -> YearlyAuditTab(
                        categoryTrajectories = categoryTrajectories,
                        plannedCategoryCeilings = plannedCategoryCeilings,
                        isCategoryLegacy = { catName ->
                            uiState.masterCategories.any { it.name.equals(catName, ignoreCase = true) && it.isLegacy }
                        },
                        currencySymbol = userProfile.currencySymbol,
                        isDiscreetMode = isDiscreetMode,
                        onOpenGraphGuide = { activeGraphGuide = it }
                    )
                }
            }
        }

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

        activeGraphGuide?.let { guide ->
            GraphExplanationBottomSheet(
                guide = guide,
                onDismiss = { activeGraphGuide = null }
            )
        }

        inspectedMonth?.let { mData ->
            InspectedMonthBottomSheet(
                mData = mData,
                selectedYear = uiState.selectedYear,
                currencySymbol = userProfile.currencySymbol,
                isDiscreetMode = isDiscreetMode,
                onDismiss = { inspectedMonth = null },
                onOpenMonth = { monthIdx ->
                    viewModel.selectMonth(monthIdx)
                    onNavigateToMonth(uiState.selectedYear, monthIdx)
                }
            )
        }
    }
}
