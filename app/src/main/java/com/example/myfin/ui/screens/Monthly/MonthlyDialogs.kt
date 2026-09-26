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
import kotlin.math.abs
import kotlin.math.round

private val MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

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

    val descPrompt = when (bill.type) {
        TransactionType.INCOME -> "Credits ${bill.accountName} vault and logs inflow entry."
        TransactionType.ASSET -> "Deducts from ${bill.accountName} and records under Asset Wealth."
        TransactionType.TRANSFER -> "Sweeps funds from ${bill.accountName} ➤ ${bill.toAccountName ?: "Destination"}."
        TransactionType.CORPORATE -> if (bill.category.equals("Reimbursements & Claims", ignoreCase = true))
            "Credits ${bill.accountName} vault as employer claim reimbursement."
        else
            "Deducts from ${bill.accountName} and records corporate float outlay."
        TransactionType.EXPENSE -> "Deducts from ${bill.accountName} and records expense entry."
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Settle $displayBillName", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
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
                                text = "${currencySymbol}${String.format(Locale.US, "%,.2f", fundingAccount.currentBalance)}",
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
                        color = SoftRed.copy(alpha = 0.12f),
                        border = BorderStroke(0.6.dp, SoftRed.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "⚠️ MAB Risk: Settling this bill will reduce balance below ${currencySymbol}${fundingAccount?.minBalance?.toInt()} minimum balance threshold.",
                            fontSize = 10.5.sp,
                            color = SoftRed,
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
                    shape = RoundedCornerShape(12.dp)
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
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
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
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp)
                            .clickable { showSettleDatePicker = true },
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

                Spacer(modifier = Modifier.height(20.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(text = "Cancel", color = TextDark)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (amt > 0.0) {
                                onConfirm(amt, selectedSettleDateMillis)
                            } else {
                                Toast.makeText(context, "Please enter an amount > 0", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when (bill.type) {
                                TransactionType.INCOME -> SoftGreen
                                TransactionType.ASSET -> SoftTeal
                                TransactionType.CORPORATE -> Color(0xFFE57A28)
                                TransactionType.TRANSFER -> AccentPurple
                                else -> SoftGreen
                            }
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Confirm & Settle", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showSettleDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedSettleDateMillis
        )

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
                    Text("Select", fontWeight = FontWeight.Bold, color = AccentPurple)
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

@Composable
fun MonthYearPickerDialog(
    selectedYear: Int,
    selectedMonth: Int,
    onSelectYear: (Int) -> Unit,
    onSelectMonth: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = CardWhite, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(text = "Select Timeframe", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSelectYear(selectedYear - 1) }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Year")
                    }
                    Text(text = "$selectedYear", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    IconButton(onClick = { onSelectYear(selectedYear + 1) }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0 until 4) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (j in 0 until 3) {
                                val monthIdx = i * 3 + j + 1
                                val isSelected = selectedMonth == monthIdx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) AccentPurple else CanvasLight)
                                        .clickable { onSelectMonth(monthIdx) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = MONTH_NAMES[monthIdx - 1],
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) Color.White else TextDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafeToSpendInfoBottomSheet(
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Pure Safe-to-Spend (STS)",
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Lump-sum guilt-free headroom above living runway",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = AccentPurple.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Speed, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(19.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "What is Pure Safe-to-Spend?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unlike daily pacing allowances, Pure Safe-to-Spend ring-fences your upcoming AutoPay commitments and reserves your daily baseline living expenses through your next paycheck on the ${uiState.metrics.nextPaydayDay}th. The amount shown is 100% guilt-free to spend today as a lump sum without running out of money before salary arrives.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Live Pure Surplus Math", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val pendingBillsAmt = uiState.fixedBills.filter {
                        !it.isPaid &&
                        it.type != TransactionType.INCOME &&
                        !(it.type == TransactionType.CORPORATE && it.category.equals("Reimbursements & Claims", ignoreCase = true))
                    }.sumOf { it.amount }

                    val excessAdvance = uiState.reimbursementStatus.excessAdvanceHeld
                    val totalLiquidPool = uiState.metrics.liquidOperatingCash + pendingBillsAmt + excessAdvance

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Liquid Capital in Vaults (above MAB)", fontSize = 11.5.sp, color = TextDark)
                        Text("+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalLiquidPool)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoftGreen)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pending Fixed Commitments", fontSize = 11.5.sp, color = TextDark)
                        Text("-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", pendingBillsAmt)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoftRed)
                    }

                    if (excessAdvance > 0.0) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Ring-Fenced Company Advance", fontSize = 11.5.sp, color = TextDark)
                            Text("-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", excessAdvance)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE57A28))
                        }
                    }

                    val reservedRunway = (uiState.metrics.liquidOperatingCash - uiState.metrics.safeToSpend).coerceAtLeast(0.0)
                    val runwayLabel = if (uiState.metrics.isSalaryDelayed) {
                        "Living Runway (Salary expected, 1d reserved)"
                    } else {
                        "Living Runway (${uiState.metrics.daysUntilPayday}d until ${uiState.metrics.nextPaydayDay}th)"
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(runwayLabel, fontSize = 11.5.sp, color = TextDark)
                        Text("-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", reservedRunway)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoftAmber)
                    }

                    HorizontalDivider(color = BorderLight, thickness = 0.6.dp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Pure Guilt-Free Safe-to-Spend", fontWeight = FontWeight.Black, fontSize = 12.5.sp, color = TextDark)
                            Text("Immediate safe lump-sum headroom", fontSize = 10.sp, color = TextMuted)
                        }
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.safeToSpend)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (uiState.metrics.safeToSpend > 0) AccentPurple else SoftRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Security, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dynamic Payday Protection: The app tracks your salary transaction log day (${uiState.metrics.nextPaydayDay}th) and reserves a full daily runway buffer for the ${uiState.metrics.daysUntilPayday} days remaining. Even if you spend all of your STS today, your fixed bills and daily necessities remain 100% funded.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextDark)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
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
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "3-Pillar Target Execution",
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Planned budget limits vs. actual monthly execution",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = AccentPurple.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(19.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "The 3-Pillar Budget Framework",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MyFin organizes every rupee into three foundational pillars: Expenses (lifestyle spending capped to prevent burn), Income (earnings engines), and Assets / SIPs (paying yourself first into wealth before lifestyle spending).",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Live Target Performance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val plannedExp = uiState.metrics.plannedExpenses
                    val actualExp = uiState.metrics.actualExpenses
                    val expDiff = plannedExp - actualExp

                    val plannedInc = uiState.metrics.plannedIncome
                    val actualInc = uiState.metrics.actualIncome

                    val plannedAst = uiState.metrics.plannedAssets
                    val actualAst = uiState.metrics.actualAssets

                    // Pillar 1: Expenses
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("1. Lifestyle Expenses", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                            Text(
                                text = if (plannedExp > 0) {
                                    if (expDiff >= 0) "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expDiff)} headroom left"
                                    else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(expDiff))} over budget"
                                } else "No spending limit set",
                                fontSize = 10.sp,
                                color = if (expDiff < 0 && plannedExp > 0) SoftRed else TextMuted
                            )
                        }
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualExp)} / ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", plannedExp)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (expDiff < 0 && plannedExp > 0) SoftRed else TextDark
                        )
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)

                    // Pillar 2: Income
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("2. Inflow / Income", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                            Text(
                                text = if (actualInc >= plannedInc && plannedInc > 0) "Target achieved" else "Incoming progress",
                                fontSize = 10.sp,
                                color = SoftGreen
                            )
                        }
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualInc)} / ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", plannedInc)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SoftGreen
                        )
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)

                    // Pillar 3: Assets
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("3. Assets / SIP Wealth", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                            Text(
                                text = if (actualAst >= plannedAst && plannedAst > 0) "Target fully funded" else "Pending SIP transfers",
                                fontSize = 10.sp,
                                color = SoftTeal
                            )
                        }
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", actualAst)} / ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", plannedAst)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SoftTeal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Planner Alignment: Targets are configured in the Budget Planner. Any active recurring AutoPay commitment automatically sets a baseline minimum target for that category.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextDark)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
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
    val goalLabel = if (userProfile.fortressManualTarget > 0.0) {
        "Emergency Goal (Manual Target)"
    } else {
        "Emergency Goal (${userProfile.fortressEmergencyMonths} Mos Runway)"
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fortress Safety Net & Sweeps",
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Emergency reserve protection and surplus sweeps",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color(0xFF0D9488).copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(19.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "What is the Fortress Vault?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "The Fortress Vault is your strictly ring-fenced emergency fund. It is excluded from your daily liquid spending pool so it cannot be spent accidentally. It protects your living security with auto-sweep FDs to survive unexpected income interruptions.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text("Live Fortress Status", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Current Emergency FDs Reserve", fontSize = 11.5.sp, color = TextDark)
                        Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.fortressFdBalance)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF0D9488))
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(goalLabel, fontSize = 11.5.sp, color = TextDark)
                        Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.fortressTarget)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Goal Progress", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                            Text("Funded portion of emergency target", fontSize = 10.sp, color = TextMuted)
                        }
                        Text(
                            text = "${uiState.fortressProgressPercentage}% Funded",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            color = Color(0xFF0D9488)
                        )
                    }

                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Liquid Checking Sweep Floor", fontSize = 11.5.sp, color = TextDark)
                        Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", userProfile.fortressSweepThreshold)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Automated Month-End Sweeps: Between the 28th and the last day of the month, any unspent checking cash exceeding your living runway and sweep floor can be swept directly into Fortress with a single tap.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextDark)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun DeleteTransactionConfirmDialog(
    tx: TransactionEntity,
    currencySymbol: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cleanTitle = tx.title.trim()
    val cleanSubcat = tx.subcategory.trim()
    val displayTxName = when {
        cleanTitle.isBlank() || cleanTitle.equals(cleanSubcat, ignoreCase = true) -> cleanSubcat.ifBlank { "Transaction" }
        cleanTitle.startsWith(cleanSubcat, ignoreCase = true) -> {
            val unique = cleanTitle.removePrefix(cleanSubcat).trim(' ', '-', ':', '(', ')')
            if (unique.isNotBlank()) "$cleanSubcat ($unique)" else cleanSubcat
        }
        cleanSubcat.isBlank() -> cleanTitle
        else -> "$cleanSubcat ($cleanTitle)"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete Entry?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                text = if (tx.linkedFixedBillId != null)
                    "This entry is linked to an AutoPay bill. Deleting it will restore your vault balance and revert the parent commitment back to Unpaid."
                else "Are you sure you want to delete '$displayTxName' (${currencySymbol}${String.format(Locale.US, "%,.2f", tx.amount)})? This will permanently remove it from your vault ledger."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete", color = SoftRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextDark)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete AutoPay Commitment?", fontWeight = FontWeight.Bold) },
        text = {
            Text(text = "Deleting '$displayBillName' will remove this recurring template. Any linked payment already recorded in your ledger for this month will also be deleted and restored to your vault.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete Commitment", color = SoftRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextDark)
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Revert to Unsettled?", fontWeight = FontWeight.Bold) },
        text = {
            Text(text = "Reverting '$displayBillName' will delete the logged payment from your transaction ledger and restore the balance to ${bill.accountName}.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Revert Status", color = SoftRed, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextDark)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceFlowInfoBottomSheet(
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    initialMode: Int = 0,
    onDismiss: () -> Unit
) {
    var selectedMode by remember(initialMode) { mutableIntStateOf(initialMode.coerceIn(0, 3)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val incomeBase = uiState.metrics.personalIncome.takeIf { it > 0.0 } ?: uiState.metrics.actualIncome
            val lifestyleExp = uiState.metrics.lifestyleExpenses
            val preSipSaved = uiState.metrics.netSavedBeforeInvest
            val assetsInvested = uiState.metrics.actualAssets
            val postSipSurplus = preSipSaved - assetsInvested

            val startBal = uiState.metrics.startLiquidBalance
            val endBal = uiState.metrics.endLiquidBalance
            val expectedCash = startBal + postSipSurplus
            val bankCashMovement = endBal - startBal
            val capitalRelocation = endBal - expectedCash
            val retentionPct = if (incomeBase > 0) round((preSipSaved / incomeBase) * 100.0).toInt() else 0

            // Strict accountType check adhering to system architecture
            val isFortressAccount = { acc: AccountBalanceResult ->
                acc.accountType.equals("Fortress", ignoreCase = true)
            }
            val liquidAccounts = uiState.activeAccounts.filter { !isFortressAccount(it) }

            // 1. Dynamic Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (selectedMode) {
                            0 -> "Physical Bank Flow"
                            1 -> "Wealth Retention (Pre-SIP)"
                            2 -> "Unallocated Surplus"
                            else -> "Reserves & Float Relocation"
                        },
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                    Text(
                        text = when (selectedMode) {
                            0 -> "Rupee movement across physical liquid accounts"
                            1 -> "Salary preserved before wealth investments"
                            2 -> "Guilt-free cash remaining after SIPs"
                            else -> "Why bank balance differs from theoretical surplus"
                        },
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = when (selectedMode) {
                        0 -> SoftGreen.copy(alpha = 0.12f)
                        1 -> SoftTeal.copy(alpha = 0.12f)
                        2 -> AccentPurple.copy(alpha = 0.12f)
                        else -> Color(0xFFE57A28).copy(alpha = 0.12f)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = when (selectedMode) {
                                0 -> Icons.Default.AccountBalance
                                1 -> Icons.Default.TrendingUp
                                2 -> Icons.Default.Savings
                                else -> Icons.Default.SyncAlt
                            },
                            contentDescription = null,
                            tint = when (selectedMode) {
                                0 -> SoftGreen
                                1 -> SoftTeal
                                2 -> AccentPurple
                                else -> Color(0xFFE57A28)
                            },
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Mode Selector Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BorderLight.copy(alpha = 0.5f))
                    .padding(3.dp)
            ) {
                listOf("Bank Flow", "Retention", "Surplus", "Relocation").forEachIndexed { idx, label ->
                    val isSelected = selectedMode == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) CardWhite else Color.Transparent)
                            .clickable { selectedMode = idx }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) TextDark else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Dynamic Card Content per Mode
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    when (selectedMode) {
                        // MODE 0: BANK FLOW
                        0 -> {
                            Text("LIQUID CASH DELTA", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = TextMuted)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Opening Liquid Vault Balance", fontSize = 11.5.sp, color = TextMuted)
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", startBal)}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = TextDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Net Physical Cash Movement", fontSize = 11.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                Text("${if (bankCashMovement >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", bankCashMovement)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = if (bankCashMovement >= 0) SoftGreen else SoftRed)
                            }
                            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Current Active Balance", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                    Text("Total physical checking pool", fontSize = 10.sp, color = TextMuted)
                                }
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", endBal)}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = SoftGreen)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("VAULT COMPOSITION", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = TextMuted)
                            liquidAccounts.forEach { acc ->
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("• ${acc.accountName}", fontSize = 11.sp, color = TextMuted)
                                    Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", acc.currentBalance)}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                }
                            }
                        }

                        // MODE 1: WEALTH RETENTION (PRE-SIP)
                        1 -> {
                            Text("INCOME RETENTION MATH", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = TextMuted)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Personal Income Inflow", fontSize = 11.5.sp, color = TextDark)
                                Text("+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", incomeBase)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoftGreen)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Lifestyle Living Expenses", fontSize = 11.5.sp, color = TextDark)
                                Text("-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", lifestyleExp)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoftRed)
                            }
                            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Savings (Pre-SIP)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                    Text("$retentionPct% of salary retained", fontSize = 10.sp, color = TextMuted)
                                }
                                Text("${if (preSipSaved >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", preSipSaved)}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = SoftTeal)
                            }
                        }

                        // MODE 2: POST-SIP SURPLUS
                        2 -> {
                            Text("SURPLUS AFTER WEALTH INVESTING", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = TextMuted)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Savings (Pre-SIP)", fontSize = 11.5.sp, color = TextDark)
                                Text("+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", preSipSaved)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoftTeal)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Transferred to Assets & SIP", fontSize = 11.5.sp, color = TextDark)
                                Text("-${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", assetsInvested)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = SoftTeal)
                            }
                            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Net Monthly Surplus", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                    Text("Unallocated salary remaining", fontSize = 10.sp, color = TextMuted)
                                }
                                Text("${if (postSipSurplus >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", postSipSurplus)}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = AccentPurple)
                            }
                        }

                        // MODE 3: CAPITAL RELOCATIONS & GAP
                        3 -> {
                            Text("WHY BANK BALANCE DIFFERS FROM SURPLUS", fontSize = 9.5.sp, fontWeight = FontWeight.Black, color = TextMuted)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Opening Liquid Balance", fontSize = 11.5.sp, color = TextMuted)
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", startBal)}", fontSize = 11.5.sp, color = TextDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("+ Net Monthly Surplus", fontSize = 11.5.sp, color = TextDark)
                                Text("+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", postSipSurplus)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentPurple)
                            }
                            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Expected Bank Cash", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expectedCash)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Capital Relocation & Work Float", fontSize = 11.5.sp, color = TextDark)
                                    Text("Fortress sweeps, claims & loans", fontSize = 9.5.sp, color = TextMuted)
                                }
                                Text("${if (capitalRelocation >= 0) "+" else "-"}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(capitalRelocation))}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFE57A28))
                            }
                            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Actual Active Liquid Balance", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                                    Text("Real bank money present", fontSize = 10.sp, color = TextMuted)
                                }
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", endBal)}", fontWeight = FontWeight.Black, fontSize = 14.sp, color = TextDark)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextDark)
            ) {
                Text("Close", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}
