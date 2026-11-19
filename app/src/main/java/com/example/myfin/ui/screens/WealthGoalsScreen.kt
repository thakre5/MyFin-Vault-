@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)

package com.example.myfin.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.*
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private val ASSET_CLASSES = listOf(
    "All", "Mutual Fund", "Equity", "ETF", "FD/RD", "Gold", "PPF/EPF", "Other"
)

@Composable
fun WealthGoalsScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onSelectNavTarget: (NavigationTarget) -> Unit
) {
    val holdings by viewModel.investmentHoldings.collectAsState()
    val goals by viewModel.goalProgressList.collectAsState()
    val monthlyState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val currencySymbol = userProfile.currencySymbol.ifBlank { "₹" }
    var isDiscreetMode by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val (isDockVisible, nestedScrollConnection) = rememberAutoScrollVisibilityConnection()

    var showAddInvestmentSheet by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<InvestmentEntity?>(null) }
    var quickUpdateHolding by remember { mutableStateOf<InvestmentHoldingUiModel?>(null) }

    var showAddGoalSheet by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }

    val assetSubcategories = remember(monthlyState.masterSubcategories) {
        monthlyState.masterSubcategories
            .filter { it.type == TransactionType.ASSET }
            .map { it.name }
            .distinct()
    }
    val activeAccountNames = remember(monthlyState.activeAccounts) {
        monthlyState.activeAccounts.map { it.accountName }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
            .nestedScroll(nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CardWhite)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextDark, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Investments & Goals",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark
                        )
                        Text(
                            text = "Offline Portfolio & Milestone Tracker",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = { isDiscreetMode = !isDiscreetMode },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isDiscreetMode) Primary50 else CardWhite)
                ) {
                    Icon(
                        imageVector = if (isDiscreetMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Discreet Mode",
                        tint = if (isDiscreetMode) Primary600 else TextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> InvestmentsPortfolioTab(
                        holdings = holdings,
                        currencySymbol = currencySymbol,
                        isDiscreet = isDiscreetMode,
                        onAddInvestment = {
                            editingInvestment = null
                            showAddInvestmentSheet = true
                        },
                        onEditInvestment = { inv ->
                            editingInvestment = inv
                            showAddInvestmentSheet = true
                        },
                        onQuickUpdateValuation = { model ->
                            quickUpdateHolding = model
                        }
                    )

                    1 -> FinancialGoalsTab(
                        goals = goals,
                        currencySymbol = currencySymbol,
                        isDiscreet = isDiscreetMode,
                        onAddGoal = {
                            editingGoal = null
                            showAddGoalSheet = true
                        },
                        onEditGoal = { goal ->
                            editingGoal = goal
                            showAddGoalSheet = true
                        },
                        onToggleComplete = { goal ->
                            viewModel.toggleGoalCompleted(goal)
                        }
                    )
                }
            }
        }

        // Floating Pager Indicator
        FloatingPagerIndicator(
            pagerState = pagerState,
            pageTitles = listOf("Portfolio", "Goals"),
            isVisible = isDockVisible.value,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 80.dp)
        )

        // Bottom Dock
        AppBottomDock(
            currentSelection = NavigationTarget.WEALTH_GOALS,
            onSelectTarget = onSelectNavTarget,
            isVisible = isDockVisible.value,
            fabActions = listOf(
                DockFabAction(
                    icon = Icons.AutoMirrored.Filled.TrendingUp,
                    label = "Add Investment",
                    onClick = {
                        editingInvestment = null
                        showAddInvestmentSheet = true
                    }
                ),
                DockFabAction(
                    icon = Icons.Default.Flag,
                    label = "Add Financial Goal",
                    onClick = {
                        editingGoal = null
                        showAddGoalSheet = true
                    }
                )
            ),
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showAddInvestmentSheet) {
        AddEditInvestmentSheet(
            initialInvestment = editingInvestment,
            currencySymbol = currencySymbol,
            assetSubcategories = assetSubcategories,
            onDismiss = {
                showAddInvestmentSheet = false
                editingInvestment = null
            },
            onSave = { inv ->
                viewModel.saveInvestment(inv)
                showAddInvestmentSheet = false
                editingInvestment = null
            },
            onDelete = { inv ->
                viewModel.deleteInvestment(inv)
                showAddInvestmentSheet = false
                editingInvestment = null
            }
        )
    }

    quickUpdateHolding?.let { model ->
        QuickUpdateValuationDialog(
            holding = model,
            currencySymbol = currencySymbol,
            onDismiss = { quickUpdateHolding = null },
            onSave = { newVal ->
                viewModel.updateInvestmentValuation(model.entity, newVal)
                quickUpdateHolding = null
            }
        )
    }

    if (showAddGoalSheet) {
        AddEditGoalSheet(
            initialGoal = editingGoal,
            currencySymbol = currencySymbol,
            activeAccounts = activeAccountNames,
            holdings = holdings,
            onDismiss = {
                showAddGoalSheet = false
                editingGoal = null
            },
            onSave = { goal ->
                viewModel.saveGoal(goal)
                showAddGoalSheet = false
                editingGoal = null
            },
            onDelete = { goal ->
                viewModel.deleteGoal(goal)
                showAddGoalSheet = false
                editingGoal = null
            }
        )
    }
}

@Composable
private fun InvestmentsPortfolioTab(
    holdings: List<InvestmentHoldingUiModel>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onAddInvestment: () -> Unit,
    onEditInvestment: (InvestmentEntity) -> Unit,
    onQuickUpdateValuation: (InvestmentHoldingUiModel) -> Unit
) {
    var selectedAssetClass by remember { mutableStateOf("All") }

    val activeHoldings = remember(holdings) { holdings.filter { !it.entity.isArchived } }
    val filteredHoldings = remember(activeHoldings, selectedAssetClass) {
        if (selectedAssetClass == "All") activeHoldings
        else activeHoldings.filter { it.entity.assetClass.equals(selectedAssetClass, ignoreCase = true) }
    }

    val totalInvested = remember(activeHoldings) { activeHoldings.sumOf { it.totalInvested } }
    val totalValue = remember(activeHoldings) { activeHoldings.sumOf { it.currentValue } }
    val totalPnL = totalValue - totalInvested
    val totalReturnPct = if (totalInvested > 0.0) (totalPnL / totalInvested) * 100.0 else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Portfolio Hero Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Primary800
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Primary900, Primary700, Primary600)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL PORTFOLIO VALUE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary200,
                                letterSpacing = 0.8.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (totalPnL >= 0.0) Success500.copy(alpha = 0.22f) else Error500.copy(alpha = 0.22f)
                            ) {
                                val sign = if (totalPnL >= 0.0) "+" else ""
                                Text(
                                    text = if (isDiscreet) "••••" else "$sign${String.format(Locale.US, "%.1f", totalReturnPct)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isDiscreet) "••••••" else "$currencySymbol${String.format(Locale.US, "%,.2f", totalValue)}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Invested Capital", fontSize = 10.5.sp, color = Primary200)
                                Text(
                                    text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", totalInvested)}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Unrealized P&L", fontSize = 10.5.sp, color = Primary200)
                                val pnlPrefix = if (totalPnL >= 0.0) "+" else "-"
                                Text(
                                    text = if (isDiscreet) "••••" else "$pnlPrefix$currencySymbol${String.format(Locale.US, "%,.0f", abs(totalPnL))}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (totalPnL >= 0.0) Success100 else Error100
                                )
                            }
                        }
                    }
                }
            }
        }

        // Asset Class Filter Row
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ASSET_CLASSES) { assetClass ->
                    val isSel = selectedAssetClass == assetClass
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedAssetClass = assetClass },
                        label = {
                            Text(
                                text = assetClass,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary50,
                            selectedLabelColor = Primary700
                        )
                    )
                }
            }
        }

        if (filteredHoldings.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Primary600,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Track Old & New Investments",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Log historical Mutual Funds, Stocks, FDs, or Gold without affecting your current bank balances, and link monthly SIPs automatically.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddInvestment,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add First Holding", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else {
            items(filteredHoldings, key = { it.entity.id }) { model ->
                val inv = model.entity
                val isPositive = model.unrealizedPnL >= 0.0

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onEditInvestment(inv) },
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = inv.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(shape = RoundedCornerShape(5.dp), color = Primary50) {
                                        Text(
                                            text = inv.assetClass,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Primary700,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    if (inv.institution.isNotBlank()) {
                                        Text(
                                            text = inv.institution,
                                            fontSize = 10.5.sp,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", model.currentValue)}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark
                                )
                                val pnlSign = if (isPositive) "+" else "-"
                                Text(
                                    text = if (isDiscreet) "••••" else "$pnlSign$currencySymbol${String.format(Locale.US, "%,.0f", abs(model.unrealizedPnL))} (${String.format(Locale.US, "%.1f", model.returnPct)}%)",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPositive) Success700 else Error700
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.6.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isDiscreet) "Invested: ••••" else "Invested: $currencySymbol${String.format(Locale.US, "%,.0f", model.totalInvested)}",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.Medium
                                )
                                if (!inv.linkedSubcategory.isNullOrBlank()) {
                                    Text(
                                        text = "Auto-synced SIP: ${inv.linkedSubcategory}",
                                        fontSize = 9.5.sp,
                                        color = SoftTeal,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onQuickUpdateValuation(model) },
                                shape = RoundedCornerShape(8.dp),
                                color = Primary50,
                                border = BorderStroke(0.6.dp, Primary200)
                            ) {
                                Text(
                                    text = "Update Value",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary700,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FinancialGoalsTab(
    goals: List<GoalProgressUiModel>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onAddGoal: () -> Unit,
    onEditGoal: (GoalEntity) -> Unit,
    onToggleComplete: (GoalEntity) -> Unit
) {
    val totalTarget = remember(goals) { goals.sumOf { it.entity.targetAmount } }
    val totalSaved = remember(goals) { goals.sumOf { it.currentSaved } }
    val overallPct = if (totalTarget > 0.0) ((totalSaved / totalTarget) * 100.0).toInt().coerceIn(0, 100) else 0
    val dateFmt = remember { SimpleDateFormat("MMM yyyy", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Goals Hero Card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = Primary800
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Neutral900, Primary900, Primary700)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "FINANCIAL MILESTONES",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary200,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "$overallPct% Funded",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = if (isDiscreet) "••••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", totalSaved)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = if (isDiscreet) "of •••••• target" else "of $currencySymbol${String.format(Locale.US, "%,.0f", totalTarget)} combined target",
                            fontSize = 11.5.sp,
                            color = Primary200
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LinearProgressIndicator(
                            progress = { overallPct / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape),
                            color = SoftTeal,
                            trackColor = Color.White.copy(alpha = 0.18f)
                        )
                    }
                }
            }
        }

        if (goals.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = null,
                            tint = Primary600,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Set Your First Financial Goal",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Link a Vault account or Investment holding to track live progress and calculate your exact required monthly savings pace.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onAddGoal,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Goal", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else {
            items(goals, key = { it.entity.id }) { model ->
                val goal = model.entity
                val pct = (model.progressFraction * 100f).toInt()
                val isDone = goal.isCompleted || pct >= 100

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onEditGoal(goal) },
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, if (isDone) Success500.copy(alpha = 0.5f) else BorderLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (isDone) Success100 else Primary50)
                                        .clickable { onToggleComplete(goal) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isDone) Icons.Default.Check else Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = if (isDone) Success700 else Primary600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = goal.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Target: ${dateFmt.format(Date(goal.targetDateMillis))} (${model.monthsRemaining}m left)",
                                        fontSize = 10.5.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isDone) Success100 else Primary50
                            ) {
                                Text(
                                    text = "$pct%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isDone) Success700 else Primary700,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { model.progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = if (isDone) Success500 else Primary600,
                            trackColor = Neutral200
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isDiscreet) "•••• / ••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", model.currentSaved)} / $currencySymbol${String.format(Locale.US, "%,.0f", goal.targetAmount)}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )

                            if (!isDone && model.requiredMonthlyContribution > 0.0) {
                                Text(
                                    text = if (isDiscreet) "Need ••••/mo" else "Need $currencySymbol${String.format(Locale.US, "%,.0f", model.requiredMonthlyContribution)}/mo",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Primary600
                                )
                            } else if (isDone) {
                                Text(
                                    text = "Goal Achieved",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Success700
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickUpdateValuationDialog(
    holding: InvestmentHoldingUiModel,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val context = LocalContext.current
    var valText by remember(holding.entity.id) {
        mutableStateOf(
            if (holding.currentValue % 1.0 == 0.0) holding.currentValue.toLong().toString()
            else holding.currentValue.toString()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("Update Valuation", fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column {
                Text(
                    text = "${holding.entity.name} (Invested: $currencySymbol${String.format(Locale.US, "%,.0f", holding.totalInvested)})",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = valText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        val parts = filtered.split('.')
                        valText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                    },
                    label = { Text("Current Market Value ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = valText.toDoubleOrNull()
                if (parsed == null || parsed < 0.0) {
                    Toast.makeText(context, "Enter a valid valuation", Toast.LENGTH_SHORT).show()
                } else {
                    onSave(parsed)
                }
            }) {
                Text("Save", fontWeight = FontWeight.Bold, color = Primary600)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
private fun AddEditInvestmentSheet(
    initialInvestment: InvestmentEntity?,
    currencySymbol: String,
    assetSubcategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (InvestmentEntity) -> Unit,
    onDelete: (InvestmentEntity) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val invKey = initialInvestment?.id ?: -1L

    var name by remember(invKey) { mutableStateOf(initialInvestment?.name.orEmpty()) }
    var assetClass by remember(invKey) { mutableStateOf(initialInvestment?.assetClass ?: "Mutual Fund") }
    var openingInvestedText by remember(invKey) {
        mutableStateOf(
            initialInvestment?.openingInvestedAmount?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }.orEmpty()
        )
    }
    var currentValText by remember(invKey) {
        mutableStateOf(
            initialInvestment?.currentValuation?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }.orEmpty()
        )
    }
    var linkedSubcategory by remember(invKey) { mutableStateOf(initialInvestment?.linkedSubcategory) }
    var institution by remember(invKey) { mutableStateOf(initialInvestment?.institution.orEmpty()) }
    var notes by remember(invKey) { mutableStateOf(initialInvestment?.notes.orEmpty()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialInvestment == null) "Add Investment Holding" else "Edit Holding",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TextDark
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Holding Name (e.g. Nifty Next 50 SIP)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("Asset Class", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(ASSET_CLASSES.drop(1)) { cls ->
                    val isSel = assetClass.equals(cls, ignoreCase = true)
                    FilterChip(
                        selected = isSel,
                        onClick = { assetClass = cls },
                        label = { Text(cls, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary50,
                            selectedLabelColor = Primary700
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = openingInvestedText,
                    onValueChange = { input ->
                        val f = input.filter { it.isDigit() || it == '.' }
                        openingInvestedText = f
                    },
                    label = { Text("Prior Invested ($currencySymbol)", fontSize = 11.5.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = currentValText,
                    onValueChange = { input ->
                        val f = input.filter { it.isDigit() || it == '.' }
                        currentValText = f
                    },
                    label = { Text("Current Value ($currencySymbol)", fontSize = 11.5.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            if (assetSubcategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Auto-Sync Monthly SIPs (Optional)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = linkedSubcategory == null,
                            onClick = { linkedSubcategory = null },
                            label = { Text("None (Manual)", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(assetSubcategories) { sub ->
                        val isSel = linkedSubcategory.equals(sub, ignoreCase = true)
                        FilterChip(
                            selected = isSel,
                            onClick = { linkedSubcategory = sub },
                            label = { Text(sub, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftTealLight,
                                selectedLabelColor = SoftTeal
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = institution,
                onValueChange = { institution = it },
                label = { Text("Broker / Bank (Optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes / Folio (Optional)") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initialInvestment != null) {
                    OutlinedButton(
                        onClick = { onDelete(initialInvestment) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error700),
                        border = BorderStroke(1.dp, Error500.copy(alpha = 0.5f))
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        val trimmedName = name.trim()
                        if (trimmedName.isBlank()) {
                            Toast.makeText(context, "Please enter a holding name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val opening = openingInvestedText.toDoubleOrNull() ?: 0.0
                        val current = currentValText.toDoubleOrNull() ?: opening
                        onSave(
                            InvestmentEntity(
                                id = initialInvestment?.id ?: 0L,
                                name = trimmedName,
                                assetClass = assetClass,
                                openingInvestedAmount = opening,
                                currentValuation = current,
                                linkedSubcategory = linkedSubcategory,
                                institution = institution.trim(),
                                notes = notes.trim()
                            )
                        )
                    },
                    modifier = Modifier.weight(1.4f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                ) {
                    Text(
                        text = if (initialInvestment == null) "Save Holding" else "Update Holding",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AddEditGoalSheet(
    initialGoal: GoalEntity?,
    currencySymbol: String,
    activeAccounts: List<String>,
    holdings: List<InvestmentHoldingUiModel>,
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit,
    onDelete: (GoalEntity) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val goalKey = initialGoal?.id ?: -1L

    var title by remember(goalKey) { mutableStateOf(initialGoal?.title.orEmpty()) }
    var targetText by remember(goalKey) {
        mutableStateOf(
            initialGoal?.targetAmount?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }.orEmpty()
        )
    }
    var initialSavedText by remember(goalKey) {
        mutableStateOf(
            initialGoal?.initialSavedAmount?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }.orEmpty()
        )
    }
    var targetDateMillis by remember(goalKey) {
        mutableStateOf(
            initialGoal?.targetDateMillis ?: Calendar.getInstance().apply { add(Calendar.YEAR, 1) }.timeInMillis
        )
    }
    var linkedAccount by remember(goalKey) { mutableStateOf(initialGoal?.linkedAccountName) }
    var linkedInvestmentId by remember(goalKey) { mutableStateOf(initialGoal?.linkedInvestmentId) }

    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialGoal == null) "Create Financial Goal" else "Edit Goal",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TextDark
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Goal Name (e.g. Emergency Fund, New Bike)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Target ($currencySymbol)", fontSize = 11.5.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = initialSavedText,
                    onValueChange = { initialSavedText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Saved Manually ($currencySymbol)", fontSize = 11.5.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text("Target Horizon (${dateFmt.format(Date(targetDateMillis))})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(6 to "6 Months", 12 to "1 Year", 36 to "3 Years", 60 to "5 Years").forEach { (months, label) ->
                    item {
                        FilterChip(
                            selected = false,
                            onClick = {
                                targetDateMillis = Calendar.getInstance().apply { add(Calendar.MONTH, months) }.timeInMillis
                            },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            if (activeAccounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Link Vault Account (Optional)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = linkedAccount == null,
                            onClick = { linkedAccount = null },
                            label = { Text("None", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(activeAccounts) { acc ->
                        val isSel = linkedAccount.equals(acc, ignoreCase = true)
                        FilterChip(
                            selected = isSel,
                            onClick = { linkedAccount = acc },
                            label = { Text(acc, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary50,
                                selectedLabelColor = Primary700
                            )
                        )
                    }
                }
            }

            if (holdings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Link Investment Holding (Optional)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = linkedInvestmentId == null,
                            onClick = { linkedInvestmentId = null },
                            label = { Text("None", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(holdings) { model ->
                        val isSel = linkedInvestmentId == model.entity.id
                        FilterChip(
                            selected = isSel,
                            onClick = { linkedInvestmentId = model.entity.id },
                            label = { Text(model.entity.name, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftTealLight,
                                selectedLabelColor = SoftTeal
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (initialGoal != null) {
                    OutlinedButton(
                        onClick = { onDelete(initialGoal) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error700),
                        border = BorderStroke(1.dp, Error500.copy(alpha = 0.5f))
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = {
                        val trimmed = title.trim()
                        val target = targetText.toDoubleOrNull() ?: 0.0
                        if (trimmed.isBlank() || target <= 0.0) {
                            Toast.makeText(context, "Enter a goal name and valid target amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSave(
                            GoalEntity(
                                id = initialGoal?.id ?: 0L,
                                title = trimmed,
                                targetAmount = target,
                                initialSavedAmount = initialSavedText.toDoubleOrNull() ?: 0.0,
                                targetDateMillis = targetDateMillis,
                                linkedAccountName = linkedAccount,
                                linkedInvestmentId = linkedInvestmentId,
                                isCompleted = initialGoal?.isCompleted ?: false
                            )
                        )
                    },
                    modifier = Modifier.weight(1.4f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                ) {
                    Text(
                        text = if (initialGoal == null) "Save Goal" else "Update Goal",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
