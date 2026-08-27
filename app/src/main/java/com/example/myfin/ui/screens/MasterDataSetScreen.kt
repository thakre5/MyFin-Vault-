package com.example.myfin.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.CategoryWithSubcategories
import com.example.myfin.data.SubcategoryEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaxonomyScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToVaults: () -> Unit = {}
) {
    val uiState by viewModel.monthlyUiState.collectAsState()
    var selectedTab by remember { mutableStateOf(TransactionType.EXPENSE) }
    var searchQuery by remember { mutableStateOf("") }
    var expandedCategoryId by remember { mutableStateOf<Long?>(null) }
    var subcategoryTargetCatId by remember { mutableStateOf<Long?>(null) }

    val categoriesList = remember(uiState.categoriesWithSubs, selectedTab, searchQuery) {
        uiState.categoriesWithSubs
            .filter { it.category.type == selectedTab }
            .filter {
                if (searchQuery.isBlank()) true
                else it.category.name.contains(searchQuery, ignoreCase = true) ||
                     it.subcategories.any { sub -> sub.name.contains(searchQuery, ignoreCase = true) }
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
                    .padding(horizontal = 20.dp, vertical = 6.dp),
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
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Drawer", tint = TextDark, modifier = Modifier.size(22.dp))
                }

                Text(
                    text = "Categories",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = TextDark,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Surface(
                    shape = CircleShape,
                    color = AccentPurple,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("S", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }

            // Type Segment Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BorderLight.copy(alpha = 0.45f))
                    .padding(2.5.dp)
            ) {
                listOf(
                    TransactionType.EXPENSE to "Expenses",
                    TransactionType.INCOME to "Income",
                    TransactionType.TRANSFER to "Assets / SIP"
                ).forEach { (type, title) ->
                    val isSel = selectedTab == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (isSel) CardWhite else Color.Transparent)
                            .clickable { selectedTab = type }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp,
                            color = if (isSel) when (type) {
                                TransactionType.EXPENSE -> SoftRed
                                TransactionType.INCOME -> SoftGreen
                                TransactionType.TRANSFER -> AccentPurple
                            } else TextMuted
                        )
                    }
                }
            }

            // Search Bar Filter
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filter categories & subcategories...", fontSize = 12.sp, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp)) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite,
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight.copy(alpha = 0.6f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )

            // Category Cards List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 6.dp, bottom = 105.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(categoriesList, key = { it.category.id }) { catWithSubs ->
                    val isExpanded = expandedCategoryId == catWithSubs.category.id
                    val txCount = uiState.groupedTransactions.values.flatten().count { it.category.equals(catWithSubs.category.name, ignoreCase = true) }

                    Surface(
                        modifier = Modifier.fillMaxWidth().shadow(1.5.dp, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            // Fixed Flex Header Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when (catWithSubs.category.type) {
                                                TransactionType.EXPENSE -> SoftRed.copy(alpha = 0.12f)
                                                TransactionType.INCOME -> SoftGreen.copy(alpha = 0.12f)
                                                TransactionType.TRANSFER -> AccentPurple.copy(alpha = 0.12f)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            catWithSubs.category.name.contains("Health", true) -> Icons.Default.FitnessCenter
                                            catWithSubs.category.name.contains("Leisure", true) || catWithSubs.category.name.contains("Trip", true) -> Icons.Default.Flight
                                            catWithSubs.category.name.contains("General", true) -> Icons.Default.Category
                                            else -> Icons.Default.AccountBalanceWallet
                                        },
                                        contentDescription = null,
                                        tint = when (catWithSubs.category.type) {
                                            TransactionType.EXPENSE -> SoftRed
                                            TransactionType.INCOME -> SoftGreen
                                            TransactionType.TRANSFER -> AccentPurple
                                        },
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Category Name + Uncompressed System Badge
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = catWithSubs.category.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    if (catWithSubs.category.isSystem) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = CanvasLight,
                                            border = BorderStroke(0.6.dp, BorderLight)
                                        ) {
                                            Text(
                                                text = "System",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextMuted,
                                                maxLines = 1,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Fixed Width Right Actions
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = AccentPurple.copy(alpha = 0.12f),
                                        modifier = Modifier.clickable { subcategoryTargetCatId = catWithSubs.category.id }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Add Sub", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { expandedCategoryId = if (isExpanded) null else catWithSubs.category.id },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${catWithSubs.subcategories.size} items • $txCount Transactions",
                                fontSize = 11.sp,
                                color = TextMuted,
                                modifier = Modifier.padding(start = 50.dp)
                            )

                            // Expanded Subcategory Drawer
                            AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 10.dp, start = 12.dp)) {
                                    HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (catWithSubs.subcategories.isEmpty()) {
                                        Text("No subcategories added yet", fontSize = 11.sp, color = TextMuted, modifier = Modifier.padding(vertical = 4.dp))
                                    } else {
                                        catWithSubs.subcategories.forEach { sub ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(TextMuted))
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(sub.name, fontSize = 12.sp, color = TextDark, fontWeight = FontWeight.Medium)
                                                }
                                                IconButton(
                                                    onClick = { viewModel.deleteSubcategory(sub) },
                                                    modifier = Modifier.size(22.dp)
                                                ) {
                                                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(13.dp))
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

        // Add Subcategory Bottom Sheet
        subcategoryTargetCatId?.let { catId ->
            var subName by remember { mutableStateOf("") }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { subcategoryTargetCatId = null },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                ) {
                    Text("Add Subcategory", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = subName,
                        onValueChange = { subName = it },
                        label = { Text("Subcategory Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (subName.isNotBlank()) {
                                viewModel.addSubcategory(catId, subName.trim())
                                subcategoryTargetCatId = null
                            }
                        },
                        enabled = subName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Add Subcategory", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
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
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
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
                        onClick = onNavigateToDashboard
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = { },
                containerColor = TextDark,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).shadow(16.dp, CircleShape)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Actions", modifier = Modifier.size(28.dp))
            }
        }
    }
}
