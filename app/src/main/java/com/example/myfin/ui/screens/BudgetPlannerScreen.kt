package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.*

@Composable
fun BudgetPlannerScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val budgetPlans = uiState.budgetPlans
    val categories = uiState.masterCategories

    val plannedAmounts = remember(budgetPlans, categories) {
        val map = mutableStateMapOf<String, String>()
        categories.forEach { cat ->
            val plan = budgetPlans.find { it.category == cat.name && it.type == cat.type }
            map[cat.name] = if (plan != null && plan.plannedAmount > 0.0) plan.plannedAmount.toInt().toString() else ""
        }
        map
    }

    val bottomNavPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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

                Text("Budget Planner", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)

                IconButton(
                    onClick = {
                        viewModel.copyPreviousMonthBudget { count ->
                            Toast.makeText(context, "Copied $count category targets from last month", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CardWhite)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy Prior", tint = AccentPurple)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp + bottomNavPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CardWhite
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Monthly Targets Baseline", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Configure your monthly spending and investment caps. These values serve as the benchmark for your Safe-to-Spend calculations.",
                                fontSize = 11.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                items(categories, key = { "${it.name}_${it.type}" }) { cat ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.5.dp, RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        color = CardWhite
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                                Text(
                                    text = cat.type.name,
                                    fontSize = 10.5.sp,
                                    color = when (cat.type) {
                                        TransactionType.INCOME -> SoftGreen
                                        TransactionType.EXPENSE -> SoftRed
                                        TransactionType.ASSET -> SoftTeal
                                        else -> TextMuted
                                    }
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = plannedAmounts[cat.name].orEmpty(),
                                    onValueChange = { plannedAmounts[cat.name] = it },
                                    placeholder = { Text("0", fontSize = 12.sp) },
                                    leadingIcon = { Text(userProfile.currencySymbol, fontSize = 12.sp, color = TextMuted) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.width(130.dp),
                                    shape = RoundedCornerShape(10.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                IconButton(
                                    onClick = {
                                        val amt = plannedAmounts[cat.name]?.toDoubleOrNull() ?: 0.0
                                        viewModel.saveBudgetPlan(cat.name, amt, cat.type)
                                        Toast.makeText(context, "Saved plan for ${cat.name}", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(AccentPurpleLight)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save", tint = AccentPurple, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
