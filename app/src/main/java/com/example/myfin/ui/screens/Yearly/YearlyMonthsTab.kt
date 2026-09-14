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
import androidx.compose.material.icons.filled.Info
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
import com.example.myfin.ui.YearlyMonthData
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs

@Composable
fun YearlyMonthsTab(
    yearlyMonthsData: List<YearlyMonthData>,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onOpenGraphGuide: (GraphExplanationGuide) -> Unit,
    onInspectMonth: (YearlyMonthData) -> Unit
) {
    val chunkedMonths = remember(yearlyMonthsData) { yearlyMonthsData.chunked(2) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
    ) {
        item(key = "layered_mountain_card") {
            LayeredMountainCompositionCard(
                title = "Monthly Outflow Composition",
                subtitle = "Fixed Commitments vs. Lifestyle Burn vs. Assets SIP",
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
            Spacer(modifier = Modifier.height(20.dp))
        }

        item(key = "timeline_grid_title") {
            Text("Monthly Financial Breakdown", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(chunkedMonths, key = { it.first().monthIndex }) { rowPair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MonthGridTimelineCard(
                    data = rowPair[0],
                    currencySymbol = currencySymbol,
                    isDiscreet = isDiscreetMode,
                    onTapMonth = { onInspectMonth(rowPair[0]) },
                    modifier = Modifier.weight(1f)
                )
                if (rowPair.size > 1) {
                    MonthGridTimelineCard(
                        data = rowPair[1],
                        currencySymbol = currencySymbol,
                        isDiscreet = isDiscreetMode,
                        onTapMonth = { onInspectMonth(rowPair[1]) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun LayeredMountainCompositionCard(
    title: String,
    subtitle: String,
    yearlyMonths: List<YearlyMonthData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp)),
        shape = RoundedCornerShape(26.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
                    Text(subtitle, fontSize = 11.5.sp, color = TextMuted)
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

            LayeredMountainCanvas(
                yearlyMonths = yearlyMonths,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                YEARLY_MONTH_NAMES.forEach { m ->
                    Text(m, fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF475569)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Fixed Bills", fontSize = 10.5.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Lifestyle Burn", fontSize = 10.5.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF06B6D4)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Assets SIP", fontSize = 10.5.sp, color = TextDark, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun LayeredMountainCanvas(
    yearlyMonths: List<YearlyMonthData>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = 12
        val stepX = w / (count - 1).toFloat()

        val maxStack = yearlyMonths.maxOfOrNull { it.lifestyleExpenses + it.assets }?.coerceAtLeast(100.0) ?: 100.0

        val ptsFixed = mutableListOf<Offset>()
        val ptsLifestyle = mutableListOf<Offset>()
        val ptsAssets = mutableListOf<Offset>()

        yearlyMonths.forEachIndexed { idx, m ->
            val x = idx * stepX
            val rFixed = (m.fixedExpenses / maxStack).toFloat().coerceIn(0.04f, 0.92f)
            val rLife = (m.lifestyleExpenses / maxStack).toFloat().coerceIn(0.04f, 0.92f)
            val rAsset = ((m.lifestyleExpenses + m.assets) / maxStack).toFloat().coerceIn(0.04f, 0.92f)

            ptsFixed.add(Offset(x, h * (1f - rFixed)))
            ptsLifestyle.add(Offset(x, h * (1f - rLife)))
            ptsAssets.add(Offset(x, h * (1f - rAsset)))
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

        for (i in 0 until count) {
            val x = i * stepX
            drawLine(color = Color(0xFFF1F5F9), start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1.dp.toPx())
        }

        drawLayerPath(
            ptsAssets,
            Color(0xFF06B6D4),
            Brush.verticalGradient(listOf(Color(0xFF06B6D4).copy(alpha = 0.35f), Color.Transparent))
        )

        drawLayerPath(
            ptsLifestyle,
            Color(0xFF8B5CF6),
            Brush.verticalGradient(listOf(Color(0xFF8B5CF6).copy(alpha = 0.45f), Color.Transparent))
        )

        drawLayerPath(
            ptsFixed,
            Color(0xFF475569),
            Brush.verticalGradient(listOf(Color(0xFF475569).copy(alpha = 0.55f), Color(0xFF1E293B).copy(alpha = 0.25f)))
        )
    }
}

@Composable
private fun MonthGridTimelineCard(
    data: YearlyMonthData,
    currencySymbol: String,
    isDiscreet: Boolean,
    onTapMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSurplus = data.netSavings >= 0
    val hasActivity = data.income > 0 || data.expenses > 0 || data.assets > 0
    val statusColor = if (data.isFuture) TextMuted else if (isSurplus) SoftGreen else SoftRed

    val absSavings = abs(data.netSavings)
    val formattedSavings = if (absSavings >= 1000) "${(absSavings / 1000).toInt()}k" else "${absSavings.toInt()}"
    val badgeText = when {
        data.isFuture -> "Planned"
        !hasActivity -> "No Data"
        isSurplus -> "+$currencySymbol$formattedSavings"
        else -> "-$currencySymbol$formattedSavings"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onTapMonth),
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = data.monthName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = if (isDiscreet) "••••" else badgeText,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", data.lifestyleExpenses)}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = if (data.isFuture || !hasActivity) TextMuted else TextDark
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Personal Burn", fontSize = 10.sp, color = TextMuted)
                if (data.workExpenses > 0.0) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "+${(data.workExpenses / 1000).toInt()}k float",
                        fontSize = 9.sp,
                        color = Color(0xFFE57A28),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val total = (data.income + data.lifestyleExpenses + data.assets).coerceAtLeast(1.0)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(BorderLight.copy(alpha = 0.4f))
            ) {
                if (hasActivity) {
                    Box(modifier = Modifier.weight((data.income / total).toFloat().coerceIn(0.05f, 0.9f)).fillMaxHeight().background(SoftGreen))
                    Box(modifier = Modifier.weight((data.lifestyleExpenses / total).toFloat().coerceIn(0.05f, 0.9f)).fillMaxHeight().background(AccentPurple))
                    Box(modifier = Modifier.weight((data.assets / total).toFloat().coerceIn(0.05f, 0.9f)).fillMaxHeight().background(SoftTeal))
                }
            }
        }
    }
}
