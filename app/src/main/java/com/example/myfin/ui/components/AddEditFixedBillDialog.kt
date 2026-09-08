package com.example.myfin.ui.components

import android.widget.Toast
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Add
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

    var selectedType by remember { mutableStateOf(initialBill?.type ?: TransactionType.EXPENSE) }

    var noteText by remember {
        mutableStateOf(
            initialBill?.let { bill ->
                val cleanTitle = bill.title.trim()
                val cleanSubcat = bill.subcategory.trim()
                when {
                    cleanTitle.isBlank() || cleanTitle.equals(cleanSubcat, ignoreCase = true) || cleanTitle.startsWith("Vault Transfer", ignoreCase = true) -> ""
                    else -> cleanTitle
                }
            } ?: ""
        )
    }

    var amountText by remember { mutableStateOf(initialBill?.amount?.let { if (it > 0) it.toString() else "" } ?: "") }
    var dueDayText by remember { mutableStateOf(initialBill?.dueDay?.toString() ?: "") }

    var selectedTransferSubtype by remember {
        mutableStateOf(
            try {
                TransferSubtype.valueOf(initialBill?.subcategory.orEmpty())
            } catch (_: Exception) {
                when (initialBill?.subcategory) {
                    "Fortress Sweep" -> TransferSubtype.WEALTH_ALLOCATION
                    "Rebalance" -> TransferSubtype.REBALANCE
                    else -> TransferSubtype.BILL_FUNDING
                }
            }
        )
    }

    var isPaidState by remember { mutableStateOf(initialBill?.isPaid ?: false) }
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

    // Preserve historical bill's category even if it is legacy
    val filteredCategoryEntities = remember(masterCategories, selectedType, initialBill) {
        val cats = masterCategories.filter { it.type == selectedType }.toMutableList()
        if (initialBill != null && initialBill.type == selectedType && initialBill.category.isNotBlank()) {
            if (cats.none { it.name.equals(initialBill.category, ignoreCase = true) }) {
                cats.add(CategoryEntity(name = initialBill.category, type = selectedType, isLegacy = true))
            }
        }
        cats
    }

    var selectedCategory by remember(filteredCategoryEntities) {
        mutableStateOf(
            initialBill?.category ?: filteredCategoryEntities.firstOrNull()?.name ?: "General"
        )
    }

    LaunchedEffect(selectedType) {
        if (selectedType != TransactionType.TRANSFER) {
            val names = filteredCategoryEntities.map { it.name }
            if (names.isNotEmpty() && selectedCategory !in names) {
                selectedCategory = names.firstOrNull() ?: "General"
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
                subs.add(SubcategoryEntity(parentCategory = selectedCategory, name = initialBill.subcategory, type = selectedType, isLegacy = true))
            }
        }
        subs
    }

    var selectedSubcategory by remember(availableSubcategoryEntities) {
        mutableStateOf(
            initialBill?.subcategory ?: availableSubcategoryEntities.firstOrNull()?.name ?: "General"
        )
    }

    LaunchedEffect(availableSubcategoryEntities) {
        val names = availableSubcategoryEntities.map { it.name }
        if (names.isNotEmpty() && selectedSubcategory !in names) {
            selectedSubcategory = names.firstOrNull() ?: "General"
        }
    }

    val isSelectedCategoryLegacy = remember(filteredCategoryEntities, selectedCategory) {
        filteredCategoryEntities.find { it.name.equals(selectedCategory, ignoreCase = true) }?.isLegacy == true
    }

    var selectedAccount by remember {
        mutableStateOf(initialBill?.accountName ?: accountList.firstOrNull() ?: "PRIMARY BANK")
    }
    var selectedToAccount by remember {
        mutableStateOf(initialBill?.toAccountName ?: accountList.getOrNull(1) ?: accountList.firstOrNull() ?: "SECONDARY BANK")
    }

    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var showNewSubcategoryDialog by remember { mutableStateOf(false) }
    var newSubcategoryName by remember { mutableStateOf("") }

    val isSelfTransfer = selectedType == TransactionType.TRANSFER && selectedAccount.isNotBlank() && selectedToAccount.isNotBlank() && selectedAccount.equals(selectedToAccount, ignoreCase = true)

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
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialBill == null) "Add AutoPay Commitment" else "Edit AutoPay Template",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = TextDark
                )
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

            // 5-Way Segment Switcher
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
                                    TransactionType.EXPENSE -> SoftRed
                                    TransactionType.INCOME -> SoftGreen
                                    TransactionType.ASSET -> SoftTeal
                                    TransactionType.CORPORATE -> Color(0xFFE57A28)
                                    TransactionType.TRANSFER -> AccentPurple
                                }
                            } else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = BorderLight
                    )
                )

                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { if (it.length <= 2) dueDayText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Due Day (1-31)", fontSize = 12.sp) },
                    placeholder = { Text("Opt", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = BorderLight
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Note / Title (Optional)", fontSize = 12.sp) },
                placeholder = { Text("e.g. Netflix, Rent, Investment SIP", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedType == TransactionType.TRANSFER) {
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
                            border = BorderStroke(0.8.dp, if (isSelected) AccentPurple else BorderLight)
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    TextButton(
                        onClick = { showNewCategoryDialog = true },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentPurple)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("New", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(
                        items = if (filteredCategoryEntities.isEmpty()) listOf(CategoryEntity("General", selectedType)) else filteredCategoryEntities,
                        key = { "${it.name}_${it.isLegacy}" }
                    ) { catEntity ->
                        val isSelected = selectedCategory.equals(catEntity.name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = catEntity.name },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(catEntity.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    if (catEntity.isLegacy) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Legacy",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFFFFF3E0))
                                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                        )
                                    } else if (catEntity.isNew) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50))
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (catEntity.isLegacy) Color(0xFFFFF3E0) else AccentPurpleLight,
                                selectedLabelColor = if (catEntity.isLegacy) Color(0xFFE65100) else AccentPurple
                            ),
                            border = if (catEntity.isLegacy) BorderStroke(0.8.dp, Color(0xFFFFB74D)) else null
                        )
                    }
                }

                AnimatedVisibility(visible = isSelectedCategoryLegacy) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFFF9E6),
                        border = BorderStroke(0.6.dp, Color(0xFFFFCC00).copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFD48800), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "'$selectedCategory' is a legacy category and will be retired next month.",
                                fontSize = 10.5.sp,
                                color = Color(0xFF873800)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Subcategory", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("(Primary Identification)", fontSize = 10.sp, color = TextMuted)
                    }
                    TextButton(
                        onClick = { showNewSubcategoryDialog = true },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentPurple)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("New", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableSubcategoryEntities, key = { "${it.name}_${it.isLegacy}" }) { subEntity ->
                        val isSelected = selectedSubcategory.equals(subEntity.name, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedSubcategory = subEntity.name },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(subEntity.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                    if (subEntity.isLegacy) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Legacy",
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFE65100),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(0xFFFFF3E0))
                                                .padding(horizontal = 3.dp, vertical = 0.5.dp)
                                        )
                                    } else if (subEntity.isNew) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(5.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF4CAF50))
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (subEntity.isLegacy) Color(0xFFFFF3E0) else AccentPurpleLight,
                                selectedLabelColor = if (subEntity.isLegacy) Color(0xFFE65100) else AccentPurple
                            ),
                            border = if (subEntity.isLegacy) BorderStroke(0.8.dp, Color(0xFFFFB74D)) else null
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (selectedType == TransactionType.TRANSFER) "Source Vault (From)" else "Deduction Vault",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(accountList) { acc ->
                    val isSelected = selectedAccount == acc
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedAccount = acc
                            if (selectedToAccount.equals(acc, ignoreCase = true)) {
                                selectedToAccount = accountList.firstOrNull { !it.equals(acc, ignoreCase = true) }.orEmpty()
                            }
                        },
                        label = { Text(acc, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        shape = RoundedCornerShape(8.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentPurpleLight,
                            selectedLabelColor = AccentPurple
                        )
                    )
                }
            }

            if (selectedType == TransactionType.TRANSFER) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Destination Vault (To)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accountList.filter { it != selectedAccount }) { acc ->
                        val isSelected = selectedToAccount == acc
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedToAccount = acc },
                            label = { Text(acc, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SoftTeal.copy(alpha = 0.15f),
                                selectedLabelColor = SoftTeal
                            )
                        )
                    }
                }

                if (isSelfTransfer) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = SoftRed, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Source and destination vaults must be distinct.", fontSize = 10.5.sp, color = SoftRed, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

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
                                text = "Mark as Paid for this cycle",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                            Text(
                                text = if (isPaidState) "Will record a completed transaction" else "Will stay scheduled as pending",
                                fontSize = 10.5.sp,
                                color = TextMuted
                            )
                        }

                        Switch(
                            checked = isPaidState,
                            onCheckedChange = { isPaidState = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentPurple,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BorderLight
                            )
                        )
                    }

                    if (isPaidState) {
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Payment Date",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
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
                                color = if (!isToday && !isYesterday) AccentPurpleLight else CardWhite,
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
                    Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt <= 0.0) {
                            Toast.makeText(context, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
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
                            selectedType == TransactionType.TRANSFER -> "Vault Transfer ($selectedAccount ➔ $selectedToAccount)"
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
                    modifier = Modifier.weight(1.3f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text(
                        text = if (initialBill == null) "Save AutoPay" else "Update AutoPay",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        if (showNewCategoryDialog) {
            AlertDialog(
                onDismissRequest = { showNewCategoryDialog = false },
                title = { Text("New Category", fontWeight = FontWeight.Bold) },
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
                        if (newCategoryName.isNotBlank()) {
                            onAddNewCategory(newCategoryName.trim(), selectedType)
                            selectedCategory = newCategoryName.trim()
                            newCategoryName = ""
                            showNewCategoryDialog = false
                        }
                    }) {
                        Text("Add", fontWeight = FontWeight.Bold, color = AccentPurple)
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
                title = { Text("New Subcategory for $selectedCategory", fontWeight = FontWeight.Bold) },
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
                        if (newSubcategoryName.isNotBlank()) {
                            onAddNewSubcategory(selectedCategory, newSubcategoryName.trim(), selectedType)
                            selectedSubcategory = newSubcategoryName.trim()
                            newSubcategoryName = ""
                            showNewSubcategoryDialog = false
                        }
                    }) {
                        Text("Add", fontWeight = FontWeight.Bold, color = AccentPurple)
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
}
