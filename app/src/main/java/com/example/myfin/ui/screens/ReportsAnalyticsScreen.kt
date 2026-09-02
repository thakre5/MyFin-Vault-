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
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

enum class TimeRangeFilter(val label: String) {
    THIS_WEEK("This Week"),
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month")
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

data class ChartMetricInfo(
    val title: String,
    val subtitle: String,
    val formula: String,
    val breakdown: String,
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
    val userProfile by viewModel.userProfile.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()
    val pageTitles = remember { listOf("Summary", "Categories", "Wealth") }

    var selectedTimeRange by remember { mutableStateOf(TimeRangeFilter.THIS_WEEK) }
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

    val allTransactions = remember(uiState.groupedTransactions) {
        uiState.groupedTransactions.values.flatten()
    }

    val activeAccounts = remember(uiState.activeAccounts, uiState.accounts) {
        uiState.activeAccounts.ifEmpty { uiState.accounts.filter { !it.isArchived } }
    }

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
                val currentMonth = calendar.get(Calendar.MONTH)
                val currentYear = calendar.get(Calendar.YEAR)
                allTransactions.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    txCal.get(Calendar.MONTH) == currentMonth && txCal.get(Calendar.YEAR) == currentYear
                }
            }
            TimeRangeFilter.LAST_MONTH -> {
                val targetCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
                val lastMonth = targetCal.get(Calendar.MONTH)
                val targetYear = targetCal.get(Calendar.YEAR)
                allTransactions.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    txCal.get(Calendar.MONTH) == lastMonth && txCal.get(Calendar.YEAR) == targetYear
                }
            }
        }
    }

    val totalIncome = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }
    val fixedOutflow = remember(uiState.fixedBills, selectedTimeRange) {
        val totalFixed = uiState.fixedBills.filter { it.type == TransactionType.EXPENSE || it.type == TransactionType.ASSET }.sumOf { it.amount }
        when (selectedTimeRange) {
            TimeRangeFilter.THIS_WEEK -> totalFixed * (7.0 / 30.0)
            TimeRangeFilter.THIS_MONTH, TimeRangeFilter.LAST_MONTH -> totalFixed
        }
    }
    val variableOutflow = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE && it.linkedFixedBillId == null }.sumOf { it.amount }
    }
    val totalExpenses = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val totalAssets = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == TransactionType.ASSET }.sumOf { it.amount }
    }
    val netSurplus = totalIncome - totalExpenses - totalAssets

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

                val weekTxs = filteredTransactions.filter { it.date in startOfWeek until endOfWeek && it.type == TransactionType.EXPENSE }
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

                for (tx in filteredTransactions.filter { it.type == TransactionType.EXPENSE }) {
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
            // 1. PINNED TOP BAR (WITH DOWNWARD DISSOLVE SHELF)
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

                // Smooth Dissolve Shelf Placed Below Top Header
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

            // 2. FULL-SCREEN HORIZONTAL PAGER (SWIPEABLE ANALYTICS TABS)
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
                            totalIncome = totalIncome,
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
                            allTransactions = allTransactions,
                            isDiscreet = isDiscreetMode,
                            onOpenMetricInfo = { activeChartMetricInfo = it }
                        )
                    }
                    1 -> {
                        CategoriesAnalyticsTabContent(
                            userProfileCurrency = userProfile.currencySymbol,
                            totalExpenses = totalExpenses,
                            totalAssets = totalAssets,
                            transactions = filteredTransactions,
                            allTransactions = allTransactions,
                            isDiscreet = isDiscreetMode,
                            onOpenMetricInfo = { activeChartMetricInfo = it }
                        )
                    }
                    2 -> {
                        WealthAnalyticsTabContent(
                            userProfileCurrency = userProfile.currencySymbol,
                            vaultMode = userProfile.vaultMode,
                            onOpenStrategyInfo = { showStrategyInfoSheet = true },
                            totalAssets = totalAssets,
                            totalExpenses = totalExpenses,
                            accounts = activeAccounts,
                            transactions = allTransactions,
                            isDiscreet = isDiscreetMode,
                            onOpenMetricInfo = { activeChartMetricInfo = it }
                        )
                    }
                }
            }
        }

        // 3. BOTTOM GRADIENT SCRIM (DISSOLVES CONTENT BEFORE DOCK)
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

        // 4. FLOATING PAGER INDICATOR PILL (ANCHORED LEFT ABOVE DOCK)
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

        // 5. STANDARDIZED FLOATING BOTTOM NAVIGATION DOCK WITH FAB
        AppBottomDock(
            currentSelection = NavigationTarget.REPORTS_ANALYTICS,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.MONTHLY_VIEW -> onNavigateToDashboard()
                    NavigationTarget.BUDGET_PLANNER -> onNavigateToPlanner()
                    NavigationTarget.VAULT_ACCOUNTS -> onNavigateToVaults()
                    NavigationTarget.DATA_SET -> onNavigateToTaxonomy()
                    NavigationTarget.REPORTS_ANALYTICS -> { /* Active */ }
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

        // Dedicated Bottom Information Sheet for Graph Titles
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
                        Column {
                            Text(info.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                            Text(info.subtitle, fontSize = 12.sp, color = TextMuted)
                        }
                        IconButton(onClick = { activeChartMetricInfo = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            fontSize = 12.sp,
                            color = AccentPurple,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Current Contribution", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(info.breakdown, fontSize = 12.5.sp, color = TextDark, lineHeight = 17.sp)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Optimization Insight", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(info.advice, fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)
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
    accentColor: Color
) {
    Surface(
        modifier = modifier,
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
    allTransactions: List<TransactionEntity>,
    isDiscreet: Boolean,
    onOpenMetricInfo: (ChartMetricInfo) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showLocalVelocityMenu by remember { mutableStateOf(false) }

    val totalOutflow = fixedOutflow + variableOutflow
    val totalPeriodExpenses = spendData.sumOf { it.totalAmount }
    val dailyBurn = if (selectedTimeRange == TimeRangeFilter.THIS_WEEK) (totalPeriodExpenses / 7.0) else (totalPeriodExpenses / 30.0)
    val totalBudget = if (plannedBudget > 0) plannedBudget else (totalIncome.takeIf { it > 0 } ?: (totalOutflow * 1.25).coerceAtLeast(1.0))
    val retentionRate = if (totalIncome > 0) ((netSurplus / totalIncome) * 100).coerceIn(0.0, 100.0) else 0.0
    val commitmentLoad = if (totalBudget > 0) ((fixedOutflow / totalBudget) * 100).coerceIn(0.0, 100.0) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 4.dp, bottom = 140.dp)
    ) {
        // Dual-Wave Minimalist Hero Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Capital Retention",
                            subtitle = "Net Saved vs Inflow Rate",
                            formula = "Retention_% = ((I_actual - E_actual - A_actual) / I_actual) * 100",
                            breakdown = "Current realized Inflow: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalIncome)} | Net Retained: $userProfileCurrency${String.format(Locale.US, "%,.0f", netSurplus)} (${String.format(Locale.US, "%.1f", retentionRate)}%).",
                            advice = "Higher retention builds your emergency buffer and compounds investment capacity faster."
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
                // Left Metric Column
                Column(modifier = Modifier.weight(0.95f)) {
                    Text(
                        text = if (isDiscreet) "••••" else String.format(Locale.US, "%.1f", retentionRate),
                        fontSize = 32.sp,
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
                        text = if (isDiscreet) "•••• retained" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", netSurplus)} retained",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netSurplus >= 0) SoftTeal else SoftRed
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Interactive Dual Glow Wave Graph
                Column(
                    modifier = Modifier.weight(1.55f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InteractiveDualGlowWaveCanvas(
                        spendData = spendData,
                        currencySymbol = userProfileCurrency,
                        isDiscreet = isDiscreet,
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
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Allocation Breakdown Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Allocation Breakdown",
                            subtitle = "Fixed Obligations vs Discretionary Pacing",
                            formula = "Commitment_Load_% = (C_fixed / Planned_Budget) * 100",
                            breakdown = "Fixed AutoPay commitments: $userProfileCurrency${String.format(Locale.US, "%,.0f", fixedOutflow)} | Variable spent: $userProfileCurrency${String.format(Locale.US, "%,.0f", variableOutflow)}.",
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
                modifier = Modifier.size(148.dp),
                contentAlignment = Alignment.Center
            ) {
                ConcentricRingsDonutCanvas(
                    fixedAmount = fixedOutflow,
                    variableAmount = variableOutflow
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isDiscreet) "••%" else "${retentionRate.toInt()}%",
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

        // Outflow Velocity Header
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
                            formula = "Daily_Burn = (Σ Period_Outflow) / Total_Days",
                            breakdown = "Active cycle burn: $userProfileCurrency${String.format(Locale.US, "%,.0f", dailyBurn)}/day across $selectedTimeRange.",
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
            isDiscreet = isDiscreet
        )

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = "Velocity Density",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(10.dp))
        MicroFrequencyStripCanvas(transactions = allTransactions)

        Spacer(modifier = Modifier.height(28.dp))

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

        Text(
            text = if (isDiscreet) "Budget Target: ••••" else "Budget Target $userProfileCurrency${String.format(Locale.US, "%,.0f", plannedBudget)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(10.dp))

        DualTrajectoryLineCanvas(
            spendData = spendData,
            plannedBudget = plannedBudget,
            currencySymbol = userProfileCurrency,
            isDiscreet = isDiscreet
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
                accentColor = SoftTeal
            )
            SummaryHealthIndicatorPill(
                modifier = Modifier.weight(1f),
                title = "AutoPay Load",
                value = "${commitmentLoad.toInt()}%",
                badgeText = "Committed",
                accentColor = SoftRed
            )
            SummaryHealthIndicatorPill(
                modifier = Modifier.weight(1f),
                title = "Runway Burn",
                value = if (isDiscreet) "••••/d" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", dailyBurn)}/d",
                badgeText = "Pacing",
                accentColor = AccentPurple
            )
        }
    }
}

@Composable
private fun InteractiveDualGlowWaveCanvas(
    spendData: List<DailySpendData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    modifier: Modifier = Modifier
) {
    var touchIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .pointerInput(spendData) {
                detectTapGestures { offset ->
                    val segmentW = size.width / spendData.size.coerceAtLeast(1)
                    val idx = (offset.x / segmentW).toInt().coerceIn(0, spendData.lastIndex)
                    touchIndex = if (touchIndex == idx) null else idx
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
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
                    Offset(0f, h * 0.82f),
                    Offset(w * 0.25f, h * 0.38f),
                    Offset(w * 0.50f, h * 0.60f),
                    Offset(w * 0.75f, h * 0.40f),
                    Offset(w * 0.90f, h * 0.22f),
                    Offset(w, h * 0.78f)
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
                    Offset(0f, h * 0.88f),
                    Offset(w * 0.25f, h * 0.80f),
                    Offset(w * 0.50f, h * 0.36f),
                    Offset(w * 0.75f, h * 0.65f),
                    Offset(w * 0.90f, h * 0.78f),
                    Offset(w, h * 0.88f)
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
                    colors = listOf(
                        Color(0xFF38BDF8).copy(alpha = 0.55f),
                        Color(0xFF6366F1).copy(alpha = 0.25f),
                        Color.Transparent
                    )
                )
            )
            drawPath(
                path = stroke1,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF6366F1))
                ),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            drawPath(
                path = fill2,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF43F5E).copy(alpha = 0.50f),
                        Color(0xFFA855F7).copy(alpha = 0.20f),
                        Color.Transparent
                    )
                )
            )
            drawPath(
                path = stroke2,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFFF43F5E), Color(0xFFA855F7))
                ),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            // Touch Marker
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

        // Live Tooltip Overlay
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
    transactions: List<TransactionEntity>,
    allTransactions: List<TransactionEntity>,
    isDiscreet: Boolean,
    onOpenMetricInfo: (ChartMetricInfo) -> Unit
) {
    val categoryExpenses = remember(transactions) {
        transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Month-over-Month Velocity Surge Drift Detection
    val categorySurges = remember(allTransactions) {
        val now = Calendar.getInstance()
        val curM = now.get(Calendar.MONTH)
        val curY = now.get(Calendar.YEAR)
        now.add(Calendar.MONTH, -1)
        val prevM = now.get(Calendar.MONTH)
        val prevY = now.get(Calendar.YEAR)

        val txCal = Calendar.getInstance()
        val curMap = allTransactions.filter {
            txCal.timeInMillis = it.date
            txCal.get(Calendar.MONTH) == curM && txCal.get(Calendar.YEAR) == curY && it.type == TransactionType.EXPENSE
        }.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }

        val prevMap = allTransactions.filter {
            txCal.timeInMillis = it.date
            txCal.get(Calendar.MONTH) == prevM && txCal.get(Calendar.YEAR) == prevY && it.type == TransactionType.EXPENSE
        }.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }

        curMap.mapNotNull { (cat, curAmt) ->
            val prevAmt = prevMap[cat] ?: 0.0
            if (prevAmt > 0 && curAmt > prevAmt) {
                val growth = (((curAmt - prevAmt) / prevAmt) * 100).toInt()
                if (growth >= 15) cat to growth else null
            } else null
        }.sortedByDescending { it.second }
    }

    val needsCategories = setOf("Utilities & Living Bills", "Everyday Living", "Health & Medical", "Debt & Financial Obligations", "Living", "Rent", "Bills")
    val needsSum = remember(transactions) {
        transactions.filter { it.type == TransactionType.EXPENSE && (it.category in needsCategories || it.linkedFixedBillId != null) }.sumOf { it.amount }
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

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Spending Matrix Radar",
                            subtitle = "Multi-Axis Category Allocation",
                            formula = "Axis_Ratio = (Category_Total / Max_Category_Sum) * 100",
                            breakdown = "Evaluates expense density spread across top categories in the active timeframe.",
                            advice = "A balanced hexagonal shape prevents over-reliance or unmanaged spikes in any single category."
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
                    text = "Target Benchmark vs. Actual Outflow",
                    fontSize = 11.5.sp,
                    color = TextMuted
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
            CategoryRadarWebCanvas(
                categorySums = categoryExpenses.map { it.second }
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

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
            assetAmount = totalAssets
        )

        // Month-over-Month Velocity Surge Banner
        if (categorySurges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
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
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
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
                                .fillMaxWidth(ratio.coerceIn(0.05f, 1f))
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
            val catTxs = transactions.filter { it.category.equals(cat, ignoreCase = true) && it.type == TransactionType.EXPENSE }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(cat, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
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
    totalAssets: Double,
    totalExpenses: Double,
    accounts: List<AccountBalanceResult>,
    transactions: List<TransactionEntity>,
    isDiscreet: Boolean,
    onOpenMetricInfo: (ChartMetricInfo) -> Unit
) {
    val totalLiquid = remember(accounts) { accounts.sumOf { it.currentBalance } }
    val currentMonthExpenses = remember(transactions) {
        val cal = Calendar.getInstance()
        val curM = cal.get(Calendar.MONTH)
        val curY = cal.get(Calendar.YEAR)
        val txCal = Calendar.getInstance()
        transactions.filter {
            txCal.timeInMillis = it.date
            txCal.get(Calendar.MONTH) == curM && txCal.get(Calendar.YEAR) == curY && it.type == TransactionType.EXPENSE
        }.sumOf { it.amount }
    }
    val monthlyBurnRate = if (currentMonthExpenses > 0) currentMonthExpenses else totalExpenses.coerceAtLeast(1.0)
    val runwayMonths = (totalLiquid / monthlyBurnRate)
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
                            formula = "Net_Worth = Total_Liquid_Vaults + Total_SIP_Assets",
                            breakdown = "Liquid Vaults: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalLiquid)} | Invested Assets: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalAssets)}.",
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
                    text = "Liquid Reserves + SIP Assets Accumulation",
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
            assetTotal = totalAssets
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Capital Distribution",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(14.dp))
        ThreeBubbleAllocationCanvas(
            bankAmount = accounts.filter { !it.accountType.equals("Cash", true) }.sumOf { it.currentBalance },
            cashAmount = accounts.filter { it.accountType.equals("Cash", true) }.sumOf { it.currentBalance },
            assetAmount = totalAssets,
            currency = userProfileCurrency
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
                            formula = "Runway_Months = Liquid_Vaults / max(1.0, Monthly_Burn_Rate)",
                            breakdown = "Liquid Reserves: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalLiquid)} | Monthly Burn: $userProfileCurrency${String.format(Locale.US, "%,.0f", monthlyBurnRate)}.",
                            advice = "Maintaining a 6-month buffer covers unexpected emergencies without forcing liquidations."
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
                    text = "${String.format(Locale.US, "%.1f", runwayMonths)} Months",
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
    isDiscreet: Boolean
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(spendData) {
                    detectTapGestures { offset ->
                        val count = spendData.size
                        val barWidth = 14.dp.toPx()
                        val spacing = (size.width - (count * barWidth)) / (count - 1).coerceAtLeast(1)
                        val idx = (offset.x / (barWidth + spacing)).toInt().coerceIn(0, spendData.lastIndex)
                        selectedBarIndex = if (selectedBarIndex == idx) null else idx
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = spendData.size
                val availableWidth = size.width
                val barWidth = 14.dp.toPx()
                val totalBarsWidth = count * barWidth
                val spacing = (availableWidth - totalBarsWidth) / (count - 1).coerceAtLeast(1)
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
    spendData: List<DailySpendData>,
    plannedBudget: Double,
    currencySymbol: String,
    isDiscreet: Boolean
) {
    val days = spendData.map { it.dayLabel }
    val maxDailyBudget = plannedBudget.coerceAtLeast(100.0)
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
                    if (maxDailyBudget >= 1000) "${(maxDailyBudget / 1000).toInt()}k" else "${maxDailyBudget.toInt()}",
                    if (maxDailyBudget >= 1000) "${(maxDailyBudget * 0.75 / 1000).toInt()}k" else "${(maxDailyBudget * 0.75).toInt()}",
                    if (maxDailyBudget >= 1000) "${(maxDailyBudget * 0.50 / 1000).toInt()}k" else "${(maxDailyBudget * 0.50).toInt()}",
                    if (maxDailyBudget >= 1000) "${(maxDailyBudget * 0.25 / 1000).toInt()}k" else "${(maxDailyBudget * 0.25).toInt()}"
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
                    .pointerInput(spendData) {
                        detectTapGestures { offset ->
                            val count = spendData.size.coerceAtLeast(2)
                            val idx = ((offset.x / size.width) * (count - 1)).toInt().coerceIn(0, spendData.lastIndex)
                            selectedIndex = if (selectedIndex == idx) null else idx
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
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

                    val count = spendData.size.coerceAtLeast(2)
                    val targetPoints = (0 until count).map { i ->
                        val x = (i.toFloat() / (count - 1).coerceAtLeast(1)) * w
                        val targetProgress = (i + 1).toFloat() / count
                        val y = h * (1f - (targetProgress * 0.7f).coerceIn(0.15f, 0.85f))
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
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                    val targetCenter = targetPoints[count / 2]
                    drawCircle(color = SoftTeal, radius = 3.5.dp.toPx(), center = targetCenter)

                    var runningCumulative = 0.0
                    val actualPoints = (0 until count).map { i ->
                        runningCumulative += spendData.getOrNull(i)?.totalAmount ?: 0.0
                        val spendRatio = (runningCumulative / maxDailyBudget).toFloat().coerceIn(0f, 1f)
                        val y = h * (1f - (spendRatio * 0.80f + 0.10f))
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

                    if (actualPoints.size >= 4) {
                        val dot1 = actualPoints[1]
                        drawCircle(color = CardWhite, radius = 4.dp.toPx(), center = dot1)
                        drawCircle(color = AccentPurple, radius = 3.dp.toPx(), center = dot1)

                        val dot2 = actualPoints[actualPoints.size - 2]
                        drawCircle(color = CardWhite, radius = 4.dp.toPx(), center = dot2)
                        drawCircle(color = AccentPurple, radius = 3.dp.toPx(), center = dot2)
                    }
                }

                selectedIndex?.let { idx ->
                    val data = spendData.getOrNull(idx)
                    if (data != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CardWhite,
                            border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.3f)),
                            shadowElevation = 3.dp,
                            modifier = Modifier.align(Alignment.TopCenter)
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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            days.forEach { day ->
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
private fun SymmetricalFunnelRibbonCanvas(
    needsAmount: Double,
    wantsAmount: Double,
    assetAmount: Double
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        val w = size.width
        val h = size.height

        val total = (needsAmount + wantsAmount + assetAmount).coerceAtLeast(1.0)
        val needsRatio = (needsAmount / total).toFloat().coerceIn(0.15f, 0.70f)
        val wantsRatio = (wantsAmount / total).toFloat().coerceIn(0.15f, 0.70f)

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
}

@Composable
private fun LayeredMountainAreaChartCanvas(
    liquidTotal: Double,
    assetTotal: Double
) {
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
}

@Composable
private fun ThreeBubbleAllocationCanvas(
    bankAmount: Double,
    cashAmount: Double,
    assetAmount: Double,
    currency: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
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
                val label = if (bankAmount >= 1000) "$currency${(bankAmount / 1000).toInt()}k" else "$currency${bankAmount.toInt()}"
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(SoftTeal.copy(alpha = 0.88f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Assets", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                val label = if (assetAmount >= 1000) "$currency${(assetAmount / 1000).toInt()}k" else "$currency${assetAmount.toInt()}"
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
                val label = if (cashAmount >= 1000) "$currency${(cashAmount / 1000).toInt()}k" else "$currency${cashAmount.toInt()}"
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }
    }
}
