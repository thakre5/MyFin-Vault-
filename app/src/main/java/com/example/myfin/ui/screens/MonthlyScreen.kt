package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.TransferSubtype
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MonthlyScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToTaxonomy: () -> Unit = {},
    onNavigateToVaults: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val filterCriteria by viewModel.filterCriteria.collectAsState()
    val showRollover by viewModel.showRolloverPrompt.collectAsState()

    val pagerState = rememberPagerState(pageCount = { 3 })
    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()
    val pageTitles = remember { listOf("Summary", "Ledger", "AutoPay") }

    var selectedMatrixType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedTxFilterType by remember { mutableStateOf<TransactionType?>(null) }

    var isDiscreetMode by remember { mutableStateOf(false) }
    var dismissedWaterfallMonth by remember { mutableIntStateOf(0) }

    var showAddSheet by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    // Recurring Commitments Filter States
    var hideSettledCommitments by remember { mutableStateOf(false) }
    var selectedCommitmentFilter by remember { mutableStateOf<TransactionType?>(null) }

    // Transaction Details & Editing States
    var viewingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var editingTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var showAddFixedBill by remember { mutableStateOf(false) }
    var editingFixedBill by remember { mutableStateOf<FixedBillEntity?>(null) }

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var billToDelete by remember { mutableStateOf<FixedBillEntity?>(null) }
    var billToRevert by remember { mutableStateOf<FixedBillEntity?>(null) }
    var settlingFixedBill by remember { mutableStateOf<FixedBillEntity?>(null) }

    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    // Synchronize active accounts and isolate archived accounts
    val activeAccounts = remember(uiState.activeAccounts, uiState.accounts) {
        uiState.activeAccounts.ifEmpty { uiState.accounts.filter { !it.isArchived } }
    }
    val accountsList = remember(activeAccounts) {
        activeAccounts.map { it.accountName }
    }

    val operatingAccountName = remember(activeAccounts) {
        activeAccounts.firstOrNull { it.accountType.equals("Operating", ignoreCase = true) }?.accountName
            ?: activeAccounts.firstOrNull()?.accountName ?: "Primary Bank"
    }
    val fortressAccountName = remember(activeAccounts) {
        activeAccounts.firstOrNull { it.accountType.equals("Fortress", ignoreCase = true) }?.accountName
            ?: activeAccounts.getOrNull(2)?.accountName ?: "Tertiary Bank"
    }

    // Relative Timeframe & Daily Burn Allowance Calculation
    val daysInMonth = remember(uiState.selectedMonth, uiState.selectedYear) {
        Calendar.getInstance().apply {
            set(uiState.selectedYear, uiState.selectedMonth - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val todayCal = remember { Calendar.getInstance() }
    val isCurrentMonth = uiState.selectedYear == todayCal.get(Calendar.YEAR) &&
            uiState.selectedMonth == (todayCal.get(Calendar.MONTH) + 1)
    val isPastMonth = (uiState.selectedYear < todayCal.get(Calendar.YEAR)) ||
            (uiState.selectedYear == todayCal.get(Calendar.YEAR) && uiState.selectedMonth < (todayCal.get(Calendar.MONTH) + 1))

    val daysRemaining = when {
        isCurrentMonth -> (daysInMonth - todayCal.get(Calendar.DAY_OF_MONTH) + 1).coerceAtLeast(1)
        isPastMonth -> 0
        else -> daysInMonth
    }
    val dailySpendAllowance = if (daysRemaining > 0) {
        (uiState.metrics.safeToSpend / daysRemaining).coerceAtLeast(0.0)
    } else 0.0

    // Payday Waterfall Split Detection
    val paydayPlan = uiState.paydaySuggestion
    val showWaterfallPrompt = remember(paydayPlan, uiState.selectedMonth, dismissedWaterfallMonth) {
        paydayPlan != null && dismissedWaterfallMonth != uiState.selectedMonth
    }

    val fabActions = remember {
        listOf(
            DockFabAction(
                icon = Icons.Default.Add,
                label = "Add Entry",
                onClick = {
                    editingTx = null
                    showAddSheet = true
                }
            ),
            DockFabAction(
                icon = Icons.Default.SyncAlt,
                label = "Transfer",
                onClick = { showTransferSheet = true }
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
                            contentDescription = "Drawer / Navigation",
                            tint = TextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Centered Month Selector
                    Surface(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showMonthPicker = true },
                        shape = RoundedCornerShape(20.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${MONTH_NAMES[uiState.selectedMonth - 1]} ${uiState.selectedYear}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Top Right Actions (Settled Toggle + Discreet Mode)
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (pagerState.currentPage == 2) {
                            IconButton(
                                onClick = { hideSettledCommitments = !hideSettledCommitments },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (hideSettledCommitments) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                                    contentDescription = "Toggle Settled Visibility",
                                    tint = if (hideSettledCommitments) AccentPurple else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { isDiscreetMode = !isDiscreetMode },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isDiscreetMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Balance Privacy",
                                tint = if (isDiscreetMode) AccentPurple else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

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

            // 2. FULL-SCREEN HORIZONTAL PAGER
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    // --- SUB-SCREEN 0: SUMMARY DASHBOARD ---
                    0 -> {
                        // Safe Composable context before entering LazyListScope
                        val activeMatrix = remember(uiState.categories, selectedMatrixType) {
                            uiState.categories.filter { it.type == selectedMatrixType && it.category.isNotBlank() }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
                        ) {
                            // Notification Slot
                            if (showRollover || showWaterfallPrompt || uiState.commitmentsShortfall.isShortfall) {
                                item {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // 1. Commitments Shortfall Warning Banner
                                        if (uiState.commitmentsShortfall.isShortfall) {
                                            val shortfall = uiState.commitmentsShortfall
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                                                shape = RoundedCornerShape(16.dp),
                                                color = CardWhite,
                                                border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(SoftRed.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.WarningAmber,
                                                            contentDescription = null,
                                                            tint = SoftRed,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Commitments Shortfall Warning",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = TextDark
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        val dueText = if (shortfall.earliestDueDay != null) " by ${shortfall.earliestDueDay}th" else ""
                                                        Text(
                                                            text = "Transfer ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", shortfall.shortfallAmount)}$dueText to protect MAB & avoid bill bounce.",
                                                            fontSize = 11.sp,
                                                            color = TextMuted,
                                                            lineHeight = 15.sp,
                                                            maxLines = 2
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Button(
                                                        onClick = { showTransferSheet = true },
                                                        shape = RoundedCornerShape(8.dp),
                                                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text(text = "Transfer", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        // 2. Clone / Rollover Banner
                                        if (showRollover) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                                                shape = RoundedCornerShape(16.dp),
                                                color = CardWhite,
                                                border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.28f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
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
                                                            imageVector = Icons.Default.SyncAlt,
                                                            contentDescription = null,
                                                            tint = AccentPurple,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Clone & Roll Over Commitments",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = TextDark
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Carry forward recurring AutoPay bills and budget limits to the next cycle.",
                                                            fontSize = 11.sp,
                                                            color = TextMuted,
                                                            lineHeight = 15.sp,
                                                            maxLines = 2
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Button(
                                                            onClick = { viewModel.executeRolloverToNextMonth() },
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(32.dp)
                                                        ) {
                                                            Text(text = "Sync", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        TextButton(
                                                            onClick = { viewModel.dismissRolloverPrompt() },
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                            modifier = Modifier.height(22.dp)
                                                        ) {
                                                            Text(text = "Dismiss", fontSize = 10.sp, color = TextMuted)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 3. Fortress Surplus Waterfall Prompt
                                        if (showWaterfallPrompt && paydayPlan != null) {
                                            Surface(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                                                shape = RoundedCornerShape(16.dp),
                                                color = CardWhite,
                                                border = BorderStroke(1.dp, SoftTeal.copy(alpha = 0.35f))
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(14.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(SoftTeal.copy(alpha = 0.12f)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Security,
                                                            contentDescription = null,
                                                            tint = SoftTeal,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "Fortress Surplus Detected",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp,
                                                            color = TextDark
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Sweep ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", paydayPlan.toFortress)} excess cash into Fortress Vault.",
                                                            fontSize = 11.sp,
                                                            color = TextMuted,
                                                            lineHeight = 15.sp,
                                                            maxLines = 2
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.width(10.dp))

                                                    Column(
                                                        horizontalAlignment = Alignment.End,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Button(
                                                            onClick = {
                                                                viewModel.applyPaydayAllocation(
                                                                    plan = paydayPlan,
                                                                    operatingAccount = operatingAccountName,
                                                                    fortressAccount = fortressAccountName
                                                                )
                                                                dismissedWaterfallMonth = uiState.selectedMonth
                                                                Toast.makeText(context, "Surplus swept to Fortress!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            shape = RoundedCornerShape(8.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                            modifier = Modifier.height(32.dp)
                                                        ) {
                                                            Text(text = "Sweep Now", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        TextButton(
                                                            onClick = { dismissedWaterfallMonth = uiState.selectedMonth },
                                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                            modifier = Modifier.height(22.dp)
                                                        ) {
                                                            Text(text = "Dismiss", fontSize = 10.sp, color = TextMuted)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                }
                            }

                            // Hero Card: Real Liquid Safe-to-Spend Guardrail & Live Sparkline
                            item {
                                val isHealthy = uiState.metrics.safeToSpend > 0
                                val statusColor = if (isHealthy) SoftGreen else SoftRed

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(6.dp, RoundedCornerShape(26.dp)),
                                    shape = RoundedCornerShape(26.dp),
                                    color = CardWhite,
                                    border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.18f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .background(
                                                Brush.verticalGradient(
                                                    colors = listOf(
                                                        Color(0xFFFFFFFF),
                                                        Color(0xFFFCFAFF),
                                                        AccentPurple.copy(alpha = 0.05f)
                                                    )
                                                )
                                            )
                                            .padding(22.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(statusColor)
                                                )
                                                Spacer(modifier = Modifier.width(7.dp))
                                                Text(
                                                    text = "LIQUID SAFE TO SPEND",
                                                    color = TextMuted,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = 0.8.sp
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isHealthy) AccentPurple.copy(alpha = 0.1f) else statusColor.copy(alpha = 0.12f),
                                                border = BorderStroke(0.6.dp, if (isHealthy) AccentPurple.copy(alpha = 0.25f) else statusColor.copy(alpha = 0.3f))
                                            ) {
                                                Text(
                                                    text = "${uiState.metrics.safeToSpendPercentage}% Capacity",
                                                    color = if (isHealthy) AccentPurple else statusColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = if (isDiscreetMode) "••••••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.safeToSpend)}",
                                            fontSize = 34.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isHealthy) TextDark else SoftRed,
                                            letterSpacing = (-0.6).sp
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Text(
                                            text = when {
                                                isPastMonth -> "Month closed: final remaining balance"
                                                isCurrentMonth && isHealthy -> if (isDiscreetMode) "Daily allowance protected" else "Avg ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailySpendAllowance)}/day safe allowance for $daysRemaining days left"
                                                isCurrentMonth -> "Overrun warning: spending exceeds liquid operating buffer"
                                                else -> "Projected safe allowance across $daysRemaining days"
                                            },
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isHealthy) TextMuted else SoftRed
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        SpendingSparkline(
                                            points = uiState.metrics.dailyExpensePoints,
                                            lineColor = if (isHealthy) AccentPurple else SoftRed,
                                            gradientStartColor = (if (isHealthy) AccentPurple else SoftRed).copy(alpha = 0.32f),
                                            gradientEndColor = (if (isHealthy) AccentPurple else SoftRed).copy(alpha = 0.0f)
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            PillarMetricCard(
                                                title = "Inflow",
                                                amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.plannedIncome)}",
                                                tintColor = SoftGreen,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    coroutineScope.launch {
                                                        selectedTxFilterType = TransactionType.INCOME
                                                        viewModel.updateFilter(TransactionType.INCOME, filterCriteria.account, filterCriteria.startDate, filterCriteria.endDate)
                                                        pagerState.animateScrollToPage(1)
                                                    }
                                                }
                                            )
                                            PillarMetricCard(
                                                title = "Fixed Bills",
                                                amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.fixedCommitmentsTotal)}",
                                                tintColor = SoftRed,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    coroutineScope.launch {
                                                        pagerState.animateScrollToPage(2)
                                                    }
                                                }
                                            )
                                            PillarMetricCard(
                                                title = "SIP Assets",
                                                amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualAssets)}",
                                                tintColor = SoftTeal,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    coroutineScope.launch {
                                                        selectedTxFilterType = TransactionType.ASSET
                                                        viewModel.updateFilter(TransactionType.ASSET, filterCriteria.account, filterCriteria.startDate, filterCriteria.endDate)
                                                        pagerState.animateScrollToPage(1)
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // Balance Flow & Net Savings Delta Card
                            item {
                                val actualIncome = uiState.metrics.actualIncome
                                val actualExpenses = uiState.metrics.actualExpenses
                                val actualAssets = uiState.metrics.actualAssets
                                val netSavings = uiState.metrics.netSavedAfterInvest
                                val currentEndBalance = uiState.metrics.totalVaultBalance
                                val monthMovement = actualIncome - actualExpenses - actualAssets
                                val startBalance = currentEndBalance - monthMovement
                                val savingsRatePct = if (actualIncome > 0) ((netSavings / actualIncome) * 100).toInt() else 0

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(3.dp, RoundedCornerShape(20.dp)),
                                    shape = RoundedCornerShape(20.dp),
                                    color = CardWhite,
                                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = "START BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.4.sp)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", startBalance)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = TextDark
                                            )
                                            Text(text = "Opening Vault", fontSize = 10.sp, color = TextMuted)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .height(34.dp)
                                                .width(1.dp)
                                                .background(BorderLight.copy(alpha = 0.6f))
                                        )

                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(start = 12.dp)
                                        ) {
                                            Text(text = "END BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.4.sp)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", currentEndBalance)}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = if (currentEndBalance >= 0) TextDark else SoftRed
                                            )
                                            Text(text = "Active Liquid", fontSize = 10.sp, color = TextMuted)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .height(34.dp)
                                                .width(1.dp)
                                                .background(BorderLight.copy(alpha = 0.6f))
                                        )

                                        Column(
                                            modifier = Modifier
                                                .weight(1.1f)
                                                .padding(start = 12.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(text = "NET SAVINGS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.4.sp)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "${if (netSavings >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", netSavings)}",
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = if (netSavings >= 0) SoftTeal else SoftRed
                                            )
                                            Text(
                                                text = "${if (netSavings >= 0) "+" else ""}$savingsRatePct% Net Rate",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (netSavings >= 0) SoftTeal else SoftRed
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            // 3-Pillar Target & Cashflow Execution
                            item {
                                val plannedExpenses = uiState.metrics.plannedExpenses
                                val actualExpenses = uiState.metrics.actualExpenses
                                val expDiff = actualExpenses - plannedExpenses
                                val expFraction = if (plannedExpenses > 0) (actualExpenses / plannedExpenses).toFloat().coerceIn(0f, 1f) else 1f

                                val plannedIncome = uiState.metrics.plannedIncome
                                val actualIncome = uiState.metrics.actualIncome
                                val incDiff = actualIncome - plannedIncome
                                val incFraction = if (plannedIncome > 0) (actualIncome / plannedIncome).toFloat().coerceIn(0f, 1f) else 1f

                                val plannedAssets = uiState.metrics.plannedAssets
                                val actualAssets = uiState.metrics.actualAssets
                                val astDiff = actualAssets - plannedAssets
                                val astFraction = if (plannedAssets > 0) (actualAssets / plannedAssets).toFloat().coerceIn(0f, 1f) else 1f

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(3.dp, RoundedCornerShape(22.dp)),
                                    shape = RoundedCornerShape(22.dp),
                                    color = CardWhite,
                                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(18.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Text(
                                            text = "3-PILLAR TARGET & CASHFLOW EXECUTION",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextMuted,
                                            letterSpacing = 0.6.sp
                                        )

                                        PillarDualBarRow(
                                            title = "Expenses",
                                            planned = plannedExpenses,
                                            actual = actualExpenses,
                                            currencySymbol = userProfile.currencySymbol,
                                            progressFraction = expFraction,
                                            barColor = SoftRed,
                                            isDiscreet = isDiscreetMode,
                                            varianceText = if (plannedExpenses > 0) {
                                                if (isDiscreetMode) "Tracked"
                                                else if (expDiff > 0) "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expDiff)} Over"
                                                else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(expDiff))} Left"
                                            } else "No Cap",
                                            isAlert = expDiff > 0 && plannedExpenses > 0
                                        )

                                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.8.dp)

                                        PillarDualBarRow(
                                            title = "Income",
                                            planned = plannedIncome,
                                            actual = actualIncome,
                                            currencySymbol = userProfile.currencySymbol,
                                            progressFraction = incFraction,
                                            barColor = SoftGreen,
                                            isDiscreet = isDiscreetMode,
                                            varianceText = if (plannedIncome > 0) {
                                                if (incDiff >= 0) "Target Met"
                                                else if (isDiscreetMode) "Short"
                                                else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(incDiff))} Short"
                                            } else "Recorded Inflow",
                                            isAlert = false
                                        )

                                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.8.dp)

                                        PillarDualBarRow(
                                            title = "Assets / SIP",
                                            planned = plannedAssets,
                                            actual = actualAssets,
                                            currencySymbol = userProfile.currencySymbol,
                                            progressFraction = astFraction,
                                            barColor = SoftTeal,
                                            isDiscreet = isDiscreetMode,
                                            varianceText = if (plannedAssets > 0) {
                                                if (astDiff >= 0) "Target Met"
                                                else if (isDiscreetMode) "Short"
                                                else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(astDiff))} Short"
                                            } else "Recorded Wealth",
                                            isAlert = false
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                            }

                            // Category Matrix Section Header & Segment Switcher
                            item {
                                Text(text = "Category Matrix", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(BorderLight.copy(alpha = 0.5f))
                                        .padding(4.dp)
                                ) {
                                    listOf(
                                        Triple(TransactionType.EXPENSE, "Expenses", SoftRed),
                                        Triple(TransactionType.INCOME, "Income", SoftGreen),
                                        Triple(TransactionType.ASSET, "Assets / SIP", SoftTeal)
                                    ).forEach { (type, label, color) ->
                                        val isSelected = selectedMatrixType == type
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) CardWhite else Color.Transparent)
                                                .clickable { selectedMatrixType = type }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = if (isSelected) color else TextMuted
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            if (activeMatrix.isEmpty()) {
                                item {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(18.dp),
                                        color = CardWhite
                                    ) {
                                        Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                            Text(text = "No active entries in this segment", fontSize = 12.sp, color = TextMuted)
                                        }
                                    }
                                }
                            } else {
                                item {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(3.dp, RoundedCornerShape(22.dp)),
                                        shape = RoundedCornerShape(22.dp),
                                        color = CardWhite,
                                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                            activeMatrix.forEachIndexed { index, cat ->
                                                val isExpanded = expandedCategories[cat.category] ?: false
                                                val rotation by animateFloatAsState(
                                                    targetValue = if (isExpanded) 180f else 0f,
                                                    animationSpec = tween(200),
                                                    label = "arrowRotation"
                                                )

                                                val progressFraction = if (cat.plannedAmount > 0) {
                                                    (cat.actualAmount / cat.plannedAmount).toFloat().coerceIn(0f, 1f)
                                                } else 1f

                                                val utilizationPercentage = if (cat.plannedAmount > 0) {
                                                    ((cat.actualAmount / cat.plannedAmount) * 100).toInt()
                                                } else 100

                                                val progressColor = when {
                                                    cat.isOverBudget -> SoftRed
                                                    cat.type == TransactionType.INCOME -> SoftGreen
                                                    cat.type == TransactionType.ASSET -> SoftTeal
                                                    utilizationPercentage >= 85 -> SoftAmber
                                                    else -> AccentPurple
                                                }

                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { expandedCategories[cat.category] = !isExpanded }
                                                        .padding(vertical = 9.dp, horizontal = 4.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Left Side: Category Icon + Title & Directly Bound Subtitle
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(38.dp)
                                                                    .clip(RoundedCornerShape(10.dp))
                                                                    .background(progressColor.copy(alpha = 0.12f)),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = cat.category.take(1).uppercase(),
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 14.5.sp,
                                                                    color = progressColor
                                                                )
                                                            }

                                                            Spacer(modifier = Modifier.width(12.dp))

                                                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Text(
                                                                        text = cat.category,
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 13.5.sp,
                                                                        color = TextDark,
                                                                        maxLines = 1,
                                                                        overflow = TextOverflow.Ellipsis
                                                                    )

                                                                    if (cat.isOverBudget) {
                                                                        Surface(
                                                                            shape = RoundedCornerShape(4.dp),
                                                                            color = SoftRed.copy(alpha = 0.12f),
                                                                            border = BorderStroke(0.5.dp, SoftRed.copy(alpha = 0.35f))
                                                                        ) {
                                                                            Text(
                                                                                text = "Over",
                                                                                fontSize = 8.5.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = SoftRed,
                                                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                                            )
                                                                        }
                                                                    }
                                                                }

                                                                Spacer(modifier = Modifier.height(2.dp))

                                                                val statusText = if (cat.plannedAmount > 0) {
                                                                    val remaining = cat.plannedAmount - cat.actualAmount
                                                                    if (isDiscreetMode) "Target configured"
                                                                    else if (remaining >= 0) {
                                                                        "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", remaining)} left of ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cat.plannedAmount)}"
                                                                    } else {
                                                                        "Exceeded by ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(remaining))}"
                                                                    }
                                                                } else {
                                                                    "No target limit configured"
                                                                }

                                                                Text(
                                                                    text = statusText,
                                                                    fontSize = 11.sp,
                                                                    fontWeight = if (cat.isOverBudget) FontWeight.SemiBold else FontWeight.Normal,
                                                                    color = if (cat.isOverBudget) SoftRed else TextMuted,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }

                                                        Spacer(modifier = Modifier.width(10.dp))

                                                        // Right Side: Amount, Percentage & Dropdown Icon
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Column(horizontalAlignment = Alignment.End) {
                                                                Text(
                                                                    text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cat.actualAmount)}",
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 14.5.sp,
                                                                    color = if (cat.isOverBudget) SoftRed else TextDark
                                                                )
                                                                if (cat.plannedAmount > 0) {
                                                                    Text(
                                                                        text = "$utilizationPercentage%",
                                                                        fontSize = 10.sp,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = progressColor
                                                                    )
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.width(6.dp))

                                                            Icon(
                                                                Icons.Default.ExpandMore,
                                                                contentDescription = "Expand",
                                                                tint = TextMuted,
                                                                modifier = Modifier
                                                                    .size(16.dp)
                                                                    .rotate(rotation)
                                                            )
                                                        }
                                                    }

                                                    // Refined 3.5dp Progress Bar Directly Anchored Below the Header Row
                                                    if (cat.plannedAmount > 0) {
                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        LinearProgressIndicator(
                                                            progress = { progressFraction },
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .height(3.5.dp)
                                                                .clip(RoundedCornerShape(2.dp)),
                                                            color = progressColor,
                                                            trackColor = BorderLight.copy(alpha = 0.5f)
                                                        )
                                                    }

                                                    // Expandable Subcategory Contributions
                                                    AnimatedVisibility(
                                                        visible = isExpanded,
                                                        enter = expandVertically() + fadeIn(),
                                                        exit = shrinkVertically() + fadeOut()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(top = 10.dp)
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .background(CanvasLight)
                                                                .padding(10.dp)
                                                        ) {
                                                            if (cat.activeSubcategories.isEmpty()) {
                                                                Text(text = "No logged transactions in subcategories", fontSize = 11.sp, color = TextMuted)
                                                            } else {
                                                                Text(
                                                                    text = "SUBCATEGORY CONTRIBUTIONS",
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = TextMuted,
                                                                    letterSpacing = 0.5.sp,
                                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                                )

                                                                cat.activeSubcategories.forEach { sub ->
                                                                    val subPercentage = if (cat.actualAmount > 0) {
                                                                        ((sub.amount / cat.actualAmount) * 100).toInt()
                                                                    } else 0

                                                                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                                                        Row(
                                                                            modifier = Modifier.fillMaxWidth(),
                                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                                            verticalAlignment = Alignment.CenterVertically
                                                                        ) {
                                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                Box(
                                                                                    modifier = Modifier
                                                                                        .size(5.dp)
                                                                                        .clip(CircleShape)
                                                                                        .background(progressColor)
                                                                                )
                                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                                Text(text = sub.name, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                                                            }

                                                                            Text(
                                                                                text = if (isDiscreetMode) "•••• ($subPercentage%)" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", sub.amount)} ($subPercentage%)",
                                                                                fontSize = 11.5.sp,
                                                                                fontWeight = FontWeight.Bold,
                                                                                color = TextDark
                                                                            )
                                                                        }
                                                                        Spacer(modifier = Modifier.height(2.5.dp))
                                                                        LinearProgressIndicator(
                                                                            progress = { (subPercentage / 100f).coerceIn(0f, 1f) },
                                                                            modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .height(3.dp)
                                                                                .clip(RoundedCornerShape(1.5.dp)),
                                                                            color = progressColor.copy(alpha = 0.65f),
                                                                            trackColor = BorderLight.copy(alpha = 0.5f)
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                if (index < activeMatrix.lastIndex) {
                                                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- SUB-SCREEN 1: TRANSACTIONS LEDGER ---
                    1 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                        ) {
                            // Pinned Search & Action Bar
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                color = CardWhite,
                                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))

                                    Box(modifier = Modifier.weight(1f)) {
                                        if (filterCriteria.query.isEmpty()) {
                                            Text(text = "Search ledger...", color = TextMuted, fontSize = 13.sp, maxLines = 1)
                                        }
                                        BasicTextField(
                                            value = filterCriteria.query,
                                            onValueChange = { viewModel.updateSearchQuery(it) },
                                            singleLine = true,
                                            textStyle = TextStyle(fontSize = 13.sp, color = TextDark, fontWeight = FontWeight.Medium),
                                            cursorBrush = SolidColor(AccentPurple),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (filterCriteria.query.isNotBlank()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(16.dp))
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    IconButton(onClick = { showFilterSheet = true }, modifier = Modifier.size(28.dp)) {
                                        Icon(
                                            Icons.Default.Tune,
                                            contentDescription = "Filter",
                                            tint = if (filterCriteria.type != null || filterCriteria.account != "ALL" || filterCriteria.startDate != null) AccentPurple else TextMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Active Date Range Filter Banner (If Active)
                            if (filterCriteria.startDate != null && filterCriteria.endDate != null) {
                                val sdf = remember { SimpleDateFormat("dd MMM", Locale.US) }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentPurple.copy(alpha = 0.12f),
                                    border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DateRange, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Date Filter: ${sdf.format(Date(filterCriteria.startDate!!))} – ${sdf.format(Date(filterCriteria.endDate!!))}",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentPurple
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.updateFilter(filterCriteria.type, filterCriteria.account, null, null) },
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear Date Filter", tint = AccentPurple, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Pinned Transaction Type Filter Chips
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BorderLight.copy(alpha = 0.5f))
                                    .padding(3.dp)
                            ) {
                                listOf(
                                    null to "All",
                                    TransactionType.EXPENSE to "Expenses",
                                    TransactionType.INCOME to "Income",
                                    TransactionType.ASSET to "Assets",
                                    TransactionType.TRANSFER to "Transfers"
                                ).forEach { (type, label) ->
                                    val isSelected = selectedTxFilterType == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (isSelected) CardWhite else Color.Transparent)
                                            .clickable {
                                                selectedTxFilterType = type
                                                viewModel.updateFilter(type, filterCriteria.account, filterCriteria.startDate, filterCriteria.endDate)
                                            }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (isSelected) {
                                                when (type) {
                                                    TransactionType.EXPENSE -> SoftRed
                                                    TransactionType.INCOME -> SoftGreen
                                                    TransactionType.ASSET -> SoftTeal
                                                    TransactionType.TRANSFER -> AccentPurple
                                                    else -> TextDark
                                                }
                                            } else TextMuted
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Pinned Vault Selector Chips
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    FilterChip(
                                        selected = filterCriteria.account == "ALL",
                                        onClick = { viewModel.updateFilter(filterCriteria.type, "ALL", filterCriteria.startDate, filterCriteria.endDate) },
                                        label = { Text(text = "All Vaults", fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AccentPurple.copy(alpha = 0.12f),
                                            selectedLabelColor = AccentPurple
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = filterCriteria.account == "ALL",
                                            selectedBorderColor = AccentPurple.copy(alpha = 0.4f),
                                            borderColor = BorderLight
                                        )
                                    )
                                }
                                items(accountsList) { acc ->
                                    val isSelected = filterCriteria.account == acc
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateFilter(filterCriteria.type, acc, filterCriteria.startDate, filterCriteria.endDate) },
                                        label = { Text(text = acc, fontSize = 11.sp) },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AccentPurple.copy(alpha = 0.12f),
                                            selectedLabelColor = AccentPurple
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = isSelected,
                                            selectedBorderColor = AccentPurple.copy(alpha = 0.4f),
                                            borderColor = BorderLight
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Scrollable Ledger List with Sticky Headers & Crisp Edge Definition
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(top = 2.dp, bottom = 140.dp)
                            ) {
                                if (uiState.groupedTransactions.isEmpty()) {
                                    item(key = "empty_ledger") {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 40.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(text = "No transactions recorded", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = "Try clearing filters or log a new entry", fontSize = 12.sp, color = TextMuted)
                                                if (filterCriteria.query.isNotBlank() || filterCriteria.type != null || filterCriteria.account != "ALL" || filterCriteria.startDate != null) {
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    TextButton(
                                                        onClick = {
                                                            selectedTxFilterType = null
                                                            viewModel.resetFilters()
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp), tint = AccentPurple)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Reset Filters", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    uiState.groupedTransactions.forEach { (dateHeader, txList) ->
                                        val dailyExpenseTotal = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                                        val dailyIncomeTotal = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                                        val sortedTxList = txList.sortedByDescending { it.date }

                                        // Sticky date header with crisp bottom edge divider
                                        stickyHeader(key = "header_$dateHeader") {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = CanvasLight
                                            ) {
                                                Column(modifier = Modifier.fillMaxWidth()) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 10.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = TextDark
                                                            ) {
                                                                Text(
                                                                    text = dateHeader.uppercase(),
                                                                    fontWeight = FontWeight.Black,
                                                                    fontSize = 9.5.sp,
                                                                    color = Color.White,
                                                                    letterSpacing = 0.6.sp,
                                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                                                )
                                                            }

                                                            Spacer(modifier = Modifier.width(8.dp))

                                                            Text(
                                                                text = "${txList.size} ${if (txList.size == 1) "entry" else "entries"}",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Medium,
                                                                color = TextMuted
                                                            )
                                                        }

                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            if (dailyIncomeTotal > 0.0) {
                                                                Text(
                                                                    text = if (isDiscreetMode) "••••" else "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyIncomeTotal)}",
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.5.sp,
                                                                    color = SoftGreen
                                                                )
                                                            }
                                                            if (dailyExpenseTotal > 0.0) {
                                                                Text(
                                                                    text = if (isDiscreetMode) "••••" else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyExpenseTotal)}",
                                                                    fontWeight = FontWeight.Bold,
                                                                    fontSize = 11.5.sp,
                                                                    color = TextDark
                                                                )
                                                            }
                                                        }
                                                    }
                                                    HorizontalDivider(
                                                        color = BorderLight.copy(alpha = 0.5f),
                                                        thickness = 0.6.dp
                                                    )
                                                }
                                            }
                                        }

                                        items(sortedTxList, key = { it.id }) { tx ->
                                            Box(modifier = Modifier.padding(vertical = 3.5.dp)) {
                                                SwipeableTransactionItem(
                                                    transaction = tx,
                                                    currencySymbol = userProfile.currencySymbol,
                                                    onTap = { viewingTx = it },
                                                    onEdit = { editingTx = it; showAddSheet = true },
                                                    onDelete = { transactionToDelete = it }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- SUB-SCREEN 2: RECURRING COMMITMENTS ---
                    2 -> {
                        val currentDayOfMonth = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
                        val filteredBills = remember(uiState.fixedBills, hideSettledCommitments, selectedCommitmentFilter) {
                            uiState.fixedBills.filter { bill ->
                                val matchesHidden = !hideSettledCommitments || !bill.isPaid
                                val matchesType = selectedCommitmentFilter == null || bill.type == selectedCommitmentFilter
                                matchesHidden && matchesType
                            }
                        }

                        val pendingCommitmentsTotal = remember(filteredBills) {
                            filteredBills.filter { !it.isPaid }.sumOf { it.amount }
                        }

                        val overdueCount = remember(filteredBills, currentDayOfMonth) {
                            filteredBills.count { !it.isPaid && it.dueDay != null && it.dueDay!! < currentDayOfMonth }
                        }
                        val hasOverdue = overdueCount > 0

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp)
                        ) {
                            // Pinned Header Section with Stacked Title & Pending Total Pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Recurring Commitments",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "AutoPay, Standing Orders & Inflows",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    TextButton(
                                        onClick = { showAddFixedBill = true },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(15.dp), tint = AccentPurple)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(text = "Add AutoPay", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = AccentPurple)
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    val badgeColor = if (hasOverdue) SoftRed else SoftAmber
                                    val badgeText = if (isDiscreetMode) {
                                        "•••• Pending"
                                    } else {
                                        val amtStr = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", pendingCommitmentsTotal)}"
                                        if (hasOverdue) "$amtStr Pending ($overdueCount Overdue)" else "$amtStr Pending"
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = badgeColor.copy(alpha = 0.12f),
                                        border = BorderStroke(0.6.dp, badgeColor.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Pinned Action-Based Segmented Filters
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BorderLight.copy(alpha = 0.5f))
                                    .padding(3.dp)
                            ) {
                                listOf(
                                    null to "All",
                                    TransactionType.EXPENSE to "Bills",
                                    TransactionType.INCOME to "Receivables",
                                    TransactionType.ASSET to "SIPs",
                                    TransactionType.TRANSFER to "Sweeps"
                                ).forEach { (type, label) ->
                                    val isSelected = selectedCommitmentFilter == type
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (isSelected) CardWhite else Color.Transparent)
                                            .clickable { selectedCommitmentFilter = type }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 11.sp,
                                            color = if (isSelected) {
                                                when (type) {
                                                    TransactionType.EXPENSE -> SoftRed
                                                    TransactionType.INCOME -> SoftGreen
                                                    TransactionType.ASSET -> SoftTeal
                                                    TransactionType.TRANSFER -> AccentPurple
                                                    else -> TextDark
                                                }
                                            } else TextMuted
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Scrollable Commitments List
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(top = 2.dp, bottom = 140.dp)
                            ) {
                                if (filteredBills.isEmpty()) {
                                    item(key = "empty_commitments") {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(16.dp),
                                            color = CardWhite
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .padding(24.dp)
                                                    .fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = if (hideSettledCommitments) "No pending commitments in this filter" else "No recurring commitments recorded",
                                                    fontSize = 12.sp,
                                                    color = TextMuted
                                                )
                                                if (hideSettledCommitments || selectedCommitmentFilter != null) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    TextButton(
                                                        onClick = {
                                                            hideSettledCommitments = false
                                                            selectedCommitmentFilter = null
                                                        }
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentPurple)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Reset Filters", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    items(filteredBills, key = { it.id }) { bill ->
                                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                                            SwipeableFixedBillItem(
                                                bill = bill,
                                                currencySymbol = userProfile.currencySymbol,
                                                onTap = { b ->
                                                    if (!b.isPaid) {
                                                        settlingFixedBill = b
                                                    } else {
                                                        billToRevert = b
                                                    }
                                                },
                                                onEdit = { b ->
                                                    if (b.isPaid) {
                                                        Toast.makeText(context, "Cannot edit settled commitment. Tap card to revert to Unpaid first.", Toast.LENGTH_LONG).show()
                                                    } else {
                                                        editingFixedBill = b
                                                    }
                                                },
                                                onDelete = { b -> billToDelete = b },
                                                onSettleBill = { b, customAmt, dateMillis ->
                                                    viewModel.toggleFixedBillPaid(b, customAmount = customAmt, customDateMillis = dateMillis)
                                                }
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

        // 3. BOTTOM GRADIENT SCRIM
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

        // 4. FLOATING PAGER INDICATOR PILL
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

        // 5. FLOATING BOTTOM NAVIGATION DOCK WITH FAB
        AppBottomDock(
            currentSelection = NavigationTarget.MONTHLY_VIEW,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.BUDGET_PLANNER -> onNavigateToPlanner()
                    NavigationTarget.VAULT_ACCOUNTS -> onNavigateToVaults()
                    NavigationTarget.REPORTS_ANALYTICS -> onNavigateToAnalytics()
                    NavigationTarget.DATA_SET -> onNavigateToTaxonomy()
                    NavigationTarget.MONTHLY_VIEW -> { /* Active */ }
                    else -> {}
                }
            },
            fabActions = fabActions,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        )

        // 6. MODALS, BOTTOM SHEETS & CONFIRMATION DIALOGS

        // Transaction Detail Bottom Sheet
        viewingTx?.let { tx ->
            TransactionDetailBottomSheet(
                transaction = tx,
                currencySymbol = userProfile.currencySymbol,
                onDismiss = { viewingTx = null },
                onEdit = {
                    viewingTx = null
                    editingTx = it
                    showAddSheet = true
                },
                onDelete = {
                    viewingTx = null
                    transactionToDelete = it
                }
            )
        }

        // Delete Transaction Alert
        transactionToDelete?.let { tx ->
            val cleanTitle = tx.title.trim()
            val cleanSubcat = tx.subcategory.trim()
            val displayTxName = when {
                cleanTitle.isBlank() || cleanTitle.equals(cleanSubcat, ignoreCase = true) -> cleanSubcat.ifBlank { "Transaction" }
                cleanTitle.startsWith(cleanSubcat, ignoreCase = true) -> {
                    val unique = cleanTitle.removePrefix(cleanSubcat).trim(' ', '-', ':', '(', ')')
                    if (unique.isNotBlank()) "$cleanSubcat ($unique)" else cleanSubcat
                }
                cleanSubcat.isBlank() -> cleanTitle
                else -> "$cleanSubcat ($cleanTitle)"
            }

            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text(text = "Delete Entry?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        text = if (tx.linkedFixedBillId != null)
                            "This entry is linked to an AutoPay bill. Deleting it will restore your vault balance and revert the parent commitment back to Unpaid."
                        else "Are you sure you want to delete '$displayTxName' (${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", tx.amount)})? This will permanently remove it from your vault ledger."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    }) {
                        Text(text = "Delete", color = SoftRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text(text = "Cancel", color = TextDark)
                    }
                }
            )
        }

        // Delete Fixed Bill Alert
        billToDelete?.let { bill ->
            val friendlySubcat = if (bill.type == TransactionType.TRANSFER) {
                when (bill.subcategory.trim()) {
                    "WEALTH_ALLOCATION" -> "Fortress Sweep"
                    "BILL_FUNDING" -> "Bill Funding"
                    "REBALANCE" -> "Rebalance"
                    else -> bill.subcategory.trim().ifBlank { "Vault Sweep" }
                }
            } else bill.subcategory.trim()

            val cleanTitle = bill.title.trim()
            val displayBillName = when {
                cleanTitle.isBlank() || cleanTitle.equals(friendlySubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> {
                    friendlySubcat.ifBlank { cleanTitle.ifBlank { "Commitment" } }
                }
                cleanTitle.startsWith(friendlySubcat, ignoreCase = true) -> {
                    val unique = cleanTitle.removePrefix(friendlySubcat).trim(' ', '-', ':', '(', ')')
                    if (unique.isNotBlank()) "$friendlySubcat ($unique)" else friendlySubcat
                }
                friendlySubcat.isBlank() -> cleanTitle
                else -> "$friendlySubcat ($cleanTitle)"
            }

            AlertDialog(
                onDismissRequest = { billToDelete = null },
                title = { Text(text = "Delete AutoPay Commitment?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(text = "Deleting '$displayBillName' will remove this recurring template. Any linked payment already recorded in your ledger for this month will also be deleted and restored to your vault.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteFixedBill(bill)
                        billToDelete = null
                    }) {
                        Text(text = "Delete Commitment", color = SoftRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { billToDelete = null }) {
                        Text(text = "Cancel", color = TextDark)
                    }
                }
            )
        }

        // Revert Fixed Bill Status Alert
        billToRevert?.let { bill ->
            val friendlySubcat = if (bill.type == TransactionType.TRANSFER) {
                when (bill.subcategory.trim()) {
                    "WEALTH_ALLOCATION" -> "Fortress Sweep"
                    "BILL_FUNDING" -> "Bill Funding"
                    "REBALANCE" -> "Rebalance"
                    else -> bill.subcategory.trim().ifBlank { "Vault Sweep" }
                }
            } else bill.subcategory.trim()

            val cleanTitle = bill.title.trim()
            val displayBillName = when {
                cleanTitle.isBlank() || cleanTitle.equals(friendlySubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> {
                    friendlySubcat.ifBlank { cleanTitle.ifBlank { "Commitment" } }
                }
                cleanTitle.startsWith(friendlySubcat, ignoreCase = true) -> {
                    val unique = cleanTitle.removePrefix(friendlySubcat).trim(' ', '-', ':', '(', ')')
                    if (unique.isNotBlank()) "$friendlySubcat ($unique)" else friendlySubcat
                }
                friendlySubcat.isBlank() -> cleanTitle
                else -> "$friendlySubcat ($cleanTitle)"
            }

            AlertDialog(
                onDismissRequest = { billToRevert = null },
                title = { Text(text = "Revert to Unsettled?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(text = "Reverting '$displayBillName' will delete the logged payment from your transaction ledger and restore the balance to ${bill.accountName}.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.toggleFixedBillPaid(bill)
                        billToRevert = null
                    }) {
                        Text(text = "Revert Status", color = SoftRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { billToRevert = null }) {
                        Text(text = "Cancel", color = TextDark)
                    }
                }
            )
        }

        // Settle Fixed Bill Dialog with Historical Date Logging
        settlingFixedBill?.let { bill ->
            val friendlySubcat = if (bill.type == TransactionType.TRANSFER) {
                when (bill.subcategory.trim()) {
                    "WEALTH_ALLOCATION" -> "Fortress Sweep"
                    "BILL_FUNDING" -> "Bill Funding"
                    "REBALANCE" -> "Rebalance"
                    else -> bill.subcategory.trim().ifBlank { "Vault Sweep" }
                }
            } else bill.subcategory.trim()

            val cleanTitle = bill.title.trim()
            val displayBillName = when {
                cleanTitle.isBlank() || cleanTitle.equals(friendlySubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> {
                    friendlySubcat.ifBlank { cleanTitle.ifBlank { "Commitment" } }
                }
                cleanTitle.startsWith(friendlySubcat, ignoreCase = true) -> {
                    val unique = cleanTitle.removePrefix(friendlySubcat).trim(' ', '-', ':', '(', ')')
                    if (unique.isNotBlank()) "$friendlySubcat ($unique)" else friendlySubcat
                }
                friendlySubcat.isBlank() -> cleanTitle
                else -> "$friendlySubcat ($cleanTitle)"
            }

            val formattedDefaultAmount = if (bill.amount % 1.0 == 0.0) bill.amount.toLong().toString() else bill.amount.toString()
            var finalAmountText by remember(bill.id) { mutableStateOf(formattedDefaultAmount) }
            var selectedSettleDateMillis by remember(bill.id) { mutableStateOf(System.currentTimeMillis()) }
            var showSettleDatePicker by remember { mutableStateOf(false) }

            val isToday = remember(selectedSettleDateMillis) {
                val calSelected = Calendar.getInstance().apply { timeInMillis = selectedSettleDateMillis }
                val calNow = Calendar.getInstance()
                calSelected.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calSelected.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
            }

            val isYesterday = remember(selectedSettleDateMillis) {
                val calSelected = Calendar.getInstance().apply { timeInMillis = selectedSettleDateMillis }
                val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                calSelected.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                calSelected.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)
            }

            val formattedDateLabel = remember(selectedSettleDateMillis, isToday, isYesterday) {
                when {
                    isToday -> "Today"
                    isYesterday -> "Yesterday"
                    else -> SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(selectedSettleDateMillis))
                }
            }

            val fundingAccount = remember(bill.accountName, activeAccounts) {
                activeAccounts.find { it.accountName.equals(bill.accountName, ignoreCase = true) }
            }
            val amt = finalAmountText.toDoubleOrNull() ?: bill.amount
            val willBreachMab = fundingAccount != null && fundingAccount.minBalance > 0.0 &&
                    (fundingAccount.currentBalance - amt) < fundingAccount.minBalance

            val descPrompt = when (bill.type) {
                TransactionType.INCOME -> "Credits ${bill.accountName} vault and logs inflow entry."
                TransactionType.ASSET -> "Deducts from ${bill.accountName} and records under Asset Wealth."
                TransactionType.TRANSFER -> "Sweeps funds from ${bill.accountName} ➔ ${bill.toAccountName ?: "Destination"}."
                TransactionType.EXPENSE -> "Deducts from ${bill.accountName} and records expense entry."
            }

            Dialog(onDismissRequest = { settlingFixedBill = null }) {
                Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Settle $displayBillName", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = descPrompt, fontSize = 12.sp, color = TextMuted)

                        if (fundingAccount != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = CanvasLight,
                                border = BorderStroke(0.6.dp, BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = "Available in ${fundingAccount.accountName}:", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", fundingAccount.currentBalance)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = TextDark
                                    )
                                }
                            }
                        }

                        if (willBreachMab) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SoftRed.copy(alpha = 0.12f),
                                border = BorderStroke(0.6.dp, SoftRed.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "⚠️ MAB Risk: Settling this bill will reduce balance below ${userProfile.currencySymbol}${fundingAccount?.minBalance?.toInt()} minimum balance threshold.",
                                    fontSize = 10.5.sp,
                                    color = SoftRed,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = finalAmountText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                val parts = filtered.split('.')
                                finalAmountText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                            },
                            label = { Text(text = "Actual Amount (${userProfile.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Settlement Date Selector
                        Text("Payment Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = isToday,
                                onClick = { selectedSettleDateMillis = System.currentTimeMillis() },
                                label = { Text("Today", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPurpleLight,
                                    selectedLabelColor = AccentPurple
                                )
                            )

                            FilterChip(
                                selected = isYesterday,
                                onClick = {
                                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                    selectedSettleDateMillis = cal.timeInMillis
                                },
                                label = { Text("Yesterday", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPurpleLight,
                                    selectedLabelColor = AccentPurple
                                )
                            )

                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clickable { showSettleDatePicker = true },
                                shape = RoundedCornerShape(8.dp),
                                color = if (!isToday && !isYesterday) AccentPurpleLight else CanvasLight,
                                border = BorderStroke(0.8.dp, if (!isToday && !isYesterday) AccentPurple else BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = if (!isToday && !isYesterday) formattedDateLabel else "Date",
                                        fontSize = 11.sp,
                                        fontWeight = if (!isToday && !isYesterday) FontWeight.Bold else FontWeight.Medium,
                                        color = if (!isToday && !isYesterday) AccentPurple else TextDark
                                    )
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = "Pick Date",
                                        tint = if (!isToday && !isYesterday) AccentPurple else TextMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { settlingFixedBill = null }) {
                                Text(text = "Cancel", color = TextDark)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (amt > 0.0) {
                                        viewModel.toggleFixedBillPaid(bill, customAmount = amt, customDateMillis = selectedSettleDateMillis)
                                        settlingFixedBill = null
                                    } else {
                                        Toast.makeText(context, "Please enter an amount > 0", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = when (bill.type) {
                                        TransactionType.INCOME -> SoftGreen
                                        TransactionType.ASSET -> SoftTeal
                                        TransactionType.TRANSFER -> AccentPurple
                                        else -> SoftGreen
                                    }
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(text = "Confirm & Settle", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (showSettleDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = selectedSettleDateMillis
                )

                DatePickerDialog(
                    onDismissRequest = { showSettleDatePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                datePickerState.selectedDateMillis?.let { pickedUtc ->
                                    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                        timeInMillis = pickedUtc
                                    }
                                    val localCal = Calendar.getInstance().apply {
                                        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                        set(Calendar.HOUR_OF_DAY, 12)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedSettleDateMillis = localCal.timeInMillis
                                }
                                showSettleDatePicker = false
                            }
                        ) {
                            Text("Select", fontWeight = FontWeight.Bold, color = AccentPurple)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSettleDatePicker = false }) {
                            Text("Cancel", color = TextDark)
                        }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
        }

        // Standardized Transfer Bottom Sheet (With Past Date Support)
        if (showTransferSheet) {
            AccountTransferDialog(
                accounts = accountsList,
                currencySymbol = userProfile.currencySymbol,
                onDismiss = { showTransferSheet = false },
                onTransfer = { from, to, amount, note, subtype, date ->
                    viewModel.executeInstantTransfer(
                        fromAccount = from,
                        toAccount = to,
                        amount = amount,
                        note = note,
                        subtype = subtype,
                        date = date
                    )
                    Toast.makeText(context, "Transferred ${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", amount)}", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Month / Year Picker Dialog
        if (showMonthPicker) {
            Dialog(onDismissRequest = { showMonthPicker = false }) {
                Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Select Timeframe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.selectYear(uiState.selectedYear - 1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Year")
                            }
                            Text(text = "${uiState.selectedYear}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            IconButton(onClick = { viewModel.selectYear(uiState.selectedYear + 1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (i in 0 until 4) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    for (j in 0 until 3) {
                                        val monthIdx = i * 3 + j + 1
                                        val isSelected = uiState.selectedMonth == monthIdx
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) AccentPurple else CanvasLight)
                                                .clickable {
                                                    viewModel.selectMonth(monthIdx)
                                                    showMonthPicker = false
                                                }
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = MONTH_NAMES[monthIdx - 1],
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = if (isSelected) Color.White else TextDark
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

        // Filter Bottom Sheet (With Date Presets & Custom Calendar Range)
        if (showFilterSheet) {
            FilterBottomSheet(
                currentFilter = filterCriteria,
                accountList = accountsList,
                onDismiss = { showFilterSheet = false },
                onApply = { type, acc, start, end ->
                    selectedTxFilterType = type
                    viewModel.updateFilter(type, acc, start, end)
                },
                onReset = {
                    selectedTxFilterType = null
                    viewModel.resetFilters()
                }
            )
        }

        // Add / Edit Transaction Sheet (With Today, Yesterday & Calendar Picker)
        if (showAddSheet) {
            AddTransactionBottomSheet(
                editingTransaction = editingTx,
                currencySymbol = userProfile.currencySymbol,
                accountList = accountsList,
                masterCategories = uiState.masterCategories,
                masterSubcategories = uiState.masterSubcategories,
                onDismiss = { showAddSheet = false },
                onSave = { id, title, amount, category, subcat, acc, type, date ->
                    val resolvedSubtype = if (type == TransactionType.TRANSFER) {
                        try {
                            TransferSubtype.valueOf(subcat)
                        } catch (_: Exception) {
                            TransferSubtype.NONE
                        }
                    } else TransferSubtype.NONE

                    viewModel.saveTransaction(
                        id = id,
                        title = title,
                        amount = amount,
                        category = category,
                        subcategory = subcat,
                        accountName = acc,
                        type = type,
                        date = date,
                        toAccountName = if (type == TransactionType.TRANSFER) editingTx?.toAccountName else null,
                        transferSubtype = resolvedSubtype
                    )
                    val cal = Calendar.getInstance().apply { timeInMillis = date }
                    val txMonth = cal.get(Calendar.MONTH) + 1
                    val txYear = cal.get(Calendar.YEAR)
                    if (txMonth != uiState.selectedMonth || txYear != uiState.selectedYear) {
                        Toast.makeText(context, "Logged to ${MONTH_NAMES[txMonth - 1]} $txYear ledger", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        // Add AutoPay Dialog (With Immediate Settlement Support)
        if (showAddFixedBill) {
            AddEditFixedBillDialog(
                currencySymbol = userProfile.currencySymbol,
                accountList = accountsList,
                masterCategories = uiState.masterCategories,
                masterSubcategories = uiState.masterSubcategories,
                onAddNewCategory = { name, type -> viewModel.addCategory(name, type) },
                onAddNewSubcategory = { parent, name, type -> viewModel.addSubcategory(parent, name, type) },
                onDismiss = { showAddFixedBill = false },
                onSave = { title, amt, cat, subcat, acc, toAcc, type, dueDay, isPaid, paidDate ->
                    viewModel.addFixedBill(title, amt, cat, subcat, acc, toAcc, type, dueDay, isPaid, paidDate)
                    if (isPaid) {
                        val cal = Calendar.getInstance().apply { timeInMillis = paidDate }
                        val txMonth = cal.get(Calendar.MONTH) + 1
                        val txYear = cal.get(Calendar.YEAR)
                        if (txMonth != uiState.selectedMonth || txYear != uiState.selectedYear) {
                            Toast.makeText(context, "Settled in ${MONTH_NAMES[txMonth - 1]} $txYear ledger", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }

        // Edit AutoPay Dialog
        editingFixedBill?.let { bill ->
            AddEditFixedBillDialog(
                initialBill = bill,
                currencySymbol = userProfile.currencySymbol,
                accountList = accountsList,
                masterCategories = uiState.masterCategories,
                masterSubcategories = uiState.masterSubcategories,
                onAddNewCategory = { name, type -> viewModel.addCategory(name, type) },
                onAddNewSubcategory = { parent, name, type -> viewModel.addSubcategory(parent, name, type) },
                onDismiss = { editingFixedBill = null },
                onSave = { title, amt, cat, subcat, acc, toAcc, type, dueDay, isPaid, paidDate ->
                    viewModel.updateFixedBill(bill.id, title, amt, cat, subcat, acc, toAcc, type, dueDay)
                    if (isPaid != bill.isPaid) {
                        viewModel.toggleFixedBillPaid(bill.copy(amount = amt), amt, paidDate)
                    }
                }
            )
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
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(13.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(tintColor)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = amount,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = tintColor
            )
        }
    }
}

@Composable
private fun PillarDualBarRow(
    title: String,
    planned: Double,
    actual: Double,
    currencySymbol: String,
    progressFraction: Float,
    barColor: Color,
    isDiscreet: Boolean,
    varianceText: String,
    isAlert: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isAlert) SoftRed.copy(alpha = 0.12f) else CanvasLight
                ) {
                    Text(
                        text = varianceText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isAlert) SoftRed else TextMuted,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isDiscreet) "Plan: ••••" else "Plan: $currencySymbol${String.format(Locale.US, "%,.0f", planned)}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isDiscreet) "Act: ••••" else "Act: $currencySymbol${String.format(Locale.US, "%,.0f", actual)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = if (isAlert) SoftRed else barColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progressFraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = if (isAlert) SoftRed else barColor,
            trackColor = barColor.copy(alpha = 0.15f)
        )
    }
}
