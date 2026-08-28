package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

enum class DashboardTab { SUMMARY, TRANSACTIONS, MONTHLY_PAYMENTS }

@Composable
fun MonthlyScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToPlanner: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val filterCriteria by viewModel.filterCriteria.collectAsState()
    val showRollover by viewModel.showRolloverPrompt.collectAsState()

    var activeTab by remember { mutableStateOf(DashboardTab.SUMMARY) }
    var selectedMatrixType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedTxFilterType by remember { mutableStateOf<TransactionType?>(null) }

    var showActionMenu by remember { mutableStateOf(false) }
    var showAddSheet by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

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
    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val accountsList = remember(uiState.accounts) {
        if (uiState.accounts.isEmpty()) listOf("BOM", "CASH", "HDFC", "INDUSIND")
        else uiState.accounts.map { it.accountName }
    }

    val daysInMonth = remember(uiState.selectedMonth, uiState.selectedYear) {
        Calendar.getInstance().apply {
            set(uiState.selectedYear, uiState.selectedMonth - 1, 1)
        }.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
    val daysRemaining = (daysInMonth - currentDay + 1).coerceAtLeast(1)
    val dailySpendAllowance = (uiState.metrics.safeToSpend / daysRemaining).coerceAtLeast(0.0)

    Box(modifier = Modifier.fillMaxSize().background(CanvasLight)) {
        // Main Scrollable Dashboard Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 96.dp, bottom = 120.dp)
        ) {
            // --- TAB 1: SUMMARY ---
            if (activeTab == DashboardTab.SUMMARY) {
                if (showRollover) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(3.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Prepare for Next Month", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Carry forward your AutoPay commitments and budget templates.", fontSize = 11.sp, color = TextMuted)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.End) {
                                    Button(
                                        onClick = { viewModel.executeRolloverToNextMonth() },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Sync Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(
                                        onClick = { viewModel.dismissRolloverPrompt() },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text("Dismiss", fontSize = 10.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Hero Card: Safe-to-Spend Guardrail & Live Sparkline (Infused with AccentPurple Ambient Gradient)
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
                                        text = "SAFE TO SPEND GUARDRAIL",
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
                                text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.safeToSpend)}",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isHealthy) TextDark else SoftRed,
                                letterSpacing = (-0.6).sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (isHealthy) {
                                    "Avg ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailySpendAllowance)}/day safe allowance for $daysRemaining days left"
                                } else {
                                    "Overrun warning: spending exceeds available cashflow buffer"
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
                                    amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.plannedIncome)}",
                                    tintColor = SoftGreen,
                                    modifier = Modifier.weight(1f)
                                )
                                PillarMetricCard(
                                    title = "Fixed Bills",
                                    amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.fixedCommitmentsTotal)}",
                                    tintColor = SoftRed,
                                    modifier = Modifier.weight(1f)
                                )
                                PillarMetricCard(
                                    title = "SIP Assets",
                                    amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualAssets)}",
                                    tintColor = SoftTeal,
                                    modifier = Modifier.weight(1f)
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
                                Text("START BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.4.sp)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", startBalance)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = TextDark
                                )
                                Text("Opening Vault", fontSize = 10.sp, color = TextMuted)
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
                                Text("END BALANCE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.4.sp)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", currentEndBalance)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = if (currentEndBalance >= 0) TextDark else SoftRed
                                )
                                Text("Current Liquid", fontSize = 10.sp, color = TextMuted)
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
                                Text("NET SAVINGS", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.4.sp)
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${if (netSavings >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", netSavings)}",
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

                // 3-Pillar Cashflow & Target Comparison System
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
                                varianceText = if (plannedExpenses > 0) {
                                    if (expDiff > 0) "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expDiff)} Over"
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
                                varianceText = if (plannedIncome > 0) {
                                    if (incDiff >= 0) "Target Met"
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
                                varianceText = if (plannedAssets > 0) {
                                    if (astDiff >= 0) "Target Met"
                                    else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(astDiff))} Short"
                                } else "Recorded Wealth",
                                isAlert = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Category Matrix (Accordion)
                item {
                    Text("Category Matrix", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
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

                val activeMatrix = uiState.categories.filter { it.type == selectedMatrixType }

                if (activeMatrix.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = CardWhite
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No active entries in this segment", fontSize = 12.sp, color = TextMuted)
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
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                                activeMatrix.forEachIndexed { index, cat ->
                                    val isExpanded = expandedCategories[cat.category] ?: false
                                    val rotation by animateFloatAsState(
                                        targetValue = if (isExpanded) 180f else 0f,
                                        animationSpec = tween(220),
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
                                            .padding(vertical = 10.dp, horizontal = 4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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
                                                        fontSize = 15.sp,
                                                        color = progressColor
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = cat.category,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 14.sp,
                                                            color = TextDark
                                                        )
                                                        if (cat.isOverBudget) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Surface(shape = RoundedCornerShape(4.dp), color = SoftRed.copy(alpha = 0.12f)) {
                                                                Text("Over Budget", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SoftRed, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                                                            }
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = if (cat.plannedAmount > 0) {
                                                            val remaining = cat.plannedAmount - cat.actualAmount
                                                            if (remaining >= 0) {
                                                                "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", remaining)} left of ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cat.plannedAmount)}"
                                                            } else {
                                                                "Exceeded by ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(remaining))}"
                                                            }
                                                        } else {
                                                            "No target limit configured"
                                                        },
                                                        fontSize = 11.sp,
                                                        color = if (cat.isOverBudget) SoftRed else TextMuted
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cat.actualAmount)}",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 15.sp,
                                                        color = if (cat.isOverBudget) SoftRed else TextDark
                                                    )
                                                    if (cat.plannedAmount > 0) {
                                                        Text(
                                                            text = "$utilizationPercentage%",
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = progressColor
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    Icons.Default.ExpandMore,
                                                    contentDescription = "Expand",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp).rotate(rotation)
                                                )
                                            }
                                        }

                                        if (cat.plannedAmount > 0) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            LinearProgressIndicator(
                                                progress = { progressFraction },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(5.dp)
                                                    .clip(RoundedCornerShape(3.dp)),
                                                color = progressColor,
                                                trackColor = BorderLight.copy(alpha = 0.6f)
                                            )
                                        }

                                        AnimatedVisibility(
                                            visible = isExpanded,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 12.dp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(CanvasLight)
                                                    .padding(12.dp)
                                            ) {
                                                if (cat.activeSubcategories.isEmpty()) {
                                                    Text("No logged transactions in subcategories", fontSize = 11.5.sp, color = TextMuted)
                                                } else {
                                                    Text(
                                                        text = "SUBCATEGORY CONTRIBUTIONS",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = TextMuted,
                                                        letterSpacing = 0.5.sp,
                                                        modifier = Modifier.padding(bottom = 6.dp)
                                                    )

                                                    cat.activeSubcategories.forEach { sub ->
                                                        val subPercentage = if (cat.actualAmount > 0) {
                                                            ((sub.amount / cat.actualAmount) * 100).toInt()
                                                        } else 0

                                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
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
                                                                    Text(sub.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                                                }

                                                                Text(
                                                                    text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", sub.amount)} ($subPercentage%)",
                                                                    fontSize = 12.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = TextDark
                                                                )
                                                            }
                                                            Spacer(modifier = Modifier.height(3.dp))
                                                            LinearProgressIndicator(
                                                                progress = { (subPercentage / 100f).coerceIn(0f, 1f) },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(3.5.dp)
                                                                    .clip(RoundedCornerShape(2.dp)),
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
                                        HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- TAB 2: TRANSACTIONS ---
            if (activeTab == DashboardTab.TRANSACTIONS) {
                item {
                    // Compact Slim Search Field with subtle AccentPurple cursor & focus
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
                                    Text("Search ledger...", color = TextMuted, fontSize = 13.sp, maxLines = 1)
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

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FilterChip(
                                selected = filterCriteria.account == "ALL",
                                onClick = { viewModel.updateFilter(filterCriteria.type, "ALL", filterCriteria.startDate, filterCriteria.endDate) },
                                label = { Text("All Vaults", fontSize = 11.sp) },
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
                                label = { Text(acc, fontSize = 11.sp) },
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

                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (uiState.groupedTransactions.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No transactions recorded", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Try clearing filters or log a new entry", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
                    uiState.groupedTransactions.forEach { (dateHeader, txList) ->
                        val dailyExpenseTotal = txList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                        val dailyIncomeTotal = txList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp, bottom = 6.dp, start = 4.dp, end = 4.dp),
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
                                            text = "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyIncomeTotal)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = SoftGreen
                                        )
                                    }
                                    if (dailyExpenseTotal > 0.0) {
                                        Text(
                                            text = "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyExpenseTotal)}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = TextDark
                                        )
                                    }
                                }
                            }
                        }

                        items(txList, key = { it.id }) { tx ->
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

            // --- TAB 3: FIXED SIPS & COMMITMENTS ---
            if (activeTab == DashboardTab.MONTHLY_PAYMENTS) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fixed SIPs & Bills", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        TextButton(onClick = { showAddFixedBill = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp), tint = AccentPurple)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add AutoPay", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AccentPurple)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                if (uiState.fixedBills.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No recurring AutoPay commitments for this month", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
                    items(uiState.fixedBills, key = { it.id }) { bill ->
                        Box(modifier = Modifier.padding(vertical = 4.dp)) {
                            SwipeableFixedBillItem(
                                bill = bill,
                                currencySymbol = userProfile.currencySymbol,
                                onTap = {
                                    if (!bill.isPaid) {
                                        settlingFixedBill = bill
                                    } else {
                                        billToRevert = bill
                                    }
                                },
                                onEdit = {
                                    if (bill.isPaid) {
                                        Toast.makeText(context, "Cannot edit settled commitment. Tap the card to revert to Unpaid first.", Toast.LENGTH_LONG).show()
                                    } else {
                                        editingFixedBill = bill
                                    }
                                },
                                onDelete = { billToDelete = bill }
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // PINNED TOP BAR (Compact with Accent Glow)
        // ==========================================
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .zIndex(3f),
            color = CanvasLight.copy(alpha = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AccentPurple.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardWhite)
                            .border(0.8.dp, AccentPurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .shadow(1.dp, RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Drawer / Navigation",
                            tint = TextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { showMonthPicker = true },
                        shape = RoundedCornerShape(20.dp),
                        color = CardWhite,
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.22f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${monthNames[uiState.selectedMonth - 1]} ${uiState.selectedYear}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4 + 1 FLOATING BOTTOM NAVIGATION DOCK
        // ==========================================
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
                .zIndex(4f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shadow(16.dp, CircleShape),
                shape = CircleShape,
                color = CardWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockPillTab(
                        title = "Summary",
                        icon = Icons.Default.Assessment,
                        isSelected = activeTab == DashboardTab.SUMMARY,
                        onClick = { activeTab = DashboardTab.SUMMARY }
                    )
                    DockPillTab(
                        title = "Ledger",
                        icon = Icons.Default.ReceiptLong,
                        isSelected = activeTab == DashboardTab.TRANSACTIONS,
                        onClick = { activeTab = DashboardTab.TRANSACTIONS }
                    )
                    DockPillTab(
                        title = "Monthly",
                        icon = Icons.Default.EventRepeat,
                        isSelected = activeTab == DashboardTab.MONTHLY_PAYMENTS,
                        onClick = { activeTab = DashboardTab.MONTHLY_PAYMENTS }
                    )
                    DockPillTab(
                        title = "Planner",
                        icon = Icons.Default.PieChart,
                        isSelected = false,
                        onClick = onNavigateToPlanner
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = { showActionMenu = !showActionMenu },
                containerColor = AccentPurple,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).shadow(16.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Actions",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(if (showActionMenu) 45f else 0f)
                )
            }
        }

        // Anchored Action Menu
        if (showActionMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showActionMenu = false }
                    )
            )

            AnimatedVisibility(
                visible = showActionMenu,
                enter = scaleIn(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(180)
                ) + fadeIn(animationSpec = tween(180)),
                exit = scaleOut(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(150)
                ) + fadeOut(animationSpec = tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 94.dp, end = 20.dp)
                    .zIndex(6f)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    shadowElevation = 10.dp,
                    border = BorderStroke(0.8.dp, AccentPurple.copy(alpha = 0.2f)),
                    modifier = Modifier.width(190.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        // Action 1: Add Entry
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    editingTx = null
                                    showAddSheet = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Add Entry",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = TextDark
                            )
                        }

                        HorizontalDivider(
                            color = BorderLight.copy(alpha = 0.6f),
                            thickness = 0.8.dp
                        )

                        // Action 2: Instant Vault Transfer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    showTransferDialog = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.SyncAlt,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Transfer",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = TextDark
                            )
                        }
                    }
                }
            }
        }

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

        transactionToDelete?.let { tx ->
            AlertDialog(
                onDismissRequest = { transactionToDelete = null },
                title = { Text("Delete Transaction?", fontWeight = FontWeight.Bold) },
                text = {
                    Text(
                        if (tx.linkedFixedBillId != null)
                            "This entry is linked to an AutoPay bill. Deleting it will restore your vault balance and revert the parent commitment back to Unpaid."
                        else "Are you sure you want to delete '${tx.title}' (${userProfile.currencySymbol}${tx.amount})? This will permanently remove it from your vault ledger."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteTransaction(tx)
                        transactionToDelete = null
                    }) {
                        Text("Delete", color = SoftRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { transactionToDelete = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        billToDelete?.let { bill ->
            AlertDialog(
                onDismissRequest = { billToDelete = null },
                title = { Text("Delete AutoPay Commitment?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Deleting '${bill.title}' will remove this recurring template. Any linked payment already recorded in your ledger for this month will also be deleted and restored to your vault.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteFixedBill(bill)
                        billToDelete = null
                    }) {
                        Text("Delete Commitment", color = SoftRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { billToDelete = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        billToRevert?.let { bill ->
            AlertDialog(
                onDismissRequest = { billToRevert = null },
                title = { Text("Revert to Unsettled?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Reverting '${bill.title}' will delete the logged payment from your transaction ledger and restore the balance to ${bill.accountName}.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.toggleFixedBillPaid(bill)
                        billToRevert = null
                    }) {
                        Text("Revert Status", color = SoftRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { billToRevert = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        settlingFixedBill?.let { bill ->
            var finalAmountText by remember { mutableStateOf(bill.amount.toString()) }

            val actionPrompt = when (bill.type) {
                TransactionType.INCOME -> "Confirm Inflow Received?"
                TransactionType.ASSET -> "Confirm SIP Investment?"
                TransactionType.TRANSFER -> "Confirm Vault Sweep?"
                TransactionType.EXPENSE -> "Mark as Paid & Deduct?"
            }

            val descPrompt = when (bill.type) {
                TransactionType.INCOME -> "Credits ${bill.accountName} vault and logs inflow entry."
                TransactionType.ASSET -> "Deducts from ${bill.accountName} and records under Asset Wealth."
                TransactionType.TRANSFER -> "Sweeps funds from ${bill.accountName} ➔ ${bill.toAccountName ?: "Destination"}."
                TransactionType.EXPENSE -> "Deducts from ${bill.accountName} and records expense entry."
            }

            Dialog(onDismissRequest = { settlingFixedBill = null }) {
                Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(actionPrompt, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(descPrompt, fontSize = 12.sp, color = TextMuted)

                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = finalAmountText,
                            onValueChange = { finalAmountText = it },
                            label = { Text("Actual Amount (${userProfile.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { settlingFixedBill = null }) {
                                Text("Cancel", color = TextDark)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val amt = finalAmountText.toDoubleOrNull() ?: bill.amount
                                    viewModel.toggleFixedBillPaid(bill, customAmount = amt)
                                    settlingFixedBill = null
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
                                Text("Confirm & Settle", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showTransferDialog) {
            AccountTransferDialog(
                accounts = accountsList,
                onDismiss = { showTransferDialog = false },
                onTransfer = { from, to, amount, note ->
                    viewModel.executeInstantTransfer(from, to, amount, note)
                }
            )
        }

        if (showMonthPicker) {
            Dialog(onDismissRequest = { showMonthPicker = false }) {
                Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Select Timeframe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.selectYear(uiState.selectedYear - 1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Year")
                            }
                            Text("${uiState.selectedYear}", fontWeight = FontWeight.Black, fontSize = 16.sp)
                            IconButton(onClick = { viewModel.selectYear(uiState.selectedYear + 1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
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
                                                text = months[monthIdx - 1],
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

        if (showAddSheet) {
            AddTransactionBottomSheet(
                editingTransaction = editingTx,
                currencySymbol = userProfile.currencySymbol,
                accountList = accountsList,
                masterCategories = uiState.masterCategories,
                masterSubcategories = uiState.masterSubcategories,
                onDismiss = { showAddSheet = false },
                onSave = { id, title, amount, category, subcat, acc, type, date ->
                    viewModel.saveTransaction(id, title, amount, category, subcat, acc, type, date)
                }
            )
        }

        if (showAddFixedBill) {
            AddEditFixedBillDialog(
                accountList = accountsList,
                masterCategories = uiState.masterCategories,
                masterSubcategories = uiState.masterSubcategories,
                onAddNewCategory = { name, type -> viewModel.addCategory(name, type) },
                onAddNewSubcategory = { parent, name, type -> viewModel.addSubcategory(parent, name, type) },
                onDismiss = { showAddFixedBill = false },
                onSave = { title, amt, cat, subcat, acc, toAcc, type, dueDay ->
                    viewModel.addFixedBill(title, amt, cat, subcat, acc, toAcc, type, dueDay)
                }
            )
        }

        editingFixedBill?.let { bill ->
            AddEditFixedBillDialog(
                initialBill = bill,
                accountList = accountsList,
                masterCategories = uiState.masterCategories,
                masterSubcategories = uiState.masterSubcategories,
                onAddNewCategory = { name, type -> viewModel.addCategory(name, type) },
                onAddNewSubcategory = { parent, name, type -> viewModel.addSubcategory(parent, name, type) },
                onDismiss = { editingFixedBill = null },
                onSave = { title, amt, cat, subcat, acc, toAcc, type, dueDay ->
                    viewModel.updateFixedBill(bill.id, title, amt, cat, subcat, acc, toAcc, type, dueDay)
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
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
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
                Text(title, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
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
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
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
                    text = "Plan: $currencySymbol${String.format(Locale.US, "%,.0f", planned)}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Act: $currencySymbol${String.format(Locale.US, "%,.0f", actual)}",
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

@Composable
private fun DockPillTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) AccentPurple.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) AccentPurple else TextMuted,
                modifier = Modifier.size(17.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = AccentPurple)
            }
        }
    }
}
