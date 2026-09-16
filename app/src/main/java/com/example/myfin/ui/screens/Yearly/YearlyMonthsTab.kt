package com.example.myfin.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.YearlyMonthData
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class OutflowLayerFilter(val label: String) {
    ALL("All Layers"),
    FIXED("Fixed Bills"),
    LIFESTYLE("Lifestyle"),
    ASSETS("Assets SIP")
}

@Composable
fun YearlyMonthsTab(
    yearlyMonthsData: List<YearlyMonthData>,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onOpenGraphGuide: (GraphExplanationGuide) -> Unit,
    onInspectMonth: (YearlyMonthData) -> Unit
) {
    val activeMonths = remember(yearlyMonthsData) {
        yearlyMonthsData.filter { !it.isFuture && (it.lifestyleExpenses > 0.0 || it.income > 0.0) }
    }

    val monthsWithBurn = remember(activeMonths) {
        activeMonths.filter { it.lifestyleExpenses > 0.0 }
    }

    val avgMonthlyBurn = remember(monthsWithBurn) {
        if (monthsWithBurn.isNotEmpty()) monthsWithBurn.map { it.lifestyleExpenses }.average() else 0.0
    }

    val leanestMonth = remember(monthsWithBurn) {
        monthsWithBurn.minByOrNull { it.lifestyleExpenses }
    }

    val peakMonth = remember(monthsWithBurn) {
        monthsWithBurn.maxByOrNull { it.lifestyleExpenses }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 240.dp)
    ) {
        // 1. COMPACT LAYERED COMPOSITION CARD WITH DYNAMIC LAYER STATS
        item(key = "layered_mountain_card") {
            CompactLayeredMountainCard(
                yearlyMonths = yearlyMonthsData,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = {
                    onOpenGraphGuide(
                        GraphExplanationGuide(
                            title = "Monthly Outflow Composition",
                            subtitle = "Stacked Expenditure Silhouette",
                            whatItShows = "Breaks your monthly spend into three functional layers, isolating personal living burn from long-term capital deployment.",
                            visualElements = listOf(
                                "Bottom Slate Layer" to "Fixed non-negotiable bills (Rent, Utilities, EMI commitments).",
                                "Middle Violet Layer" to "Discretionary lifestyle burn (Groceries, Dining, Fuel).",
                                "Top Cyan Crest" to "Capital deployed directly into wealth building (Mutual Funds, SIPs)."
                            ),
                            whyItMatters = "Validates that high-spend months aren't eating into your investing layer.",
                            actionableTip = "If the bottom fixed layer exceeds 50% of your earnings, restructure subscriptions and fixed contracts."
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. BEST VS. WORST CYCLE SPOTLIGHT CARD (ADAPTIVE FOR SINGLE OR MULTI-MONTH)
        if (monthsWithBurn.isNotEmpty()) {
            item(key = "spotlight_cycles_card") {
                MonthlySpotlightComparisonCard(
                    leanest = leanestMonth,
                    peak = peakMonth,
                    activeCount = monthsWithBurn.size,
                    avgBurn = avgMonthlyBurn,
                    currencySymbol = currencySymbol,
                    isDiscreet = isDiscreetMode,
                    onTapMonth = { monthData ->
                        if (monthData != null) onInspectMonth(monthData)
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // 3. 12-MONTH CONNECTED MILESTONE TIMELINE
        item(key = "timeline_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "12-Month Flow & Milestones",
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Text(
                    text = "Tap to inspect cycle",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        itemsIndexed(
            items = yearlyMonthsData,
            key = { _, it -> it.monthIndex }
        ) { index, monthData ->
            val isFirst = index == 0
            val isLast = index == yearlyMonthsData.size - 1

            TimelineMonthRow(
                data = monthData,
                avgBurn = avgMonthlyBurn,
                isFirst = isFirst,
                isLast = isLast,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onTapMonth = { onInspectMonth(monthData) }
            )
        }
    }
}

// =========================================================
// 1. COMPACT LAYERED COMPOSITION CARD (ISOLATION SAFE)
// =========================================================

@Composable
private fun CompactLayeredMountainCard(
    yearlyMonths: List<YearlyMonthData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf(OutflowLayerFilter.ALL) }

    val layerTotal = remember(yearlyMonths, selectedFilter) {
        when (selectedFilter) {
            OutflowLayerFilter.ALL -> yearlyMonths.sumOf { it.lifestyleExpenses + it.assets }
            OutflowLayerFilter.FIXED -> yearlyMonths.sumOf { it.fixedExpenses }
            OutflowLayerFilter.LIFESTYLE -> yearlyMonths.sumOf { it.lifestyleExpenses }
            OutflowLayerFilter.ASSETS -> yearlyMonths.sumOf { it.assets }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
            // Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onInfoClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Outflow Composition", fontWeight = FontWeight.Black, fontSize = 16.5.sp, color = TextDark)
                    Text(
                        text = if (isDiscreet) "${selectedFilter.label}: ••••" else "${selectedFilter.label}: $currencySymbol${String.format(Locale.US, "%,.0f", layerTotal)}",
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Micro-Layer Interactive Filter Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OutflowLayerFilter.entries.forEach { filter ->
                    val isSelected = selectedFilter == filter
                    val chipColor = when (filter) {
                        OutflowLayerFilter.ALL -> AccentPurple
                        OutflowLayerFilter.FIXED -> Color(0xFF475569)
                        OutflowLayerFilter.LIFESTYLE -> Color(0xFF8B5CF6)
                        OutflowLayerFilter.ASSETS -> Color(0xFF06B6D4)
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedFilter = filter },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) chipColor.copy(alpha = 0.14f) else CanvasLight,
                        border = BorderStroke(0.6.dp, if (isSelected) chipColor else BorderLight)
                    ) {
                        Text(
                            text = filter.label,
                            fontSize = 9.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) chipColor else TextDark,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Compact Canvas (118.dp) with true layer isolation
            FilteredMountainCanvas(
                yearlyMonths = yearlyMonths,
                activeFilter = selectedFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(118.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Baseline Month Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                YEARLY_MONTH_NAMES.forEach { m ->
                    Text(
                        text = m.take(1),
                        fontSize = 9.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilteredMountainCanvas(
    yearlyMonths: List<YearlyMonthData>,
    activeFilter: OutflowLayerFilter,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = 12
        val stepX = w / (count - 1).toFloat()

        val maxOutflow = yearlyMonths.maxOfOrNull { it.lifestyleExpenses + it.assets }?.coerceAtLeast(100.0) ?: 100.0

        // Background reference gridlines
        for (i in 1..2) {
            val y = h * (i / 3f)
            drawLine(color = Color(0xFFF1F5F9), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1.dp.toPx())
        }

        // Shaded Future Months
        val firstFutureIndex = yearlyMonths.indexOfFirst { it.isFuture }
        if (firstFutureIndex != -1) {
            val futureStartX = firstFutureIndex * stepX
            drawRect(
                color = Color(0xFFF8FAFC).copy(alpha = 0.70f),
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

        fun drawLayerPath(pts: List<Offset>, color: Color, fillBrush: Brush) {
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
            drawPath(path = area, brush = fillBrush)
            drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }

        when (activeFilter) {
            // STACKED COMPOSITION (Cyan at top, Violet in middle, Slate at bottom)
            OutflowLayerFilter.ALL -> {
                val ptsAssets = yearlyMonths.mapIndexed { idx, m ->
                    val x = idx * stepX
                    val r = ((m.lifestyleExpenses + m.assets) / maxOutflow).toFloat().coerceIn(0.04f, 0.92f)
                    Offset(x, h * (1f - r))
                }
                val ptsLifestyle = yearlyMonths.mapIndexed { idx, m ->
                    val x = idx * stepX
                    val r = (m.lifestyleExpenses / maxOutflow).toFloat().coerceIn(0.04f, 0.92f)
                    Offset(x, h * (1f - r))
                }
                val ptsFixed = yearlyMonths.mapIndexed { idx, m ->
                    val x = idx * stepX
                    val r = (m.fixedExpenses / maxOutflow).toFloat().coerceIn(0.04f, 0.92f)
                    Offset(x, h * (1f - r))
                }

                drawLayerPath(
                    ptsAssets,
                    Color(0xFF06B6D4),
                    Brush.verticalGradient(listOf(Color(0xFF06B6D4).copy(alpha = 0.28f), Color.Transparent))
                )
                drawLayerPath(
                    ptsLifestyle,
                    Color(0xFF8B5CF6),
                    Brush.verticalGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.38f), Color.Transparent))
                )
                drawLayerPath(
                    ptsFixed,
                    Color(0xFF475569),
                    Brush.verticalGradient(listOf(Color(0xFF475569).copy(alpha = 0.50f), Color(0xFF1E293B).copy(alpha = 0.20f)))
                )
            }

            // ISOLATED FIXED BILLS
            OutflowLayerFilter.FIXED -> {
                val ptsFixed = yearlyMonths.mapIndexed { idx, m ->
                    val x = idx * stepX
                    val r = (m.fixedExpenses / maxOutflow).toFloat().coerceIn(0.04f, 0.92f)
                    Offset(x, h * (1f - r))
                }
                drawLayerPath(
                    ptsFixed,
                    Color(0xFF475569),
                    Brush.verticalGradient(listOf(Color(0xFF475569).copy(alpha = 0.55f), Color.Transparent))
                )
            }

            // ISOLATED LIFESTYLE BURN
            OutflowLayerFilter.LIFESTYLE -> {
                val ptsLifestyle = yearlyMonths.mapIndexed { idx, m ->
                    val x = idx * stepX
                    val r = (m.lifestyleExpenses / maxOutflow).toFloat().coerceIn(0.04f, 0.92f)
                    Offset(x, h * (1f - r))
                }
                drawLayerPath(
                    ptsLifestyle,
                    Color(0xFF8B5CF6),
                    Brush.verticalGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.45f), Color.Transparent))
                )
            }

            // ISOLATED ASSETS SIP STREAM (True standalone from 0)
            OutflowLayerFilter.ASSETS -> {
                val ptsIsolatedAssets = yearlyMonths.mapIndexed { idx, m ->
                    val x = idx * stepX
                    val r = (m.assets / maxOutflow).toFloat().coerceIn(0.04f, 0.92f)
                    Offset(x, h * (1f - r))
                }
                drawLayerPath(
                    ptsIsolatedAssets,
                    Color(0xFF06B6D4),
                    Brush.verticalGradient(listOf(Color(0xFF06B6D4).copy(alpha = 0.45f), Color.Transparent))
                )
            }
        }
    }
}

// =========================================================
// 2. BEST VS. WORST CYCLE SPOTLIGHT CARD (COLLISION SAFE)
// =========================================================

@Composable
private fun MonthlySpotlightComparisonCard(
    leanest: YearlyMonthData?,
    peak: YearlyMonthData?,
    activeCount: Int,
    avgBurn: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    onTapMonth: (YearlyMonthData?) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
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
                Text(
                    text = "ANNUAL OUTFLOW SPOTLIGHT",
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    color = TextMuted,
                    letterSpacing = 0.6.sp
                )
                Text(
                    text = if (isDiscreet) "Avg: ••••" else "Avg Burn: $currencySymbol${String.format(Locale.US, "%,.0f", avgBurn)}/mo",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (activeCount < 2 || leanest?.monthIndex == peak?.monthIndex) {
                // Single Active Month Pacing View
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onTapMonth(peak) },
                    shape = RoundedCornerShape(12.dp),
                    color = CanvasLight,
                    border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Active Pacing", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(peak?.monthName ?: "—", fontSize = 14.sp, fontWeight = FontWeight.Black, color = TextDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (isDiscreet || peak == null) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", peak.lifestyleExpenses)}",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark
                            )
                            Text("Initial monthly burn baseline", fontSize = 8.5.sp, color = TextMuted)
                        }
                    }
                }
            } else {
                // Multi-Month Comparative View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Leanest Month
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTapMonth(leanest) },
                        shape = RoundedCornerShape(12.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, SoftGreen.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Leanest Cycle", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SoftGreen)
                                Icon(Icons.AutoMirrored.Filled.TrendingDown, contentDescription = null, tint = SoftGreen, modifier = Modifier.size(13.dp))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = leanest?.monthName ?: "—",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark
                            )
                            Text(
                                text = if (isDiscreet || leanest == null) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", leanest.lifestyleExpenses)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftGreen
                            )
                            val leanDiff = if (leanest != null && avgBurn > 0) (((avgBurn - leanest.lifestyleExpenses) / avgBurn) * 100).roundToInt() else 0
                            Text(
                                text = "$leanDiff% under average",
                                fontSize = 8.5.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Peak Month
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTapMonth(peak) },
                        shape = RoundedCornerShape(12.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, SoftRed.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Peak Cycle", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = SoftRed)
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = SoftRed, modifier = Modifier.size(13.dp))
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = peak?.monthName ?: "—",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark
                            )
                            Text(
                                text = if (isDiscreet || peak == null) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", peak.lifestyleExpenses)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftRed
                            )
                            val peakDiff = if (peak != null && avgBurn > 0) (((peak.lifestyleExpenses - avgBurn) / avgBurn) * 100).roundToInt() else 0
                            Text(
                                text = "+$peakDiff% over average",
                                fontSize = 8.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================
// 3. CONNECTED MILESTONE TIMELINE ROW (WITH CORPORATE FLOAT)
// =========================================================

@Composable
private fun TimelineMonthRow(
    data: YearlyMonthData,
    avgBurn: Double,
    isFirst: Boolean,
    isLast: Boolean,
    currencySymbol: String,
    isDiscreet: Boolean,
    onTapMonth: () -> Unit
) {
    val isSurplus = data.netSavings >= 0
    val hasActivity = data.income > 0 || data.lifestyleExpenses > 0 || data.assets > 0
    val statusColor = if (data.isFuture) TextMuted else if (isSurplus) SoftGreen else SoftRed

    val absSavings = abs(data.netSavings)
    val formattedSavings = if (absSavings >= 1000) "${(absSavings / 1000).toInt()}k" else "${absSavings.toInt()}"
    val badgeText = when {
        data.isFuture -> "Planned"
        !hasActivity -> "No Data"
        isSurplus -> "+$currencySymbol$formattedSavings"
        else -> "-$currencySymbol$formattedSavings"
    }

    val burnDeltaPercent = if (avgBurn > 0 && hasActivity && !data.isFuture) {
        (((data.lifestyleExpenses - avgBurn) / avgBurn) * 100).roundToInt()
    } else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline Spine
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(14.dp)
                    .background(if (isFirst) Color.Transparent else BorderLight)
            )

            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(if (data.isFuture) CanvasLight else statusColor.copy(alpha = 0.18f))
                    .padding(3.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(if (data.isFuture) TextMuted.copy(alpha = 0.4f) else statusColor)
                )
            }

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (isLast) Color.Transparent else BorderLight)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Card Content
        Surface(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
                .shadow(1.5.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onTapMonth),
            shape = RoundedCornerShape(16.dp),
            color = CardWhite,
            border = BorderStroke(0.7.dp, BorderLight.copy(alpha = 0.7f))
        ) {
            Column(modifier = Modifier.padding(13.dp)) {
                // Row 1: Month Name & Retention Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = data.monthName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextDark
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = if (isDiscreet) "••••" else badgeText,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Row 2: Burn Figure, Benchmark Indicator & Corporate Float
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", data.lifestyleExpenses)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (data.isFuture || !hasActivity) TextMuted else TextDark
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Lifestyle Burn",
                                fontSize = 9.5.sp,
                                color = TextMuted
                            )
                            if (data.workExpenses > 0.0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${(data.workExpenses / 1000).toInt()}k float",
                                    fontSize = 8.5.sp,
                                    color = Color(0xFFE57A28),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (hasActivity && !data.isFuture && avgBurn > 0) {
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = (if (burnDeltaPercent <= 0) SoftGreen else SoftRed).copy(alpha = 0.10f)
                        ) {
                            Text(
                                text = if (burnDeltaPercent <= 0) "${abs(burnDeltaPercent)}% under avg" else "+$burnDeltaPercent% over avg",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (burnDeltaPercent <= 0) SoftGreen else SoftRed,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 3: 3-Pillar Micro Distribution Bar
                val totalMonthOutflow = (data.lifestyleExpenses + data.assets).coerceAtLeast(1.0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(BorderLight.copy(alpha = 0.5f))
                ) {
                    if (hasActivity && !data.isFuture) {
                        val fixedRatio = (data.fixedExpenses / totalMonthOutflow).toFloat().coerceIn(0f, 1f)
                        val discretionaryBurn = (data.lifestyleExpenses - data.fixedExpenses).coerceAtLeast(0.0)
                        val discRatio = (discretionaryBurn / totalMonthOutflow).toFloat().coerceIn(0f, 1f)
                        val assetRatio = (data.assets / totalMonthOutflow).toFloat().coerceIn(0f, 1f)

                        if (fixedRatio > 0) {
                            Box(modifier = Modifier.weight(fixedRatio.coerceAtLeast(0.04f)).fillMaxHeight().background(Color(0xFF475569)))
                        }
                        if (discRatio > 0) {
                            Box(modifier = Modifier.weight(discRatio.coerceAtLeast(0.04f)).fillMaxHeight().background(Color(0xFF8B5CF6)))
                        }
                        if (assetRatio > 0) {
                            Box(modifier = Modifier.weight(assetRatio.coerceAtLeast(0.04f)).fillMaxHeight().background(Color(0xFF06B6D4)))
                        }
                    }
                }
            }
        }
    }
}
