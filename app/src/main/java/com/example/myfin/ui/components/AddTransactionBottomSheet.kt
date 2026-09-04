package com.example.myfin.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.CategoryEntity
import com.example.myfin.data.SubcategoryEntity
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.TransferSubtype
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    editingTransaction: TransactionEntity? = null,
    currencySymbol: String,
    accountList: List<String>,
    masterCategories: List<CategoryEntity>,
    masterSubcategories: List<SubcategoryEntity>,
    onDismiss: () -> Unit,
    onSave: (id: Long, title: String, amount: Double, category: String, subcategory: String, account: String, type: TransactionType, date: Long) -> Unit
) {
    val isEditing = editingTransaction != null
    var selectedType by remember { mutableStateOf(editingTransaction?.type ?: TransactionType.EXPENSE) }
    var title by remember { mutableStateOf(editingTransaction?.title.orEmpty()) }
    var amountText by remember { mutableStateOf(editingTransaction?.amount?.let { if (it > 0) it.toString() else "" }.orEmpty()) }

    // Transfer Subtype State
    var selectedTransferSubtype by remember {
        mutableStateOf(
            when (editingTransaction?.subcategory) {
                TransferSubtype.WEALTH_ALLOCATION.name, "Fortress Sweep" -> TransferSubtype.WEALTH_ALLOCATION
                TransferSubtype.REBALANCE.name, "Rebalance" -> TransferSubtype.REBALANCE
                else -> TransferSubtype.BILL_FUNDING
            }
        )
    }

    // Destination Vault for Transfers
    var selectedToAccount by remember(accountList) {
        mutableStateOf(
            editingTransaction?.toAccountName ?: accountList.getOrNull(1) ?: accountList.firstOrNull().orEmpty()
        )
    }

    // Date State & Dialog
    var selectedDateMillis by remember { mutableStateOf(editingTransaction?.date ?: System.currentTimeMillis()) }
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

    var selectedAccount by remember(accountList) {
        mutableStateOf(editingTransaction?.accountName ?: accountList.firstOrNull().orEmpty())
    }

    val availableCategories = remember(masterCategories, selectedType) {
        masterCategories.filter { it.type == selectedType }.map { it.name }
    }
    var selectedCategory by remember(availableCategories) {
        mutableStateOf(editingTransaction?.category ?: availableCategories.firstOrNull().orEmpty())
    }

    val availableSubcategories = remember(masterSubcategories, selectedCategory) {
        masterSubcategories.filter { it.parentCategory == selectedCategory }.map { it.name }
    }
    var selectedSubcategory by remember(availableSubcategories) {
        mutableStateOf(editingTransaction?.subcategory ?: availableSubcategories.firstOrNull().orEmpty())
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isInputValid = parsedAmount > 0.0 && selectedAccount.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Transaction" else "Add New Entry",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TextDark
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Transaction Type Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BorderLight.copy(alpha = 0.5f))
                    .padding(3.dp)
            ) {
                listOf(
                    TransactionType.EXPENSE to "Expense",
                    TransactionType.INCOME to "Income",
                    TransactionType.ASSET to "Asset / SIP",
                    TransactionType.TRANSFER to "Transfer"
                ).forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) CardWhite else Color.Transparent)
                            .clickable {
                                selectedType = type
                                if (type != TransactionType.TRANSFER) {
                                    val cats = masterCategories.filter { it.type == type }.map { it.name }
                                    selectedCategory = cats.firstOrNull().orEmpty()
                                    val subs = masterSubcategories.filter { it.parentCategory == selectedCategory }.map { it.name }
                                    selectedSubcategory = subs.firstOrNull().orEmpty()
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp,
                            color = if (isSelected) {
                                when (type) {
                                    TransactionType.EXPENSE -> SoftRed
                                    TransactionType.INCOME -> SoftGreen
                                    TransactionType.ASSET -> SoftTeal
                                    TransactionType.TRANSFER -> AccentPurple
                                }
                            } else TextMuted
                        )
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
                label = { Text("Amount ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (selectedType == TransactionType.TRANSFER) "Transfer Note (Optional)" else "Title / Merchant (Optional)") },
                placeholder = {
                    Text(
                        if (selectedType == TransactionType.TRANSFER) "e.g., Emergency Reserve"
                        else selectedSubcategory.ifBlank { "e.g., Grocery Store" }
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction Date Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Transaction Date", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = isToday,
                        onClick = { selectedDateMillis = System.currentTimeMillis() },
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

            // Transfer Subtype Selector OR Standard Category/Subcategory Chips
            if (selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Transfer Classification Subtype",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TransferSubtype.BILL_FUNDING to "Bill Funding",
                        TransferSubtype.WEALTH_ALLOCATION to "Fortress Sweep",
                        TransferSubtype.REBALANCE to "Rebalance"
                    ).forEach { (subtype, label) ->
                        val isSelected = selectedTransferSubtype == subtype
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTransferSubtype = subtype },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) AccentPurple.copy(alpha = 0.12f) else CanvasLight,
                            border = BorderStroke(
                                0.8.dp,
                                if (isSelected) AccentPurple else BorderLight
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) AccentPurple else TextDark
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))

                // Category Chips
                Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableCategories.ifEmpty { listOf("General") }) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = cat
                                val subs = masterSubcategories.filter { it.parentCategory == cat }.map { it.name }
                                selectedSubcategory = subs.firstOrNull().orEmpty()
                            },
                            label = { Text(cat, fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AccentPurpleLight,
                                selectedLabelColor = AccentPurple
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Subcategory Chips
                if (availableSubcategories.isNotEmpty()) {
                    Text("Subcategory", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableSubcategories) { sub ->
                            val isSelected = selectedSubcategory == sub
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSubcategory = sub },
                                label = { Text(sub, fontSize = 11.5.sp) },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentPurpleLight,
                                    selectedLabelColor = AccentPurple
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // Source Vault Account Chips
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (selectedType == TransactionType.TRANSFER) "Source Vault (From)" else "Vault Account",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(accountList) { acc ->
                    val isSelected = selectedAccount == acc
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedAccount = acc },
                        label = { Text(acc, fontSize = 11.5.sp) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
            }

            // Destination Vault Account Chips for Transfer
            if (selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Destination Vault (To)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accountList.filter { it != selectedAccount }) { acc ->
                        val isSelected = selectedToAccount == acc
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedToAccount = acc },
                            label = { Text(acc, fontSize = 11.5.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftTeal.copy(alpha = 0.15f),
                                selectedLabelColor = SoftTeal
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    if (isInputValid) {
                        val resolvedCategory = if (selectedType == TransactionType.TRANSFER) "Transfer" else selectedCategory.ifBlank { "General" }
                        val resolvedSubcategory = if (selectedType == TransactionType.TRANSFER) selectedTransferSubtype.name else selectedSubcategory.ifBlank { "General" }
                        val resolvedTitle = when {
                            title.isNotBlank() -> title.trim()
                            selectedType == TransactionType.TRANSFER -> "Vault Transfer ($selectedAccount ➔ $selectedToAccount)"
                            else -> resolvedSubcategory
                        }

                        onSave(
                            editingTransaction?.id ?: 0L,
                            resolvedTitle,
                            parsedAmount,
                            resolvedCategory,
                            resolvedSubcategory,
                            selectedAccount.ifBlank { accountList.firstOrNull().orEmpty() },
                            selectedType,
                            selectedDateMillis
                        )
                        onDismiss()
                    }
                },
                enabled = isInputValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text(
                    text = if (isEditing) "Update Entry" else "Save Entry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
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
