package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.theme.*
import java.util.Locale

enum class DashboardTab { SUMMARY, TRANSACTIONS, MONTHLY_PAYMENTS }

@Composable
fun MonthlyScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit
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

    Box(modifier = Modifier.fillMaxSize().background(CanvasLight)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Header Bar
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardWhite)
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextDark)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardWhite)
                            .clickable { showMonthPicker = true }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${monthNames[uiState.selectedMonth - 1]} ${uiState.selectedYear} ▾",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = TextDark
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- TAB 1: SUMMARY ---
            if (activeTab == DashboardTab.SUMMARY) {
                if (showRollover) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(4.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite
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

                // Modernized Hero Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(26.dp)),
                        shape = RoundedCornerShape(26.dp),
                        color = CardWhite
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFFAF9FE),
                                            Color(0xFFF3F1FD)
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
                                Text(
                                    text = "SAFE TO SPEND",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.8.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (uiState.metrics.safeToSpend > 0) SoftGreen.copy(alpha = 0.14f) else SoftRed.copy(alpha = 0.14f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (uiState.metrics.safeToSpend > 0) SoftGreen else SoftRed)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "${uiState.metrics.safeToSpendPercentage}% Remaining",
                                            color = if (uiState.metrics.safeToSpend > 0) SoftGreen else SoftRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.safeToSpend)}",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = if (uiState.metrics.safeToSpend > 0) TextDark else SoftRed,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sparkline Visualization
                            SpendingSparkline(
                                points = uiState.metrics.dailyExpensePoints,
                                lineColor = AccentPurple,
                                gradientStartColor = AccentPurple.copy(alpha = 0.25f),
                                gradientEndColor = AccentPurple.copy(alpha = 0.0f)
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Micro Financial Breakdown Chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CardWhite.copy(alpha = 0.85f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Inflow", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.plannedIncome)}",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SoftGreen
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CardWhite.copy(alpha = 0.85f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Fixed Bills", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.fixedCommitmentsTotal)}",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SoftRed
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CardWhite.copy(alpha = 0.85f)
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("SIP Assets", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualAssets)}",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SoftTeal
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Planned vs Actual Spend Cards
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(modifier = Modifier.weight(1f).shadow(2.dp, RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp), color = CardWhite) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Budget Limit", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.plannedExpenses)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                        }
                        Surface(modifier = Modifier.weight(1f).shadow(2.dp, RoundedCornerShape(18.dp)), shape = RoundedCornerShape(18.dp), color = CardWhite) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Actual Spent", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualExpenses)}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = SoftRed)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Category Matrix Segmented Tab
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
                            modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = CardWhite
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                activeMatrix.forEachIndexed { index, cat ->
                                    val isExpanded = expandedCategories[cat.category] ?: false
                                    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "arrowRotation")

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { expandedCategories[cat.category] = !isExpanded }
                                            .padding(vertical = 10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when (cat.type) {
                                                                TransactionType.INCOME -> SoftGreen.copy(alpha = 0.12f)
                                                                TransactionType.EXPENSE -> SoftRed.copy(alpha = 0.12f)
                                                                TransactionType.ASSET -> SoftTeal.copy(alpha = 0.12f)
                                                                else -> AccentPurple.copy(alpha = 0.12f)
                                                            }
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = cat.category.take(1).uppercase(),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 14.sp,
                                                        color = when (cat.type) {
                                                            TransactionType.INCOME -> SoftGreen
                                                            TransactionType.EXPENSE -> SoftRed
                                                            TransactionType.ASSET -> SoftTeal
                                                            else -> AccentPurple
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(cat.category, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                                        if (cat.isOverBudget) {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Surface(shape = RoundedCornerShape(4.dp), color = SoftRed.copy(alpha = 0.12f)) {
                                                                Text("Over Budget", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SoftRed, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                            }
                                                        }
                                                    }
                                                    Text(
                                                        text = if (cat.plannedAmount > 0) "Plan: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cat.plannedAmount)}"
                                                        else "Actual recorded",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cat.actualAmount)}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = when {
                                                        cat.isOverBudget -> SoftRed
                                                        cat.type == TransactionType.INCOME -> SoftGreen
                                                        cat.type == TransactionType.EXPENSE -> SoftRed
                                                        cat.type == TransactionType.ASSET -> SoftTeal
                                                        else -> TextDark
                                                    }
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.ExpandMore,
                                                    contentDescription = "Expand",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp).rotate(rotation)
                                                )
                                            }
                                        }

                                        AnimatedVisibility(visible = isExpanded) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 10.dp, start = 48.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(CanvasLight)
                                                    .padding(10.dp)
                                            ) {
                                                if (cat.activeSubcategories.isEmpty()) {
                                                    Text("No subcategory transactions", fontSize = 11.sp, color = TextMuted)
                                                } else {
                                                    cat.activeSubcategories.forEach { sub ->
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(sub.name, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                                            Text(
                                                                text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", sub.amount)}",
                                                                fontSize = 12.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = when (cat.type) {
                                                                    TransactionType.INCOME -> SoftGreen
                                                                    TransactionType.EXPENSE -> SoftRed
                                                                    TransactionType.ASSET -> SoftTeal
                                                                    else -> TextDark
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (index < activeMatrix.lastIndex) {
                                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.8.dp)
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
                    OutlinedTextField(
                        value = filterCriteria.query,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Search transactions...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (filterCriteria.query.isNotBlank()) {
                                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                                    }
                                }
                                IconButton(onClick = { showFilterSheet = true }) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = "Filter",
                                        tint = if (filterCriteria.type != null || filterCriteria.account != "ALL" || filterCriteria.startDate != null) AccentPurple else TextMuted
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = BorderLight
                        )
                    )

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
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                        items(accountsList) { acc ->
                            FilterChip(
                                selected = filterCriteria.account == acc,
                                onClick = { viewModel.updateFilter(filterCriteria.type, acc, filterCriteria.startDate, filterCriteria.endDate) },
                                label = { Text(acc, fontSize = 11.sp) },
                                shape = RoundedCornerShape(8.dp)
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
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "${txList.size} entries",
                                    fontSize = 11.sp,
                                    color = TextMuted.copy(alpha = 0.7f)
                                )
                            }
                        }

                        items(txList, key = { it.id }) { tx ->
                            Box(modifier = Modifier.padding(vertical = 3.dp)) {
                                SwipeableTransactionItem(
                                    transaction = tx,
                                    currencySymbol = userProfile.currencySymbol,
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

        // Floating Navigation Dock
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 20.dp, end = 20.dp)
                .fillMaxWidth(),
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
                        .padding(horizontal = 6.dp),
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
                        title = "Transactions",
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
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            FloatingActionButton(
                onClick = { showActionMenu = true },
                containerColor = TextDark,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).shadow(16.dp, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Actions", modifier = Modifier.size(28.dp))
            }
        }

        // Modals & Sheets
        if (showActionMenu) {
            Dialog(onDismissRequest = { showActionMenu = false }) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = CardWhite,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Create Financial Movement", fontWeight = FontWeight.Black, fontSize = 17.sp, color = TextDark)
                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    editingTx = null
                                    showAddSheet = true
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = CanvasLight
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ReceiptLong, contentDescription = "Tx", tint = AccentPurple)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Add Entry", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                    Text("Log Expense, Income or SIP Asset", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    showTransferDialog = true
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = CanvasLight
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SyncAlt, contentDescription = "Transfer", tint = SoftTeal)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Instant Vault Transfer", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                    Text("Move funds between Bank & Cash vaults", fontSize = 11.sp, color = TextMuted)
                                }
                            }
                        }
                    }
                }
            }
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
private fun DockPillTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) CanvasLight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) AccentPurple else TextMuted,
                modifier = Modifier.size(18.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentPurple)
            }
        }
    }
}
