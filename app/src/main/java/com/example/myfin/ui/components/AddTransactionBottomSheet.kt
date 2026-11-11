package com.example.myfin.ui.components

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.text.style.TextOverflow
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
    onSave: (
        id: Long,
        title: String,
        amount: Double,
        category: String,
        subcategory: String,
        account: String,
        toAccount: String?,
        type: TransactionType,
        date: Long,
        isRecurring: Boolean,
        dueDay: Int?
    ) -> Unit
) {
    val context = LocalContext.current
    val isEditing = editingTransaction != null
    val txKey = editingTransaction?.id ?: -1L

    var selectedType by remember(txKey) {
        mutableStateOf(editingTransaction?.type ?: TransactionType.EXPENSE)
    }

    var title by remember(txKey) {
        mutableStateOf(
            editingTransaction?.let { tx ->
                val cleanTitle = tx.title.trim()
                val cleanSubcat = tx.subcategory.trim()
                when {
                    cleanTitle.isBlank() ||
                            cleanTitle.equals(cleanSubcat, ignoreCase = true) ||
                            cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> ""
                    cleanTitle.startsWith(cleanSubcat, ignoreCase = true) -> {
                        cleanTitle.removePrefix(cleanSubcat).trim(' ', '-', ':', '(', ')')
                    }
                    else -> cleanTitle
                }
            }.orEmpty()
        )
    }

    var amountText by remember(txKey) {
        mutableStateOf(
            editingTransaction?.amount?.let {
                if (it > 0) {
                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                } else ""
            }.orEmpty()
        )
    }

    var isRecurringCommitment by remember(txKey) { mutableStateOf(false) }
    var dueDayText by remember(txKey) { mutableStateOf("") }

    var selectedTransferSubtype by remember(txKey) {
        mutableStateOf(
            editingTransaction?.transferSubtype?.takeIf { it != TransferSubtype.NONE }
                ?: when (editingTransaction?.subcategory?.trim()) {
                    TransferSubtype.WEALTH_ALLOCATION.name, "Fortress Sweep" -> TransferSubtype.WEALTH_ALLOCATION
                    TransferSubtype.CASH_WITHDRAWAL.name, "Cash ATM Withdrawal" -> TransferSubtype.CASH_WITHDRAWAL
                    TransferSubtype.REBALANCE.name, "Rebalance", "Vault Rebalance" -> TransferSubtype.REBALANCE
                    else -> TransferSubtype.BILL_FUNDING
                }
        )
    }

    var selectedAccount by remember(txKey, accountList) {
        mutableStateOf(editingTransaction?.accountName ?: accountList.firstOrNull().orEmpty())
    }

    var selectedToAccount by remember(txKey, accountList, selectedAccount) {
        mutableStateOf(
            editingTransaction?.toAccountName
                ?: accountList.firstOrNull { !it.equals(selectedAccount, ignoreCase = true) }
                ?: accountList.firstOrNull().orEmpty()
        )
    }

    var selectedDateMillis by remember(txKey) {
        mutableStateOf(editingTransaction?.date ?: System.currentTimeMillis())
    }
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

    // Historical Preservation: Keep historical category even if retired outside grace period
    val availableCategoryEntities = remember(masterCategories, selectedType, editingTransaction) {
        val cats = masterCategories.filter { it.type == selectedType }.toMutableList()
        if (editingTransaction != null && editingTransaction.type == selectedType && editingTransaction.category.isNotBlank()) {
            if (cats.none { it.name.equals(editingTransaction.category, ignoreCase = true) }) {
                cats.add(0, CategoryEntity(name = editingTransaction.category, type = selectedType, isLegacy = true))
            }
        }
        cats
    }

    var selectedCategory by remember(txKey, selectedType) {
        val initialMatch = if (editingTransaction != null && editingTransaction.type == selectedType && editingTransaction.category.isNotBlank()) {
            availableCategoryEntities.firstOrNull { it.name.equals(editingTransaction.category, ignoreCase = true) }?.name
                ?: editingTransaction.category
        } else null
        mutableStateOf(initialMatch ?: availableCategoryEntities.firstOrNull()?.name ?: "General")
    }

    LaunchedEffect(selectedType, availableCategoryEntities) {
        if (selectedType != TransactionType.TRANSFER && availableCategoryEntities.isNotEmpty()) {
            val matchesExisting = availableCategoryEntities.any {
                it.name.equals(selectedCategory, ignoreCase = true)
            }
            if (!matchesExisting) {
                selectedCategory = availableCategoryEntities.first().name
            }
        }
    }

    val availableSubcategoryEntities = remember(masterSubcategories, selectedCategory, selectedType, editingTransaction) {
        val subs = masterSubcategories.filter {
            it.parentCategory.equals(selectedCategory, ignoreCase = true) && it.type == selectedType
        }.toMutableList()

        if (editingTransaction != null && editingTransaction.type == selectedType &&
            editingTransaction.category.equals(selectedCategory, ignoreCase = true) && editingTransaction.subcategory.isNotBlank()
        ) {
            if (subs.none { it.name.equals(editingTransaction.subcategory, ignoreCase = true) }) {
                subs.add(0, SubcategoryEntity(parentCategory = selectedCategory, name = editingTransaction.subcategory, type = selectedType, isLegacy = true))
            }
        }
        subs
    }

    var selectedSubcategory by remember(txKey, selectedCategory, selectedType) {
        val initialSubMatch = if (
            editingTransaction != null &&
            editingTransaction.type == selectedType &&
            editingTransaction.category.equals(selectedCategory, ignoreCase = true) &&
            editingTransaction.subcategory.isNotBlank()
        ) {
            availableSubcategoryEntities.firstOrNull { it.name.equals(editingTransaction.subcategory, ignoreCase = true) }?.name
                ?: editingTransaction.subcategory
        } else null
        mutableStateOf(initialSubMatch ?: availableSubcategoryEntities.firstOrNull()?.name ?: "General")
    }

    LaunchedEffect(availableSubcategoryEntities) {
        if (availableSubcategoryEntities.isNotEmpty()) {
            val matchesExisting = availableSubcategoryEntities.any {
                it.name.equals(selectedSubcategory, ignoreCase = true)
            }
            if (!matchesExisting) {
                selectedSubcategory = availableSubcategoryEntities.first().name
            }
        }
    }

    // Auto-scroll LazyRows so active Category, Subcategory, and Vault chips are visible immediately
    val categoryListState = rememberLazyListState()
    val subcategoryListState = rememberLazyListState()
    val accountListState = rememberLazyListState()

    LaunchedEffect(selectedCategory, availableCategoryEntities) {
        val idx = availableCategoryEntities.indexOfFirst { it.name.equals(selectedCategory, ignoreCase = true) }
        if (idx >= 0) {
            categoryListState.animateScrollToItem(idx)
        }
    }

    LaunchedEffect(selectedSubcategory, availableSubcategoryEntities) {
        val idx = availableSubcategoryEntities.indexOfFirst { it.name.equals(selectedSubcategory, ignoreCase = true) }
        if (idx >= 0) {
            subcategoryListState.animateScrollToItem(idx)
        }
    }

    LaunchedEffect(selectedAccount, accountList) {
        val idx = accountList.indexOfFirst { it.equals(selectedAccount, ignoreCase = true) }
        if (idx >= 0) {
            accountListState.animateScrollToItem(idx)
        }
    }

    val isSelectedCategoryLegacy = remember(availableCategoryEntities, selectedCategory) {
        availableCategoryEntities.find { it.name.equals(selectedCategory, ignoreCase = true) }?.isLegacy == true
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isSelfTransfer = selectedType == TransactionType.TRANSFER &&
            selectedAccount.isNotBlank() &&
            selectedToAccount.isNotBlank() &&
            selectedAccount.equals(selectedToAccount, ignoreCase = true)
    val isInputValid = parsedAmount > 0.0 &&
            selectedAccount.isNotBlank() &&
            (selectedType != TransactionType.TRANSFER || selectedToAccount.isNotBlank()) &&
            !isSelfTransfer

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
                color = Neutral300
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

            // 5-Way Segment Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Neutral100)
                    .padding(3.dp)
            ) {
                listOf(
                    TransactionType.EXPENSE to "Expense",
                    TransactionType.INCOME to "Income",
                    TransactionType.ASSET to "Asset",
                    TransactionType.CORPORATE to "Corporate",
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
                                    val cats = masterCategories.filter { it.type == type }
                                    selectedCategory = cats.firstOrNull()?.name ?: "General"
                                    val subs = masterSubcategories.filter { it.parentCategory.equals(selectedCategory, ignoreCase = true) && it.type == type }
                                    selectedSubcategory = subs.firstOrNull()?.name ?: "General"
                                }
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.5.sp,
                            color = if (isSelected) {
                                when (type) {
                                    TransactionType.EXPENSE -> Error500
                                    TransactionType.INCOME -> Success500
                                    TransactionType.ASSET -> SoftTeal
                                    TransactionType.CORPORATE -> Warning700
                                    TransactionType.TRANSFER -> Primary600
                                }
                            } else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

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
                    focusedBorderColor = Primary600,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (selectedType == TransactionType.TRANSFER) "Transfer Note (Optional)" else "Note / Merchant (Optional)") },
                placeholder = {
                    Text(
                        if (selectedType == TransactionType.TRANSFER) "e.g., Emergency Reserve"
                        else selectedSubcategory.ifBlank { "e.g., Flight Ticket, Courier" }
                    )
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary600,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        onClick = {
                            selectedDateMillis = System.currentTimeMillis()
                            if (isRecurringCommitment && dueDayText.isBlank()) {
                                dueDayText = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).toString()
                            }
                        },
                        label = { Text("Today", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
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
                            if (isRecurringCommitment && dueDayText.isBlank()) {
                                dueDayText = cal.get(Calendar.DAY_OF_MONTH).toString()
                            }
                        },
                        label = { Text("Yesterday", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary50,
                            selectedLabelColor = Primary700
                        )
                    )

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(34.dp)
                            .clickable { showDatePickerDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        color = if (!isToday && !isYesterday) Primary50 else CanvasLight,
                        border = BorderStroke(0.8.dp, if (!isToday && !isYesterday) Primary600 else BorderLight)
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
                                color = if (!isToday && !isYesterday) Primary700 else TextDark
                            )
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pick Date",
                                tint = if (!isToday && !isYesterday) Primary600 else TextMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            if (selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Transfer Classification Subtype",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val subtypes = listOf(
                        TransferSubtype.BILL_FUNDING to "Bill Funding",
                        TransferSubtype.WEALTH_ALLOCATION to "Fortress Sweep",
                        TransferSubtype.REBALANCE to "Rebalance",
                        TransferSubtype.CASH_WITHDRAWAL to "Cash ATM"
                    )
                    items(subtypes) { (subtype, label) ->
                        val isSelected = selectedTransferSubtype == subtype
                        Surface(
                            modifier = Modifier
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { selectedTransferSubtype = subtype },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Primary50 else CanvasLight,
                            border = BorderStroke(0.8.dp, if (isSelected) Primary600 else BorderLight)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Primary700 else TextDark
                                )
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))

                Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    state = categoryListState,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = if (availableCategoryEntities.isEmpty()) listOf(CategoryEntity("General", selectedType)) else availableCategoryEntities,
                        key = { "${it.name}_${it.isLegacy}" }
                    ) { catEntity ->
                        val isSelected = selectedCategory.equals(catEntity.name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = catEntity.name
                                val subs = masterSubcategories.filter {
                                    it.parentCategory.equals(catEntity.name, ignoreCase = true) && it.type == selectedType
                                }
                                selectedSubcategory = subs.firstOrNull()?.name ?: "General"
                            },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(catEntity.name, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    if (catEntity.isLegacy) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Legacy",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Warning700,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Warning100)
                                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                        )
                                    } else if (catEntity.isNew) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Success500)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (catEntity.isLegacy) Warning100 else Primary50,
                                selectedLabelColor = if (catEntity.isLegacy) Warning700 else Primary700
                            ),
                            border = if (catEntity.isLegacy) BorderStroke(0.8.dp, Warning500) else null
                        )
                    }
                }

                AnimatedVisibility(visible = isSelectedCategoryLegacy) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Warning100.copy(alpha = 0.65f),
                        border = BorderStroke(0.6.dp, Warning500.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Warning700, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "'$selectedCategory' is a legacy category and will be retired next month.",
                                fontSize = 10.5.sp,
                                color = Warning700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (availableSubcategoryEntities.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Subcategory", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("(Primary Identification)", fontSize = 9.5.sp, color = TextMuted.copy(alpha = 0.7f))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        state = subcategoryListState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableSubcategoryEntities, key = { "${it.name}_${it.isLegacy}" }) { subEntity ->
                            val isSelected = selectedSubcategory.equals(subEntity.name, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSubcategory = subEntity.name },
                                label = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(subEntity.name, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                        if (subEntity.isLegacy) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Legacy",
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Warning700,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Warning100)
                                                    .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                            )
                                        } else if (subEntity.isNew) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(Success500)
                                            )
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = if (subEntity.isLegacy) Warning100 else Primary50,
                                    selectedLabelColor = if (subEntity.isLegacy) Warning700 else Primary700
                                ),
                                border = if (subEntity.isLegacy) BorderStroke(0.8.dp, Warning500) else null
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (selectedType == TransactionType.TRANSFER) "Source Vault (From)" else "Vault Account",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                state = accountListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accountList) { acc ->
                    val isSelected = selectedAccount.equals(acc, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedAccount = acc
                            if (selectedToAccount.equals(acc, ignoreCase = true)) {
                                selectedToAccount = accountList.firstOrNull { !it.equals(acc, ignoreCase = true) }.orEmpty()
                            }
                        },
                        label = { Text(acc, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary50,
                            selectedLabelColor = Primary700
                        )
                    )
                }
            }

            if (selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Destination Vault (To)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(accountList.filter { !it.equals(selectedAccount, ignoreCase = true) }) { acc ->
                        val isSelected = selectedToAccount.equals(acc, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedToAccount = acc },
                            label = { Text(acc, fontSize = 11.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftTealLight,
                                selectedLabelColor = SoftTeal
                            )
                        )
                    }
                }

                if (isSelfTransfer) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Error500, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Source and destination vaults must be distinct.", fontSize = 10.5.sp, color = Error700, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (!isEditing) {
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
                                    text = if (selectedType == TransactionType.TRANSFER) "Set as Recurring Monthly Sweep" else "Repeat as Monthly AutoPay",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Text(
                                    text = "Creates recurring commitment and settles this month",
                                    fontSize = 10.5.sp,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = isRecurringCommitment,
                                onCheckedChange = { checked ->
                                    isRecurringCommitment = checked
                                    if (checked && dueDayText.isBlank()) {
                                        val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
                                        dueDayText = cal.get(Calendar.DAY_OF_MONTH).toString()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Primary600,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = BorderLight
                                )
                            )
                        }

                        if (isRecurringCommitment) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Monthly Due Day (1-31):",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark
                                )
                                OutlinedTextField(
                                    value = dueDayText,
                                    onValueChange = { input ->
                                        val digits = input.filter { it.isDigit() }
                                        if (digits.isEmpty()) {
                                            dueDayText = ""
                                        } else if (digits.length <= 2) {
                                            val num = digits.toIntOrNull() ?: 0
                                            if (num in 1..31) {
                                                dueDayText = digits
                                            }
                                        }
                                    },
                                    placeholder = { Text("Day", fontSize = 11.sp) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.width(75.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Primary600,
                                        unfocusedBorderColor = BorderLight
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isInputValid) {
                        val resolvedCategory = if (selectedType == TransactionType.TRANSFER) "Transfer" else selectedCategory.ifBlank { "General" }
                        val resolvedSubcategory = if (selectedType == TransactionType.TRANSFER) selectedTransferSubtype.name else selectedSubcategory.ifBlank { "General" }

                        val cleanNote = title.trim()
                        val resolvedTitle = when {
                            cleanNote.isNotBlank() && !cleanNote.equals(resolvedSubcategory, ignoreCase = true) -> {
                                if (cleanNote.startsWith(resolvedSubcategory, ignoreCase = true)) {
                                    val stripped = cleanNote.removePrefix(resolvedSubcategory).trim(' ', '-', ':', '(', ')')
                                    if (stripped.isNotBlank()) stripped else resolvedSubcategory
                                } else {
                                    cleanNote
                                }
                            }
                            selectedType == TransactionType.TRANSFER -> "Vault Transfer ($selectedAccount ➔ $selectedToAccount)"
                            else -> resolvedSubcategory
                        }

                        val parsedDueDay = if (isRecurringCommitment) {
                            dueDayText.toIntOrNull()?.let { if (it in 1..31) it else null }
                                ?: Calendar.getInstance().apply { timeInMillis = selectedDateMillis }.get(Calendar.DAY_OF_MONTH)
                        } else null

                        onSave(
                            editingTransaction?.id ?: 0L,
                            resolvedTitle,
                            parsedAmount,
                            resolvedCategory,
                            resolvedSubcategory,
                            selectedAccount.ifBlank { accountList.firstOrNull().orEmpty() },
                            if (selectedType == TransactionType.TRANSFER) selectedToAccount else null,
                            selectedType,
                            selectedDateMillis,
                            isRecurringCommitment,
                            parsedDueDay
                        )
                        onDismiss()
                    } else {
                        val msg = if (isSelfTransfer) "Source and destination vaults must be distinct." else "Please enter a valid amount > 0"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = isInputValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary600)
            ) {
                Text(
                    text = if (isEditing) "Update Entry" else if (isRecurringCommitment) "Save & Create AutoPay" else "Save Entry",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
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
                            if (isRecurringCommitment && dueDayText.isBlank()) {
                                dueDayText = localCal.get(Calendar.DAY_OF_MONTH).toString()
                            }
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Select", fontWeight = FontWeight.Bold, color = Primary600)
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
