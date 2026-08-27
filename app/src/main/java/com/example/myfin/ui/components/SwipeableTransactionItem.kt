package com.example.myfin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItem(
    transaction: TransactionEntity,
    currencySymbol: String,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit(transaction)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete(transaction)
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
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> AccentPurple
                    SwipeToDismissBoxValue.EndToStart -> SoftRed
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                label = "swipeBg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Edit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(18.dp))
                .clickable { onEdit(transaction) },
            shape = RoundedCornerShape(18.dp),
            color = CardWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    // Category Icon Avatar
                    val categoryIcon = getCategoryIcon(transaction.category, transaction.type)
                    val iconTint = when (transaction.type) {
                        TransactionType.INCOME -> SoftGreen
                        TransactionType.EXPENSE -> SoftRed
                        TransactionType.ASSET -> SoftTeal
                        TransactionType.TRANSFER -> AccentPurple
                    }

                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconTint.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryIcon,
                            contentDescription = transaction.category,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = transaction.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = TextDark,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = transaction.subcategory,
                                fontSize = 11.5.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = " • ",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = CanvasLight
                            ) {
                                Text(
                                    text = if (transaction.type == TransactionType.TRANSFER && transaction.toAccountName != null) {
                                        "${transaction.accountName} ➔ ${transaction.toAccountName}"
                                    } else {
                                        transaction.accountName
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Signed Amount Display
                val amountPrefix = when (transaction.type) {
                    TransactionType.EXPENSE -> "-"
                    TransactionType.INCOME -> "+"
                    TransactionType.ASSET -> "•"
                    TransactionType.TRANSFER -> "⇄"
                }

                val amountColor = when (transaction.type) {
                    TransactionType.INCOME -> SoftGreen
                    TransactionType.EXPENSE -> TextDark
                    TransactionType.ASSET -> SoftTeal
                    TransactionType.TRANSFER -> AccentPurple
                }

                Text(
                    text = "$amountPrefix$currencySymbol${String.format(Locale.US, "%,.2f", transaction.amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp,
                    color = amountColor
                )
            }
        }
    }
}

private fun getCategoryIcon(category: String, type: TransactionType): ImageVector {
    return when (type) {
        TransactionType.INCOME -> Icons.Default.TrendingUp
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
