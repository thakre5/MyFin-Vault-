package com.example.myfin.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myfin.data.CategoryEntity
import com.example.myfin.data.FixedBillEntity
import com.example.myfin.data.SubcategoryEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.theme.*

@Composable
fun AddEditFixedBillDialog(
    initialBill: FixedBillEntity? = null,
    accountList: List<String>,
    masterCategories: List<CategoryEntity>,
    masterSubcategories: List<SubcategoryEntity>,
    onAddNewCategory: (String, TransactionType) -> Unit,
    onAddNewSubcategory: (String, String, TransactionType) -> Unit,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, category: String, subcategory: String, account: String, toAccount: String?, type: TransactionType, dueDay: Int?) -> Unit
) {
    val isEditing = initialBill != null
    var selectedType by remember { mutableStateOf(initialBill?.type ?: TransactionType.EXPENSE) }
    var title by remember { mutableStateOf(initialBill?.title.orEmpty()) }
    var amountText by remember { mutableStateOf(initialBill?.amount?.toString().orEmpty()) }
    var dueDayText by remember { mutableStateOf(initialBill?.dueDay?.toString().orEmpty()) }
    var selectedAccount by remember {
        mutableStateOf(initialBill?.accountName ?: accountList.firstOrNull().orEmpty())
    }
    var selectedToAccount by remember { mutableStateOf(initialBill?.toAccountName) }

    val availableCategories = remember(masterCategories, selectedType) {
        masterCategories.filter { it.type == selectedType }.map { it.name }
    }
    var selectedCategory by remember(availableCategories) {
        mutableStateOf(initialBill?.category ?: availableCategories.firstOrNull().orEmpty())
    }

    val availableSubcategories = remember(masterSubcategories, selectedCategory) {
        masterSubcategories.filter { it.parentCategory == selectedCategory }.map { it.name }
    }
    var selectedSubcategory by remember(availableSubcategories) {
        mutableStateOf(initialBill?.subcategory ?: availableSubcategories.firstOrNull().orEmpty())
    }

    var showNewCatDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }
    var showNewSubDialog by remember { mutableStateOf(false) }
    var newSubName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardWhite,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEditing) "Edit AutoPay Template" else "Add Recurring AutoPay",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Type Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BorderLight.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    listOf(
                        TransactionType.EXPENSE to "Expense",
                        TransactionType.INCOME to "Income",
                        TransactionType.ASSET to "Asset / SIP"
                    ).forEach { (type, label) ->
                        val isSelected = selectedType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CardWhite else Color.Transparent)
                                .clickable {
                                    selectedType = type
                                    val cats = masterCategories.filter { it.type == type }.map { it.name }
                                    selectedCategory = cats.firstOrNull().orEmpty()
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                color = if (isSelected) AccentPurple else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Commitment Name (e.g. Rent, SIP)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = dueDayText,
                        onValueChange = { dueDayText = it },
                        label = { Text("Due Day (1-31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    TextButton(
                        onClick = { showNewCatDialog = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("New", fontSize = 11.sp)
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableCategories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subcategory selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Subcategory", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    if (selectedCategory.isNotBlank()) {
                        TextButton(
                            onClick = { showNewSubDialog = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("New", fontSize = 11.sp)
                        }
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(availableSubcategories) { sub ->
                        FilterChip(
                            selected = selectedSubcategory == sub,
                            onClick = { selectedSubcategory = sub },
                            label = { Text(sub, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Account Selection
                Text("Deduction Vault", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(accountList) { acc ->
                        FilterChip(
                            selected = selectedAccount == acc,
                            onClick = { selectedAccount = acc },
                            label = { Text(acc, fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextDark)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            val due = dueDayText.toIntOrNull()?.coerceIn(1, 31)
                            if (title.isNotBlank() && amt > 0.0) {
                                onSave(
                                    title.trim(),
                                    amt,
                                    selectedCategory.ifBlank { "General" },
                                    selectedSubcategory.ifBlank { "General" },
                                    selectedAccount.ifBlank { accountList.firstOrNull().orEmpty() },
                                    selectedToAccount,
                                    selectedType,
                                    due
                                )
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save AutoPay", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showNewCatDialog) {
        AlertDialog(
            onDismissRequest = { showNewCatDialog = false },
            title = { Text("Add Category", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Category Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newCatName.isNotBlank()) {
                        onAddNewCategory(newCatName.trim(), selectedType)
                        selectedCategory = newCatName.trim()
                        newCatName = ""
                        showNewCatDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showNewCatDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showNewSubDialog) {
        AlertDialog(
            onDismissRequest = { showNewSubDialog = false },
            title = { Text("Add Subcategory to $selectedCategory", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newSubName,
                    onValueChange = { newSubName = it },
                    label = { Text("Subcategory Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newSubName.isNotBlank()) {
                        onAddNewSubcategory(selectedCategory, newSubName.trim(), selectedType)
                        selectedSubcategory = newSubName.trim()
                        newSubName = ""
                        showNewSubDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showNewSubDialog = false }) { Text("Cancel") }
            }
        )
    }
}
