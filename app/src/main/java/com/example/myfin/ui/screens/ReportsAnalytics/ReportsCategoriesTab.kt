package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.theme.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

@Composable
fun ReportsCategoriesTab(
    userProfileCurrency: String,
    totalExpenses: Double,
    totalAssets: Double,
    corporateOutlays: Double,
    corporateReimbursements: Double,
    transactions: List<TransactionEntity>,
    allTransactions: List<TransactionEntity>,
    selectedTimeRange: TimeRangeFilter,
    isDiscreet: Boolean,
    onOpenMetricInfo: (ChartMetricInfo) -> Unit
) {
    val isPersonalExpense = remember {
        { tx: TransactionEntity ->
            tx.type == TransactionType.EXPENSE && !tx.category.equals("Work & Professional", ignoreCase = true)
        }
    }

    val categoryExpenses = remember(transactions) {
        transactions
            .filter { isPersonalExpense(it) }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    val categorySurges = remember(allTransactions) {
        val now = Calendar.getInstance()
        val curM = now.get(Calendar.MONTH) + 1
        val curY = now.get(Calendar.YEAR)
        now.add(Calendar.MONTH, -1)
        val prevM = now.get(Calendar.MONTH) + 1
        val prevY = now.get(Calendar.YEAR)

        val txCal = Calendar.getInstance()
        val curMap = allTransactions.filter {
            txCal.timeInMillis = it.date
            (txCal.get(Calendar.MONTH) + 1) == curM && txCal.get(Calendar.YEAR) == curY && isPersonalExpense(it)
        }.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }

        val prevMap = allTransactions.filter {
            txCal.timeInMillis = it.date
            (txCal.get(Calendar.MONTH) + 1) == prevM && txCal.get(Calendar.YEAR) == prevY && isPersonalExpense(it)
        }.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }

        curMap.mapNotNull { (cat, curAmt) ->
            val prevAmt = prevMap[cat] ?: 0.0
            if (prevAmt > 0 && curAmt > prevAmt) {
                val growth = (((curAmt - prevAmt) / prevAmt) * 100).toInt()
                if (growth >= 15) cat to growth else null
            } else null
        }.sortedByDescending { it.second }
    }

    val needsCategories = setOf(
        "Utilities & Living Bills", "Everyday Living", "Health & Medical",
        "Family & Home Support", "Debt & Financial Obligations", "Living", "Rent", "Bills"
    )
    val needsSum = remember(transactions) {
        transactions.filter { isPersonalExpense(it) && (it.category in needsCategories || it.linkedFixedBillId != null) }.sumOf { it.amount }
    }
    val wantsSum = remember(transactions, needsSum, totalExpenses) {
        max(0.0, totalExpenses - needsSum)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 140.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        // Corporate Float Active Banner
        if (corporateOutlays > 0.0 || corporateReimbursements > 0.0) {
            val netFloat = corporateOutlays - corporateReimbursements
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "Corporate Outlays & Claims",
                                subtitle = "Business Travel Float Reconciler",
                                formula = "Net_Float = Work_Expenses_Paid - Claims_Received",
                                breakdown = "Total Outlays: $userProfileCurrency${String.format(Locale.US, "%,.0f", corporateOutlays)} | Company Refunds: $userProfileCurrency${String.format(Locale.US, "%,.0f", corporateReimbursements)}.",
                                visualElements = listOf(
                                    "Pending Claim" to "Money you paid out-of-pocket that the company owes back to you.",
                                    "Advance Held" to "Company capital sitting in your accounts, strictly ring-fenced from your living burn."
                                ),
                                advice = "Corporate expenses are ring-fenced from personal living costs so business travel never distorts your true burn rate."
                            )
                        )
                    },
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
                            imageVector = Icons.Default.WorkOutline,
                            contentDescription = null,
                            tint = Color(0xFFE57A28),
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Corporate Float Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE57A28).copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "Excluded from Burn",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE57A28),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isDiscreet) "••••" else "Outlays: $userProfileCurrency${String.format(Locale.US, "%,.0f", corporateOutlays)} | Settled: $userProfileCurrency${String.format(Locale.US, "%,.0f", corporateReimbursements)}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isDiscreet) "••••" else "${if (netFloat >= 0) "+" else ""}$userProfileCurrency${String.format(Locale.US, "%,.0f", abs(netFloat))}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.5.sp,
                            color = if (netFloat <= 0) SoftGreen else Color(0xFFE57A28)
                        )
                        Text(
                            text = if (netFloat > 0) "Claim Due" else if (netFloat < 0) "Advance Held" else "Settled",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (netFloat <= 0) SoftGreen else Color(0xFFE57A28)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Spending Matrix Radar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Spending Matrix Radar",
                            subtitle = "Multi-Axis Category Allocation",
                            formula = "Axis_Ratio = (Category_Total / Max_Category_Sum) * 100",
                            breakdown = "Evaluates personal expense density across your top 6 categories in $selectedTimeRange.",
                            visualElements = listOf(
                                "Radial Crests" to "Protruding spikes represent categories absorbing the largest share of capital.",
                                "Concentric Rings" to "Reference thresholds at 33%, 66%, and 100% of maximum spend."
                            ),
                            advice = "A balanced hexagonal shape prevents unmanaged spikes in any single category."
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Spending Matrix",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Personal Lifestyle Outflow Distribution",
                    fontSize = 11.5.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "Spending Matrix Radar",
                            subtitle = "Multi-Axis Category Allocation",
                            formula = "Radius = (Cat_Spend / Max_Spend) * Max_Radius",
                            breakdown = "Top categories: ${categoryExpenses.take(3).joinToString { "${it.first} ($userProfileCurrency${it.second.toInt()})" }}",
                            visualElements = listOf(
                                "Labeled Vertices" to "Top spending lifestyle categories.",
                                "Violet Web" to "Your realized expenditure footprint."
                            ),
                            advice = "An elongated spike on a single spoke indicates disproportionate outflow."
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            CategoryRadarWebCanvas(
                categoryExpenses = categoryExpenses.take(6)
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        // 50 / 30 / 20 Cashflow Split Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onOpenMetricInfo(
                        ChartMetricInfo(
                            title = "50 / 30 / 20 Cashflow Split",
                            subtitle = "Macro Budget Health Model",
                            formula = "Needs (50%) + Wants (30%) + SIP Wealth (20%)",
                            breakdown = "Needs: $userProfileCurrency${String.format(Locale.US, "%,.0f", needsSum)} | Wants: $userProfileCurrency${String.format(Locale.US, "%,.0f", wantsSum)} | Assets: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalAssets)}.",
                            visualElements = listOf(
                                "Red Band" to "Needs (Contractual rent, bills, groceries).",
                                "Violet Band" to "Wants (Dining, leisure, discretionary shopping).",
                                "Teal Band" to "Wealth SIPs (Mutual funds, gold, compounding assets)."
                            ),
                            advice = "Aim to contain essential survival costs within 50% to maximize monthly wealth compounding."
                        )
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Cashflow Stream Split",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(13.dp))
                }
                Text(
                    text = "Needs (50%) • Wants (30%) • SIP Assets (20%)",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SymmetricalFunnelRibbonCanvas(
            needsAmount = needsSum,
            wantsAmount = wantsSum,
            assetAmount = totalAssets,
            currency = userProfileCurrency,
            isDiscreet = isDiscreet,
            onOpenInfo = {
                onOpenMetricInfo(
                    ChartMetricInfo(
                        title = "50 / 30 / 20 Ribbon Funnel",
                        subtitle = "Relative Proportion Distribution",
                        formula = "Total = Needs + Wants + Assets",
                        breakdown = "Needs: $userProfileCurrency${String.format(Locale.US, "%,.0f", needsSum)} | Wants: $userProfileCurrency${String.format(Locale.US, "%,.0f", wantsSum)} | Assets: $userProfileCurrency${String.format(Locale.US, "%,.0f", totalAssets)}",
                        visualElements = listOf(
                            "Red Top Band" to "Essential survival commitments.",
                            "Purple Middle Band" to "Variable discretionary living.",
                            "Teal Lower Band" to "Compounding investment assets."
                        ),
                        advice = "Keep essential needs at or below 50% to ensure enough cash is available for investing."
                    )
                )
            }
        )

        if (categorySurges.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val top = categorySurges.first()
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "Velocity Surge Analysis",
                                subtitle = "Month-over-Month Category Inflation",
                                formula = "Surge % = ((This_Month - Last_Month) / Last_Month) * 100",
                                breakdown = "${top.first} spiked by +${top.second}% compared to the prior calendar month.",
                                visualElements = listOf(
                                    "Amber Badge" to "Alerts when any category grows by more than 15% in a single cycle."
                                ),
                                advice = "Audit subcategories under ${top.first} to check for one-time spikes versus recurring subscription price hikes."
                            )
                        )
                    },
                shape = RoundedCornerShape(14.dp),
                color = SoftAmber.copy(alpha = 0.12f),
                border = BorderStroke(0.7.dp, SoftAmber.copy(alpha = 0.35f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SoftAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Month-over-Month Velocity Surge", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                        val top = categorySurges.first()
                        Text("${top.first} increased by +${top.second}% vs last cycle", fontSize = 11.sp, color = TextMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Budget Consumption",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))

        if (categoryExpenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No categorized expenses in this cycle", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            categoryExpenses.take(4).forEach { (cat, amount) ->
                val ratio = if (totalExpenses > 0) (amount / totalExpenses).toFloat() else 0f
                Column(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .clickable {
                            onOpenMetricInfo(
                                ChartMetricInfo(
                                    title = "$cat Consumption",
                                    subtitle = "Share of Total Outflow",
                                    formula = "Share % = (Category_Total / Total_Expenses) * 100",
                                    breakdown = "Realized spend of $userProfileCurrency${String.format(Locale.US, "%,.0f", amount)}, absorbing ${(ratio * 100).toInt()}% of total expenses in $selectedTimeRange.",
                                    advice = "Target reducing variable expenses in your top 2 categories to free up cash for emergency reserves."
                                )
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cat,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark,
                            modifier = Modifier.weight(1f, fill = false),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isDiscreet) "•••• (${(ratio * 100).toInt()}%)" else "$userProfileCurrency${String.format(Locale.US, "%,.0f", amount)} (${(ratio * 100).toInt()}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(BorderLight.copy(alpha = 0.5f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(ratio.coerceIn(0.04f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(Brush.horizontalGradient(listOf(SoftTeal, AccentPurple)))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Itemized Category Roster",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(10.dp))

        categoryExpenses.forEach { (cat, amount) ->
            val ratio = if (totalExpenses > 0) (amount / totalExpenses) * 100 else 0.0
            val catTxs = transactions.filter { it.category.equals(cat, ignoreCase = true) && isPersonalExpense(it) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onOpenMetricInfo(
                            ChartMetricInfo(
                                title = "$cat Audit",
                                subtitle = "Category Breakdown & Sparkline",
                                formula = "Total = Σ Transactions($cat)",
                                breakdown = "Total spent: $userProfileCurrency${String.format(Locale.US, "%,.0f", amount)} across ${catTxs.size} transactions in $selectedTimeRange.",
                                visualElements = listOf(
                                    "Mini Sparkline" to "Visual trajectory of transaction sizes within this category."
                                ),
                                advice = "Review smaller, frequent charges that quietly accumulate into large monthly totals."
                            )
                        )
                    }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.2f, fill = false)) {
                    Text(
                        text = cat,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${String.format(Locale.US, "%.1f", ratio)}% total outflow", fontSize = 11.sp, color = TextMuted)
                }

                Canvas(
                    modifier = Modifier
                        .width(60.dp)
                        .height(20.dp)
                ) {
                    if (catTxs.size >= 2) {
                        val maxCatTx = catTxs.maxOf { it.amount }.coerceAtLeast(1.0)
                        val p = Path()
                        catTxs.forEachIndexed { i, tx ->
                            val px = (i.toFloat() / (catTxs.size - 1)) * size.width
                            val py = size.height * (1f - (tx.amount / maxCatTx).toFloat().coerceIn(0.1f, 0.9f))
                            if (i == 0) p.moveTo(px, py) else p.lineTo(px, py)
                        }
                        drawPath(p, color = AccentPurple, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
                    } else {
                        val p = Path().apply {
                            moveTo(0f, size.height * 0.75f)
                            lineTo(size.width, size.height * 0.4f)
                        }
                        drawPath(p, color = AccentPurple, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = if (isDiscreet) "••••" else "-$userProfileCurrency${String.format(Locale.US, "%,.0f", amount)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = SoftRed
                )
            }
            HorizontalDivider(color = BorderLight.copy(alpha = 0.35f), thickness = 0.7.dp)
        }
    }
}

@Composable
private fun CategoryRadarWebCanvas(
    categoryExpenses: List<Pair<String, Double>>
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val numAxes = 6
        val c = center
        val maxR = size.minDimension * 0.40f

        for (ring in 1..3) {
            val r = maxR * (ring / 3f)
            val ringPath = Path()
            for (i in 0 until numAxes) {
                val angle = (i * 2 * Math.PI / numAxes) - Math.PI / 2
                val x = c.x + (r * cos(angle)).toFloat()
                val y = c.y + (r * sin(angle)).toFloat()
                if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
            }
            ringPath.close()
            drawPath(ringPath, color = BorderLight.copy(alpha = 0.5f), style = Stroke(width = 0.8.dp.toPx()))
        }

        for (i in 0 until numAxes) {
            val angle = (i * 2 * Math.PI / numAxes) - Math.PI / 2
            val x = c.x + (maxR * cos(angle)).toFloat()
            val y = c.y + (maxR * sin(angle)).toFloat()
            drawLine(color = BorderLight.copy(alpha = 0.6f), start = c, end = Offset(x, y), strokeWidth = 0.8.dp.toPx())
        }

        val maxAmount = categoryExpenses.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
        val polyPath = Path()
        for (i in 0 until numAxes) {
            val amt = categoryExpenses.getOrNull(i)?.second ?: 0.0
            val ratio = if (categoryExpenses.isNotEmpty()) (amt / maxAmount).toFloat().coerceIn(0.15f, 0.95f) else 0.2f
            val r = maxR * ratio
            val angle = (i * 2 * Math.PI / numAxes) - Math.PI / 2
            val x = c.x + (r * cos(angle)).toFloat()
            val y = c.y + (r * sin(angle)).toFloat()
            if (i == 0) polyPath.moveTo(x, y) else polyPath.lineTo(x, y)
        }
        polyPath.close()

        drawPath(polyPath, color = AccentPurple.copy(alpha = 0.22f))
        drawPath(polyPath, color = AccentPurple, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun SymmetricalFunnelRibbonCanvas(
    needsAmount: Double,
    wantsAmount: Double,
    assetAmount: Double,
    currency: String,
    isDiscreet: Boolean,
    onOpenInfo: () -> Unit
) {
    val total = (needsAmount + wantsAmount + assetAmount).coerceAtLeast(1.0)
    val needsPct = ((needsAmount / total) * 100).toInt()
    val wantsPct = ((wantsAmount / total) * 100).toInt()
    val assetPct = ((assetAmount / total) * 100).toInt()

    Column(modifier = Modifier.fillMaxWidth().clickable { onOpenInfo() }) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        ) {
            val w = size.width
            val h = size.height

            val needsRatio = (needsAmount / total).toFloat().coerceIn(0.12f, 0.70f)
            val wantsRatio = (wantsAmount / total).toFloat().coerceIn(0.12f, 0.70f)

            val band1Bottom = h * needsRatio
            val band2Bottom = (h * (needsRatio + wantsRatio)).coerceAtMost(h * 0.88f)

            val path1 = Path().apply {
                moveTo(0f, 0f)
                cubicTo(w * 0.35f, 0f, w * 0.65f, 0f, w, 0f)
                lineTo(w, band1Bottom)
                cubicTo(w * 0.65f, band1Bottom, w * 0.35f, h * 0.35f, 0f, h * 0.35f)
                close()
            }
            drawPath(path1, color = SoftRed.copy(alpha = 0.85f))

            val path2 = Path().apply {
                moveTo(0f, h * 0.35f)
                cubicTo(w * 0.35f, h * 0.35f, w * 0.65f, band1Bottom, w, band1Bottom)
                lineTo(w, band2Bottom)
                cubicTo(w * 0.65f, band2Bottom, w * 0.35f, h * 0.65f, 0f, h * 0.65f)
                close()
            }
            drawPath(path2, color = AccentPurple.copy(alpha = 0.85f))

            val path3 = Path().apply {
                moveTo(0f, h * 0.65f)
                cubicTo(w * 0.35f, h * 0.65f, w * 0.65f, band2Bottom, w, band2Bottom)
                lineTo(w, h)
                cubicTo(w * 0.65f, h, w * 0.35f, h, 0f, h)
                close()
            }
            drawPath(path3, color = SoftTeal.copy(alpha = 0.85f))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftRed))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Needs $needsPct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AccentPurple))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Wants $wantsPct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Assets $assetPct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDark)
            }
        }
    }
}
