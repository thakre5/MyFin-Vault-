package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReportsAnalyticsScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToTaxonomy: () -> Unit = {},
    onNavigateToVaults: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()

    val uiState by viewModel.monthlyUiState.collectAsState()
    val yearlyState by viewModel.yearlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val avgMonthlySpend by viewModel.averageMonthlySpend.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()
    val pageTitles = remember { listOf("Summary", "Categories", "Wealth") }

    var selectedTimeRange by remember { mutableStateOf(TimeRangeFilter.THIS_MONTH) }
    var selectedVelocityRange by remember { mutableStateOf(VelocityRange.M) }
    var showTimeRangeMenu by remember { mutableStateOf(false) }
    var showStrategyInfoSheet by remember { mutableStateOf(false) }
    var activeChartMetricInfo by remember { mutableStateOf<ChartMetricInfo?>(null) }
    var isDiscreetMode by remember { mutableStateOf(false) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val ok = viewModel.exportCsvToUri(context, it)
                Toast.makeText(context, if (ok) "Ledger (.csv) exported!" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val xlsxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val ok = ExcelExportManager.exportToUri(context, it, userProfile.currencySymbol)
                Toast.makeText(context, if (ok) "Excel statement (.xlsx) saved!" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                Toast.makeText(context, "Financial Statement (.pdf) exported!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val allTransactions = remember(yearlyState.allYearTransactions, uiState.groupedTransactions) {
        val currentMonthTxs = uiState.groupedTransactions.values.flatten()
        (yearlyState.allYearTransactions + currentMonthTxs).distinctBy { it.id }
    }

    val activeAccounts = remember(uiState.activeAccounts, uiState.accounts) {
        uiState.activeAccounts.ifEmpty { uiState.accounts.filter { !it.isArchived } }
    }

    val isPersonalExpense = remember {
        { tx: TransactionEntity ->
            tx.type == TransactionType.EXPENSE && !tx.category.equals("Work & Professional", ignoreCase = true)
        }
    }

    val isNonPersonalInflow = remember {
        { tx: TransactionEntity ->
            tx.type == TransactionType.INCOME &&
            (tx.category.equals("Passive & Capital Drawdowns", ignoreCase = true) ||
             tx.category.equals("Reimbursements & Claims", ignoreCase = true) ||
             tx.category.equals("Reimbursements & Corporate Inflow", ignoreCase = true) ||
             tx.subcategory.contains("Loan Paybacks Received", ignoreCase = true) ||
             tx.title.contains("Loan Payback", ignoreCase = true) ||
             tx.subcategory.contains("Tax & Purchase Refunds", ignoreCase = true) ||
             tx.title.contains("Refund", ignoreCase = true) ||
             tx.subcategory.contains("Capital Gains", ignoreCase = true) ||
             tx.subcategory.contains("Realization", ignoreCase = true) ||
             tx.subcategory.contains("Emergency Fund Drawdown", ignoreCase = true) ||
             tx.subcategory.contains("FD / Deposit Maturity", ignoreCase = true))
        }
    }

    val isLoanGiven = remember {
        { tx: TransactionEntity ->
            tx.type == TransactionType.ASSET &&
            (tx.subcategory.contains("Personal Loans", ignoreCase = true) ||
             tx.subcategory.contains("Loaned", ignoreCase = true))
        }
    }

    val isNpaWriteOff = remember {
        { tx: TransactionEntity ->
            tx.type == TransactionType.ASSET &&
            (tx.subcategory.contains("NPA", ignoreCase = true) ||
             tx.subcategory.contains("Bad Debt", ignoreCase = true) ||
             tx.category.equals("NPA", ignoreCase = true))
        }
    }

    val isGenuineSavingsOrAsset = remember {
        { tx: TransactionEntity ->
            tx.type == TransactionType.ASSET && !isLoanGiven(tx) && !isNpaWriteOff(tx)
        }
    }

    // Dynamic Time-Range Filter Pipeline
    val filteredTransactions = remember(allTransactions, selectedTimeRange) {
        val calendar = Calendar.getInstance()
        when (selectedTimeRange) {
            TimeRangeFilter.THIS_WEEK -> {
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val offset = (dayOfWeek + 5) % 7
                val startCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -offset)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfWeek = startCal.timeInMillis
                val endOfWeek = startOfWeek + (7L * 24 * 60 * 60 * 1000)
                allTransactions.filter { it.date in startOfWeek until endOfWeek }
            }
            TimeRangeFilter.THIS_MONTH -> {
                val currentMonth = calendar.get(Calendar.MONTH) + 1
                val currentYear = calendar.get(Calendar.YEAR)
                allTransactions.filter { tx -> tx.month == currentMonth && tx.year == currentYear }
            }
            TimeRangeFilter.LAST_MONTH -> {
                val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                val lastMonth = targetCal.get(Calendar.MONTH) + 1
                val targetYear = targetCal.get(Calendar.YEAR)
                allTransactions.filter { tx -> tx.month == lastMonth && tx.year == targetYear }
            }
            TimeRangeFilter.THIS_YEAR -> {
                val currentYear = calendar.get(Calendar.YEAR)
                allTransactions.filter { tx -> tx.year == currentYear }
            }
        }
    }

    val personalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME && !isNonPersonalInflow(it) }.sumOf { it.amount }
    }

    val personalExpenses = remember(filteredTransactions) {
        filteredTransactions.filter { isPersonalExpense(it) }.sumOf { it.amount }
    }

    val totalAssets = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.ASSET }.sumOf { it.amount }
    }

    val genuineAssets = remember(filteredTransactions) {
        filteredTransactions.filter(isGenuineSavingsOrAsset).sumOf { it.amount }
    }

    val netSurplus = personalIncome - personalExpenses - genuineAssets

    val corporateOutlays = remember(filteredTransactions) {
        filteredTransactions.filter {
            (it.type == TransactionType.CORPORATE && !it.category.equals("Reimbursements & Claims", ignoreCase = true)) ||
            (it.type == TransactionType.EXPENSE && it.category.equals("Work & Professional", ignoreCase = true))
        }.sumOf { it.amount }
    }

    val corporateReimbursements = remember(filteredTransactions) {
        filteredTransactions.filter {
            (it.type == TransactionType.CORPORATE && it.category.equals("Reimbursements & Claims", ignoreCase = true)) ||
            (it.type == TransactionType.INCOME && it.category.equals("Reimbursements & Corporate Inflow", ignoreCase = true))
        }.sumOf { it.amount }
    }

    val fixedOutflow = remember(filteredTransactions, uiState.fixedBills, selectedTimeRange) {
        when (selectedTimeRange) {
            TimeRangeFilter.THIS_MONTH -> {
                val scheduledFixed = uiState.fixedBills.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val paidFixed = filteredTransactions.filter { isPersonalExpense(it) && it.linkedFixedBillId != null }.sumOf { it.amount }
                max(scheduledFixed, paidFixed)
            }
            else -> {
                filteredTransactions.filter { isPersonalExpense(it) && it.linkedFixedBillId != null }.sumOf { it.amount }
            }
        }
    }

    val variableOutflow = remember(filteredTransactions) {
        filteredTransactions.filter { isPersonalExpense(it) && it.linkedFixedBillId == null }.sumOf { it.amount }
    }

    val dynamicSpendBuckets = remember(filteredTransactions, selectedTimeRange) {
        val calendar = Calendar.getInstance()
        when (selectedTimeRange) {
            TimeRangeFilter.THIS_WEEK -> {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                val offset = (dayOfWeek + 5) % 7
                val startCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -offset)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val startOfWeek = startCal.timeInMillis
                val endOfWeek = startOfWeek + (7L * 24 * 60 * 60 * 1000)

                val weekTxs = filteredTransactions.filter { it.date in startOfWeek until endOfWeek && isPersonalExpense(it) }
                val essentialSums = DoubleArray(7) { 0.0 }
                val discretionarySums = DoubleArray(7) { 0.0 }
                val dayCal = Calendar.getInstance()

                for (tx in weekTxs) {
                    dayCal.timeInMillis = tx.date
                    val dayIndex = ((dayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7).coerceIn(0, 6)
                    if (tx.linkedFixedBillId != null) {
                        essentialSums[dayIndex] += tx.amount
                    } else {
                        discretionarySums[dayIndex] += tx.amount
                    }
                }

                days.mapIndexed { index, label ->
                    DailySpendData(label, essentialSums[index], discretionarySums[index], essentialSums[index] + discretionarySums[index])
                }
            }
            TimeRangeFilter.THIS_MONTH, TimeRangeFilter.LAST_MONTH -> {
                val weeks = listOf("W1", "W2", "W3", "W4")
                val essentialSums = DoubleArray(4) { 0.0 }
                val discretionarySums = DoubleArray(4) { 0.0 }
                val dayCal = Calendar.getInstance()

                for (tx in filteredTransactions.filter { isPersonalExpense(it) }) {
                    dayCal.timeInMillis = tx.date
                    val day = dayCal.get(Calendar.DAY_OF_MONTH)
                    val weekIdx = ((day - 1) / 7).coerceIn(0, 3)
                    if (tx.linkedFixedBillId != null) {
                        essentialSums[weekIdx] += tx.amount
                    } else {
                        discretionarySums[weekIdx] += tx.amount
                    }
                }

                weeks.mapIndexed { index, label ->
                    DailySpendData(label, essentialSums[index], discretionarySums[index], essentialSums[index] + discretionarySums[index])
                }
            }
            TimeRangeFilter.THIS_YEAR -> {
                val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                val essentialSums = DoubleArray(12) { 0.0 }
                val discretionarySums = DoubleArray(12) { 0.0 }

                for (tx in filteredTransactions.filter { isPersonalExpense(it) }) {
                    val mIdx = (tx.month - 1).coerceIn(0, 11)
                    if (tx.linkedFixedBillId != null) {
                        essentialSums[mIdx] += tx.amount
                    } else {
                        discretionarySums[mIdx] += tx.amount
                    }
                }

                months.mapIndexed { index, label ->
                    DailySpendData(label, essentialSums[index], discretionarySums[index], essentialSums[index] + discretionarySums[index])
                }
            }
        }
    }

    val velocityTrajectoryData = remember(allTransactions, selectedVelocityRange, uiState.metrics.plannedExpenses, userProfile.baseMonthlyIncome) {
        val basePlan = if (uiState.metrics.plannedExpenses > 0) uiState.metrics.plannedExpenses else userProfile.baseMonthlyIncome.coerceAtLeast(100.0)
        val now = Calendar.getInstance()
        val txCal = Calendar.getInstance()

        when (selectedVelocityRange) {
            VelocityRange.W -> {
                val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
                val offset = (dayOfWeek + 5) % 7
                val startCal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_MONTH, -offset)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                val startOfWeek = startCal.timeInMillis
                val endOfWeek = startOfWeek + (7L * 24 * 60 * 60 * 1000)
                val weekTxs = allTransactions.filter { it.date in startOfWeek until endOfWeek && isPersonalExpense(it) }

                val dailyAmounts = DoubleArray(7) { 0.0 }
                for (tx in weekTxs) {
                    txCal.timeInMillis = tx.date
                    val idx = ((txCal.get(Calendar.DAY_OF_WEEK) + 5) % 7).coerceIn(0, 6)
                    dailyAmounts[idx] += tx.amount
                }

                var runActual = 0.0
                val totalTarget = basePlan * (7.0 / 30.0)
                labels.mapIndexed { i, lbl ->
                    runActual += dailyAmounts[i]
                    val runTarget = totalTarget * ((i + 1) / 7.0)
                    TrajectoryPointData(lbl, runActual, runTarget)
                }
            }
            VelocityRange.M -> {
                val labels = listOf("W1", "W2", "W3", "W4")
                val curM = now.get(Calendar.MONTH) + 1
                val curY = now.get(Calendar.YEAR)
                val monthTxs = allTransactions.filter { it.month == curM && it.year == curY && isPersonalExpense(it) }

                val weekAmounts = DoubleArray(4) { 0.0 }
                for (tx in monthTxs) {
                    txCal.timeInMillis = tx.date
                    val wIdx = ((txCal.get(Calendar.DAY_OF_MONTH) - 1) / 7).coerceIn(0, 3)
                    weekAmounts[wIdx] += tx.amount
                }

                var runActual = 0.0
                labels.mapIndexed { i, lbl ->
                    runActual += weekAmounts[i]
                    val runTarget = basePlan * ((i + 1) / 4.0)
                    TrajectoryPointData(lbl, runActual, runTarget)
                }
            }
            VelocityRange.THREE_M -> {
                val points = mutableListOf<TrajectoryPointData>()
                var runActual = 0.0
                val totalTarget = basePlan * 3.0

                for (offset in 2 downTo 0) {
                    val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, -offset) }
                    val m = targetCal.get(Calendar.MONTH) + 1
                    val y = targetCal.get(Calendar.YEAR)
                    val mName = SimpleDateFormat("MMM", Locale.US).format(targetCal.time)
                    val mAmt = allTransactions.filter { it.month == m && it.year == y && isPersonalExpense(it) }.sumOf { it.amount }
                    runActual += mAmt
                    val runTarget = basePlan * (3 - offset)
                    points.add(TrajectoryPointData(mName, runActual, runTarget))
                }
                points
            }
            VelocityRange.SIX_M -> {
                val points = mutableListOf<TrajectoryPointData>()
                var runActual = 0.0
                for (offset in 5 downTo 0) {
                    val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, -offset) }
                    val m = targetCal.get(Calendar.MONTH) + 1
                    val y = targetCal.get(Calendar.YEAR)
                    val mName = SimpleDateFormat("MMM", Locale.US).format(targetCal.time)
                    val mAmt = allTransactions.filter { it.month == m && it.year == y && isPersonalExpense(it) }.sumOf { it.amount }
                    runActual += mAmt
                    val runTarget = basePlan * (6 - offset)
                    points.add(TrajectoryPointData(mName, runActual, runTarget))
                }
                points
            }
            VelocityRange.Y -> {
                val curY = now.get(Calendar.YEAR)
                val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val monthTxs = allTransactions.filter { it.year == curY && isPersonalExpense(it) }
                val monthAmounts = DoubleArray(12) { 0.0 }
                for (tx in monthTxs) {
                    monthAmounts[(tx.month - 1).coerceIn(0, 11)] += tx.amount
                }

                var runActual = 0.0
                months.mapIndexed { i, lbl ->
                    runActual += monthAmounts[i]
                    val runTarget = basePlan * (i + 1)
                    TrajectoryPointData(lbl.take(1), runActual, runTarget)
                }
            }
        }
    }

    val fabActions = remember(selectedTimeRange) {
        listOf(
            DockFabAction(
                icon = Icons.Default.PictureAsPdf,
                label = "Export PDF Statement",
                onClick = {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    pdfExportLauncher.launch("MyFin_Statement_${selectedTimeRange.name}_$timeStamp.pdf")
                }
            ),
            DockFabAction(
                icon = Icons.Default.TableChart,
                label = "Export Excel (.xlsx)",
                onClick = {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    xlsxExportLauncher.launch("MyFin_Report_${selectedTimeRange.name}_$timeStamp.xlsx")
                }
            ),
            DockFabAction(
                icon = Icons.Default.Description,
                label = "Export Ledger (.csv)",
                onClick = {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                    csvExportLauncher.launch("MyFin_Ledger_${selectedTimeRange.name}_$timeStamp.csv")
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasLight)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenDrawer()
                        },
                        modifier = Modifier
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Reports & Analytics",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { isDiscreetMode = !isDiscreetMode },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isDiscreetMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Balance Privacy",
                                tint = if (isDiscreetMode) AccentPurple else TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Box {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showTimeRangeMenu = true
                                },
                            shape = RoundedCornerShape(20.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedTimeRange.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTimeRangeMenu,
                            onDismissRequest = { showTimeRangeMenu = false }
                        ) {
                            TimeRangeFilter.entries.forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.label, fontSize = 13.sp, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedTimeRange = filter
                                        showTimeRangeMenu = false
                                    }
                                )
                            }
                        }
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

            // Pager Tab Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> ReportsSummaryTab(
                        userProfileCurrency = userProfile.currencySymbol,
                        totalIncome = personalIncome,
                        netSurplus = netSurplus,
                        fixedOutflow = fixedOutflow,
                        variableOutflow = variableOutflow,
                        selectedVelocityRange = selectedVelocityRange,
                        onSelectVelocityRange = { selectedVelocityRange = it },
                        selectedTimeRange = selectedTimeRange,
                        onSelectTimeRange = { selectedTimeRange = it },
                        plannedBudget = uiState.metrics.plannedExpenses,
                        safeToSpend = uiState.metrics.safeToSpend,
                        spendData = dynamicSpendBuckets,
                        trajectoryData = velocityTrajectoryData,
                        allTransactions = allTransactions,
                        isDiscreet = isDiscreetMode,
                        onOpenMetricInfo = { activeChartMetricInfo = it }
                    )
                    1 -> ReportsCategoriesTab(
                        userProfileCurrency = userProfile.currencySymbol,
                        totalExpenses = personalExpenses,
                        totalAssets = totalAssets,
                        corporateOutlays = corporateOutlays,
                        corporateReimbursements = corporateReimbursements,
                        transactions = filteredTransactions,
                        allTransactions = allTransactions,
                        selectedTimeRange = selectedTimeRange,
                        isDiscreet = isDiscreetMode,
                        onOpenMetricInfo = { activeChartMetricInfo = it }
                    )
                    2 -> ReportsWealthTab(
                        userProfileCurrency = userProfile.currencySymbol,
                        vaultMode = userProfile.vaultMode,
                        onOpenStrategyInfo = { showStrategyInfoSheet = true },
                        totalInvestments = yearlyState.assetWealthMetrics.totalInvestments,
                        realizableNetWorth = yearlyState.assetWealthMetrics.realizableNetWorth,
                        monthlyBurnRate = avgMonthlySpend,
                        accounts = activeAccounts,
                        isDiscreet = isDiscreetMode,
                        onOpenMetricInfo = { activeChartMetricInfo = it }
                    )
                }
            }
        }

        // Floating Indicator Pill
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

        // Bottom Dock
        AppBottomDock(
            currentSelection = NavigationTarget.REPORTS_ANALYTICS,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.MONTHLY_VIEW -> onNavigateToDashboard()
                    NavigationTarget.BUDGET_PLANNER -> onNavigateToPlanner()
                    NavigationTarget.VAULT_ACCOUNTS -> onNavigateToVaults()
                    NavigationTarget.DATA_SET -> onNavigateToTaxonomy()
                    NavigationTarget.REPORTS_ANALYTICS -> {}
                    else -> {}
                }
            },
            fabActions = fabActions,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        )

        if (showStrategyInfoSheet) {
            StrategyArchitectureBottomSheet(
                vaultMode = userProfile.vaultMode,
                onDismiss = { showStrategyInfoSheet = false },
                onNavigateToSettings = onNavigateToSettings
            )
        }

        activeChartMetricInfo?.let { info ->
            ChartMetricInfoBottomSheet(
                info = info,
                onDismiss = { activeChartMetricInfo = null }
            )
        }
    }
}
