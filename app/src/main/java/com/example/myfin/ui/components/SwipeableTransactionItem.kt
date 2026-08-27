package com.example.myfin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 22.dp),
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
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit Entry",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Edit",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
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
                            fontSize = 13.5.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete Entry",
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .border(0.8.dp, BorderLight.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .clickable { currentOnTap(currentTx) },
            shape = RoundedCornerShape(20.dp),
            color = CardWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Block: Icon + Breadcrumb Info
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    val categoryIcon = getCategoryIcon(currentTx.category, currentTx.type)
                    val iconTint = when (currentTx.type) {
                        TransactionType.INCOME -> SoftGreen
                        TransactionType.EXPENSE -> SoftRed
                        TransactionType.ASSET -> SoftTeal
                        TransactionType.TRANSFER -> AccentPurple
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(iconTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = currentTx.category,
                            tint = iconTint,
                            modifier = Modifier.size(21.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(13.dp))

                    Column {
                        Text(
                            text = currentTx.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextDark,
                            maxLines = 1
                        )

                        Spacer(modifier = Modifier.height(3.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "${currentTx.category} • ${currentTx.subcategory}",
                                fontSize = 11.5.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Normal
                            )

                            Surface(
                                shape = RoundedCornerShape(5.dp),
                                color = CanvasLight,
                                border = androidx.compose.foundation.BorderStroke(0.6.dp, BorderLight)
                            ) {
                                Text(
                                    text = if (currentTx.type == TransactionType.TRANSFER && currentTx.toAccountName != null) {
                                        "${currentTx.accountName} ➔ ${currentTx.toAccountName}"
                                    } else {
                                        currentTx.accountName
                                    },
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentTx.type == TransactionType.TRANSFER) AccentPurple else TextDark.copy(alpha = 0.75f),
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Block: Amount & Time
                Column(horizontalAlignment = Alignment.End) {
                    val amountPrefix = when (currentTx.type) {
                        TransactionType.EXPENSE -> "-"
                        TransactionType.INCOME -> "+"
                        TransactionType.ASSET -> "•"
                        TransactionType.TRANSFER -> "⇄"
                    }

                    val amountColor = when (currentTx.type) {
                        TransactionType.INCOME -> SoftGreen
                        TransactionType.EXPENSE -> TextDark
                        TransactionType.ASSET -> SoftTeal
                        TransactionType.TRANSFER -> AccentPurple
                    }

                    Text(
                        text = "$amountPrefix$currencySymbol${String.format(Locale.US, "%,.2f", currentTx.amount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = amountColor
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.US) }
                    Text(
                        text = timeFormatter.format(Date(currentTx.date)),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted.copy(alpha = 0.8f)
                    )
                }
            }
        }
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
