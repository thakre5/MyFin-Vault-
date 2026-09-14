package com.example.myfin.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WorkOutline
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
    onOpenGraphGuide: (GraphExplanationGuide) -> Unit
) {
    val yearlyMonthsData = yearlyState.yearlyMonths
    val annualPersonalIncome = yearlyState.annualPersonalIncome
    val annualLifestyleExpenses = yearlyState.annualLifestyleExpenses
    val reimbursementStatus = yearlyState.reimbursementStatus

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 230.dp)
    ) {
        item(key = "dual_smooth_wave_card") {
            DualSmoothWaveCard(
                title = "Cashflow Dynamics",
                subtitle = "Personal Inflow vs. Lifestyle Burn",
                yearlyMonths = yearlyMonthsData,
                annualIncome = annualPersonalIncome,
                annualExpenses = annualLifestyleExpenses,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreetMode,
                onInfoClick = {
                    onOpenGraphGuide(
                        GraphExplanationGuide(
                            title = "Cashflow Dynamics",
                            subtitle = "Personal Inflow vs. Lifestyle Burn",
                            whatItShows = "Maps monthly personal earnings against true lifestyle expenses across 12 months. All corporate travel floats and capital liquidations are excluded.",
                            visualElements = listOf(
                                "Emerald Line" to "Personal earned income (Salary & Professional earnings).",
                                "Purple Line" to "Personal lifestyle burn (Living costs, groceries, utilities).",
                                "Gap Between Lines" to "Operating cash surplus that compounds into your wealth."
                            ),
                            whyItMatters = "Directly audits living discipline. When the purple burn line approaches the green line, lifestyle inflation is absorbing your capacity to invest.",
                            actionableTip = "Keep the spread between the green and purple lines as wide as possible to sustain high savings rates."
                        )
                    )
                }
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        if (reimbursementStatus.cumulativeWorkExpenses > 0.0 || reimbursementStatus.excessAdvanceHeld > 0.0) {
            item(key = "reimbursement_banner") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, Color(0xFFE57A28).copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE57A28).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.WorkOutline,
                                contentDescription = null,
                                tint = Color(0xFFE57A28),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Corporate Float & Claims", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                            Text(
                                text = when {
                                    reimbursementStatus.excessAdvanceHeld > 0.0 ->
                                        "Holding ${currencySymbol}${String.format(Locale.US, "%,.0f", reimbursementStatus.excessAdvanceHeld)} upfront company advance (ring-fenced)."
                                    reimbursementStatus.pendingReimbursement > 0.0 ->
                                        "Company owes you ${currencySymbol}${String.format(Locale.US, "%,.0f", reimbursementStatus.pendingReimbursement)} in pending claims."
                                    else -> "All corporate outlays fully settled."
                                },
                                fontSize = 11.sp,
                                color = if (reimbursementStatus.isSettled) SoftGreen else Color(0xFFE57A28)
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
                                fontSize = 14.sp,
                                color = if (reimbursementStatus.isSettled) SoftGreen else Color(0xFFE57A28)
                            )
                            Text(
                                text = when {
                                    reimbursementStatus.excessAdvanceHeld > 0.0 -> "Advance Held"
                                    reimbursementStatus.pendingReimbursement > 0.0 -> "Claim Due"
                                    else -> "Settled"
                                },
                                fontSize = 9.5.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }
        }

        item(key = "cashflow_quarterly_grid") {
            Text("Fiscal Quarter Retention", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(10.dp))

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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        color = CardWhite,
                        border = BorderStroke(0.7.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(q.quarterLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isDiscreetMode) "••••" else rateText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = rateColor
                            )
                            Text(
                                text = if (isDiscreetMode) "••••" else surplusText,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (!hasActivity) TextMuted else TextDark
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun DualSmoothWaveCard(
    title: String,
    subtitle: String,
    yearlyMonths: List<YearlyMonthData>,
    annualIncome: Double,
    annualExpenses: Double,
    currencySymbol: String,
    isDiscreet: Boolean,
    onInfoClick: () -> Unit
) {
    var selectedMonthIndex by remember { mutableStateOf<Int?>(null) }

    // Calculate active/elapsed months rather than fixed 12 to avoid math dilution
    val activeMonthsCount = remember(yearlyMonths) {
        val count = yearlyMonths.count { !it.isFuture || it.lifestyleExpenses > 0.0 }
        count.coerceAtLeast(1)
    }
    val activeMonthlyAvgBurn = if (annualExpenses > 0) annualExpenses / activeMonthsCount else 0.0

    val netRetained = annualIncome - annualExpenses
    val retentionPercentage = if (annualIncome > 0) {
        ((netRetained / annualIncome) * 100).coerceIn(-100.0, 100.0).roundToInt()
    } else 0

    val peakMonth = yearlyMonths.filter { !it.isFuture }.maxByOrNull { it.lifestyleExpenses }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(5.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Black, fontSize = 18.5.sp, color = TextDark)
                    Text(subtitle, fontSize = 11.5.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onInfoClick,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Graph Explanation",
                        tint = TextMuted,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Touch Scrubber Banner
            AnimatedContent(
                targetState = selectedMonthIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "ScrubberBanner"
            ) { scrubIdx ->
                if (scrubIdx != null && scrubIdx in yearlyMonths.indices) {
                    val mData = yearlyMonths[scrubIdx]
                    val mInflow = mData.netSavings + mData.lifestyleExpenses + mData.assets
                    val mBurn = mData.lifestyleExpenses
                    val mSurplus = mData.netSavings
                    val mRate = if (mInflow > 0) ((mSurplus / mInflow) * 100).toInt() else 0

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, BorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${mData.monthName} ${if (mData.isFuture) "(Future)" else ""}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = AccentPurple
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = if (isDiscreet) "••••" else "Inflow: $currencySymbol${String.format(Locale.US, "%,.0f", mInflow)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isDiscreet) "••••" else "Burn: $currencySymbol${String.format(Locale.US, "%,.0f", mBurn)}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF8B5CF6),
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isDiscreet) "••••" else "${if (mSurplus >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", mSurplus)} ($mRate%)",
                                    fontSize = 11.sp,
                                    color = if (mSurplus >= 0) SoftTeal else SoftRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.height(28.dp), contentAlignment = Alignment.CenterStart) {
                        Text(
                            text = "Touch and slide across graph to scrub months",
                            fontSize = 10.5.sp,
                            color = TextMuted.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Dual Wave Canvas with Touch Gestures & Future Shading
            DualWaveCanvas(
                yearlyMonths = yearlyMonths,
                selectedMonthIndex = selectedMonthIndex,
                onSelectMonth = { selectedMonthIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // X-Axis Month Markers (J F M A M J J A S O N D)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MONTH_SHORT_LABELS.forEachIndexed { idx, label ->
                    val isSelected = selectedMonthIndex == idx
                    val isFuture = yearlyMonths.getOrNull(idx)?.isFuture == true
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                        color = when {
                            isSelected -> AccentPurple
                            isFuture -> TextMuted.copy(alpha = 0.45f)
                            else -> TextDark
                        },
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Personal Inflow", fontSize = 10.5.sp, color = TextDark, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.width(20.dp))

                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Lifestyle Burn", fontSize = 10.5.sp, color = TextDark, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.width(20.dp))

                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(Color(0xFFCBD5E1)))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Future (Projected)", fontSize = 10.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(14.dp))

            // Bottom Metric Split
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Active Monthly Burn", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", activeMonthlyAvgBurn)}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = if (peakMonth != null && peakMonth.lifestyleExpenses > 0) "Peak: ${peakMonth.monthName}" else "Across $activeMonthsCount active mos",
                        fontSize = 9.5.sp,
                        color = SoftRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Operating Surplus", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.width(5.dp))
                        Surface(
                            shape = RoundedCornerShape(5.dp),
                            color = (if (netRetained >= 0) SoftTeal else SoftRed).copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "$retentionPercentage% Retained",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netRetained >= 0) SoftTeal else SoftRed,
                                modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.5.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "${if (netRetained >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", netRetained)}",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = if (netRetained >= 0) SoftTeal else SoftRed
                    )
                    Text(
                        text = "Net Wealth Saved",
                        fontSize = 9.5.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

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
                    onPress = { offset ->
                        val stepX = size.width / (11f).coerceAtLeast(1f)
                        val idx = (offset.x / stepX).roundToInt().coerceIn(0, 11)
                        onSelectMonth(idx)
                    }
                )
            }
            .pointerInput(yearlyMonths) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val stepX = size.width / (11f).coerceAtLeast(1f)
                        val idx = (offset.x / stepX).roundToInt().coerceIn(0, 11)
                        onSelectMonth(idx)
                    },
                    onDragEnd = { onSelectMonth(null) },
                    onDragCancel = { onSelectMonth(null) },
                    onDrag = { change, _ ->
                        change.consume()
                        val stepX = size.width / (11f).coerceAtLeast(1f)
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

        val personalInflows = yearlyMonths.map { it.netSavings + it.lifestyleExpenses + it.assets }
        val personalBurns = yearlyMonths.map { it.lifestyleExpenses }

        val maxVal = (personalInflows + personalBurns).maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

        // Subtle background grid lines
        for (i in 1..3) {
            val y = h * (i / 4f)
            drawLine(
                color = Color(0xFFF1F5F9),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Shading for Future / Unelapsed months
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
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )
        }

        val ptsInflow = personalInflows.mapIndexed { idx, inf ->
            val x = idx * stepX
            val ratio = (inf / maxVal).toFloat().coerceIn(0.04f, 0.92f)
            val y = h * (1f - ratio)
            Offset(x, y)
        }

        val ptsBurn = personalBurns.mapIndexed { idx, burn ->
            val x = idx * stepX
            val ratio = (burn / maxVal).toFloat().coerceIn(0.04f, 0.92f)
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
                    colors = listOf(gradientStart.copy(alpha = 0.22f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawSmoothLineAndArea(ptsInflow, Color(0xFF10B981), Color(0xFF10B981))
        drawSmoothLineAndArea(ptsBurn, Color(0xFF8B5CF6), Color(0xFF8B5CF6))

        // Peak markers
        val peakBurnIdx = personalBurns.indices.maxByOrNull { personalBurns[it] } ?: 0
        val peakInflowIdx = personalInflows.indices.maxByOrNull { personalInflows[it] } ?: 0

        if (ptsBurn.isNotEmpty() && personalBurns[peakBurnIdx] > 0) {
            val peakPt = ptsBurn[peakBurnIdx]
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = peakPt)
            drawCircle(color = Color(0xFF8B5CF6), radius = 3.5.dp.toPx(), center = peakPt)
        }

        if (ptsInflow.isNotEmpty() && personalInflows[peakInflowIdx] > 0) {
            val inPt = ptsInflow[peakInflowIdx]
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = inPt)
            drawCircle(color = Color(0xFF10B981), radius = 3.5.dp.toPx(), center = inPt)
        }

        // Active scrubber guideline and indicator points
        if (selectedMonthIndex != null && selectedMonthIndex in ptsInflow.indices) {
            val scrubX = selectedMonthIndex * stepX
            drawLine(
                color = AccentPurple.copy(alpha = 0.7f),
                start = Offset(scrubX, 0f),
                end = Offset(scrubX, h),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
            )

            val inPoint = ptsInflow[selectedMonthIndex]
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = inPoint)
            drawCircle(color = Color(0xFF10B981), radius = 4.dp.toPx(), center = inPoint)

            val burnPoint = ptsBurn[selectedMonthIndex]
            drawCircle(color = Color.White, radius = 6.dp.toPx(), center = burnPoint)
            drawCircle(color = Color(0xFF8B5CF6), radius = 4.dp.toPx(), center = burnPoint)
        }
    }
}
