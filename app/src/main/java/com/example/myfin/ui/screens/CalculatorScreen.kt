package com.example.myfin.ui.screens

import android.app.Activity
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

@Composable
fun CalculatorScreen(
    currencySymbol: String = "₹",
    onOpenDrawer: () -> Unit,
    onNavigateBack: () -> Unit = {}
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Tape Calculator, 1 = SIP & EMI

    // Sync status bar icons to white for the dark Midnight cockpit header
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(Unit) {
            val window = (view.context as? Activity)?.window
            val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
            insetsController?.isAppearanceLightStatusBars = false
            onDispose {
                insetsController?.isAppearanceLightStatusBars = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Neutral900)
    ) {
        // 1. MIDNIGHT COCKPIT TOP BAR + SEGMENTED SWITCHER
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Glassmorphic Mode Switcher Pill in Header
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.10f))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        0 to "Calculator",
                        1 to "SIP & EMI"
                    ).forEach { (idx, title) ->
                        val isSelected = selectedTab == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(17.dp))
                                .background(if (isSelected) Primary600 else Color.Transparent)
                                .clickable { selectedTab = idx }
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.White else Neutral400
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.10f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        // 2. TAB BODY
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
            formatCleanNumber(abs(liveResult))
        }
    }

    fun appendToken(token: String) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val binaryOps = setOf('+', '-', '×', '÷')
        if (token.length == 1 && token[0] in binaryOps) {
            if (expression.isEmpty()) {
                if (token == "-") expression = "-"
                return
            }
            val lastChar = expression.last()
            if (lastChar in binaryOps) {
                expression = expression.dropLast(1) + token
                return
            }
        } else if (token == "%") {
            if (expression.isEmpty() || expression.last() in binaryOps || expression.last() == '%') return
        } else if (token == ".") {
            val lastNumberSegment = expression.split('+', '-', '×', '÷').lastOrNull() ?: ""
            if (lastNumberSegment.contains('.')) return
            if (lastNumberSegment.isEmpty()) {
                expression += "0."
                return
            }
        }
        expression += token
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 1. DARK COCKPIT READOUT & FLOATING HISTORY TAPE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.42f)
                .padding(horizontal = 22.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = Neutral500,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "HISTORY TAPE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Neutral500,
                        letterSpacing = 0.8.sp
                    )
                }

                if (tapeHistory.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Error500.copy(alpha = 0.16f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { tapeHistory.clear() }
                    ) {
                        Text(
                            text = "Clear Tape",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Error100,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            // Scrollable Tape Pills
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp)
            ) {
                if (tapeHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = "Tap '=' to log calculations to tape • Tap any entry to reuse",
                            fontSize = 11.sp,
                            color = Neutral600,
                            textAlign = TextAlign.End
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        reverseLayout = true,
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.Bottom)
                    ) {
                        items(tapeHistory) { item ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.White.copy(alpha = 0.06f),
                                border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.10f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val cleanVal = if (item.rawResult % 1.0 == 0.0) {
                                            item.rawResult.toLong().toString()
                                        } else item.rawResult.toString()

                                        val binaryOps = setOf('+', '-', '×', '÷')
                                        if (expression.isEmpty() || expression.last() in binaryOps) {
                                            expression += cleanVal
                                        } else {
                                            // Replace active trailing operand
                                            val lastOpIdx = expression.indexOfLast { it in binaryOps }
                                            expression = if (lastOpIdx == -1) cleanVal else expression.substring(0, lastOpIdx + 1) + cleanVal
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.expression,
                                        fontSize = 12.sp,
                                        color = Neutral400,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "= ${if (item.rawResult < 0) "-" else ""}$currencySymbol${item.resultFormatted}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Primary300
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Equation & Giant Result Readout
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = expression.ifBlank { "0" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Neutral400,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val rawCopy = if (liveResult != null && !liveResult.isNaN()) {
                                    if (liveResult % 1.0 == 0.0) liveResult.toLong().toString() else String.format(Locale.US, "%.2f", liveResult)
                                } else "0"
                                clipboard.setPrimaryClip(ClipData.newPlainText("Calc Result", rawCopy))
                                Toast.makeText(context, "Copied ${if ((liveResult ?: 0.0) < 0) "-" else ""}$currencySymbol$formattedResult", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Primary300,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Copy",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Primary300
                            )
                        }
                    }

                    Text(
                        text = "${if ((liveResult ?: 0.0) < 0) "-" else ""}$currencySymbol$formattedResult",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-1.0).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 2. ELEVATED TACTILE KEYPAD SHEET
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            color = CanvasLight,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(36.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(Neutral300)
                    )
                }

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
                        .weight(1f)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    keypadRows.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { key ->
                                val isEquals = key == "="
                                val isOperator = key in setOf("÷", "×", "-", "+")
                                val isClear = key == "AC"
                                val isUtil = key in setOf("⌫", "%")

                                val keyBg = when {
                                    isEquals -> Primary600
                                    isOperator -> Primary50
                                    isClear -> Error100
                                    isUtil -> Neutral100
                                    else -> CardWhite
                                }

                                val keyContentColor = when {
                                    isEquals -> Color.White
                                    isOperator -> Primary700
                                    isClear -> Error700
                                    isUtil -> Neutral600
                                    else -> TextDark
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .shadow(if (isEquals) 4.dp else 1.dp, RoundedCornerShape(20.dp))
                                        .clip(RoundedCornerShape(20.dp))
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
                                    shape = RoundedCornerShape(20.dp),
                                    color = keyBg,
                                    border = BorderStroke(
                                        0.8.dp,
                                        if (isEquals) Primary700
                                        else if (isOperator) Primary200
                                        else BorderLight
                                    )
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (key == "⌫") {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                                contentDescription = "Backspace",
                                                tint = keyContentColor,
                                                modifier = Modifier.size(21.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                fontSize = if (isOperator || isEquals) 22.sp else 20.sp,
                                                fontWeight = FontWeight.Black,
                                                color = keyContentColor
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

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 6.dp),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = CanvasLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Neutral100)
                    .padding(4.dp)
            ) {
                listOf(
                    true to "SIP Wealth Compounder",
                    false to "Loan EMI Calculator"
                ).forEach { (sipMode, label) ->
                    val selected = isSipMode == sipMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) CardWhite else Color.Transparent)
                            .clickable { isSipMode = sipMode }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) Primary600 else TextMuted
                        )
                    }
                }
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
                val investedRatio = if (maturityValue > 0) (totalInvested / maturityValue).toFloat().coerceIn(0.05f, 0.95f) else 0.5f

                // Hero Card (Royal Cobalt Blue Gradient)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Primary800
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(Primary700, Primary900)
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PROJECTED MATURITY VALUE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Primary200,
                                letterSpacing = 0.8.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.15f)
                            ) {
                                val multiplier = if (totalInvested > 0) maturityValue / totalInvested else 1.0
                                Text(
                                    text = "${String.format(Locale.US, "%.1fx", multiplier)} Return",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Success100,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.0f", maturityValue)}",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.8).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(investedRatio)
                                    .fillMaxHeight()
                                    .background(Primary300)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f - investedRatio)
                                    .fillMaxHeight()
                                    .background(Success500)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EstimatorMetricBox(
                                label = "Principal Invested",
                                value = "$currencySymbol${String.format(Locale.US, "%,.0f", totalInvested)}",
                                tint = Primary200,
                                modifier = Modifier.weight(1f)
                            )
                            EstimatorMetricBox(
                                label = "Est. Wealth Gained",
                                value = "+$currencySymbol${String.format(Locale.US, "%,.0f", wealthGained)}",
                                tint = Success100,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Controls Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SliderControlRow(
                            title = "Monthly SIP Deposit",
                            valueDisplay = "$currencySymbol${String.format(Locale.US, "%,.0f", monthlySip)}",
                            value = monthlySip,
                            valueRange = 500f..150000f,
                            onValueChange = { monthlySip = (it / 500f).toInt() * 500f }
                        )
                        SliderControlRow(
                            title = "Expected Return Rate (p.a.)",
                            valueDisplay = "${String.format(Locale.US, "%.1f", sipReturnRate)}%",
                            value = sipReturnRate,
                            valueRange = 4f..25f,
                            onValueChange = { sipReturnRate = (it * 2).toInt() / 2f }
                        )
                        SliderControlRow(
                            title = "Investment Duration",
                            valueDisplay = "${sipYears.toInt()} Years",
                            value = sipYears,
                            valueRange = 1f..35f,
                            onValueChange = { sipYears = it.toInt().toFloat() }
                        )
                    }
                }
            } else {
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
                val principalRatio = if (totalPayable > 0) (loanPrincipal / totalPayable).toFloat().coerceIn(0.05f, 0.95f) else 0.5f

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = Neutral900
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "MONTHLY LOAN EMI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Neutral400,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.0f", emi)} / mo",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = (-0.8).sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(principalRatio)
                                    .fillMaxHeight()
                                    .background(Primary400)
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f - principalRatio)
                                    .fillMaxHeight()
                                    .background(Error500)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EstimatorMetricBox(
                                label = "Total Interest",
                                value = "$currencySymbol${String.format(Locale.US, "%,.0f", totalInterest)}",
                                tint = Error100,
                                modifier = Modifier.weight(1f)
                            )
                            EstimatorMetricBox(
                                label = "Total Payable",
                                value = "$currencySymbol${String.format(Locale.US, "%,.0f", totalPayable)}",
                                tint = Primary200,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
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
        }
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
        color = Color.White.copy(alpha = 0.09f),
        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
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
                color = Primary50
            ) {
                Text(
                    text = valueDisplay,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Black,
                    color = Primary700,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Primary600,
                activeTrackColor = Primary600
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

private fun evaluateMathExpression(expr: String): Double? {
    val sanitized = expr
        .replace("×", "*")
        .replace("÷", "/")
        .replace(",", "")
        .trim()

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
                        eat('+'.code) -> {
                            val term = parseTerm()
                            // If term is percentage-based (e.g. 1000 + 18%), compute as x + (x * pct)
                            x += if (sanitized.getOrNull(pos - 1) == '%') x * term else term
                        }
                        eat('-'.code) -> {
                            val term = parseTerm()
                            x -= if (sanitized.getOrNull(pos - 1) == '%') x * term else term
                        }
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> {
                            val factor = parseFactor()
                            x *= factor
                        }
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
