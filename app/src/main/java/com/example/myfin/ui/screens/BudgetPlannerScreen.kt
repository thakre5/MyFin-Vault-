package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myfin.data.CategoryEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.CategoryPerformance
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPlannerScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var selectedSegment by remember { mutableStateOf(TransactionType.EXPENSE) }
    var editingCategory by remember { mutableStateOf<CategoryPerformance?>(null) }
    var showCopyConfirmDialog by remember { mutableStateOf(false) }

    val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    // Financial Allocation Calculations
    val totalPlannedIncome = uiState.metrics.plannedIncome
    val totalPlannedExpenses = uiState.metrics.plannedExpenses
    val totalPlannedAssets = uiState.metrics.plannedAssets
    val totalAllocated = totalPlannedExpenses + totalPlannedAssets
    val unallocatedBuffer = totalPlannedIncome - totalAllocated
    val allocationPercentage = if (totalPlannedIncome > 0) {
        ((totalAllocated / totalPlannedIncome) * 100).toInt()
    } else 0
    val isOverAllocated = unallocatedBuffer < 0

    Box(modifier = Modifier.fillMaxSize().background(CanvasLight)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 95.dp, bottom = 100.dp)
        ) {
            // Hero Allocation Capacity Card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(26.dp)),
                    shape = RoundedCornerShape(26.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFFFFFFFF),
                                        Color(0xFFFCFBFE),
                                        Color(0xFFF6F4FD)
                                    )
                                )
                            )
                            .padding(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL MONTHLY INFLOW BASELINE",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Black,
                                color = TextMuted,
                                letterSpacing = 0.7.sp
                            )

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isOverAllocated) SoftRed.copy(alpha = 0.12f) else SoftGreen.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = if (isOverAllocated) "Over-allocated ($allocationPercentage%)" else "$allocationPercentage% Allocated",
                                    color = if (isOverAllocated) SoftRed else SoftGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalPlannedIncome)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            letterSpacing = (-0.5).sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (isOverAllocated) {
                                "Deficit: Planned limits exceed income by ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(unallocatedBuffer))}"
                            } else {
                                "Unallocated buffer: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", unallocatedBuffer)} left to assign"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isOverAllocated) SoftRed else TextMuted
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Multi-Segment Allocation Progress Bar
                        val expenseFraction = if (totalPlannedIncome > 0) (totalPlannedExpenses / totalPlannedIncome).toFloat().coerceIn(0f, 1f) else 0f
                        val assetFraction = if (totalPlannedIncome > 0) (totalPlannedAssets / totalPlannedIncome).toFloat().coerceIn(0f, 1f) else 0f

                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(9.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(BorderLight.copy(alpha = 0.6f))
                            ) {
                                if (expenseFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(expenseFraction.coerceAtLeast(0.001f))
                                            .fillMaxHeight()
                                            .background(SoftRed)
                                    )
                                }
                                if (assetFraction > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(assetFraction.coerceAtLeast(0.001f))
                                            .fillMaxHeight()
                                            .background(SoftTeal)
                                    )
                                }
                                val remainder = (1f - (expenseFraction + assetFraction)).coerceAtLeast(0f)
                                if (remainder > 0f) {
                                    Box(
                                        modifier = Modifier
                                            .weight(remainder)
                                            .fillMaxHeight()
                                            .background(Color.Transparent)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Allocation Legends
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftRed))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Expenses: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalPlannedExpenses)}", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Assets/SIP: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalPlannedAssets)}", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Segment Tabs (Expense, Income, Assets)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(BorderLight.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    listOf(
                        Triple(TransactionType.EXPENSE, "Expense Limits", SoftRed),
                        Triple(TransactionType.INCOME, "Income Targets", SoftGreen),
                        Triple(TransactionType.ASSET, "SIP / Wealth Targets", SoftTeal)
                    ).forEach { (type, label, color) ->
                        val isSelected = selectedSegment == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) CardWhite else Color.Transparent)
                                .clickable { selectedSegment = type }
                                .padding(vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.5.sp,
                                color = if (isSelected) color else TextMuted
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            val currentCategories = uiState.categories.filter { it.type == selectedSegment }

            if (currentCategories.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite
                    ) {
                        Box(modifier = Modifier.padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("No categories in this segment", fontSize = 12.5.sp, color = TextMuted)
                        }
                    }
                }
            } else {
                items(currentCategories, key = { it.category }) { cat ->
                    BudgetCategoryItemCard(
                        category = cat,
                        currencySymbol = userProfile.currencySymbol,
                        onIncrement = { increment ->
                            val updatedAmount = (cat.plannedAmount + increment).coerceAtLeast(0.0)
                            viewModel.updateCategoryBudget(cat.category, updatedAmount, cat.type)
                        },
                        onManualEdit = { editingCategory = cat }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Pinned Top Bar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            color = CanvasLight.copy(alpha = 0.96f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardWhite)
                            .border(0.8.dp, BorderLight.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Drawer",
                            tint = TextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Text(
                        text = "Budget Planner",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = TextDark
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = "${monthNames[uiState.selectedMonth - 1]} ${uiState.selectedYear}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Manual Edit Allocation Bottom Sheet
        editingCategory?.let { cat ->
            var customAmountText by remember { mutableStateOf(if (cat.plannedAmount > 0) cat.plannedAmount.toInt().toString() else "") }

            Dialog(onDismissRequest = { editingCategory = null }) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = CardWhite,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Set Budget: ${cat.category}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Configure monthly ceiling for this category",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = customAmountText,
                            onValueChange = { customAmountText = it },
                            label = { Text("Planned Amount (${userProfile.currencySymbol})") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = BorderLight
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick increment shortcut row inside modal
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(500, 1000, 5000).forEach { inc ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            val current = customAmountText.toDoubleOrNull() ?: 0.0
                                            customAmountText = (current + inc).toInt().toString()
                                        },
                                    color = CanvasLight,
                                    border = BorderStroke(0.6.dp, BorderLight)
                                ) {
                                    Text(
                                        text = "+${userProfile.currencySymbol}$inc",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.updateCategoryBudget(cat.category, 0.0, cat.type)
                                    editingCategory = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Reset", color = SoftRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val amt = customAmountText.toDoubleOrNull() ?: 0.0
                                    viewModel.updateCategoryBudget(cat.category, amt, cat.type)
                                    editingCategory = null
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                            ) {
                                Text("Save Plan", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCategoryItemCard(
    category: CategoryPerformance,
    currencySymbol: String,
    onIncrement: (Double) -> Unit,
    onManualEdit: () -> Unit
) {
    val typeColor = when (category.type) {
        TransactionType.INCOME -> SoftGreen
        TransactionType.EXPENSE -> SoftRed
        TransactionType.ASSET -> SoftTeal
        TransactionType.TRANSFER -> AccentPurple
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Category Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(typeColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category.category.take(1).uppercase(),
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = typeColor
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = category.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextDark
                        )
                        Text(
                            text = "Actual spend so far: $currencySymbol${String.format(Locale.US, "%,.0f", category.actualAmount)}",
                            fontSize = 10.5.sp,
                            color = TextMuted
                        )
                    }
                }

                // Clickable target value badge for manual edit
                Surface(
                    modifier = Modifier.clickable(onClick = onManualEdit),
                    shape = RoundedCornerShape(10.dp),
                    color = CanvasLight,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.0f", category.plannedAmount)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            color = if (category.plannedAmount > 0) TextDark else TextMuted
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(12.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Increment Action Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                QuickIncrementPill(label = "+500", onClick = { onIncrement(500.0) }, modifier = Modifier.weight(1f))
                QuickIncrementPill(label = "+1K", onClick = { onIncrement(1000.0) }, modifier = Modifier.weight(1f))
                QuickIncrementPill(label = "+5K", onClick = { onIncrement(5000.0) }, modifier = Modifier.weight(1f))
                QuickIncrementPill(label = "-1K", onClick = { onIncrement(-1000.0) }, modifier = Modifier.weight(1f), isNegative = true)
            }
        }
    }
}

@Composable
private fun QuickIncrementPill(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isNegative: Boolean = false
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (isNegative) SoftRed.copy(alpha = 0.08f) else CanvasLight,
        border = BorderStroke(0.6.dp, if (isNegative) SoftRed.copy(alpha = 0.3f) else BorderLight)
    ) {
        Text(
            text = label,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isNegative) SoftRed else TextDark,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(vertical = 5.dp)
        )
    }
}
