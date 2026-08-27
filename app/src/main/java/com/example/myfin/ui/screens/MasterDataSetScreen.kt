package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.CategoryEntity
import com.example.myfin.data.SubcategoryEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataSetScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToVaults: () -> Unit = {},
    onNavigateToMonthly: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var selectedSegment by remember { mutableStateOf(TransactionType.EXPENSE) }
    var searchQuery by remember { mutableStateOf("") }
    var showActionMenu by remember { mutableStateOf(false) }

    // Bottom Sheets & Dialog States
    var showAddCategorySheet by remember { mutableStateOf(false) }
    var showAddSubcategorySheet by remember { mutableStateOf(false) }
    var preselectedParentCategory by remember { mutableStateOf<String?>(null) }

    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var subcategoryToEdit by remember { mutableStateOf<SubcategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }
    var subcategoryToDelete by remember { mutableStateOf<SubcategoryEntity?>(null) }
    var alertNoticeMessage by remember { mutableStateOf<String?>(null) }

    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    val segmentCategories = remember(uiState.masterCategories, selectedSegment) {
        uiState.masterCategories.filter { it.type == selectedSegment }
    }

    val segmentSubcategories = remember(uiState.masterSubcategories, selectedSegment) {
        uiState.masterSubcategories.filter { it.type == selectedSegment }
    }

    // Transaction counts map
    val transactionCountsByCategory = remember(uiState.groupedTransactions) {
        val allTx = uiState.groupedTransactions.values.flatten()
        allTx.groupingBy { it.category }.eachCount()
    }

    val filteredCategories = remember(segmentCategories, segmentSubcategories, searchQuery) {
        if (searchQuery.isBlank()) {
            segmentCategories
        } else {
            val matchingSubParents = segmentSubcategories
                .filter { it.name.contains(searchQuery, ignoreCase = true) }
                .map { it.parentCategory }
                .toSet()

            segmentCategories.filter {
                it.name.contains(searchQuery, ignoreCase = true) || matchingSubParents.contains(it.name)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(CanvasLight)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight.copy(alpha = 0.7f), RoundedCornerShape(11.dp))
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Drawer",
                        tint = TextDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Categories",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = TextDark
                )

                // User Avatar
                Surface(
                    shape = CircleShape,
                    color = AccentPurple,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = userProfile.displayName.take(1).uppercase().ifBlank { "S" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Pinned Search & Switcher Controls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Single-Line Flow Segment Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BorderLight.copy(alpha = 0.5f))
                        .padding(3.dp)
                ) {
                    listOf(
                        Triple(TransactionType.EXPENSE, "Expenses", SoftRed),
                        Triple(TransactionType.INCOME, "Income", SoftGreen),
                        Triple(TransactionType.ASSET, "Assets / SIP", SoftTeal)
                    ).forEach { (type, label, color) ->
                        val isSelected = selectedSegment == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isSelected) CardWhite else Color.Transparent)
                                .clickable { selectedSegment = type }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.5.sp,
                                color = if (isSelected) color else TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Compact Search Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(8.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text("Filter categories & subcategories...", color = TextMuted, fontSize = 12.5.sp, maxLines = 1)
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 12.5.sp, color = TextDark, fontWeight = FontWeight.Medium),
                                cursorBrush = SolidColor(AccentPurple),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMuted, modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scrollable Category & Subcategory List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 2.dp, bottom = 105.dp)
            ) {
                if (filteredCategories.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = CardWhite
                        ) {
                            Box(modifier = Modifier.padding(28.dp), contentAlignment = Alignment.Center) {
                                Text("No matching categories found", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
                    items(filteredCategories, key = { "${it.type}_${it.name}" }) { cat ->
                        val isProtected = viewModel.protectedCategories.contains(cat.name)
                        val subList = segmentSubcategories.filter { it.parentCategory == cat.name }
                        val isExpanded = expandedCategories[cat.name] ?: false
                        val txCount = transactionCountsByCategory[cat.name] ?: 0

                        StyledCategoryCard(
                            category = cat,
                            subcategories = subList,
                            transactionCount = txCount,
                            isProtected = isProtected,
                            isExpanded = isExpanded,
                            onToggleExpand = { expandedCategories[cat.name] = !isExpanded },
                            onAddSubcategory = {
                                preselectedParentCategory = cat.name
                                showAddSubcategorySheet = true
                            },
                            onSwipeEditCategory = { categoryToEdit = cat },
                            onSwipeDeleteCategory = {
                                if (isProtected) {
                                    alertNoticeMessage = "'${cat.name}' is a core system category and cannot be deleted."
                                } else {
                                    categoryToDelete = cat
                                }
                            },
                            onSwipeEditSubcategory = { sub -> subcategoryToEdit = sub },
                            onSwipeDeleteSubcategory = { sub -> subcategoryToDelete = sub }
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        // 4 + 1 Floating Bottom Navigation Dock
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shadow(16.dp, CircleShape),
                shape = CircleShape,
                color = CardWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockPillTab(
                        title = "Taxonomy",
                        icon = Icons.Default.Category,
                        isSelected = true,
                        onClick = { }
                    )
                    DockPillTab(
                        title = "Planner",
                        icon = Icons.Default.PieChart,
                        isSelected = false,
                        onClick = onNavigateToPlanner
                    )
                    DockPillTab(
                        title = "Vaults",
                        icon = Icons.Default.AccountBalanceWallet,
                        isSelected = false,
                        onClick = onNavigateToVaults
                    )
                    DockPillTab(
                        title = "Monthly",
                        icon = Icons.Default.Assessment,
                        isSelected = false,
                        onClick = onNavigateToMonthly
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = { showActionMenu = !showActionMenu },
                containerColor = TextDark,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).shadow(16.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Create",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(if (showActionMenu) 45f else 0f)
                )
            }
        }

        // Anchored Action Menu
        if (showActionMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showActionMenu = false }
                    )
            )

            AnimatedVisibility(
                visible = showActionMenu,
                enter = scaleIn(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(180)
                ) + fadeIn(animationSpec = tween(180)),
                exit = scaleOut(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(150)
                ) + fadeOut(animationSpec = tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 94.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    shadowElevation = 10.dp,
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                    modifier = Modifier.width(190.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    showAddCategorySheet = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Category, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Add Category", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        }

                        HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    preselectedParentCategory = segmentCategories.firstOrNull()?.name.orEmpty()
                                    showAddSubcategorySheet = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SubdirectoryArrowRight, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Add Subcategory", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        }
                    }
                }
            }
        }

        // Alert: Protected Category Notice
        alertNoticeMessage?.let { msg ->
            AlertDialog(
                onDismissRequest = { alertNoticeMessage = null },
                title = { Text("Protected Category", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = { Text(msg, fontSize = 13.sp, color = TextDark) },
                confirmButton = {
                    TextButton(onClick = { alertNoticeMessage = null }) {
                        Text("Understood", color = AccentPurple, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Alert: Delete Category Confirmation
        categoryToDelete?.let { cat ->
            AlertDialog(
                onDismissRequest = { categoryToDelete = null },
                title = { Text("Delete Category '${cat.name}'?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        "Deleting this category will remove its subcategories. Historical transactions will be reassigned to 'General' to protect your balances, and future unpaid AutoPay bills will be removed.",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteCategory(cat) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            categoryToDelete = null
                        }
                    ) {
                        Text("Delete", color = SoftRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { categoryToDelete = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        // Alert: Delete Subcategory Confirmation
        subcategoryToDelete?.let { sub ->
            AlertDialog(
                onDismissRequest = { subcategoryToDelete = null },
                title = { Text("Delete Subcategory '${sub.name}'?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        "Are you sure you want to remove '${sub.name}' from ${sub.parentCategory}? Future unpaid AutoPay commitments linked to it will be cleared.",
                        fontSize = 13.sp,
                        color = TextDark
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteSubcategory(sub)
                            subcategoryToDelete = null
                        }
                    ) {
                        Text("Delete", color = SoftRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { subcategoryToDelete = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        // Sheet: Add Category
        if (showAddCategorySheet) {
            var newCategoryName by remember { mutableStateOf("") }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showAddCategorySheet = false },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp),
                        shape = CircleShape,
                        color = BorderLight
                    ) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("Add Master Category", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Category will be created under ${selectedSegment.name}", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("Category Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = BorderLight
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                viewModel.addCategory(newCategoryName.trim(), selectedSegment)
                                showAddCategorySheet = false
                            }
                        },
                        enabled = newCategoryName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Create Category", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Sheet: Add Subcategory
        if (showAddSubcategorySheet) {
            var selectedParent by remember { mutableStateOf(preselectedParentCategory ?: segmentCategories.firstOrNull()?.name.orEmpty()) }
            var newSubName by remember { mutableStateOf("") }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showAddSubcategorySheet = false },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp),
                        shape = CircleShape,
                        color = BorderLight
                    ) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("Add Subcategory Tag", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Attach subcategory to parent classification", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Parent Category: $selectedParent", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextDark)

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newSubName,
                        onValueChange = { newSubName = it },
                        label = { Text("Subcategory Tag Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = BorderLight
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (newSubName.isNotBlank() && selectedParent.isNotBlank()) {
                                viewModel.addSubcategory(selectedParent, newSubName.trim(), selectedSegment)
                                showAddSubcategorySheet = false
                            }
                        },
                        enabled = newSubName.isNotBlank() && selectedParent.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save Subcategory", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Sheet: Rename Category
        categoryToEdit?.let { cat ->
            var renameText by remember { mutableStateOf(cat.name) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { categoryToEdit = null },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp),
                        shape = CircleShape,
                        color = BorderLight
                    ) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("Rename Category", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("All historical transactions, plans, and bills will cascade automatically.", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Category Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = BorderLight
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (renameText.isNotBlank() && renameText.trim() != cat.name) {
                                viewModel.updateCategory(cat, renameText.trim())
                            }
                            categoryToEdit = null
                        },
                        enabled = renameText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Apply Cascade Rename", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Sheet: Rename Subcategory
        subcategoryToEdit?.let { sub ->
            var renameText by remember { mutableStateOf(sub.name) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { subcategoryToEdit = null },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp),
                        shape = CircleShape,
                        color = BorderLight
                    ) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("Rename Subcategory", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Subcategory under '${sub.parentCategory}'", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        label = { Text("Subcategory Tag Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = BorderLight
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (renameText.isNotBlank() && renameText.trim() != sub.name) {
                                viewModel.updateSubcategory(sub, renameText.trim())
                            }
                            subcategoryToEdit = null
                        },
                        enabled = renameText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save Subcategory", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyledCategoryCard(
    category: CategoryEntity,
    subcategories: List<SubcategoryEntity>,
    transactionCount: Int,
    isProtected: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAddSubcategory: () -> Unit,
    onSwipeEditCategory: () -> Unit,
    onSwipeDeleteCategory: () -> Unit,
    onSwipeEditSubcategory: (SubcategoryEntity) -> Unit,
    onSwipeDeleteSubcategory: (SubcategoryEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val currentOnEditCat by rememberUpdatedState(onSwipeEditCategory)
    val currentOnDeleteCat by rememberUpdatedState(onSwipeDeleteCategory)

    var lastTargetValue by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnEditCat()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnDeleteCat()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != lastTargetValue && dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastTargetValue = dismissState.targetValue
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "arrowRotation"
    )

    val (iconBg, iconColor, categoryIcon) = getThematicCategoryIcon(category.name, category.type)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
    ) {
        Column {
            // Parent Category Swipe Header
            SwipeToDismissBox(
                state = dismissState,
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true,
                backgroundContent = {
                    val direction = dismissState.dismissDirection
                    val backgroundColor by animateColorAsState(
                        targetValue = when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.StartToEnd -> AccentPurple
                            SwipeToDismissBoxValue.EndToStart -> SoftRed
                            SwipeToDismissBoxValue.Settled -> Color.Transparent
                        },
                        animationSpec = tween(200),
                        label = "catSwipeBgColor"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(backgroundColor)
                            .padding(horizontal = 20.dp),
                        contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                    ) {
                        if (direction == SwipeToDismissBoxValue.StartToEnd) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Rename", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Delete", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleExpand),
                    color = CardWhite
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Circular Pastel Thematic Icon
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = categoryIcon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(13.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = category.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isProtected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CanvasLight,
                                            border = BorderStroke(0.6.dp, BorderLight)
                                        ) {
                                            Text(
                                                text = "System",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextMuted,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${subcategories.size} items • $transactionCount Transactions",
                                    fontSize = 11.5.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Right Pill Button + Chevron
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(onClick = onAddSubcategory),
                                shape = RoundedCornerShape(12.dp),
                                color = AccentPurple.copy(alpha = 0.08f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Add Sub",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPurple
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(20.dp).rotate(arrowRotation)
                            )
                        }
                    }
                }
            }

            // Expanded Subcategory Nested Pill List
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 14.dp, top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (subcategories.isEmpty()) {
                        Text(
                            text = "No subcategories yet. Tap '+ Add Sub' to create one.",
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        subcategories.forEach { sub ->
                            StyledSubcategoryPill(
                                subcategory = sub,
                                onSwipeEdit = { onSwipeEditSubcategory(sub) },
                                onSwipeDelete = { onSwipeDeleteSubcategory(sub) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StyledSubcategoryPill(
    subcategory: SubcategoryEntity,
    onSwipeEdit: () -> Unit,
    onSwipeDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val currentOnEdit by rememberUpdatedState(onSwipeEdit)
    val currentOnDelete by rememberUpdatedState(onSwipeDelete)

    var lastTargetValue by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnDelete()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != lastTargetValue && dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastTargetValue = dismissState.targetValue
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val backgroundColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> AccentPurple
                    SwipeToDismissBoxValue.EndToStart -> SoftRed
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                animationSpec = tween(200),
                label = "subSwipeBgColor"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 14.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFF7F8FA),
            border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subcategory.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun getThematicCategoryIcon(categoryName: String, type: TransactionType): Triple<Color, Color, ImageVector> {
    return when {
        categoryName.contains("Debt", ignoreCase = true) -> Triple(Color(0xFFFFEEDB), Color(0xFFE57A28), Icons.Default.CreditCard)
        categoryName.contains("Utilities", ignoreCase = true) -> Triple(Color(0xFFFFF7D6), Color(0xFFD49E00), Icons.Default.Lightbulb)
        categoryName.contains("Everyday", ignoreCase = true) || categoryName.contains("Living", ignoreCase = true) -> Triple(Color(0xFFFFF0D4), Color(0xFFE07E10), Icons.Default.ShoppingBag)
        categoryName.contains("Health", ignoreCase = true) || categoryName.contains("Medical", ignoreCase = true) -> Triple(Color(0xFFFFE4EC), Color(0xFFE0407B), Icons.Default.FitnessCenter)
        categoryName.contains("Family", ignoreCase = true) || categoryName.contains("Home", ignoreCase = true) -> Triple(Color(0xFFFFE8E8), Color(0xFFE04848), Icons.Default.Home)
        categoryName.contains("Work", ignoreCase = true) || categoryName.contains("Professional", ignoreCase = true) || categoryName.contains("Company", ignoreCase = true) -> Triple(Color(0xFFE3F5FF), Color(0xFF0288D1), Icons.Default.Work)
        categoryName.contains("Leisure", ignoreCase = true) || categoryName.contains("Trips", ignoreCase = true) -> Triple(Color(0xFFF0EBFF), Color(0xFF7C4DFF), Icons.Default.FlightTakeoff)
        type == TransactionType.INCOME -> Triple(Color(0xFFE6F8EF), Color(0xFF00A86B), Icons.AutoMirrored.Filled.TrendingUp)
        type == TransactionType.ASSET -> Triple(Color(0xFFE0F7FA), Color(0xFF00897B), Icons.Default.Savings)
        else -> Triple(Color(0xFFECEFF1), Color(0xFF607D8B), Icons.Default.Category)
    }
}

@Composable
private fun DockPillTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) CanvasLight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) AccentPurple else TextMuted,
                modifier = Modifier.size(17.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = AccentPurple)
            }
        }
    }
}
