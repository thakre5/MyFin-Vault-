package com.example.myfin.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransferSubtype
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountTransferDialog(
    accounts: List<String>,
    currencySymbol: String = "₹",
    onDismiss: () -> Unit,
    onTransfer: (
        from: String,
        to: String,
        amount: Double,
        note: String,
        subtype: TransferSubtype,
        date: Long,
        isRecurring: Boolean,
        dueDay: Int?
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var fromAccount by remember(accounts) { mutableStateOf(accounts.firstOrNull().orEmpty()) }
    var toAccount by remember(accounts) { mutableStateOf(accounts.getOrNull(1) ?: accounts.firstOrNull().orEmpty()) }
    var amountText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var selectedSubtype by remember { mutableStateOf(TransferSubtype.WEALTH_ALLOCATION) }

    // Recurring Monthly Sweep Toggle States
    var isRecurringSweep by remember { mutableStateOf(false) }
    var dueDayText by remember { mutableStateOf("") }

    // Date State & Dialog
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

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

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isTransferValid = parsedAmount > 0.0 && fromAccount.isNotBlank() && toAccount.isNotBlank() && fromAccount != toAccount

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(40.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = BorderLight
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Internal Vault Transfer",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = TextDark
                    )
                    Text(
                        text = "Reallocate cashflow with zero-leakage tracking",
                        fontSize = 11.5.sp,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(CanvasLight)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source Account Selector
            Text("Source Vault (From)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(accounts) { acc ->
                    val isSel = fromAccount == acc
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { fromAccount = acc },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) AccentPurple.copy(alpha = 0.14f) else CanvasLight,
                        border = BorderStroke(0.6.dp, if (isSel) AccentPurple else BorderLight)
                    ) {
                        Text(
                            text = acc,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) AccentPurple else TextDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Destination Account Selector
            Text("Destination Vault (To)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(accounts) { acc ->
                    val isSel = toAccount == acc
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { toAccount = acc },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) AccentPurple.copy(alpha = 0.14f) else CanvasLight,
                        border = BorderStroke(0.6.dp, if (isSel) AccentPurple else BorderLight)
                    ) {
                        Text(
                            text = acc,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) AccentPurple else TextDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Strategic Subtype Classification
            Text("Transfer Classification Subtype", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    TransferSubtype.BILL_FUNDING to "Bill Funding",
                    TransferSubtype.WEALTH_ALLOCATION to "Fortress Sweep",
                    TransferSubtype.REBALANCE to "Rebalance"
                ).forEach { (subtype, label) ->
                    val isSel = selectedSubtype == subtype
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedSubtype = subtype },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSel) AccentPurple.copy(alpha = 0.14f) else CanvasLight,
                        border = BorderStroke(0.6.dp, if (isSel) AccentPurple else BorderLight)
                    ) {
                        Text(
                            text = label,
                            fontSize = 10.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) AccentPurple else TextDark,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 7.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction Date Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Transfer Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isToday,
                        onClick = {
                            selectedDateMillis = System.currentTimeMillis()
                            if (isRecurringSweep && dueDayText.isBlank()) {
                                dueDayText = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
                            }
                        },
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
                            selectedDateMillis = cal.timeInMillis
                            if (isRecurringSweep && dueDayText.isBlank()) {
                                dueDayText = cal.get(Calendar.DAY_OF_MONTH).toString()
                            }
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
                            .height(34.dp)
                            .clickable { showDatePickerDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isToday && !isYesterday) AccentPurpleLight else CanvasLight,
                        border = BorderStroke(0.8.dp, if (!isToday && !isYesterday) AccentPurple else BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (!isToday && !isYesterday) formattedDateLabel else "Other Date",
                                fontSize = 11.5.sp,
                                fontWeight = if (!isToday && !isYesterday) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isToday && !isYesterday) AccentPurple else TextDark
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pick Date",
                                tint = if (!isToday && !isYesterday) AccentPurple else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Amount Input
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    val parts = filtered.split('.')
                    amountText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                },
                label = { Text("Transfer Amount ($currencySymbol)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Note Input
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Purpose Note (Optional)", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )

            // REPEAT AS MONTHLY SWEEP TOGGLE
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CanvasLight,
                border = BorderStroke(0.6.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Set as Recurring Monthly Sweep",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = "Saves this route ($fromAccount ➔ $toAccount) as an AutoPay commitment",
                                fontSize = 10.5.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = isRecurringSweep,
                            onCheckedChange = { checked ->
                                isRecurringSweep = checked
                                if (checked && dueDayText.isBlank()) {
                                    val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                                    dueDayText = cal.get(Calendar.DAY_OF_MONTH).toString()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentPurple,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BorderLight
                            )
                        )
                    }

                    if (isRecurringSweep) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Monthly Sweep Day (1-31):",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark
                            )
                            OutlinedTextField(
                                value = dueDayText,
                                onValueChange = { input ->
                                    if (input.length <= 2) {
                                        dueDayText = input.filter { it.isDigit() }
                                    }
                                },
                                placeholder = { Text("Day", fontSize = 11.sp) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(75.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentPurple,
                                    unfocusedBorderColor = BorderLight
                                )
                            )
                        }
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
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                }

                Button(
                    onClick = {
                        if (isTransferValid) {
                            val parsedDueDay = if (isRecurringSweep) {
                                dueDayText.toIntOrNull()?.let { if (it in 1..31) it else null }
                                    ?: Calendar.getInstance().apply { timeInMillis = selectedDateMillis }.get(Calendar.DAY_OF_MONTH)
                            } else null

                            onTransfer(
                                fromAccount,
                                toAccount,
                                parsedAmount,
                                noteText.trim(),
                                selectedSubtype,
                                selectedDateMillis,
                                isRecurringSweep,
                                parsedDueDay
                            )
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Select distinct vaults and enter an amount > 0", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = isTransferValid,
                    modifier = Modifier.weight(1.3f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text(
                        text = if (isRecurringSweep) "Transfer & Save AutoPay" else "Execute Transfer",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    if (showDatePickerDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
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
                            if (isRecurringSweep && dueDayText.isBlank()) {
                                dueDayText = localCal.get(Calendar.DAY_OF_MONTH).toString()
                            }
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold, color = AccentPurple)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel", color = TextDark)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
