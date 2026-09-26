package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.YearlyMonthData
import com.example.myfin.ui.YearlyUiState
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

val YEARLY_MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

enum class CashflowMatrixSheetType {
    CASHFLOW_DYNAMICS,
    CASHFLOW_PULSE,
    THREE_PILLAR_ALLOCATION,
    RUN_RATE_FORECAST,
    QUARTERLY_RETENTION,
    CORPORATE_FLOAT
}

data class QuarterlyMetrics(
    val quarterLabel: String,
    val quarterIndex: Int,
    val totalIncome: Double,
    val totalExpenses: Double,
    val totalAssets: Double,
    val netSurplus: Double,
    val savingsRate: Double
)

data class CategoryAnnualTrajectory(
    val categoryName: String,
    val annualTotal: Double,
    val percentageOfTotal: Double,
    val monthlyAmounts: List<Double>,
    val peakMonthIndex: Int,
    val peakMonthAmount: Double
)

data class GraphExplanationGuide(
    val title: String,
    val subtitle: String,
    val whatItShows: String,
    val visualElements: List<Pair<String, String>>,
    val whyItMatters: String,
    val actionableTip: String
)

@Composable
fun QuickMetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    tint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = tint)
        }
    }
}

// =========================================================
// LIVE ACTIVE MATRIX BOTTOM SHEET (REAL FINANCIAL NUMBERS)
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashflowActiveMatrixSheet(
    sheetType: CashflowMatrixSheetType,
    yearlyState: YearlyUiState,
    quarterlyData: List<QuarterlyMetrics>,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onDismiss: () -> Unit,
    onNavigateToMonth: (Int) -> Unit = {}
) {
    val yearlyMonths = yearlyState.yearlyMonths
    val annualIncome = yearlyState.annualPersonalIncome
    val annualExpenses = yearlyState.annualLifestyleExpenses
    val annualAssets = yearlyState.totalYearlyAssets
    val netRetained = annualIncome - annualExpenses - annualAssets

    val activeMonths = remember(yearlyMonths) {
        yearlyMonths.count { !it.isFuture || it.lifestyleExpenses > 0.0 || it.personalIncome > 0.0 }.coerceAtLeast(1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 34.dp)
                .navigationBarsPadding()
        ) {
            when (sheetType) {
                // 1. CASHFLOW DYNAMICS LIVE AUDIT MATRIX
                CashflowMatrixSheetType.CASHFLOW_DYNAMICS -> {
                    val activeMonthlyBurn = if (annualExpenses > 0) annualExpenses / activeMonths else 0.0
                    val activeMonthlyIncome = if (annualIncome > 0) annualIncome / activeMonths else 0.0
                    val peakMonth = yearlyMonths.filter { !it.isFuture }.maxByOrNull { it.lifestyleExpenses }
                    val retentionPct = if (annualIncome > 0) (((annualIncome - annualExpenses) / annualIncome) * 100).roundToInt() else 0

                    SheetHeader(
                        title = "Cashflow Dynamics Matrix",
                        subtitle = "Personal Inflow vs Lifestyle Burn Audit",
                        badge = if (retentionPct >= 0) "$retentionPct% Retained" else "${abs(retentionPct)}% Deficit",
                        badgeColor = if (retentionPct >= 20) SoftTeal else SoftRed
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MatrixDataRow(
                            label = "Personal Inflow (YTD)",
                            value = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", annualIncome)}",
                            subtext = "Avg: $currencySymbol${String.format(Locale.US, "%,.0f", activeMonthlyIncome)}/mo active",
                            tint = Color(0xFF10B981)
                        )
                        MatrixDataRow(
                            label = "Personal Lifestyle Burn (YTD)",
                            value = if (isDiscreetMode) "••••" else "-$currencySymbol${String.format(Locale.US, "%,.0f", annualExpenses)}",
                            subtext = "Avg: $currencySymbol${String.format(Locale.US, "%,.0f", activeMonthlyBurn)}/mo active",
                            tint = Color(0xFF8B5CF6)
                        )
                        MatrixDataRow(
                            label = "Peak Expenditure Cycle",
                            value = peakMonth?.monthName ?: "None",
                            subtext = if (peakMonth != null) "Burn: $currencySymbol${String.format(Locale.US, "%,.0f", peakMonth.lifestyleExpenses)}" else "",
                            tint = SoftRed
                        )
                        HorizontalDivider(color = BorderLight, thickness = 0.8.dp)
                        MatrixDataRow(
                            label = "Net Operating Surplus Retained",
                            value = if (isDiscreetMode) "••••" else "${if (annualIncome >= annualExpenses) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", annualIncome - annualExpenses)}",
                            subtext = if (retentionPct >= 0) "$retentionPct% of personal earnings preserved" else "${abs(retentionPct)}% net operating deficit",
                            tint = if (annualIncome >= annualExpenses) SoftTeal else SoftRed,
                            isBold = true
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    ProTipNotice("Operating surplus is the purest wealth indicator. A wide spread between inflow and burn feeds your Fortress vaults directly.")
                }

                // 2. 12-MONTH CASHFLOW PULSE MATRIX (ITEMIZED MONTHS TABLE WITH DIRECT NAVIGATION)
                CashflowMatrixSheetType.CASHFLOW_PULSE -> {
                    val surplusMonths = yearlyMonths.count { !it.isFuture && it.netSavings > 0 }
                    val deficitMonths = yearlyMonths.count { !it.isFuture && it.netSavings < 0 }

                    SheetHeader(
                        title = "12-Month Cashflow Pulse",
                        subtitle = "Tap month to inspect detailed breakdown",
                        badge = "$surplusMonths Surplus / $deficitMonths Deficit",
                        badgeColor = SoftGreen
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(yearlyMonths) { m ->
                            val isSurplus = m.netSavings >= 0
                            val statusColor = if (m.isFuture) TextMuted else if (isSurplus) SoftGreen else SoftRed
                            val mInflow = m.personalIncome

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onDismiss()
                                        onNavigateToMonth(m.monthIndex)
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = CanvasLight,
                                border = BorderStroke(0.6.dp, BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(m.monthName, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = TextDark)
                                        Text(
                                            text = if (isDiscreetMode) "••••" else "In: $currencySymbol${String.format(Locale.US, "%,.0f", mInflow)} | Burn: $currencySymbol${String.format(Locale.US, "%,.0f", m.lifestyleExpenses)}",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = statusColor.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = if (isDiscreetMode) "••••" else if (m.isFuture) "Planned" else "${if (isSurplus) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", m.netSavings)}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    ProTipNotice("Aim for at least 9 surplus months each year to avoid drawing from emergency cushions.")
                }

                // 3. ANNUAL 3-PILLAR ALLOCATION MATRIX (WITH 50/30/20 BENCHMARKS)
                CashflowMatrixSheetType.THREE_PILLAR_ALLOCATION -> {
                    val fixedTotal = yearlyMonths.sumOf { it.fixedExpenses }
                    val varTotal = (annualExpenses - fixedTotal).coerceAtLeast(0.0)
                    val fixedPct = if (annualIncome > 0) ((fixedTotal / annualIncome) * 100).toInt() else 0
                    val varPct = if (annualIncome > 0) ((varTotal / annualIncome) * 100).toInt() else 0
                    val assetPct = if (annualIncome > 0) ((annualAssets / annualIncome) * 100).toInt() else 0
                    val retainedPct = if (annualIncome > 0) ((netRetained.coerceAtLeast(0.0) / annualIncome) * 100).toInt() else 0

                    SheetHeader(
                        title = "Annual 3-Pillar Allocation",
                        subtitle = "Capital Distribution vs 50/30/20 Guidelines",
                        badge = "Total Inflow: $currencySymbol${String.format(Locale.US, "%,.0f", annualIncome)}",
                        badgeColor = AccentPurple
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillarDetailedRow(
                            pillarName = "Pillar 1: Fixed Commitments",
                            amount = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", fixedTotal)}",
                            percentage = "$fixedPct%",
                            benchmark = "Target: <50%",
                            isHealthy = fixedPct <= 50,
                            color = Color(0xFF475569)
                        )
                        PillarDetailedRow(
                            pillarName = "Pillar 2: Variable Lifestyle",
                            amount = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", varTotal)}",
                            percentage = "$varPct%",
                            benchmark = "Target: <30%",
                            isHealthy = varPct <= 30,
                            color = Color(0xFF8B5CF6)
                        )
                        PillarDetailedRow(
                            pillarName = "Pillar 3: Wealth SIP & Assets",
                            amount = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", annualAssets)}",
                            percentage = "$assetPct%",
                            benchmark = "Target: >20%",
                            isHealthy = assetPct >= 20,
                            color = Color(0xFF06B6D4)
                        )
                        PillarDetailedRow(
                            pillarName = if (netRetained >= 0) "Pillar 4: Liquid Vault Buffer" else "Pillar 4: Net Capital Deficit",
                            amount = if (isDiscreetMode) "••••" else "${if (netRetained < 0) "-" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", abs(netRetained))}",
                            percentage = "$retainedPct%",
                            benchmark = if (netRetained >= 0) "Operating Cushion" else "Outflows Exceed Inflow",
                            isHealthy = netRetained >= 0,
                            color = if (netRetained >= 0) Color(0xFF10B981) else SoftRed
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    ProTipNotice("Investments + Liquid Retained = ${(assetPct + retainedPct)}% total savings capacity. Aim to keep this combined rate above 25%.")
                }

                // 4. 12-MONTH RUN-RATE FORECAST MATH
                CashflowMatrixSheetType.RUN_RATE_FORECAST -> {
                    val projInflow = (annualIncome / activeMonths) * 12.0
                    val projBurn = (annualExpenses / activeMonths) * 12.0
                    val projAssets = (annualAssets / activeMonths) * 12.0
                    val projSurplus = projInflow - projBurn - projAssets

                    SheetHeader(
                        title = "12-Month Run-Rate Math",
                        subtitle = "Velocity Extrapolated Across 12 Full Months",
                        badge = "$activeMonths Active Cycles",
                        badgeColor = Color(0xFFE57A28)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Formula: Projected = (YTD Total ÷ $activeMonths Active Months) × 12",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MatrixDataRow("Projected Full-Year Inflow", if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", projInflow)}", "Current YTD: $currencySymbol${String.format(Locale.US, "%,.0f", annualIncome)}", Color(0xFF10B981))
                        MatrixDataRow("Projected Full-Year Burn", if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", projBurn)}", "Current YTD: $currencySymbol${String.format(Locale.US, "%,.0f", annualExpenses)}", SoftRed)
                        MatrixDataRow("Projected Annual SIP Assets", if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", projAssets)}", "Current YTD: $currencySymbol${String.format(Locale.US, "%,.0f", annualAssets)}", Color(0xFF06B6D4))
                        HorizontalDivider(color = BorderLight, thickness = 0.8.dp)
                        MatrixDataRow("Forecasted Year-End Surplus", if (isDiscreetMode) "••••" else "${if (projSurplus >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", projSurplus)}", "Net liquid capital expected by Dec 31", if (projSurplus >= 0) SoftTeal else SoftRed, isBold = true)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    ProTipNotice("If projected surplus is positive, you can safely allocate extra capital to long-term compounding.")
                }

                // 5. FISCAL QUARTER RETENTION TABLE
                CashflowMatrixSheetType.QUARTERLY_RETENTION -> {
                    SheetHeader(
                        title = "Fiscal Quarter Retention Matrix",
                        subtitle = "Quarterly Cashflow Breakdown (Q1–Q4)",
                        badge = "4 Quarters",
                        badgeColor = SoftTeal
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        quarterlyData.forEach { q ->
                            val hasData = q.totalIncome > 0 || q.totalExpenses > 0 || q.totalAssets > 0
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = CanvasLight,
                                border = BorderStroke(0.6.dp, BorderLight)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(q.quarterLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = (if (q.netSurplus >= 0) SoftTeal else SoftRed).copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = if (isDiscreetMode) "••••" else if (!hasData) "Pending" else "${q.savingsRate.toInt()}% Retained",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (!hasData) TextMuted else if (q.netSurplus >= 0) SoftTeal else SoftRed,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (isDiscreetMode) "••••" else "In: $currencySymbol${String.format(Locale.US, "%,.0f", q.totalIncome)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = if (isDiscreetMode) "••••" else "Burn: $currencySymbol${String.format(Locale.US, "%,.0f", q.totalExpenses)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF8B5CF6)
                                        )
                                        Text(
                                            text = if (isDiscreetMode) "••••" else "Surplus: ${if (q.netSurplus >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", q.netSurplus)}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (q.netSurplus >= 0) SoftTeal else SoftRed
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    ProTipNotice("Quarterly rhythms help catch seasonal spikes (e.g. festivals in Q4 or annual bonuses in Q1).")
                }

                // 6. CORPORATE FLOAT & CLAIMS MATRIX
                CashflowMatrixSheetType.CORPORATE_FLOAT -> {
                    val status = yearlyState.reimbursementStatus
                    val totalReimbursementsReceived = yearlyMonths.sumOf { it.corporateReimbursements }

                    SheetHeader(
                        title = "Corporate Float & Claims Ledger",
                        subtitle = "Business Travel & Advance Ring-Fence",
                        badge = if (status.isSettled) "Settled" else "Pending Action",
                        badgeColor = if (status.isSettled) SoftGreen else Color(0xFFE57A28)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MatrixDataRow("Work Expenses Logged (YTD)", if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", status.totalWorkExpenses)}", "Business outlays paid by you this year", Color(0xFFE57A28))
                        MatrixDataRow("Corporate Advances Held", if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", status.excessAdvanceHeld)}", "Upfront company funds (Ring-fenced)", Color(0xFF0D9488))
                        MatrixDataRow("Reimbursements Received (YTD)", if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", totalReimbursementsReceived)}", "Settled company refunds this year", SoftGreen)
                        HorizontalDivider(color = BorderLight, thickness = 0.8.dp)
                        MatrixDataRow("All-Time Pending Claim Due", if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", status.pendingReimbursement)}", "Balance owed back to your personal accounts", if (status.pendingReimbursement > 0) SoftRed else SoftGreen, isBold = true)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    ProTipNotice("Corporate outlays are completely excluded from personal lifestyle burn to ensure clean accounting.")
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextDark)
            ) {
                Text("Done", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// Reusable Matrix Header
@Composable
private fun SheetHeader(
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = TextDark)
            Text(subtitle, fontSize = 11.5.sp, color = TextMuted)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = badgeColor.copy(alpha = 0.12f)
        ) {
            Text(
                text = badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = badgeColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// Reusable Matrix Data Row
@Composable
private fun MatrixDataRow(
    label: String,
    value: String,
    subtext: String,
    tint: Color,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, fontSize = 12.5.sp, color = TextDark, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium)
            if (subtext.isNotBlank()) {
                Text(subtext, fontSize = 10.sp, color = TextMuted)
            }
        }
        Text(value, fontSize = if (isBold) 14.5.sp else 13.sp, fontWeight = if (isBold) FontWeight.Black else FontWeight.Bold, color = tint)
    }
}

// Reusable 3-Pillar Detailed Row
@Composable
private fun PillarDetailedRow(
    pillarName: String,
    amount: String,
    percentage: String,
    benchmark: String,
    isHealthy: Boolean,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(color))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(pillarName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text(benchmark, fontSize = 10.sp, color = if (isHealthy) SoftGreen else Color(0xFFF59E0B))
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(amount, fontSize = 12.5.sp, fontWeight = FontWeight.Black, color = TextDark)
                Text(percentage, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color)
            }
        }
    }
}

// Reusable Pro-Tip Banner
@Composable
private fun ProTipNotice(text: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, fontSize = 10.5.sp, color = TextDark.copy(alpha = 0.85f), lineHeight = 14.sp)
        }
    }
}

// =========================================================
// STATIC GRAPH EXPLANATION BOTTOM SHEET (USED FOR TAB 1-3)
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphExplanationBottomSheet(
    guide: GraphExplanationGuide,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(guide.title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
                    Text(guide.subtitle, fontSize = 12.sp, color = TextMuted)
                }
                Surface(
                    shape = CircleShape,
                    color = AccentPurple.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("What this graph shows", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(guide.whatItShows, fontSize = 12.sp, color = TextDark.copy(alpha = 0.85f), lineHeight = 18.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Visual Guide", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                guide.visualElements.forEach { (label, desc) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentPurple)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(desc, fontSize = 11.sp, color = TextMuted, lineHeight = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.7.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Why this matters for your wealth", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(guide.whyItMatters, fontSize = 11.5.sp, color = TextDark.copy(alpha = 0.8f), lineHeight = 16.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pro Tip: ${guide.actionableTip}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPurple,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextDark)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// =========================================================
// INSPECTED MONTH BOTTOM SHEET (ACCURATE PERSONAL INFLOW)
// =========================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectedMonthBottomSheet(
    mData: YearlyMonthData,
    selectedYear: Int,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onDismiss: () -> Unit,
    onOpenMonth: (Int) -> Unit
) {
    val monthPersonalIncome = mData.personalIncome

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${mData.monthName} $selectedYear", fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
                    Text(if (mData.isFuture) "Planned Cycle" else "Completed Accounting Cycle", fontSize = 11.5.sp, color = TextMuted)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (mData.netSavings >= 0) SoftGreen.copy(alpha = 0.14f) else SoftRed.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isDiscreetMode) "••••" else if (mData.netSavings >= 0) "+${currencySymbol}${String.format(Locale.US, "%,.0f", mData.netSavings)}" else "-${currencySymbol}${String.format(Locale.US, "%,.0f", abs(mData.netSavings))}",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = if (mData.netSavings >= 0) SoftGreen else SoftRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "Personal Inflow",
                    value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", monthPersonalIncome)}",
                    tint = SoftGreen
                )
                QuickMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "Lifestyle Burn",
                    value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", mData.lifestyleExpenses)}",
                    tint = AccentPurple
                )
                QuickMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "Assets SIP",
                    value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", mData.assets)}",
                    tint = SoftTeal
                )
            }

            if (mData.workExpenses > 0.0 || mData.corporateReimbursements > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE57A28).copy(alpha = 0.08f),
                    border = BorderStroke(0.6.dp, Color(0xFFE57A28).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkOutline, contentDescription = null, tint = Color(0xFFE57A28), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Corporate Float Active", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE57A28))
                        }
                        Text(
                            text = if (isDiscreetMode) "••••" else "Outlay: ${currencySymbol}${String.format(Locale.US, "%,.0f", mData.workExpenses)} | Claims: ${currencySymbol}${String.format(Locale.US, "%,.0f", mData.corporateReimbursements)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE57A28)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Top Expenses in ${mData.monthName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))

            val topMonthCategories = mData.transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category.trim() }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(3)

            if (topMonthCategories.isEmpty()) {
                Text("No recorded expenses for this month.", fontSize = 11.5.sp, color = TextMuted)
            } else {
                topMonthCategories.forEach { (cat, amt) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat, fontSize = 12.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", amt)}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onDismiss()
                    onOpenMonth(mData.monthIndex)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextDark)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Open ${mData.monthName} Monthly Dashboard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
