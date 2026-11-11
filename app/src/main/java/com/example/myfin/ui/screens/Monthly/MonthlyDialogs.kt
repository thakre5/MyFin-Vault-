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
                        color = Error100,
                        border = BorderStroke(0.6.dp, Error500.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "⚠️ MAB Risk: Settling this bill will reduce balance below ${currencySymbol}${fundingAccount?.minBalance?.toInt()} minimum balance threshold.",
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

                Spacer(modifier = Modifier.height
