package com.example.myfin.ui.components

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
import com.example.myfin.ui.theme.*

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
                val types = if (editingTransaction?.type == TransactionType.TRANSFER) {
                    listOf(
                        TransactionType.EXPENSE to "Expense",
                        TransactionType.INCOME to "Income",
                        TransactionType.ASSET to "Asset / SIP",
                        TransactionType.TRANSFER to "Transfer"
                    )
                } else {
                    listOf(
                        TransactionType.EXPENSE to "Expense",
                        TransactionType.INCOME to "Income",
                        TransactionType.ASSET to "Asset / SIP"
                    )
                }

                types.forEach { (type, label) ->
                    val isSelected = selectedType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) CardWhite else Color.Transparent)
                            .clickable {
                                selectedType = type
                                val cats = masterCategories.filter { it.type == type }.map { it.name }
                                selectedCategory = cats.firstOrNull().orEmpty()
                                val subs = masterSubcategories.filter { it.parentCategory == selectedCategory }.map { it.name }
                                selectedSubcategory = subs.firstOrNull().orEmpty()
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

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title / Merchant (Optional)") },
                placeholder = { Text(selectedSubcategory.ifBlank { "e.g., Grocery Store" }) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )

            if (selectedType != TransactionType.TRANSFER) {
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

            // Vault Account Chips
            Text("Vault Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
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

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (isInputValid) {
                        onSave(
                            editingTransaction?.id ?: 0L,
                            title.trim(),
                            parsedAmount,
                            selectedCategory.ifBlank { "General" },
                            selectedSubcategory.ifBlank { "General" },
                            selectedAccount.ifBlank { accountList.firstOrNull().orEmpty() },
                            selectedType,
                            editingTransaction?.date ?: System.currentTimeMillis()
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
}
