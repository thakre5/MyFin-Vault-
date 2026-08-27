package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.CategoryEntity
import com.example.myfin.data.SubcategoryEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.*

@Composable
fun MasterDataSetScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()

    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var subcategoryToEdit by remember { mutableStateOf<SubcategoryEntity?>(null) }
    var subcategoryToDelete by remember { mutableStateOf<SubcategoryEntity?>(null) }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showAddSubDialogForParent by remember { mutableStateOf<String?>(null) }

    val categories = uiState.masterCategories.filter { it.type == selectedType }
    val subcategories = uiState.masterSubcategories.filter { it.type == selectedType }

    val bottomNavPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CardWhite)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu", tint = TextDark)
                }

                Text("Taxonomy Manager", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)

                IconButton(
                    onClick = { showAddCategoryDialog = true },
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CardWhite)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Category", tint = AccentPurple)
                }
            }

            // Type Segment Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BorderLight.copy(alpha = 0.5f))
                    .padding(4.dp)
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
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSelected) CardWhite else Color.Transparent)
                            .clickable { selectedType = type }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            color = if (isSelected) AccentPurple else TextMuted
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp + bottomNavPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(categories, key = { it.name }) { cat ->
                    val childSubs = subcategories.filter { it.parentCategory == cat.name }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = CardWhite
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cat.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextDark
                                    )
                                    if (viewModel.protectedCategories.contains(cat.name)) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CanvasLight
                                        ) {
                                            Text(
                                                text = "System",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextMuted,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { showAddSubDialogForParent = cat.name },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add Subcategory", tint = AccentPurple, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { categoryToEdit = cat },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                    if (!viewModel.protectedCategories.contains(cat.name)) {
                                        IconButton(
                                            onClick = { categoryToDelete = cat },
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SoftRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }

                            if (childSubs.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                childSubs.forEach { sub ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp, horizontal = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "• ${sub.name}",
                                            fontSize = 12.5.sp,
                                            color = TextDark,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row {
                                            IconButton(
                                                onClick = { subcategoryToEdit = sub },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Edit Subcategory", tint = TextMuted, modifier = Modifier.size(14.dp))
                                            }
                                            if (sub.name != "General") {
                                                IconButton(
                                                    onClick = { subcategoryToDelete = sub },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Delete Subcategory", tint = SoftRed, modifier = Modifier.size(14.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Category Dialog
        if (showAddCategoryDialog) {
            var newName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddCategoryDialog = false },
                title = { Text("Add ${selectedType.name} Category", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Category Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addCategory(newName.trim(), selectedType)
                            showAddCategoryDialog = false
                        }
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Add Subcategory Dialog
        showAddSubDialogForParent?.let { parentName ->
            var subName by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showAddSubDialogForParent = null },
                title = { Text("Add Subcategory to $parentName", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Subcategory Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (subName.isNotBlank()) {
                            viewModel.addSubcategory(parentName, subName.trim(), selectedType)
                            showAddSubDialogForParent = null
                        }
                    }) { Text("Add") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSubDialogForParent = null }) { Text("Cancel") }
                }
            )
        }

        // Edit Category Dialog
        categoryToEdit?.let { cat ->
            var updatedName by remember { mutableStateOf(cat.name) }
            AlertDialog(
                onDismissRequest = { categoryToEdit = null },
                title = { Text("Rename Category", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = updatedName,
                        onValueChange = { updatedName = it },
                        label = { Text("New Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (updatedName.isNotBlank() && updatedName != cat.name) {
                            viewModel.updateCategory(cat, updatedName.trim())
                            categoryToEdit = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToEdit = null }) { Text("Cancel") }
                }
            )
        }

        // Delete Category Confirmation
        categoryToDelete?.let { cat ->
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = { Text("Delete '${cat.name}'?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("Deleting this category will safely reassign historical transactions to 'General'. Future unpaid AutoPay commitments for this category will be removed.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCategory(cat) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                            categoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) { Text("Cancel") }
                }
            )
        }

        // Edit Subcategory Dialog
        subcategoryToEdit?.let { sub ->
            var updatedSubName by remember { mutableStateOf(sub.name) }
            AlertDialog(
                onDismissRequest = { subcategoryToEdit = null },
                title = { Text("Rename Subcategory", fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = updatedSubName,
                        onValueChange = { updatedSubName = it },
                        label = { Text("New Name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        if (updatedSubName.isNotBlank() && updatedSubName != sub.name) {
                            viewModel.updateSubcategory(sub, updatedSubName.trim())
                            subcategoryToEdit = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { subcategoryToEdit = null }) { Text("Cancel") }
                }
            )
        }

        // Delete Subcategory Dialog
        subcategoryToDelete?.let { sub ->
            AlertDialog(
                onDismissRequest = { subcategoryToDelete = null },
                title = { Text("Delete Subcategory '${sub.name}'?", fontWeight = FontWeight.Bold) },
                text = { Text("Historical transactions under this subcategory will remain intact.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSubcategory(sub)
                            subcategoryToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { subcategoryToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}
