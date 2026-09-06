package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.CategoryPerformance
import com.example.myfin.ui.components.AppBottomDock
import com.example.myfin.ui.components.DockFabAction
import com.example.myfin.ui.components.NavigationTarget
import com.example.myfin.ui.components.rememberAutoScrollVisibilityConnection
import com.example.myfin.ui.theme.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private val EXPENSE_PRIORITY = listOf(
    "Debt & Financial Obligations",
    "Utilities & Living Bills",
    "Everyday Living",
    "Health & Medical",
    "Family & Home Support",
    "Work & Professional",
    "Leisure, Trips & Media",
    "General"
)

private val INCOME_PRIORITY = listOf(
    "Salary & Professional Inflow",
    "Reimbursements & Corporate Inflow",
    "Passive & Capital Drawdowns",
    "General"
)

private val ASSET_PRIORITY = listOf(
    "Investments & Wealth",
    "Liquid Reserves & Receivables",
    "General"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlannerScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToTaxonomy: () -> Unit = {},
    onNavigateToMonthly: () -> Unit = {},
    onNavigateToYearly: () -> Unit = {},
    onNavigateToVaults: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var selectedSegment by remember { mutableStateOf(TransactionType.EXPENSE) }
    var lockedCategoryAlert by remember { mutableStateOf<CategoryPerformance?>(null) }
    var editingCategory by remember { mutableStateOf<CategoryPerformance?>(null) }
    var showQuickSelectTargetSheet by remember { mutableStateOf(false) }
    var showCopyPlanDialog by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()

    // Timeline calculations for 5th-of-the-month discipline freeze
    val todayCal = remember { Calendar.getInstance() }
    val currentCalendarMonth = todayCal.get(Calendar.MONTH) + 1
    val currentCalendarYear = todayCal.get(Calendar.YEAR)
    val currentDayOfMonth = todayCal.get(Calendar.DAY_OF_MONTH)

    val isCurrentMonth = (uiState.selectedMonth == currentCalendarMonth) && (uiState.selectedYear == currentCalendarYear)
    val isPastMonth = (uiState.selectedYear < currentCalendarYear) ||
            (uiState.selectedYear == currentCalendarYear && uiState.selectedMonth < currentCalendarMonth)
    val isPastFifth = isCurrentMonth && (currentDayOfMonth > 5)

    // Financial Allocation Metrics (With Profile Base Income Fallback)
    val totalPlannedIncome = uiState.metrics.plannedIncome
    val effectiveIncomeBaseline = if (totalPlannedIncome > 0.0) totalPlannedIncome else userProfile.baseMonthlyIncome
    val totalPlannedExpenses = uiState.metrics.plannedExpenses
    val totalPlannedAssets = uiState.metrics.plannedAssets
    val totalAllocated = totalPlannedExpenses + totalPlannedAssets
    val unallocatedBuffer = effectiveIncomeBaseline - totalAllocated
    val allocationPercentage = if (effectiveIncomeBaseline > 0) {
        ((totalAllocated / effectiveIncomeBaseline) * 100).toInt()
    } else 0
    val isOverAllocated = effectiveIncomeBaseline > 0 && unallocatedBuffer < 0

    // Prioritized Category Resolution with type isolation to prevent collisions
    val displayedCategories = remember(uiState.masterCategories, uiState.categories, selectedSegment) {
        val masterList = uiState.masterCategories.filter { it.type == selectedSegment }
        val performanceMap = uiState.categories
            .filter { it.type == selectedSegment }
            .associateBy { it.category }

        val priorityList = when (selectedSegment) {
            TransactionType.EXPENSE -> EXPENSE_PRIORITY
            TransactionType.INCOME -> INCOME_PRIORITY
            TransactionType.ASSET -> ASSET_PRIORITY
            TransactionType.TRANSFER -> emptyList()
        }

        val allResolved = if (masterList.isNotEmpty()) {
            masterList.map { masterCat ->
                performanceMap[masterCat.name] ?: CategoryPerformance(
                    category = masterCat.name,
                    type = selectedSegment,
                    plannedAmount = 0.0,
                    actualAmount = 0.0
                )
            }
        } else {
            uiState.categories.filter { it.type == selectedSegment }
        }

        allResolved.sortedBy { cat ->
            val idx = priorityList.indexOf(cat.category)
            if (idx != -1) idx else 999
        }
    }

    // Dynamic Contextual FAB Actions per Selected Tab
    val fabActions = remember(selectedSegment, isPastMonth, isPastFifth) {
        when (selectedSegment) {
            TransactionType.EXPENSE -> listOf(
                DockFabAction(
                    icon = Icons.Default.Tune,
                    label = "Set Expense Target",
                    onClick = {
                        if (isPastMonth) {
                            Toast.makeText(context, "Historical months are read-only.", Toast.LENGTH_SHORT).show()
                        } else {
                            showQuickSelectTargetSheet = true
                        }
                    }
                ),
                DockFabAction(
                    icon = Icons.Default.Autorenew,
                    label = "Manage Fixed Bills",
                    onClick = { onNavigateToVaults() }
                ),
                DockFabAction(
                    icon = Icons.Default.History,
                    label = "Copy Last Month's Plan",
                    onClick = {
                        if (isPastMonth) {
                            Toast.makeText(context, "Cannot overwrite historical months.", Toast.LENGTH_SHORT).show()
                        } else {
                            showCopyPlanDialog = true
                        }
                    }
                )
            )
            TransactionType.INCOME -> listOf(
                DockFabAction(
                    icon = Icons.Default.TrendingUp,
                    label = "Set Income Target",
                    onClick = {
                        if (isPastMonth) {
                            Toast.makeText(context, "Historical months are read-only.", Toast.LENGTH_SHORT).show()
                        } else {
                            showQuickSelectTargetSheet = true
                        }
                    }
                ),
                DockFabAction(
                    icon = Icons.Default.History,
                    label = "Copy Last Month's Plan",
                    onClick = {
                        if (isPastMonth) {
                            Toast.makeText(context, "Cannot overwrite historical months.", Toast.LENGTH_SHORT).show()
                        } else {
                            showCopyPlanDialog = true
                        }
                    }
                )
            )
            TransactionType.ASSET -> listOf(
                DockFabAction(
                    icon = Icons.Default.PieChart,
                    label = "Set SIP Target",
                    onClick = {
                        if (isPastMonth) {
                            Toast.makeText(context, "Historical months are read-only.", Toast.LENGTH_SHORT).show()
                        } else {
                            showQuickSelectTargetSheet = true
                        }
                    }
                ),
                DockFabAction(
                    icon = Icons.Default.History,
                    label = "Copy Last Month's Plan",
                    onClick = {
                        if (isPastMonth) {
                            Toast.makeText(context, "Cannot overwrite historical months.", Toast.LENGTH_SHORT).show()
                        } else {
                            showCopyPlanDialog = true
                        }
                    }
                )
            )
            TransactionType.TRANSFER -> emptyList()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
            .nestedScroll(scrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. PINNED TOP HEADER WITH SHELF GRADIENT DISSOLVE
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasLight)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 6.dp, bottom = 8.dp)
                ) {
                    // Top Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
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

                        Text(
                            text = "Budget Planner",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TextDark
                        )

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { showMonthPicker = true },
                            shape = RoundedCornerShape(18.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                            shadowElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${MONTH_NAMES[uiState.selectedMonth - 1]} ${uiState.selectedYear}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inflow Baseline Hero Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
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
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "TOTAL MONTHLY INFLOW BASELINE",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = if (isOverAllocated) SoftRed.copy(alpha = 0.12f) else AccentPurple.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (effectiveIncomeBaseline == 0.0) {
                                            "Baseline Unset"
                                        } else if (isOverAllocated) {
                                            "Over-allocated ($allocationPercentage%)"
                                        } else {
                                            "$allocationPercentage% Allocated"
                                        },
                                        color = if (isOverAllocated) SoftRed else AccentPurple,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", effectiveIncomeBaseline)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark,
                                letterSpacing = (-0.4).sp
                            )

                            Text(
                                text = when {
                                    effectiveIncomeBaseline == 0.0 -> "Set expected income baseline in the Income tab to calculate allocation buffer"
                                    isOverAllocated -> "Deficit: Exceeds income by ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(unallocatedBuffer))}"
                                    else -> "Unallocated buffer: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", unallocatedBuffer)} left to assign"
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isOverAllocated) SoftRed else TextMuted
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Dual Segment Allocation Progress Bar
                            val expenseFraction = if (effectiveIncomeBaseline > 0) (totalPlannedExpenses / effectiveIncomeBaseline).toFloat().coerceIn(0f, 1f) else 0f
                            val assetFraction = if (effectiveIncomeBaseline > 0) (totalPlannedAssets / effectiveIncomeBaseline).toFloat().coerceIn(0f, 1f) else 0f

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(BorderLight.copy(alpha = 0.6f))
                            ) {
                                if (expenseFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(expenseFraction.coerceAtLeast(0.001f))
                                            .fillMaxHeight()
                                            .background(SoftRed)
                                    )
                                }
                                if (assetFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(assetFraction.coerceAtLeast(0.001f))
                                            .fillMaxHeight()
                                            .background(SoftTeal)
                                    )
                                }
                                val remainder = (1f - (expenseFraction + assetFraction)).coerceAtLeast(0f)
                                if (remainder > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(remainder)
                                            .fillMaxHeight()
                                            .background(Color.Transparent)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Allocation Legends
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(SoftRed))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expenses: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalPlannedExpenses)}", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(SoftTeal))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Assets/SIP: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalPlannedAssets)}", fontSize = 10.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Flow Segment Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(BorderLight.copy(alpha = 0.5f))
                            .padding(2.5.dp)
                    ) {
                        listOf(
                            Triple(TransactionType.EXPENSE, "Expenses", SoftRed),
                            Triple(TransactionType.INCOME, "Income", SoftGreen),
                            Triple(TransactionType.ASSET, "Assets / SIP", SoftTeal)
                        ).forEach { (type, label, color) ->
                            val isSelected = selectedSegment == type
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CardWhite else Color.Transparent)
                                    .clickable { selectedSegment = type }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isSelected) color else TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Smooth Dissolve Shelf Placed Below Segment Bar
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

            // 2. SCROLLABLE CATEGORY LIMITS LIST
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 125.dp)
            ) {
                if (displayedCategories.isEmpty()) {
                    item(key = "empty_categories") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No categories in this segment", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
                    items(displayedCategories, key = { "${it.type.name}_${it.category}" }) { cat ->
                        val committedAmount = remember(uiState.fixedBills, cat) {
                            uiState.fixedBills.filter { it.category == cat.category && it.type == cat.type }.sumOf { it.amount }
                        }
                        val isLocked = isPastMonth || (isCurrentMonth && isPastFifth && cat.plannedAmount > 0.0)

                        BudgetCategoryCleanCard(
                            category = cat,
                            committedAutoPay = committedAmount,
                            currencySymbol = userProfile.currencySymbol,
                            isLocked = isLocked,
                            onClick = {
                                if (isPastMonth) {
                                    Toast.makeText(context, "Historical months cannot be modified.", Toast.LENGTH_SHORT).show()
                                } else if (isCurrentMonth && isPastFifth && cat.plannedAmount > 0.0) {
                                    lockedCategoryAlert = cat
                                } else {
                                    editingCategory = cat
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                    }
                }
            }
        }

        // 3. FLOATING BOTTOM DOCK WITH TAB-AWARE FAB (includes integrated animated gradient scrim)
        AppBottomDock(
            currentSelection = NavigationTarget.BUDGET_PLANNER,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.MONTHLY_VIEW -> onNavigateToMonthly()
                    NavigationTarget.DATA_SET -> onNavigateToTaxonomy()
                    NavigationTarget.VAULT_ACCOUNTS -> onNavigateToVaults()
                    NavigationTarget.REPORTS_ANALYTICS -> onNavigateToAnalytics()
                    NavigationTarget.YEARLY_VIEW -> onNavigateToYearly()
                    NavigationTarget.BUDGET_PLANNER -> { /* Active */ }
                    else -> {}
                }
            },
            fabActions = fabActions,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        )

        // Month / Year Picker Modal Dialog
        if (showMonthPicker) {
            Dialog(onDismissRequest = { showMonthPicker = false }) {
                Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(text = "Select Planning Timeframe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
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

        // Alert: Locked Past the 5th
        lockedCategoryAlert?.let { cat ->
            AlertDialog(
                onDismissRequest = { lockedCategoryAlert = null },
                title = { Text("Budget Ceiling Frozen", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        "Budget limits are locked after the 5th of the month. Your ceiling of ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cat.plannedAmount)} for '${cat.category}' is frozen to maintain month-end discipline."
                    )
                },
                confirmButton = {
                    TextButton(onClick = { lockedCategoryAlert = null }) {
                        Text("Understood", fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                }
            )
        }

        // Alert: Copy Last Month's Plan Confirmation
        if (showCopyPlanDialog) {
            AlertDialog(
                onDismissRequest = { showCopyPlanDialog = false },
                title = { Text("Copy Last Month's Plan?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        "This will copy all planned category limits and baseline goals from the previous month into ${MONTH_NAMES[uiState.selectedMonth - 1]} ${uiState.selectedYear}.",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.copyPreviousMonthBudget { count ->
                                Toast.makeText(context, "$count budget targets synced from previous month!", Toast.LENGTH_SHORT).show()
                            }
                            showCopyPlanDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Copy Plan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCopyPlanDialog = false }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        // Sheet: Quick Pick Target from FAB
        if (showQuickSelectTargetSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showQuickSelectTargetSheet = false },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp),
                        shape = CircleShape,
                        color = BorderLight
                    ) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Select ${selectedSegment.name.lowercase().replaceFirstChar { it.uppercase() }} Category",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Choose a category to set or modify its monthly target ceiling", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(displayedCategories, key = { "${it.type.name}_${it.category}" }) { cat ->
                            val isFrozen = isCurrentMonth && isPastFifth && (cat.plannedAmount > 0.0)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clickable {
                                        showQuickSelectTargetSheet = false
                                        if (isFrozen) {
                                            lockedCategoryAlert = cat
                                        } else {
                                            editingCategory = cat
                                        }
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = CanvasLight,
                                border = BorderStroke(0.6.dp, BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.category,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isFrozen) TextMuted else TextDark
                                    )
                                    if (isFrozen) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Lock, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Frozen", fontSize = 11.sp, color = TextMuted)
                                        }
                                    } else {
                                        Text(
                                            text = if (cat.plannedAmount > 0) "${userProfile.currencySymbol}${cat.plannedAmount.toInt()}" else "Unset",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (cat.plannedAmount > 0) AccentPurple else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        // Sheet: Set Category Target Budget
        editingCategory?.let { cat ->
            val committedAutoPay = remember(uiState.fixedBills, cat) {
                uiState.fixedBills.filter { it.category == cat.category && it.type == cat.type }.sumOf { it.amount }
            }
            var customAmountText by remember(cat) {
                mutableStateOf(
                    if (cat.plannedAmount > 0) {
                        if (cat.plannedAmount % 1.0 == 0.0) cat.plannedAmount.toLong().toString()
                        else cat.plannedAmount.toString()
                    } else ""
                )
            }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { editingCategory = null },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(
                        modifier = Modifier
                            .padding(vertical = 10.dp)
                            .width(40.dp)
                            .height(4.dp),
                        shape = CircleShape,
                        color = BorderLight
                    ) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Set Budget: ${cat.category}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = TextDark
                        )
                        IconButton(
                            onClick = { editingCategory = null },
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(CanvasLight)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark, modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (committedAutoPay > 0) "Committed AutoPay Floor: ${userProfile.currencySymbol}${committedAutoPay.toInt()}" else "Configure monthly baseline limit for this category",
                        fontSize = 11.5.sp,
                        color = if (committedAutoPay > 0) AccentPurple else TextMuted,
                        fontWeight = if (committedAutoPay > 0) FontWeight.SemiBold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split('.')
                            customAmountText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                        },
                        label = { Text("Planned Amount (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = BorderLight
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Increment Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(500, 1000, 5000).forEach { inc ->
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable {
                                        val current = customAmountText.toDoubleOrNull() ?: 0.0
                                        val sum = current + inc
                                        customAmountText = if (sum % 1.0 == 0.0) sum.toLong().toString() else String.format(Locale.US, "%.2f", sum)
                                    },
                                color = CanvasLight,
                                border = BorderStroke(0.6.dp, BorderLight)
                            ) {
                                Text(
                                    text = "+${userProfile.currencySymbol}$inc",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    modifier = Modifier.padding(vertical = 7.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val resetTarget = if (committedAutoPay > 0) committedAutoPay else 0.0
                                viewModel.updateCategoryBudget(cat.category, resetTarget, cat.type)
                                editingCategory = null
                            },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed)
                        ) {
                            Text(
                                text = if (committedAutoPay > 0) "Reset to Floor" else "Reset",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = {
                                val amt = customAmountText.toDoubleOrNull() ?: 0.0
                                if (committedAutoPay > 0 && amt < committedAutoPay) {
                                    Toast.makeText(context, "Budget cannot be lower than committed AutoPay (${userProfile.currencySymbol}${committedAutoPay.toInt()})", Toast.LENGTH_LONG).show()
                                } else {
                                    viewModel.updateCategoryBudget(cat.category, amt, cat.type)
                                    editingCategory = null
                                }
                            },
                            modifier = Modifier.weight(1.3f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                        ) {
                            Text("Save Plan", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun BudgetCategoryCleanCard(
    category: CategoryPerformance,
    committedAutoPay: Double,
    currencySymbol: String,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    val typeColor = when (category.type) {
        TransactionType.INCOME -> SoftGreen
        TransactionType.EXPENSE -> SoftRed
        TransactionType.ASSET -> SoftTeal
        TransactionType.TRANSFER -> AccentPurple
    }

    val subtitleText = when {
        committedAutoPay > 0.0 -> "AutoPay committed: $currencySymbol${String.format(Locale.US, "%,.0f", committedAutoPay)}"
        category.plannedAmount > 0.0 -> "Actual spend: $currencySymbol${String.format(Locale.US, "%,.0f", category.actualAmount)}"
        else -> "Tap to set budget target"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category.category.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = typeColor
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = category.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isLocked) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = TextMuted.copy(alpha = 0.65f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitleText,
                        fontSize = 11.sp,
                        color = if (committedAutoPay > 0.0) AccentPurple else TextMuted,
                        fontWeight = if (committedAutoPay > 0.0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "$currencySymbol${String.format(Locale.US, "%,.0f", category.plannedAmount)}",
                fontWeight = FontWeight.Black,
                fontSize = 14.5.sp,
                color = if (category.plannedAmount > 0) TextDark else TextMuted
            )
        }
    }
}
