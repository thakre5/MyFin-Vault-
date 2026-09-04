package com.example.myfin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableFixedBillItem(
    bill: FixedBillEntity,
    currencySymbol: String,
    onTap: (FixedBillEntity) -> Unit,
    onEdit: (FixedBillEntity) -> Unit,
    onDelete: (FixedBillEntity) -> Unit,
    onSettleBill: ((bill: FixedBillEntity, customAmount: Double, dateMillis: Long) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current

    val currentBill by rememberUpdatedState(bill)
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnDelete by rememberUpdatedState(onDelete)
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnSettle by rememberUpdatedState(onSettleBill)

    var showSettleDialog by remember { mutableStateOf(false) }
    var lastTargetValue by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }

    // Dynamic Overdue Calculation (Local val resolves smart cast error)
    val currentDayOfMonth = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    val isOverdue = remember(currentBill.isPaid, currentBill.dueDay, currentDayOfMonth) {
        val due = currentBill.dueDay
        !currentBill.isPaid && due != null && currentDayOfMonth > due
    }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnEdit(currentBill)
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnDelete(currentBill)
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
                label = "billSwipeBg"
            )

            val iconScale by animateFloatAsState(
                targetValue = if (isSwipingStart || isSwipingEnd) 1.15f else 0.85f,
                animationSpec = tween(150),
                label = "billIconScale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
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
                                    contentDescription = "Edit Commitment",
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
                                    contentDescription = "Delete Commitment",
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
                .shadow(1.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .border(
                    BorderStroke(
                        0.8.dp,
                        if (isOverdue) SoftRed.copy(alpha = 0.4f) else BorderLight.copy(alpha = 0.6f)
                    ),
                    RoundedCornerShape(18.dp)
                )
                .clickable {
                    if (!currentBill.isPaid && currentOnSettle != null) {
                        showSettleDialog = true
                    } else {
                        currentOnTap(currentBill)
                    }
                },
            shape = RoundedCornerShape(18.dp),
            color = CardWhite
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            if (!currentBill.isPaid && currentOnSettle != null) {
                                showSettleDialog = true
                            } else {
                                currentOnTap(currentBill)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (currentBill.isPaid) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = if (currentBill.isPaid) "Settled" else "Pending",
                            tint = when {
                                currentBill.isPaid -> SoftGreen
                                isOverdue -> SoftRed
                                else -> TextMuted
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = currentBill.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            val billDueDay = currentBill.dueDay
                            if (billDueDay != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isOverdue) SoftRed.copy(alpha = 0.12f) else CanvasLight,
                                    border = BorderStroke(0.6.dp, if (isOverdue) SoftRed.copy(alpha = 0.4f) else BorderLight)
                                ) {
                                    Text(
                                        text = if (isOverdue) "Due Day $billDueDay (Overdue)" else "Due Day $billDueDay",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isOverdue) SoftRed else TextMuted,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${currentBill.category} • ${currentBill.accountName}${if (currentBill.toAccountName != null) " ➔ " + currentBill.toAccountName else ""}",
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", currentBill.amount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = when (currentBill.type) {
                            TransactionType.INCOME -> SoftGreen
                            TransactionType.EXPENSE -> SoftRed
                            TransactionType.ASSET -> SoftTeal
                            TransactionType.TRANSFER -> AccentPurple
                        }
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            currentBill.isPaid -> "Settled"
                            isOverdue -> "Overdue"
                            else -> "Pending"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.5.sp,
                        color = when {
                            currentBill.isPaid -> SoftGreen
                            isOverdue -> SoftRed
                            else -> SoftAmber
                        }
                    )
                }
            }
        }
    }

    // Interactive Settle Dialog
    if (showSettleDialog) {
        var customAmountText by remember { mutableStateOf(currentBill.amount.toString()) }
        var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
        var showCalendarPicker by remember { mutableStateOf(false) }

        val isToday = remember(selectedDateMillis) {
            val calSelected = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            val calNow = Calendar.getInstance()
            calSelected.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calSelected.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
        }

        val isYesterday = remember(selectedDateMillis) {
            val calSelected = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
            calSelected.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
            calSelected.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)
        }

        val formattedDateLabel = remember(selectedDateMillis, isToday, isYesterday) {
            when {
                isToday -> "Today"
                isYesterday -> "Yesterday"
                else -> SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(selectedDateMillis))
            }
        }

        AlertDialog(
            onDismissRequest = { showSettleDialog = false },
            title = {
                Text(
                    text = "Settle ${currentBill.title}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextDark
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "From Vault: ${currentBill.accountName}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { input ->
                            customAmountText = input.filter { it.isDigit() || it == '.' }
                        },
                        label = { Text("Amount Paid ($currencySymbol)", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Payment Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = isToday,
                            onClick = { selectedDateMillis = System.currentTimeMillis() },
                            label = { Text("Today", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPurpleLight,
                                selectedLabelColor = AccentPurple
                            )
                        )

                        FilterChip(
                            selected = isYesterday,
                            onClick = {
                                val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                selectedDateMillis = cal.timeInMillis
                            },
                            label = { Text("Yesterday", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPurpleLight,
                                selectedLabelColor = AccentPurple
                            )
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clickable { showCalendarPicker = true },
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isToday && !isYesterday) AccentPurpleLight else CanvasLight,
                            border = BorderStroke(0.8.dp, if (!isToday && !isYesterday) AccentPurple else BorderLight)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (!isToday && !isYesterday) formattedDateLabel else "Date",
                                    fontSize = 11.sp,
                                    fontWeight = if (!isToday && !isYesterday) FontWeight.Bold else FontWeight.Medium,
                                    color = if (!isToday && !isYesterday) AccentPurple else TextDark
                                )
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date",
                                    tint = if (!isToday && !isYesterday) AccentPurple else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val parsed = customAmountText.toDoubleOrNull() ?: currentBill.amount
                        currentOnSettle?.invoke(currentBill, parsed, selectedDateMillis)
                        showSettleDialog = false
                    }
                ) {
                    Text("Confirm Paid", color = AccentPurple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettleDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )

        if (showCalendarPicker) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = selectedDateMillis
            )

            DatePickerDialog(
                onDismissRequest = { showCalendarPicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { pickedUtc ->
                                val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                    timeInMillis = pickedUtc
                                }
                                val localCal = Calendar.getInstance().apply {
                                    set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                                    set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                                    set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                                    set(Calendar.HOUR_OF_DAY, 12)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                selectedDateMillis = localCal.timeInMillis
                            }
                            showCalendarPicker = false
                        }
                    ) {
                        Text("Select", fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCalendarPicker = false }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}
