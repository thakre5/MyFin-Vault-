package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun YearlyAuditTab(
    categoryTrajectories: List<CategoryAnnualTrajectory>,
    plannedCategoryCeilings: Map<String, Double>,
    isCategoryLegacy: (String) -> Boolean,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onOpenGraphGuide: (GraphExplanationGuide) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
    ) {
        item(key = "organic_curved_star_radar") {
            OrganicCurvedStarRadarCard(
                categorySums = categoryTrajectories.map { it.annualTotal },
                onInfoClick = {
                    onOpenGraphGuide(
                        GraphExplanationGuide(
                            title = "Annual Spending Pareto",
                            subtitle = "Organic Category Weight Distribution",
                            whatItShows = "Maps which lifestyle categories absorb the highest percentage of your outflow over the course of the year.",
                            visualElements = listOf(
                                "Outer Spikes" to "Categories where spending is concentrated or spiking.",
                                "Center Rings" to "Lower spending thresholds.",
                                "Radial Symmetry" to "A balanced star indicates well-distributed expenditure."
                            ),
                            whyItMatters = "Identifies disproportionate budget drains according to the Pareto Principle (80% of expenses often come from 20% of categories).",
                            actionableTip = "Focus optimizations on the longest protruding spike to make the biggest impact."
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(22.dp))
        }

        item(key = "budget_vs_actual_dual_pillars") {
            BudgetVsActualDualPillarsCard(
                categoryTrajectories = categoryTrajectories,
                plannedCategoryCeilings = plannedCategoryCeilings,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = {
                    onOpenGraphGuide(
                        GraphExplanationGuide(
                            title = "Budgeted vs. Actual Outflow",
                            subtitle = "Variance Analysis",
                            whatItShows = "Compares actual realized spending against real annualized budget ceilings set in your Budget Planner.",
                            visualElements = listOf(
                                "Slate Grey Bar" to "Real annualized budget limit (Monthly Target × 12).",
                                "Purple Bar" to "Actual realized annual spending.",
                                "Height Difference" to "Reflects true surplus or overrun."
                            ),
                            whyItMatters = "Instantly highlights categories where annual spending has exceeded planning targets.",
                            actionableTip = "Categories where the purple bar exceeds the grey bar require tighter variable spend controls."
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(22.dp))
        }

        item(key = "trajectories_title") {
            Text("Annual Trajectory by Category", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
            Text("12-month burn pattern & peak month spikes", fontSize = 11.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (categoryTrajectories.isEmpty()) {
            item(key = "empty_trajectories") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CardWhite
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No recorded expenses for this year", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        } else {
            items(categoryTrajectories, key = { it.categoryName }) { item ->
                val isLegacy = remember(isCategoryLegacy, item.categoryName) {
                    isCategoryLegacy(item.categoryName)
                }
                CategoryTrajectoryRowCard(
                    item = item,
                    currencySymbol = currencySymbol,
                    isDiscreet = isDiscreetMode,
                    isLegacy = isLegacy
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun OrganicCurvedStarRadarCard(
    categorySums: List<Double>,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Annual Spending Pareto", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Text("Organic category weight distribution", fontSize = 11.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                OrganicStarRadarCanvas(categorySums = categorySums)

                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = CardWhite,
                    shadowElevation = 3.dp,
                    border = BorderStroke(1.dp, Color(0xFFEDE9FE))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.PieChart, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganicStarRadarCanvas(
    categorySums: List<Double>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val numAxes = 6
        val c = center
        val maxR = size.minDimension * 0.44f

        for (ring in 1..3) {
            val r = maxR * (ring / 3f)
            drawCircle(
                color = Color(0xFFF1F5F9),
                radius = r,
                center = c,
                style = Stroke(width = 1.dp.toPx())
            )
        }

        val hasData = categorySums.any { it > 0.0 }
        val maxAmt = categorySums.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0

        val polyPath = Path()
        val n = numAxes

        for (i in 0 until n) {
            val amt = categorySums.getOrNull(i) ?: 0.0
            val ratio = if (hasData) (amt / maxAmt).toFloat().coerceIn(0.20f, 0.95f) else 0.45f
            val rTip = maxR * ratio
            val angTip = (i * 2 * Math.PI / n) - Math.PI / 2
            val pTip = Offset(c.x + (rTip * cos(angTip)).toFloat(), c.y + (rTip * sin(angTip)).toFloat())

            val nextAmt = categorySums.getOrNull((i + 1) % n) ?: 0.0
            val nextRatio = if (hasData) (nextAmt / maxAmt).toFloat().coerceIn(0.20f, 0.95f) else 0.45f
            val rValley = min(rTip, maxR * nextRatio) * 0.58f
            val angValley = ((i + 0.5) * 2 * Math.PI / n) - Math.PI / 2
            val pValley = Offset(c.x + (rValley * cos(angValley)).toFloat(), c.y + (rValley * sin(angValley)).toFloat())

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
            style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun BudgetVsActualDualPillarsCard(
    categoryTrajectories: List<CategoryAnnualTrajectory>,
    plannedCategoryCeilings: Map<String, Double>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Budgeted vs. Actual Outflow", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                    Text("Real variance analysis against annualized budget targets", fontSize = 11.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            val displayList = categoryTrajectories.take(4)
            val maxBurn = displayList.maxOfOrNull { cat ->
                maxOf(cat.annualTotal, plannedCategoryCeilings[cat.categoryName] ?: 0.0)
            }?.coerceAtLeast(100.0) ?: 100.0

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                displayList.forEach { cat ->
                    val actualRatio = (cat.annualTotal / maxBurn).toFloat().coerceIn(0.10f, 1f)
                    val plannedAmt = plannedCategoryCeilings[cat.categoryName] ?: 0.0
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
                                    .width(14.dp)
                                    .height((90 * plannedRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (plannedAmt > 0) Color(0xFFCBD5E1) else Color(0xFFE2E8F0))
                            )
                            Box(
                                modifier = Modifier
                                    .width(14.dp)
                                    .height((90 * actualRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(if (isOverrun) SoftRed else Color(0xFF8B5CF6))
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(cat.categoryName.take(6), fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.7.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Planned Target", fontSize = 10.5.sp, color = TextMuted)
                }
                Spacer(modifier = Modifier.width(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Realized Spend", fontSize = 10.5.sp, color = TextMuted)
                }
            }
        }
    }
}

@Composable
private fun CategoryTrajectoryRowCard(
    item: CategoryAnnualTrajectory,
    currencySymbol: String,
    isDiscreet: Boolean,
    isLegacy: Boolean = false
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardWhite,
        border = BorderStroke(0.6.dp, if (isLegacy) Color(0xFFFFB74D).copy(alpha = 0.7f) else BorderLight)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text("${String.format(Locale.US, "%.1f", item.percentageOfTotal)}% of annual outflow", fontSize = 10.5.sp, color = TextMuted)
                }

                Text(
                    text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", item.annualTotal)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = AccentPurple
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            ) {
                val maxMonth = item.monthlyAmounts.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
                val pts = item.monthlyAmounts.mapIndexed { idx, amt ->
                    val x = (idx.toFloat() / 11f) * size.width
                    val y = size.height * (1f - (amt / maxMonth).toFloat().coerceIn(0.1f, 0.9f))
                    Offset(x, y)
                }

                val path = Path()
                pts.forEachIndexed { idx, pt ->
                    if (idx == 0) path.moveTo(pt.x, pt.y) else path.lineTo(pt.x, pt.y)
                }
                drawPath(path, color = AccentPurple, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))

                val peakPt = pts[item.peakMonthIndex]
                drawCircle(color = SoftRed, radius = 3.dp.toPx(), center = peakPt)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Jan", fontSize = 9.sp, color = TextMuted)
                Text("Peak: ${YEARLY_MONTH_NAMES[item.peakMonthIndex]}", fontSize = 9.5.sp, color = SoftRed, fontWeight = FontWeight.Bold)
                Text("Dec", fontSize = 9.sp, color = TextMuted)
            }
        }
    }
}
