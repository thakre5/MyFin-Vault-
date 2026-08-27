package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.AccountBalanceResult
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

enum class VaultTier(val title: String, val description: String, val color: Color, val bgTint: Color) {
    OPERATING("Operating", "Daily living & spending", Color(0xFFE57A28), Color(0xFFFFF0D4)),
    COMMITMENTS("Commitments", "AutoPay & fixed bills", AccentPurple, Color(0xFFF3E5F5)),
    FORTRESS("Fortress", "Liquid emergency safety net", SoftTeal, Color(0xFFE0F7FA)),
    CASH("Cash", "Physical wallet & micro-spend", SoftGreen, Color(0xFFE6F8EF))
}

data class SuccessReceiptPayload(
    val subtitle: String,
    val headline: String,
    val description: String,
    val buttonText: String = "Done"
)

private fun getVaultTier(accountType: String, accountName: String): VaultTier {
    return when {
        accountType.equals("Operating", ignoreCase = true) -> VaultTier.OPERATING
        accountType.equals("Commitments", ignoreCase = true) -> VaultTier.COMMITMENTS
        accountType.equals("Fortress", ignoreCase = true) -> VaultTier.FORTRESS
        accountType.equals("Cash", ignoreCase = true) -> VaultTier.CASH
        else -> {
            val name = accountName.uppercase()
            when {
                name.contains("CASH") || name.contains("WALLET") -> VaultTier.CASH
                name.contains("COMMITMENT") || name.contains("BILL") || name.contains("BOM") || name.contains("EMI") || name.contains("AXIS") || name.contains("SBI") -> VaultTier.COMMITMENTS
                name.contains("FORTRESS") || name.contains("EMERGENCY") || name.contains("FD") || name.contains("RESERVE") || name.contains("INDUSIND") -> VaultTier.FORTRESS
                else -> VaultTier.OPERATING
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultAccountsScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToTaxonomy: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var isThreeVaultStrategy by rememberSaveable { mutableStateOf(true) }
    var pendingModeTarget by remember { mutableStateOf<Boolean?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var receiptPayload by remember { mutableStateOf<SuccessReceiptPayload?>(null) }

    var showActionMenu by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var adjustingAccount by remember { mutableStateOf<AccountBalanceResult?>(null) }

    val allDistinctAccountNames = remember(uiState.accounts, uiState.groupedTransactions) {
        val fromAccs = uiState.accounts.map { it.accountName }
        val fromTxs = uiState.groupedTransactions.values.flatten().map { it.accountName }.filter { it.isNotBlank() }
        (fromAccs + fromTxs).distinct().ifEmpty {
            listOf("Operating Account", "Commitments Account", "Fortress Account", "Cash Wallet")
        }
    }

    // Reactive derivation of live balances and tier associations
    val displayAccounts = remember(uiState.accounts, uiState.groupedTransactions, allDistinctAccountNames) {
        val allTxs = uiState.groupedTransactions.values.flatten()
        allDistinctAccountNames.mapIndexed { index, name ->
            val existing = uiState.accounts.find { it.accountName.equals(name, ignoreCase = true) }
            val txsForAcc = allTxs.filter { it.accountName.equals(name, ignoreCase = true) }
            val txBalance = txsForAcc.sumOf { if (it.type == TransactionType.INCOME) it.amount else -it.amount }
            val starting = existing?.startingBalance ?: 0.0
            val totalBal = if (txsForAcc.isNotEmpty()) starting + txBalance else (existing?.currentBalance ?: 0.0)

            // Resolve stored tier from entity or infer
            val tier = existing?.accountType?.ifBlank { null } ?: txsForAcc.firstOrNull { it.subcategory.isNotBlank() }?.subcategory ?: getVaultTier("", name).title

            AccountBalanceResult(
                accountName = name,
                accountType = tier,
                sortOrder = existing?.sortOrder ?: index,
                startingBalance = starting,
                currentBalance = totalBal
            )
        }
    }

    var activeSelectedCardIndex by remember { mutableIntStateOf(0) }
    val activeAccount = remember(displayAccounts, activeSelectedCardIndex) {
        displayAccounts.getOrNull(activeSelectedCardIndex.coerceIn(0, (displayAccounts.size - 1).coerceAtLeast(0))) ?: displayAccounts.first()
    }

    val accountNames = remember(displayAccounts) { displayAccounts.map { it.accountName } }

    val totalLiquidBalance = remember(displayAccounts) { displayAccounts.sumOf { it.currentBalance } }
    val opTotal = remember(displayAccounts) {
        displayAccounts.filter { getVaultTier(it.accountType, it.accountName) == VaultTier.OPERATING }.sumOf { it.currentBalance }
    }
    val comTotal = remember(displayAccounts) {
        displayAccounts.filter { getVaultTier(it.accountType, it.accountName) == VaultTier.COMMITMENTS }.sumOf { it.currentBalance }
    }
    val fortTotal = remember(displayAccounts) {
        displayAccounts.filter { getVaultTier(it.accountType, it.accountName) == VaultTier.FORTRESS }.sumOf { it.currentBalance }
    }
    val cashTotal = remember(displayAccounts) {
        displayAccounts.filter { getVaultTier(it.accountType, it.accountName) == VaultTier.CASH }.sumOf { it.currentBalance }
    }

    // Cashflow Matrix Metrics
    val activeAccountTxs = remember(uiState.groupedTransactions, activeAccount.accountName) {
        uiState.groupedTransactions.values.flatten().filter { it.accountName.equals(activeAccount.accountName, ignoreCase = true) }
    }

    val activeExpenses = remember(activeAccountTxs) {
        activeAccountTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }
    val activeIncome = remember(activeAccountTxs) {
        activeAccountTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
    }

    val daysElapsed = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1) }
    val dailyBurnRate = remember(activeExpenses, daysElapsed) {
        if (activeExpenses > 0) activeExpenses / daysElapsed else 0.0
    }
    val runwayDays = remember(activeAccount.currentBalance, dailyBurnRate) {
        if (dailyBurnRate > 0 && activeAccount.currentBalance > 0) (activeAccount.currentBalance / dailyBurnRate).toInt() else 90
    }

    val pendingBillsForAccount = remember(uiState.fixedBills, activeAccount.accountName) {
        uiState.fixedBills.filter { !it.isPaid && it.type == TransactionType.EXPENSE }
    }
    val totalPendingBillsAmount = remember(pendingBillsForAccount) {
        pendingBillsForAccount.sumOf { it.amount }
    }

    Box(modifier = Modifier.fillMaxSize().background(CanvasLight)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Centered Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight.copy(alpha = 0.7f), RoundedCornerShape(11.dp))
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Drawer",
                        tint = TextDark,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = if (isThreeVaultStrategy) "3-Vault Strategy" else "Vault Accounts",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(CardWhite)
                        .border(0.8.dp, BorderLight.copy(alpha = 0.7f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.HelpOutline,
                        contentDescription = "Help Guide",
                        tint = TextMuted,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            // Dedicated Mode Switcher Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BorderLight.copy(alpha = 0.45f))
                    .padding(2.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (isThreeVaultStrategy) CardWhite else Color.Transparent)
                        .clickable {
                            if (!isThreeVaultStrategy) {
                                pendingModeTarget = true
                            }
                        }
                        .padding(vertical = 6.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3-Vault Strategy",
                        fontWeight = if (isThreeVaultStrategy) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = if (isThreeVaultStrategy) AccentPurple else TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (!isThreeVaultStrategy) CardWhite else Color.Transparent)
                        .clickable {
                            if (isThreeVaultStrategy) {
                                pendingModeTarget = false
                            }
                        }
                        .padding(vertical = 6.5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Simple Mode",
                        fontWeight = if (!isThreeVaultStrategy) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = if (!isThreeVaultStrategy) TextDark else TextMuted
                    )
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 105.dp)
            ) {
                // Vault Asset Allocation Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Vault Asset Allocation",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Liquidity distribution across accounts",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }

                                IconButton(
                                    onClick = onNavigateToDashboard,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CanvasLight)
                                ) {
                                    Icon(
                                        Icons.Default.NorthEast,
                                        contentDescription = "Analytics",
                                        tint = TextDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val opFraction = if (totalLiquidBalance > 0) (opTotal / totalLiquidBalance).toFloat().coerceIn(0f, 1f) else 0.25f
                                val comFraction = if (totalLiquidBalance > 0) (comTotal / totalLiquidBalance).toFloat().coerceIn(0f, 1f) else 0.25f
                                val fortFraction = if (totalLiquidBalance > 0) (fortTotal / totalLiquidBalance).toFloat().coerceIn(0f, 1f) else 0.25f
                                val cashFraction = if (totalLiquidBalance > 0) (cashTotal / totalLiquidBalance).toFloat().coerceIn(0f, 1f) else 0.25f

                                Box(
                                    modifier = Modifier.size(110.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    FourWayDonutAllocationChart(
                                        opFraction = opFraction,
                                        comFraction = comFraction,
                                        fortFraction = fortFraction,
                                        cashFraction = cashFraction,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Liquid", fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalLiquidBalance)}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextDark
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    AllocationStatPill(
                                        title = "Operating",
                                        percentage = if (totalLiquidBalance > 0) ((opTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = Color(0xFFE57A28),
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", opTotal)}"
                                    )
                                    AllocationStatPill(
                                        title = "Commitments",
                                        percentage = if (totalLiquidBalance > 0) ((comTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = AccentPurple,
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", comTotal)}"
                                    )
                                    AllocationStatPill(
                                        title = "Fortress",
                                        percentage = if (totalLiquidBalance > 0) ((fortTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = SoftTeal,
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortTotal)}"
                                    )
                                    AllocationStatPill(
                                        title = "Cash",
                                        percentage = if (totalLiquidBalance > 0) ((cashTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = SoftGreen,
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cashTotal)}"
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Connected Bank Accounts Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Connected Bank Accounts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = TextDark
                        )

                        Text(
                            text = "Tap to view analytics",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Horizontal Scrollable Bank Cards
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(displayAccounts.size) { idx ->
                            val acc = displayAccounts[idx]
                            val tier = getVaultTier(acc.accountType, acc.accountName)
                            val isSelected = activeSelectedCardIndex == idx

                            BankAccountPhysicalCard(
                                account = acc,
                                currencySymbol = userProfile.currencySymbol,
                                tier = tier,
                                isSelected = isSelected,
                                showRole = isThreeVaultStrategy,
                                onSelect = { activeSelectedCardIndex = idx },
                                onEditBalance = { adjustingAccount = acc },
                                modifier = Modifier.width(260.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Cashflow Matrix (Replacing raw ledger)
                item {
                    Text(
                        text = "Account Cashflow Matrix (${activeAccount.accountName})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // 2x2 Liquidity Health & Runway Matrix
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                MatrixMetricCell(
                                    title = "Daily Burn Rate",
                                    value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyBurnRate)}/day",
                                    icon = Icons.Default.Whatshot,
                                    iconColor = SoftRed,
                                    subtitle = "Avg spend velocity",
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                MatrixMetricCell(
                                    title = "Runway Buffer",
                                    value = "$runwayDays Days",
                                    icon = Icons.Default.Timer,
                                    iconColor = if (runwayDays >= 30) SoftGreen else SoftAmber,
                                    subtitle = "Coverage at current burn",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(modifier = Modifier.fillMaxWidth()) {
                                MatrixMetricCell(
                                    title = "Queued AutoPay",
                                    value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalPendingBillsAmount)}",
                                    icon = Icons.Default.Schedule,
                                    iconColor = AccentPurple,
                                    subtitle = "${pendingBillsForAccount.size} pending bills",
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                val netDelta = activeIncome - activeExpenses
                                MatrixMetricCell(
                                    title = "Net Cashflow",
                                    value = "${if (netDelta >= 0) "+" else "-"}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(netDelta))}",
                                    icon = if (netDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                    iconColor = if (netDelta >= 0) SoftGreen else SoftRed,
                                    subtitle = "Retained balance delta",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Strategic Routing & Sweep Trigger
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(18.dp)),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Savings, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(17.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Strategic Vault Routing", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                }

                                Surface(shape = RoundedCornerShape(6.dp), color = AccentPurple.copy(alpha = 0.12f)) {
                                    Text("92% On Plan", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentPurple, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Outflow Multi-segment Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(CanvasLight)
                            ) {
                                val totalOut = (activeExpenses + totalPendingBillsAmount).coerceAtLeast(1.0)
                                Box(modifier = Modifier.weight((activeExpenses / totalOut).toFloat().coerceIn(0.05f, 0.95f)).fillMaxHeight().background(Color(0xFFE57A28)))
                                Box(modifier = Modifier.weight((totalPendingBillsAmount / totalOut).toFloat().coerceIn(0.05f, 0.95f)).fillMaxHeight().background(AccentPurple))
                                Box(modifier = Modifier.weight(0.15f).fillMaxHeight().background(SoftTeal))
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CanvasLight)
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Monthly Commitments Covered", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                    Text("Surplus ready to sweep to Fortress", fontSize = 10.sp, color = TextMuted)
                                }

                                Button(
                                    onClick = { showTransferSheet = true },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text("Sweep Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4 + 1 Floating Bottom Navigation Dock
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .shadow(16.dp, CircleShape),
                shape = CircleShape,
                color = CardWhite
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DockPillTab(
                        title = "Taxonomy",
                        icon = Icons.Default.Category,
                        isSelected = false,
                        onClick = onNavigateToTaxonomy
                    )
                    DockPillTab(
                        title = "Planner",
                        icon = Icons.Default.PieChart,
                        isSelected = false,
                        onClick = onNavigateToPlanner
                    )
                    DockPillTab(
                        title = "Vaults",
                        icon = Icons.Default.AccountBalanceWallet,
                        isSelected = true,
                        onClick = { }
                    )
                    DockPillTab(
                        title = "Monthly",
                        icon = Icons.Default.Assessment,
                        isSelected = false,
                        onClick = onNavigateToDashboard
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            FloatingActionButton(
                onClick = { showActionMenu = !showActionMenu },
                containerColor = TextDark,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(60.dp).shadow(16.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Actions",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(if (showActionMenu) 45f else 0f)
                )
            }
        }

        // Floating Action Menu
        if (showActionMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showActionMenu = false }
                    )
            )

            AnimatedVisibility(
                visible = showActionMenu,
                enter = scaleIn(transformOrigin = TransformOrigin(1f, 1f), animationSpec = tween(180)) + fadeIn(animationSpec = tween(180)),
                exit = scaleOut(transformOrigin = TransformOrigin(1f, 1f), animationSpec = tween(150)) + fadeOut(animationSpec = tween(150)),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 94.dp, end = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CardWhite,
                    shadowElevation = 10.dp,
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                    modifier = Modifier.width(190.dp)
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    showAddAccountSheet = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddCard, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Add Account", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        }

                        HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showActionMenu = false
                                    showTransferSheet = true
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, tint = TextDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Transfer", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                        }
                    }
                }
            }
        }

        // Mode Switch Confirmation Alert
        pendingModeTarget?.let { targetMode ->
            AlertDialog(
                onDismissRequest = { pendingModeTarget = null },
                title = {
                    Text(
                        text = if (targetMode) "Enable 3-Vault Strategy?" else "Switch to Simple Mode?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = if (targetMode) {
                            "This organizes your accounts into Operating (daily spend), Commitments (AutoPay bills), Emergency Fortress, and Cash tiers. Your balances remain completely intact."
                        } else {
                            "This will display your accounts in a unified flat list without role compartmentalization."
                        },
                        fontSize = 13.sp,
                        color = TextDark
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            isThreeVaultStrategy = targetMode
                            pendingModeTarget = null
                            receiptPayload = SuccessReceiptPayload(
                                subtitle = "Preference Updated",
                                headline = if (targetMode) "3-Vault Strategy Active" else "Simple Mode Active",
                                description = if (targetMode) {
                                    "Accounts structured into Operating, Commitments, Fortress, and Cash tiers."
                                } else {
                                    "Accounts structured into a flexible, flat liquidity list."
                                },
                                buttonText = "Done"
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Confirm Switch", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingModeTarget = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        // Help Guide Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text("3-Vault Strategy Guide", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("• Operating: Variable daily life and groceries. Never touch for fixed bills.")
                        Text("• Commitments: Dedicated for AutoPay, loan EMIs, and monthly fixed commitments.")
                        Text("• Fortress: Liquid emergency backup protecting against unforeseen surprises.")
                        Text("• Cash: Physical cash on hand and petty expenses.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text("Understood", fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                }
            )
        }

        // Instant Vault Transfer Bottom Sheet
        if (showTransferSheet) {
            var fromAccount by remember { mutableStateOf(accountNames.firstOrNull().orEmpty()) }
            var toAccount by remember { mutableStateOf(accountNames.getOrNull(1) ?: accountNames.firstOrNull().orEmpty()) }
            var amountText by remember { mutableStateOf("") }
            var noteText by remember { mutableStateOf("") }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showTransferSheet = false },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                dragHandle = {
                    Surface(modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp), shape = CircleShape, color = BorderLight) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 8.dp)
                ) {
                    Text("Instant Vault Transfer", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Transfer liquidity between your bank accounts & vaults", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Source Account (From)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(accountNames) { acc ->
                            val isSel = fromAccount == acc
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { fromAccount = acc },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) AccentPurple.copy(alpha = 0.14f) else CanvasLight,
                                border = BorderStroke(0.6.dp, if (isSel) AccentPurple else BorderLight)
                            ) {
                                Text(
                                    text = acc,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) AccentPurple else TextDark,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Destination Account (To)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(accountNames) { acc ->
                            val isSel = toAccount == acc
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { toAccount = acc },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) AccentPurple.copy(alpha = 0.14f) else CanvasLight,
                                border = BorderStroke(0.6.dp, if (isSel) AccentPurple else BorderLight)
                            ) {
                                Text(
                                    text = acc,
                                    fontSize = 11.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) AccentPurple else TextDark,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Transfer Amount (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note / Purpose (e.g., Grocery Sweep)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && fromAccount.isNotBlank() && toAccount.isNotBlank() && fromAccount != toAccount) {
                                viewModel.executeInstantTransfer(fromAccount, toAccount, amt, noteText)
                                showTransferSheet = false
                                receiptPayload = SuccessReceiptPayload(
                                    subtitle = "Transfer Successful",
                                    headline = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", amt)}",
                                    description = "$fromAccount ➔ $toAccount ${if (noteText.isNotBlank()) "($noteText)" else ""}",
                                    buttonText = "View Vaults"
                                )
                            } else {
                                Toast.makeText(context, "Please enter a valid amount and distinct accounts", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Confirm Transfer", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Adjust Account Balance Bottom Sheet
        adjustingAccount?.let { acc ->
            var newBalanceText by remember(acc) {
                mutableStateOf(String.format(Locale.US, "%.0f", acc.currentBalance))
            }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { adjustingAccount = null },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp), shape = CircleShape, color = BorderLight) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("Adjust: ${acc.accountName}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Set updated balance. A ledger adjustment will be recorded.", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = newBalanceText,
                        onValueChange = { newBalanceText = it },
                        label = { Text("Current Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val targetBal = newBalanceText.toDoubleOrNull() ?: acc.currentBalance
                            val diff = targetBal - acc.currentBalance
                            if (diff != 0.0) {
                                val txType = if (diff > 0.0) TransactionType.INCOME else TransactionType.EXPENSE
                                val amountVal = if (diff < 0.0) -diff else diff
                                viewModel.saveTransaction(
                                    id = 0L,
                                    title = "Balance Adjustment",
                                    amount = amountVal,
                                    category = "General",
                                    subcategory = acc.accountType,
                                    accountName = acc.accountName,
                                    type = txType,
                                    date = System.currentTimeMillis()
                                )
                            }
                            adjustingAccount = null
                            Toast.makeText(context, "Balance adjusted for ${acc.accountName}", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save Balance", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Add Account Bottom Sheet (Stores Explicit Tier in subcategory & entity)
        if (showAddAccountSheet) {
            var name by remember { mutableStateOf("") }
            var digitsText by remember { mutableStateOf("") }
            var balanceText by remember { mutableStateOf("") }
            var selectedType by remember { mutableStateOf("Savings") }
            var selectedTier by remember { mutableStateOf(VaultTier.OPERATING) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showAddAccountSheet = false },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                dragHandle = {
                    Surface(modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp), shape = CircleShape, color = BorderLight) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("Add Vault Account", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Configure account details, type, and strategic role tier", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (e.g., Primary Salary, Bills Account)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = digitsText,
                        onValueChange = { if (it.length <= 4) digitsText = it },
                        label = { Text("Last 4 Digits (Optional)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Account Type", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("Savings", "Salary", "Current", "Credit Card", "Cash Wallet", "Fixed Deposit")) { type ->
                            val isSel = selectedType == type
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedType = type },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) AccentPurple.copy(alpha = 0.12f) else CanvasLight,
                                border = BorderStroke(0.6.dp, if (isSel) AccentPurple else BorderLight)
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) AccentPurple else TextDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Strategic Vault Role", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VaultTier.values().forEach { tier ->
                            val isSel = selectedTier == tier
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { selectedTier = tier },
                                shape = RoundedCornerShape(9.dp),
                                color = if (isSel) tier.color.copy(alpha = 0.14f) else CanvasLight,
                                border = BorderStroke(0.7.dp, if (isSel) tier.color else BorderLight)
                            ) {
                                Text(
                                    text = tier.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) tier.color else TextDark,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Initial Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val bal = balanceText.toDoubleOrNull() ?: 0.0
                                val initialAmount = if (bal > 0.0) bal else 0.0
                                viewModel.saveTransaction(
                                    id = 0L,
                                    title = if (bal > 0.0) "Opening Balance" else "Account Initialized",
                                    amount = initialAmount,
                                    category = "General",
                                    subcategory = selectedTier.title,
                                    accountName = name.trim().uppercase(),
                                    type = TransactionType.INCOME,
                                    date = System.currentTimeMillis()
                                )
                                showAddAccountSheet = false
                                Toast.makeText(context, "Account '${name.trim().uppercase()}' registered as ${selectedTier.title}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Success Receipt Bottom Sheet
        receiptPayload?.let { payload ->
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { receiptPayload = null },
                sheetState = sheetState,
                containerColor = CardWhite,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                dragHandle = {
                    Surface(
                        modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp),
                        shape = CircleShape,
                        color = BorderLight
                    ) {}
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AccentPurple,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = payload.subtitle,
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = payload.headline,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(5.dp))

                    Text(
                        text = payload.description,
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { receiptPayload = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text(
                            text = payload.buttonText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun MatrixMetricCell(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(title, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(1.dp))
        Text(subtitle, fontSize = 9.5.sp, color = TextMuted)
    }
}

@Composable
private fun BankAccountPhysicalCard(
    account: AccountBalanceResult,
    currencySymbol: String,
    tier: VaultTier,
    isSelected: Boolean,
    showRole: Boolean,
    onSelect: () -> Unit,
    onEditBalance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maskedDigits = remember(account.accountName) {
        String.format(Locale.US, "%04d", abs(account.accountName.hashCode() % 9000 + 1000))
    }

    Surface(
        modifier = modifier
            .shadow(if (isSelected) 4.dp else 1.5.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(if (isSelected) 1.5.dp else 0.8.dp, if (isSelected) tier.color else BorderLight.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            tier.bgTint.copy(alpha = if (isSelected) 0.5f else 0.3f),
                            CardWhite
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(tier.bgTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (tier) {
                            VaultTier.CASH -> Icons.Default.Payments
                            VaultTier.FORTRESS -> Icons.Default.Security
                            VaultTier.COMMITMENTS -> Icons.Default.CreditCard
                            VaultTier.OPERATING -> Icons.Default.AccountBalance
                        },
                        contentDescription = null,
                        tint = tier.color,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showRole) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = tier.color.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = tier.title,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = tier.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onEditBalance,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Balance",
                            tint = TextMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = account.accountName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "•••• $maskedDigits",
                fontSize = 11.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "$currencySymbol${String.format(Locale.US, "%,.2f", account.currentBalance)}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = if (account.currentBalance >= 0) TextDark else SoftRed
            )
        }
    }
}

@Composable
private fun FourWayDonutAllocationChart(
    opFraction: Float,
    comFraction: Float,
    fortFraction: Float,
    cashFraction: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 11.dp.toPx()
        val diameter = size.minDimension - strokeWidth
        val arcSize = Size(diameter, diameter)
        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

        val total = (opFraction + comFraction + fortFraction + cashFraction).coerceAtLeast(0.001f)
        val opAngle = (opFraction / total) * 360f
        val comAngle = (comFraction / total) * 360f
        val fortAngle = (fortFraction / total) * 360f
        val cashAngle = (cashFraction / total) * 360f

        var startAngle = -90f

        // Operating Arc
        drawArc(
            color = Color(0xFFE57A28),
            startAngle = startAngle,
            sweepAngle = (opAngle - 4f).coerceAtLeast(2f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += opAngle

        // Commitments Arc
        drawArc(
            color = AccentPurple,
            startAngle = startAngle,
            sweepAngle = (comAngle - 4f).coerceAtLeast(2f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += comAngle

        // Fortress Arc
        drawArc(
            color = SoftTeal,
            startAngle = startAngle,
            sweepAngle = (fortAngle - 4f).coerceAtLeast(2f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += fortAngle

        // Cash Arc
        drawArc(
            color = SoftGreen,
            startAngle = startAngle,
            sweepAngle = (cashAngle - 4f).coerceAtLeast(2f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun AllocationStatPill(
    title: String,
    percentage: Int,
    color: Color,
    amount: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = CanvasLight,
        border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$percentage%", fontSize = 10.sp, color = TextMuted)
            }
            Text(amount, fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextDark)
        }
    }
}

@Composable
private fun DockPillTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) CanvasLight else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = title,
                tint = if (isSelected) AccentPurple else TextMuted,
                modifier = Modifier.size(17.dp)
            )
            if (isSelected) {
                Spacer(modifier = Modifier.width(5.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = AccentPurple)
            }
        }
    }
}
