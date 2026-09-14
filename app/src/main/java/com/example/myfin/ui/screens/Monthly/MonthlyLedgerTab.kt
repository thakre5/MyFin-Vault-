package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    Column(modifier = Modifier.fillMaxSize()) {
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

                IconButton(onClick = onOpenFilterSheet, modifier = Modifier.size(28.dp)) {
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
                TransactionType.CORPORATE to "Corporate",
                TransactionType.TRANSFER to "Transfers"
            ).forEach { (type, label) ->
                val isSelected = filterCriteria.type == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) CardWhite else Color.Transparent)
                        .clickable {
                            viewModel.updateFilter(type, filterCriteria.account, filterCriteria.startDate, filterCriteria.endDate)
                        }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 10.sp,
                        color = if (isSelected) {
                            when (type) {
                                TransactionType.EXPENSE -> SoftRed
                                TransactionType.INCOME -> SoftGreen
                                TransactionType.ASSET -> SoftTeal
                                TransactionType.CORPORATE -> Color(0xFFE57A28)
                                TransactionType.TRANSFER -> AccentPurple
                                else -> TextDark
                            }
                        } else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                                TextButton(onClick = { viewModel.resetFilters() }) {
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
                                onTap = { onViewTx(it) },
                                onEdit = { onEditTx(it) },
                                onDelete = { onDeleteTx(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
