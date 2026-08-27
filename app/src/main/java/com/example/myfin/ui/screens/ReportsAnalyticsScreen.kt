package com.example.myfin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.*
import java.util.Locale

@Composable
fun ReportsAnalyticsScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val totalSpent = uiState.metrics.actualExpenses
    val expenseCategories = uiState.categories.filter { it.type == TransactionType.EXPENSE && it.actualAmount > 0 }

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

                Text("Analytics & Reports", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)

                Spacer(modifier = Modifier.size(38.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp + bottomNavPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("Monthly Inflow vs Outflow Net", fontSize = 12.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            val delta = uiState.metrics.actualIncome - uiState.metrics.actualExpenses
                            Text(
                                text = "${if (delta >= 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", delta)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp,
                                color = if (delta >= 0) SoftTeal else SoftRed
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Inflow: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualIncome)}", fontSize = 11.sp, color = SoftGreen, fontWeight = FontWeight.Bold)
                                Text("Outflow: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualExpenses)}", fontSize = 11.sp, color = SoftRed, fontWeight = FontWeight.Bold)
                                Text("SIP Asset: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualAssets)}", fontSize = 11.sp, color = SoftTeal, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item {
                    Text("Expense Weight Distribution", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                }

                if (expenseCategories.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = CardWhite
                        ) {
                            Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                                Text("No recorded expenses for this timeframe", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
                    items(expenseCategories, key = { it.category }) { cat ->
                        val percent = if (totalSpent > 0) ((cat.actualAmount / totalSpent) * 100).toInt() else 0

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(1.5.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = CardWhite
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(cat.category, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                                    Text(
                                        text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cat.actualAmount)} ($percent%)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = SoftRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { (percent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = SoftRed,
                                    trackColor = BorderLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
