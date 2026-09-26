package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private val YEARLY_MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private val RADAR_PALETTE = listOf(
    Color(0xFF8B5CF6), // Purple
    Color(0xFF06B6D4), // Cyan
    Color(0xFF10B981), // Emerald
    Color(0xFFF59E0B), // Amber
    Color(0xFFF43F5E), // Rose
    Color(0xFF64748B)  // Slate
)

@Composable
fun YearlyAuditTab(
    categoryTrajectories: List<CategoryAnnualTrajectory>,
    plannedCategoryCeilings: Map<String, Double>,
    isCategoryLegacy: (String) -> Boolean,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onOpenGraphGuide: (GraphExplanationGuide) -> Unit,
    onCategoryClick: (String) -> Unit = {}
) {
    val totalAnnualBurn = remember(categoryTrajectories) {
        categoryTrajectories.sumOf { it.annualTotal }
    }

    val paretoMetrics = remember(categoryTrajectories, totalAnnualBurn) {
        if (totalAnnualBurn <= 0.0 || categoryTrajectories.isEmpty()) {
            null
        } else {
            var cumulative = 0.0
            var count = 0
            val sorted = categoryTrajectories.sortedByDescending { it.annualTotal }
            for (cat in sorted) {
                cumulative += cat.annualTotal
                count++
                if (cumulative / totalAnnualBurn >= 0.75) break
            }
            val percentage = ((cumulative / totalAnnualBurn) * 100).roundToInt()
            Pair(count, percentage)
        }
    }

    val overrunCategories = remember(categoryTrajectories, plannedCategoryCeilings) {
        categoryTrajectories.filter { cat ->
            val ceiling = plannedCategoryCeilings[cat.categoryName.trim()] ?: 0.0
            ceiling > 0.0 && cat.annualTotal > ceiling
        }
    }

    val totalOverrunAmount = remember(overrunCategories, plannedCategoryCeilings) {
        overrunCategories.sumOf { cat ->
            val ceiling = plannedCategoryCeilings[cat.categoryName.trim()] ?: 0.0
            cat.annualTotal - ceiling
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 240.dp)
    ) {
        // 1. COMPACT PARETO RADAR CARD
        item(key = "compact_pareto_radar_card") {
            CompactParetoRadarCard(
                categoryTrajectories = categoryTrajectories,
                paretoMetrics = paretoMetrics,
                totalBurn = totalAnnualBurn,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = {
                    onOpenGraphGuide(
                        GraphExplanationGuide(
                            title = "Annual Spending Pareto",
                            subtitle = "Cumulative Category Concentration",
                            whatItShows = "Audits your annual outflow distribution against the Pareto Principle (where the vital top categories drive the majority of burn).",
                            visualElements = listOf(
                                "Radial Crests" to "Protrusions represent categories absorbing the largest share of capital.",
                                "Concentric Rings" to "Proportional spending thresholds (33%, 66%, 100%).",
                                "Pareto Metric" to "Shows the exact count of categories accounting for >75% of total annual burn."
                            ),
                            whyItMatters = "Focusing your savings efforts on the top 2-3 protruding categories yields far greater financial results than micromanaging small expenses.",
                            actionableTip = "Cap discretionary limits on your top 2 categories to instantly improve annual retention."
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. BUDGET VS. ACTUAL OUTFLOW VARIANCE
        item(key = "budget_vs_actual_dual_pillars") {
            CompactBudgetVsActualCard(
                categoryTrajectories = categoryTrajectories,
                plannedCategoryCeilings = plannedCategoryCeilings,
                overrunCount = overrunCategories.size,
                totalOverrun = totalOverrunAmount,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = {
                    onOpenGraphGuide(
                        GraphExplanationGuide(
                            title = "Budget vs. Realized Outflow",
                            subtitle = "Annualized Variance Analysis",
                            whatItShows = "Compares actual realized category spending against real annualized limits configured in your Budget Planner.",
                            visualElements = listOf(
                                "Grey Bars" to "Annualized budget ceiling (Target × 12).",
                                "Purple Bars" to "Actual realized annual spend.",
                                "Red Bars" to "Identifies categories where spending exceeded targets."
                            ),
                            whyItMatters = "Pinpoints exact lifestyle domains where budget targets were violated over the calendar year.",
                            actionableTip = "For categories flagged in red, tighten monthly limits or rebalance targets for the upcoming year."
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. CATEGORY TRAJECTORY LIST HEADER
        item(key = "trajectories_title") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Category Trajectory & Spikes", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextDark)
                    Text("12-month pattern with peak burn month", fontSize = 10.5.sp, color = TextMuted)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = CanvasLight,
                    border = BorderStroke(0.6.dp, BorderLight)
                ) {
                    Text(
                        text = "${categoryTrajectories.size} Categories",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (categoryTrajectories.isEmpty()) {
            item(key = "empty_trajectories") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Box(modifier = Modifier.padding(28.dp), contentAlignment = Alignment.Center) {
                        Text("No recorded category expenses for this year", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        } else {
            items(categoryTrajectories, key = { it.categoryName }) { item ->
                val isLegacy = remember(isCategoryLegacy, item.categoryName) {
                    isCategoryLegacy(item.categoryName)
                }
                val plannedCeiling = plannedCategoryCeilings[item.categoryName.trim()] ?: 0.0

                PolishedCategoryTrajectoryRow(
                    item = item,
                    annualCeiling = plannedCeiling,
                    currencySymbol = currencySymbol,
                    isDiscreet = isDiscreetMode,
                    isLegacy = isLegacy,
                    onClick = { onCategoryClick(item.categoryName) }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

// =========================================================
// 1. COMPACT PARETO RADAR CARD (WITH ADAPTIVE N < 3 FALLBACK)
// =========================================================

@Composable
private fun CompactParetoRadarCard(
    categoryTrajectories: List<CategoryAnnualTrajectory>,
    paretoMetrics: Pair<Int, Int>?,
    totalBurn: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    val topCategories = remember(categoryTrajectories) {
        categoryTrajectories.take(6)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onInfoClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Annual Spending Pareto", fontWeight = FontWeight.Black, fontSize = 16.5.sp, color = TextDark)
                    Text(
                        text = if (isDiscreet) "Total Burn: ••••" else "Total Burn: $currencySymbol${String.format(Locale.US, "%,.0f", totalBurn)}",
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
                        contentDescription = "Pareto Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (paretoMetrics != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentPurple.copy(alpha = 0.08f),
                    border = BorderStroke(0.6.dp, AccentPurple.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Top ${paretoMetrics.first} categories drive ${paretoMetrics.second}% of annual burn",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (topCategories.size >= 3) {
                // Star Polygon for >= 3 categories
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CompactStarRadarCanvas(
                        topCategories = topCategories,
                        modifier = Modifier.fillMaxSize()
                    )

                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = CardWhite,
                        shadowElevation = 2.dp,
                        border = BorderStroke(0.8.dp, Color(0xFFEDE9FE))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${topCategories.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentPurple
                            )
                        }
                    }
                }
            } else if (topCategories.isNotEmpty()) {
                // Adaptive Split Bar for 1 or 2 categories
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                            .background(BorderLight.copy(alpha = 0.4f))
                    ) {
                        topCategories.forEachIndexed { idx, cat ->
                            val color = RADAR_PALETTE[idx % RADAR_PALETTE.size]
                            val ratio = if (totalBurn > 0) (cat.annualTotal / totalBurn).toFloat().coerceIn(0.04f, 1f) else 1f
                            Box(
                                modifier = Modifier
                                    .weight(ratio)
                                    .fillMaxHeight()
                                    .background(color)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                topCategories.chunked(2).forEach { rowPair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowPair.forEach { cat ->
                            val colorIdx = (topCategories.indexOf(cat)).coerceIn(0, RADAR_PALETTE.lastIndex)
                            val chipColor = RADAR_PALETTE[colorIdx]

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CanvasLight)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                                    Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(chipColor))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = cat.categoryName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${cat.percentageOfTotal.toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = chipColor
                                )
                            }
                        }
                        if (rowPair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactStarRadarCanvas(
    topCategories: List<CategoryAnnualTrajectory>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val count = topCategories.size
        val c = center
        val maxR = size.minDimension * 0.44f

        for (ring in 1..3) {
            val r = maxR * (ring / 3f)
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = r,
                center = c,
                style = Stroke(width = 0.8.dp.toPx())
            )
        }

        if (count < 3) return@Canvas

        val n = count.coerceAtMost(6)
        val maxAmt = topCategories.maxOfOrNull { it.annualTotal }?.coerceAtLeast(1.0) ?: 1.0
        val polyPath = Path()

        for (i in 0 until n) {
            val amt = topCategories.getOrNull(i)?.annualTotal ?: 0.0
            val ratio = (amt / maxAmt).toFloat().coerceIn(0.18f, 0.95f)
            val rTip = maxR * ratio
            val angTip = (i * 2 * Math.PI / n) - Math.PI / 2
            val pTip = Offset(c.x + (rTip * cos(angTip)).toFloat(), c.y + (rTip * sin(angTip)).toFloat())

            val nextAmt = topCategories.getOrNull((i + 1) % n)?.annualTotal ?: 0.0
            val nextRatio = (nextAmt / maxAmt).toFloat().coerceIn(0.18f, 0.95f)
            val rValley = min(rTip, maxR * nextRatio) * 0.58f
            val angValley = ((i + 0.5) * 2 * Math.PI / n) - Math.PI / 2
            val pValley = Offset(c.x + (rValley * cos(angValley)).toFloat(), c.y + (rValley * sin(angValley)).toFloat())

            drawLine(
                color = Color(0xFFE2E8F0),
                start = c,
                end = Offset(c.x + (maxR * cos(angTip)).toFloat(), c.y + (maxR * sin(angTip)).toFloat()),
                strokeWidth = 0.8.dp.toPx()
            )

            if (i == 0) polyPath.moveTo(pTip.x, pTip.y) else polyPath.lineTo(pTip.x, pTip.y)
            polyPath.lineTo(pValley.x, pValley.y)
        }
        polyPath.close()

        drawPath(
            path = polyPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.35f), Color(0xFF8B5CF6).copy(alpha = 0.08f)),
                center = c,
                radius = maxR
            )
        )
        drawPath(
            path = polyPath,
            color = Color(0xFF8B5CF6),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        for (i in 0 until n) {
            val amt = topCategories.getOrNull(i)?.annualTotal ?: 0.0
            val ratio = (amt / maxAmt).toFloat().coerceIn(0.18f, 0.95f)
            val rTip = maxR * ratio
            val angTip = (i * 2 * Math.PI / n) - Math.PI / 2
            val pTip = Offset(c.x + (rTip * cos(angTip)).toFloat(), c.y + (rTip * sin(angTip)).toFloat())

            val dotColor = RADAR_PALETTE[i % RADAR_PALETTE.size]
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pTip)
            drawCircle(color = dotColor, radius = 2.5.dp.toPx(), center = pTip)
        }
    }
}

// =========================================================
// 2. BUDGET VS ACTUAL DUAL PILLARS (OVERRUN SENSITIVE)
// =========================================================

@Composable
private fun CompactBudgetVsActualCard(
    categoryTrajectories: List<CategoryAnnualTrajectory>,
    plannedCategoryCeilings: Map<String, Double>,
    overrunCount: Int,
    totalOverrun: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    val displayList = remember(categoryTrajectories, plannedCategoryCeilings) {
        val overruns = categoryTrajectories.filter { cat ->
            val planned = plannedCategoryCeilings[cat.categoryName.trim()] ?: 0.0
            planned > 0.0 && cat.annualTotal > planned
        }.sortedByDescending { cat ->
            cat.annualTotal - (plannedCategoryCeilings[cat.categoryName.trim()] ?: 0.0)
        }
        val others = categoryTrajectories.filter { it !in overruns }
            .sortedByDescending { it.annualTotal }
        (overruns + others).take(4)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onInfoClick),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Budget vs. Realized Outflow", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Text("Variance analysis against annualized planner limits", fontSize = 10.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier.size(28.dp).clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Variance Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (overrunCount > 0) SoftRed.copy(alpha = 0.08f) else SoftGreen.copy(alpha = 0.08f),
                border = BorderStroke(0.6.dp, if (overrunCount > 0) SoftRed.copy(alpha = 0.25f) else SoftGreen.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (overrunCount > 0) Icons.Default.WarningAmber else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (overrunCount > 0) SoftRed else SoftGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (overrunCount > 0) {
                            if (isDiscreet) "$overrunCount categories exceeded budget" else "$overrunCount categories exceeded budget (+$currencySymbol${String.format(Locale.US, "%,.0f", totalOverrun)})"
                        } else {
                            "All categories within planned annual ceilings"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (overrunCount > 0) SoftRed else SoftGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (displayList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No planned or realized category data available", fontSize = 11.5.sp, color = TextMuted)
                }
            } else {
                val maxBurn = displayList.maxOfOrNull { cat ->
                    max(cat.annualTotal, plannedCategoryCeilings[cat.categoryName.trim()] ?: 0.0)
                }?.coerceAtLeast(100.0) ?: 100.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(105.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Bottom
                ) {
                    displayList.forEach { cat ->
                        val actualRatio = (cat.annualTotal / maxBurn).toFloat().coerceIn(0.10f, 1f)
                        val plannedAmt = plannedCategoryCeilings[cat.categoryName.trim()] ?: 0.0
                        val plannedRatio = if (plannedAmt > 0) (plannedAmt / maxBurn).toFloat().coerceIn(0.08f, 1f) else 0.04f
                        val isOverrun = plannedAmt > 0 && cat.annualTotal > plannedAmt

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(13.dp)
                                        .height((76 * plannedRatio).dp)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(if (plannedAmt > 0) Color(0xFFCBD5E1) else Color(0xFFE2E8F0))
                                )
                                Box(
                                    modifier = Modifier
                                        .width(13.dp)
                                        .height((76 * actualRatio).dp)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(if (isOverrun) SoftRed else Color(0xFF8B5CF6))
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = cat.categoryName,
                                fontSize = 9.sp,
                                color = if (isOverrun) SoftRed else TextMuted,
                                fontWeight = if (isOverrun) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.width(48.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Planned Target", fontSize = 9.5.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.width(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Spend (Normal)", fontSize = 9.5.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.width(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftRed))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Overrun", fontSize = 9.5.sp, color = SoftRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// =========================================================
// 3. POLISHED CATEGORY TRAJECTORY ROW (PRIVACY-SAFE)
// =========================================================

@Composable
private fun PolishedCategoryTrajectoryRow(
    item: CategoryAnnualTrajectory,
    annualCeiling: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    isLegacy: Boolean = false,
    onClick: () -> Unit = {}
) {
    val isOverrun = annualCeiling > 0.0 && item.annualTotal > annualCeiling
    val monthlyAverage = item.annualTotal / 12.0

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        border = BorderStroke(
            0.7.dp,
            when {
                isOverrun -> SoftRed.copy(alpha = 0.5f)
                isLegacy -> Color(0xFFFFB74D).copy(alpha = 0.7f)
                else -> BorderLight
            }
        )
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.categoryName, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        if (isLegacy) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFFFF3E0),
                                border = BorderStroke(0.6.dp, Color(0xFFFFB74D))
                            ) {
                                Text(
                                    text = "Legacy",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        if (isOverrun) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SoftRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "Over Budget",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftRed,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = "${String.format(Locale.US, "%.1f", item.percentageOfTotal)}% of annual outflow • Avg $currencySymbol${String.format(Locale.US, "%,.0f", monthlyAverage)}/mo",
                        fontSize = 10.sp,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", item.annualTotal)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.5.sp,
                            color = if (isOverrun) SoftRed else AccentPurple
                        )
                        if (annualCeiling > 0) {
                            Text(
                                text = if (isDiscreet) "Cap: ••••" else "Cap: $currencySymbol${String.format(Locale.US, "%,.0f", annualCeiling)}",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Inspect Category",
                        tint = TextMuted.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
            ) {
                if (item.monthlyAmounts.isNotEmpty()) {
                    val maxMonth = item.monthlyAmounts.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                    val count = item.monthlyAmounts.size.coerceAtLeast(2)
                    val pts = item.monthlyAmounts.mapIndexed { idx, amt ->
                        val x = (idx.toFloat() / (count - 1).toFloat()) * size.width
                        val y = size.height * (1f - (amt / maxMonth).toFloat().coerceIn(0.12f, 0.88f))
                        Offset(x, y)
                    }

                    val path = Path()
                    pts.forEachIndexed { idx, pt ->
                        if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                    }
                    drawPath(
                        path = path,
                        color = if (isOverrun) SoftRed else if (item.annualTotal <= 0.0) BorderLight else AccentPurple,
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )

                    if (item.annualTotal > 0.0) {
                        val peakPt = pts.getOrNull(item.peakMonthIndex.coerceIn(0, (pts.size - 1).coerceAtLeast(0)))
                        if (peakPt != null) {
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = peakPt)
                            drawCircle(color = if (isOverrun) SoftRed else AccentPurple, radius = 2.6.dp.toPx(), center = peakPt)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Jan", fontSize = 8.5.sp, color = TextMuted)
                val peakMonthName = YEARLY_MONTH_NAMES.getOrNull(item.peakMonthIndex.coerceIn(0, 11)) ?: "Jan"
                Text(
                    text = if (item.annualTotal <= 0.0) {
                        "No spend recorded"
                    } else if (isDiscreet) {
                        "Peak: $peakMonthName"
                    } else {
                        "Peak: $peakMonthName ($currencySymbol${String.format(Locale.US, "%,.0f", item.peakMonthAmount)})"
                    },
                    fontSize = 9.sp,
                    color = if (isOverrun) SoftRed else AccentPurple,
                    fontWeight = FontWeight.Bold
                )
                Text("Dec", fontSize = 8.5.sp, color = TextMuted)
            }
        }
    }
}
