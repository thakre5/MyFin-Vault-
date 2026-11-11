package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myfin.data.AccountBalanceResult
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.UserProfile
import com.example.myfin.ui.MonthlyUiState
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

// ============================================================================
// 1. SETTLE FIXED BILL DIALOG
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleFixedBillDialog(
    bill: FixedBillEntity,
    activeAccounts: List<AccountBalanceResult>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onConfirm: (Double, Long) -> Unit
) {
    val context = LocalContext.current
    val friendlySubcat = if (bill.type == TransactionType.TRANSFER) {
        when (bill.subcategory.trim()) {
            "WEALTH_ALLOCATION" -> "Fortress Sweep"
            "BILL_FUNDING" -> "Bill Funding"
            "REBALANCE" -> "Rebalance"
            else -> bill.subcategory.trim().ifBlank { "Vault Sweep" }
        }
    } else bill.subcategory.trim()

    val cleanTitle = bill.title.trim()
    val displayBillName = when {
        cleanTitle.isBlank() || cleanTitle.equals(friendlySubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> {
            friendlySubcat.ifBlank { cleanTitle.ifBlank { "Commitment" } }
        }
        cleanTitle.startsWith(friendlySubcat, ignoreCase = true) -> {
            val unique = cleanTitle.removePrefix(friendlySubcat).trim(' ', '-', ':', '(', ')')
            if (unique.isNotBlank()) "$friendlySubcat ($unique)" else friendlySubcat
        }
        friendlySubcat.isBlank() -> cleanTitle
        else -> "$friendlySubcat ($cleanTitle)"
    }

    val formattedDefaultAmount = if (bill.amount % 1.0 == 0.0) bill.amount.toLong().toString() else bill.amount.toString()
    var finalAmountText by remember(bill.id) { mutableStateOf(formattedDefaultAmount) }
    var selectedSettleDateMillis by remember(bill.id) { mutableStateOf(System.currentTimeMillis()) }
    var showSettleDatePicker by remember { mutableStateOf(false) }

    val isToday = remember(selectedSettleDateMillis) {
        val calSelected = Calendar.getInstance().apply { timeInMillis = selectedSettleDateMillis }
        val calNow = Calendar.getInstance()
        calSelected.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calSelected.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
    }

    val isYesterday = remember(selectedSettleDateMillis) {
        val calSelected = Calendar.getInstance().apply { timeInMillis = selectedSettleDateMillis }
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        calSelected.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
                calSelected.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)
    }

    val formattedDateLabel = remember(selectedSettleDateMillis, isToday, isYesterday) {
        when {
            isToday -> "Today"
            isYesterday -> "Yesterday"
            else -> SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(selectedSettleDateMillis))
        }
    }

    val fundingAccount = remember(bill.accountName, activeAccounts) {
        activeAccounts.find { it.accountName.equals(bill.accountName, ignoreCase = true) }
    }
    val amt = finalAmountText.toDoubleOrNull() ?: bill.amount
    val willBreachMab = bill.type != TransactionType.INCOME &&
            !bill.category.equals("Reimbursements & Claims", ignoreCase = true) &&
            fundingAccount != null && fundingAccount.minBalance > 0.0 &&
            (fundingAccount.currentBalance - amt) < fundingAccount.minBalance

    val destVaultName = bill.toAccountName ?: "Destination"
    val descPrompt = when (bill.type) {
        TransactionType.INCOME -> "Credits ${bill.accountName} vault and logs inflow entry."
        TransactionType.ASSET -> "Deducts from ${bill.accountName} and records under Asset Wealth."
        TransactionType.TRANSFER -> "Sweeps funds from ${bill.accountName} ➤ $destVaultName."
        TransactionType.CORPORATE -> if (bill.category.equals("Reimbursements & Claims", ignoreCase = true))
            "Credits ${bill.accountName} vault as employer claim reimbursement."
        else
            "Deducts from ${bill.accountName} and records corporate float outlay."
        TransactionType.EXPENSE -> "Deducts from ${bill.accountName} and records expense entry."
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Settle $displayBillName",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = descPrompt, fontSize = 12.sp, color = TextMuted)

                if (fundingAccount != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Available in ${fundingAccount.accountName}:", fontSize = 11.sp, color = TextMuted)
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.2f", fundingAccount.currentBalance)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextDark
                            )
                        }
                    }
                }

                if (willBreachMab) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Error100,
                        border = BorderStroke(0.6.dp, Error500.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "⚠️ MAB Risk: Settling this bill will reduce balance below $currencySymbol${fundingAccount?.minBalance?.toInt()} minimum balance threshold.",
                            fontSize = 10.5.sp,
                            color = Error700,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = finalAmountText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        val parts = filtered.split('.')
                        finalAmountText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                    },
                    label = { Text(text = "Actual Amount ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Primary600,
                        unfocusedBorderColor = BorderLight
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Payment Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isToday,
                        onClick = { selectedSettleDateMillis = System.currentTimeMillis() },
                        label = { Text("Today", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary50,
                            selectedLabelColor = Primary600
                        )
                    )

                    FilterChip(
                        selected = isYesterday,
                        onClick = {
                            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                            selectedSettleDateMillis = cal.timeInMillis
                        },
                        label = { Text("Yesterday", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary50,
                            selectedLabelColor = Primary600
                        )
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clickable { showSettleDatePicker = true },
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
                                color = if (!isToday && !isYesterday) Primary600 else TextDark
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

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                    ) {
                        Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            val parsedAmt = finalAmountText.toDoubleOrNull() ?: 0.0
                            if (parsedAmt <= 0.0) {
                                Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            onConfirm(parsedAmt, selectedSettleDateMillis)
                        },
                        modifier = Modifier
                            .weight(1.2f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                    ) {
                        Text("Confirm & Settle", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }

        if (showSettleDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedSettleDateMillis)
            DatePickerDialog(
                onDismissRequest = { showSettleDatePicker = false },
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
                                selectedSettleDateMillis = localCal.timeInMillis
                            }
                            showSettleDatePicker = false
                        }
                    ) {
                        Text("Select", fontWeight = FontWeight.Bold, color = Primary600)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSettleDatePicker = false }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }
}

// ============================================================================
// 2. DELETE & REVERT CONFIRMATION DIALOGS
// ============================================================================
@Composable
fun DeleteTransactionConfirmDialog(
    tx: TransactionEntity,
    currencySymbol: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Delete Transaction?", fontWeight = FontWeight.Black, fontSize = 17.sp, color = TextDark)
        },
        text = {
            Text(
                text = "Remove '${tx.title}' ($currencySymbol${String.format(Locale.US, "%,.2f", tx.amount)}) from ${tx.accountName}? If linked to an AutoPay commitment, it will revert to Unpaid.",
                fontSize = 13.sp,
                color = TextMuted
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Error500),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
    )
}

@Composable
fun DeleteFixedBillConfirmDialog(
    bill: FixedBillEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Remove Commitment?", fontWeight = FontWeight.Black, fontSize = 17.sp, color = TextDark)
        },
        text = {
            Text(
                text = "Delete '${bill.title.ifBlank { bill.subcategory }}'? Any linked ledger transaction for this month will also be removed.",
                fontSize = 13.sp,
                color = TextMuted
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Error500),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Delete", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
    )
}

@Composable
fun RevertFixedBillConfirmDialog(
    bill: FixedBillEntity,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text("Revert to Unpaid?", fontWeight = FontWeight.Black, fontSize = 17.sp, color = TextDark)
        },
        text = {
            Text(
                text = "Mark '${bill.title.ifBlank { bill.subcategory }}' as unpaid and remove its settled transaction from your monthly ledger?",
                fontSize = 13.sp,
                color = TextMuted
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Warning500),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Revert to Unpaid", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
    )
}

// ============================================================================
// 3. MONTH & YEAR PICKER DIALOG
// ============================================================================
@Composable
fun MonthYearPickerDialog(
    selectedYear: Int,
    selectedMonth: Int,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardWhite,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onSelectYear(selectedYear - 1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year", tint = TextDark)
                    }
                    Text(
                        text = selectedYear.toString(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Primary600
                    )
                    IconButton(onClick = { onSelectYear(selectedYear + 1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Year", tint = TextDark)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                for (row in 0 until 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (col in 0 until 3) {
                            val monthNum = row * 3 + col + 1
                            val isSel = monthNum == selectedMonth
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectMonth(monthNum) },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) Primary600 else CanvasLight,
                                border = BorderStroke(0.6.dp, if (isSel) Primary600 else BorderLight)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = MONTH_NAMES[monthNum - 1],
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.5.sp,
                                        color = if (isSel) Color.White else TextDark
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close", fontWeight = FontWeight.Bold, color = TextMuted)
                }
            }
        }
    }
}

// ============================================================================
// 4. TRANSACTION DETAIL BOTTOM SHEET
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailBottomSheet(
    transaction: TransactionEntity,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onEdit: (TransactionEntity) -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val dateFormatted = remember(transaction.date) {
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(transaction.date))
    }

    val typeColor = when (transaction.type) {
        TransactionType.INCOME -> Success500
        TransactionType.EXPENSE -> Error500
        TransactionType.ASSET -> SoftTeal
        TransactionType.TRANSFER -> Primary600
        TransactionType.CORPORATE -> Warning500
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = typeColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = transaction.type.name,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Black,
                        color = typeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(text = dateFormatted, fontSize = 11.5.sp, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = transaction.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$currencySymbol${String.format(Locale.US, "%,.2f", transaction.amount)}",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = typeColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailItemRow("Category", transaction.category)
                    DetailItemRow("Subcategory", transaction.subcategory)
                    if (transaction.type == TransactionType.TRANSFER && !transaction.toAccountName.isNullOrBlank()) {
                        DetailItemRow("Route", "${transaction.accountName} ➔ ${transaction.toAccountName}")
                    } else {
                        DetailItemRow("Vault Account", transaction.accountName)
                    }
                    if (transaction.linkedFixedBillId != null) {
                        DetailItemRow("Linked AutoPay", "Yes (Recurring Commitment)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { onDelete(transaction) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Error500.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Error500)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onEdit(transaction) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(17.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit Entry", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = TextMuted)
        Text(text = value, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}

// ============================================================================
// 5. INFO & ANALYTICS EXPLAINER BOTTOM SHEETS
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeToSpendInfoBottomSheet(
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    onDismiss: () -> Unit
) {
    val curr = userProfile.currencySymbol
    val m = uiState.metrics

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text("Safe-to-Spend Engine", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Real-time discretionary liquidity after protecting MAB floors, pending AutoPay bills, and daily runway until payday.",
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailItemRow("Safe-to-Spend Now", "$curr${String.format(Locale.US, "%,.0f", m.safeToSpend)}")
                    DetailItemRow("Liquid Operating Cash", "$curr${String.format(Locale.US, "%,.0f", m.liquidOperatingCash)}")
                    DetailItemRow("Days Until Next Payday", "${m.daysUntilPayday} Days")
                    DetailItemRow("Theoretical Budget Cap", "$curr${String.format(Locale.US, "%,.0f", m.theoreticalSafeToSpend)}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary600)
            ) {
                Text("Got It", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreePillarInfoBottomSheet(
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    onDismiss: () -> Unit
) {
    val curr = userProfile.currencySymbol
    val m = uiState.metrics

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text("3-Pillar Cash Flow Breakdown", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your monthly capital partitioned across Personal Inflow, Lifestyle Burn, and Wealth Allocation.",
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailItemRow("Actual Inflow", "$curr${String.format(Locale.US, "%,.0f", m.actualIncome)}")
                    DetailItemRow("Lifestyle Expenses", "$curr${String.format(Locale.US, "%,.0f", m.actualExpenses)}")
                    DetailItemRow("Wealth & SIP Assets", "$curr${String.format(Locale.US, "%,.0f", m.actualAssets)}")
                    DetailItemRow("Net Saved After Invest", "$curr${String.format(Locale.US, "%,.0f", m.netSavedAfterInvest)}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary600)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FortressInfoBottomSheet(
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    onDismiss: () -> Unit
) {
    val curr = userProfile.currencySymbol

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text("Emergency Fortress Reserve", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tracks your ring-fenced emergency corpus against your ${userProfile.fortressEmergencyMonths}-month runway target.",
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailItemRow("Fortress Reserve Balance", "$curr${String.format(Locale.US, "%,.0f", uiState.fortressFdBalance)}")
                    DetailItemRow("Target Runway Corpus", "$curr${String.format(Locale.US, "%,.0f", uiState.fortressTarget)}")
                    DetailItemRow("Completion Progress", "${uiState.fortressProgressPercentage}%")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary600)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceFlowInfoBottomSheet(
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    initialMode: Int = 0,
    onDismiss: () -> Unit
) {
    val curr = userProfile.currencySymbol
    val m = uiState.metrics

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            Text(
                text = if (initialMode == 0) "Opening vs Closing Liquidity" else "Total Vault Net Worth",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Reconciles your month-start liquid balance with current active bank and cash vaults.",
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailItemRow("Opening Liquid Balance", "$curr${String.format(Locale.US, "%,.0f", m.startLiquidBalance)}")
                    DetailItemRow("Current Liquid Balance", "$curr${String.format(Locale.US, "%,.0f", m.endLiquidBalance)}")
                    DetailItemRow("Total All-Vault Balance", "$curr${String.format(Locale.US, "%,.0f", m.totalVaultBalance)}")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary600)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
