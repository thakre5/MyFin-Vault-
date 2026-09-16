package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionEntity
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

@Composable
fun ReportsSummaryTab(
    userProfileCurrency: String,
    totalIncome: Double,
    netSurplus: Double,
    fixedOutflow: Double,
    variableOutflow: Double,
    selectedVelocityRange: VelocityRange,
    onSelectVelocityRange: (VelocityRange) -> Unit,
    selectedTimeRange: TimeRangeFilter,
    onSelectTimeRange: (TimeRangeFilter) -> Unit,
    plannedBudget: Double,
    safeToSpend: Double,
    spendData: List<DailySpendData>,
    trajectoryData: List<TrajectoryPointData>,
    allTransactions: List<TransactionEntity>,
    isDiscreet: Boolean,
    onOpenMetricInfo: (ChartMetricInfo) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showLocalVelocityMenu by remember { mutableStateOf(false) }

    val totalOutflow = fixedOutflow + variableOutflow
    val totalPeriodExpenses = spendData.sumOf { it.totalAmount }
    val daysCount = when (selectedTimeRange) {
        TimeRangeFilter.THIS_WEEK -> 7.0
        TimeRangeFilter.THIS_MONTH, TimeRangeFilter.LAST_MONTH -> 30.0
        TimeRangeFilter.THIS_YEAR -> 365.0
    }
    val dailyBurn = totalPeriodExpenses / daysCount

    val scaledPeriodBudget = remember(plannedBudget, selectedTimeRange) {
        when (selectedTimeRange) {
            TimeRangeFilter.THIS_WEEK -> plannedBudget * (7.0 / 30.0)
            TimeRangeFilter.THIS_MONTH, TimeRangeFilter.LAST_MONTH -> plannedBudget
            TimeRangeFilter.THIS_YEAR -> plannedBudget * 12.0
        }
    }

    val totalBudget = if (scaledPeriodBudget > 0) scaledPeriodBudget else (totalIncome.takeIf { it > 0 } ?: (totalOutflow * 1.25).coerceAtLeast(1.0))
    val retentionRate = if (totalIncome > 0) ((netSurplus / totalIncome) * 100).coerceIn(-100.0, 100.0) else 0.0
    val commitmentLoad = if (totalBudget > 0) ((fixedOutflow / totalBudget) * 100).coerceIn(0.0, 100.0) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 4.dp, bottom = 140.dp)
    ) {
        // 1. CAPITAL RETENTION HERO CARD
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(3.dp, RoundedCornerShape(24.dp))
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Capital Retention Rate",
                            subtitle = "Net Saved vs. Verified Inflow",
                            formula = "Retention % = ((Income - Expenses - Assets) / Income) * 100",
                            breakdown = "Personal Inflow: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalIncome)} | Net Retained: $userProfileCurrency${String.format(Locale.US, "%,.0f", netSurplus)} (${String.format(Locale.US, "%,.1f", retentionRate)}%).",
                            visualElements = listOf(
                                "Cyan/Blue Top Wave" to "Total daily/weekly outflow volume trajectory.",
                                "Rose/Violet Lower Wave" to "Essential fixed commitments baseline.",
                                "Retained Badge" to "Total liquid and invested capital preserved in this cycle."
                            ),
                            advice = "A retention rate above 20% indicates healthy financial compounding."
                        )
                    )
                },
            shape = RoundedCornerShape(24.dp),
            color = CardWhite,
            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(0.95f)) {
                    Text(
                        text = if (isDiscreet) "••••" else String.format(Locale.US, "%.1f%%", retentionRate),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        letterSpacing = (-0.6).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "CAPITAL RETENTION",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(11.dp))
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = if (isDiscreet) "••••" else if (netSurplus >= 0) "$userProfileCurrency${String.format(Locale.US, "%,.0f", netSurplus)} retained" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", abs(netSurplus))} deficit",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (netSurplus >= 0) SoftTeal else SoftRed
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1.55f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    InteractiveDualGlowWaveCanvas(
                        spendData = spendData,
                        currencySymbol = userProfileCurrency,
                        isDiscreet = isDiscreet,
                        onOpenInfo = {
                            onOpenMetricInfo(
                                ChartMetricInfo(
                                    title = "Dual Inflow & Burn Waves",
                                    subtitle = "Flow Silhouette Breakdown",
                                    formula = "Burn_Spread = Total_Spent - Essential_Fixed",
                                    breakdown = "Displays active spend across ${spendData.size} time segments in $selectedTimeRange.",
                                    visualElements = listOf(
                                        "Blue Ribbon" to "Total outflow volume across the time interval.",
                                        "Rose Ribbon" to "Essential fixed bills volume.",
                                        "Touch Marker" to "Tap to scrub individual day/week expenditure."
                                    ),
                                    advice = "Keep the distance between the two ribbons narrow to prevent discretionary bloat."
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        spendData.forEach { step ->
                            Text(
                                text = step.dayLabel,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. ALLOCATION BREAKDOWN
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Allocation Breakdown",
                            subtitle = "Fixed Obligations vs Discretionary Pacing",
                            formula = "Commitment_Load % = (Fixed_Outflow / Total_Budget) * 100",
                            breakdown = "Fixed AutoPay: $userProfileCurrency${String.format(Locale.US, "%,.0f", fixedOutflow)} | Variable spent: $userProfileCurrency${String.format(Locale.US, "%,.0f", variableOutflow)}.",
                            visualElements = listOf(
                                "Outer Violet Ring" to "Variable discretionary living spend.",
                                "Inner Red Ring" to "Fixed non-negotiable AutoPay obligations.",
                                "Center Percentage" to "Share of budget retained after all expenses."
                            ),
                            advice = "Keeping Fixed AutoPay commitments under 50% guarantees ample safe-to-spend buffer for unpredicted costs."
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Allocation Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Commitments load vs. active safe-to-spend reserve",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(148.dp)
                    .clickable {
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "Concentric Allocation Rings",
                                subtitle = "Fixed vs Variable Budget Geometry",
                                formula = "Load = Fixed ÷ (Fixed + Variable)",
                                breakdown = "Fixed: $userProfileCurrency${String.format(Locale.US, "%,.0f", fixedOutflow)} | Variable: $userProfileCurrency${String.format(Locale.US, "%,.0f", variableOutflow)}",
                                visualElements = listOf(
                                    "Outer Ring (Violet)" to "Discretionary variable expenses.",
                                    "Inner Ring (Red)" to "Contractual bills and fixed debt obligations."
                                ),
                                advice = "If the inner red ring is larger than the outer ring, fixed costs dominate your cashflow."
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                ConcentricRingsDonutCanvas(
                    fixedAmount = fixedOutflow,
                    variableAmount = variableOutflow
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isDiscreet) "••%" else "${retentionRate.coerceAtLeast(0.0).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = "Protected",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardWhite,
                    border = BorderStroke(0.7.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(SoftRed)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Fixed AutoPay", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (isDiscreet) "••••" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", fixedOutflow)}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardWhite,
                    border = BorderStroke(0.7.dp, BorderLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AccentPurple)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Variable Spend", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (isDiscreet) "••••" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", variableOutflow)}",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 3. OUTFLOW VELOCITY (STACKED BARS)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Outflow Velocity",
                            subtitle = "Daily & Weekly Burn Rates",
                            formula = "Daily_Burn = (Total Period Spend) / Total Days",
                            breakdown = "Active cycle burn: $userProfileCurrency${String.format(Locale.US, "%,.0f", dailyBurn)}/day across $selectedTimeRange.",
                            visualElements = listOf(
                                "Red Base" to "Essential fixed bill proportion.",
                                "Violet Top" to "Variable discretionary spend proportion."
                            ),
                            advice = "Track spike days to isolate discretionary surges before they exceed planned thresholds."
                        )
                    )
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Outflow Velocity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Daily burn velocity distribution",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }

            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showLocalVelocityMenu = true
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = selectedTimeRange.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = showLocalVelocityMenu,
                    onDismissRequest = { showLocalVelocityMenu = false }
                ) {
                    TimeRangeFilter.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label) },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTimeRange(filter)
                                showLocalVelocityMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        StackedOutflowBarsCanvas(
            spendData = spendData,
            currencySymbol = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = {
                onOpenMetricInfo(
                    ChartMetricInfo(
                        title = "Outflow Velocity Bars",
                        subtitle = "Segmented Daily/Weekly Stack",
                        formula = "Stack = Essential_Amt + Discretionary_Amt",
                        breakdown = "Shows exact spending composition per interval in $selectedTimeRange.",
                        visualElements = listOf(
                            "Violet Segment" to "Discretionary lifestyle spending.",
                            "Red Segment" to "Essential living and fixed bill payments."
                        ),
                        advice = "Taller bars indicate high-burn days. Aim to level out peaks."
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(26.dp))

        // 4. VELOCITY DENSITY (MICRO STRIP)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Velocity Density",
                            subtitle = "28-Day Transaction Impulse Frequency",
                            formula = "Density = Count(Transactions per Day) over 28 Days",
                            breakdown = "Tracks purchasing friction and how frequently transactions are logged across consecutive days.",
                            visualElements = listOf(
                                "Taller Bars" to "Days with high transaction frequency (>4 purchases).",
                                "Shaded Slate" to "Zero-spend or low-frequency recovery days."
                            ),
                            advice = "Cluster spending into fewer days to cultivate 'no-spend' buffer days and reduce emotional micro-burn."
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Velocity Density",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextDark
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(12.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        MicroFrequencyStripCanvas(transactions = allTransactions)

        Spacer(modifier = Modifier.height(28.dp))

        // 5. CUMULATIVE TRAJECTORY (REACTIVE TO VELOCITY RANGE)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            VelocityRange.entries.forEach { range ->
                val isSelected = selectedVelocityRange == range
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AccentPurple else Color.Transparent)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectVelocityRange(range)
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val finalTarget = trajectoryData.lastOrNull()?.targetCumulative ?: scaledPeriodBudget
        Text(
            text = if (isDiscreet) "Velocity Target: ••••" else "${selectedVelocityRange.label} Target: $userProfileCurrency${String.format(Locale.US, "%,.0f", finalTarget)}",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(10.dp))

        DualTrajectoryLineCanvas(
            trajectoryData = trajectoryData,
            currencySymbol = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = {
                onOpenMetricInfo(
                    ChartMetricInfo(
                        title = "Cumulative Burn Trajectory",
                        subtitle = "Pacing vs Target Allowance (${selectedVelocityRange.label})",
                        formula = "Variance = Target_Line - Actual_Cumulative_Curve",
                        breakdown = "Target budget: $userProfileCurrency${String.format(Locale.US, "%,.0f", finalTarget)} over ${selectedVelocityRange.label}.",
                        visualElements = listOf(
                            "Teal Line" to "Linear target pace limit.",
                            "Purple Line" to "Your actual cumulative spending burn-down curve."
                        ),
                        advice = "If the purple line stays below the teal line, you are operating strictly under your planned budget."
                    )
                )
            }
        )

        Spacer(modifier = Modifier.height(26.dp))

        Text(
            text = "Health Indicators",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SummaryHealthIndicatorPill(
                modifier = Modifier.weight(1f),
                title = "Safe Reserve",
                value = if (isDiscreet) "••••" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", safeToSpend)}",
                badgeText = "Remaining",
                accentColor = SoftTeal,
                onClick = {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Safe-to-Spend Reserve",
                            subtitle = "Discretionary Liquid Buffer",
                            formula = "Safe_Spend = Liquid_Cash - Unpaid_Bills - Advance - Living_Buffer",
                            breakdown = "Currently have $userProfileCurrency${String.format(Locale.US, "%,.0f", safeToSpend)} uncommitted liquid cash.",
                            advice = "This amount is 100% guilt-free to spend because all bills, loans, and emergency reserves are already protected."
                        )
                    )
                }
            )
            SummaryHealthIndicatorPill(
                modifier = Modifier.weight(1f),
                title = "AutoPay Load",
                value = "${commitmentLoad.toInt()}%",
                badgeText = "Committed",
                accentColor = SoftRed,
                onClick = {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Commitment Load",
                            subtitle = "Fixed Obligations Burden",
                            formula = "Load % = (Fixed_Commitments / Total_Income) * 100",
                            breakdown = "Fixed bills account for ${commitmentLoad.toInt()}% of your monthly inflow allowance.",
                            advice = "Keeping this figure below 50% protects your finances from insolvency if income experiences a sudden delay."
                        )
                    )
                }
            )
            SummaryHealthIndicatorPill(
                modifier = Modifier.weight(1f),
                title = "Runway Burn",
                value = if (isDiscreet) "••••/d" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", dailyBurn)}/d",
                badgeText = "Pacing",
                accentColor = AccentPurple,
                onClick = {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Daily Runway Burn Velocity",
                            subtitle = "Speed of Capital Outflow",
                            formula = "Burn_Velocity = Total_Outflow / Elapsed_Days",
                            breakdown = "Currently spending $userProfileCurrency${String.format(Locale.US, "%,.0f", dailyBurn)} each day on average.",
                            advice = "Multiplying this number by remaining days in the month gives your forecasted end-of-month cash requirement."
                        )
                    )
                }
            )
        }
    }
}

@Composable
fun SummaryHealthIndicatorPill(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    badgeText: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        border = BorderStroke(0.7.dp, BorderLight)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = TextDark)
            Spacer(modifier = Modifier.height(2.dp))
            Text(badgeText, fontSize = 9.sp, color = accentColor, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InteractiveDualGlowWaveCanvas(
    spendData: List<DailySpendData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var touchIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Box(
        modifier = modifier
            .pointerInput(spendData) {
                detectTapGestures(
                    onPress = { offset ->
                        val segmentW = size.width / spendData.size.coerceAtLeast(1)
                        touchIndex = (offset.x / segmentW).toInt().coerceIn(0, spendData.lastIndex)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDoubleTap = { onOpenInfo() }
                )
            }
            .pointerInput(spendData) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val segmentW = size.width / spendData.size.coerceAtLeast(1)
                        touchIndex = (offset.x / segmentW).toInt().coerceIn(0, spendData.lastIndex)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDrag = { change, _ ->
                        val segmentW = size.width / spendData.size.coerceAtLeast(1)
                        touchIndex = (change.position.x / segmentW).toInt().coerceIn(0, spendData.lastIndex)
                    },
                    onDragEnd = { touchIndex = null },
                    onDragCancel = { touchIndex = null }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val maxSpend = spendData.maxOfOrNull { it.totalAmount }?.coerceAtLeast(1.0) ?: 1.0
            val hasData = spendData.any { it.totalAmount > 0 }
            val count = spendData.size.coerceAtLeast(2)

            val ptsWave1 = if (hasData) {
                spendData.mapIndexed { idx, d ->
                    val x = (idx.toFloat() / (count - 1)) * w
                    val normY = 1f - (d.totalAmount / maxSpend).toFloat().coerceIn(0.15f, 0.85f)
                    val y = (h * 0.15f) + (normY * h * 0.70f)
                    Offset(x, y)
                }
            } else {
                listOf(
                    Offset(0f, h * 0.82f), Offset(w * 0.25f, h * 0.38f), Offset(w * 0.50f, h * 0.60f),
                    Offset(w * 0.75f, h * 0.40f), Offset(w * 0.90f, h * 0.22f), Offset(w, h * 0.78f)
                )
            }

            val ptsWave2 = if (hasData) {
                spendData.mapIndexed { idx, d ->
                    val x = (idx.toFloat() / (count - 1)) * w
                    val essNorm = 1f - (d.essentialAmount / maxSpend).toFloat().coerceIn(0.10f, 0.90f)
                    val y = (h * 0.20f) + (essNorm * h * 0.65f)
                    Offset(x, y)
                }
            } else {
                listOf(
                    Offset(0f, h * 0.88f), Offset(w * 0.25f, h * 0.80f), Offset(w * 0.50f, h * 0.36f),
                    Offset(w * 0.75f, h * 0.65f), Offset(w * 0.90f, h * 0.78f), Offset(w, h * 0.88f)
                )
            }

            fun buildSpline(pts: List<Offset>): Pair<Path, Path> {
                val stroke = Path().apply {
                    if (pts.isNotEmpty()) {
                        moveTo(pts[0].x, pts[0].y)
                        for (i in 0 until pts.size - 1) {
                            val p0 = pts[i]
                            val p1 = pts[i + 1]
                            val cx = (p0.x + p1.x) / 2
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }
                }
                val fill = Path().apply {
                    addPath(stroke)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                return Pair(stroke, fill)
            }

            val (stroke1, fill1) = buildSpline(ptsWave1)
            val (stroke2, fill2) = buildSpline(ptsWave2)

            drawPath(
                path = fill1,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.55f), Color(0xFF6366F1).copy(alpha = 0.25f), Color.Transparent)
                )
            )
            drawPath(
                path = stroke1,
                brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF6366F1))),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            drawPath(
                path = fill2,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF43F5E).copy(alpha = 0.50f), Color(0xFFA855F7).copy(alpha = 0.20f), Color.Transparent)
                )
            )
            drawPath(
                path = stroke2,
                brush = Brush.horizontalGradient(listOf(Color(0xFFF43F5E), Color(0xFFA855F7))),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )

            touchIndex?.let { idx ->
                val pt = ptsWave1.getOrNull(idx)
                if (pt != null) {
                    drawLine(
                        color = AccentPurple.copy(alpha = 0.6f),
                        start = Offset(pt.x, 0f),
                        end = Offset(pt.x, h),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawCircle(color = AccentPurple, radius = 3.5.dp.toPx(), center = pt)
                }
            }
        }

        touchIndex?.let { idx ->
            val data = spendData.getOrNull(idx)
            if (data != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CardWhite,
                    border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.3f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                ) {
                    Text(
                        text = if (isDiscreet) "${data.dayLabel}: ••••" else "${data.dayLabel}: $currencySymbol${String.format(Locale.US, "%,.0f", data.totalAmount)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ConcentricRingsDonutCanvas(
    fixedAmount: Double,
    variableAmount: Double
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 9.dp.toPx()
        val diameter = size.minDimension

        val outerRadius = (diameter / 2f) - (strokeWidth / 2f)
        drawCircle(
            color = AccentPurple.copy(alpha = 0.12f),
            radius = outerRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        val total = (fixedAmount + variableAmount).coerceAtLeast(1.0)
        val variableSweep = ((variableAmount / total) * 360f).toFloat().coerceIn(10f, 340f)
        drawArc(
            color = AccentPurple,
            startAngle = -90f,
            sweepAngle = variableSweep,
            useCenter = false,
            topLeft = Offset(center.x - outerRadius, center.y - outerRadius),
            size = Size(outerRadius * 2, outerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        val innerRadius = outerRadius - strokeWidth - 10.dp.toPx()
        drawCircle(
            color = SoftRed.copy(alpha = 0.12f),
            radius = innerRadius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        val fixedSweep = ((fixedAmount / total) * 360f).toFloat().coerceIn(10f, 340f)
        drawArc(
            color = SoftRed,
            startAngle = 40f,
            sweepAngle = fixedSweep,
            useCenter = false,
            topLeft = Offset(center.x - innerRadius, center.y - innerRadius),
            size = Size(innerRadius * 2, innerRadius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun StackedOutflowBarsCanvas(
    spendData: List<DailySpendData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .pointerInput(spendData) {
                    detectTapGestures(
                        onPress = { offset ->
                            val count = spendData.size.coerceAtLeast(1)
                            val barWidth = 14.dp.toPx()
                            val spacing = (size.width - (count * barWidth)) / max(1, count - 1)
                            val idx = (offset.x / (barWidth + spacing)).toInt().coerceIn(0, spendData.lastIndex)
                            selectedBarIndex = if (selectedBarIndex == idx) null else idx
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDoubleTap = { onOpenInfo() }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = spendData.size
                val availableWidth = size.width
                val barWidth = 14.dp.toPx()
                val totalBarsWidth = count * barWidth
                val spacing = (availableWidth - totalBarsWidth) / max(1, count - 1)
                val cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())

                val maxSpend = spendData.maxOfOrNull { it.totalAmount }?.coerceAtLeast(1.0) ?: 1.0

                for (i in 0 until count) {
                    val data = spendData[i]
                    val x = i * (barWidth + spacing)

                    val heightRatio = if (data.totalAmount > 0) {
                        (data.totalAmount / maxSpend).toFloat().coerceIn(0.12f, 0.95f)
                    } else 0.06f

                    val totalH = size.height * heightRatio
                    val essentialRatio = if (data.totalAmount > 0) (data.essentialAmount / data.totalAmount).toFloat() else 0.5f
                    val redH = totalH * essentialRatio
                    val barTop = size.height - totalH

                    val isSelected = selectedBarIndex == i

                    drawRoundRect(
                        color = if (data.totalAmount > 0) {
                            if (isSelected) AccentPurple.copy(alpha = 0.8f) else AccentPurple
                        } else BorderLight.copy(alpha = 0.4f),
                        topLeft = Offset(x, barTop),
                        size = Size(barWidth, totalH),
                        cornerRadius = cornerRadius
                    )

                    if (redH > 0f && data.totalAmount > 0) {
                        drawRoundRect(
                            color = SoftRed,
                            topLeft = Offset(x, size.height - redH),
                            size = Size(barWidth, redH),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }

            selectedBarIndex?.let { idx ->
                val data = spendData.getOrNull(idx)
                if (data != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = CardWhite,
                        border = BorderStroke(0.6.dp, BorderLight),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 2.dp)
                    ) {
                        Text(
                            text = if (isDiscreet) "${data.dayLabel}: ••••" else "${data.dayLabel}: $currencySymbol${String.format(Locale.US, "%,.0f", data.totalAmount)}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            spendData.forEach { data ->
                Box(
                    modifier = Modifier.width(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = data.dayLabel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun MicroFrequencyStripCanvas(transactions: List<TransactionEntity>) {
    val dayFrequencies = remember(transactions) {
        val buckets = IntArray(28) { 0 }
        val now = System.currentTimeMillis()
        val oneDayMillis = 86400000L
        for (tx in transactions) {
            val diffDays = ((now - tx.date) / oneDayMillis).toInt().coerceIn(0, 27)
            buckets[27 - diffDays]++
        }
        buckets
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(34.dp)
    ) {
        val barCount = 28
        val spacing = size.width / barCount
        val barW = 3.dp.toPx()
        val maxCount = dayFrequencies.maxOrNull()?.coerceAtLeast(1) ?: 1

        for (i in 0 until barCount) {
            val count = dayFrequencies[i]
            val hRatio = if (count > 0) (count.toFloat() / maxCount).coerceIn(0.25f, 1f) else 0.12f
            val barH = size.height * hRatio
            drawRoundRect(
                color = if (count > 0) {
                    if (i % 3 == 0) SoftTeal else AccentPurple.copy(alpha = 0.85f)
                } else BorderLight.copy(alpha = 0.45f),
                topLeft = Offset(i * spacing, size.height - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(1.5.dp.toPx(), 1.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun DualTrajectoryLineCanvas(
    trajectoryData: List<TrajectoryPointData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    val labels = trajectoryData.map { it.stepLabel }
    val maxTrajectoryValue = trajectoryData.maxOfOrNull { max(it.actualCumulative, it.targetCumulative) }?.coerceAtLeast(100.0) ?: 100.0
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .height(110.dp)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    if (maxTrajectoryValue >= 1000) "${(maxTrajectoryValue / 1000).toInt()}k" else "${maxTrajectoryValue.toInt()}",
                    if (maxTrajectoryValue >= 1000) "${(maxTrajectoryValue * 0.75 / 1000).toInt()}k" else "${(maxTrajectoryValue * 0.75).toInt()}",
                    if (maxTrajectoryValue >= 1000) "${(maxTrajectoryValue * 0.50 / 1000).toInt()}k" else "${(maxTrajectoryValue * 0.50).toInt()}",
                    if (maxTrajectoryValue >= 1000) "${(maxTrajectoryValue * 0.25 / 1000).toInt()}k" else "${(maxTrajectoryValue * 0.25).toInt()}"
                ).forEach { label ->
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(110.dp)
                    .pointerInput(trajectoryData) {
                        detectTapGestures(
                            onPress = { offset ->
                                val count = trajectoryData.size.coerceAtLeast(2)
                                val idx = ((offset.x / size.width) * (count - 1)).toInt().coerceIn(0, trajectoryData.lastIndex)
                                selectedIndex = if (selectedIndex == idx) null else idx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDoubleTap = { onOpenInfo() }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    for (step in 1..3) {
                        val yLine = h * (step / 4f)
                        drawLine(
                            color = BorderLight.copy(alpha = 0.5f),
                            start = Offset(0f, yLine),
                            end = Offset(w, yLine),
                            strokeWidth = 0.8.dp.toPx()
                        )
                    }

                    val count = trajectoryData.size.coerceAtLeast(2)
                    val targetPoints = (0 until count).map { i ->
                        val x = (i.toFloat() / (count - 1).coerceAtLeast(1)) * w
                        val targetVal = trajectoryData.getOrNull(i)?.targetCumulative ?: 0.0
                        val y = h * (1f - (targetVal / maxTrajectoryValue).toFloat().coerceIn(0.08f, 0.92f))
                        Offset(x, y)
                    }

                    val targetPath = Path().apply {
                        moveTo(targetPoints[0].x, targetPoints[0].y)
                        for (i in 0 until targetPoints.size - 1) {
                            val p0 = targetPoints[i]
                            val p1 = targetPoints[i + 1]
                            val cx = (p0.x + p1.x) / 2
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    drawPath(
                        path = targetPath,
                        color = SoftTeal,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                    )

                    val actualPoints = (0 until count).map { i ->
                        val actualVal = trajectoryData.getOrNull(i)?.actualCumulative ?: 0.0
                        val y = h * (1f - (actualVal / maxTrajectoryValue).toFloat().coerceIn(0.08f, 0.92f))
                        Offset(x = (i.toFloat() / (count - 1).coerceAtLeast(1)) * w, y = y)
                    }

                    val actualPath = Path().apply {
                        moveTo(actualPoints[0].x, actualPoints[0].y)
                        for (i in 0 until actualPoints.size - 1) {
                            val p0 = actualPoints[i]
                            val p1 = actualPoints[i + 1]
                            val cx = (p0.x + p1.x) / 2
                            cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                        }
                    }

                    drawPath(
                        path = actualPath,
                        color = AccentPurple,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

                    if (actualPoints.size >= 2) {
                        val lastPt = actualPoints.last()
                        drawCircle(color = CardWhite, radius = 4.5.dp.toPx(), center = lastPt)
                        drawCircle(color = AccentPurple, radius = 3.dp.toPx(), center = lastPt)
                    }
                }

                selectedIndex?.let { idx ->
                    val data = trajectoryData.getOrNull(idx)
                    if (data != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CardWhite,
                            border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.3f)),
                            shadowElevation = 3.dp,
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            Text(
                                text = if (isDiscreet) "${data.stepLabel}: ••••" else "${data.stepLabel}: $currencySymbol${String.format(Locale.US, "%,.0f", data.actualCumulative)} (Target: $currencySymbol${String.format(Locale.US, "%,.0f", data.targetCumulative)})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { day ->
                Text(
                    text = day,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }
    }
}
