package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.example.myfin.ui.YearlyUiState
import com.example.myfin.ui.theme.*
import java.util.Locale

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
        contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)
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
                                text = if (isDiscreetMode) "••••" else "${q.savingsRate.toInt()}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = if (q.netSurplus >= 0) SoftTeal else SoftRed
                            )
                            Text(
                                text = if (isDiscreetMode) "••••" else "${currencySymbol}${(q.netSurplus / 1000).toInt()}k",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
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
    val monthlyAvgBurn = if (annualExpenses > 0) annualExpenses / 12.0 else 0.0
    val netRetained = annualIncome - annualExpenses
    val peakMonth = yearlyMonths.maxByOrNull { it.lifestyleExpenses }

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

            Spacer(modifier = Modifier.height(16.dp))

            DualWaveCanvas(
                yearlyMonths = yearlyMonths,
                currencySymbol = currencySymbol,
                isDiscreet = isDiscreet,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Personal Inflow", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Bold)

                Spacer(modifier = Modifier.width(24.dp))

                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF8B5CF6)))
                Spacer(modifier = Modifier.width(5.dp))
                Text("Lifestyle Burn", fontSize = 11.sp, color = TextDark, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(18.dp))
            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Monthly Avg Burn", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", monthlyAvgBurn)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark
                    )
                    Text(
                        text = if (peakMonth != null) "Peak: ${peakMonth.monthName}" else "",
                        fontSize = 10.sp,
                        color = SoftRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Operating Surplus", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", netRetained)}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (netRetained >= 0) SoftTeal else SoftRed
                    )
                    Text(
                        text = "Net Cash Retained",
                        fontSize = 10.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DualWaveCanvas(
    yearlyMonths: List<YearlyMonthData>,
    currencySymbol: String,
    isDiscreet: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = 12
        val stepX = w / (count - 1).toFloat()

        val personalInflows = yearlyMonths.map { it.netSavings + it.lifestyleExpenses + it.assets }
        val personalBurns = yearlyMonths.map { it.lifestyleExpenses }

        val maxVal = (personalInflows + personalBurns).maxOrNull()?.coerceAtLeast(100.0) ?: 100.0

        for (i in 1..3) {
            val y = h * (i / 4f)
            drawLine(
                color = Color(0xFFF1F5F9),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1.dp.toPx()
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
                    colors = listOf(gradientStart.copy(alpha = 0.28f), Color.Transparent),
                    startY = 0f,
                    endY = h
                )
            )

            drawPath(
                path = path,
                color = strokeColor,
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawSmoothLineAndArea(ptsInflow, Color(0xFF10B981), Color(0xFF10B981))
        drawSmoothLineAndArea(ptsBurn, Color(0xFF8B5CF6), Color(0xFF8B5CF6))

        val peakBurnIdx = personalBurns.indices.maxByOrNull { personalBurns[it] } ?: 0
        val peakInflowIdx = personalInflows.indices.maxByOrNull { personalInflows[it] } ?: 0

        if (ptsBurn.isNotEmpty()) {
            val peakPt = ptsBurn[peakBurnIdx]
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = peakPt)
            drawCircle(color = Color(0xFF8B5CF6), radius = 3.5.dp.toPx(), center = peakPt)

            val inPt = ptsInflow[peakInflowIdx]
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = inPt)
            drawCircle(color = Color(0xFF10B981), radius = 3.5.dp.toPx(), center = inPt)
        }
    }
}
