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

    // Extract local properties to avoid complex expression smart-cast issues
    val billId = currentBill.id
    val billTitle = currentBill.title
    val billSubcategory = currentBill.subcategory
    val billCategory = currentBill.category
    val billAccountName = currentBill.accountName
    val billToAccountName = currentBill.toAccountName
    val billType = currentBill.type
    val billAmount = currentBill.amount
    val billDueDay = currentBill.dueDay
    val billIsPaid = currentBill.isPaid

    // Dynamic Overdue Calculation
    val currentDayOfMonth = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH) }
    val isOverdue = remember(billIsPaid, billDueDay, currentDayOfMonth) {
        !billIsPaid && billDueDay != null && currentDayOfMonth > billDueDay
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
        val typeTagColor = when (billType) {
            TransactionType.EXPENSE -> SoftRed
            TransactionType.INCOME -> SoftGreen
            TransactionType.ASSET -> SoftTeal
            TransactionType.TRANSFER -> AccentPurple
        }

        val typeTagText = when (billType) {
            TransactionType.EXPENSE -> "DUE"
            TransactionType.INCOME -> "RECEIVABLE"
            TransactionType.ASSET -> "SIP"
            TransactionType.TRANSFER -> "SWEEP"
        }

        // Map enum string to friendly name
        val friendlySubcategory = remember(billSubcategory) {
            when (billSubcategory.trim()) {
                "WEALTH_ALLOCATION" -> "Fortress Sweep"
                "BILL_FUNDING" -> "Bill Funding"
                "REBALANCE" -> "Rebalance"
                else -> billSubcategory.trim()
            }
        }

        // Row 1 Title Formatting
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

        // Row 3 Route Text
        val routeText = remember(billAccountName, billToAccountName, billType) {
            if (billType == TransactionType.TRANSFER && !billToAccountName.isNullOrBlank()) {
                "${billAccountName.uppercase()} ➔ ${billToAccountName.uppercase()}"
            } else {
                billAccountName.uppercase()
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(if (billIsPaid) 0.5.dp else 1.5.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .border(
                    BorderStroke(
                        0.8.dp,
                        if (isOverdue) SoftRed.copy(alpha = 0.4f) else BorderLight.copy(alpha = 0.6f)
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
                // Left Status Checkmark Box - Uses SoftGreen to match the UI
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (billIsPaid) SoftGreen else CanvasLight)
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
                            border = BorderStroke(1.5.dp, BorderLight.copy(alpha = 0.85f)),
                            modifier = Modifier.size(24.dp)
                        ) {}
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Center Column: 3 Structured Rows with Marquee Scrolling
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.5.dp)
                ) {
                    // Row 1: Subcategory (Custom Title)
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

                    // Row 2: [TYPE TAG]  Category
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = typeTagColor.copy(alpha = 0.12f)
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

                    // Row 3: Vault Route • Date Indicator
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
                                    billIsPaid -> SoftGreen.copy(alpha = 0.10f)
                                    isOverdue -> SoftRed.copy(alpha = 0.10f)
                                    else -> CanvasLight
                                },
                                border = BorderStroke(
                                    0.6.dp,
                                    when {
                                        billIsPaid -> SoftGreen.copy(alpha = 0.35f)
                                        isOverdue -> SoftRed.copy(alpha = 0.4f)
                                        else -> BorderLight
                                    }
                                )
                            ) {
                                Text(
                                    text = when {
                                        billIsPaid -> "Settled"
                                        isOverdue -> "Overdue (Due $billDueDay)"
                                        else -> "Due $billDueDay"
                                    },
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        billIsPaid -> SoftGreen
                                        isOverdue -> SoftRed
                                        else -> TextDark
                                    },
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Column: Amount & Status Vertically Centered
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", billAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (billIsPaid) TextDark.copy(alpha = 0.5f) else typeTagColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (billIsPaid) "Settled" else "Pending",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (billIsPaid) SoftGreen else SoftAmber
                    )
                }
            }
        }
    }

    // Interactive Settle Dialog
    if (showSettleDialog) {
        var customAmountText by remember { mutableStateOf(billAmount.toString()) }
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
                    text = "Settle $billTitle",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = TextDark
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "From Vault: $billAccountName",
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
                        val parsed = customAmountText.toDoubleOrNull() ?: billAmount
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
