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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.CategoryEntity
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.SubcategoryEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.TransferSubtype
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFixedBillDialog(
    initialBill: FixedBillEntity? = null,
    currencySymbol: String = "₹",
    accountList: List<String>,
    masterCategories: List<CategoryEntity>,
    masterSubcategories: List<SubcategoryEntity>,
    onAddNewCategory: (String, TransactionType) -> Unit,
    onAddNewSubcategory: (String, String, TransactionType) -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        category: String,
        subcategory: String,
        accountName: String,
        toAccountName: String?,
        type: TransactionType,
        dueDay: Int?,
        isPaid: Boolean,
        paidDateMillis: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val billKey = initialBill?.id ?: -1L

    var selectedType by remember(billKey) {
        mutableStateOf(initialBill?.type ?: TransactionType.EXPENSE)
    }

    var isCorporateInflow by remember(billKey) {
        mutableStateOf(
            initialBill?.let { bill ->
                bill.type == TransactionType.CORPORATE &&
                        bill.category.trim().equals("Reimbursements & Claims", ignoreCase = true)
            } ?: false
        )
    }

    var noteText by remember(billKey) {
        mutableStateOf(
            initialBill?.let { bill ->
                val cleanTitle = bill.title.trim()
                val cleanSubcat = bill.subcategory.trim()
                when {
                    cleanTitle.isBlank() ||
                            cleanTitle.equals(cleanSubcat, ignoreCase = true) ||
                            cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> ""
                    else -> cleanTitle
                }
            } ?: ""
        )
    }

    var amountText by remember(billKey) {
        mutableStateOf(
            initialBill?.amount?.let {
                if (it > 0) {
                    if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                } else ""
            } ?: ""
        )
    }

    var dueDayText by remember(billKey) {
        mutableStateOf(initialBill?.dueDay?.toString() ?: "")
    }

    var selectedTransferSubtype by remember(billKey) {
        mutableStateOf(
            try {
                TransferSubtype.valueOf(initialBill?.subcategory.orEmpty())
            } catch (_: Exception) {
                when (initialBill?.subcategory) {
                    "Fortress Sweep" -> TransferSubtype.WEALTH_ALLOCATION
                    "Cash ATM", "Cash ATM Withdrawal" -> TransferSubtype.CASH_WITHDRAWAL
                    "Rebalance", "Vault Rebalance" -> TransferSubtype.REBALANCE
                    else -> TransferSubtype.BILL_FUNDING
                }
            }
        )
    }

    val defaultPaidDateMillis = remember(billKey, initialBill?.month, initialBill?.year, initialBill?.dueDay) {
        if (initialBill == null) {
            System.currentTimeMillis()
        } else {
            val nowCal = Calendar.getInstance()
            val sysMonth = nowCal.get(Calendar.MONTH) + 1
            val sysYear = nowCal.get(Calendar.YEAR)
            if (initialBill.month == sysMonth && initialBill.year == sysYear) {
                System.currentTimeMillis()
            } else {
                Calendar.getInstance().apply {
                    set(Calendar.YEAR, initialBill.year)
                    set(Calendar.MONTH, (initialBill.month - 1).coerceIn(0, 11))
                    set(Calendar.DAY_OF_MONTH, 1)
                    val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                    val targetDay = (initialBill.dueDay ?: 1).coerceIn(1, maxDay)
                    set(Calendar.DAY_OF_MONTH, targetDay)
                    set(Calendar.HOUR_OF_DAY, 12)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            }
        }
    }

    var isPaidState by remember(billKey) { mutableStateOf(initialBill?.isPaid ?: false) }
    var selectedDateMillis by remember(billKey) { mutableStateOf(defaultPaidDateMillis) }
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

    val filteredCategoryEntities = remember(masterCategories, selectedType, isCorporateInflow, initialBill) {
        val baseCats = masterCategories.filter { it.type == selectedType }
        val cats = if (selectedType == TransactionType.CORPORATE) {
            val filtered = baseCats.filter { cat ->
                val isClaimCat = cat.name.equals("Reimbursements & Claims", ignoreCase = true)
                if (isCorporateInflow) isClaimCat else !isClaimCat
            }.toMutableList()
            if (filtered.isEmpty()) {
                val fallbackName = if (isCorporateInflow) "Reimbursements & Claims" else "Work & Professional"
                filtered.add(CategoryEntity(name = fallbackName, type = TransactionType.CORPORATE))
            }
            filtered
        } else {
            baseCats.toMutableList()
        }

        if (initialBill != null && initialBill.type == selectedType && initialBill.category.isNotBlank()) {
            val matchesDirection = selectedType != TransactionType.CORPORATE ||
                    (initialBill.category.equals("Reimbursements & Claims", ignoreCase = true) == isCorporateInflow)
            if (matchesDirection && cats.none { it.name.equals(initialBill.category, ignoreCase = true) }) {
                cats.add(0, CategoryEntity(name = initialBill.category, type = selectedType, isLegacy = true))
            }
        }
        cats
    }

    var selectedCategory by remember(billKey, selectedType) {
        val initialMatch = if (initialBill != null && initialBill.type == selectedType && initialBill.category.isNotBlank()) {
            filteredCategoryEntities.firstOrNull { it.name.equals(initialBill.category, ignoreCase = true) }?.name
                ?: initialBill.category
        } else null
        mutableStateOf(initialMatch ?: filteredCategoryEntities.firstOrNull()?.name ?: "General")
    }

    LaunchedEffect(selectedType, isCorporateInflow, filteredCategoryEntities) {
        if (selectedType != TransactionType.TRANSFER && filteredCategoryEntities.isNotEmpty()) {
            val matchesExisting = filteredCategoryEntities.any {
                it.name.equals(selectedCategory, ignoreCase = true)
            }
            if (!matchesExisting) {
                selectedCategory = filteredCategoryEntities.first().name
            }
        }
    }

    val availableSubcategoryEntities = remember(masterSubcategories, selectedCategory, selectedType, initialBill) {
        val subs = masterSubcategories.filter {
            it.parentCategory.equals(selectedCategory, ignoreCase = true) && it.type == selectedType
        }.toMutableList()

        if (initialBill != null && initialBill.type == selectedType &&
            initialBill.category.equals(selectedCategory, ignoreCase = true) && initialBill.subcategory.isNotBlank()
        ) {
            if (subs.none { it.name.equals(initialBill.subcategory, ignoreCase = true) }) {
                subs.add(0, SubcategoryEntity(parentCategory = selectedCategory, name = initialBill.subcategory, type = selectedType, isLegacy = true))
            }
        }
        subs
    }

    var selectedSubcategory by remember(billKey, selectedCategory, selectedType) {
        val initialSubMatch = if (
            initialBill != null &&
            initialBill.type == selectedType &&
            initialBill.category.equals(selectedCategory, ignoreCase = true) &&
            initialBill.subcategory.isNotBlank()
        ) {
            availableSubcategoryEntities.firstOrNull { it.name.equals(initialBill.subcategory, ignoreCase = true) }?.name
                ?: initialBill.subcategory
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

    val categoryListState = rememberLazyListState()
    val subcategoryListState = rememberLazyListState()
    val accountListState = rememberLazyListState()

    LaunchedEffect(selectedCategory, filteredCategoryEntities) {
        val idx = filteredCategoryEntities.indexOfFirst { it.name.equals(selectedCategory, ignoreCase = true) }
        if (idx >= 0) categoryListState.animateScrollToItem(idx)
    }

    LaunchedEffect(selectedSubcategory, availableSubcategoryEntities) {
        val idx = availableSubcategoryEntities.indexOfFirst { it.name.equals(selectedSubcategory, ignoreCase = true) }
        if (idx >= 0) subcategoryListState.animateScrollToItem(idx)
    }

    val isSelectedCategoryLegacy = remember(filteredCategoryEntities, selectedCategory) {
        filteredCategoryEntities.find { it.name.equals(selectedCategory, ignoreCase = true) }?.isLegacy == true
    }

    var selectedAccount by remember(billKey) {
        mutableStateOf(initialBill?.accountName ?: accountList.firstOrNull() ?: "PRIMARY BANK")
    }
    var selectedToAccount by remember(billKey) {
        mutableStateOf(initialBill?.toAccountName ?: accountList.getOrNull(1) ?: accountList.firstOrNull() ?: "SECONDARY BANK")
    }

    LaunchedEffect(selectedAccount, accountList) {
        val idx = accountList.indexOfFirst { it.equals(selectedAccount, ignoreCase = true) }
        if (idx >= 0) accountListState.animateScrollToItem(idx)
    }

    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var showNewSubcategoryDialog by remember { mutableStateOf(false) }
    var newSubcategoryName by remember { mutableStateOf("") }

    val isSelfTransfer = selectedType == TransactionType.TRANSFER &&
            selectedAccount.isNotBlank() &&
            selectedToAccount.isNotBlank() &&
            selectedAccount.equals(selectedToAccount, ignoreCase = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFFF2F5FF),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 4.dp)
                    .width(38.dp)
                    .height(4.dp),
                shape = CircleShape,
                color = Neutral300
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFF2F5FF), Color(0xFFF8FAFC), CardWhite)
                    )
                )
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 2.dp, bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialBill == null) "Add AutoPay Commitment" else "Edit AutoPay Template",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = TextDark
                )
                Surface(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onDismiss),
                    shape = CircleShape,
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextDark,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 5-Way Segment Switcher
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Neutral100,
                border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.85f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                                .clickable { selectedType = type }
                                .padding(vertical = 7.dp),
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
            }

            // Explicit 2-Way Direction Toggle for Corporate AutoPay Commitments
            AnimatedVisibility(visible = selectedType == TransactionType.CORPORATE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isCorporateInflow = false },
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isCorporateInflow) Warning100 else CardWhite,
                        border = BorderStroke(
                            width = if (!isCorporateInflow) 1.2.dp else 0.8.dp,
                            color = if (!isCorporateInflow) Warning500 else BorderLight
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (!isCorporateInflow) Warning500 else BorderLight.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NorthEast,
                                    contentDescription = null,
                                    tint = if (!isCorporateInflow) Color.White else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Column {
                                Text(
                                    text = "Paid for Work",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isCorporateInflow) Warning700 else TextDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Company owes you",
                                    fontSize = 9.sp,
                                    color = if (!isCorporateInflow) Warning700.copy(alpha = 0.85f) else TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isCorporateInflow = true },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isCorporateInflow) Success100 else CardWhite,
                        border = BorderStroke(
                            width = if (isCorporateInflow) 1.2.dp else 0.8.dp,
                            color = if (isCorporateInflow) Success500 else BorderLight
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(if (isCorporateInflow) Success500 else BorderLight.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SouthWest,
                                    contentDescription = null,
                                    tint = if (isCorporateInflow) Color.White else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(7.dp))
                            Column {
                                Text(
                                    text = "From Company",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCorporateInflow) Success700 else TextDark,
                                    maxLines = 1
                                )
                                Text(
                                    text = "Claim or advance",
                                    fontSize = 9.sp,
                                    color = if (isCorporateInflow) Success700.copy(alpha = 0.85f) else TextMuted,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        val parts = filtered.split('.')
                        amountText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                    },
                    label = { Text("Amount ($currencySymbol)", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1.25f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedBorderColor = Primary600,
                        unfocusedBorderColor = BorderLight
                    )
                )

                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { input ->
                        val digits = input.filter { it.isDigit() }
                        if (digits.isEmpty()) {
                            dueDayText = ""
                        } else if (digits.length <= 2) {
                            val num = digits.toIntOrNull() ?: 0
                            if (num in 1..31) dueDayText = digits
                        }
                    },
                    label = { Text("Due Day", fontSize = 12.sp) },
                    placeholder = { Text("1-31", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(0.95f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedBorderColor = Primary600,
                        unfocusedBorderColor = BorderLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note / Title (Optional)", fontSize = 12.sp) },
                placeholder = { Text("e.g. Netflix, Rent, Investment SIP", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedBorderColor = Primary600,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedType == TransactionType.TRANSFER) {
                Text("Transfer Classification", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(5.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val subtypes = listOf(
                        TransferSubtype.BILL_FUNDING to "Bill Funding",
                        TransferSubtype.WEALTH_ALLOCATION to "Fortress Sweep",
                        TransferSubtype.REBALANCE to "Rebalance",
                        TransferSubtype.CASH_WITHDRAWAL to "Cash ATM"
                    )
                    items(subtypes) { (subtype, label) ->
                        FixedBillPillChip(
                            label = label,
                            selected = selectedTransferSubtype == subtype,
                            onClick = { selectedTransferSubtype = subtype }
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showNewCategoryDialog = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = Primary600)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("New", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary600)
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))

                LazyRow(
                    state = categoryListState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(
                        items = if (filteredCategoryEntities.isEmpty()) listOf(CategoryEntity("General", selectedType)) else filteredCategoryEntities,
                        key = { "${it.name}_${it.isLegacy}" }
                    ) { catEntity ->
                        val isSelected = selectedCategory.equals(catEntity.name, ignoreCase = true)
                        FixedBillPillChip(
                            label = catEntity.name,
                            selected = isSelected,
                            isLegacy = catEntity.isLegacy,
                            isNew = catEntity.isNew,
                            onClick = { selectedCategory = catEntity.name }
                        )
                    }
                }

                AnimatedVisibility(visible = isSelectedCategoryLegacy) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Warning100,
                        border = BorderStroke(0.6.dp, Warning500.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Warning700, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "'$selectedCategory' is a legacy category and will be retired next month.",
                                fontSize = 10.sp,
                                color = Warning700
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Subcategory", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("(Primary ID)", fontSize = 9.5.sp, color = TextMuted)
                    }
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { showNewSubcategoryDialog = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp), tint = Primary600)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("New", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary600)
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))

                LazyRow(
                    state = subcategoryListState,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(availableSubcategoryEntities, key = { "${it.name}_${it.isLegacy}" }) { subEntity ->
                        val isSelected = selectedSubcategory.equals(subEntity.name, ignoreCase = true)
                        FixedBillPillChip(
                            label = subEntity.name,
                            selected = isSelected,
                            isLegacy = subEntity.isLegacy,
                            isNew = subEntity.isNew,
                            onClick = { selectedSubcategory = subEntity.name }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = when {
                    selectedType == TransactionType.TRANSFER -> "Source Vault (From)"
                    selectedType == TransactionType.INCOME -> "Credit Vault (To)"
                    selectedType == TransactionType.CORPORATE && isCorporateInflow -> "Credit Vault (To)"
                    else -> "Deduction Vault"
                },
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(5.dp))
            LazyRow(
                state = accountListState,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(accountList) { acc ->
                    val isSelected = selectedAccount.equals(acc, ignoreCase = true)
                    FixedBillPillChip(
                        label = acc,
                        selected = isSelected,
                        onClick = {
                            selectedAccount = acc
                            if (selectedToAccount.equals(acc, ignoreCase = true)) {
                                selectedToAccount = accountList.firstOrNull { !it.equals(acc, ignoreCase = true) }.orEmpty()
                            }
                        }
                    )
                }
            }

            if (selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Destination Vault (To)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(5.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accountList.filter { !it.equals(selectedAccount, ignoreCase = true) }) { acc ->
                        val isSelected = selectedToAccount.equals(acc, ignoreCase = true)
                        FixedBillPillChip(
                            label = acc,
                            selected = isSelected,
                            selectedBg = SoftTealLight,
                            selectedText = SoftTeal,
                            selectedBorder = SoftTeal,
                            onClick = { selectedToAccount = acc }
                        )
                    }
                }

                if (isSelfTransfer) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = Error500, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Source and destination vaults must be distinct.", fontSize = 10.sp, color = Error700, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CardWhite,
                border = BorderStroke(0.7.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mark as Paid for this cycle",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = if (isPaidState) "Will record a completed transaction" else "Will stay scheduled as pending",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = isPaidState,
                            onCheckedChange = { isPaidState = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Primary600,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BorderLight
                            )
                        )
                    }

                    if (isPaidState) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Payment Date", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(5.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FixedBillPillChip(
                                label = "Today",
                                selected = isToday,
                                onClick = { selectedDateMillis = System.currentTimeMillis() }
                            )
                            FixedBillPillChip(
                                label = "Yesterday",
                                selected = isYesterday,
                                onClick = {
                                    val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
                                    selectedDateMillis = cal.timeInMillis
                                }
                            )
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
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
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextDark)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (selectedType == TransactionType.TRANSFER && selectedToAccount.isBlank()) {
                            Toast.makeText(context, "Please select a destination vault.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (isSelfTransfer) {
                            Toast.makeText(context, "Source and destination vaults must be distinct.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val parsedDueDay = dueDayText.toIntOrNull()?.let { day ->
                            if (day in 1..31) day else null
                        }

                        val resolvedCategory = if (selectedType == TransactionType.TRANSFER) "Transfer" else selectedCategory
                        val resolvedSubcategory = if (selectedType == TransactionType.TRANSFER) selectedTransferSubtype.name else selectedSubcategory

                        val cleanNote = noteText.trim()
                        val finalTitle = when {
                            cleanNote.isNotBlank() && !cleanNote.equals(resolvedSubcategory, ignoreCase = true) -> cleanNote
                            selectedType == TransactionType.TRANSFER -> "Vault Transfer ($selectedAccount \u2794 $selectedToAccount)"
                            else -> resolvedSubcategory
                        }

                        onSave(
                            finalTitle,
                            amt,
                            resolvedCategory,
                            resolvedSubcategory,
                            selectedAccount,
                            if (selectedType == TransactionType.TRANSFER) selectedToAccount else null,
                            selectedType,
                            parsedDueDay,
                            isPaidState,
                            selectedDateMillis
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1.3f).height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary600)
                ) {
                    Text(
                        text = if (initialBill == null) "Save AutoPay" else "Update AutoPay",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }
        }
    }

    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            containerColor = CardWhite,
            title = { Text("New Category", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = newCategoryName.trim()
                    if (trimmed.isNotBlank()) {
                        onAddNewCategory(trimmed, selectedType)
                        selectedCategory = trimmed
                        newCategoryName = ""
                        showNewCategoryDialog = false
                    }
                }) {
                    Text("Add", fontWeight = FontWeight.Bold, color = Primary600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCategoryDialog = false }) {
                    Text("Cancel", color = TextDark)
                }
            }
        )
    }

    if (showNewSubcategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubcategoryDialog = false },
            containerColor = CardWhite,
            title = { Text("New Subcategory for $selectedCategory", fontWeight = FontWeight.Bold, color = TextDark) },
            text = {
                OutlinedTextField(
                    value = newSubcategoryName,
                    onValueChange = { newSubcategoryName = it },
                    label = { Text("Subcategory Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val trimmed = newSubcategoryName.trim()
                    if (trimmed.isNotBlank()) {
                        onAddNewSubcategory(selectedCategory, trimmed, selectedType)
                        selectedSubcategory = trimmed
                        newSubcategoryName = ""
                        showNewSubcategoryDialog = false
                    }
                }) {
                    Text("Add", fontWeight = FontWeight.Bold, color = Primary600)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewSubcategoryDialog = false }) {
                    Text("Cancel", color = TextDark)
                }
            }
        )
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

@Composable
private fun FixedBillPillChip(
    label: String,
    selected: Boolean,
    isLegacy: Boolean = false,
    isNew: Boolean = false,
    selectedBg: Color = Primary50,
    selectedText: Color = Primary700,
    selectedBorder: Color = Primary600,
    onClick: () -> Unit
) {
    val bgColor = when {
        selected && isLegacy -> Warning100
        selected -> selectedBg
        else -> CardWhite
    }
    val txtColor = when {
        selected && isLegacy -> Warning700
        selected -> selectedText
        else -> TextDark
    }
    val borderColor = when {
        isLegacy -> Warning500
        selected -> selectedBorder
        else -> BorderLight
    }

    Surface(
        modifier = Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        border = BorderStroke(0.8.dp, borderColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = txtColor,
                maxLines = 1
            )
            if (isLegacy) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Legacy",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = Warning700,
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(Warning100)
                        .padding(horizontal = 3.dp, vertical = 0.5.dp)
                )
            } else if (isNew) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Success500)
                )
            }
        }
    }
}
