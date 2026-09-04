package com.example.myfin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
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

    // Extract local properties to avoid complex expression smart-cast issues
    val txTitle = currentTx.title.trim()
    val txSubcategory = currentTx.subcategory.trim()
    val txCategory = currentTx.category.trim()
    val txAccountName = currentTx.accountName.trim()
    val txToAccountName = currentTx.toAccountName?.trim()
    val txType = currentTx.type
    val txAmount = currentTx.amount
    val txDate = currentTx.date
    val txLinkedFixedBillId = currentTx.linkedFixedBillId

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
                    .clip(RoundedCornerShape(18.dp))
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
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Entry",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
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
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Entry",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        // Row 1 Title: Format as Subcategory (Title) if title is distinct, otherwise Subcategory
        val displayTitle = remember(txTitle, txSubcategory) {
            val isRedundant = txTitle.isBlank() ||
                txTitle.equals(txSubcategory, ignoreCase = true) ||
                txTitle.startsWith("Vault Transfer", ignoreCase = true) ||
                (txSubcategory.isNotBlank() && txSubcategory.contains(txTitle, ignoreCase = true) && txSubcategory.length - txTitle.length <= 4) ||
                (txTitle.isNotBlank() && txTitle.contains(txSubcategory, ignoreCase = true) && txTitle.length - txSubcategory.length <= 4)

            if (!isRedundant && txSubcategory.isNotBlank()) {
                "$txSubcategory ($txTitle)"
            } else {
                txSubcategory.ifBlank { txTitle.ifBlank { "Transaction" } }
            }
        }

        // Row 2 Bank Route Text
        val bankRouteText = remember(txAccountName, txToAccountName, txType) {
            if (txType == TransactionType.TRANSFER && !txToAccountName.isNullOrBlank()) {
                "${txAccountName.uppercase()} ➔ ${txToAccountName.uppercase()}"
            } else {
                txAccountName.uppercase()
            }
        }

        val categoryIcon = getCategoryIcon(txCategory, txType)
        val iconTint = when (txType) {
            TransactionType.INCOME -> SoftGreen
            TransactionType.EXPENSE -> SoftRed
            TransactionType.ASSET -> SoftTeal
            TransactionType.TRANSFER -> AccentPurple
        }

        val formattedDateTime = remember(txDate) {
            formatContextualDateTime(txDate)
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .border(0.8.dp, BorderLight.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                .clickable { currentOnTap(currentTx) },
            shape = RoundedCornerShape(18.dp),
            color = CardWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
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

                Spacer(modifier = Modifier.width(11.dp))

                // Center Column: Row 1 Subcategory (Title) + Row 2 (50% Category | 50% Bank)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.5.dp)
                ) {
                    // Row 1: Subcategory (Title) with continuous Marquee
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
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    delayMillis = 1200,
                                    initialDelayMillis = 1200,
                                    velocity = 30.dp
                                )
                        )

                        if (txLinkedFixedBillId != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AccentPurpleLight,
                                border = BorderStroke(0.5.dp, AccentPurple.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "AutoPay",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentPurple,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Row 2: 50% Category & 50% Bank with continuous Marquee
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 50% Category
                        Text(
                            text = txCategory,
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    delayMillis = 1800,
                                    initialDelayMillis = 1800,
                                    velocity = 25.dp
                                )
                        )

                        // 50% Bank Vault Route
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = CanvasLight,
                            border = BorderStroke(0.6.dp, BorderLight),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = bankRouteText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (txType == TransactionType.TRANSFER) AccentPurple else TextDark.copy(alpha = 0.75f),
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .basicMarquee(
                                        iterations = Int.MAX_VALUE,
                                        delayMillis = 1800,
                                        initialDelayMillis = 1800,
                                        velocity = 25.dp
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Right Block: Amount & Smart Contextual Timestamp Vertically Centered
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    val amountPrefix = when (txType) {
                        TransactionType.EXPENSE -> "-"
                        TransactionType.INCOME -> "+"
                        TransactionType.ASSET -> "•"
                        TransactionType.TRANSFER -> "⇄"
                    }

                    val amountColor = when (txType) {
                        TransactionType.INCOME -> SoftGreen
                        TransactionType.EXPENSE -> TextDark
                        TransactionType.ASSET -> SoftTeal
                        TransactionType.TRANSFER -> AccentPurple
                    }

                    Text(
                        text = "$amountPrefix$currencySymbol${String.format(Locale.US, "%,.2f", txAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.5.sp,
                        color = amountColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = formattedDateTime,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

private fun formatContextualDateTime(timestamp: Long): String {
    val txCal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val nowCal = Calendar.getInstance()
    val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(timestamp))

    val isSameDay = txCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            txCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

    val isYesterday = txCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
            txCal.get(Calendar.DAY_OF_YEAR) == (nowCal.get(Calendar.DAY_OF_YEAR) - 1)

    val isSameYear = txCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)

    return when {
        isSameDay -> timeStr
        isYesterday -> "Yesterday, $timeStr"
        isSameYear -> "${SimpleDateFormat("dd MMM", Locale.US).format(Date(timestamp))}, $timeStr"
        else -> "${SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(timestamp))}, $timeStr"
    }
}

private fun getCategoryIcon(category: String, type: TransactionType): ImageVector {
    return when (type) {
        TransactionType.INCOME -> Icons.AutoMirrored.Filled.TrendingUp
        TransactionType.ASSET -> Icons.Default.Savings
        TransactionType.TRANSFER -> Icons.Default.SyncAlt
        TransactionType.EXPENSE -> when (category) {
            "Utilities & Living Bills" -> Icons.Default.Bolt
            "Everyday Living" -> Icons.Default.ShoppingCart
            "Leisure, Trips & Media" -> Icons.Default.FlightTakeoff
            "Health & Medical" -> Icons.Default.LocalHospital
            "Family & Home Support" -> Icons.Default.Favorite
            "Debt & Financial Obligations" -> Icons.Default.CreditCard
            "Work & Professional" -> Icons.Default.Work
            else -> Icons.Default.Receipt
        }
    }
}
