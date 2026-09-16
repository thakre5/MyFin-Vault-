package com.example.myfin.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.AssetWealthMetrics
import com.example.myfin.ui.MultiYearAssetMetric
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.sin

@Composable
fun YearlyAssetsTab(
    currentWealthAccumulated: Double,
    annualTargetGoal: Double,
    goalCompletionPercentage: Float,
    wealthMetrics: AssetWealthMetrics,
    multiYearAssets: List<MultiYearAssetMetric>,
    selectedYear: Int,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onOpenGraphGuide: (GraphExplanationGuide) -> Unit
) {
    val totalKnownWealth = (wealthMetrics.totalInvestments + wealthMetrics.liquidReserves).coerceAtLeast(1.0)
    val liquidShare = (wealthMetrics.liquidReserves / totalKnownWealth).toFloat().coerceIn(0f, 1f)
    val investedShare = (wealthMetrics.totalInvestments / totalKnownWealth).toFloat().coerceIn(0f, 1f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 240.dp)
    ) {
        // 1. ASYMMETRICAL SPLIT HERO CARD (COMPACT HEART ON RIGHT)
        item(key = "compact_split_heart_card") {
            CompactSplitGoalHeartCard(
                title = "Wealth Accumulation Goal",
                currentAmount = currentWealthAccumulated,
                targetAmount = annualTargetGoal,
                completionRatio = goalCompletionPercentage,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = {
                    onOpenGraphGuide(
                        GraphExplanationGuide(
                            title = "Annual Wealth Goal",
                            subtitle = "Liquid Capital & Net Worth Milestone",
                            whatItShows = "Visualizes progress toward your annual net worth milestone, tracking capital deployed into investments plus retained cash.",
                            visualElements = listOf(
                                "Liquid Wave Level" to "Percentage of your annual wealth target achieved.",
                                "Target Fraction" to "Current capital saved vs. target threshold.",
                                "Outer Heart Perimeter" to "Total compounding goal threshold."
                            ),
                            whyItMatters = "Directly audits long-term capital deployment over daily survival burn.",
                            actionableTip = "Aim to hit 100% by Q4. Every surplus rupee routed to Fortress raises the water level."
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 2. REALIZABLE NET WORTH & SOLVENCY BREAKDOWN
        item(key = "asset_wealth_breakdown_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Realizable Net Worth", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", wealthMetrics.realizableNetWorth)}",
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentPurple
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SoftTeal.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = if (isDiscreetMode) "Gross: ••••" else "Gross $currencySymbol${String.format(Locale.US, "%,.0f", wealthMetrics.grossWealth)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Liquid vs Invested Split Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.5.dp)
                            .clip(CircleShape)
                            .background(BorderLight.copy(alpha = 0.4f))
                    ) {
                        if (investedShare > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(investedShare)
                                    .fillMaxHeight()
                                    .background(SoftTeal)
                            )
                        }
                        if (liquidShare > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(liquidShare)
                                    .fillMaxHeight()
                                    .background(SoftGreen)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Invested: ${(investedShare * 100).toInt()}%",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftTeal
                        )
                        Text(
                            text = "Liquid: ${(liquidShare * 100).toInt()}%",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.7.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Investments",
                            value = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", wealthMetrics.totalInvestments)}",
                            tint = SoftTeal
                        )
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Liquid Cash",
                            value = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", wealthMetrics.liquidReserves)}",
                            tint = SoftGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Active Loans Out",
                            value = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", wealthMetrics.activeReceivables)}",
                            tint = AccentPurple
                        )
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "NPA Bad Debt",
                            value = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", wealthMetrics.npaWrittenOff)}",
                            tint = SoftRed
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. MULTI-YEAR COMPACT PORTFOLIO STOCK
        item(key = "multi_year_asset_pillars_card") {
            MultiYearSegmentedPillarsCard(
                multiYearAssets = multiYearAssets,
                selectedYear = selectedYear,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = {
                    onOpenGraphGuide(
                        GraphExplanationGuide(
                            title = "Cumulative Portfolio Stock",
                            subtitle = "Multi-Year Compounding Wealth",
                            whatItShows = "Displays total accumulated asset stock over time (Investments + Liquid Reserves - Capital Drawdowns) across multi-year cycles.",
                            visualElements = listOf(
                                "Grey Bars" to "Past years' accumulated asset base.",
                                "Violet Gradient Bar" to "Active year's current portfolio stock.",
                                "Growth Badge" to "Year-over-year asset expansion rate."
                            ),
                            whyItMatters = "Validates that your wealth base is compounding continuously across multi-year cycles.",
                            actionableTip = "Aim to sustain steady positive year-over-year asset base growth."
                        )
                    )
                }
            )
        }
    }
}

// =========================================================
// 1. COMPACT ASYMMETRICAL SPLIT GOAL CARD (HEART ON RIGHT)
// =========================================================

@Composable
private fun CompactSplitGoalHeartCard(
    title: String,
    currentAmount: Double,
    targetAmount: Double,
    completionRatio: Float,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    val remainingGap = (targetAmount - currentAmount).coerceAtLeast(0.0)
    val pct = (completionRatio * 100).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Side: Metrics, Progress Bar, Gap
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEDE9FE)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Milestone",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (pct >= 100) SoftGreen.copy(alpha = 0.14f) else CanvasLight,
                        border = BorderStroke(0.6.dp, BorderLight)
                    ) {
                        Text(
                            text = if (pct >= 100) "Goal Achieved" else "Paced for Q4",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pct >= 100) SoftGreen else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isDiscreet) "•••• / ••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", currentAmount)} / $currencySymbol${String.format(Locale.US, "%,.0f", targetAmount)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Left Mini Track Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(4.5.dp)
                        .clip(CircleShape)
                        .background(BorderLight.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(completionRatio.coerceIn(0.04f, 1f))
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(AccentPurple, Color(0xFF10B981))
                                )
                            )
                    )
                    if (completionRatio < 1f) {
                        Box(
                            modifier = Modifier
                                .weight((1f - completionRatio).coerceAtLeast(0f))
                                .fillMaxHeight()
                                .background(Color.Transparent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isDiscreet) "Compounding active" else if (remainingGap == 0.0) "Annual milestone reached!" else "Need $currencySymbol${String.format(Locale.US, "%,.0f", remainingGap)} more to hit target",
                    fontSize = 9.5.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Side: Small Animated Liquid Heart Canvas (96.dp) with Info Button
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                CleanLivingHeartCanvas(
                    fillPercentage = completionRatio,
                    modifier = Modifier.fillMaxSize()
                )

                Text(
                    text = "$pct%",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Goal Info",
                        tint = TextMuted.copy(alpha = 0.8f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// =========================================================
// COMPACT ANIMATED LIQUID HEART CANVAS
// =========================================================

@Composable
private fun CleanLivingHeartCanvas(
    fillPercentage: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "HeartWaveTransition")

    val wavePhase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase1"
    )

    val wavePhase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WavePhase2"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Subtle Grid
        val gridStep = 16.dp.toPx()
        var currentX = 0f
        while (currentX < w) {
            drawLine(color = Color(0xFFF3F4F6), start = Offset(currentX, 0f), end = Offset(currentX, h), strokeWidth = 0.8.dp.toPx())
            currentX += gridStep
        }
        var currentY = 0f
        while (currentY < h) {
            drawLine(color = Color(0xFFF3F4F6), start = Offset(0f, currentY), end = Offset(w, currentY), strokeWidth = 0.8.dp.toPx())
            currentY += gridStep
        }

        val heartPath = Path().apply {
            moveTo(w / 2f, h * 0.28f)
            cubicTo(w * 0.28f, h * 0.04f, w * 0.02f, h * 0.22f, w * 0.02f, h * 0.48f)
            cubicTo(w * 0.02f, h * 0.70f, w * 0.26f, h * 0.84f, w / 2f, h * 0.98f)
            cubicTo(w * 0.74f, h * 0.84f, w * 0.98f, h * 0.70f, w * 0.98f, h * 0.48f)
            cubicTo(w * 0.98f, h * 0.22f, w * 0.72f, h * 0.04f, w / 2f, h * 0.28f)
            close()
        }

        drawPath(path = heartPath, color = Color.White)
        drawPath(path = heartPath, color = Color(0xFFF3E8FF).copy(alpha = 0.65f))

        clipPath(heartPath) {
            val fillHeight = h * fillPercentage.coerceIn(0.06f, 0.96f)
            val fillTop = (h * 0.98f) - fillHeight
            val amplitude = 4.dp.toPx()
            val wavelength = w * 0.85f

            // Wave Layer 1
            val backWave = Path().apply {
                val startY = fillTop + amplitude * sin(wavePhase1)
                moveTo(0f, startY)
                var x = 0f
                while (x <= w) {
                    val y = fillTop + amplitude * sin((2 * Math.PI * (x / wavelength) + wavePhase1).toFloat())
                    lineTo(x, y)
                    x += 2.5f
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = backWave,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.65f), Color(0xFF6D28D9)),
                    startY = fillTop - 8f,
                    endY = h
                )
            )

            // Wave Layer 2
            val frontSurface = Path()
            val frontWave = Path().apply {
                val startY = fillTop + (amplitude * 0.85f) * sin(-wavePhase2)
                moveTo(0f, startY)
                frontSurface.moveTo(0f, startY)
                var x = 0f
                while (x <= w) {
                    val y = fillTop + (amplitude * 0.85f) * sin((2 * Math.PI * (x / (wavelength * 0.92f)) - wavePhase2).toFloat())
                    lineTo(x, y)
                    frontSurface.lineTo(x, y)
                    x += 2.5f
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = frontWave,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFA78BFA), Color(0xFF4C1D95)),
                    startY = fillTop - 8f,
                    endY = h
                )
            )

            drawPath(
                path = frontSurface,
                color = Color.White.copy(alpha = 0.45f),
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawPath(
            path = heartPath,
            color = Color(0xFFDDD6FE),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

// =========================================================
// 3. MULTI-YEAR COMPACT SEGMENTED PILLARS
// =========================================================

@Composable
private fun MultiYearSegmentedPillarsCard(
    multiYearAssets: List<MultiYearAssetMetric>,
    selectedYear: Int,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp)),
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
                    Text("Cumulative Portfolio Stock", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Multi-year asset compounding trajectory", fontSize = 10.5.sp, color = TextMuted)
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

            Spacer(modifier = Modifier.height(14.dp))

            MultiYearSegmentedCanvas(
                multiYearAssets = multiYearAssets,
                selectedYear = selectedYear,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                multiYearAssets.forEach { item ->
                    val isCurrent = item.year == selectedYear
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isCurrent) AccentPurple.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Year ${item.year}",
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isCurrent) AccentPurple else TextDark
                            )
                            if (item.growthPercent != 0.0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = if (item.growthPercent >= 0) SoftGreen.copy(alpha = 0.12f) else SoftRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (item.growthPercent >= 0) "+${item.growthPercent.toInt()}%" else "${item.growthPercent.toInt()}%",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.growthPercent >= 0) SoftGreen else SoftRed,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", item.totalAssets)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = if (isCurrent) AccentPurple else TextDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiYearSegmentedCanvas(
    multiYearAssets: List<MultiYearAssetMetric>,
    selectedYear: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = multiYearAssets.size.coerceAtLeast(1)
        val maxVal = multiYearAssets.maxOfOrNull { it.totalAssets }?.coerceAtLeast(100.0) ?: 100.0

        val barWidth = 28.dp.toPx()
        val spacing = (w - (barWidth * count)) / (count + 1).coerceAtLeast(1)

        multiYearAssets.forEachIndexed { idx, item ->
            val x = spacing + idx * (barWidth + spacing)
            val ratio = (item.totalAssets / maxVal).toFloat().coerceIn(0.06f, 0.90f)
            val barH = (h * 0.74f) * ratio
            val isCurrent = item.year == selectedYear
            val baseY = h - 16.dp.toPx()

            drawRoundRect(
                brush = if (isCurrent) {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF8B5CF6), Color(0xFF10B981)),
                        startY = baseY - barH,
                        endY = baseY
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)),
                        startY = baseY - barH,
                        endY = baseY
                    )
                },
                topLeft = Offset(x, baseY - barH),
                size = Size(barWidth, barH),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
        }

        drawLine(
            color = Color(0xFFE5E7EB),
            start = Offset(0f, h - 14.dp.toPx()),
            end = Offset(w, h - 14.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}
