package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.AccountBalanceResult
import com.example.myfin.data.AccountEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.AppBottomDock
import com.example.myfin.ui.components.DockFabAction
import com.example.myfin.ui.components.NavigationTarget
import com.example.myfin.ui.components.rememberAutoScrollVisibilityConnection
import com.example.myfin.ui.theme.*
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

data class PendingEditConfirmation(
    val originalAccount: AccountBalanceResult,
    val updatedName: String,
    val updatedRole: VaultTier,
    val targetBalance: Double,
    val minBalance: Double = 0.0,
    val isFixedDeposit: Boolean = false
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
                name.contains("CASH") || name.contains("WALLET") || name.contains("PETTY") -> VaultTier.CASH
                name.contains("COMMITMENT") || name.contains("BILL") || name.contains("EMI") || name.contains("AUTOPAY") || name.contains("OBLIGATION") -> VaultTier.COMMITMENTS
                name.contains("FORTRESS") || name.contains("EMERGENCY") || name.contains("FD") || name.contains("RESERVE") || name.contains("DEPOSIT") || name.contains("SAVING") -> VaultTier.FORTRESS
                else -> VaultTier.OPERATING
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultStrategyScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToTaxonomy: () -> Unit = {},
    onNavigateToVaultAnalytics: () -> Unit = {},
    onNavigateToVaultSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val avgMonthlySpend by viewModel.averageMonthlySpend.collectAsState()

    var showHelpDialog by remember { mutableStateOf(false) }
    var receiptPayload by remember { mutableStateOf<SuccessReceiptPayload?>(null) }
    var isDiscreetMode by remember { mutableStateOf(false) }

    var showTransferSheet by remember { mutableStateOf(false) }
    var transferPrefillAmount by remember { mutableStateOf("") }
    var transferPrefillNote by remember { mutableStateOf("Strategic Vault Sweep") }
    var transferFromAccount by remember { mutableStateOf("") }
    var transferToAccount by remember { mutableStateOf("") }

    var showAddAccountSheet by remember { mutableStateOf(false) }
    var showRoutingDetailsSheet by remember { mutableStateOf(false) }

    var editingAccount by remember { mutableStateOf<AccountBalanceResult?>(null) }
    var pendingEditConfirmation by remember { mutableStateOf<PendingEditConfirmation?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()

    val displayAccounts = uiState.accounts
    var activeSelectedCardIndex by remember { mutableIntStateOf(0) }
    val activeAccount = remember(displayAccounts, activeSelectedCardIndex) {
        if (displayAccounts.isNotEmpty()) {
            displayAccounts.getOrNull(activeSelectedCardIndex.coerceIn(0, displayAccounts.size - 1))
        } else null
    }

    val accountNames = remember(displayAccounts) { displayAccounts.map { it.accountName } }

    val fortressAccountName = remember(displayAccounts) {
        displayAccounts.find { getVaultTier(it.accountType, it.accountName) == VaultTier.FORTRESS }?.accountName
            ?: displayAccounts.getOrNull(2)?.accountName.orEmpty()
    }

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

    val allFlattenedTxs = remember(uiState.groupedTransactions) {
        uiState.groupedTransactions.values.flatten()
    }

    val activeAccountTxs = remember(allFlattenedTxs, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        allFlattenedTxs.filter {
            it.accountName.equals(name, ignoreCase = true) ||
                    (it.type == TransactionType.TRANSFER && it.toAccountName.equals(name, ignoreCase = true))
        }
    }

    val activeExpenses = remember(activeAccountTxs, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        activeAccountTxs.filter { it.type == TransactionType.EXPENSE && it.accountName.equals(name, ignoreCase = true) }.sumOf { it.amount }
    }
    val activeIncome = remember(activeAccountTxs, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        activeAccountTxs.filter {
            (it.type == TransactionType.INCOME && it.accountName.equals(name, ignoreCase = true)) ||
                    (it.type == TransactionType.TRANSFER && it.toAccountName.equals(name, ignoreCase = true))
        }.sumOf { it.amount }
    }
    val activeTransfersOut = remember(activeAccountTxs, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        activeAccountTxs.filter { it.type == TransactionType.TRANSFER && it.accountName.equals(name, ignoreCase = true) }.sumOf { it.amount }
    }

    val daysElapsed = remember { Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1) }
    val totalDaysInMonth = remember { Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH) }
    val remainingDays = remember(daysElapsed, totalDaysInMonth) { max(1, totalDaysInMonth - daysElapsed) }

    val dailyBurnRate = remember(activeExpenses, daysElapsed) {
        if (activeExpenses > 0) activeExpenses / daysElapsed else 0.0
    }
    val runwayDays = remember(activeAccount?.currentBalance, dailyBurnRate) {
        val bal = activeAccount?.currentBalance ?: 0.0
        if (dailyBurnRate > 0 && bal > 0) (bal / dailyBurnRate).toInt() else 90
    }

    val pendingBillsForAccount = remember(uiState.fixedBills, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        uiState.fixedBills.filter { !it.isPaid && it.type == TransactionType.EXPENSE && (it.accountName.equals(name, ignoreCase = true) || it.accountName.isBlank()) }
    }
    val totalPendingBillsAmount = remember(pendingBillsForAccount) {
        pendingBillsForAccount.sumOf { it.amount }
    }

    val selectedTier = activeAccount?.let { getVaultTier(it.accountType, it.accountName) } ?: VaultTier.OPERATING
    val accountMinBalance = activeAccount?.minBalance ?: 0.0

    // Dynamic Overdraft & Minimum Balance Risk Check
    val isOverdraftRisk = remember(activeAccount?.currentBalance, totalPendingBillsAmount, selectedTier, accountMinBalance) {
        val bal = activeAccount?.currentBalance ?: 0.0
        when (selectedTier) {
            VaultTier.COMMITMENTS -> bal < (totalPendingBillsAmount + accountMinBalance)
            VaultTier.OPERATING -> bal < (totalPendingBillsAmount + accountMinBalance)
            else -> false
        }
    }

    // Dynamic Sweepable Surplus Calculations
    val calculatedSweepSurplus = remember(
        activeAccount?.currentBalance,
        totalPendingBillsAmount,
        dailyBurnRate,
        remainingDays,
        selectedTier,
        accountMinBalance
    ) {
        val bal = activeAccount?.currentBalance ?: 0.0
        when (selectedTier) {
            VaultTier.OPERATING -> {
                val monthlyBurnSafety = dailyBurnRate * remainingDays
                (bal - totalPendingBillsAmount - monthlyBurnSafety - accountMinBalance).coerceAtLeast(0.0)
            }
            VaultTier.COMMITMENTS -> {
                (bal - totalPendingBillsAmount - accountMinBalance).coerceAtLeast(0.0)
            }
            else -> 0.0
        }
    }

    // Auto-Sweep Operating Threshold (For Account 3 Internal Split)
    val autoSweepThreshold: Double = if (userProfile.fortressThreshold > 0.0) userProfile.fortressThreshold else 25000.0

    val fabActions = remember {
        listOf(
            DockFabAction(
                icon = Icons.Default.AddCard,
                label = "Add Account",
                onClick = { showAddAccountSheet = true }
            ),
            DockFabAction(
                icon = Icons.Default.SyncAlt,
                label = "Transfer",
                onClick = {
                    transferPrefillAmount = ""
                    transferPrefillNote = "Internal Vault Transfer"
                    transferFromAccount = ""
                    transferToAccount = ""
                    showTransferSheet = true
                }
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
            .nestedScroll(scrollConnection)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pinned Top Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasLight)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(38.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Drawer",
                            tint = TextDark,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "3-Vault Strategy",
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                            color = TextDark,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = { isDiscreetMode = !isDiscreetMode },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isDiscreetMode) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Balance Privacy",
                                tint = if (isDiscreetMode) AccentPurple else TextMuted,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .height(38.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            IconButton(
                                onClick = onNavigateToVaultAnalytics,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Insights,
                                    contentDescription = "Reports & Analytics",
                                    tint = TextDark,
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(0.8.dp)
                                    .height(16.dp)
                                    .background(BorderLight.copy(alpha = 0.8f))
                            )

                            IconButton(
                                onClick = onNavigateToVaultSettings,
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Vault Settings",
                                    tint = AccentPurple,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CanvasLight,
                                    CanvasLight.copy(alpha = 0f)
                                )
                            )
                        )
                )
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 125.dp)
            ) {
                // Donut Allocation Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = CardWhite,
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.18f))
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFFCFAFF),
                                            AccentPurple.copy(alpha = 0.04f)
                                        )
                                    )
                                )
                                .padding(18.dp)
                        ) {
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
                                    onClick = { showHelpDialog = true },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(AccentPurple.copy(alpha = 0.10f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HelpOutline,
                                        contentDescription = "Help Guide",
                                        tint = AccentPurple,
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
                                        Text(text = "Liquid", fontSize = 9.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                                        Text(
                                            text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalLiquidBalance)}",
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
                                        amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", opTotal)}"
                                    )
                                    AllocationStatPill(
                                        title = "Commitments",
                                        percentage = if (totalLiquidBalance > 0) ((comTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = AccentPurple,
                                        amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", comTotal)}"
                                    )
                                    AllocationStatPill(
                                        title = "Fortress",
                                        percentage = if (totalLiquidBalance > 0) ((fortTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = SoftTeal,
                                        amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortTotal)}"
                                    )
                                    AllocationStatPill(
                                        title = "Cash",
                                        percentage = if (totalLiquidBalance > 0) ((cashTotal / totalLiquidBalance) * 100).toInt() else 0,
                                        color = SoftGreen,
                                        amount = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", cashTotal)}"
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Connected Bank Accounts
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
                            text = "Tap to view cashflow",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Carousel of Cards
                if (displayAccounts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text(text = "Initializing accounts...", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
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
                                    showRole = true,
                                    isDiscreet = isDiscreetMode,
                                    autoSweepThreshold = autoSweepThreshold,
                                    onSelect = { activeSelectedCardIndex = idx },
                                    onEdit = { editingAccount = acc },
                                    modifier = Modifier.width(280.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Overdraft & Minimum Balance Warning Banner
                if (isOverdraftRisk) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFF1F2),
                            border = BorderStroke(0.8.dp, SoftRed.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = SoftRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (selectedTier == VaultTier.COMMITMENTS) "Overdraft & MAB Warning" else "Overdraft Buffer Warning",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = SoftRed
                                        )
                                        Text(
                                            text = if (selectedTier == VaultTier.COMMITMENTS && accountMinBalance > 0) {
                                                "Liquid balance is below upcoming AutoPay bills plus the mandatory ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", accountMinBalance)} minimum balance."
                                            } else {
                                                "Queued AutoPay bills exceed this account's liquid balance."
                                            },
                                            fontSize = 11.sp,
                                            color = TextDark.copy(alpha = 0.8f),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                                Button(
                                    onClick = {
                                        transferPrefillAmount = ""
                                        transferPrefillNote = "Cover ${selectedTier.title} Balance"
                                        transferFromAccount = ""
                                        transferToAccount = activeAccount?.accountName.orEmpty()
                                        showTransferSheet = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Text(text = "Cover", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Sweep Surplus to Fortress Banner (Operating or Commitments with Surplus)
                if ((selectedTier == VaultTier.OPERATING || selectedTier == VaultTier.COMMITMENTS) && calculatedSweepSurplus > 0) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, SoftTeal.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(SoftTeal.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = SoftTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isDiscreetMode) "Surplus Buffer Ready for Sweep" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", calculatedSweepSurplus)} Ready to Sweep",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = TextDark
                                    )
                                    Text(
                                        text = if (selectedTier == VaultTier.OPERATING) {
                                            "Surplus above estimated living spend and minimum balance ready to be swept into your Fortress investment reserve."
                                        } else {
                                            "Excess funds above queued AutoPay commitments and minimum balance ready to be swept into Fortress."
                                        },
                                        fontSize = 10.5.sp,
                                        color = TextMuted,
                                        lineHeight = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = {
                                        transferPrefillAmount = String.format(Locale.US, "%.0f", calculatedSweepSurplus)
                                        transferPrefillNote = "${selectedTier.title} Surplus Sweep"
                                        transferFromAccount = activeAccount?.accountName.orEmpty()
                                        transferToAccount = fortressAccountName
                                        showTransferSheet = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SoftTeal),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text(text = "Sweep to Fortress", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }

                // Account Cashflow Matrix
                activeAccount?.let { acc ->
                    item {
                        Text(
                            text = "Account Cashflow Matrix (${acc.accountName})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

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
                                        value = if (isDiscreetMode) "••••/day" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyBurnRate)}/day",
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
                                    if (selectedTier == VaultTier.FORTRESS) {
                                        val capProgress = if (autoSweepThreshold > 0.0) ((acc.currentBalance / autoSweepThreshold) * 100).toInt() else 0
                                        MatrixMetricCell(
                                            title = "Goal Savings Buffer",
                                            value = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", autoSweepThreshold)}",
                                            icon = Icons.Default.VerifiedUser,
                                            iconColor = SoftTeal,
                                            subtitle = "$capProgress% Target Funded",
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        val reservedTotal = totalPendingBillsAmount + accountMinBalance
                                        MatrixMetricCell(
                                            title = if (accountMinBalance > 0) "Queued + MAB" else "Queued AutoPay",
                                            value = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", reservedTotal)}",
                                            icon = Icons.Default.Schedule,
                                            iconColor = AccentPurple,
                                            subtitle = if (accountMinBalance > 0) {
                                                "${pendingBillsForAccount.size} bills + ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", accountMinBalance)} MAB"
                                            } else {
                                                "${pendingBillsForAccount.size} pending bills"
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    val netDelta = activeIncome - activeExpenses - activeTransfersOut
                                    MatrixMetricCell(
                                        title = "Net Cashflow",
                                        value = if (isDiscreetMode) "••••" else "${if (netDelta >= 0) "+" else "-"}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(netDelta))}",
                                        icon = if (netDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        iconColor = if (netDelta >= 0) SoftGreen else SoftRed,
                                        subtitle = "Retained balance delta",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Emergency Fund Runway Milestones Card
                        if (selectedTier == VaultTier.FORTRESS) {
                            val curBal = acc.currentBalance
                            val monthsCovered = if (avgMonthlySpend > 0.0) (curBal / avgMonthlySpend) else 0.0

                            val m3Target = avgMonthlySpend * 3
                            val m6Target = avgMonthlySpend * 6
                            val m12Target = avgMonthlySpend * 12

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(2.dp, RoundedCornerShape(18.dp)),
                                shape = RoundedCornerShape(18.dp),
                                color = CardWhite,
                                border = BorderStroke(0.8.dp, SoftTeal.copy(alpha = 0.35f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = null, tint = SoftTeal, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = "Emergency Runway Milestones",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.5.sp,
                                                    color = TextDark,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "Based on ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", avgMonthlySpend)}/mo spending average",
                                                    fontSize = 10.sp,
                                                    color = TextMuted
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = SoftTeal.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = "${String.format(Locale.US, "%.1f", monthsCovered)} Mo Covered",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SoftTeal,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        RunwayMilestoneRow(
                                            title = "3 Months Essential Buffer",
                                            target = m3Target,
                                            current = curBal,
                                            currencySymbol = userProfile.currencySymbol,
                                            isDiscreet = isDiscreetMode
                                        )
                                        RunwayMilestoneRow(
                                            title = "6 Months Security Cushion",
                                            target = m6Target,
                                            current = curBal,
                                            currencySymbol = userProfile.currencySymbol,
                                            isDiscreet = isDiscreetMode
                                        )
                                        RunwayMilestoneRow(
                                            title = "12 Months Fortress Immunity",
                                            target = m12Target,
                                            current = curBal,
                                            currencySymbol = userProfile.currencySymbol,
                                            isDiscreet = isDiscreetMode
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Strategic Vault Routing Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(18.dp))
                                .clickable { showRoutingDetailsSheet = true },
                            shape = RoundedCornerShape(18.dp),
                            color = CardWhite,
                            border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.20f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(imageVector = Icons.Default.Savings, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = "Strategic Vault Routing", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                    }

                                    Surface(shape = RoundedCornerShape(6.dp), color = AccentPurple.copy(alpha = 0.12f)) {
                                        Text(text = "92% On Plan", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentPurple, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(CircleShape)
                                        .background(CanvasLight)
                                ) {
                                    val totalOut = (activeExpenses + totalPendingBillsAmount + activeTransfersOut).coerceAtLeast(1.0)
                                    Box(modifier = Modifier.weight((activeExpenses / totalOut).toFloat().coerceIn(0.05f, 0.95f)).fillMaxHeight().background(Color(0xFFE57A28)))
                                    Box(modifier = Modifier.weight((totalPendingBillsAmount / totalOut).toFloat().coerceIn(0.05f, 0.95f)).fillMaxHeight().background(AccentPurple))
                                    Box(modifier = Modifier.weight(((activeTransfersOut + 1.0) / totalOut).toFloat().coerceIn(0.05f, 0.95f)).fillMaxHeight().background(SoftTeal))
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
                                        Text(text = "Monthly Commitments Covered", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Text(text = "Tap to view routing breakdown", fontSize = 10.sp, color = TextMuted)
                                    }

                                    Button(
                                        onClick = {
                                            transferPrefillAmount = if (calculatedSweepSurplus > 0) String.format(Locale.US, "%.0f", calculatedSweepSurplus) else ""
                                            transferPrefillNote = "${selectedTier.title} Surplus Sweep"
                                            transferFromAccount = activeAccount?.accountName.orEmpty()
                                            transferToAccount = fortressAccountName
                                            showTransferSheet = true
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text(text = "Sweep Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Gradient Scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CanvasLight.copy(alpha = 0.85f),
                            CanvasLight
                        )
                    )
                )
                .zIndex(2.5f)
        )

        // Bottom Dock
        AppBottomDock(
            currentSelection = NavigationTarget.VAULT_ACCOUNTS,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.MONTHLY_VIEW -> onNavigateToDashboard()
                    NavigationTarget.BUDGET_PLANNER -> onNavigateToPlanner()
                    NavigationTarget.DATA_SET -> onNavigateToTaxonomy()
                    NavigationTarget.REPORTS_ANALYTICS -> onNavigateToVaultAnalytics()
                    NavigationTarget.VAULT_ACCOUNTS -> { /* Active */ }
                    else -> {}
                }
            },
            fabActions = fabActions,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        )

        // Help Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text(text = "3-Vault Financial Strategy", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Text(
                        text = "• Operating Vault: Daily living expenses, dining, and immediate cash needs.\n\n" +
                                "• Commitments Vault: Dedicated buffer for rent, EMIs, and AutoPay bills.\n\n" +
                                "• Fortress Vault: Untouchable liquid emergency fund and investment sweeps.",
                        fontSize = 12.5.sp,
                        color = TextDark,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showHelpDialog = false }) {
                        Text(text = "Understood", fontWeight = FontWeight.Bold, color = AccentPurple)
                    }
                }
            )
        }

        // Edit Account Sheet
        editingAccount?.let { acc ->
            var nameText by remember(acc) { mutableStateOf(acc.accountName) }
            var selectedRole by remember(acc) { mutableStateOf(getVaultTier(acc.accountType, acc.accountName)) }
            var balanceText by remember(acc) { mutableStateOf(String.format(Locale.US, "%.2f", acc.currentBalance)) }
            var minBalanceText by remember(acc) { mutableStateOf(String.format(Locale.US, "%.0f", acc.minBalance)) }
            var isFdAccount by remember(acc) { mutableStateOf(acc.accountName.contains("FD", ignoreCase = true)) }
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { editingAccount = null },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Edit: ${acc.accountName}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)

                        IconButton(
                            onClick = {
                                accountToDelete = AccountEntity(
                                    accountName = acc.accountName,
                                    startingBalance = acc.startingBalance,
                                    accountType = acc.accountType,
                                    minBalance = acc.minBalance,
                                    sortOrder = acc.sortOrder
                                )
                            }
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Account", tint = SoftRed, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text(text = "Account Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Strategic Vault Role", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VaultTier.entries.forEach { tier ->
                            val isSel = selectedRole == tier
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { selectedRole = tier },
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

                    if (selectedRole == VaultTier.FORTRESS) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isFdAccount = !isFdAccount }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isFdAccount,
                                onCheckedChange = { isFdAccount = it },
                                colors = CheckboxDefaults.colors(checkedColor = SoftTeal)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "Locked Fixed Deposit (FD)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(text = "Marks account as term deposit reserve under Fortress tier", fontSize = 10.5.sp, color = TextMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text(text = "Current Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = minBalanceText,
                        onValueChange = { minBalanceText = it },
                        label = { Text(text = "Minimum Balance Floor (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        supportingText = { Text(text = "Bank minimum average balance to prevent maintenance penalties", fontSize = 10.5.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.7.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Modification Impact", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• Renaming will automatically update all linked transactions and fixed bills.\n• Role changes reallocate this balance in your asset allocation chart.\n• Balance adjustments create an automated ledger entry for the difference.",
                                fontSize = 10.5.sp,
                                color = TextMuted,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val targetBal = balanceText.toDoubleOrNull() ?: acc.currentBalance
                            val parsedMinBal = minBalanceText.toDoubleOrNull() ?: 0.0
                            if (nameText.isNotBlank()) {
                                pendingEditConfirmation = PendingEditConfirmation(
                                    originalAccount = acc,
                                    updatedName = nameText.trim().uppercase(),
                                    updatedRole = selectedRole,
                                    targetBalance = targetBal,
                                    minBalance = parsedMinBal,
                                    isFixedDeposit = isFdAccount
                                )
                            }
                        },
                        enabled = nameText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text(text = "Save Changes", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Confirmation Modal
        pendingEditConfirmation?.let { conf ->
            val isNameChanged = !conf.originalAccount.accountName.equals(conf.updatedName, ignoreCase = true)
            val isRoleChanged = !conf.originalAccount.accountType.equals(conf.updatedRole.title, ignoreCase = true)
            val isBalChanged = conf.targetBalance != conf.originalAccount.currentBalance
            val isMinBalChanged = conf.minBalance != conf.originalAccount.minBalance

            AlertDialog(
                onDismissRequest = { pendingEditConfirmation = null },
                title = { Text(text = "Confirm Account Modifications?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isNameChanged) {
                            Text(text = "• Rename: '${conf.originalAccount.accountName}' ➔ '${conf.updatedName}'")
                        }
                        if (isRoleChanged) {
                            Text(text = "• Strategic Role: '${conf.originalAccount.accountType}' ➔ '${conf.updatedRole.title}'")
                        }
                        if (isBalChanged) {
                            val diff = conf.targetBalance - conf.originalAccount.currentBalance
                            Text(text = "• Balance Adjustment: ${if (diff > 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", diff)}")
                        }
                        if (isMinBalChanged) {
                            Text(text = "• Minimum Balance: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", conf.minBalance)}")
                        }
                        if (!isNameChanged && !isRoleChanged && !isBalChanged && !isMinBalChanged) {
                            Text(text = "No changes detected.")
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val orig = conf.originalAccount
                            viewModel.updateAccountDetails(
                                oldName = orig.accountName,
                                newName = conf.updatedName,
                                startingBalance = orig.startingBalance,
                                accountType = conf.updatedRole.title,
                                sortOrder = orig.sortOrder
                            )

                            if (isBalChanged) {
                                viewModel.adjustAccountBalance(conf.updatedName, conf.targetBalance)
                            }

                            pendingEditConfirmation = null
                            editingAccount = null
                            Toast.makeText(context, "Account '${conf.updatedName}' updated", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Confirm & Apply", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingEditConfirmation = null }) {
                        Text(text = "Cancel", color = TextDark)
                    }
                }
            )
        }

        // Routing Details Sheet
        if (showRoutingDetailsSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { showRoutingDetailsSheet = false },
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Strategic Routing Breakdown", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                            Text(text = "Cashflow allocation & surplus routing analysis", fontSize = 11.5.sp, color = TextMuted)
                        }

                        Surface(shape = RoundedCornerShape(8.dp), color = AccentPurple.copy(alpha = 0.12f)) {
                            Text(text = "92% On Plan", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentPurple, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Outflow Distribution (This Cycle)", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE57A28)))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Everyday Spend & Living", fontSize = 12.sp, color = TextDark)
                                }
                                Text(text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", activeExpenses)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentPurple))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Queued / Paid AutoPay Bills", fontSize = 12.sp, color = TextDark)
                                }
                                Text(text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", totalPendingBillsAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SoftTeal))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Transfers & Fortress Sweeps", fontSize = 12.sp, color = TextDark)
                                }
                                Text(text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", activeTransfersOut)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = "Surplus Calculation Engine", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Current Liquid Balance", fontSize = 11.5.sp, color = TextMuted)
                                Text(text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", activeAccount?.currentBalance ?: 0.0)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                val label = if (accountMinBalance > 0) "Reserved (AutoPay + MAB)" else "Reserved for Upcoming AutoPay"
                                val reservedVal = totalPendingBillsAmount + accountMinBalance
                                Text(text = label, fontSize = 11.5.sp, color = TextMuted)
                                Text(text = if (isDiscreetMode) "••••" else "-${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", reservedVal)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SoftRed)
                            }
                            HorizontalDivider(color = BorderLight, thickness = 0.6.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = "Available Sweepable Surplus", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", calculatedSweepSurplus)}", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = SoftGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            showRoutingDetailsSheet = false
                            transferPrefillAmount = if (calculatedSweepSurplus > 0) String.format(Locale.US, "%.0f", calculatedSweepSurplus) else ""
                            transferPrefillNote = "${selectedTier.title} Surplus Sweep"
                            transferFromAccount = activeAccount?.accountName.orEmpty()
                            transferToAccount = fortressAccountName
                            showTransferSheet = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text(text = if (isDiscreetMode) "Sweep Surplus to Fortress" else "Sweep Surplus to Fortress (${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", calculatedSweepSurplus)})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Delete Account Dialog
        accountToDelete?.let { acc ->
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                title = { Text(text = "Delete Account?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = { Text(text = "Are you sure you want to remove '${acc.accountName}'? Accounts with existing transactions cannot be removed without reassigning.", fontSize = 13.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAccount(acc) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    editingAccount = null
                                }
                            }
                            accountToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(text = "Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToDelete = null }) {
                        Text(text = "Cancel", color = TextDark)
                    }
                }
            )
        }

        // Transfer Bottom Sheet
        if (showTransferSheet) {
            var fromAccount by remember(transferFromAccount) {
                mutableStateOf(transferFromAccount.ifBlank { accountNames.firstOrNull().orEmpty() })
            }
            var toAccount by remember(transferToAccount) {
                mutableStateOf(transferToAccount.ifBlank { accountNames.getOrNull(1) ?: accountNames.firstOrNull().orEmpty() })
            }
            var amountText by remember(transferPrefillAmount) { mutableStateOf(transferPrefillAmount) }
            var noteText by remember(transferPrefillNote) { mutableStateOf(transferPrefillNote) }
            var isAutoSweepMonthly by remember { mutableStateOf(false) }
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
                    Text(text = "Instant Vault Transfer", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Transfer liquidity between your bank accounts & vaults", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Source Account (From)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
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

                    Text(text = "Destination Account (To)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
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
                        label = { Text(text = "Transfer Amount (${userProfile.currencySymbol})", fontSize = 12.sp) },
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
                        label = { Text(text = "Note / Purpose (e.g., Strategic Vault Sweep)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { isAutoSweepMonthly = !isAutoSweepMonthly }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isAutoSweepMonthly,
                            onCheckedChange = { isAutoSweepMonthly = it },
                            colors = CheckboxDefaults.colors(checkedColor = AccentPurple)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(text = "Repeat Monthly (Auto-Sweep Rule)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text(text = "Adds this sweep to recurring monthly AutoPay commitments", fontSize = 10.5.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && fromAccount.isNotBlank() && toAccount.isNotBlank() && fromAccount != toAccount) {
                                viewModel.executeInstantTransfer(fromAccount, toAccount, amt, noteText)

                                if (isAutoSweepMonthly) {
                                    viewModel.addFixedBill(
                                        title = noteText.ifBlank { "Monthly Vault Sweep" },
                                        amount = amt,
                                        category = "Transfers",
                                        subcategory = "General",
                                        account = fromAccount,
                                        toAccount = toAccount,
                                        type = TransactionType.TRANSFER,
                                        dueDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
                                    )
                                }

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
                        Text(text = "Confirm Transfer", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Add Account Bottom Sheet
        if (showAddAccountSheet) {
            var name by remember { mutableStateOf("") }
            var balanceText by remember { mutableStateOf("") }
            var minBalanceText by remember { mutableStateOf("") }
            var selectedTierOption by remember { mutableStateOf(VaultTier.OPERATING) }
            var isFdAccount by remember { mutableStateOf(false) }
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
                    Text(text = "Add Vault Account", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(text = "Configure account name, strategic role, and starting balance", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(text = "Account Name (e.g., Primary Checking, Bills Vault)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "Strategic Vault Role", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VaultTier.entries.forEach { tier ->
                            val isSel = selectedTierOption == tier
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(9.dp))
                                    .clickable { selectedTierOption = tier },
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

                    if (selectedTierOption == VaultTier.FORTRESS) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { isFdAccount = !isFdAccount }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isFdAccount,
                                onCheckedChange = { isFdAccount = it },
                                colors = CheckboxDefaults.colors(checkedColor = SoftTeal)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(text = "Fixed Deposit (FD)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text(text = "Creates this account with automatic FD tagging under Fortress", fontSize = 10.5.sp, color = TextMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text(text = "Initial Starting Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = minBalanceText,
                        onValueChange = { minBalanceText = it },
                        label = { Text(text = "Minimum Balance Floor (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        supportingText = { Text(text = "Optional minimum average balance requirement", fontSize = 10.5.sp) },
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
                                val effectiveName = if (isFdAccount && !name.contains("FD", ignoreCase = true)) "${name.trim().uppercase()} (FD)" else name.trim().uppercase()
                                viewModel.addAccount(
                                    name = effectiveName,
                                    startingBalance = bal,
                                    type = selectedTierOption.title
                                )
                                showAddAccountSheet = false
                                Toast.makeText(context, "Account '$effectiveName' registered as ${selectedTierOption.title}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text(text = "Create Account", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                    Surface(modifier = Modifier.padding(vertical = 10.dp).width(40.dp).height(4.dp), shape = CircleShape, color = BorderLight) {}
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
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = payload.subtitle, fontSize = 12.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = payload.headline, fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextDark, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = payload.description, fontSize = 12.sp, color = TextMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { receiptPayload = null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text(text = payload.buttonText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun RunwayMilestoneRow(
    title: String,
    target: Double,
    current: Double,
    currencySymbol: String,
    isDiscreet: Boolean
) {
    val progress = if (target > 0.0) (current / target).toFloat().coerceIn(0f, 1f) else 0f
    val isComplete = current >= target && target > 0.0

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Text(
                text = if (isDiscreet) "••••" else if (isComplete) "100% Secured" else "${currencySymbol}${String.format(Locale.US, "%,.0f", current)} / ${currencySymbol}${String.format(Locale.US, "%,.0f", target)}",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isComplete) SoftGreen else TextMuted
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.5.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = if (isComplete) SoftGreen else SoftTeal,
            trackColor = BorderLight.copy(alpha = 0.5f)
        )
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
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(text = title, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextDark)
        Spacer(modifier = Modifier.height(1.dp))
        Text(text = subtitle, fontSize = 9.5.sp, color = TextMuted)
    }
}

@Composable
private fun BankAccountPhysicalCard(
    account: AccountBalanceResult,
    currencySymbol: String,
    tier: VaultTier,
    isSelected: Boolean,
    showRole: Boolean,
    isDiscreet: Boolean,
    autoSweepThreshold: Double,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maskedDigits = remember(account.accountName) {
        String.format(Locale.US, "%04d", abs(account.accountName.hashCode() % 9000 + 1000))
    }
    val isFortress = tier == VaultTier.FORTRESS

    val goalSavings = if (isFortress) min(account.currentBalance, autoSweepThreshold) else account.currentBalance
    val emergencyFd = if (isFortress) max(0.0, account.currentBalance - autoSweepThreshold) else 0.0

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
                        imageVector = when {
                            isFortress -> Icons.Default.Lock
                            tier == VaultTier.CASH -> Icons.Default.Payments
                            tier == VaultTier.COMMITMENTS -> Icons.Default.CreditCard
                            else -> Icons.Default.AccountBalance
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
                                text = if (isFortress) "Auto-Sweep & FD" else tier.title,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = tier.color,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Account",
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

            if (isFortress) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CanvasLight,
                    border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Goal Savings Buffer", fontSize = 10.5.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Text(
                                text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", goalSavings)}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Emergency FD Reserve", fontSize = 10.5.sp, color = SoftTeal, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (isDiscreet) "••••" else "$currencySymbol${String.format(Locale.US, "%,.0f", emergencyFd)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = SoftTeal
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = if (isDiscreet) "••••••••" else "$currencySymbol${String.format(Locale.US, "%,.2f", account.currentBalance)}",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    color = if (account.currentBalance >= 0) TextDark else SoftRed
                )
            }
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
                Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "$percentage%", fontSize = 10.sp, color = TextMuted)
            }
            Text(text = amount, fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextDark)
        }
    }
}
