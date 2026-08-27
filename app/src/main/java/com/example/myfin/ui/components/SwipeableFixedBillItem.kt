package com.example.myfin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableFixedBillItem(
    bill: FixedBillEntity,
    currencySymbol: String,
    onTap: (FixedBillEntity) -> Unit,
    onEdit: (FixedBillEntity) -> Unit,
    onDelete: (FixedBillEntity) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit(bill)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete(bill)
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
                label = "billSwipeBg"
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
                .shadow(2.dp, RoundedCornerShape(16.dp))
                .clickable { onTap(bill) },
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
                    IconButton(
                        onClick = { onTap(bill) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (bill.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (bill.isPaid) "Settled" else "Pending",
                            tint = if (bill.isPaid) SoftGreen else TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = bill.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            if (bill.dueDay != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CanvasLight
                                ) {
                                    Text(
                                        text = "Due Day ${bill.dueDay}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${bill.category} • ${bill.accountName}${if (bill.toAccountName != null) " ➔ " + bill.toAccountName else ""}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${currencySymbol}${String.format(Locale.US, "%,.0f", bill.amount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = when (bill.type) {
                            TransactionType.INCOME -> SoftGreen
                            TransactionType.EXPENSE -> SoftRed
                            TransactionType.ASSET -> SoftTeal
                            TransactionType.TRANSFER -> AccentPurple
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (bill.isPaid) "Settled" else "Pending",
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = if (bill.isPaid) SoftGreen else SoftAmber
                    )
                }
            }
        }
    }
}
