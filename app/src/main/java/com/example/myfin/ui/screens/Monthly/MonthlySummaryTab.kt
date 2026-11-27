package com.example.myfin.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.TransactionType
import com.example.myfin.data.UserProfile
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.CategoryPerformance
import com.example.myfin.ui.FilterCriteria
import com.example.myfin.ui.MonthlyUiState
import com.example.myfin.ui.components.NowBarAlert
import com.example.myfin.ui.components.VaultNowBar
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

private fun formatOrdinalDay(day: Int): String {
    val suffix = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$day$suffix"
}

@Composable
fun MonthlySummaryTab(
    viewModel: BudgetViewModel,
    uiState: MonthlyUiState,
    userProfile: UserProfile,
    filterCriteria: FilterCriteria,
    isDiscreetMode: Boolean,
    isMidnightTheme: Boolean = false,
    isCurrentMonth: Boolean,
    isPastMonth: Boolean,
    dismissedWaterfallMonth: Int,
    dismissedSweepMonth: Int,
    operatingAccountName: String,
    commitmentsAccountName: String,
    fortressAccountName: String,
    onOpenStsInfo: () -> Unit,
    onOpenThreePillarInfo: () -> Unit,
    onOpenFortressInfo: () -> Unit,
    onOpenBalanceFlowInfo: (Int) -> Unit,
    onOpenTransferSheet: () -> Unit,
    onDismissWaterfall: () -> Unit,
    onDismissSweep: () -> Unit,
    onNavigateToLedgerWithFilter: (TransactionType) -> Unit,
    onNavigateToCommitments: () -> Unit,
    onOpenDrawer: () -> Unit = {},
    onOpenAddSheet: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedMatrixType by remember { mutableStateOf<TransactionType?>(null) }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    val isHealthy = uiState.metrics.safeToSpend > 0

    val profileBitmap by produceState<ImageBitmap?>(initialValue = null, userProfile.profileImageUri) {
        value = withContext(Dispatchers.IO) {
            try {
                val uriStr = userProfile.profileImageUri?.takeIf { it.isNotBlank() } ?: return@withContext null
                context.contentResolver.openInputStream(Uri.parse(uriStr))?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val paydayPlan = uiState.paydaySuggestion
    val showWaterfallPrompt = remember(paydayPlan, uiState.selectedMonth, dismissedWaterfallMonth, isCurrentMonth) {
        isCurrentMonth && paydayPlan != null && (paydayPlan.toCommitments > 0.0 || paydayPlan.totalToFortress > 0.0) && dismissedWaterfallMonth != uiState.selectedMonth
    }

    val monthEndSweepPlan = uiState.monthEndSweepSuggestion
    val showMonthEndSweepPrompt = remember(monthEndSweepPlan, uiState.selectedMonth, dismissedSweepMonth, isCurrentMonth) {
        isCurrentMonth && monthEndSweepPlan != null && monthEndSweepPlan.sweepAmount > 0.0 && dismissedSweepMonth != uiState.selectedMonth
    }

    val nowBarAlerts = remember(
        uiState.commitmentsShortfall,
        uiState.isRolloverBannerVisible,
        uiState.rolloverBannerMessage,
        showWaterfallPrompt,
        paydayPlan,
        showMonthEndSweepPrompt,
        monthEndSweepPlan,
        userProfile.currencySymbol
    ) {
        buildList {
            if (uiState.commitmentsShortfall.isShortfall) {
                val shortfall = uiState.commitmentsShortfall
                val dueText = shortfall.earliestDueDay?.let { " by ${formatOrdinalDay(it)}" } ?: ""
                add(
                    NowBarAlert(
                        id = "shortfall",
                        icon = Icons.Default.WarningAmber,
                        iconTint = Error500,
                        iconBg = Error100,
                        title = "Shortfall • ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", shortfall.shortfallAmount)}",
                        subtitle = "Transfer to ${shortfall.affectedAccountName}$dueText to protect MAB & avoid AutoPay bounce.",
                        actionLabel = "Transfer",
                        actionColor = Error500,
                        onAction = onOpenTransferSheet
                    )
                )
            }

            if (uiState.isRolloverBannerVisible) {
                add(
                    NowBarAlert(
                        id = "rollover",
                        icon = Icons.Default.SyncAlt,
                        iconTint = Primary600,
                        iconBg = Primary100,
                        title = "AutoPay Bills Scheduled",
                        subtitle = uiState.rolloverBannerMessage.ifBlank { "Recurring AutoPay bills and budget limits scheduled for next cycle." },
                        actionLabel = "Dismiss",
                        actionColor = Primary600,
                        onAction = { viewModel.dismissRolloverBanner() },
                        onDismiss = { viewModel.dismissRolloverBanner() }
                    )
                )
            }

            if (showWaterfallPrompt && paydayPlan != null) {
                val parts = buildList {
                    if (paydayPlan.toCommitments > 0.0) add("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", paydayPlan.toCommitments)} to Bills")
                    if (paydayPlan.totalToFortress > 0.0) add("${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", paydayPlan.totalToFortress)} to Fortress")
                }.joinToString(" & ")

                add(
                    NowBarAlert(
                        id = "payday",
                        icon = Icons.Default.AccountBalanceWallet,
                        iconTint = SoftTeal,
                        iconBg = SoftTealLight,
                        title = "Payday Allocation Ready",
                        subtitle = if (parts.isNotBlank()) "Allocate $parts." else "Living cushion preserved in Operating.",
                        actionLabel = "Allocate",
                        actionColor = SoftTeal,
                        onAction = {
                            viewModel.applyPaydayAllocation(
                                plan = paydayPlan,
                                operatingAccount = operatingAccountName,
                                commitmentsAccount = commitmentsAccountName,
                                fortressAccount = fortressAccountName
                            )
                            onDismissWaterfall()
                            Toast.makeText(context, "Payday allocation executed!", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = onDismissWaterfall
                    )
                )
            }

            if (showMonthEndSweepPrompt && monthEndSweepPlan != null) {
                add(
                    NowBarAlert(
                        id = "sweep",
                        icon = Icons.Default.Savings,
                        iconTint = Success500,
                        iconBg = Success100,
                        title = "Wealth Sweep • ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", monthEndSweepPlan.sweepAmount)}",
                        subtitle = "Sweep unspent surplus to Fortress Extra.",
                        actionLabel = "Sweep",
                        actionColor = Success500,
                        onAction = {
                            viewModel.applyMonthEndSweep(
                                plan = monthEndSweepPlan,
                                operatingAccount = operatingAccountName,
                                fortressAccount = fortressAccountName
                            )
                            onDismissSweep()
                            Toast.makeText(context, "Surplus swept to Fortress Extra!", Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = onDismissSweep
                    )
                )
            }
        }
    }

    val activeMatrix = remember(uiState.categories, selectedMatrixType) {
        uiState.categories
            .filter { cat ->
                cat.category.isNotBlank() &&
                        cat.type != TransactionType.TRANSFER &&
                        (selectedMatrixType == null || cat.type == selectedMatrixType)
            }
            .sortedByDescending { it.actualAmount }
    }

    val pendingAutoPayBills = remember(uiState.fixedBills) {
        uiState.fixedBills.filter { bill ->
            !bill.isPaid &&
            bill.type != TransactionType.INCOME &&
            !(bill.type == TransactionType.CORPORATE && bill.category.equals("Reimbursements & Claims", ignoreCase = true))
        }
    }
    val pendingAutoPayTotal = remember(pendingAutoPayBills) {
        pendingAutoPayBills.sumOf { it.amount }
    }

    val daysUntilSalary = uiState.metrics.daysUntilPayday.coerceAtLeast(1)
    val safeDailyVelocity = if (uiState.metrics.safeToSpend > 0) {
        uiState.metrics.safeToSpend / daysUntilSalary
    } else 0.0

    val timeGreeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val displayName = userProfile.displayName.trim().ifBlank { "User" }
    val displayInflow = if (uiState.metrics.plannedIncome > 0.0) uiState.metrics.plannedIncome else uiState.metrics.personalIncome
    val displayExpenses = uiState.metrics.lifestyleExpenses
    val displayAssets = if (uiState.metrics.plannedAssets > 0.0) uiState.metrics.plannedAssets else uiState.metrics.actualAssets

    val headingTextColor = if (isMidnightTheme) Color.White else TextDark
    val subtitleTextColor = if (isMidnightTheme) Neutral400 else TextMuted
    val cardSurfaceColor = if (isMidnightTheme) Color(0xFF1A1F2B) else CardWhite
    val cardBorderColor = if (isMidnightTheme) Color.White.copy(alpha = 0.12f) else BorderLight.copy(alpha = 0.85f)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 140.dp)
    ) {
        // 1. GREETING HEADER + OVERLAPPING AVATAR + 3-PILLAR METRIC CARDS
        item(key = "hero_greeting_and_pillars") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$timeGreeting,",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = subtitleTextColor
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "Hello, $displayName 👋",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = headingTextColor,
                            letterSpacing = (-0.5).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when {
                                isPastMonth -> "Monthly ledger closed and reconciled."
                                isHealthy -> "Your runway is safe for the month."
                                else -> "Runway needs attention before payday."
                            },
                            fontSize = 11.5.sp,
                            color = subtitleTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isHealthy) Success100 else Error100,
                        border = BorderStroke(0.8.dp, if (isHealthy) Success500.copy(alpha = 0.35f) else Error500.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isHealthy) Success700 else Error700)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHealthy) "Safe Runway" else "Attention",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHealthy) Success700 else Error700
                            )
                        }
                    }
                }

                // 3-Card Row with overlapping profile avatar on top-right
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PillarStatCard(
                            icon = Icons.Default.SouthWest,
                            iconBg = Color(0xFFEDE9FE),
                            iconTint = Color(0xFF7C3AED),
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayInflow)}",
                            label = "Inflow",
                            surfaceColor = cardSurfaceColor,
                            borderColor = cardBorderColor,
                            valueColor = headingTextColor,
                            labelColor = subtitleTextColor,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToLedgerWithFilter(TransactionType.INCOME) }
                        )

                        PillarStatCard(
                            icon = Icons.Default.NorthEast,
                            iconBg = Color(0xFFE0F2FE),
                            iconTint = Primary600,
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayExpenses)}",
                            label = "Expenses",
                            surfaceColor = cardSurfaceColor,
                            borderColor = cardBorderColor,
                            valueColor = headingTextColor,
                            labelColor = subtitleTextColor,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToLedgerWithFilter(TransactionType.EXPENSE) }
                        )

                        PillarStatCard(
                            icon = Icons.Default.LocalFireDepartment,
                            iconBg = Color(0xFFFFEDD5),
                            iconTint = Color(0xFFEA580C),
                            amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", displayAssets)}",
                            label = "SIP Assets",
                            surfaceColor = cardSurfaceColor,
                            borderColor = cardBorderColor,
                            valueColor = headingTextColor,
                            labelColor = subtitleTextColor,
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigateToLedgerWithFilter(TransactionType.ASSET) }
                        )
                    }

                    // Circular Profile Avatar overlapping top-right of the 3rd card
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp)
                            .zIndex(2f)
                            .size(54.dp)
                            .shadow(6.dp, CircleShape)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF7C3AED), Primary600, SoftTeal)
                                )
                            )
                            .border(2.5.dp, if (isMidnightTheme) Neutral900 else Color.White, CircleShape)
                            .clickable(onClick = onOpenDrawer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileBitmap != null) {
                            Image(
                                bitmap = profileBitmap!!,
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = displayName.take(1).uppercase(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // 2. SAMSUNG NOW BAR DECK (Dynamic contextual alerts)
        if (nowBarAlerts.isNotEmpty()) {
            item(key = "now_bar_capsule") {
                VaultNowBar(
                    alerts = nowBarAlerts,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }

        // 3. VIBRANT GRADIENT SAFE-TO-SPEND HERO CARD
        item(key = "fintech_hero_card") {
            val safeFraction = (uiState.metrics.safeToSpendPercentage / 100f).coerceIn(0f, 1f)
            val nextPaydayOrd = formatOrdinalDay(uiState.metrics.nextPaydayDay)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(onClick = onOpenStsInfo),
                shape = RoundedCornerShape(22.dp),
                color = Primary700
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF7B4DFF),
                                    Primary600,
                                    SoftTeal
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "SAFE TO SPEND",
                                        color = Color.White.copy(alpha = 0.88f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.7.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = "Explain Safe to Spend",
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color.White.copy(alpha = 0.18f),
                                        border = BorderStroke(0.6.dp, Color.White.copy(alpha = 0.28f))
                                    ) {
                                        Text(
                                            text = "${uiState.metrics.safeToSpendPercentage}% Cap",
                                            color = Color.White,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (isDiscreetMode) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color.White.copy(alpha = 0.15f),
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "  ● ● ● ● ●  ",
                                            fontSize = 20.sp,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.safeToSpend)}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = (-0.7).sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = when {
                                        isPastMonth -> "Month closed • Final safe ledger balance"
                                        uiState.metrics.isSalaryDelayed -> "Salary expected ($nextPaydayOrd) • 1-day safety runway reserved"
                                        isCurrentMonth && isHealthy -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", safeDailyVelocity)}/day safe pace • $daysUntilSalary days until payday ($nextPaydayOrd)"
                                        isCurrentMonth -> "Runway deficit: Spending exceeds safe buffer"
                                        else -> "Projected runway reserved for $daysUntilSalary days"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (uiState.metrics.isSalaryDelayed) Warning100 else Color.White.copy(alpha = 0.9f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.20f))
                                    .border(0.8.dp, Color.White.copy(alpha = 0.32f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Inspect Safe to Spend",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isHealthy) "Safe Runway Active" else "Buffer Depleted",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "Safe Limit",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.24f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(safeFraction.coerceAtLeast(if (isHealthy) 0.04f else 0f))
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(10.dp)) }

        // 4. SLIM UPCOMING AUTOPAY STRIP (Tappable -> AutoPay Tab)
        item(key = "upcoming_autopay_strip") {
            val hasPending = pendingAutoPayBills.isNotEmpty()
            val billWord = if (pendingAutoPayBills.size == 1) "bill" else "bills"
            val nextPaydayOrd = formatOrdinalDay(uiState.metrics.nextPaydayDay)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.5.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(onClick = onNavigateToCommitments),
                shape = RoundedCornerShape(14.dp),
                color = cardSurfaceColor,
                border = BorderStroke(0.8.dp, cardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (hasPending) Icons.Default.Bolt else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (hasPending) Warning500 else Success500,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (hasPending) {
                                val amtLabel = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", pendingAutoPayTotal)}"
                                "Upcoming AutoPay • $amtLabel due (${pendingAutoPayBills.size} $billWord before $nextPaydayOrd)"
                            } else {
                                "All AutoPay commitments settled for this month"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = headingTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open AutoPay",
                        tint = subtitleTextColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // 5. CATEGORY MATRIX HEADER & 5-WAY SWITCHER (All | Expenses | Income | Assets | Corporate)
        item(key = "matrix_header_and_switcher") {
            val plannedExp = uiState.metrics.plannedExpenses
            val actualExp = uiState.metrics.actualExpenses
            val expDiff = plannedExp - actualExp

            val liveHeadroomLabel = when (selectedMatrixType) {
                null -> "${activeMatrix.size} Active"
                TransactionType.EXPENSE -> when {
                    plannedExp <= 0 -> "No Target Cap"
                    expDiff >= 0 -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", expDiff)} Headroom"
                    else -> "+${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(expDiff))} Over Budget"
                }
                TransactionType.INCOME -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualIncome)} Received"
                TransactionType.ASSET -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.metrics.actualAssets)} Funded"
                TransactionType.CORPORATE -> "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", uiState.reimbursementStatus.pendingReimbursement)} Pending"
                TransactionType.TRANSFER -> "Transfers"
            }

            val (headroomBg, headroomText) = when (selectedMatrixType) {
                null -> Primary100 to Primary700
                TransactionType.EXPENSE -> if (plannedExp > 0 && expDiff < 0) Error100 to Error700 else Success100 to Success700
                TransactionType.INCOME -> Success100 to Success700
                TransactionType.ASSET -> SoftTealLight to SoftTeal
                TransactionType.CORPORATE -> Warning100 to Warning700
                TransactionType.TRANSFER -> Primary100 to Primary700
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category Matrix",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    color = headingTextColor
                )

                Surface(
                    shape = RoundedCornerShape(7.dp),
                    color = headroomBg,
                    border = BorderStroke(0.6.dp, headroomText.copy(alpha = 0.30f))
                ) {
                    Text(
                        text = if (isDiscreetMode && selectedMatrixType != null) "••••" else liveHeadroomLabel,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = headroomText,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isMidnightTheme) Color.White.copy(alpha = 0.10f)
                        else Neutral100
                    )
                    .padding(4.dp)
            ) {
                listOf(
                    Triple<TransactionType?, String, Color>(null, "All", Primary600),
                    Triple(TransactionType.EXPENSE, "Expenses", Error500),
                    Triple(TransactionType.INCOME, "Income", Success500),
                    Triple(TransactionType.ASSET, "Assets", SoftTeal),
                    Triple(TransactionType.CORPORATE, "Corporate", Warning700)
                ).forEach { (type, label, color) ->
                    val isSelected = selectedMatrixType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) CardWhite else Color.Transparent)
                            .clickable { selectedMatrixType = type }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 10.5.sp,
                            color = if (isSelected) color else if (isMidnightTheme) Neutral400 else TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 6. CATEGORY MATRIX ROWS (Floating White Card - Untouched)
        if (activeMatrix.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = "No active entries in this segment", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        } else {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        activeMatrix.forEachIndexed { index, cat ->
                            val isLegacy = remember(uiState.masterCategories, cat) {
                                uiState.masterCategories.any { it.name.equals(cat.category, ignoreCase = true) && it.type == cat.type && it.isLegacy }
                            }
                            val isNew = remember(uiState.masterCategories, cat) {
                                uiState.masterCategories.any { it.name.equals(cat.category, ignoreCase = true) && it.type == cat.type && it.isNew }
                            }
                            val mapKey = "${cat.type.name}_${cat.category}"

                            CategoryMatrixRow(
                                cat = cat,
                                isExpanded = expandedCategories[mapKey] ?: false,
                                onToggleExpand = {
                                    expandedCategories[mapKey] = !(expandedCategories[mapKey] ?: false)
                                },
                                currencySymbol = userProfile.currencySymbol,
                                isDiscreetMode = isDiscreetMode,
                                isLegacy = isLegacy,
                                isNew = isNew
                            )
                            if (index < activeMatrix.lastIndex) {
                                HorizontalDivider(color = BorderLight.copy(alpha = 0.5f), thickness = 0.6.dp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillarStatCard(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    amount: String,
    label: String,
    surfaceColor: Color,
    borderColor: Color,
    valueColor: Color,
    labelColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(2.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = surfaceColor,
        border = BorderStroke(0.8.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = amount,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = label,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryMatrixRow(
    cat: CategoryPerformance,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    currencySymbol: String,
    isDiscreetMode: Boolean,
    isLegacy: Boolean = false,
    isNew: Boolean = false
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "arrowRotation"
    )

    val isTargetOriented = cat.type == TransactionType.INCOME ||
            cat.type == TransactionType.ASSET ||
            (cat.type == TransactionType.CORPORATE && cat.category.equals("Reimbursements & Claims", ignoreCase = true))

    val isOverBudgetRow = cat.plannedAmount > 0.0 &&
            cat.actualAmount > cat.plannedAmount &&
            !isTargetOriented

    val progressFraction = if (cat.plannedAmount > 0) {
        (cat.actualAmount / cat.plannedAmount).toFloat().coerceIn(0f, 1f)
    } else 1f

    val utilizationPercentage = if (cat.plannedAmount > 0) {
        ((cat.actualAmount / cat.plannedAmount) * 100).toInt()
    } else 100

    val progressColor = when {
        isOverBudgetRow -> Error500
        cat.type == TransactionType.INCOME -> Success500
        cat.type == TransactionType.ASSET -> SoftTeal
        cat.type == TransactionType.CORPORATE -> Warning500
        utilizationPercentage >= 85 -> Warning500
        else -> Primary600
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onToggleExpand)
            .padding(vertical = 8.dp, horizontal = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLegacy) Warning100 else progressColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat.category.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (isLegacy) Warning700 else progressColor
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cat.category,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        if (isLegacy) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Warning100,
                                border = BorderStroke(0.6.dp, Warning500)
                            ) {
                                Text(
                                    text = "Legacy",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Warning700,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else if (isNew) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Success500)
                            )
                        }

                        if (isOverBudgetRow) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Error100,
                                border = BorderStroke(0.5.dp, Error500.copy(alpha = 0.35f)),
                                modifier = Modifier.wrapContentWidth()
                            ) {
                                Text(
                                    text = "Over",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Error700,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    val statusText = if (cat.plannedAmount > 0) {
                        val diff = cat.actualAmount - cat.plannedAmount
                        if (isDiscreetMode) {
                            "Target configured"
                        } else if (isTargetOriented) {
                            if (diff >= 0) {
                                "Target achieved (+${currencySymbol}${String.format(Locale.US, "%,.0f", diff)})"
                            } else {
                                "${currencySymbol}${String.format(Locale.US, "%,.0f", abs(diff))} needed to reach target"
                            }
                        } else {
                            val remaining = cat.plannedAmount - cat.actualAmount
                            if (remaining >= 0) {
                                "$currencySymbol${String.format(Locale.US, "%,.0f", remaining)} left of $currencySymbol${String.format(Locale.US, "%,.0f", cat.plannedAmount)}"
                            } else {
                                "Exceeded by $currencySymbol${String.format(Locale.US, "%,.0f", abs(remaining))}"
                            }
                        }
                    } else {
                        val maskedOrActual = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", cat.actualAmount)}"
                        if (isLegacy) {
                            "Retiring next month • Logged: $maskedOrActual"
                        } else if (cat.type == TransactionType.CORPORATE) {
                            "Logged actual: $maskedOrActual"
                        } else {
                            "No target limit configured"
                        }
                    }

                    val statusColor = when {
                        isOverBudgetRow -> Error700
                        isTargetOriented && cat.plannedAmount > 0 && cat.actualAmount >= cat.plannedAmount -> Success700
                        else -> TextMuted
                    }

                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = if (isOverBudgetRow) FontWeight.SemiBold else FontWeight.Normal,
                        color = statusColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isDiscreetMode) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", cat.actualAmount)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = if (isOverBudgetRow) Error700 else TextDark
                    )
                    if (cat.plannedAmount > 0) {
                        Text(
                            text = "$utilizationPercentage%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = progressColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Expand",
                    tint = TextMuted,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(rotation)
                )
            }
        }

        if (cat.plannedAmount > 0) {
            Spacer(modifier = Modifier.height(7.dp))
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = BorderLight.copy(alpha = 0.5f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CanvasLight)
                    .padding(10.dp)
            ) {
                if (cat.activeSubcategories.isEmpty()) {
                    Text(text = "No logged transactions in subcategories", fontSize = 11.sp, color = TextMuted)
                } else {
                    Text(
                        text = "SUBCATEGORY CONTRIBUTIONS",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        color = TextMuted,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    cat.activeSubcategories.forEach { sub ->
                        val subPercentage = if (cat.actualAmount > 0) {
                            ((sub.amount / cat.actualAmount) * 100).toInt()
                        } else 0

                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(progressColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = sub.name, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextDark)
                                }

                                Text(
                                    text = if (isDiscreetMode) "•••• ($subPercentage%)" else "$currencySymbol${String.format(Locale.US, "%,.2f", sub.amount)} ($subPercentage%)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                            Spacer(modifier = Modifier.height(2.5.dp))
                            LinearProgressIndicator(
                                progress = { (subPercentage / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp)),
                                color = progressColor.copy(alpha = 0.65f),
                                trackColor = BorderLight.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}
