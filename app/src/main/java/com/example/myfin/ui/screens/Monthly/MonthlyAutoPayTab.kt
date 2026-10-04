package com.example.myfin.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.UserProfile
import com.example.myfin.ui.MonthlyUiState
import com.example.myfin.ui.theme.*
import java.util.Calendar
import java.util.Locale

enum class CommitmentMacroFilter(val label: String) {
    ALL("All"),
    OUTFLOW("Outflow"),
    INFLOW("Inflow"),
    INTERNAL("Internal")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyAutoPayTab(
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    isDiscreetMode: Boolean,
    isMidnightTheme: Boolean = false,
    isCurrentMonth: Boolean,
    isPastMonth: Boolean,
    hideSettledCommitments: Boolean,
    selectedCommitmentFilter: TransactionType? = null,
    onSelectCommitmentFilter: ((TransactionType?) -> Unit)? = null,
    onResetFilters: () -> Unit,
    onAddAutoPay: () -> Unit,
    onTapBill: (FixedBillEntity) -> Unit,
    onEditBill: (FixedBillEntity) -> Unit,
    onDeleteBill: (FixedBillEntity) -> Unit,
    onSettleBill: (FixedBillEntity, Double, Long) -> Unit
) {
    var activeMacroFilter by remember { mutableStateOf(CommitmentMacroFilter.ALL) }
    var isSettledSectionExpanded by remember { mutableStateOf(false) }

    val chevronRotation by animateFloatAsState(
        targetValue = if (isSettledSectionExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "chevronRotation"
    )

    val currentDayOfMonth = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }

    // Outflow: Expenses, SIP Investments, Fortress Sweeps, Corporate Outlays
    val isOutflow = remember {
        { bill: FixedBillEntity ->
            when {
                bill.type == TransactionType.EXPENSE -> true
                bill.type == TransactionType.ASSET -> true
                bill.type == TransactionType.TRANSFER -> {
                    bill.subcategory.equals("WEALTH_ALLOCATION", ignoreCase = true) ||
                            bill.toAccountName?.contains("Fortress", ignoreCase = true) == true ||
                            bill.category.equals("Fortress", ignoreCase = true)
                }
                bill.type == TransactionType.CORPORATE -> !bill.category.equals("Reimbursements & Claims", ignoreCase = true)
                else -> false
            }
        }
    }

    // Inflow: Salary & Corporate Claims
    val isInflow = remember {
        { bill: FixedBillEntity ->
            bill.type == TransactionType.INCOME ||
                    (bill.type == TransactionType.CORPORATE && bill.category.equals("Reimbursements & Claims", ignoreCase = true))
        }
    }

    // Internal: Operating -> Commitments Bill Funding sweeps & rebalances
    val isInternal = remember {
        { bill: FixedBillEntity ->
            bill.type == TransactionType.TRANSFER && !isOutflow(bill)
        }
    }

    val filteredBills = remember(uiState.fixedBills, activeMacroFilter, hideSettledCommitments) {
        uiState.fixedBills.filter { bill ->
            val matchesSettledVisibility = !hideSettledCommitments || !bill.isPaid
            val matchesMacro = when (activeMacroFilter) {
                CommitmentMacroFilter.ALL -> true
                CommitmentMacroFilter.OUTFLOW -> isOutflow(bill)
                CommitmentMacroFilter.INFLOW -> isInflow(bill)
                CommitmentMacroFilter.INTERNAL -> isInternal(bill)
            }
            matchesSettledVisibility && matchesMacro
        }
    }

    val overdueOrActionNeeded = remember(filteredBills, currentDayOfMonth, isCurrentMonth, isPastMonth) {
        filteredBills.filter { bill ->
            if (bill.isPaid) false
            else when {
                isPastMonth -> true
                isCurrentMonth -> bill.dueDay != null && bill.dueDay <= currentDayOfMonth
                else -> false
            }
        }.sortedWith(compareBy({ it.dueDay ?: 0 }, { it.title.lowercase(Locale.ROOT) }))
    }

    val upcomingBills = remember(filteredBills, currentDayOfMonth, isCurrentMonth, isPastMonth) {
        filteredBills.filter { bill ->
            if (bill.isPaid) false
            else when {
                isPastMonth -> false
                isCurrentMonth -> bill.dueDay == null || bill.dueDay > currentDayOfMonth
                else -> true
            }
        }.sortedWith(compareBy({ it.dueDay ?: 99 }, { it.title.lowercase(Locale.ROOT) }))
    }

    val settledBills = remember(filteredBills) {
        filteredBills.filter { it.isPaid }
            .sortedWith(compareBy({ it.dueDay ?: 99 }, { it.title.lowercase(Locale.ROOT) }))
    }

    val pendingOutflowTotal = remember(filteredBills) {
        filteredBills.filter { !it.isPaid && isOutflow(it) }.sumOf { it.amount }
    }
    val pendingInflowTotal = remember(filteredBills) {
        filteredBills.filter { !it.isPaid && isInflow(it) }.sumOf { it.amount }
    }
    val pendingInternalTotal = remember(filteredBills) {
        filteredBills.filter { !it.isPaid && isInternal(it) }.sumOf { it.amount }
    }

    val overdueOutflowCount = remember(overdueOrActionNeeded) {
        overdueOrActionNeeded.count { isOutflow(it) }
    }

    val headingColor = if (isMidnightTheme) Color.White else TextDark
    val subtextColor = if (isMidnightTheme) Color(0xFF94A3B8) else TextMuted
    val filterRowBg = if (isMidnightTheme) Color.White.copy(alpha = 0.10f) else BorderLight.copy(alpha = 0.5f)

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. TOP HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Recurring Commitments",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = headingColor,
                    letterSpacing = (-0.3).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "AutoPay, Standing Orders & Sweeps",
                    fontSize = 11.sp,
                    color = subtextColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                TextButton(
                    onClick = onAddAutoPay,
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add AutoPay",
                        modifier = Modifier.size(15.dp),
                        tint = AccentPurple
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Add AutoPay",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AccentPurple
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                val (badgeText, badgeColor) = remember(
                    activeMacroFilter,
                    pendingOutflowTotal,
                    pendingInflowTotal,
                    pendingInternalTotal,
                    overdueOutflowCount,
                    isDiscreetMode,
                    userProfile.currencySymbol
                ) {
                    val curr = userProfile.currencySymbol
                    when (activeMacroFilter) {
                        CommitmentMacroFilter.INFLOW -> {
                            val txt = if (isDiscreetMode) "•••• Expected" else "+$curr${String.format(Locale.US, "%,.0f", pendingInflowTotal)} Expected"
                            txt to SoftTeal
                        }
                        CommitmentMacroFilter.INTERNAL -> {
                            val txt = if (isDiscreetMode) "•••• Scheduled" else "⇄ $curr${String.format(Locale.US, "%,.0f", pendingInternalTotal)} Sweeps"
                            txt to AccentPurple
                        }
                        CommitmentMacroFilter.OUTFLOW -> {
                            if (overdueOutflowCount > 0) {
                                val txt = if (isDiscreetMode) "•••• Due ($overdueOutflowCount Overdue)" else "-$curr${String.format(Locale.US, "%,.0f", pendingOutflowTotal)} Due ($overdueOutflowCount Overdue)"
                                txt to SoftRed
                            } else {
                                val txt = if (isDiscreetMode) "•••• Due" else "-$curr${String.format(Locale.US, "%,.0f", pendingOutflowTotal)} Due"
                                txt to SoftAmber
                            }
                        }
                        CommitmentMacroFilter.ALL -> {
                            if (overdueOutflowCount > 0) {
                                val txt = if (isDiscreetMode) "•••• Due ($overdueOutflowCount Overdue)" else "$curr${String.format(Locale.US, "%,.0f", pendingOutflowTotal)} Due ($overdueOutflowCount Overdue)"
                                txt to SoftRed
                            } else {
                                val txt = if (isDiscreetMode) "•••• Pending" else "$curr${String.format(Locale.US, "%,.0f", pendingOutflowTotal)} Pending"
                                txt to SoftAmber
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = badgeColor.copy(alpha = 0.12f),
                    border = BorderStroke(0.6.dp, badgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. 4-WAY MACRO SWITCHER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(filterRowBg)
                .padding(3.dp)
        ) {
            CommitmentMacroFilter.entries.forEach { filter ->
                val isSelected = activeMacroFilter == filter
                val activeTint = when (filter) {
                    CommitmentMacroFilter.ALL -> AccentPurple
                    CommitmentMacroFilter.OUTFLOW -> SoftRed
                    CommitmentMacroFilter.INFLOW -> SoftGreen
                    CommitmentMacroFilter.INTERNAL -> AccentPurple
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CardWhite else Color.Transparent)
                        .clickable { activeMacroFilter = filter }
                        .padding(vertical = 7.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = filter.label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = if (isSelected) activeTint else subtextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. GROUPED LIST
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(top = 2.dp, bottom = 140.dp)
        ) {
            if (filteredBills.isEmpty()) {
                item(key = "empty_commitments") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 20.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(28.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (hideSettledCommitments) "No pending commitments in this filter" else "No recurring commitments recorded",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Try clearing active filters or add a new commitment",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                            if (activeMacroFilter != CommitmentMacroFilter.ALL || hideSettledCommitments) {
                                Spacer(modifier = Modifier.height(10.dp))
                                TextButton(onClick = {
                                    activeMacroFilter = CommitmentMacroFilter.ALL
                                    onResetFilters()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = AccentPurple
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Reset Filters",
                                        color = AccentPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // SECTION 1: ACTION NEEDED / OVERDUE
                if (overdueOrActionNeeded.isNotEmpty()) {
                    item(key = "header_action_needed") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 6.dp, start = 2.dp, end = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ACTION NEEDED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SoftRed,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${overdueOrActionNeeded.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subtextColor
                                )
                            }
                        }
                    }

                    item(key = "container_action_needed") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            color = CardWhite,
                            border = BorderStroke(0.9.dp, SoftRed.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                overdueOrActionNeeded.forEachIndexed { index, bill ->
                                    AutoPayCommitmentRow(
                                        bill = bill,
                                        currencySymbol = userProfile.currencySymbol,
                                        isDiscreetMode = isDiscreetMode,
                                        isOutflow = isOutflow(bill),
                                        isInflow = isInflow(bill),
                                        currentDayOfMonth = currentDayOfMonth,
                                        onTap = { onTapBill(bill) },
                                        onEdit = { onEditBill(bill) },
                                        onDelete = { onDeleteBill(bill) },
                                        onSettle = {
                                            onSettleBill(bill, bill.amount, System.currentTimeMillis())
                                        }
                                    )
                                    if (index < overdueOrActionNeeded.lastIndex) {
                                        HorizontalDivider(
                                            color = BorderLight.copy(alpha = 0.5f),
                                            thickness = 0.6.dp,
                                            modifier = Modifier.padding(start = 54.dp, end = 14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // SECTION 2: UPCOMING
                if (upcomingBills.isNotEmpty()) {
                    item(key = "header_upcoming") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 6.dp, start = 2.dp, end = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "UPCOMING SCHEDULE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = headingColor,
                                    letterSpacing = 0.6.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${upcomingBills.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subtextColor
                                )
                            }
                        }
                    }

                    item(key = "container_upcoming") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(1.5.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                upcomingBills.forEachIndexed { index, bill ->
                                    AutoPayCommitmentRow(
                                        bill = bill,
                                        currencySymbol = userProfile.currencySymbol,
                                        isDiscreetMode = isDiscreetMode,
                                        isOutflow = isOutflow(bill),
                                        isInflow = isInflow(bill),
                                        currentDayOfMonth = currentDayOfMonth,
                                        onTap = { onTapBill(bill) },
                                        onEdit = { onEditBill(bill) },
                                        onDelete = { onDeleteBill(bill) },
                                        onSettle = {
                                            onSettleBill(bill, bill.amount, System.currentTimeMillis())
                                        }
                                    )
                                    if (index < upcomingBills.lastIndex) {
                                        HorizontalDivider(
                                            color = BorderLight.copy(alpha = 0.5f),
                                            thickness = 0.6.dp,
                                            modifier = Modifier.padding(start = 54.dp, end = 14.dp)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // SECTION 3: SETTLED & COMPLETED
                if (settledBills.isNotEmpty() && !hideSettledCommitments) {
                    item(key = "header_settled") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isSettledSectionExpanded = !isSettledSectionExpanded }
                                .padding(vertical = 4.dp),
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = SoftGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "SETTLED & COMPLETED",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = SoftGreen,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• ${settledBills.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = subtextColor
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isSettledSectionExpanded) "Hide" else "Show",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = subtextColor
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Toggle Settled",
                                        tint = subtextColor,
                                        modifier = Modifier
                                            .size(16.dp)
                                            .rotate(chevronRotation)
                                    )
                                }
                            }
                        }
                    }

                    if (isSettledSectionExpanded) {
                        item(key = "container_settled") {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(1.dp, RoundedCornerShape(18.dp)),
                                shape = RoundedCornerShape(18.dp),
                                color = CardWhite.copy(alpha = 0.9f),
                                border = BorderStroke(0.7.dp, BorderLight.copy(alpha = 0.6f))
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    settledBills.forEachIndexed { index, bill ->
                                        AutoPayCommitmentRow(
                                            bill = bill,
                                            currencySymbol = userProfile.currencySymbol,
                                            isDiscreetMode = isDiscreetMode,
                                            isOutflow = isOutflow(bill),
                                            isInflow = isInflow(bill),
                                            currentDayOfMonth = currentDayOfMonth,
                                            onTap = { onTapBill(bill) },
                                            onEdit = { onEditBill(bill) },
                                            onDelete = { onDeleteBill(bill) },
                                            onSettle = {
                                                onSettleBill(bill, bill.amount, System.currentTimeMillis())
                                            }
                                        )
                                        if (index < settledBills.lastIndex) {
                                            HorizontalDivider(
                                                color = BorderLight.copy(alpha = 0.5f),
                                                thickness = 0.6.dp,
                                                modifier = Modifier.padding(start = 54.dp, end = 14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun AutoPayCommitmentRow(
    bill: FixedBillEntity,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isOutflow: Boolean,
    isInflow: Boolean,
    currentDayOfMonth: Int,
    onTap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSettle: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val backgroundColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> AccentPurple
                    SwipeToDismissBoxValue.EndToStart -> SoftRed
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                animationSpec = tween(200),
                label = "swipeBg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Edit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    ) {
        val cleanTitle = remember(bill.title) {
            val raw = bill.title.trim()
            val firstParen = raw.indexOf('(')
            val secondParen = if (firstParen >= 0) raw.indexOf('(', firstParen + 1) else -1
            if (secondParen > firstParen) {
                raw.substring(0, secondParen).trim()
            } else raw
        }

        val subline = remember(bill.category, bill.accountName, bill.toAccountName, bill.type) {
            val accountRoute = if (bill.type == TransactionType.TRANSFER && !bill.toAccountName.isNullOrBlank()) {
                "${bill.accountName} ➔ ${bill.toAccountName}"
            } else bill.accountName
            if (bill.category.isNotBlank() && accountRoute.isNotBlank()) {
                "${bill.category} • $accountRoute"
            } else bill.category.ifBlank { accountRoute }
        }

        val (statusText, statusBg, statusTint) = remember(bill.isPaid, bill.dueDay, currentDayOfMonth, isOutflow, isInflow) {
            if (bill.isPaid) {
                Triple("Settled ✓", SoftGreen.copy(alpha = 0.12f), SoftGreen)
            } else {
                val due = bill.dueDay
                when {
                    due == null -> Triple("Scheduled", BorderLight.copy(alpha = 0.7f), TextMuted)
                    due < currentDayOfMonth -> {
                        if (isInflow) {
                            Triple("Awaiting Deposit (Exp ${due}th)", SoftTeal.copy(alpha = 0.12f), SoftTeal)
                        } else {
                            Triple("Overdue (Due ${due}th)", SoftRed.copy(alpha = 0.12f), SoftRed)
                        }
                    }
                    due == currentDayOfMonth -> Triple("Due Today", SoftAmber.copy(alpha = 0.12f), SoftAmber)
                    due - currentDayOfMonth <= 3 -> Triple("Due in ${due - currentDayOfMonth}d", SoftAmber.copy(alpha = 0.12f), SoftAmber)
                    else -> Triple("Due ${due}th", CanvasLight, TextMuted)
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onTap),
            color = CardWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (bill.isPaid) SoftGreen else Color.Transparent)
                        .then(
                            if (!bill.isPaid) {
                                Modifier.clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSettle()
                                }
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (bill.isPaid) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Settled",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    } else {
                        Surface(
                            modifier = Modifier.size(22.dp),
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.5.dp, BorderLight.copy(alpha = 0.9f))
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    Text(
                        text = cleanTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (bill.isPaid) TextDark.copy(alpha = 0.6f) else TextDark,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                delayMillis = 1500,
                                initialDelayMillis = 1500,
                                velocity = 30.dp
                            )
                    )

                    Text(
                        text = subline,
                        fontSize = 11.sp,
                        color = if (bill.isPaid) TextMuted.copy(alpha = 0.6f) else TextMuted,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(
                                iterations = Int.MAX_VALUE,
                                delayMillis = 2000,
                                initialDelayMillis = 2000,
                                velocity = 25.dp
                            )
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    val amountColor = when {
                        bill.isPaid -> TextDark.copy(alpha = 0.5f)
                        isInflow -> SoftGreen
                        bill.type == TransactionType.ASSET -> SoftTeal
                        bill.type == TransactionType.TRANSFER -> AccentPurple
                        bill.dueDay != null && bill.dueDay < currentDayOfMonth -> SoftRed
                        else -> TextDark
                    }

                    Text(
                        text = if (isDiscreetMode) "••••" else "${if (isInflow) "+" else if (isOutflow) "-" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", bill.amount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.5.sp,
                        color = amountColor
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = statusBg,
                        border = BorderStroke(0.5.dp, statusTint.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = statusText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTint,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
