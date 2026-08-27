package com.example.myfin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.5.dp, RoundedCornerShape(16.dp))
                .clickable { onEdit(transaction) },
            shape = RoundedCornerShape(16.dp),
            color = CardWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                when (transaction.type) {
                                    TransactionType.INCOME -> SoftGreen.copy(alpha = 0.12f)
                                    TransactionType.EXPENSE -> SoftRed.copy(alpha = 0.12f)
                                    TransactionType.ASSET -> SoftTeal.copy(alpha = 0.12f)
                                    TransactionType.TRANSFER -> AccentPurple.copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = transaction.category.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = when (transaction.type) {
                                TransactionType.INCOME -> SoftGreen
                                TransactionType.EXPENSE -> SoftRed
                                TransactionType.ASSET -> SoftTeal
                                TransactionType.TRANSFER -> AccentPurple
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = transaction.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${transaction.category} • ${transaction.subcategory}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            if (transaction.type == TransactionType.TRANSFER && transaction.toAccountName != null) {
                                Text(
                                    text = " (${transaction.accountName} ➔ ${transaction.toAccountName})",
                                    fontSize = 10.5.sp,
                                    color = AccentPurple,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Text(
                                    text = " [${transaction.accountName}]",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "${if (transaction.type == TransactionType.EXPENSE) "-" else if (transaction.type == TransactionType.INCOME) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.2f", transaction.amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp,
                    color = when (transaction.type) {
                        TransactionType.INCOME -> SoftGreen
                        TransactionType.EXPENSE -> SoftRed
                        TransactionType.ASSET -> SoftTeal
                        TransactionType.TRANSFER -> AccentPurple
                    }
                )
            }
        }
    }
}
