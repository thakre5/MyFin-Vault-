package com.example.myfin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.MonthlySummary
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun YearlyScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToMonth: (month: Int, year: Int) -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    val uiState by viewModel.yearlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var headerHeightDp by remember { mutableStateOf(260.dp) }
    val bottomNavPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(3f)
                .onGloballyPositioned { coordinates ->
                    val calculatedDp = with(density) { coordinates.size.height.toDp() }
                    if (calculatedDp > 100.dp && headerHeightDp != calculatedDp) {
                        headerHeightDp = calculatedDp
                    }
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            CanvasLight,
                            CanvasLight.copy(alpha = 0.98f),
                            CanvasLight.copy(alpha = 0.92f)
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenDrawer()
                    },
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CardWhite)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu", tint = TextDark)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setYearlySelectedYear(uiState.selectedYear - 1)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Prev Year", tint = TextDark)
                    }

                    Text(
                        text = "${uiState.selectedYear} Annual Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setYearlySelectedYear(uiState.selectedYear + 1)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next Year", tint = TextDark)
                    }
                }

                Spacer(modifier = Modifier.size(38.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = CardWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Annual Inflow", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.totalAnnualIncome)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = SoftGreen
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Annual Outflow", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.totalAnnualExpense)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = SoftRed
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Net Wealth Delta", fontSize = 11.sp, color = TextMuted)
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.netAnnualSavings)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (uiState.netAnnualSavings >= 0) SoftTeal else SoftRed
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = headerHeightDp + 10.dp,
                start = 20.dp,
                end = 20.dp,
                bottom = 40.dp + bottomNavPadding
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Monthly Performance Rollup", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            }

            items(uiState.monthlySummaries, key = { it.month }) { monthSummary ->
                YearlyMonthCard(
                    summary = monthSummary,
                    currencySymbol = userProfile.currencySymbol,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setSelectedMonth(monthSummary.month, uiState.selectedYear)
                        onNavigateToMonth(monthSummary.month, uiState.selectedYear)
                    }
                )
            }

            if (uiState.categoryRollups.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Annual Category Distribution", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                }

                items(uiState.categoryRollups, key = { "${it.category}_${it.type}" }) { catRollup ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = CardWhite,
                        shadowElevation = 1.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(catRollup.category, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                                Text(catRollup.type.name, fontSize = 10.5.sp, color = TextMuted)
                            }
                            Text(
                                text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", catRollup.totalActualAmount)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = when (catRollup.type) {
                                    TransactionType.INCOME -> SoftGreen
                                    TransactionType.EXPENSE -> SoftRed
                                    TransactionType.ASSET -> SoftTeal
                                    else -> TextDark
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearlyMonthCard(
    summary: MonthlySummary,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val monthName = SimpleDateFormat("MMMM", Locale.US).format(
        Calendar.getInstance().apply { set(Calendar.MONTH, summary.month - 1) }.time
    )
    val net = summary.totalActualIncome - summary.totalActualExpense

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 1.5.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(monthName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Spacer(modifier = Modifier.height(2.dp))
                Row {
                    Text("In: $currencySymbol${String.format(Locale.US, "%,.0f", summary.totalActualIncome)}", fontSize = 11.sp, color = SoftGreen)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Out: $currencySymbol${String.format(Locale.US, "%,.0f", summary.totalActualExpense)}", fontSize = 11.sp, color = SoftRed)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("Net", fontSize = 10.5.sp, color = TextMuted)
                Text(
                    text = "${if (net >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", net)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = if (net >= 0) SoftTeal else SoftRed
                )
            }
        }
    }
}
