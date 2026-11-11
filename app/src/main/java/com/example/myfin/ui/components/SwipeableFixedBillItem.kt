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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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

    val billTitle = currentBill.title
    val billSubcategory = currentBill.subcategory
    val billCategory = currentBill.category
    val billAccountName = currentBill.accountName
    val billToAccountName = currentBill.toAccountName
    val billType = currentBill.type
    val billAmount = currentBill.amount
    val billDueDay = currentBill.dueDay
    val billIsPaid = currentBill.isPaid
    val billMonth = currentBill.month
    val billYear = currentBill.year

    val currentCal = remember { Calendar.getInstance() }
    val currentDayOfMonth = currentCal.get(Calendar.DAY_OF_MONTH)
    val currentSysMonth = currentCal.get(Calendar.MONTH) + 1
    val currentSysYear = currentCal.get(Calendar.YEAR)

    val isOverdue = remember(billIsPaid, billDueDay, billMonth, billYear, currentDayOfMonth, currentSysMonth, currentSysYear) {
        if (billIsPaid || billDueDay == null) false
        else {
            val isPastMonth = (billYear < currentSysYear) || (billYear == currentSysYear && billMonth < currentSysMonth)
            val isCurrentMonth = (billYear == currentSysYear && billMonth == currentSysMonth)
            isPastMonth || (isCurrentMonth && currentDayOfMonth > billDueDay)
        }
    }

    val friendlySubcategory = remember(billSubcategory) {
        when (billSubcategory.trim()) {
            "WEALTH_ALLOCATION" -> "Fortress Sweep"
            "BILL_FUNDING" -> "Bill Funding"
            "REBALANCE" -> "Rebalance"
            "CASH_WITHDRAWAL" -> "Cash ATM"
            else -> billSubcategory.trim()
        }
    }

    val displayPrimaryTitle = remember(billTitle, friendlySubcategory) {
        val cleanTitle = billTitle.trim()
        val cleanSubcat = friendlySubcategory.trim()
        val isRedundant = cleanTitle.isBlank() ||
                cleanTitle.equals(cleanSubcat, ignoreCase = true) ||
                cleanTitle.startsWith("Vault Transfer", ignoreCase = true) ||
                (cleanSubcat.isNotBlank() && cleanSubcat.contains(cleanTitle, ignoreCase = true) && cleanSubcat.length - cleanTitle.length <= 4) ||
                (cleanTitle.isNotBlank() && cleanTitle.contains(cleanSubcat, ignoreCase = true) && cleanTitle.length - cleanSubcat.length <= 4)

        if (!isRedundant && cleanSubcat.isNotBlank()) {
            "$cleanSubcat ($cleanTitle)"
        } else {
            cleanSubcat.ifBlank { cleanTitle.ifBlank { "Commitment" } }
        }
    }

    val routeText = remember(billAccountName, billToAccountName, billType) {
        if (billType == TransactionType.TRANSFER && !billToAccountName.isNullOrBlank()) {
            "${billAccountName.uppercase()} ➔ ${billToAccountName.uppercase()}"
        } else {
            billAccountName.uppercase()
        }
    }

    val (typeTagColor, typeTagBg) = when (billType) {
        TransactionType.EXPENSE -> Error700 to Error100
        TransactionType.INCOME -> Success700 to Success100
        TransactionType.ASSET -> SoftTeal to SoftTealLight
        TransactionType.CORPORATE -> Warning700 to Warning100
        TransactionType.TRANSFER -> Primary700 to Primary50
    }

    val amountTint = when (billType) {
        TransactionType.EXPENSE -> Error500
        TransactionType.INCOME -> Success500
        TransactionType.ASSET -> SoftTeal
        TransactionType.CORPORATE -> Warning700
        TransactionType.TRANSFER -> Primary600
    }

    val typeTagText = when (billType) {
        TransactionType.EXPENSE -> "DUE"
        TransactionType.INCOME -> "INFLOW"
        TransactionType.ASSET -> "SIP"
        TransactionType.CORPORATE -> "CORP"
        TransactionType.TRANSFER -> "SWEEP"
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
                    SwipeToDismissBoxValue.StartToEnd -> Primary600
                    SwipeToDismissBoxValue.EndToStart -> Error500
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
                .shadow(if (billIsPaid) 0.5.dp else 1.5.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .border(
                    BorderStroke(
                        0.8.dp,
                        if (isOverdue) Error500.copy(alpha = 0.45f) else BorderLight
                    ),
                    RoundedCornerShape(18.dp)
                )
                .clickable {
                    if (!billIsPaid && currentOnSettle != null) {
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
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (billIsPaid) Success500 else CanvasLight)
                        .clickable {
                            if (!billIsPaid && currentOnSettle != null) {
                                showSettleDialog = true
                            } else {
                                currentOnTap(currentBill)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (billIsPaid) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Settled",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = BorderStroke(1.5.dp, Neutral300),
                            modifier = Modifier.size(24.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.5.dp)
                ) {
                    Text(
                        text = displayPrimaryTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (billIsPaid) TextDark.copy(alpha = 0.65f) else TextDark,
                        maxLines = 1,
                        modifier = Modifier.basicMarquee(
                            iterations = Int.MAX_VALUE,
                            delayMillis = 1200,
                            initialDelayMillis = 1200,
                            velocity = 30.dp
                        )
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = typeTagBg
                        ) {
                            Text(
                                text = typeTagText,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Black,
                                color = typeTagColor,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }

                        Text(
                            text = billCategory,
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                delayMillis = 1800,
                                initialDelayMillis = 1800,
                                velocity = 25.dp
                            )
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = routeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    delayMillis = 1800,
                                    initialDelayMillis = 1800,
                                    velocity = 25.dp
                                )
                        )

                        if (billDueDay != null || billIsPaid) {
                            Text(
                                text = "•",
                                fontSize = 10.sp,
                                color = TextMuted.copy(alpha = 0.6f)
                            )

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = when {
                                    billIsPaid -> Success100
                                    isOverdue -> Error100
                                    else -> CanvasLight
                                },
                                border = BorderStroke(
                                    0.6.dp,
                                    when {
                                        billIsPaid -> Success500.copy(alpha = 0.4f)
                                        isOverdue -> Error500.copy(alpha = 0.4f)
                                        else -> BorderLight
                                    }
                                )
                            ) {
                                val duePrefix = if (billType == TransactionType.INCOME) "Exp" else "Due"
                                Text(
                                    text = when {
                                        billIsPaid -> "Settled"
                                        isOverdue -> "Overdue ($duePrefix $billDueDay)"
                                        else -> "$duePrefix $billDueDay"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        billIsPaid -> Success700
                                        isOverdue -> Error700
                                        else -> TextDark
                                    },
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    val formattedAmount = if (billAmount % 1.0 == 0.0) {
                        String.format(Locale.US, "%,.0f", billAmount)
                    } else {
                        String.format(Locale.US, "%,.2f", billAmount)
                    }

                    Text(
                        text = "$currencySymbol$formattedAmount",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (billIsPaid) TextDark.copy(alpha = 0.5f) else amountTint
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            billIsPaid -> "Settled"
                            isOverdue -> "Overdue"
                            else -> "Pending"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            billIsPaid -> Success700
                            isOverdue -> Error700
                            else -> Warning700
                        }
                    )
                }
            }
        }
    }

    if (showSettleDialog) {
        val initialFormattedAmount = if (billAmount % 1.0 == 0.0) {
            billAmount.toLong().toString()
        } else {
            billAmount.toString()
        }
        var customAmountText by remember { mutableStateOf(initialFormattedAmount) }
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
            containerColor = CardWhite,
            title = {
                Text(
                    text = "Settle $displayPrimaryTitle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextDark
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (billType == TransactionType.TRANSFER) "Vault Route: $routeText" else "From Vault: $billAccountName",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = customAmountText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split('.')
                            customAmountText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                        },
                        label = { Text("Amount Paid ($currencySymbol)", fontSize = 12.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Primary600,
                            unfocusedBorderColor = BorderLight
                        )
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
                                selectedContainerColor = Primary50,
                                selectedLabelColor = Primary700
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
                                selectedContainerColor = Primary50,
                                selectedLabelColor = Primary700
                            )
                        )

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clickable { showCalendarPicker = true },
                            shape = RoundedCornerShape(8.dp),
                            color = if (!isToday && !isYesterday) Primary50 else CanvasLight,
                            border = BorderStroke(0.8.dp, if (!isToday && !isYesterday) Primary600 else BorderLight)
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
                                    color = if (!isToday && !isYesterday) Primary700 else TextDark
                                )
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Pick Date",
                                    tint = if (!isToday && !isYesterday) Primary600 else TextMuted,
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
                        val parsed = customAmountText.toDoubleOrNull() ?: billAmount
                        currentOnSettle?.invoke(currentBill, parsed, selectedDateMillis)
                        showSettleDialog = false
                    }
                ) {
                    val confirmLabel = if (billType == TransactionType.INCOME) "Confirm Received" else "Confirm Paid"
                    Text(confirmLabel, color = Primary600, fontWeight = FontWeight.Bold)
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
                        Text("Select", fontWeight = FontWeight.Bold, color = Primary600)
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
