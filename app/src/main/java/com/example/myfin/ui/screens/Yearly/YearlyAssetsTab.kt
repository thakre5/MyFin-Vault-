package com.example.myfin.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
    ) {
        item(key = "live_heart_goal_card") {
            LiveAnimatedGoalHeartCard(
                title = "Annual Wealth Accumulation Goal",
                currentAmount = currentWealthAccumulated,
                targetAmount = annualTargetGoal,
                completionRatio = goalCompletionPercentage,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = {
                    onOpenGraphGuide(
                        GraphExplanationGuide(
                            title = "Wealth Accumulation Goal",
                            subtitle = "Live Liquid Capital Tracker",
                            whatItShows = "Visualizes progress toward your annual net worth milestone, tracking capital deployed into investments plus retained cash.",
                            visualElements = listOf(
                                "Liquid Wave Level" to "Percentage of your annual wealth target achieved.",
                                "Target Fraction" to "Current capital saved vs. target threshold.",
                                "Outer Shield" to "Total annual compounding target capacity."
                            ),
                            whyItMatters = "Shifts focus from daily survival to multi-year wealth accumulation.",
                            actionableTip = "Aim to hit 100% by Q4. Every surplus rupee routed to Fortress raises the water level."
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        item(key = "asset_wealth_breakdown_card") {
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Realizable Net Worth", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.realizableNetWorth)}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentPurple
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SoftTeal.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "Gross ${currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.grossWealth)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Investments",
                            value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.totalInvestments)}",
                            tint = SoftTeal
                        )
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "Liquid Cash",
                            value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.liquidReserves)}",
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
                            value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.activeReceivables)}",
                            tint = AccentPurple
                        )
                        QuickMetricTile(
                            modifier = Modifier.weight(1f),
                            label = "NPA Bad Debt",
                            value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", wealthMetrics.npaWrittenOff)}",
                            tint = SoftRed
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

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
                            subtitle = "Compounding Wealth Across Years",
                            whatItShows = "Displays your total accumulated asset stock over time (Investments + Liquid Reserves - Capital Drawdowns) according to Option A cumulative accounting.",
                            visualElements = listOf(
                                "Unified Pillar" to "Total cumulative asset stock accumulated up to that year.",
                                "Emerald Pillar" to "Active year's accumulated net worth stock.",
                                "Growth Badge" to "Year-over-year expansion rate of your asset portfolio."
                            ),
                            whyItMatters = "Validates that your wealth base is compounding continuously across multi-year cycles.",
                            actionableTip = "Aim to sustain steady positive year-over-year asset base growth."
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun LiveAnimatedGoalHeartCard(
    title: String,
    currentAmount: Double,
    targetAmount: Double,
    completionRatio: Float,
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
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
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

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    color = Color(0xFFEDE9FE)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isDiscreet) "•••• / ••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", currentAmount)} / $currencySymbol${String.format(Locale.US, "%,.0f", targetAmount)}",
                    fontSize = 13.5.sp,
                    color = TextMuted,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CleanLivingHeartCanvas(
                        fillPercentage = completionRatio,
                        modifier = Modifier.fillMaxSize()
                    )

                    Text(
                        text = "${(completionRatio * 100).toInt()}%",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                val gap = (targetAmount - currentAmount).coerceAtLeast(0.0)
                Text(
                    text = if (isDiscreet) "Accumulate assets to reach your annual target." else "Deploy $currencySymbol${String.format(Locale.US, "%,.0f", gap)} more to hit your compounding milestone.",
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

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

        val gridStep = 24.dp.toPx()
        var currentX = 0f
        while (currentX < w) {
            drawLine(color = Color(0xFFF3F4F6), start = Offset(currentX, 0f), end = Offset(currentX, h), strokeWidth = 1.dp.toPx())
            currentX += gridStep
        }
        var currentY = 0f
        while (currentY < h) {
            drawLine(color = Color(0xFFF3F4F6), start = Offset(0f, currentY), end = Offset(w, currentY), strokeWidth = 1.dp.toPx())
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
            val amplitude = 7.dp.toPx()
            val wavelength = w * 0.85f

            val backWave = Path().apply {
                val startY = fillTop + amplitude * sin(wavePhase1)
                moveTo(0f, startY)
                var x = 0f
                while (x <= w) {
                    val y = fillTop + amplitude * sin((2 * Math.PI * (x / wavelength) + wavePhase1).toFloat())
                    lineTo(x, y)
                    x += 3f
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = backWave,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.65f), Color(0xFF6D28D9)),
                    startY = fillTop - 10f,
                    endY = h
                )
            )

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
                    x += 3f
                }
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = frontWave,
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFA78BFA), Color(0xFF4C1D95)),
                    startY = fillTop - 10f,
                    endY = h
                )
            )

            drawPath(
                path = frontSurface,
                color = Color.White.copy(alpha = 0.45f),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawPath(
            path = heartPath,
            color = Color(0xFFDDD6FE),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

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
                    Text("Cumulative Portfolio Stock", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    Text("Accumulated asset compounding across calendar years", fontSize = 11.5.sp, color = TextMuted)
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

            MultiYearSegmentedCanvas(
                multiYearAssets = multiYearAssets,
                selectedYear = selectedYear,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                multiYearAssets.forEach { item ->
                    val isCurrent = item.year == selectedYear
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) AccentPurple.copy(alpha = 0.08f) else Color.Transparent)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Year ${item.year}",
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isCurrent) AccentPurple else TextDark
                            )
                            if (item.growthPercent != 0.0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (item.growthPercent >= 0) SoftGreen.copy(alpha = 0.12f) else SoftRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (item.growthPercent >= 0) "+${item.growthPercent.toInt()}%" else "${item.growthPercent.toInt()}%",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item.growthPercent >= 0) SoftGreen else SoftRed,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", item.totalAssets)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
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

        val barWidth = 32.dp.toPx()
        val spacing = (w - (barWidth * count)) / (count + 1).coerceAtLeast(1)

        multiYearAssets.forEachIndexed { idx, item ->
            val x = spacing + idx * (barWidth + spacing)
            val ratio = (item.totalAssets / maxVal).toFloat().coerceIn(0.06f, 0.90f)
            val barH = (h * 0.74f) * ratio
            val isCurrent = item.year == selectedYear
            val baseY = h - 22.dp.toPx()

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
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
        }

        drawLine(
            color = Color(0xFFE5E7EB),
            start = Offset(0f, h - 18.dp.toPx()),
            end = Offset(w, h - 18.dp.toPx()),
            strokeWidth = 1.dp.toPx()
        )
    }
}
