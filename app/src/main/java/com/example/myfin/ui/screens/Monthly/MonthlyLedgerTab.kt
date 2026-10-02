package com.example.myfin.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.UserProfile
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.FilterCriteria
import com.example.myfin.ui.MonthlyUiState
import com.example.myfin.ui.components.SwipeableTransactionItem
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MonthlyLedgerTab(
    viewModel: BudgetViewModel,
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    filterCriteria: FilterCriteria,
    accountsList: List<String>,
    isDiscreetMode: Boolean,
    onOpenFilterSheet: () -> Unit,
    onViewTx: (TransactionEntity) -> Unit,
    onEditTx: (TransactionEntity) -> Unit,
    onDeleteTx: (TransactionEntity) -> Unit
) {
    var isSearchExpanded by remember { mutableStateOf(filterCriteria.query.isNotBlank()) }

    val hasActiveCustomFilters = filterCriteria.type != null ||
            (filterCriteria.account.isNotBlank() && filterCriteria.account != "ALL") ||
            filterCriteria.startDate != null ||
            filterCriteria.endDate != null

    val isSearchingOrFiltered = filterCriteria.query.isNotBlank() || hasActiveCustomFilters

    // Active Vault Balance: Sum of balances excluding Fortress
    val activeVaultBalance = remember(uiState.activeAccounts, uiState.accounts) {
        val list = uiState.activeAccounts.ifEmpty { uiState.accounts.filter { !it.isArchived } }
        list.filter { !it.accountType.equals("Fortress", ignoreCase = true) }
            .sumOf { it.currentBalance }
    }

    // Monthly Inflow, Outflow & Remaining calculation
    val monthlyInflow = uiState.metrics.actualIncome
    val monthlyOutflow = uiState.metrics.lifestyleExpenses + uiState.metrics.actualAssets
    val remainingFromInflow = monthlyInflow - monthlyOutflow

    val dateRangeLabel = remember(filterCriteria.startDate, filterCriteria.endDate) {
        val start = filterCriteria.startDate
        val end = filterCriteria.endDate
        if (start != null || end != null) {
            val sdf = SimpleDateFormat("dd MMM", Locale.US)
            val startStr = start?.let { sdf.format(Date(it)) }
            val endStr = end?.let { sdf.format(Date(it)) }
            when {
                startStr != null && endStr != null -> "$startStr – $endStr"
                startStr != null -> "From $startStr"
                else -> "Until $endStr"
            }
        } else null
    }

    val dateKeys = remember(uiState.groupedTransactions) {
        uiState.groupedTransactions.keys.toList()
    }
    val expandedDates = remember(uiState.groupedTransactions, isSearchingOrFiltered) {
        mutableStateMapOf<String, Boolean>().apply {
            dateKeys.forEachIndexed { index, dateKey ->
                put(dateKey, if (isSearchingOrFiltered) true else index < 2)
            }
        }
    }

    // Collapsible Sheet Motion Mechanics
    val maxDeckHeight = 160.dp
    val maxDeckPx = with(LocalDensity.current) { maxDeckHeight.toPx() }
    var deckOffsetPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < 0f) { // Scrolling up -> collapse dark deck
                    val newOffset = (deckOffsetPx + delta).coerceIn(-maxDeckPx, 0f)
                    val consumed = newOffset - deckOffsetPx
                    deckOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta > 0f) { // Scrolling down -> restore dark deck
                    val newOffset = (deckOffsetPx + delta).coerceIn(-maxDeckPx, 0f)
                    val consumed = newOffset - deckOffsetPx
                    deckOffsetPx = newOffset
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }
        }
    }

    val isFullyCollapsed = deckOffsetPx <= -maxDeckPx + 4f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
    ) {
        // 1. TOP MIDNIGHT DECK (Collapsible on Scroll)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxDeckHeight)
                .graphicsLayer {
                    translationY = deckOffsetPx
                    alpha = ((maxDeckPx + deckOffsetPx) / maxDeckPx).coerceIn(0f, 1f)
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E1B4B)
                        )
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .clip(RoundedCornerShape(22.dp))
        ) {
            // Dark Deck Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ACTIVE VAULT BALANCE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFA5B4FC),
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    if (isDiscreetMode) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "  ● ● ● ● ●  ",
                                fontSize = 18.sp,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", activeVaultBalance)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.6).sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Inflow • Outflow • Remaining Subtext
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isDiscreetMode) "•••• in" else "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", monthlyInflow)} in",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SoftGreen
                        )
                        Text(text = "•", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text(
                            text = if (isDiscreetMode) "•••• out" else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", monthlyOutflow)} out",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFCA5A5)
                        )
                        Text(text = "•", fontSize = 10.sp, color = Color(0xFF64748B))
                        Text(
                            text = if (isDiscreetMode) "•••• left" else "${if (remainingFromInflow >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", remainingFromInflow)} left",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (remainingFromInflow >= 0) Color(0xFF38BDF8) else SoftRed
                        )
                    }
                }

                // Reference-style Vertical Card Peek on the right edge
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(88.dp)
                        .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF38BDF8),
                                    Color(0xFF6366F1),
                                    Color(0xFFFBBF24)
                                )
                            )
                        )
                        .shadow(4.dp, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f))
                    )
                }
            }
        }

        // 2. SLIDING WHITE SHEET CONTAINER (Expands to 100% on scroll)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(0, (maxDeckPx + deckOffsetPx).roundToInt()) },
            shape = RoundedCornerShape(
                topStart = if (isFullyCollapsed) 0.dp else 24.dp,
                topEnd = if (isFullyCollapsed) 0.dp else 24.dp
            ),
            color = CanvasLight,
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp)
            ) {
                // Tactical Drag Pill Handle
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(BorderLight.copy(alpha = 0.8f))
                    )
                }

                // Pinned Header: Transactions Title + Inline Expanding Search + Filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transactions",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        letterSpacing = (-0.4).sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        modifier = Modifier.weight(1f, fill = isSearchExpanded),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isSearchExpanded) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = CardWhite,
                                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search",
                                        tint = TextMuted,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))

                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (filterCriteria.query.isEmpty()) {
                                            Text(
                                                text = "Search...",
                                                color = TextMuted,
                                                fontSize = 12.sp,
                                                maxLines = 1
                                            )
                                        }
                                        BasicTextField(
                                            value = filterCriteria.query,
                                            onValueChange = { viewModel.updateSearchQuery(it) },
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 12.5.sp,
                                                color = TextDark,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            cursorBrush = SolidColor(AccentPurple),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.updateSearchQuery("")
                                            isSearchExpanded = false
                                        },
                                        modifier = Modifier.size(22.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close Search",
                                            tint = TextMuted,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        } else {
                            IconButton(
                                onClick = { isSearchExpanded = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = TextDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Filter button with active dot
                        IconButton(
                            onClick = onOpenFilterSheet,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.TopEnd) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = "Filter",
                                    tint = if (hasActiveCustomFilters) AccentPurple else TextDark,
                                    modifier = Modifier.size(20.dp)
                                )
                                if (hasActiveCustomFilters) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(AccentPurple)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active filter pills
                if (hasActiveCustomFilters) {
                    Spacer(modifier = Modifier.height(2.dp))
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        filterCriteria.type?.let { type ->
                            item {
                                RemovableFilterChip(
                                    label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    onRemove = {
                                        viewModel.updateFilter(
                                            null,
                                            filterCriteria.account,
                                            filterCriteria.startDate,
                                            filterCriteria.endDate
                                        )
                                    }
                                )
                            }
                        }

                        if (filterCriteria.account.isNotBlank() && filterCriteria.account != "ALL") {
                            item {
                                RemovableFilterChip(
                                    label = filterCriteria.account,
                                    onRemove = {
                                        viewModel.updateFilter(
                                            filterCriteria.type,
                                            "ALL",
                                            filterCriteria.startDate,
                                            filterCriteria.endDate
                                        )
                                    }
                                )
                            }
                        }

                        dateRangeLabel?.let { label ->
                            item {
                                RemovableFilterChip(
                                    label = label,
                                    icon = Icons.Default.DateRange,
                                    onRemove = {
                                        viewModel.updateFilter(
                                            filterCriteria.type,
                                            filterCriteria.account,
                                            null,
                                            null
                                        )
                                    }
                                )
                            }
                        }

                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { viewModel.resetFilters() }
                            ) {
                                Text(
                                    text = "Reset All",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPurple,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = BorderLight.copy(alpha = 0.5f),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                )

                // 3. TRANSACTION LIST WITH SMART DAILY ACCORDION
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp),
                    contentPadding = PaddingValues(top = 2.dp, bottom = 180.dp)
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
                                    Text(
                                        text = "No transactions recorded",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Try clearing search or filters",
                                        fontSize = 12.sp,
                                        color = TextMuted
                                    )
                                    if (filterCriteria.query.isNotBlank() || hasActiveCustomFilters) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        TextButton(onClick = { viewModel.resetFilters() }) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
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
                        uiState.groupedTransactions.forEach { (dateHeader, txList) ->
                            val dailyInflow = txList.filter {
                                it.type == TransactionType.INCOME ||
                                        (it.type == TransactionType.CORPORATE && it.category.equals("Reimbursements & Claims", ignoreCase = true))
                            }.sumOf { it.amount }

                            val dailyOutflow = txList.filter {
                                it.type == TransactionType.EXPENSE ||
                                        it.type == TransactionType.ASSET ||
                                        (it.type == TransactionType.CORPORATE && !it.category.equals("Reimbursements & Claims", ignoreCase = true))
                            }.sumOf { it.amount }

                            val dailyTransferVolume = txList.filter { it.type == TransactionType.TRANSFER }.sumOf { it.amount }
                            val sortedTxList = txList.sortedByDescending { it.date }
                            val isExpanded = expandedDates[dateHeader] ?: true

                            if (isExpanded) {
                                stickyHeader(key = "header_$dateHeader") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = CanvasLight
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { expandedDates[dateHeader] = false }
                                                .padding(top = 12.dp, bottom = 6.dp, start = 2.dp, end = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = dateHeader.uppercase(),
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 11.5.sp,
                                                    color = TextDark,
                                                    letterSpacing = 0.5.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• ${txList.size} ${if (txList.size == 1) "entry" else "entries"}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = TextMuted
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Collapse",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (dailyInflow > 0.0) {
                                                    Text(
                                                        text = if (isDiscreetMode) "••••" else "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyInflow)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = SoftGreen
                                                    )
                                                }
                                                if (dailyOutflow > 0.0) {
                                                    Text(
                                                        text = if (isDiscreetMode) "••••" else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyOutflow)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = TextDark
                                                    )
                                                }
                                                if (filterCriteria.type == TransactionType.TRANSFER && dailyTransferVolume > 0.0) {
                                                    Text(
                                                        text = if (isDiscreetMode) "••••" else "⇄ ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyTransferVolume)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = AccentPurple
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                item(key = "container_$dateHeader") {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .shadow(1.5.dp, RoundedCornerShape(18.dp))
                                            .clip(RoundedCornerShape(18.dp)),
                                        shape = RoundedCornerShape(18.dp),
                                        color = CardWhite,
                                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            sortedTxList.forEachIndexed { index, tx ->
                                                SwipeableTransactionItem(
                                                    transaction = tx,
                                                    currencySymbol = userProfile.currencySymbol,
                                                    onTap = { onViewTx(it) },
                                                    onEdit = { onEditTx(it) },
                                                    onDelete = { onDeleteTx(it) }
                                                )
                                                if (index < sortedTxList.lastIndex) {
                                                    HorizontalDivider(
                                                        color = BorderLight.copy(alpha = 0.5f),
                                                        thickness = 0.6.dp,
                                                        modifier = Modifier.padding(start = 64.dp, end = 14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            } else {
                                item(key = "collapsed_$dateHeader") {
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 3.dp)
                                            .shadow(1.dp, RoundedCornerShape(14.dp))
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { expandedDates[dateHeader] = true },
                                        shape = RoundedCornerShape(14.dp),
                                        color = CardWhite,
                                        border = BorderStroke(0.7.dp, BorderLight.copy(alpha = 0.7f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (dailyInflow > dailyOutflow) SoftGreen
                                                            else if (dailyOutflow > 0) SoftRed
                                                            else AccentPurple
                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = dateHeader.uppercase(),
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = TextDark,
                                                    letterSpacing = 0.3.sp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "• ${txList.size} ${if (txList.size == 1) "entry" else "entries"}",
                                                    fontSize = 11.sp,
                                                    color = TextMuted
                                                )
                                            }

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (dailyInflow > 0.0) {
                                                    Text(
                                                        text = if (isDiscreetMode) "••••" else "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyInflow)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = SoftGreen
                                                    )
                                                }
                                                if (dailyOutflow > 0.0) {
                                                    Text(
                                                        text = if (isDiscreetMode) "••••" else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyOutflow)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 11.sp,
                                                        color = TextDark
                                                    )
                                                }
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Expand",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(16.dp)
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
        }
    }
}

@Composable
private fun RemovableFilterChip(
    label: String,
    icon: ImageVector? = null,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = AccentPurple.copy(alpha = 0.10f),
        border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentPurple
            )
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                tint = AccentPurple,
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove)
                    .padding(2.dp)
            )
        }
    }
}
