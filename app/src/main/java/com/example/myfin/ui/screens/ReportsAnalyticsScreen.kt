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
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.AccountBalanceResult
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
import kotlin.math.max
import kotlin.math.sin

enum class TimeRangeFilter(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    THIS_YEAR("This Year")
}

enum class VelocityRange(val label: String) {
    W("W"),
    M("M"),
    THREE_M("3 M"),
    SIX_M("6 M"),
    Y("Y")
}

data class DailySpendData(
    val dayLabel: String,
    val essentialAmount: Double,
    val discretionaryAmount: Double,
    val totalAmount: Double
)

data class TrajectoryPointData(
    val stepLabel: String,
    val actualCumulative: Double,
    val targetCumulative: Double
)

data class ChartMetricInfo(
    val title: String,
    val subtitle: String,
    val formula: String,
    val breakdown: String,
    val visualElements: List<Pair<String, String>> = emptyList(),
    val advice: String
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
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
                Toast.makeText(context, if (ok) "Ledger (.csv) exported successfully!" else "Export failed", Toast.LENGTH_SHORT).show()
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

    // Dynamic Daily / Weekly / Monthly Spend Buckets matching selectedTimeRange
    val dynamicSpendBuckets = remember(filteredTransactions, selectedTimeRange) {
        val calendar = Calendar.getInstance()
        when (selectedTimeRange) {
            TimeRangeFilter.THIS_WEEK -> {
                val days = listOf("M", "T", "W", "T", "F", "S", "S")
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

                val weekTxs = filteredTransactions.filter { it.date in startOfWeek until endOfWeek && isPersonalExpense(it) }
                val essentialSums = DoubleArray(7) { 0.0 }
                val discretionarySums = DoubleArray(7) { 0.0 }
                val dayCal = Calendar.getInstance()

                for (tx in weekTxs) {
                    dayCal.timeInMillis = tx.date
                    val txDay = dayCal.get(Calendar.DAY_OF_WEEK)
                    val dayIndex = (txDay + 5) % 7
                    if (tx.linkedFixedBillId != null) {
                        essentialSums[dayIndex] += tx.amount
                    } else {
                        discretionarySums[dayIndex] += tx.amount
                    }
                }

                days.mapIndexed { index, label ->
                    DailySpendData(
                        dayLabel = label,
                        essentialAmount = essentialSums[index],
                        discretionaryAmount = discretionarySums[index],
                        totalAmount = essentialSums[index] + discretionarySums[index]
                    )
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
                    DailySpendData(
                        dayLabel = label,
                        essentialAmount = essentialSums[index],
                        discretionaryAmount = discretionarySums[index],
                        totalAmount = essentialSums[index] + discretionarySums[index]
                    )
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
                    DailySpendData(
                        dayLabel = label,
                        essentialAmount = essentialSums[index],
                        discretionaryAmount = discretionarySums[index],
                        totalAmount = essentialSums[index] + discretionarySums[index]
                    )
                }
            }
        }
    }

    // Dynamic Multi-Span Trajectory Engine (W, M, 3 M, 6 M, Y)
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
            // 1. PINNED TOP BAR
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

            // 2. HORIZONTAL PAGER
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> {
                        SummaryAnalyticsTabContent(
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
                    }
                    1 -> {
                        CategoriesAnalyticsTabContent(
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
                    }
                    2 -> {
                        WealthAnalyticsTabContent(
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
        }

        // 3. FLOATING PAGER INDICATOR PILL
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

        // 4. FLOATING BOTTOM NAVIGATION DOCK WITH FAB
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

        // Strategy Architecture Information Sheet
        if (showStrategyInfoSheet) {
            val is3Vault = !userProfile.vaultMode.equals("SIMPLE", ignoreCase = true)
            ModalBottomSheet(
                onDismissRequest = { showStrategyInfoSheet = false },
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
                        Text("Vault Strategy Architecture", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (is3Vault) AccentPurple.copy(alpha = 0.12f) else TextDark.copy(alpha = 0.08f)
                        ) {
                            Text(
                                text = if (is3Vault) "3-Vault Active" else "Simple Mode Active",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (is3Vault) AccentPurple else TextDark,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (is3Vault) {
                            "Your wealth is systematically partitioned across structured financial tiers to prevent accidental overspending."
                        } else {
                            "Your wealth is managed as a unified, flat liquidity pool across all connected bank cards and wallets."
                        },
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    if (is3Vault) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StrategyTierInfoRow(
                                icon = Icons.Default.AccountBalance,
                                color = AccentPurple,
                                title = "Operating Vault Tier",
                                desc = "Covers everyday groceries and variable daily lifestyle spend."
                            )
                            StrategyTierInfoRow(
                                icon = Icons.Default.CreditCard,
                                color = SoftRed,
                                title = "Commitments Vault Tier",
                                desc = "Dedicated lockbox protecting AutoPay bills and EMI obligations."
                            )
                            StrategyTierInfoRow(
                                icon = Icons.Default.Security,
                                color = SoftTeal,
                                title = "Fortress Vault Tier",
                                desc = "Liquid emergency reserve safeguarding against unforeseen life events."
                            )
                            StrategyTierInfoRow(
                                icon = Icons.Default.Payments,
                                color = SoftGreen,
                                title = "Physical Cash Tier",
                                desc = "Physical wallet buffer for cash transactions and petty expenses."
                            )
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = CanvasLight,
                            border = BorderStroke(0.6.dp, BorderLight)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Flat Liquidity Structure", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                                Text(
                                    text = "Simple Mode aggregates all accounts into a single total net liquidity figure without reserve rules, strategic sweeps, or role badges.",
                                    fontSize = 11.5.sp,
                                    color = TextMuted,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showStrategyInfoSheet = false
                            onNavigateToSettings()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Customize Strategy in Settings", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Dedicated Bottom Information Sheet for Graph Titles & Visual Guides
        activeChartMetricInfo?.let { info ->
            ModalBottomSheet(
                onDismissRequest = { activeChartMetricInfo = null },
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(info.title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = TextDark)
                            Text(info.subtitle, fontSize = 11.5.sp, color = TextMuted)
                        }
                        IconButton(onClick = { activeChartMetricInfo = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Active Reading & Contribution", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(info.breakdown, fontSize = 12.5.sp, color = TextDark, lineHeight = 17.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Mathematical Formula", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = AccentPurple.copy(alpha = 0.08f),
                        border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = info.formula,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = AccentPurple,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    if (info.visualElements.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Visual Elements Explained", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            info.visualElements.forEach { (tag, desc) ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Box(modifier = Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(AccentPurple))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Text(desc, fontSize = 10.5.sp, color = TextMuted, lineHeight = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(info.advice, fontSize = 11.sp, color = TextDark, lineHeight = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryHealthIndicatorPill(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    badgeText: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(0.7.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = TextDark)
            Spacer(modifier = Modifier.height(2.dp))
            Text(badgeText, fontSize = 9.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StrategyTierInfoRow(
    icon: ImageVector,
    color: Color,
    title: String,
    desc: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextDark)
                Text(desc, fontSize = 10.5.sp, color = TextMuted, lineHeight = 14.sp)
            }
        }
    }
}

@Composable
private fun SummaryAnalyticsTabContent(
    userProfileCurrency: String,
    totalIncome: Double,
    netSurplus: Double,
    fixedOutflow: Double,
    variableOutflow: Double,
    selectedVelocityRange: VelocityRange,
    onSelectVelocityRange: (VelocityRange) -> Unit,
    selectedTimeRange: TimeRangeFilter,
    onSelectTimeRange: (TimeRangeFilter) -> Unit,
    plannedBudget: Double,
    safeToSpend: Double,
    spendData: List<DailySpendData>,
    trajectoryData: List<TrajectoryPointData>,
    allTransactions: List<TransactionEntity>,
    isDiscreet: Boolean,
    onOpenMetricInfo: (ChartMetricInfo) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showLocalVelocityMenu by remember { mutableStateOf(false) }

    val totalOutflow = fixedOutflow + variableOutflow
    val totalPeriodExpenses = spendData.sumOf { it.totalAmount }
    val daysCount = when (selectedTimeRange) {
        TimeRangeFilter.THIS_WEEK -> 7.0
        TimeRangeFilter.THIS_MONTH, TimeRangeFilter.LAST_MONTH -> 30.0
        TimeRangeFilter.THIS_YEAR -> 365.0
    }
    val dailyBurn = totalPeriodExpenses / daysCount

    val scaledPeriodBudget = remember(plannedBudget, selectedTimeRange) {
        when (selectedTimeRange) {
            TimeRangeFilter.THIS_WEEK -> plannedBudget * (7.0 / 30.0)
            TimeRangeFilter.THIS_MONTH, TimeRangeFilter.LAST_MONTH -> plannedBudget
            TimeRangeFilter.THIS_YEAR -> plannedBudget * 12.0
        }
    }

    val totalBudget = if (scaledPeriodBudget > 0) scaledPeriodBudget else (totalIncome.takeIf { it > 0 } ?: (totalOutflow * 1.25).coerceAtLeast(1.0))
    val retentionRate = if (totalIncome > 0) ((netSurplus / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0
    val commitmentLoad = if (totalBudget > 0) ((fixedOutflow / totalBudget) * 100).coerceIn(0.0, 100.0) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 4.dp, bottom = 140.dp)
    ) {
        // 1. CAPITAL RETENTION HERO CARD
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Capital Retention Rate",
                            subtitle = "Net Saved vs. Verified Inflow",
                            formula = "Retention % = ((Income - Expenses - Assets) / Income) * 100",
                            breakdown = "Personal Inflow: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalIncome)} | Net Retained: $userProfileCurrency${String.format(Locale.US, "%,.0f", netSurplus)} (${String.format(Locale.US, "%,.1f", retentionRate)}%).",
                            visualElements = listOf(
                                "Cyan/Blue Top Wave" to "Total daily/weekly outflow volume trajectory.",
                                "Rose/Violet Lower Wave" to "Essential fixed commitments baseline.",
                                "Retained Badge" to "Total liquid and invested capital preserved in this cycle."
                            ),
                            advice = "A retention rate above 20% indicates healthy financial compounding."
                        )
                    )
                },
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.95f)) {
                    Text(
                        text = if (isDiscreet) "••••" else String.format(Locale.US, "%.1f%%", retentionRate),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        letterSpacing = (-0.6).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CAPITAL RETENTION",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(11.dp))
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isDiscreet) "••••" else if (netSurplus >= 0) "$userProfileCurrency${String.format(Locale.US, "%,.0f", netSurplus)} retained" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", abs(netSurplus))} deficit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netSurplus >= 0) SoftTeal else SoftRed
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1.55f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InteractiveDualGlowWaveCanvas(
                        spendData = spendData,
                        currencySymbol = userProfileCurrency,
                        isDiscreet = isDiscreet,
                        onOpenInfo = {
                            onOpenMetricInfo(
                                ChartMetricInfo(
                                    title = "Dual Inflow & Burn Waves",
                                    subtitle = "Flow Silhouette Breakdown",
                                    formula = "Burn_Spread = Total_Spent - Essential_Fixed",
                                    breakdown = "Displays active spend across ${spendData.size} time segments in $selectedTimeRange.",
                                    visualElements = listOf(
                                        "Blue Ribbon" to "Total outflow volume across the time interval.",
                                        "Rose Ribbon" to "Essential fixed bills volume.",
                                        "Touch Marker" to "Tap to scrub individual day/week expenditure."
                                    ),
                                    advice = "Keep the distance between the two ribbons narrow to prevent discretionary bloat."
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        spendData.forEach { step ->
                            Text(
                                text = step.dayLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. ALLOCATION BREAKDOWN
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Allocation Breakdown",
                            subtitle = "Fixed Obligations vs Discretionary Pacing",
                            formula = "Commitment_Load % = (Fixed_Outflow / Total_Budget) * 100",
                            breakdown = "Fixed AutoPay: $userProfileCurrency${String.format(Locale.US, "%,.0f", fixedOutflow)} | Variable spent: $userProfileCurrency${String.format(Locale.US, "%,.0f", variableOutflow)}.",
                            visualElements = listOf(
                                "Outer Violet Ring" to "Variable discretionary living spend.",
                                "Inner Red Ring" to "Fixed non-negotiable AutoPay obligations.",
                                "Center Percentage" to "Share of budget retained after all expenses."
                            ),
                            advice = "Keeping Fixed AutoPay commitments under 50% guarantees ample safe-to-spend buffer for unpredicted costs."
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Allocation Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Commitments load vs. active safe-to-spend reserve",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clickable {
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "Concentric Allocation Rings",
                                subtitle = "Fixed vs Variable Budget Geometry",
                                formula = "Load = Fixed ÷ (Fixed + Variable)",
                                breakdown = "Fixed: $userProfileCurrency${String.format(Locale.US, "%,.0f", fixedOutflow)} | Variable: $userProfileCurrency${String.format(Locale.US, "%,.0f", variableOutflow)}",
                                visualElements = listOf(
                                    "Outer Ring (Violet)" to "Discretionary variable expenses.",
                                    "Inner Ring (Red)" to "Contractual bills and fixed debt obligations."
                                ),
                                advice = "If the inner red ring is larger than the outer ring, fixed costs dominate your cashflow."
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                ConcentricRingsDonutCanvas(
                    fixedAmount = fixedOutflow,
                    variableAmount = variableOutflow
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isDiscreet) "••%" else "${retentionRate.coerceAtLeast(0.0).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = "Protected",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardWhite,
                    border = BorderStroke(0.7.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SoftRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Fixed AutoPay", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (isDiscreet) "••••" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", fixedOutflow)}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardWhite,
                    border = BorderStroke(0.7.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AccentPurple)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Variable Spend", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (isDiscreet) "••••" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", variableOutflow)}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 3. OUTFLOW VELOCITY (STACKED BARS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Outflow Velocity",
                            subtitle = "Daily & Weekly Burn Rates",
                            formula = "Daily_Burn = (Total Period Spend) / Total Days",
                            breakdown = "Active cycle burn: $userProfileCurrency${String.format(Locale.US, "%,.0f", dailyBurn)}/day across $selectedTimeRange.",
                            visualElements = listOf(
                                "Red Base" to "Essential fixed bill proportion.",
                                "Violet Top" to "Variable discretionary spend proportion."
                            ),
                            advice = "Track spike days to isolate discretionary surges before they exceed planned thresholds."
                        )
                    )
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Outflow Velocity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Daily burn velocity distribution",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showLocalVelocityMenu = true
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = selectedTimeRange.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showLocalVelocityMenu,
                    onDismissRequest = { showLocalVelocityMenu = false }
                ) {
                    TimeRangeFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label) },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTimeRange(filter)
                                showLocalVelocityMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        StackedOutflowBarsCanvas(
            spendData = spendData,
            currencySymbol = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = {
                onOpenMetricInfo(
                    ChartMetricInfo(
                        title = "Outflow Velocity Bars",
                        subtitle = "Segmented Daily/Weekly Stack",
                        formula = "Stack = Essential_Amt + Discretionary_Amt",
                        breakdown = "Shows exact spending composition per interval in $selectedTimeRange.",
                        visualElements = listOf(
                            "Violet Segment" to "Discretionary lifestyle spending.",
                            "Red Segment" to "Essential living and fixed bill payments."
                        ),
                        advice = "Taller bars indicate high-burn days. Aim to level out peaks."
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(26.dp))

        // 4. VELOCITY DENSITY (MICRO STRIP)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Velocity Density",
                            subtitle = "28-Day Transaction Impulse Frequency",
                            formula = "Density = Count(Transactions per Day) over 28 Days",
                            breakdown = "Tracks purchasing friction and how frequently transactions are logged across consecutive days.",
                            visualElements = listOf(
                                "Taller Bars" to "Days with high transaction frequency (>4 purchases).",
                                "Shaded Slate" to "Zero-spend or low-frequency recovery days."
                            ),
                            advice = "Cluster spending into fewer days to cultivate 'no-spend' buffer days and reduce emotional micro-burn."
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Velocity Density",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextDark
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        MicroFrequencyStripCanvas(transactions = allTransactions)

        Spacer(modifier = Modifier.height(28.dp))

        // 5. CUMULATIVE TRAJECTORY (REACTIVE TO VELOCITY RANGE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VelocityRange.entries.forEach { range ->
                val isSelected = selectedVelocityRange == range
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AccentPurple else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectVelocityRange(range)
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val finalTarget = trajectoryData.lastOrNull()?.targetCumulative ?: scaledPeriodBudget
        Text(
            text = if (isDiscreet) "Velocity Target: ••••" else "${selectedVelocityRange.label} Target: $userProfileCurrency${String.format(Locale.US, "%,.0f", finalTarget)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(10.dp))

        DualTrajectoryLineCanvas(
            trajectoryData = trajectoryData,
            currencySymbol = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = {
                onOpenMetricInfo(
                    ChartMetricInfo(
                        title = "Cumulative Burn Trajectory",
                        subtitle = "Pacing vs Target Allowance (${selectedVelocityRange.label})",
                        formula = "Variance = Target_Line - Actual_Cumulative_Curve",
                        breakdown = "Target budget: $userProfileCurrency${String.format(Locale.US, "%,.0f", finalTarget)} over ${selectedVelocityRange.label}.",
                        visualElements = listOf(
                            "Teal Line" to "Linear target pace limit.",
                            "Purple Line" to "Your actual cumulative spending burn-down curve."
                        ),
                        advice = "If the purple line stays below the teal line, you are operating strictly under your planned budget."
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = "Health Indicators",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryHealthIndicatorPill(
                modifier = Modifier.weight(1f),
                title = "Safe Reserve",
                value = if (isDiscreet) "••••" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", safeToSpend)}",
                badgeText = "Remaining",
                accentColor = SoftTeal,
                onClick = {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Safe-to-Spend Reserve",
                            subtitle = "Discretionary Liquid Buffer",
                            formula = "Safe_Spend = Liquid_Cash - Unpaid_Bills - Advance - Living_Buffer",
                            breakdown = "Currently have $userProfileCurrency${String.format(Locale.US, "%,.0f", safeToSpend)} uncommitted liquid cash.",
                            advice = "This amount is 100% guilt-free to spend because all bills, loans, and emergency reserves are already protected."
                        )
                    )
                }
            )
            SummaryHealthIndicatorPill(
                modifier = Modifier.weight(1f),
                title = "AutoPay Load",
                value = "${commitmentLoad.toInt()}%",
                badgeText = "Committed",
                accentColor = SoftRed,
                onClick = {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Commitment Load",
                            subtitle = "Fixed Obligations Burden",
                            formula = "Load % = (Fixed_Commitments / Total_Income) * 100",
                            breakdown = "Fixed bills account for ${commitmentLoad.toInt()}% of your monthly inflow allowance.",
                            advice = "Keeping this figure below 50% protects your finances from insolvency if income experiences a sudden delay."
                        )
                    )
                }
            )
            SummaryHealthIndicatorPill(
                modifier = Modifier.weight(1f),
                title = "Runway Burn",
                value = if (isDiscreet) "••••/d" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", dailyBurn)}/d",
                badgeText = "Pacing",
                accentColor = AccentPurple,
                onClick = {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Daily Runway Burn Velocity",
                            subtitle = "Speed of Capital Outflow",
                            formula = "Burn_Velocity = Total_Outflow / Elapsed_Days",
                            breakdown = "Currently spending $userProfileCurrency${String.format(Locale.US, "%,.0f", dailyBurn)} each day on average.",
                            advice = "Multiplying this number by remaining days in the month gives your forecasted end-of-month cash requirement."
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun InteractiveDualGlowWaveCanvas(
    spendData: List<DailySpendData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var touchIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .pointerInput(spendData) {
                detectTapGestures(
                    onPress = { offset ->
                        val segmentW = size.width / spendData.size.coerceAtLeast(1)
                        touchIndex = (offset.x / segmentW).toInt().coerceIn(0, spendData.lastIndex)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDoubleTap = { onOpenInfo() }
                )
            }
            .pointerInput(spendData) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val segmentW = size.width / spendData.size.coerceAtLeast(1)
                        touchIndex = (offset.x / segmentW).toInt().coerceIn(0, spendData.lastIndex)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDrag = { change, _ ->
                        val segmentW = size.width / spendData.size.coerceAtLeast(1)
                        touchIndex = (change.position.x / segmentW).toInt().coerceIn(0, spendData.lastIndex)
                    },
                    onDragEnd = { touchIndex = null },
                    onDragCancel = { touchIndex = null }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val maxSpend = spendData.maxOfOrNull { it.totalAmount }?.coerceAtLeast(1.0) ?: 1.0
            val hasData = spendData.any { it.totalAmount > 0 }
            val count = spendData.size.coerceAtLeast(2)

            val ptsWave1 = if (hasData) {
                spendData.mapIndexed { idx, d ->
                    val x = (idx.toFloat() / (count - 1)) * w
                    val normY = 1f - (d.totalAmount / maxSpend).toFloat().coerceIn(0.15f, 0.85f)
                    val y = (h * 0.15f) + (normY * h * 0.70f)
                    Offset(x, y)
                }
            } else {
                listOf(
                    Offset(0f, h * 0.82f), Offset(w * 0.25f, h * 0.38f), Offset(w * 0.50f, h * 0.60f),
                    Offset(w * 0.75f, h * 0.40f), Offset(w * 0.90f, h * 0.22f), Offset(w, h * 0.78f)
                )
            }

            val ptsWave2 = if (hasData) {
                spendData.mapIndexed { idx, d ->
                    val x = (idx.toFloat() / (count - 1)) * w
                    val essNorm = 1f - (d.essentialAmount / maxSpend).toFloat().coerceIn(0.10f, 0.90f)
                    val y = (h * 0.20f) + (essNorm * h * 0.65f)
                    Offset(x, y)
                }
            } else {
                listOf(
                    Offset(0f, h * 0.88f), Offset(w * 0.25f, h * 0.80f), Offset(w * 0.50f, h * 0.36f),
                    Offset(w * 0.75f, h * 0.65f), Offset(w * 0.90f, h * 0.78f), Offset(w, h * 0.88f)
                )
            }

            fun buildSpline(pts: List<Offset>): Pair<Path, Path> {
                val stroke = Path().apply {
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
                val fill = Path().apply {
                    addPath(stroke)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                return Pair(stroke, fill)
            }

            val (stroke1, fill1) = buildSpline(ptsWave1)
            val (stroke2, fill2) = buildSpline(ptsWave2)

            drawPath(
                path = fill1,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.55f), Color(0xFF6366F1).copy(alpha = 0.25f), Color.Transparent)
                )
            )
            drawPath(
                path = stroke1,
                brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1))),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            drawPath(
                path = fill2,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF43F5E).copy(alpha = 0.50f), Color(0xFFA855F7).copy(alpha = 0.20f), Color.Transparent)
                )
            )
            drawPath(
                path = stroke2,
                brush = Brush.horizontalGradient(listOf(Color(0xFFF43F5E), Color(0xFFA855F7))),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            touchIndex?.let { idx ->
                val pt = ptsWave1.getOrNull(idx)
                if (pt != null) {
                    drawLine(
                        color = AccentPurple.copy(alpha = 0.6f),
                        start = Offset(pt.x, 0f),
                        end = Offset(pt.x, h),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawCircle(color = AccentPurple, radius = 3.5.dp.toPx(), center = pt)
                }
            }
        }

        touchIndex?.let { idx ->
            val data = spendData.getOrNull(idx)
            if (data != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CardWhite,
                    border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.3f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                ) {
                    Text(
                        text = if (isDiscreet) "${data.dayLabel}: ••••" else "${data.dayLabel}: $currencySymbol${String.format(Locale.US, "%,.0f", data.totalAmount)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoriesAnalyticsTabContent(
    userProfileCurrency: String,
    totalExpenses: Double,
    totalAssets: Double,
    corporateOutlays: Double,
    corporateReimbursements: Double,
    transactions: List<TransactionEntity>,
    allTransactions: List<TransactionEntity>,
    selectedTimeRange: TimeRangeFilter,
    isDiscreet: Boolean,
    onOpenMetricInfo: (ChartMetricInfo) -> Unit
) {
    val isPersonalExpense = remember {
        { tx: TransactionEntity ->
            tx.type == TransactionType.EXPENSE && !tx.category.equals("Work & Professional", ignoreCase = true)
        }
    }

    val categoryExpenses = remember(transactions) {
        transactions
            .filter { isPersonalExpense(it) }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val categorySurges = remember(allTransactions) {
        val now = Calendar.getInstance()
        val curM = now.get(Calendar.MONTH) + 1
        val curY = now.get(Calendar.YEAR)
        now.add(Calendar.MONTH, -1)
        val prevM = now.get(Calendar.MONTH) + 1
        val prevY = now.get(Calendar.YEAR)

        val txCal = Calendar.getInstance()
        val curMap = allTransactions.filter {
            txCal.timeInMillis = it.date
            (txCal.get(Calendar.MONTH) + 1) == curM && txCal.get(Calendar.YEAR) == curY && isPersonalExpense(it)
        }.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }

        val prevMap = allTransactions.filter {
            txCal.timeInMillis = it.date
            (txCal.get(Calendar.MONTH) + 1) == prevM && txCal.get(Calendar.YEAR) == prevY && isPersonalExpense(it)
        }.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }

        curMap.mapNotNull { (cat, curAmt) ->
            val prevAmt = prevMap[cat] ?: 0.0
            if (prevAmt > 0 && curAmt > prevAmt) {
                val growth = (((curAmt - prevAmt) / prevAmt) * 100).toInt()
                if (growth >= 15) cat to growth else null
            } else null
        }.sortedByDescending { it.second }
    }

    val needsCategories = setOf(
        "Utilities & Living Bills", "Everyday Living", "Health & Medical",
        "Family & Home Support", "Debt & Financial Obligations", "Living", "Rent", "Bills"
    )
    val needsSum = remember(transactions) {
        transactions.filter { isPersonalExpense(it) && (it.category in needsCategories || it.linkedFixedBillId != null) }.sumOf { it.amount }
    }
    val wantsSum = remember(transactions, needsSum, totalExpenses) {
        max(0.0, totalExpenses - needsSum)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 140.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Corporate Float Active Banner
        if (corporateOutlays > 0.0 || corporateReimbursements > 0.0) {
            val netFloat = corporateOutlays - corporateReimbursements
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "Corporate Outlays & Claims",
                                subtitle = "Business Travel Float Reconciler",
                                formula = "Net_Float = Work_Expenses_Paid - Claims_Received",
                                breakdown = "Total Outlays: $userProfileCurrency${String.format(Locale.US, "%,.0f", corporateOutlays)} | Company Refunds: $userProfileCurrency${String.format(Locale.US, "%,.0f", corporateReimbursements)}.",
                                visualElements = listOf(
                                    "Pending Claim" to "Money you paid out-of-pocket that the company owes back to you.",
                                    "Advance Held" to "Company capital sitting in your accounts, strictly ring-fenced from your living burn."
                                ),
                                advice = "Corporate expenses are ring-fenced from personal living costs so business travel never distorts your true burn rate."
                            )
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, Color(0xFFE57A28).copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE57A28).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkOutline,
                            contentDescription = null,
                            tint = Color(0xFFE57A28),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Corporate Float Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE57A28).copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "Excluded from Burn",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE57A28),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDiscreet) "••••" else "Outlays: $userProfileCurrency${String.format(Locale.US, "%,.0f", corporateOutlays)} | Settled: $userProfileCurrency${String.format(Locale.US, "%,.0f", corporateReimbursements)}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isDiscreet) "••••" else "${if (netFloat >= 0) "+" else ""}$userProfileCurrency${String.format(Locale.US, "%,.0f", abs(netFloat))}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            color = if (netFloat <= 0) SoftGreen else Color(0xFFE57A28)
                        )
                        Text(
                            text = if (netFloat > 0) "Claim Due" else if (netFloat < 0) "Advance Held" else "Settled",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (netFloat <= 0) SoftGreen else Color(0xFFE57A28)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Spending Matrix Radar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Spending Matrix Radar",
                            subtitle = "Multi-Axis Category Allocation",
                            formula = "Axis_Ratio = (Category_Total / Max_Category_Sum) * 100",
                            breakdown = "Evaluates personal expense density across your top 6 categories in $selectedTimeRange.",
                            visualElements = listOf(
                                "Radial Crests" to "Protruding spikes represent categories absorbing the largest share of capital.",
                                "Concentric Rings" to "Reference thresholds at 33%, 66%, and 100% of maximum spend."
                            ),
                            advice = "A balanced hexagonal shape prevents unmanaged spikes in any single category."
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Spending Matrix",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Personal Lifestyle Outflow Distribution",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Spending Matrix Radar",
                            subtitle = "Multi-Axis Category Allocation",
                            formula = "Radius = (Cat_Spend / Max_Spend) * Max_Radius",
                            breakdown = "Top categories: ${categoryExpenses.take(3).joinToString { "${it.first} ($userProfileCurrency${it.second.toInt()})" }}",
                            visualElements = listOf(
                                "Labeled Vertices" to "Top spending lifestyle categories.",
                                "Violet Web" to "Your realized expenditure footprint."
                            ),
                            advice = "An elongated spike on a single spoke indicates disproportionate outflow."
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            CategoryRadarWebCanvas(
                categoryExpenses = categoryExpenses.take(6)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // 50 / 30 / 20 Cashflow Split Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "50 / 30 / 20 Cashflow Split",
                            subtitle = "Macro Budget Health Model",
                            formula = "Needs (50%) + Wants (30%) + SIP Wealth (20%)",
                            breakdown = "Needs: $userProfileCurrency${String.format(Locale.US, "%,.0f", needsSum)} | Wants: $userProfileCurrency${String.format(Locale.US, "%,.0f", wantsSum)} | Assets: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalAssets)}.",
                            visualElements = listOf(
                                "Red Band" to "Needs (Contractual rent, bills, groceries).",
                                "Violet Band" to "Wants (Dining, leisure, discretionary shopping).",
                                "Teal Band" to "Wealth SIPs (Mutual funds, gold, compounding assets)."
                            ),
                            advice = "Aim to contain essential survival costs within 50% to maximize monthly wealth compounding."
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cashflow Stream Split",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Needs (50%) • Wants (30%) • SIP Assets (20%)",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SymmetricalFunnelRibbonCanvas(
            needsAmount = needsSum,
            wantsAmount = wantsSum,
            assetAmount = totalAssets,
            currency = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = {
                onOpenMetricInfo(
                    ChartMetricInfo(
                        title = "50 / 30 / 20 Ribbon Funnel",
                        subtitle = "Relative Proportion Distribution",
                        formula = "Total = Needs + Wants + Assets",
                        breakdown = "Needs: $userProfileCurrency${String.format(Locale.US, "%,.0f", needsSum)} | Wants: $userProfileCurrency${String.format(Locale.US, "%,.0f", wantsSum)} | Assets: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalAssets)}",
                        visualElements = listOf(
                            "Red Top Band" to "Essential survival commitments.",
                            "Purple Middle Band" to "Variable discretionary living.",
                            "Teal Lower Band" to "Compounding investment assets."
                        ),
                        advice = "Keep essential needs at or below 50% to ensure enough cash is available for investing."
                    )
                )
            }
        )

        if (categorySurges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val top = categorySurges.first()
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "Velocity Surge Analysis",
                                subtitle = "Month-over-Month Category Inflation",
                                formula = "Surge % = ((This_Month - Last_Month) / Last_Month) * 100",
                                breakdown = "${top.first} spiked by +${top.second}% compared to the prior calendar month.",
                                visualElements = listOf(
                                    "Amber Badge" to "Alerts when any category grows by more than 15% in a single cycle."
                                ),
                                advice = "Audit subcategories under ${top.first} to check for one-time spikes versus recurring subscription price hikes."
                            )
                        )
                    },
                shape = RoundedCornerShape(14.dp),
                color = SoftAmber.copy(alpha = 0.12f),
                border = BorderStroke(0.7.dp, SoftAmber.copy(alpha = 0.35f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Month-over-Month Velocity Surge", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                        val top = categorySurges.first()
                        Text("${top.first} increased by +${top.second}% vs last cycle", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Budget Consumption",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (categoryExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No categorized expenses in this cycle", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            categoryExpenses.take(4).forEach { (cat, amount) ->
                val ratio = if (totalExpenses > 0) (amount / totalExpenses).toFloat() else 0f
                Column(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .clickable {
                            onOpenMetricInfo(
                                ChartMetricInfo(
                                    title = "$cat Consumption",
                                    subtitle = "Share of Total Outflow",
                                    formula = "Share % = (Category_Total / Total_Expenses) * 100",
                                    breakdown = "Realized spend of $userProfileCurrency${String.format(Locale.US, "%,.0f", amount)}, absorbing ${(ratio * 100).toInt()}% of total expenses in $selectedTimeRange.",
                                    advice = "Target reducing variable expenses in your top 2 categories to free up cash for emergency reserves."
                                )
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isDiscreet) "•••• (${(ratio * 100).toInt()}%)" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", amount)} (${(ratio * 100).toInt()}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BorderLight.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceIn(0.04f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(SoftTeal, AccentPurple)))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Itemized Category Roster",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(10.dp))

        categoryExpenses.forEach { (cat, amount) ->
            val ratio = if (totalExpenses > 0) (amount / totalExpenses) * 100 else 0.0
            val catTxs = transactions.filter { it.category.equals(cat, ignoreCase = true) && isPersonalExpense(it) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "$cat Audit",
                                subtitle = "Category Breakdown & Sparkline",
                                formula = "Total = Σ Transactions($cat)",
                                breakdown = "Total spent: $userProfileCurrency${String.format(Locale.US, "%,.0f", amount)} across ${catTxs.size} transactions in $selectedTimeRange.",
                                visualElements = listOf(
                                    "Mini Sparkline" to "Visual trajectory of transaction sizes within this category."
                                ),
                                advice = "Review smaller, frequent charges that quietly accumulate into large monthly totals."
                            )
                        )
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f, fill = false)) {
                    Text(
                        text = cat,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${String.format(Locale.US, "%.1f", ratio)}% total outflow", fontSize = 11.sp, color = TextMuted)
                }

                Canvas(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                ) {
                    if (catTxs.size >= 2) {
                        val maxCatTx = catTxs.maxOf { it.amount }.coerceAtLeast(1.0)
                        val p = Path()
                        catTxs.forEachIndexed { i, tx ->
                            val px = (i.toFloat() / (catTxs.size - 1)) * size.width
                            val py = size.height * (1f - (tx.amount / maxCatTx).toFloat().coerceIn(0.1f, 0.9f))
                            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                        }
                        drawPath(p, color = AccentPurple, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
                    } else {
                        val p = Path().apply {
                            moveTo(0f, size.height * 0.75f)
                            lineTo(size.width, size.height * 0.4f)
                        }
                        drawPath(p, color = AccentPurple, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (isDiscreet) "••••" else "-$userProfileCurrency${String.format(Locale.US, "%,.0f", amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = SoftRed
                )
            }
            HorizontalDivider(color = BorderLight.copy(alpha = 0.35f), thickness = 0.7.dp)
        }
    }
}

@Composable
private fun WealthAnalyticsTabContent(
    userProfileCurrency: String,
    vaultMode: String,
    onOpenStrategyInfo: () -> Unit,
    totalInvestments: Double,
    realizableNetWorth: Double,
    monthlyBurnRate: Double,
    accounts: List<AccountBalanceResult>,
    isDiscreet: Boolean,
    onOpenMetricInfo: (ChartMetricInfo) -> Unit
) {
    val totalLiquid = remember(accounts) { accounts.sumOf { it.currentBalance } }
    val runwayMonths = remember(totalLiquid, monthlyBurnRate) {
        if (monthlyBurnRate > 0) (totalLiquid / monthlyBurnRate) else if (totalLiquid > 0) 99.0 else 0.0
    }
    val is3Vault = !vaultMode.equals("SIMPLE", ignoreCase = true)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 140.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Net Capital Trajectory",
                            subtitle = "Liquid Reserves vs. Wealth Assets",
                            formula = "Realizable_Net_Worth = Liquid_Reserves + Active_Investments + Receivables",
                            breakdown = "Liquid Vaults: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalLiquid)} | Invested Portfolio: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalInvestments)} | Realizable Net Worth: $userProfileCurrency${String.format(Locale.US, "%,.0f", realizableNetWorth)}.",
                            visualElements = listOf(
                                "Violet Layer" to "Liquid capital held in banks and cash accounts.",
                                "Teal Layer" to "Compounding market portfolio stock (Mutual funds, gold, SIPs)."
                            ),
                            advice = "Visualizes your liquid defensive buffer alongside appreciating capital."
                        )
                    )
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Net Capital Trajectory",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Liquid Reserves + Invested Portfolio",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenStrategyInfo() },
                shape = RoundedCornerShape(10.dp),
                color = if (is3Vault) AccentPurple.copy(alpha = 0.12f) else CanvasLight,
                border = BorderStroke(0.7.dp, if (is3Vault) AccentPurple else BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (is3Vault) Icons.Default.Layers else Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = if (is3Vault) AccentPurple else TextDark,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (is3Vault) "3-Vault" else "Simple",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (is3Vault) AccentPurple else TextDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LayeredMountainAreaChartCanvas(
            liquidTotal = totalLiquid,
            assetTotal = totalInvestments,
            currencySymbol = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = {
                onOpenMetricInfo(
                    ChartMetricInfo(
                        title = "Net Capital Silhouette",
                        subtitle = "Liquid vs Compounding Balance Sheet",
                        formula = "Net_Worth = Liquid_Cash + Total_Investments",
                        breakdown = "Liquid: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalLiquid)} | Invested: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalInvestments)}",
                        visualElements = listOf(
                            "Violet Silhouette" to "Liquid bank and cash reserves.",
                            "Teal Silhouette" to "Long-term compounding investments."
                        ),
                        advice = "Aim to grow the teal investment silhouette faster than the liquid baseline."
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Capital Distribution",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(14.dp))

        val bankAmount = accounts.filter { !it.accountType.equals("Cash", true) }.sumOf { it.currentBalance }
        val cashAmount = accounts.filter { it.accountType.equals("Cash", true) }.sumOf { it.currentBalance }

        ThreeBubbleAllocationCanvas(
            bankAmount = bankAmount,
            cashAmount = cashAmount,
            assetAmount = totalInvestments,
            currency = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = {
                onOpenMetricInfo(
                    ChartMetricInfo(
                        title = "Capital Allocation Bubbles",
                        subtitle = "Three-Tier Wealth Balance",
                        formula = "Total = Bank_Reserves + Cash_Buffer + Invested_Assets",
                        breakdown = "Banks: $userProfileCurrency${String.format(Locale.US, "%,.0f", bankAmount)} | Cash: $userProfileCurrency${String.format(Locale.US, "%,.0f", cashAmount)} | Portfolio: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalInvestments)}.",
                        visualElements = listOf(
                            "Purple Bubble" to "Bank balances held in primary and commitments accounts.",
                            "Teal Bubble" to "Long-term investment assets and mutual funds.",
                            "Green Bubble" to "Physical cash and petty expense buffers."
                        ),
                        advice = "Maintain small, focused cash reserves while routing excess bank liquidity to the portfolio bubble."
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Emergency Buffer Runway",
                            subtitle = "Financial Survival Duration",
                            formula = "Runway_Months = Liquid_Vaults / max(1.0, Average_Monthly_Spend)",
                            breakdown = "Liquid Reserves: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalLiquid)} | Monthly Burn: $userProfileCurrency${String.format(Locale.US, "%,.0f", monthlyBurnRate)}/mo.",
                            visualElements = listOf(
                                "Runway Counter" to "Months your liquid reserves can fund full living expenses without any new income."
                            ),
                            advice = "Maintaining a 6-month buffer covers unexpected emergencies without forcing investment liquidations."
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Emergency Buffer Runway",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isDiscreet) "•• Months" else "${String.format(Locale.US, "%.1f", runwayMonths)} Months",
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    color = SoftTeal
                )
                Text("Living expenses secured in vaults", fontSize = 11.5.sp, color = TextMuted)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SoftTeal.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (runwayMonths >= 6) "Healthy Cushion" else "Building Buffer",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = SoftTeal
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = if (is3Vault) "Strategic Vaults Status" else "Vaults Liquidity Status",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(10.dp))

        accounts.forEach { acc ->
            val spendableSurplus = (acc.currentBalance - acc.minBalance).coerceAtLeast(0.0)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "${acc.accountName} Vault Audit",
                                subtitle = "${acc.accountType} Tier Account Details",
                                formula = "Spendable_Surplus = Current_Balance - Minimum_Account_Balance",
                                breakdown = "Current Balance: $userProfileCurrency${String.format(Locale.US, "%,.0f", acc.currentBalance)} | MAB Buffer: $userProfileCurrency${String.format(Locale.US, "%,.0f", acc.minBalance)} | Free Surplus: $userProfileCurrency${String.format(Locale.US, "%,.0f", spendableSurplus)}.",
                                visualElements = listOf(
                                    "MAB Flag" to "Required minimum balance protected against overdraft charges."
                                ),
                                advice = "Only spendable surplus is counted in Safe-to-Spend algorithms."
                            )
                        )
                    }
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (acc.accountType.equals("Cash", true)) SoftTeal.copy(alpha = 0.12f) else AccentPurple.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (acc.accountType.equals("Cash", true)) Icons.Default.Payments else Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (acc.accountType.equals("Cash", true)) SoftTeal else AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(acc.accountName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (is3Vault) "${acc.accountType} Tier" else "${acc.accountType} Vault", fontSize = 11.sp, color = TextMuted)
                            if (acc.minBalance > 0.0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("• MAB: $userProfileCurrency${String.format(Locale.US, "%,.0f", acc.minBalance)}", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                            }
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isDiscreet) "••••" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", acc.currentBalance)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.5.sp,
                        color = TextDark
                    )
                    if (acc.minBalance > 0.0) {
                        Text(
                            text = if (isDiscreet) "Surplus: ••••" else "Surplus: $userProfileCurrency${String.format(Locale.US, "%,.0f", spendableSurplus)}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftTeal
                        )
                    } else {
                        Text(
                            text = if (isDiscreet) "Base: ••••" else "Base: $userProfileCurrency${acc.startingBalance.toInt()}",
                            fontSize = 10.5.sp,
                            color = TextMuted
                        )
                    }
                }
            }
            HorizontalDivider(color = BorderLight.copy(alpha = 0.35f), thickness = 0.7.dp)
        }
    }
}

@Composable
private fun ConcentricRingsDonutCanvas(
    fixedAmount: Double,
    variableAmount: Double
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 9.dp.toPx()
        val diameter = size.minDimension

        val outerRadius = (diameter / 2f) - (strokeWidth / 2f)
        drawCircle(
            color = AccentPurple.copy(alpha = 0.12f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        val total = (fixedAmount + variableAmount).coerceAtLeast(1.0)
        val variableSweep = ((variableAmount / total) * 360f).toFloat().coerceIn(10f, 340f)
        drawArc(
            color = AccentPurple,
            startAngle = -90f,
            sweepAngle = variableSweep,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        val innerRadius = outerRadius - strokeWidth - 10.dp.toPx()
        drawCircle(
            color = SoftRed.copy(alpha = 0.12f),
            radius = innerRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        val fixedSweep = ((fixedAmount / total) * 360f).toFloat().coerceIn(10f, 340f)
        drawArc(
            color = SoftRed,
            startAngle = 40f,
            sweepAngle = fixedSweep,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun StackedOutflowBarsCanvas(
    spendData: List<DailySpendData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(spendData) {
                    detectTapGestures(
                        onPress = { offset ->
                            val count = spendData.size.coerceAtLeast(1)
                            val barWidth = 14.dp.toPx()
                            val spacing = (size.width - (count * barWidth)) / max(1, count - 1)
                            val idx = (offset.x / (barWidth + spacing)).toInt().coerceIn(0, spendData.lastIndex)
                            selectedBarIndex = if (selectedBarIndex == idx) null else idx
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDoubleTap = { onOpenInfo() }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = spendData.size
                val availableWidth = size.width
                val barWidth = 14.dp.toPx()
                val totalBarsWidth = count * barWidth
                val spacing = (availableWidth - totalBarsWidth) / max(1, count - 1)
                val cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())

                val maxSpend = spendData.maxOfOrNull { it.totalAmount }?.coerceAtLeast(1.0) ?: 1.0

                for (i in 0 until count) {
                    val data = spendData[i]
                    val x = i * (barWidth + spacing)

                    val heightRatio = if (data.totalAmount > 0) {
                        (data.totalAmount / maxSpend).toFloat().coerceIn(0.12f, 0.95f)
                    } else 0.06f

                    val totalH = size.height * heightRatio
                    val essentialRatio = if (data.totalAmount > 0) (data.essentialAmount / data.totalAmount).toFloat() else 0.5f
                    val redH = totalH * essentialRatio
                    val barTop = size.height - totalH

                    val isSelected = selectedBarIndex == i

                    drawRoundRect(
                        color = if (data.totalAmount > 0) {
                            if (isSelected) AccentPurple.copy(alpha = 0.8f) else AccentPurple
                        } else BorderLight.copy(alpha = 0.4f),
                        topLeft = Offset(x, barTop),
                        size = Size(barWidth, totalH),
                        cornerRadius = cornerRadius
                    )

                    if (redH > 0f && data.totalAmount > 0) {
                        drawRoundRect(
                            color = SoftRed,
                            topLeft = Offset(x, size.height - redH),
                            size = Size(barWidth, redH),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }

            selectedBarIndex?.let { idx ->
                val data = spendData.getOrNull(idx)
                if (data != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CardWhite,
                        border = BorderStroke(0.6.dp, BorderLight),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (isDiscreet) "${data.dayLabel}: ••••" else "${data.dayLabel}: $currencySymbol${String.format(Locale.US, "%,.0f", data.totalAmount)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            spendData.forEach { data ->
                Box(
                    modifier = Modifier.width(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = data.dayLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun MicroFrequencyStripCanvas(transactions: List<TransactionEntity>) {
    val dayFrequencies = remember(transactions) {
        val buckets = IntArray(28) { 0 }
        val now = System.currentTimeMillis()
        val oneDayMillis = 86400000L
        for (tx in transactions) {
            val diffDays = ((now - tx.date) / oneDayMillis).toInt().coerceIn(0, 27)
            buckets[27 - diffDays]++
        }
        buckets
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val barCount = 28
        val spacing = size.width / barCount
        val barW = 3.dp.toPx()
        val maxCount = dayFrequencies.maxOrNull()?.coerceAtLeast(1) ?: 1

        for (i in 0 until barCount) {
            val count = dayFrequencies[i]
            val hRatio = if (count > 0) (count.toFloat() / maxCount).coerceIn(0.25f, 1f) else 0.12f
            val barH = size.height * hRatio
            drawRoundRect(
                color = if (count > 0) {
                    if (i % 3 == 0) SoftTeal else AccentPurple.copy(alpha = 0.85f)
                } else BorderLight.copy(alpha = 0.45f),
                topLeft = Offset(i * spacing, size.height - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun DualTrajectoryLineCanvas(
    trajectoryData: List<TrajectoryPointData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    val labels = trajectoryData.map { it.stepLabel }
    val maxTrajectoryValue = trajectoryData.maxOfOrNull { max(it.actualCumulative, it.targetCumulative) }?.coerceAtLeast(100.0) ?: 100.0
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .height(110.dp)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    if (maxTrajectoryValue >= 1000) "${(maxTrajectoryValue / 1000).toInt()}k" else "${maxTrajectoryValue.toInt()}",
                    if (maxTrajectoryValue >= 1000) "${(maxTrajectoryValue * 0.75 / 1000).toInt()}k" else "${(maxTrajectoryValue * 0.75).toInt()}",
                    if (maxTrajectoryValue >= 1000) "${(maxTrajectoryValue * 0.50 / 1000).toInt()}k" else "${(maxTrajectoryValue * 0.50).toInt()}",
                    if (maxTrajectoryValue >= 1000) "${(maxTrajectoryValue * 0.25 / 1000).toInt()}k" else "${(maxTrajectoryValue * 0.25).toInt()}"
                ).forEach { label ->
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .pointerInput(trajectoryData) {
                        detectTapGestures(
                            onPress = { offset ->
                                val count = trajectoryData.size.coerceAtLeast(2)
                                val idx = ((offset.x / size.width) * (count - 1)).toInt().coerceIn(0, trajectoryData.lastIndex)
                                selectedIndex = if (selectedIndex == idx) null else idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDoubleTap = { onOpenInfo() }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    for (step in 1..3) {
                        val yLine = h * (step / 4f)
                        drawLine(
                            color = BorderLight.copy(alpha = 0.5f),
                            start = Offset(0f, yLine),
                            end = Offset(w, yLine),
                            strokeWidth = 0.8.dp.toPx()
                        )
                    }

                    val count = trajectoryData.size.coerceAtLeast(2)
                    val targetPoints = (0 until count).map { i ->
                        val x = (i.toFloat() / (count - 1).coerceAtLeast(1)) * w
                        val targetVal = trajectoryData.getOrNull(i)?.targetCumulative ?: 0.0
                        val y = h * (1f - (targetVal / maxTrajectoryValue).toFloat().coerceIn(0.08f, 0.92f))
                        Offset(x, y)
                    }

                    val targetPath = Path().apply {
                        moveTo(targetPoints[0].x, targetPoints[0].y)
                        for (i in 0 until targetPoints.size - 1) {
                            val p0 = targetPoints[i]
                            val p1 = targetPoints[i + 1]
                            val cx = (p0.x + p1.x) / 2
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    drawPath(
                        path = targetPath,
                        color = SoftTeal,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val actualPoints = (0 until count).map { i ->
                        val actualVal = trajectoryData.getOrNull(i)?.actualCumulative ?: 0.0
                        val y = h * (1f - (actualVal / maxTrajectoryValue).toFloat().coerceIn(0.08f, 0.92f))
                        Offset(x = (i.toFloat() / (count - 1).coerceAtLeast(1)) * w, y = y)
                    }

                    val actualPath = Path().apply {
                        moveTo(actualPoints[0].x, actualPoints[0].y)
                        for (i in 0 until actualPoints.size - 1) {
                            val p0 = actualPoints[i]
                            val p1 = actualPoints[i + 1]
                            val cx = (p0.x + p1.x) / 2
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    drawPath(
                        path = actualPath,
                        color = AccentPurple,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    if (actualPoints.size >= 2) {
                        val lastPt = actualPoints.last()
                        drawCircle(color = CardWhite, radius = 4.5.dp.toPx(), center = lastPt)
                        drawCircle(color = AccentPurple, radius = 3.dp.toPx(), center = lastPt)
                    }
                }

                selectedIndex?.let { idx ->
                    val data = trajectoryData.getOrNull(idx)
                    if (data != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CardWhite,
                            border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.3f)),
                            shadowElevation = 3.dp,
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Text(
                                text = if (isDiscreet) "${data.stepLabel}: ••••" else "${data.stepLabel}: $currencySymbol${String.format(Locale.US, "%,.0f", data.actualCumulative)} (Target: $currencySymbol${String.format(Locale.US, "%,.0f", data.targetCumulative)})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { day ->
                Text(
                    text = day,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }
    }
}

@Composable
private fun CategoryRadarWebCanvas(
    categoryExpenses: List<Pair<String, Double>>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val numAxes = 6
        val c = center
        val maxR = size.minDimension * 0.40f

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
            drawPath(ringPath, color = BorderLight.copy(alpha = 0.5f), style = Stroke(width = 0.8.dp.toPx()))
        }

        for (i in 0 until numAxes) {
            val angle = (i * 2 * Math.PI / numAxes) - Math.PI / 2
            val x = c.x + (maxR * cos(angle)).toFloat()
            val y = c.y + (maxR * sin(angle)).toFloat()
            drawLine(color = BorderLight.copy(alpha = 0.6f), start = c, end = Offset(x, y), strokeWidth = 0.8.dp.toPx())
        }

        val maxAmount = categoryExpenses.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
        val polyPath = Path()
        for (i in 0 until numAxes) {
            val amt = categoryExpenses.getOrNull(i)?.second ?: 0.0
            val ratio = if (categoryExpenses.isNotEmpty()) (amt / maxAmount).toFloat().coerceIn(0.15f, 0.95f) else 0.2f
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
private fun SymmetricalFunnelRibbonCanvas(
    needsAmount: Double,
    wantsAmount: Double,
    assetAmount: Double,
    currency: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    val total = (needsAmount + wantsAmount + assetAmount).coerceAtLeast(1.0)
    val needsPct = ((needsAmount / total) * 100).toInt()
    val wantsPct = ((wantsAmount / total) * 100).toInt()
    val assetPct = ((assetAmount / total) * 100).toInt()

    Column(modifier = Modifier.fillMaxWidth().clickable { onOpenInfo() }) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val w = size.width
            val h = size.height

            val needsRatio = (needsAmount / total).toFloat().coerceIn(0.12f, 0.70f)
            val wantsRatio = (wantsAmount / total).toFloat().coerceIn(0.12f, 0.70f)

            val band1Bottom = h * needsRatio
            val band2Bottom = (h * (needsRatio + wantsRatio)).coerceAtMost(h * 0.88f)

            val path1 = Path().apply {
                moveTo(0f, 0f)
                cubicTo(w * 0.35f, 0f, w * 0.65f, 0f, w, 0f)
                lineTo(w, band1Bottom)
                cubicTo(w * 0.65f, band1Bottom, w * 0.35f, h * 0.35f, 0f, h * 0.35f)
                close()
            }
            drawPath(path1, color = SoftRed.copy(alpha = 0.85f))

            val path2 = Path().apply {
                moveTo(0f, h * 0.35f)
                cubicTo(w * 0.35f, h * 0.35f, w * 0.65f, band1Bottom, w, band1Bottom)
                lineTo(w, band2Bottom)
                cubicTo(w * 0.65f, band2Bottom, w * 0.35f, h * 0.65f, 0f, h * 0.65f)
                close()
            }
            drawPath(path2, color = AccentPurple.copy(alpha = 0.85f))

            val path3 = Path().apply {
                moveTo(0f, h * 0.65f)
                cubicTo(w * 0.35f, h * 0.65f, w * 0.65f, band2Bottom, w, band2Bottom)
                lineTo(w, h)
                cubicTo(w * 0.65f, h, w * 0.35f, h, 0f, h)
                close()
            }
            drawPath(path3, color = SoftTeal.copy(alpha = 0.85f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftRed))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Needs $needsPct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentPurple))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Wants $wantsPct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Assets $assetPct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
    }
}

@Composable
private fun LayeredMountainAreaChartCanvas(
    liquidTotal: Double,
    assetTotal: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().clickable { onOpenInfo() }) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            val w = size.width
            val h = size.height

            val totalWealth = (liquidTotal + assetTotal).coerceAtLeast(1.0)
            val liquidShare = (liquidTotal / totalWealth).toFloat().coerceIn(0.2f, 0.8f)

            val p1 = Path().apply {
                moveTo(0f, h * (1f - (liquidShare * 0.6f + 0.1f)))
                cubicTo(w * 0.3f, h * (1f - (liquidShare * 0.7f + 0.05f)), w * 0.6f, h * (1f - (liquidShare * 0.85f)), w, h * (1f - liquidShare))
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                p1,
                brush = Brush.verticalGradient(listOf(AccentPurple.copy(alpha = 0.45f), AccentPurple.copy(alpha = 0.05f)))
            )

            val p2 = Path().apply {
                moveTo(0f, h * 0.85f)
                cubicTo(w * 0.35f, h * 0.70f, w * 0.7f, h * 0.60f, w, h * (1f - (liquidShare * 0.5f)))
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                p2,
                brush = Brush.verticalGradient(listOf(SoftTeal.copy(alpha = 0.55f), SoftTeal.copy(alpha = 0.05f)))
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentPurple))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Liquid Cash", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Investments", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ThreeBubbleAllocationCanvas(
    bankAmount: Double,
    cashAmount: Double,
    assetAmount: Double,
    currency: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable { onOpenInfo() },
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .clip(CircleShape)
                .background(AccentPurple.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Banks", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                val label = if (isDiscreet) "••••" else if (bankAmount >= 1000) "$currency${(bankAmount / 1000).toInt()}k" else "$currency${bankAmount.toInt()}"
                Text(label, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .size(74.dp)
                .clip(CircleShape)
                .background(SoftTeal.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Portfolio", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                val label = if (isDiscreet) "••••" else if (assetAmount >= 1000) "$currency${(assetAmount / 1000).toInt()}k" else "$currency${assetAmount.toInt()}"
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(SoftGreen.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Cash", fontSize = 9.sp, color = Color.White.copy(alpha = 0.8f))
                val label = if (isDiscreet) "••••" else if (cashAmount >= 1000) "$currency${(cashAmount / 1000).toInt()}k" else "$currency${cashAmount.toInt()}"
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
