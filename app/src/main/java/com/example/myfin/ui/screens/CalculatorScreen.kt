package com.example.myfin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.pow

@Composable
fun CalculatorScreen(
    currencySymbol: String = "₹",
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Tape Calculator, 1 = SIP & EMI

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 1. TOP BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Financial Calculator",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        letterSpacing = (-0.3).sp
                    )
                    Text(
                        text = if (selectedTab == 0) "Live Expression & History Tape" else "Wealth SIP & Loan EMI Estimator",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(CardWhite)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Back to Dashboard",
                    tint = TextDark,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // 2. 2-TAB SEGMENTED SWITCHER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BorderLight.copy(alpha = 0.55f))
                .padding(4.dp)
        ) {
            listOf(
                0 to "Standard + Tape",
                1 to "SIP & EMI Estimator"
            ).forEach { (idx, title) ->
                val isSelected = selectedTab == idx
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) CardWhite else Color.Transparent)
                        .clickable { selectedTab = idx }
                        .padding(vertical = 8.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = if (isSelected) AccentPurple else TextMuted
                    )
                }
            }
        }

        // 3. TAB CONTENT
        if (selectedTab == 0) {
            StandardTapeCalculatorTab(currencySymbol = currencySymbol)
        } else {
            SipAndEmiEstimatorTab(currencySymbol = currencySymbol)
        }
    }
}

private data class CalcTapeEntry(
    val expression: String,
    val resultFormatted: String,
    val rawResult: Double
)

@Composable
private fun StandardTapeCalculatorTab(currencySymbol: String) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var expression by rememberSaveable { mutableStateOf("") }
    val tapeHistory = remember { mutableStateListOf<CalcTapeEntry>() }

    val liveResult = remember(expression) {
        if (expression.isBlank()) null else evaluateMathExpression(expression)
    }

    val formattedResult = remember(liveResult) {
        if (liveResult == null || liveResult.isNaN() || liveResult.isInfinite()) {
            "0"
        } else {
            formatCleanNumber(liveResult)
        }
    }

    fun appendToken(token: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val operators = setOf('+', '-', '×', '÷', '%')
        if (token.length == 1 && token[0] in operators) {
            if (expression.isEmpty()) {
                if (token == "-") expression = "-"
                return
            }
            val lastChar = expression.last()
            if (lastChar in operators) {
                expression = expression.dropLast(1) + token
                return
            }
        }
        expression += token
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // DISPLAY & TAPE CARD
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .shadow(2.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = CardWhite,
            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Tape History Header + List
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CALCULATION TAPE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = TextMuted,
                            letterSpacing = 0.6.sp
                        )
                        if (tapeHistory.isNotEmpty()) {
                            Text(
                                text = "Clear Tape",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftRed,
                                modifier = Modifier.clickable { tapeHistory.clear() }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (tapeHistory.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Recent calculations will appear here.\nTap any past result to reuse it.",
                                fontSize = 11.5.sp,
                                color = TextMuted.copy(alpha = 0.65f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(tapeHistory) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CanvasLight)
                                        .clickable {
                                            val cleanVal = if (item.rawResult % 1.0 == 0.0) {
                                                item.rawResult.toLong().toString()
                                            } else item.rawResult.toString()
                                            appendToken(cleanVal)
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.expression,
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "= $currencySymbol${item.resultFormatted}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPurple
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = BorderLight.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Active Expression & Live Total
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = expression.ifBlank { "0" },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted,
                        textAlign = TextAlign.End,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val rawCopy = if (liveResult != null && !liveResult.isNaN()) {
                                    if (liveResult % 1.0 == 0.0) liveResult.toLong().toString() else String.format(Locale.US, "%.2f", liveResult)
                                } else "0"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Calc Result", rawCopy))
                                Toast.makeText(context, "Copied $rawCopy to clipboard", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy result",
                            tint = AccentPurple.copy(alpha = 0.7f),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "$currencySymbol$formattedResult",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            letterSpacing = (-0.8).sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // NUMERIC & OPERATOR KEYPAD
        val keypadRows = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("00", "0", ".", "=")
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keypadRows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { key ->
                        val isOperator = key in setOf("÷", "×", "-", "+", "=")
                        val isAction = key in setOf("AC", "⌫", "%")

                        val bgColor = when {
                            key == "=" -> AccentPurple
                            isOperator -> AccentPurple.copy(alpha = 0.12f)
                            isAction -> BorderLight.copy(alpha = 0.65f)
                            else -> CardWhite
                        }
                        val contentColor = when {
                            key == "=" -> Color.White
                            isOperator -> AccentPurple
                            key == "AC" -> SoftRed
                            else -> TextDark
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    when (key) {
                                        "AC" -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            expression = ""
                                        }
                                        "⌫" -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            if (expression.isNotEmpty()) {
                                                expression = expression.dropLast(1)
                                            }
                                        }
                                        "=" -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            if (liveResult != null && !liveResult.isNaN() && expression.isNotBlank()) {
                                                tapeHistory.add(
                                                    0,
                                                    CalcTapeEntry(
                                                        expression = expression,
                                                        resultFormatted = formattedResult,
                                                        rawResult = liveResult
                                                    )
                                                )
                                                expression = if (liveResult % 1.0 == 0.0) {
                                                    liveResult.toLong().toString()
                                                } else {
                                                    String.format(Locale.US, "%.2f", liveResult)
                                                }
                                            }
                                        }
                                        else -> appendToken(key)
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = bgColor,
                            border = BorderStroke(
                                0.7.dp,
                                if (key == "=") AccentPurple else BorderLight.copy(alpha = 0.8f)
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (key == "⌫") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Backspace",
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 19.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = contentColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SipAndEmiEstimatorTab(currencySymbol: String) {
    var isSipMode by rememberSaveable { mutableStateOf(true) }

    // SIP States
    var monthlySip by rememberSaveable { mutableFloatStateOf(10000f) }
    var sipReturnRate by rememberSaveable { mutableFloatStateOf(12f) }
    var sipYears by rememberSaveable { mutableFloatStateOf(10f) }

    // EMI States
    var loanPrincipal by rememberSaveable { mutableFloatStateOf(500000f) }
    var loanInterestRate by rememberSaveable { mutableFloatStateOf(9.5f) }
    var loanTenureYears by rememberSaveable { mutableFloatStateOf(5f) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Mode Pill Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = isSipMode,
                onClick = { isSipMode = true },
                label = { Text("SIP Wealth Compounder", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = !isSipMode,
                onClick = { isSipMode = false },
                label = { Text("Loan EMI Calculator", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                modifier = Modifier.weight(1f)
            )
        }

        if (isSipMode) {
            val months = (sipYears.toInt() * 12).coerceAtLeast(1)
            val monthlyRate = (sipReturnRate / 12.0) / 100.0
            val totalInvested = monthlySip.toDouble() * months
            val maturityValue = if (monthlyRate == 0.0) {
                totalInvested
            } else {
                monthlySip * (((1 + monthlyRate).pow(months.toDouble()) - 1) / monthlyRate) * (1 + monthlyRate)
            }
            val wealthGained = (maturityValue - totalInvested).coerceAtLeast(0.0)

            // Hero Summary Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF3730A3)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF4338CA), Color(0xFF312E81))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Text(
                        text = "PROJECTED MATURITY VALUE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFC7D2FE),
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", maturityValue)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EstimatorMetricBox(
                            label = "Total Invested",
                            value = "$currencySymbol${String.format(Locale.US, "%,.0f", totalInvested)}",
                            tint = Color(0xFF67E8F9),
                            modifier = Modifier.weight(1f)
                        )
                        EstimatorMetricBox(
                            label = "Est. Wealth Gained",
                            value = "+$currencySymbol${String.format(Locale.US, "%,.0f", wealthGained)}",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Sliders Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SliderControlRow(
                        title = "Monthly SIP Investment",
                        valueDisplay = "$currencySymbol${String.format(Locale.US, "%,.0f", monthlySip)}",
                        value = monthlySip,
                        valueRange = 500f..150000f,
                        onValueChange = { monthlySip = (it / 500f).toInt() * 500f }
                    )
                    SliderControlRow(
                        title = "Expected Annual Return (CAGR)",
                        valueDisplay = "${String.format(Locale.US, "%.1f", sipReturnRate)}%",
                        value = sipReturnRate,
                        valueRange = 4f..25f,
                        onValueChange = { sipReturnRate = (it * 2).toInt() / 2f }
                    )
                    SliderControlRow(
                        title = "Investment Horizon",
                        valueDisplay = "${sipYears.toInt()} Years",
                        value = sipYears,
                        valueRange = 1f..35f,
                        onValueChange = { sipYears = it.toInt().toFloat() }
                    )
                }
            }
        } else {
            // Loan EMI Calculation
            val months = (loanTenureYears.toInt() * 12).coerceAtLeast(1)
            val monthlyRate = (loanInterestRate / 12.0) / 100.0
            val emi = if (monthlyRate == 0.0) {
                loanPrincipal.toDouble() / months
            } else {
                (loanPrincipal * monthlyRate * (1 + monthlyRate).pow(months.toDouble())) /
                        ((1 + monthlyRate).pow(months.toDouble()) - 1)
            }
            val totalPayable = emi * months
            val totalInterest = (totalPayable - loanPrincipal).coerceAtLeast(0.0)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(22.dp)),
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0F172A)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "MONTHLY LOAN EMI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.6.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", emi)} / mo",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EstimatorMetricBox(
                            label = "Total Interest",
                            value = "$currencySymbol${String.format(Locale.US, "%,.0f", totalInterest)}",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.weight(1f)
                        )
                        EstimatorMetricBox(
                            label = "Total Payable",
                            value = "$currencySymbol${String.format(Locale.US, "%,.0f", totalPayable)}",
                            tint = Color(0xFFC7D2FE),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    SliderControlRow(
                        title = "Loan Principal Amount",
                        valueDisplay = "$currencySymbol${String.format(Locale.US, "%,.0f", loanPrincipal)}",
                        value = loanPrincipal,
                        valueRange = 25000f..5000000f,
                        onValueChange = { loanPrincipal = (it / 25000f).toInt() * 25000f }
                    )
                    SliderControlRow(
                        title = "Annual Interest Rate",
                        valueDisplay = "${String.format(Locale.US, "%.1f", loanInterestRate)}%",
                        value = loanInterestRate,
                        valueRange = 5f..24f,
                        onValueChange = { loanInterestRate = (it * 2).toInt() / 2f }
                    )
                    SliderControlRow(
                        title = "Loan Tenure",
                        valueDisplay = "${loanTenureYears.toInt()} Years (${loanTenureYears.toInt() * 12} mos)",
                        value = loanTenureYears,
                        valueRange = 1f..30f,
                        onValueChange = { loanTenureYears = it.toInt().toFloat() }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun EstimatorMetricBox(
    label: String,
    value: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.16f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.75f))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
private fun SliderControlRow(
    title: String,
    valueDisplay: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AccentPurple.copy(alpha = 0.12f)
            ) {
                Text(
                    text = valueDisplay,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = AccentPurple,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = AccentPurple,
                activeTrackColor = AccentPurple
            )
        )
    }
}

private fun formatCleanNumber(value: Double): String {
    return if (value % 1.0 == 0.0) {
        String.format(Locale.US, "%,d", value.toLong())
    } else {
        String.format(Locale.US, "%,.2f", value)
    }
}

/**
 * Lightweight, safe recursive-descent arithmetic evaluator supporting +, -, ×, ÷, and %
 */
private fun evaluateMathExpression(expr: String): Double? {
    val sanitized = expr
        .replace("×", "*")
        .replace("÷", "/")
        .replace(",", "")
        .trim()
        .trimEnd('+', '-', '*', '/', '.')

    if (sanitized.isEmpty()) return null

    return try {
        object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < sanitized.length) sanitized[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < sanitized.length) return Double.NaN
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> {
                            val divisor = parseFactor()
                            x = if (divisor == 0.0) Double.NaN else x / divisor
                        }
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()

                var x: Double
                val startPos = pos
                if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = sanitized.substring(startPos, pos).toDouble()
                } else {
                    return Double.NaN
                }

                if (eat('%'.code)) {
                    x /= 100.0
                }
                return x
            }
        }.parse()
    } catch (_: Exception) {
        null
    }
}
