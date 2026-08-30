package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.CategoryEntity
import com.example.myfin.data.SubcategoryEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.AppBottomDock
import com.example.myfin.ui.components.DockFabAction
import com.example.myfin.ui.components.NavigationTarget
import com.example.myfin.ui.components.rememberAutoScrollVisibilityConnection
import com.example.myfin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MasterDataSetScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToVaults: () -> Unit = {},
    onNavigateToMonthly: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()

    var selectedSegment by remember { mutableStateOf(TransactionType.EXPENSE) }
    var searchQuery by remember { mutableStateOf("") }

    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()

    // Bottom Sheets & Dialog States
    var showAddCategorySheet by remember { mutableStateOf(false) }
    var showAddSubcategorySheet by remember { mutableStateOf(false) }
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

    // Taxonomy Metrics
    val totalCats = segmentCategories.size
    val totalSubs = segmentSubcategories.size
    val protectedCount = segmentCategories.count { viewModel.protectedCategories.contains(it.name) }
    val customCount = (totalCats - protectedCount).coerceAtLeast(0)

    val segmentColor = when (selectedSegment) {
        TransactionType.EXPENSE -> SoftRed
        TransactionType.INCOME -> SoftGreen
        TransactionType.ASSET -> SoftTeal
        TransactionType.TRANSFER -> AccentPurple
    }

    val fabActions = remember {
        listOf(
            DockFabAction(
                icon = Icons.Default.Category,
                label = "Add Category",
                onClick = { showAddCategorySheet = true }
            ),
            DockFabAction(
                icon = Icons.Default.SubdirectoryArrowRight,
                label = "Add Subcategory",
                onClick = { showAddSubcategorySheet = true }
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
            .nestedScroll(scrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================
            // 1. PINNED TOP HEADER WITH SHELF DISSOLVE
            // =========================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasLight)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 6.dp, bottom = 8.dp)
                ) {
                    // Top App Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Drawer",
                                tint = TextDark,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Text(
                            text = "Master Taxonomy",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = TextDark
                        )

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "$totalCats Groups",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3-Pillar Taxonomy Metric Card
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = CardWhite,
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.18f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFFCFAFF),
                                            AccentPurple.copy(alpha = 0.04f)
                                        )
                                    )
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CATEGORIES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "$totalCats Active",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = segmentColor
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(BorderLight.copy(alpha = 0.7f))
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SUBCATEGORIES",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "$totalSubs Mapped",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp)
                                    .background(BorderLight.copy(alpha = 0.7f))
                            )

                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "CUSTOM TAGS",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextMuted,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "$customCount Custom",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentPurple
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Flow Segment Switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(11.dp))
                            .background(BorderLight.copy(alpha = 0.5f))
                            .padding(2.5.dp)
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
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CardWhite else Color.Transparent)
                                    .clickable { selectedSegment = type }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (isSelected) color else TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search Input
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(13.dp),
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

                // Smooth Dissolve Shelf Placed Below Search Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CanvasLight,
                                    CanvasLight.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }

            // =========================================================
            // 2. SCROLLABLE CATEGORY & SUBCATEGORY TREE
            // =========================================================
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 125.dp)
            ) {
                if (filteredCategories.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No matching taxonomy groups found", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
                    items(filteredCategories, key = { "${it.type}_${it.name}" }) { cat ->
                        val isProtected = viewModel.protectedCategories.contains(cat.name)
                        val subList = segmentSubcategories.filter { it.parentCategory == cat.name }
                        val isExpanded = expandedCategories[cat.name] ?: false

                        IntegratedCategoryTreeCard(
                            category = cat,
                            subcategories = subList,
                            isProtected = isProtected,
                            isExpanded = isExpanded,
                            onToggleExpand = { expandedCategories[cat.name] = !isExpanded },
                            onSwipeEditCategory = { categoryToEdit = cat },
                            onSwipeDeleteCategory = {
                                if (isProtected) {
                                    alertNoticeMessage = "'${cat.name}' is a core protected category and cannot be deleted."
                                } else {
                                    categoryToDelete = cat
                                }
                            },
                            onSwipeEditSubcategory = { sub -> subcategoryToEdit = sub },
                            onSwipeDeleteSubcategory = { sub -> subcategoryToDelete = sub }
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // =========================================================
        // 3. BOTTOM GRADIENT SCRIM (DISSOLVES CONTENT BEFORE DOCK)
        // =========================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CanvasLight.copy(alpha = 0.85f),
                            CanvasLight
                        )
                    )
                )
                .zIndex(2.5f)
        )

        // =========================================================
        // 4. STANDARDIZED FLOATING BOTTOM DOCK WITH FAB
        // =========================================================
        AppBottomDock(
            currentSelection = NavigationTarget.DATA_SET,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.MONTHLY_VIEW -> onNavigateToMonthly()
                    NavigationTarget.BUDGET_PLANNER -> onNavigateToPlanner()
                    NavigationTarget.VAULT_ACCOUNTS -> onNavigateToVaults()
                    NavigationTarget.REPORTS_ANALYTICS -> onNavigateToAnalytics()
                    NavigationTarget.DATA_SET -> { /* Active */ }
                    else -> {}
                }
            },
            fabActions = fabActions,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        )

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
                            val trimmedName = newCategoryName.trim()
                            if (trimmedName.isNotBlank()) {
                                val alreadyExists = segmentCategories.any { it.name.equals(trimmedName, ignoreCase = true) }
                                if (alreadyExists) {
                                    Toast.makeText(context, "Category '$trimmedName' already exists", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addCategory(trimmedName, selectedSegment)
                                    showAddCategorySheet = false
                                }
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
            var selectedParent by remember(selectedSegment, segmentCategories) {
                mutableStateOf(segmentCategories.firstOrNull()?.name.orEmpty())
            }
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

                    Text("Select Parent Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                        items(segmentCategories) { cat ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                                    .clickable { selectedParent = cat.name },
                                shape = RoundedCornerShape(8.dp),
                                color = if (selectedParent == cat.name) AccentPurple.copy(alpha = 0.12f) else CanvasLight,
                                border = BorderStroke(0.6.dp, if (selectedParent == cat.name) AccentPurple else BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = cat.name,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selectedParent == cat.name) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selectedParent == cat.name) AccentPurple else TextDark
                                    )
                                }
                            }
                        }
                    }

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
                            val trimmedSub = newSubName.trim()
                            if (trimmedSub.isNotBlank() && selectedParent.isNotBlank()) {
                                val alreadyExists = segmentSubcategories.any {
                                    it.parentCategory == selectedParent && it.name.equals(trimmedSub, ignoreCase = true)
                                }
                                if (alreadyExists) {
                                    Toast.makeText(context, "Subcategory '$trimmedSub' already exists under $selectedParent", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.addSubcategory(selectedParent, trimmedSub, selectedSegment)
                                    showAddSubcategorySheet = false
                                }
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
                            val trimmedNew = renameText.trim()
                            if (trimmedNew.isNotBlank() && trimmedNew != cat.name) {
                                viewModel.updateCategory(cat, trimmedNew)
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
                            val trimmedNew = renameText.trim()
                            if (trimmedNew.isNotBlank() && trimmedNew != sub.name) {
                                viewModel.updateSubcategory(sub, trimmedNew)
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
private fun IntegratedCategoryTreeCard(
    category: CategoryEntity,
    subcategories: List<SubcategoryEntity>,
    isProtected: Boolean,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
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

    val typeColor = when (category.type) {
        TransactionType.INCOME -> SoftGreen
        TransactionType.EXPENSE -> SoftRed
        TransactionType.ASSET -> SoftTeal
        TransactionType.TRANSFER -> AccentPurple
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
    ) {
        Column {
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
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(typeColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = category.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = typeColor
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = category.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isProtected) {
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Icon(
                                            Icons.Default.Lock,
                                            contentDescription = "System Core",
                                            tint = TextMuted.copy(alpha = 0.7f),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${subcategories.size} subcategories mapped",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Icon(
                            Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(arrowRotation)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasLight.copy(alpha = 0.6f))
                ) {
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.8.dp)

                    if (subcategories.isEmpty()) {
                        Text(
                            text = "No subcategories configured",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                        )
                    } else {
                        subcategories.forEachIndexed { index, sub ->
                            IntegratedSubcategoryRow(
                                subcategory = sub,
                                onSwipeEdit = { onSwipeEditSubcategory(sub) },
                                onSwipeDelete = { onSwipeDeleteSubcategory(sub) }
                            )

                            if (index < subcategories.lastIndex) {
                                HorizontalDivider(
                                    color = BorderLight.copy(alpha = 0.4f),
                                    thickness = 0.6.dp,
                                    modifier = Modifier.padding(start = 48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegratedSubcategoryRow(
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
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(15.dp))
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(15.dp))
                }
            }
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardWhite)
                .padding(horizontal = 20.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(TextMuted)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = subcategory.name,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
