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
import com.example.myfin.ui.UnlinkedLedgerAssetUiModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

private val ASSET_CLASSES = listOf(
    "All", "Mutual Fund", "Equity", "ETF", "FD/RD", "Gold", "PPF/EPF", "Other"
)

private fun assetClassColor(assetClass: String): Color = when (assetClass.lowercase(Locale.ROOT)) {
    "mutual fund" -> Primary600
    "equity" -> Primary400
    "etf" -> SoftTeal
    "fd/rd" -> Success500
    "gold" -> Warning500
    "ppf/epf" -> Neutral600
    else -> Neutral400
}

@Composable
fun WealthGoalsScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onSelectNavTarget: (NavigationTarget) -> Unit
) {
    val holdings by viewModel.investmentHoldings.collectAsState()
    val unlinkedAssets by viewModel.unlinkedLedgerAssets.collectAsState()
    val goals by viewModel.goalProgressList.collectAsState()
    val monthlyState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val avgMonthlySpend by viewModel.averageMonthlySpend.collectAsState()

    val currencySymbol = userProfile.currencySymbol.ifBlank { "₹" }
    var isDiscreetMode by remember { mutableStateOf(false) }

    val pagerState = rememberPagerState(pageCount = { 2 })
    val (isDockVisible, nestedScrollConnection) = rememberAutoScrollVisibilityConnection()

    var showAddInvestmentSheet by remember { mutableStateOf(false) }
    var editingInvestment by remember { mutableStateOf<InvestmentEntity?>(null) }
    var prefillUnlinkedAsset by remember { mutableStateOf<UnlinkedLedgerAssetUiModel?>(null) }
    var quickUpdateHolding by remember { mutableStateOf<InvestmentHoldingUiModel?>(null) }

    var showAddGoalSheet by remember { mutableStateOf(false) }
    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var topUpGoalTarget by remember { mutableStateOf<GoalProgressUiModel?>(null) }

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
                            text = "Unified Portfolio & Milestone Tracker",
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
                        unlinkedAssets = unlinkedAssets,
                        currencySymbol = currencySymbol,
                        isDiscreet = isDiscreetMode,
                        onAddInvestment = {
                            editingInvestment = null
                            prefillUnlinkedAsset = null
                            showAddInvestmentSheet = true
                        },
                        onEditInvestment = { inv ->
                            editingInvestment = inv
                            prefillUnlinkedAsset = null
                            showAddInvestmentSheet = true
                        },
                        onTrackUnlinkedAsset = { asset ->
                            editingInvestment = null
                            prefillUnlinkedAsset = asset
                            showAddInvestmentSheet = true
                        },
                        onQuickUpdateValuation = { model ->
                            quickUpdateHolding = model
                        }
                    )

                    1 -> FinancialGoalsTab(
                        goals = goals,
                        holdings = holdings,
                        fortressFdBalance = monthlyState.fortressFdBalance,
                        fortressTarget = monthlyState.fortressTarget,
                        fortressProgressPct = monthlyState.fortressProgressPercentage,
                        fortressMonths = userProfile.fortressEmergencyMonths.takeIf { it > 0 } ?: 6,
                        isManualFortressTarget = userProfile.fortressManualTarget > 0.0,
                        avgMonthlyBurn = avgMonthlySpend,
                        currencySymbol = currencySymbol,
                        isDiscreet = isDiscreetMode,
                        onSelectFortressMonths = { months ->
                            viewModel.updateFortressEmergencyMonths(months)
                        },
                        onAddGoal = {
                            editingGoal = null
                            showAddGoalSheet = true
                        },
                        onEditGoal = { goal ->
                            editingGoal = goal
                            showAddGoalSheet = true
                        },
                        onTopUpGoal = { model ->
                            topUpGoalTarget = model
                        },
                        onToggleComplete = { goal ->
                            viewModel.toggleGoalCompleted(goal)
                        }
                    )
                }
            }
        }

        // Floating Pager Indicator (Bottom-Left to match other screens)
        FloatingPagerIndicator(
            pagerState = pagerState,
            pageTitles = listOf("Portfolio", "Goals"),
            isVisible = isDockVisible.value,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 80.dp)
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
                        prefillUnlinkedAsset = null
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
            prefillAsset = prefillUnlinkedAsset,
            currencySymbol = currencySymbol,
            assetSubcategories = assetSubcategories,
            onDismiss = {
                showAddInvestmentSheet = false
                editingInvestment = null
                prefillUnlinkedAsset = null
            },
            onSave = { inv ->
                viewModel.saveInvestment(inv)
                showAddInvestmentSheet = false
                editingInvestment = null
                prefillUnlinkedAsset = null
            },
            onDelete = { inv ->
                viewModel.deleteInvestment(inv)
                showAddInvestmentSheet = false
                editingInvestment = null
                prefillUnlinkedAsset = null
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

    topUpGoalTarget?.let { model ->
        QuickTopUpGoalDialog(
            goalModel = model,
            currencySymbol = currencySymbol,
            onDismiss = { topUpGoalTarget = null },
            onSave = { added ->
                viewModel.topUpGoal(model.entity, added)
                topUpGoalTarget = null
            }
        )
    }

    if (showAddGoalSheet) {
        AddEditGoalSheet(
            initialGoal = editingGoal,
            currencySymbol = currencySymbol,
            activeAccounts = activeAccountNames,
            holdings = holdings,
            unlinkedAssets = unlinkedAssets,
            onDismiss = {
                showAddGoalSheet = false
                editingGoal = null
            },
            onSave = { goal, selectedUnlinked ->
                if (selectedUnlinked != null) {
                    viewModel.saveGoalWithUnlinkedAsset(goal, selectedUnlinked)
                } else {
                    viewModel.saveGoal(goal)
                }
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
    unlinkedAssets: List<UnlinkedLedgerAssetUiModel>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onAddInvestment: () -> Unit,
    onEditInvestment: (InvestmentEntity) -> Unit,
    onTrackUnlinkedAsset: (UnlinkedLedgerAssetUiModel) -> Unit,
    onQuickUpdateValuation: (InvestmentHoldingUiModel) -> Unit
) {
    var selectedAssetClass by remember { mutableStateOf("All") }

    val activeHoldings = remember(holdings) { holdings.filter { !it.entity.isArchived } }
    val filteredHoldings = remember(activeHoldings, selectedAssetClass) {
        if (selectedAssetClass == "All") activeHoldings
        else activeHoldings.filter { it.entity.assetClass.equals(selectedAssetClass, ignoreCase = true) }
    }
    val filteredUnlinked = remember(unlinkedAssets, selectedAssetClass) {
        if (selectedAssetClass == "All") unlinkedAssets
        else unlinkedAssets.filter { it.inferredAssetClass.equals(selectedAssetClass, ignoreCase = true) }
    }

    val trackedInvested = remember(activeHoldings) { activeHoldings.sumOf { it.totalInvested } }
    val trackedValue = remember(activeHoldings) { activeHoldings.sumOf { it.currentValue } }
    val unlinkedTotal = remember(unlinkedAssets) { unlinkedAssets.sumOf { it.totalInvested } }

    val totalInvested = trackedInvested + unlinkedTotal
    val totalValue = trackedValue + unlinkedTotal
    val totalPnL = trackedValue - trackedInvested
    val totalReturnPct = if (trackedInvested > 0.0) (totalPnL / trackedInvested) * 100.0 else 0.0

    val allocationSlices = remember(activeHoldings, unlinkedAssets) {
        val map = mutableMapOf<String, Double>()
        activeHoldings.forEach { h ->
            val cls = h.entity.assetClass.ifBlank { "Other" }
            map[cls] = (map[cls] ?: 0.0) + h.currentValue
        }
        unlinkedAssets.forEach { u ->
            val cls = u.inferredAssetClass.ifBlank { "Other" }
            map[cls] = (map[cls] ?: 0.0) + u.totalInvested
        }
        map.entries.filter { it.value > 0.0 }.sortedByDescending { it.value }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 140.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Unified Portfolio Hero Card
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
                                Text("Unrealized P&L (Tracked)", fontSize = 10.5.sp, color = Primary200)
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

        // Asset Allocation Split Bar
        if (allocationSlices.isNotEmpty() && totalValue > 0.0) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "ASSET ALLOCATION",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                                .background(Neutral200)
                        ) {
                            allocationSlices.forEach { (cls, value) ->
                                val weight = (value / totalValue).toFloat().coerceAtLeast(0.02f)
                                Box(
                                    modifier = Modifier
                                        .weight(weight)
                                        .fillMaxHeight()
                                        .background(assetClassColor(cls))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(allocationSlices) { (cls, value) ->
                                val pct = ((value / totalValue) * 100.0).toInt()
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(assetClassColor(cls))
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$cls $pct%",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDark
                                    )
                                }
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

        if (filteredHoldings.isEmpty() && filteredUnlinked.isEmpty()) {
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
                            text = if (selectedAssetClass == "All") "Track Old & New Investments" else "No $selectedAssetClass Holdings",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Log historical Mutual Funds, Stocks, FDs, or Gold without affecting your current bank balances, or log ASSET entries in your ledger to see them auto-detected here.",
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
                            Text("Add Holding", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else {
            // Section 1: Tracked Holdings
            if (filteredHoldings.isNotEmpty()) {
                item {
                    Text(
                        text = "TRACKED HOLDINGS (${filteredHoldings.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(top = 2.dp, start = 2.dp)
                    )
                }

                items(filteredHoldings, key = { "holding_${it.entity.id}" }) { model ->
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
                                            text = "Auto-synced: ${inv.linkedSubcategory}",
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

            // Section 2: Auto-Detected from Ledger
            if (filteredUnlinked.isNotEmpty()) {
                item {
                    Text(
                        text = "DETECTED FROM LEDGER (${filteredUnlinked.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        letterSpacing = 0.6.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                    )
                }

                items(filteredUnlinked, key = { "unlinked_${it.key}" }) { asset ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onTrackUnlinkedAsset(asset) },
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, SoftTeal.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = asset.displayName,
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
                                        Surface(shape = RoundedCornerShape(5.dp), color = SoftTealLight) {
                                            Text(
                                                text = asset.inferredAssetClass,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SoftTeal,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = asset.subcategory,
                                            fontSize = 10.5.sp,
                                            color = TextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", asset.totalInvested)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "${asset.transactionCount} ${if (asset.transactionCount == 1) "entry" else "entries"}",
                                        fontSize = 10.sp,
                                        color = TextMuted
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
                                Text(
                                    text = "Ledger Cost Basis (No P&L yet)",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onTrackUnlinkedAsset(asset) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = SoftTealLight,
                                    border = BorderStroke(0.6.dp, SoftTeal.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "Track Value",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftTeal,
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
}

@Composable
private fun FinancialGoalsTab(
    goals: List<GoalProgressUiModel>,
    holdings: List<InvestmentHoldingUiModel>,
    fortressFdBalance: Double,
    fortressTarget: Double,
    fortressProgressPct: Int,
    fortressMonths: Int,
    isManualFortressTarget: Boolean,
    avgMonthlyBurn: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    onSelectFortressMonths: (Int) -> Unit,
    onAddGoal: () -> Unit,
    onEditGoal: (GoalEntity) -> Unit,
    onTopUpGoal: (GoalProgressUiModel) -> Unit,
    onToggleComplete: (GoalEntity) -> Unit
) {
    val totalTarget = remember(goals) { goals.sumOf { it.entity.targetAmount } }
    val totalSaved = remember(goals) { goals.sumOf { it.currentSaved } }
    val overallPct = if (totalTarget > 0.0) ((totalSaved / totalTarget) * 100.0).toInt().coerceIn(0, 100) else 0
    val combinedMonthlyPace = remember(goals) {
        goals.filter { !it.entity.isCompleted && it.progressFraction < 1f }
            .sumOf { it.requiredMonthlyContribution }
    }
    val dateFmt = remember { SimpleDateFormat("MMM yyyy", Locale.US) }
    val nowMillis = remember { System.currentTimeMillis() }

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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column {
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
                            }

                            if (combinedMonthlyPace > 0.0) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Monthly Pace", fontSize = 10.sp, color = Primary200)
                                    Text(
                                        text = if (isDiscreet) "••••/mo" else "$currencySymbol${String.format(Locale.US, "%,.0f", combinedMonthlyPace)}/mo",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Success100
                                    )
                                }
                            }
                        }

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

        // Pinned Fortress Emergency Fund Card (Auto-Synced to Sweep FDs & Monthly Burn)
        item {
            val effectiveTarget = if (fortressTarget > 0.0) fortressTarget else (avgMonthlyBurn * fortressMonths)
            val shortfall = (effectiveTarget - fortressFdBalance).coerceAtLeast(0.0)
            val pct = fortressProgressPct.coerceIn(0, 100)
            val isFunded = pct >= 100 && effectiveTarget > 0.0

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(1.dp, if (isFunded) Success500.copy(alpha = 0.5f) else Primary200)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isFunded) Success100 else Primary50),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (isFunded) Success700 else Primary600,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Emergency Fund (Fortress FDs)",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = if (isManualFortressTarget) "Manual target override"
                                    else "Auto-calculated: ${fortressMonths}M × ${if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", avgMonthlyBurn)}"}/mo",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isFunded) Success100 else Primary50
                        ) {
                            Text(
                                text = "$pct%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isFunded) Success700 else Primary700,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { pct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = if (isFunded) Success500 else SoftTeal,
                        trackColor = Neutral200
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDiscreet) "•••• / ••••"
                            else "$currencySymbol${String.format(Locale.US, "%,.0f", fortressFdBalance)} / $currencySymbol${String.format(Locale.US, "%,.0f", effectiveTarget)}",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = if (isFunded) "Safety Net Funded"
                            else if (isDiscreet) "Gap: ••••"
                            else "Gap: $currencySymbol${String.format(Locale.US, "%,.0f", shortfall)}",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isFunded) Success700 else Warning700
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1-Tap Runway Multiplier Chips (3M / 6M / 9M / 12M)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(3, 6, 9, 12).forEach { months ->
                            val isSel = !isManualFortressTarget && fortressMonths == months
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectFortressMonths(months) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) Primary600 else CanvasLight,
                                border = BorderStroke(0.8.dp, if (isSel) Primary600 else BorderLight)
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${months}M",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else TextDark
                                    )
                                }
                            }
                        }
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
                            text = "Link a Vault account or any ongoing SIP/Investment to track live progress and calculate your exact required monthly savings pace.",
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
                val isOverdue = !isDone && goal.targetDateMillis < nowMillis

                val linkedSourceLabel = remember(goal, holdings) {
                    val invName = goal.linkedInvestmentId?.let { id ->
                        holdings.firstOrNull { it.entity.id == id }?.entity?.name
                    }
                    when {
                        !invName.isNullOrBlank() && !goal.linkedAccountName.isNullOrBlank() -> "Synced: $invName + ${goal.linkedAccountName}"
                        !invName.isNullOrBlank() -> "Synced: $invName"
                        !goal.linkedAccountName.isNullOrBlank() -> "Vault: ${goal.linkedAccountName}"
                        else -> null
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { onEditGoal(goal) },
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    border = BorderStroke(
                        0.8.dp,
                        when {
                            isDone -> Success500.copy(alpha = 0.5f)
                            isOverdue -> Warning500.copy(alpha = 0.55f)
                            else -> BorderLight
                        }
                    )
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (isOverdue) "Past Target (${dateFmt.format(Date(goal.targetDateMillis))})"
                                            else "Target: ${dateFmt.format(Date(goal.targetDateMillis))} (${model.monthsRemaining}m left)",
                                            fontSize = 10.5.sp,
                                            color = if (isOverdue) Warning700 else TextMuted,
                                            fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
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

                        if (linkedSourceLabel != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SoftTealLight
                            ) {
                                Text(
                                    text = linkedSourceLabel,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftTeal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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
                            Column {
                                Text(
                                    text = if (isDiscreet) "•••• / ••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", model.currentSaved)} / $currencySymbol${String.format(Locale.US, "%,.0f", goal.targetAmount)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                if (!isDone && model.requiredMonthlyContribution > 0.0) {
                                    Text(
                                        text = if (isDiscreet) "Need ••••/mo" else "Need $currencySymbol${String.format(Locale.US, "%,.0f", model.requiredMonthlyContribution)}/mo",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary600
                                    )
                                } else if (isDone) {
                                    Text(
                                        text = "Goal Achieved",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Success700
                                    )
                                }
                            }

                            if (!isDone) {
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onTopUpGoal(model) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Primary50,
                                    border = BorderStroke(0.6.dp, Primary200)
                                ) {
                                    Text(
                                        text = "+ Top Up",
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
private fun QuickTopUpGoalDialog(
    goalModel: GoalProgressUiModel,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    val context = LocalContext.current
    var addText by remember(goalModel.entity.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        title = { Text("Top Up ${goalModel.entity.title}", fontWeight = FontWeight.Bold, color = TextDark) },
        text = {
            Column {
                Text(
                    text = "Current saved: $currencySymbol${String.format(Locale.US, "%,.0f", goalModel.currentSaved)} / $currencySymbol${String.format(Locale.US, "%,.0f", goalModel.entity.targetAmount)}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = addText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        val parts = filtered.split('.')
                        addText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                    },
                    label = { Text("Contribution to Add ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsed = addText.toDoubleOrNull()
                if (parsed == null || parsed <= 0.0) {
                    Toast.makeText(context, "Enter a valid contribution amount", Toast.LENGTH_SHORT).show()
                } else {
                    onSave(parsed)
                }
            }) {
                Text("Add", fontWeight = FontWeight.Bold, color = Primary600)
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
    prefillAsset: UnlinkedLedgerAssetUiModel?,
    currencySymbol: String,
    assetSubcategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (InvestmentEntity) -> Unit,
    onDelete: (InvestmentEntity) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val invKey = "${initialInvestment?.id ?: -1L}_${prefillAsset?.key.orEmpty()}"

    var name by remember(invKey) {
        mutableStateOf(initialInvestment?.name ?: prefillAsset?.displayName.orEmpty())
    }
    var assetClass by remember(invKey) {
        mutableStateOf(initialInvestment?.assetClass ?: prefillAsset?.inferredAssetClass ?: "Mutual Fund")
    }
    var openingInvestedText by remember(invKey) {
        mutableStateOf(
            initialInvestment?.openingInvestedAmount?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }.orEmpty()
        )
    }
    var currentValText by remember(invKey) {
        val preVal = initialInvestment?.currentValuation ?: prefillAsset?.totalInvested
        mutableStateOf(
            preVal?.takeIf { it > 0.0 }?.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }.orEmpty()
        )
    }
    var linkedSubcategory by remember(invKey) {
        mutableStateOf(initialInvestment?.linkedSubcategory ?: prefillAsset?.subcategory)
    }
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
                    text = when {
                        initialInvestment != null -> "Edit Holding"
                        prefillAsset != null -> "Track ${prefillAsset.displayName}"
                        else -> "Add Investment Holding"
                    },
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TextDark
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            if (prefillAsset != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = SoftTealLight,
                    border = BorderStroke(0.6.dp, SoftTeal.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Auto-syncing $currencySymbol${String.format(Locale.US, "%,.0f", prefillAsset.totalInvested)} from your ledger (${prefillAsset.subcategory}). Leave Prior Invested blank unless you invested before using MyFin.",
                        fontSize = 11.sp,
                        color = TextDark,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Holding Name (e.g. Nifty Next 50 SIP, Silver)") },
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
                        val current = currentValText.toDoubleOrNull() ?: (opening + (prefillAsset?.totalInvested ?: 0.0))
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
    unlinkedAssets: List<UnlinkedLedgerAssetUiModel>,
    onDismiss: () -> Unit,
    onSave: (GoalEntity, UnlinkedLedgerAssetUiModel?) -> Unit,
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
    var selectedHorizonMonths by remember(goalKey) { mutableStateOf<Int?>(if (initialGoal == null) 12 else null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var linkedAccount by remember(goalKey) { mutableStateOf(initialGoal?.linkedAccountName) }
    var linkedInvestmentId by remember(goalKey) { mutableStateOf(initialGoal?.linkedInvestmentId) }
    var selectedUnlinkedAsset by remember(goalKey) { mutableStateOf<UnlinkedLedgerAssetUiModel?>(null) }

    val dateFmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
    val activeHoldings = remember(holdings) { holdings.filter { !it.entity.isArchived } }

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

            // Quick Start from Ongoing Investments (Tracked + Auto-Detected Ledger Assets)
            if (initialGoal == null && (activeHoldings.isNotEmpty() || unlinkedAssets.isNotEmpty())) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Quick Start from Ongoing Investments",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Primary700
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(activeHoldings, key = { "qs_h_${it.entity.id}" }) { model ->
                        val isSel = linkedInvestmentId == model.entity.id && selectedUnlinkedAsset == null
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                if (isSel) {
                                    linkedInvestmentId = null
                                } else {
                                    linkedInvestmentId = model.entity.id
                                    selectedUnlinkedAsset = null
                                    if (title.isBlank() || title.endsWith(" Goal")) {
                                        title = "${model.entity.name} Goal"
                                    }
                                    initialSavedText = ""
                                }
                            },
                            label = {
                                Text(
                                    text = "${model.entity.name} • $currencySymbol${String.format(Locale.US, "%,.0f", model.currentValue)}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftTealLight,
                                selectedLabelColor = SoftTeal
                            )
                        )
                    }

                    items(unlinkedAssets, key = { "qs_u_${it.key}" }) { asset ->
                        val isSel = selectedUnlinkedAsset?.key == asset.key
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                if (isSel) {
                                    selectedUnlinkedAsset = null
                                } else {
                                    selectedUnlinkedAsset = asset
                                    linkedInvestmentId = null
                                    if (title.isBlank() || title.endsWith(" Goal")) {
                                        title = "${asset.displayName} Goal"
                                    }
                                    initialSavedText = ""
                                }
                            },
                            label = {
                                Text(
                                    text = "${asset.displayName} • $currencySymbol${String.format(Locale.US, "%,.0f", asset.totalInvested)}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary50,
                                selectedLabelColor = Primary700
                            )
                        )
                    }
                }

                val syncedPreviewAmount = selectedUnlinkedAsset?.totalInvested
                    ?: activeHoldings.firstOrNull { it.entity.id == linkedInvestmentId }?.currentValue
                if (syncedPreviewAmount != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = SoftTealLight,
                        border = BorderStroke(0.6.dp, SoftTeal.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Auto-syncing $currencySymbol${String.format(Locale.US, "%,.0f", syncedPreviewAmount)} — enter your Target Amount & Horizon below.",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoftTeal,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Goal Name (e.g. Wedding Gold, New Bike)") },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target Horizon (${dateFmt.format(Date(targetDateMillis))})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = Primary50,
                    border = BorderStroke(0.6.dp, Primary200)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date",
                            tint = Primary600,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick Date", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Primary700)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(6 to "6 Months", 12 to "1 Year", 36 to "3 Years", 60 to "5 Years").forEach { (months, label) ->
                    item {
                        val isSel = selectedHorizonMonths == months
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedHorizonMonths = months
                                targetDateMillis = Calendar.getInstance().apply { add(Calendar.MONTH, months) }.timeInMillis
                            },
                            label = { Text(label, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary50,
                                selectedLabelColor = Primary700
                            )
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

            if (activeHoldings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Link Tracked Holding (Optional)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = linkedInvestmentId == null && selectedUnlinkedAsset == null,
                            onClick = {
                                linkedInvestmentId = null
                                selectedUnlinkedAsset = null
                            },
                            label = { Text("None", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    items(activeHoldings) { model ->
                        val isSel = linkedInvestmentId == model.entity.id
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                linkedInvestmentId = model.entity.id
                                selectedUnlinkedAsset = null
                            },
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
                            ),
                            selectedUnlinkedAsset
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = targetDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
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
                            targetDateMillis = localCal.timeInMillis
                            selectedHorizonMonths = null
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold, color = Primary600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextDark)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
