package com.example.myfin.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.YearlyMonthData
import com.example.myfin.ui.YearlyUiState
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val MONTH_SHORT_LABELS = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

@Composable
fun YearlyCashflowTab(
    yearlyState: YearlyUiState,
    quarterlyData: List<QuarterlyMetrics>,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onOpenMatrixSheet: (CashflowMatrixSheetType) -> Unit
) {
    val yearlyMonthsData = yearlyState.yearlyMonths
    val annualPersonalIncome = yearlyState.annualPersonalIncome
    val annualLifestyleExpenses = yearlyState.annualLifestyleExpenses
    val totalYearlyAssets = yearlyState.totalYearlyAssets
    val reimbursementStatus = yearlyState.reimbursementStatus

    val activeMonthsCount = remember(yearlyMonthsData) {
        val count = yearlyMonthsData.count { !it.isFuture || it.lifestyleExpenses > 0.0 || it.personalIncome > 0.0 }
        count.coerceAtLeast(1)
    }

    val totalFixedObligations = remember(yearlyMonthsData) {
        yearlyMonthsData.sumOf { it.fixedExpenses }
    }
    val totalDiscretionaryBurn = remember(annualLifestyleExpenses, totalFixedObligations) {
        (annualLifestyleExpenses - totalFixedObligations).coerceAtLeast(0.0)
    }
    val netCashRetained = remember(annualPersonalIncome, annualLifestyleExpenses, totalYearlyAssets) {
        annualPersonalIncome - annualLifestyleExpenses - totalYearlyAssets
    }
    val incomeBase = if (annualPersonalIncome > 0.0) annualPersonalIncome else 1.0

    val fixedRatio = (totalFixedObligations / incomeBase).toFloat().coerceIn(0f, 1f)
    val discretionaryRatio = (totalDiscretionaryBurn / incomeBase).toFloat().coerceIn(0f, 1f)
    val assetRatio = (totalYearlyAssets / incomeBase).toFloat().coerceIn(0f, 1f)
    val retainedRatio = (maxOf(0.0, netCashRetained) / incomeBase).toFloat().coerceIn(0f, 1f)

    val projectedInflow = remember(annualPersonalIncome, activeMonthsCount) {
        (annualPersonalIncome / activeMonthsCount) * 12.0
    }
    val projectedBurn = remember(annualLifestyleExpenses, activeMonthsCount) {
        (annualLifestyleExpenses / activeMonthsCount) * 12.0
    }
    val projectedAssets = remember(totalYearlyAssets, activeMonthsCount) {
        (totalYearlyAssets / activeMonthsCount) * 12.0
    }
    val projectedNetSurplus = projectedInflow - projectedBurn - projectedAssets

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 240.dp)
    ) {
        // 1. CASHFLOW DYNAMICS (Opens Dynamics Matrix Sheet)
        item(key = "dual_smooth_wave_card") {
            DualSmoothWaveCard(
                title = "Cashflow Dynamics",
                subtitle = "Personal Inflow vs. Lifestyle Burn",
                yearlyMonths = yearlyMonthsData,
                annualIncome = annualPersonalIncome,
                annualExpenses = annualLifestyleExpenses,
                activeMonthsCount = activeMonthsCount,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = { onOpenMatrixSheet(CashflowMatrixSheetType.CASHFLOW_DYNAMICS) }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. 12-MONTH NET CASHFLOW PULSE (Opens Monthly Itemized Table Sheet)
        item(key = "monthly_cashflow_pulse_card") {
            MonthlyCashflowPulseCard(
                yearlyMonths = yearlyMonthsData,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = { onOpenMatrixSheet(CashflowMatrixSheetType.CASHFLOW_PULSE) }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. ANNUAL 3-PILLAR CAPITAL DEPLOYMENT MATRIX (Opens 50/30/20 Benchmark Matrix)
        item(key = "annual_three_pillar_matrix_card") {
            AnnualThreePillarMatrixCard(
                annualIncome = annualPersonalIncome,
                totalFixed = totalFixedObligations,
                totalVariable = totalDiscretionaryBurn,
                totalAssets = totalYearlyAssets,
                netCashRetained = netCashRetained,
                fixedRatio = fixedRatio,
                discretionaryRatio = discretionaryRatio,
                assetRatio = assetRatio,
                retainedRatio = retainedRatio,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = { onOpenMatrixSheet(CashflowMatrixSheetType.THREE_PILLAR_ALLOCATION) }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 4. YEAR-END PROJECTED RUN-RATE FORECAST (Opens 12-Month Extrapolation Math)
        item(key = "annual_forecast_runrate_card") {
            AnnualForecastRunRateCard(
                activeMonths = activeMonthsCount,
                projectedInflow = projectedInflow,
                projectedBurn = projectedBurn,
                projectedAssets = projectedAssets,
                projectedSurplus = projectedNetSurplus,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = { onOpenMatrixSheet(CashflowMatrixSheetType.RUN_RATE_FORECAST) }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 5. CORPORATE FLOAT & CLAIMS BANNER (Opens Outlay & Advance Ledger)
        if (reimbursementStatus.cumulativeWorkExpenses > 0.0 || reimbursementStatus.excessAdvanceHeld > 0.0 || reimbursementStatus.pendingReimbursement > 0.0) {
            item(key = "reimbursement_banner") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onOpenMatrixSheet(CashflowMatrixSheetType.CORPORATE_FLOAT) },
                    shape = RoundedCornerShape(16.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, Color(0xFFE57A28).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE57A28).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.WorkOutline,
                                contentDescription = null,
                                tint = Color(0xFFE57A28),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Corporate Float & Claims", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = when {
                                    reimbursementStatus.excessAdvanceHeld > 0.0 ->
                                        "Holding ${currencySymbol}${String.format(Locale.US, "%,.0f", reimbursementStatus.excessAdvanceHeld)} upfront company advance (ring-fenced)."
                                    reimbursementStatus.pendingReimbursement > 0.0 ->
                                        "Company owes you ${currencySymbol}${String.format(Locale.US, "%,.0f", reimbursementStatus.pendingReimbursement)} in pending claims."
                                    else -> "All corporate outlays fully settled."
                                },
                                fontSize = 10.5.sp,
                                color = if (reimbursementStatus.isSettled) SoftGreen else Color(0xFFE57A28),
                                lineHeight = 14.sp
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            val floatAmount = if (reimbursementStatus.excessAdvanceHeld > 0.0) {
                                reimbursementStatus.excessAdvanceHeld
                            } else {
                                reimbursementStatus.pendingReimbursement
                            }

                            val displayAmt: Double = if (floatAmount > 0.0) floatAmount else reimbursementStatus.totalWorkExpenses

                            Text(
                                text = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", displayAmt)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = if (reimbursementStatus.isSettled) SoftGreen else Color(0xFFE57A28)
                            )
                            Text(
                                text = when {
                                    reimbursementStatus.excessAdvanceHeld > 0.0 -> "Advance Held"
                                    reimbursementStatus.pendingReimbursement > 0.0 -> "Claim Due"
                                    else -> "Settled"
                                },
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }
        }

        // 6. FISCAL QUARTER RETENTION GRID (Opens Quarterly Breakdown Sheet)
        item(key = "cashflow_quarterly_grid") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenMatrixSheet(CashflowMatrixSheetType.QUARTERLY_RETENTION) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Fiscal Quarter Retention", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Quarterly Matrix",
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quarterlyData.forEach { q ->
                    val hasActivity = q.totalIncome > 0.0 || q.totalExpenses > 0.0 || q.totalAssets > 0.0
                    val rateText = if (!hasActivity) "—" else "${q.savingsRate.toInt()}%"
                    val rateColor = if (!hasActivity) TextMuted else if (q.netSurplus >= 0) SoftTeal else SoftRed
                    val surplusText = if (!hasActivity) "Pending" else "${currencySymbol}${(q.netSurplus / 1000).toInt()}k"

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(13.dp))
                            .clickable { onOpenMatrixSheet(CashflowMatrixSheetType.QUARTERLY_RETENTION) },
                        shape = RoundedCornerShape(13.dp),
                        color = CardWhite,
                        border = BorderStroke(0.7.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(9.dp)) {
                            Text(q.quarterLabel, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = if (isDiscreetMode) "••••" else rateText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = rateColor
                            )
                            Text(
                                text = if (isDiscreetMode) "••••" else surplusText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!hasActivity) TextMuted else TextDark
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// 1. DUAL SMOOTH WAVE CARD (COMPACT & CLICKABLE)
// =========================================================

@Composable
private fun DualSmoothWaveCard(
    title: String,
    subtitle: String,
    yearlyMonths: List<YearlyMonthData>,
    annualIncome: Double,
    annualExpenses: Double,
    activeMonthsCount: Int,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) }
    val activeMonthlyAvgBurn = if (annualExpenses > 0) annualExpenses / activeMonthsCount else 0.0

    val netOperatingSurplus = annualIncome - annualExpenses
    val retentionPercentage = if (annualIncome > 0) {
        ((netOperatingSurplus / annualIncome) * 100).coerceIn(-100.0, 100.0).roundToInt()
    } else 0

    val peakMonth = yearlyMonths.filter { !it.isFuture }.maxByOrNull { it.lifestyleExpenses }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onInfoClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 16.5.sp, color = TextDark)
                    Text(subtitle, fontSize = 10.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Matrix",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Interactive Touch Scrubber Banner
            AnimatedContent(
                targetState = selectedMonthIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScrubberBanner"
            ) { scrubIdx ->
                if (scrubIdx != null && scrubIdx in yearlyMonths.indices) {
                    val mData = yearlyMonths[scrubIdx]
                    val mInflow = mData.personalIncome
                    val mBurn = mData.lifestyleExpenses
                    val mSurplus = mData.netSavings
                    val mRate = if (mInflow > 0) ((mSurplus / mInflow) * 100).toInt() else 0

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${mData.monthName} ${if (mData.isFuture) "(Plan)" else ""}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    color = AccentPurple
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { selectedMonthIndex = null },
                                    modifier = Modifier.size(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear Selection",
                                        tint = TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = if (isDiscreet) "••••" else "In: $currencySymbol${String.format(Locale.US, "%,.0f", mInflow)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isDiscreet) "••••" else "Burn: $currencySymbol${String.format(Locale.US, "%,.0f", mBurn)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF8B5CF6),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isDiscreet) "••••" else "${if (mSurplus >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", mSurplus)} ($mRate%)",
                                    fontSize = 10.sp,
                                    color = if (mSurplus >= 0) SoftTeal else SoftRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.CenterStart) {
                        Text(
                            text = "Tap or drag across wave to inspect monthly cashflow",
                            fontSize = 9.5.sp,
                            color = TextMuted.copy(alpha = 0.75f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dual Wave Canvas
            DualWaveCanvas(
                yearlyMonths = yearlyMonths,
                selectedMonthIndex = selectedMonthIndex,
                onSelectMonth = { selectedMonthIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
            )

            Spacer(modifier = Modifier.height(3.dp))

            // X-Axis Month Markers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MONTH_SHORT_LABELS.forEachIndexed { idx, label ->
                    val isSelected = selectedMonthIndex == idx
                    val isFuture = yearlyMonths.getOrNull(idx)?.isFuture == true
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                        color = when {
                            isSelected -> AccentPurple
                            isFuture -> TextMuted.copy(alpha = 0.4f)
                            else -> TextDark
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(14.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Inflow", fontSize = 9.5.sp, color = TextDark, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.width(14.dp))

                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Lifestyle Burn", fontSize = 9.5.sp, color = TextDark, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.width(14.dp))

                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Projected", fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Active Monthly Burn", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", activeMonthlyAvgBurn)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = if (peakMonth != null && peakMonth.lifestyleExpenses > 0) "Peak: ${peakMonth.monthName}" else "$activeMonthsCount active mos",
                        fontSize = 9.sp,
                        color = SoftRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Operating Surplus", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = (if (netOperatingSurplus >= 0) SoftTeal else SoftRed).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$retentionPercentage% Retained",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netOperatingSurplus >= 0) SoftTeal else SoftRed,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "${if (netOperatingSurplus >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", netOperatingSurplus)}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = if (netOperatingSurplus >= 0) SoftTeal else SoftRed
                    )
                    Text(
                        text = "Net Operating Spread",
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// =========================================================
// 2. 12-MONTH NET CASHFLOW PULSE CARD (CLICKABLE)
// =========================================================

@Composable
private fun MonthlyCashflowPulseCard(
    yearlyMonths: List<YearlyMonthData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    val maxNet = yearlyMonths.maxOfOrNull { abs(it.netSavings) }?.coerceAtLeast(100.0) ?: 100.0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onInfoClick),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "12-MONTH CASHFLOW PULSE",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Pulse Matrix",
                        tint = AccentPurple,
                        modifier = Modifier.size(13.dp)
                    )
                }

                val surplusMonths = yearlyMonths.count { !it.isFuture && it.netSavings > 0 }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = SoftGreen.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$surplusMonths / 12 Surplus",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftGreen,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pulse Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                yearlyMonths.forEach { m ->
                    val isFuture = m.isFuture
                    val net = m.netSavings
                    val ratio = (abs(net) / maxNet).toFloat().coerceIn(0.12f, 1f)
                    val barHeight = (20 * ratio).dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(BorderLight)
                            )

                            if (isFuture) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFCBD5E1))
                                )
                            } else if (abs(net) < 1.0) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(BorderLight)
                                )
                            } else {
                                val isPositive = net > 0
                                Box(
                                    modifier = Modifier
                                        .width(8.dp)
                                        .height(barHeight)
                                        .offset(y = if (isPositive) (-barHeight / 2) else (barHeight / 2))
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(if (isPositive) SoftGreen else SoftRed)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MONTH_SHORT_LABELS.forEach { m ->
                    Text(
                        text = m,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

// =========================================================
// 3. ANNUAL 3-PILLAR ALLOCATION MATRIX CARD (CLICKABLE)
// =========================================================

@Composable
private fun AnnualThreePillarMatrixCard(
    annualIncome: Double,
    totalFixed: Double,
    totalVariable: Double,
    totalAssets: Double,
    netCashRetained: Double,
    fixedRatio: Float,
    discretionaryRatio: Float,
    assetRatio: Float,
    retainedRatio: Float,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onInfoClick),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentPurple))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ANNUAL 3-PILLAR ALLOCATION",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Pillar Matrix",
                        tint = AccentPurple,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Text(
                    text = if (isDiscreet) "Inflow: ••••" else "Inflow: $currencySymbol${String.format(Locale.US, "%,.0f", annualIncome)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val totalDeployment = (totalFixed + totalVariable + totalAssets + maxOf(0.0, netCashRetained)).coerceAtLeast(1.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.5.dp)
                    .clip(CircleShape)
                    .background(BorderLight.copy(alpha = 0.4f))
            ) {
                if (totalFixed > 0) {
                    Box(modifier = Modifier.weight((totalFixed / totalDeployment).toFloat().coerceIn(0.01f, 1f)).fillMaxHeight().background(Color(0xFF475569)))
                }
                if (totalVariable > 0) {
                    Box(modifier = Modifier.weight((totalVariable / totalDeployment).toFloat().coerceIn(0.01f, 1f)).fillMaxHeight().background(Color(0xFF8B5CF6)))
                }
                if (totalAssets > 0) {
                    Box(modifier = Modifier.weight((totalAssets / totalDeployment).toFloat().coerceIn(0.01f, 1f)).fillMaxHeight().background(Color(0xFF06B6D4)))
                }
                if (netCashRetained > 0) {
                    Box(modifier = Modifier.weight((netCashRetained / totalDeployment).toFloat().coerceIn(0.01f, 1f)).fillMaxHeight().background(Color(0xFF10B981)))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PillarAllocationPill(
                    title = "Fixed Bills",
                    amount = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", totalFixed)}",
                    percentage = "${(fixedRatio * 100).toInt()}%",
                    color = Color(0xFF475569),
                    modifier = Modifier.weight(1f)
                )
                PillarAllocationPill(
                    title = "Lifestyle",
                    amount = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", totalVariable)}",
                    percentage = "${(discretionaryRatio * 100).toInt()}%",
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                PillarAllocationPill(
                    title = "Assets SIP",
                    amount = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", totalAssets)}",
                    percentage = "${(assetRatio * 100).toInt()}%",
                    color = Color(0xFF06B6D4),
                    modifier = Modifier.weight(1f)
                )
                PillarAllocationPill(
                    title = if (netCashRetained >= 0) "Retained" else "Deficit",
                    amount = if (isDiscreet) "••••" else "${if (netCashRetained < 0) "-" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", abs(netCashRetained))}",
                    percentage = "${(retainedRatio * 100).toInt()}%",
                    color = if (netCashRetained >= 0) Color(0xFF10B981) else SoftRed,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PillarAllocationPill(
    title: String,
    amount: String,
    percentage: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(color))
                Spacer(modifier = Modifier.width(3.dp))
                Text(title, fontSize = 8.5.sp, color = TextMuted, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(modifier = Modifier.height(1.dp))
            Text(amount, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Text(percentage, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

// =========================================================
// 4. YEAR-END PROJECTED RUN-RATE CARD (CLICKABLE)
// =========================================================

@Composable
private fun AnnualForecastRunRateCard(
    activeMonths: Int,
    projectedInflow: Double,
    projectedBurn: Double,
    projectedAssets: Double,
    projectedSurplus: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onInfoClick),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFE57A28)))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "12-MONTH RUN-RATE FORECAST",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "Forecast Matrix",
                        tint = AccentPurple,
                        modifier = Modifier.size(13.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE57A28).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Paced on $activeMonths Active Mos",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE57A28),
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Forecasted Inflow", fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", projectedInflow)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(1.dp)
                        .background(BorderLight)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text("Forecasted Burn", fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", projectedBurn)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SoftRed
                    )
                }

                Box(
                    modifier = Modifier
                        .height(26.dp)
                        .width(1.dp)
                        .background(BorderLight)
                )

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text("Year-End Surplus", fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "${if (projectedSurplus >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", projectedSurplus)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = if (projectedSurplus >= 0) SoftTeal else SoftRed
                    )
                }
            }
        }
    }
}

// =========================================================
// 5. CANVAS DRAWING IMPLEMENTATION (DUAL WAVE)
// =========================================================

@Composable
private fun DualWaveCanvas(
    yearlyMonths: List<YearlyMonthData>,
    selectedMonthIndex: Int?,
    onSelectMonth: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .pointerInput(yearlyMonths) {
                detectTapGestures(
                    onTap = { offset ->
                        val stepX = size.width / 11f.coerceAtLeast(1f)
                        val idx = (offset.x / stepX).roundToInt().coerceIn(0, 11)
                        onSelectMonth(if (selectedMonthIndex == idx) null else idx)
                    }
                )
            }
            .pointerInput(yearlyMonths) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val stepX = size.width / 11f.coerceAtLeast(1f)
                        val idx = (offset.x / stepX).roundToInt().coerceIn(0, 11)
                        onSelectMonth(idx)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val stepX = size.width / 11f.coerceAtLeast(1f)
                        val idx = (change.position.x / stepX).roundToInt().coerceIn(0, 11)
                        onSelectMonth(idx)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val count = 12
        val stepX = w / (count - 1).toFloat()

        val personalInflows = yearlyMonths.map { it.personalIncome }
        val personalBurns = yearlyMonths.map { it.lifestyleExpenses }

        val maxVal = (personalInflows + personalBurns).maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

        for (i in 1..2) {
            val y = h * (i / 3f)
            drawLine(
                color = Color(0xFFF1F5F9),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val firstFutureIndex = yearlyMonths.indexOfFirst { it.isFuture }
        if (firstFutureIndex != -1) {
            val futureStartX = firstFutureIndex * stepX
            drawRect(
                color = Color(0xFFF8FAFC).copy(alpha = 0.75f),
                topLeft = Offset(futureStartX, 0f),
                size = Size(w - futureStartX, h)
            )
            drawLine(
                color = Color(0xFFCBD5E1),
                start = Offset(futureStartX, 0f),
                end = Offset(futureStartX, h),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
            )
        }

        val ptsInflow = personalInflows.mapIndexed { idx, inf ->
            val x = idx * stepX
            val ratio = (inf / maxVal).toFloat().coerceIn(0.04f, 0.90f)
            val y = h * (1f - ratio)
            Offset(x, y)
        }

        val ptsBurn = personalBurns.mapIndexed { idx, burn ->
            val x = idx * stepX
            val ratio = (burn / maxVal).toFloat().coerceIn(0.04f, 0.90f)
            val y = h * (1f - ratio)
            Offset(x, y)
        }

        fun drawSmoothLineAndArea(pts: List<Offset>, strokeColor: Color, gradientStart: Color) {
            if (pts.isEmpty()) return
            val path = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 0 until pts.size - 1) {
                    val p0 = pts[i]
                    val p1 = pts[i + 1]
                    val cx = (p0.x + p1.x) / 2
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            val area = Path().apply {
                addPath(path)
                lineTo(pts.last().x, h)
                lineTo(pts.first().x, h)
                close()
            }

            drawPath(
                path = area,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientStart.copy(alpha = 0.20f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawSmoothLineAndArea(ptsInflow, Color(0xFF10B981), Color(0xFF10B981))
        drawSmoothLineAndArea(ptsBurn, Color(0xFF8B5CF6), Color(0xFF8B5CF6))

        val peakBurnIdx = personalBurns.indices.maxByOrNull { personalBurns[it] } ?: 0
        val peakInflowIdx = personalInflows.indices.maxByOrNull { personalInflows[it] } ?: 0

        if (ptsBurn.isNotEmpty() && personalBurns[peakBurnIdx] > 0) {
            val peakPt = ptsBurn[peakBurnIdx]
            drawCircle(color = Color.White, radius = 4.5.dp.toPx(), center = peakPt)
            drawCircle(color = Color(0xFF8B5CF6), radius = 3.dp.toPx(), center = peakPt)
        }

        if (ptsInflow.isNotEmpty() && personalInflows[peakInflowIdx] > 0) {
            val inPt = ptsInflow[peakInflowIdx]
            drawCircle(color = Color.White, radius = 4.5.dp.toPx(), center = inPt)
            drawCircle(color = Color(0xFF10B981), radius = 3.dp.toPx(), center = inPt)
        }

        if (selectedMonthIndex != null && selectedMonthIndex in ptsInflow.indices) {
            val scrubX = selectedMonthIndex * stepX
            drawLine(
                color = AccentPurple.copy(alpha = 0.65f),
                start = Offset(scrubX, 0f),
                end = Offset(scrubX, h),
                strokeWidth = 1.4.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
            )

            val inPoint = ptsInflow[selectedMonthIndex]
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = inPoint)
            drawCircle(color = Color(0xFF10B981), radius = 3.5.dp.toPx(), center = inPoint)

            val burnPoint = ptsBurn[selectedMonthIndex]
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = burnPoint)
            drawCircle(color = Color(0xFF8B5CF6), radius = 3.5.dp.toPx(), center = burnPoint)
        }
    }
}
