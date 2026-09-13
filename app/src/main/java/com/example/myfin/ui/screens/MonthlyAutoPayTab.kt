package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.UserProfile
import com.example.myfin.ui.MonthlyUiState
import com.example.myfin.ui.components.SwipeableFixedBillItem
import com.example.myfin.ui.theme.*
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthlyAutoPayTab(
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    isDiscreetMode: Boolean,
    isCurrentMonth: Boolean,
    isPastMonth: Boolean,
    hideSettledCommitments: Boolean,
    selectedCommitmentFilter: TransactionType?,
    onSelectCommitmentFilter: (TransactionType?) -> Unit,
    onResetFilters: () -> Unit,
    onAddAutoPay: () -> Unit,
    onTapBill: (FixedBillEntity) -> Unit,
    onEditBill: (FixedBillEntity) -> Unit,
    onDeleteBill: (FixedBillEntity) -> Unit,
    onSettleBill: (FixedBillEntity, Double, Long) -> Unit
) {
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

    val overdueCount = remember(filteredBills, currentDayOfMonth, isCurrentMonth, isPastMonth) {
        filteredBills.count { bill ->
            if (bill.isPaid || bill.dueDay == null) false
            else when {
                isPastMonth -> true
                isCurrentMonth -> bill.dueDay!! < currentDayOfMonth
                else -> false
            }
        }
    }
    val hasOverdue = overdueCount > 0

    Column(modifier = Modifier.fillMaxSize()) {
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
                    onClick = onAddAutoPay,
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
                TransactionType.CORPORATE to "Corporate",
                TransactionType.TRANSFER to "Sweeps"
            ).forEach { (type, label) ->
                val isSelected = selectedCommitmentFilter == type
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isSelected) CardWhite else Color.Transparent)
                        .clickable { onSelectCommitmentFilter(type) }
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
                                TextButton(onClick = onResetFilters) {
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
                            onTap = { onTapBill(it) },
                            onEdit = { onEditBill(it) },
                            onDelete = { onDeleteBill(it) },
                            onSettleBill = { b, customAmt, dateMillis ->
                                onSettleBill(b, customAmt, dateMillis)
                            }
                        )
                    }
                }
            }
        }
    }
}
