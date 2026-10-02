package com.example.myfin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.TransferSubtype
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SwipeableTransactionItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    onTap: (TransactionEntity) -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val currentTx by rememberUpdatedState(transaction)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnDelete by rememberUpdatedState(onDelete)

    val txTitle = currentTx.title.trim()
    val txSubcategory = currentTx.subcategory.trim()
    val txCategory = currentTx.category.trim()
    val txAccountName = currentTx.accountName.trim()
    val txToAccountName = currentTx.toAccountName?.trim()
    val txType = currentTx.type
    val txAmount = currentTx.amount
    val txDate = currentTx.date
    val txLinkedFixedBillId = currentTx.linkedFixedBillId
    val txSubtype = currentTx.transferSubtype

    var lastTargetValue by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnEdit(currentTx)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnDelete(currentTx)
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != lastTargetValue && dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastTargetValue = dismissState.targetValue
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val isSwipingStart = dismissState.targetValue == SwipeToDismissBoxValue.StartToEnd
            val isSwipingEnd = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart

            val backgroundColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> AccentPurple
                    SwipeToDismissBoxValue.EndToStart -> SoftRed
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                animationSpec = tween(200),
                label = "swipeBgColor"
            )

            val iconScale by animateFloatAsState(
                targetValue = if (isSwipingStart || isSwipingEnd) 1.15f else 0.85f,
                animationSpec = tween(150),
                label = "iconScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.scale(iconScale)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Entry",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.scale(iconScale)
                    ) {
                        Text(
                            text = "Delete",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Entry",
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        val friendlySubcategory = remember(txSubcategory, txType, txSubtype) {
            if (txType == TransactionType.TRANSFER) {
                when {
                    txSubtype == TransferSubtype.WEALTH_ALLOCATION || txSubcategory.equals("WEALTH_ALLOCATION", ignoreCase = true) -> "Fortress Sweep"
                    txSubtype == TransferSubtype.BILL_FUNDING || txSubcategory.equals("BILL_FUNDING", ignoreCase = true) -> "Bill Funding"
                    txSubtype == TransferSubtype.REBALANCE || txSubcategory.equals("REBALANCE", ignoreCase = true) -> "Vault Rebalance"
                    txSubtype == TransferSubtype.CASH_WITHDRAWAL || txSubcategory.equals("CASH_WITHDRAWAL", ignoreCase = true) -> "Cash ATM Withdrawal"
                    else -> txSubcategory.trim().ifBlank { "Vault Sweep" }
                }
            } else {
                txSubcategory.trim()
            }
        }

        val displayTitle = remember(txTitle, friendlySubcategory) {
            val isRedundant = txTitle.isBlank() ||
                txTitle.equals(friendlySubcategory, ignoreCase = true) ||
                txTitle.startsWith("Vault Transfer", ignoreCase = true) ||
                (friendlySubcategory.isNotBlank() && friendlySubcategory.contains(txTitle, ignoreCase = true) && friendlySubcategory.length - txTitle.length <= 4) ||
                (txTitle.isNotBlank() && txTitle.contains(friendlySubcategory, ignoreCase = true) && txTitle.length - friendlySubcategory.length <= 4)

            if (!isRedundant && friendlySubcategory.isNotBlank()) {
                "$friendlySubcategory ($txTitle)"
            } else {
                friendlySubcategory.ifBlank { txTitle.ifBlank { "Transaction" } }
            }
        }

        val bankRouteText = remember(txAccountName, txToAccountName, txType) {
            if (txType == TransactionType.TRANSFER && !txToAccountName.isNullOrBlank()) {
                "${txAccountName.uppercase()} ➔ ${txToAccountName.uppercase()}"
            } else {
                txAccountName
            }
        }

        val sublineText = remember(txCategory, bankRouteText) {
            if (txCategory.isNotBlank() && bankRouteText.isNotBlank()) {
                "$txCategory • $bankRouteText"
            } else {
                txCategory.ifBlank { bankRouteText }
            }
        }

        val categoryIcon = getCategoryIcon(txCategory, txType, txSubtype, txSubcategory)
        val iconTint = when (txType) {
            TransactionType.INCOME -> SoftGreen
            TransactionType.EXPENSE -> SoftRed
            TransactionType.ASSET -> SoftTeal
            TransactionType.CORPORATE -> Color(0xFFE57A28)
            TransactionType.TRANSFER -> AccentPurple
        }

        // Clean time format (e.g. 05:30 PM) without redundant "Yesterday" label
        val formattedTime = remember(txDate) {
            SimpleDateFormat("hh:mm a", Locale.US).format(Date(txDate))
        }

        // Flat row surface inside the parent daily container
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { currentOnTap(currentTx) },
            color = CardWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Icon Box
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(iconTint.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = txCategory,
                        tint = iconTint,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center Column: Title + Category & Bank
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.5.dp)
                ) {
                    // Line 1: Title + optional AutoPay badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = displayTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (txLinkedFixedBillId != null) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentPurpleLight,
                                border = BorderStroke(0.5.dp, AccentPurple.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "AutoPay",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPurple,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Line 2: Category • Bank (clean text, no clipped badges)
                    Text(
                        text = sublineText,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Column: Amount & Clean Time
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    val isCorporateInflow = txType == TransactionType.CORPORATE &&
                        txCategory.equals("Reimbursements & Claims", ignoreCase = true)

                    val amountPrefix = when (txType) {
                        TransactionType.EXPENSE -> "-"
                        TransactionType.INCOME -> "+"
                        TransactionType.ASSET -> "•"
                        TransactionType.CORPORATE -> if (isCorporateInflow) "+" else "-"
                        TransactionType.TRANSFER -> "⇄"
                    }

                    val amountColor = when (txType) {
                        TransactionType.INCOME -> SoftGreen
                        TransactionType.EXPENSE -> TextDark
                        TransactionType.ASSET -> SoftTeal
                        TransactionType.CORPORATE -> if (isCorporateInflow) SoftGreen else Color(0xFFE57A28)
                        TransactionType.TRANSFER -> AccentPurple
                    }

                    Text(
                        text = "$amountPrefix$currencySymbol${String.format(Locale.US, "%,.2f", txAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = amountColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formattedTime,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private fun getCategoryIcon(
    category: String,
    type: TransactionType,
    transferSubtype: TransferSubtype = TransferSubtype.NONE,
    subcategory: String = ""
): ImageVector {
    return when (type) {
        TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
        TransactionType.ASSET -> Icons.Default.Savings
        TransactionType.TRANSFER -> {
            if (transferSubtype == TransferSubtype.CASH_WITHDRAWAL || subcategory.equals("CASH_WITHDRAWAL", ignoreCase = true)) {
                Icons.Default.AccountBalanceWallet
            } else {
                Icons.Default.SyncAlt
            }
        }
        TransactionType.CORPORATE -> when {
            category.equals("Reimbursements & Claims", ignoreCase = true) -> Icons.Default.AccountBalance
            else -> Icons.Default.Work
        }
        TransactionType.EXPENSE -> when (category) {
            "Utilities & Living Bills" -> Icons.Default.Bolt
            "Everyday Living" -> Icons.Default.ShoppingCart
            "Leisure, Trips & Media" -> Icons.Default.FlightTakeoff
            "Health & Medical" -> Icons.Default.LocalHospital
            "Family & Home Support" -> Icons.Default.Favorite
            "Debt & Financial Obligations" -> Icons.Default.CreditCard
            else -> Icons.Default.Receipt
        }
    }
}
