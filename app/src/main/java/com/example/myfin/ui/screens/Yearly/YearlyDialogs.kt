package com.example.myfin.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.YearlyMonthData
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs

val YEARLY_MONTH_NAMES = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

data class QuarterlyMetrics(
    val quarterLabel: String,
    val quarterIndex: Int,
    val totalIncome: Double,
    val totalExpenses: Double,
    val totalAssets: Double,
    val netSurplus: Double,
    val savingsRate: Double
)

data class CategoryAnnualTrajectory(
    val categoryName: String,
    val annualTotal: Double,
    val percentageOfTotal: Double,
    val monthlyAmounts: List<Double>,
    val peakMonthIndex: Int,
    val peakMonthAmount: Double
)

data class GraphExplanationGuide(
    val title: String,
    val subtitle: String,
    val whatItShows: String,
    val visualElements: List<Pair<String, String>>,
    val whyItMatters: String,
    val actionableTip: String
)

@Composable
fun QuickMetricTile(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    tint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Black, color = tint)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphExplanationBottomSheet(
    guide: GraphExplanationGuide,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(guide.title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
                    Text(guide.subtitle, fontSize = 12.sp, color = TextMuted)
                }
                Surface(
                    shape = CircleShape,
                    color = AccentPurple.copy(alpha = 0.12f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("What this graph shows", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(4.dp))
            Text(guide.whatItShows, fontSize = 12.sp, color = TextDark.copy(alpha = 0.85f), lineHeight = 18.sp)

            Spacer(modifier = Modifier.height(16.dp))

            Text("Visual Guide", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                guide.visualElements.forEach { (label, desc) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentPurple)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(label, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(desc, fontSize = 11.sp, color = TextMuted, lineHeight = 15.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = CanvasLight,
                border = BorderStroke(0.7.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Why this matters for your wealth", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(guide.whyItMatters, fontSize = 11.5.sp, color = TextDark.copy(alpha = 0.8f), lineHeight = 16.sp)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Pro Tip: ${guide.actionableTip}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentPurple,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextDark)
            ) {
                Text("Got it", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectedMonthBottomSheet(
    mData: YearlyMonthData,
    selectedYear: Int,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    onDismiss: () -> Unit,
    onOpenMonth: (Int) -> Unit
) {
    val monthPersonalIncome = mData.netSavings + mData.lifestyleExpenses + mData.assets

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("${mData.monthName} $selectedYear", fontWeight = FontWeight.Black, fontSize = 20.sp, color = TextDark)
                    Text(if (mData.isFuture) "Planned Cycle" else "Completed Accounting Cycle", fontSize = 11.5.sp, color = TextMuted)
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (mData.netSavings >= 0) SoftGreen.copy(alpha = 0.14f) else SoftRed.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isDiscreetMode) "••••" else if (mData.netSavings >= 0) "+${currencySymbol}${String.format(Locale.US, "%,.0f", mData.netSavings)}" else "-${currencySymbol}${String.format(Locale.US, "%,.0f", abs(mData.netSavings))}",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = if (mData.netSavings >= 0) SoftGreen else SoftRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "Personal Inflow",
                    value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", monthPersonalIncome)}",
                    tint = SoftGreen
                )
                QuickMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "Lifestyle Burn",
                    value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", mData.lifestyleExpenses)}",
                    tint = AccentPurple
                )
                QuickMetricTile(
                    modifier = Modifier.weight(1f),
                    label = "Assets SIP",
                    value = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", mData.assets)}",
                    tint = SoftTeal
                )
            }

            if (mData.workExpenses > 0.0 || mData.corporateReimbursements > 0.0) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFE57A28).copy(alpha = 0.08f),
                    border = BorderStroke(0.6.dp, Color(0xFFE57A28).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkOutline, contentDescription = null, tint = Color(0xFFE57A28), modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Corporate Float Active", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE57A28))
                        }
                        Text(
                            text = if (isDiscreetMode) "••••" else "Outlay: ${currencySymbol}${String.format(Locale.US, "%,.0f", mData.workExpenses)} | Claims: ${currencySymbol}${String.format(Locale.US, "%,.0f", mData.corporateReimbursements)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE57A28)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Top Expenses in ${mData.monthName}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(modifier = Modifier.height(8.dp))

            val topMonthCategories = mData.transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }
                .toList()
                .sortedByDescending { it.second }
                .take(3)

            if (topMonthCategories.isEmpty()) {
                Text("No recorded expenses for this month.", fontSize = 11.5.sp, color = TextMuted)
            } else {
                topMonthCategories.forEach { (cat, amt) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat, fontSize = 12.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (isDiscreetMode) "••••" else "${currencySymbol}${String.format(Locale.US, "%,.0f", amt)}",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPurple
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onDismiss()
                    onOpenMonth(mData.monthIndex)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TextDark)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Open ${mData.monthName} Monthly Dashboard", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
