package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.BorderStroke
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.myfin.data.TransferSubtype
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
// Add these missing imports to:
// - app/src/main/java/com/example/myfin/ui/screens/VaultStrategyScreen.kt
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.input.nestedscroll.nestedScroll


enum class VaultTier(
    val title: String,
    val description: String,
    val color: Color,
    val bgTint: Color,
    val icon: ImageVector
) {
    OPERATING("Operating", "Daily living & UPI spending", Color(0xFFE57A28), Color(0xFFFFF0D4), Icons.Default.AccountBalance),
    COMMITMENTS("Commitments", "AutoPay ring-fence & fixed bills", AccentPurple, Color(0xFFF3E5F5), Icons.Default.CreditCard),
    FORTRESS("Fortress", "Emergency savings & sweep FDs", SoftTeal, Color(0xFFE0F7FA), Icons.Default.Security),
    CASH("Cash", "Physical wallet & micro-spend", SoftGreen, Color(0xFFE6F8EF), Icons.Default.Payments)
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
    val minBalance: Double,
    val isArchived: Boolean
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
                name.contains("COMMITMENT") || name.contains("BILL") || name.contains("BOM") || name.contains("EMI") -> VaultTier.COMMITMENTS
                name.contains("FORTRESS") || name.contains("EMERGENCY") || name.contains("FD") || name.contains("RESERVE") || name.contains("INDUSIND") -> VaultTier.FORTRESS
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

    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()

    var showHelpDialog by remember { mutableStateOf(false) }
    var receiptPayload by remember { mutableStateOf<SuccessReceiptPayload?>(null) }

    var showTransferSheet by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var showRoutingDetailsSheet by remember { mutableStateOf(false) }
    var showReorderSheet by remember { mutableStateOf(false) }

    var editingAccount by remember { mutableStateOf<AccountBalanceResult?>(null) }
    var pendingEditConfirmation by remember { mutableStateOf<PendingEditConfirmation?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    val displayAccounts = remember(uiState.accounts) {
        uiState.accounts.filter { !it.isArchived }.sortedBy { it.sortOrder }
    }
    val archivedAccounts = remember(uiState.accounts) {
        uiState.accounts.filter { it.isArchived }
    }

    var activeSelectedCardIndex by remember { mutableIntStateOf(0) }
    val activeAccount = remember(displayAccounts, activeSelectedCardIndex) {
        displayAccounts.getOrNull(activeSelectedCardIndex.coerceIn(0, (displayAccounts.size - 1).coerceAtLeast(0)))
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

    // Fortress Auto-Sweep FD Logic
    val fortressCap = remember(userProfile.fortressThreshold) {
        if (userProfile.fortressThreshold > 0.0) userProfile.fortressThreshold else 25000.0
    }
    val fortressSavings = remember(fortTotal, fortressCap) { min(fortTotal, fortressCap) }
    val fortressFd = remember(fortTotal, fortressCap) { max(0.0, fortTotal - fortressCap) }
    val fortressSavingsFraction = if (fortressCap > 0) (fortressSavings / fortressCap).toFloat().coerceIn(0f, 1f) else 1f

    // Active Account Cash Flow Analysis
    val activeAccountTxs = remember(uiState.groupedTransactions, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        uiState.groupedTransactions.values.flatten().filter {
            it.accountName.equals(name, ignoreCase = true) || it.toAccountName.equals(name, ignoreCase = true)
        }
    }

    val activeExpenses = remember(activeAccountTxs, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        activeAccountTxs.filter { it.type == TransactionType.EXPENSE && it.accountName.equals(name, ignoreCase = true) }
            .sumOf { it.amount }
    }
    val activeIncome = remember(activeAccountTxs, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        activeAccountTxs.filter { it.type == TransactionType.INCOME && it.accountName.equals(name, ignoreCase = true) }
            .sumOf { it.amount }
    }
    val activeTransfersOut = remember(activeAccountTxs, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        activeAccountTxs.filter { it.type == TransactionType.TRANSFER && it.accountName.equals(name, ignoreCase = true) }
            .sumOf { it.amount }
    }
    val activeTransfersIn = remember(activeAccountTxs, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        activeAccountTxs.filter { it.type == TransactionType.TRANSFER && it.toAccountName.equals(name, ignoreCase = true) }
            .sumOf { it.amount }
    }

    // Dynamic Calendar & Velocity Math
    val calendar = remember { Calendar.getInstance() }
    val daysElapsed = remember { calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1) }
    val daysInMonth = remember { calendar.getActualMaximum(Calendar.DAY_OF_MONTH) }
    val daysRemaining = remember(daysElapsed, daysInMonth) { max(1, daysInMonth - daysElapsed) }

    val dailyBurnRate = remember(activeExpenses, daysElapsed) {
        if (activeExpenses > 0) activeExpenses / daysElapsed else 0.0
    }
    val runwayDays = remember(activeAccount?.currentBalance, dailyBurnRate) {
        val bal = activeAccount?.currentBalance ?: 0.0
        if (dailyBurnRate > 0 && bal > 0) (bal / dailyBurnRate).toInt() else 90
    }

    val pendingBillsForAccount = remember(uiState.fixedBills, activeAccount?.accountName) {
        val name = activeAccount?.accountName.orEmpty()
        uiState.fixedBills.filter {
            !it.isPaid && it.type == TransactionType.EXPENSE &&
                    (it.accountName.equals(name, ignoreCase = true) || it.accountName.isBlank())
        }
    }
    val totalPendingBillsAmount = remember(pendingBillsForAccount) {
        pendingBillsForAccount.sumOf { it.amount }
    }

    // Surplus Engine Math
    val mabBuffer = remember(activeAccount) { activeAccount?.minBalance ?: 0.0 }
    val calculatedSweepSurplus = remember(activeAccount?.currentBalance, totalPendingBillsAmount, dailyBurnRate, daysRemaining, mabBuffer) {
        val bal = activeAccount?.currentBalance ?: 0.0
        val monthlyRemainingSpendProtection = dailyBurnRate * daysRemaining
        (bal - mabBuffer - totalPendingBillsAmount - monthlyRemainingSpendProtection).coerceAtLeast(0.0)
    }

    val fortressDeficit = remember(fortTotal, fortressCap) {
        (fortressCap - fortTotal).coerceAtLeast(0.0)
    }

    // Standard App FAB Actions for Vault Screen
    val fabActions = remember {
        listOf(
            DockFabAction(
                icon = Icons.Default.AddCard,
                label = "Add Account",
                onClick = { showAddAccountSheet = true }
            ),
            DockFabAction(
                icon = Icons.Default.SyncAlt,
                label = "Sweep / Transfer",
                onClick = { showTransferSheet = true }
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
            .nestedScroll(scrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
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

                Text(
                    text = "3-Vault Strategy",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )

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
                                Icons.Default.Insights,
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
                                Icons.Default.Settings,
                                contentDescription = "Vault Settings",
                                tint = AccentPurple,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 125.dp)
            ) {
                // Vault Asset Allocation Card
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
                                        Icons.Default.HelpOutline,
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

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Dedicated Fortress Emergency FD & Savings Split Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = CardWhite,
                        border = BorderStroke(1.dp, SoftTeal.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFFFFFFFF),
                                            SoftTeal.copy(alpha = 0.04f)
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(SoftTeal.copy(alpha = 0.14f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Security,
                                            contentDescription = null,
                                            tint = SoftTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Fortress Vault Split",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = TextDark
                                        )
                                        Text(
                                            text = "Liquid Cushion vs. Emergency FD",
                                            fontSize = 10.5.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (fortressFd > 0) SoftTeal.copy(alpha = 0.12f) else SoftAmber.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (fortressFd > 0) "FD Active" else "Filling Cushion",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (fortressFd > 0) SoftTeal else SoftAmber,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Dual-Bucket Split Progress Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CanvasLight)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(fortressSavingsFraction.coerceAtLeast(0.02f))
                                        .fillMaxHeight()
                                        .background(SoftTeal)
                                )
                                if (fortressFd > 0) {
                                    val fdFraction = (fortressFd / fortTotal.coerceAtLeast(1.0)).toFloat().coerceIn(0.05f, 0.95f)
                                    Box(
                                        modifier = Modifier
                                            .weight(fdFraction)
                                            .fillMaxHeight()
                                            .background(Color(0xFF0D9488))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Sub-Bucket Breakdown Metrics
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Liquid Savings Cushion
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(SoftTeal))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Liquid Cushion", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortressSavings)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Text(
                                        text = "Cap: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortressCap)}",
                                        fontSize = 9.5.sp,
                                        color = TextMuted
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .height(32.dp)
                                        .width(1.dp)
                                        .background(BorderLight.copy(alpha = 0.7f))
                                )

                                // Emergency Fixed Deposit (FD)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF0D9488)))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("Emergency FD", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortressFd)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (fortressFd > 0) Color(0xFF0D9488) else TextMuted
                                    )
                                    Text(
                                        text = if (fortressFd > 0) "Auto-sweep excess" else "No excess swept",
                                        fontSize = 9.5.sp,
                                        color = TextMuted
                                    )
                                }
                            }

                            if (fortressDeficit > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "• Inflow needed: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortressDeficit)} to fill liquid cushion before FD auto-sweeps.",
                                    fontSize = 10.5.sp,
                                    color = SoftAmber,
                                    fontWeight = FontWeight.Medium
                                )
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Connected Bank Accounts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = TextDark
                            )
                            if (displayAccounts.size > 1) {
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { showReorderSheet = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.SwapVert, contentDescription = "Reorder", tint = AccentPurple, modifier = Modifier.size(17.dp))
                                }
                            }
                        }

                        Text(
                            text = "Tap card to focus",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Physical Bank Cards Horizontal Carousel
                if (displayAccounts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No active accounts found. Tap '+' to create one.", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            itemsIndexed(displayAccounts) { idx, acc ->
                                val tier = getVaultTier(acc.accountType, acc.accountName)
                                val isSelected = activeSelectedCardIndex == idx

                                BankAccountPhysicalCard(
                                    account = acc,
                                    currencySymbol = userProfile.currencySymbol,
                                    tier = tier,
                                    isSelected = isSelected,
                                    showRole = true,
                                    onSelect = { activeSelectedCardIndex = idx },
                                    onEdit = { editingAccount = acc },
                                    modifier = Modifier.width(260.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // Focused Account Cashflow Matrix & MAB Protection
                activeAccount?.let { acc ->
                    item {
                        val isMabBreached = acc.minBalance > 0 && acc.currentBalance < acc.minBalance
                        val deficit = (acc.minBalance - acc.currentBalance).coerceAtLeast(0.0)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Account Cashflow Matrix (${acc.accountName})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = TextDark
                            )

                            if (isMabBreached) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SoftRed.copy(alpha = 0.12f),
                                    border = BorderStroke(0.6.dp, SoftRed.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "! MAB Penalty Risk (-${userProfile.currencySymbol}${deficit.toInt()})",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, if (isMabBreached) SoftRed.copy(alpha = 0.4f) else BorderLight.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    MatrixMetricCell(
                                        title = "Daily Burn Rate",
                                        value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", dailyBurnRate)}/day",
                                        icon = Icons.Default.Whatshot,
                                        iconColor = SoftRed,
                                        subtitle = "Avg spend velocity ($daysElapsed days)",
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
                                        title = "Ring-Fenced AutoPay",
                                        value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalPendingBillsAmount)}",
                                        icon = Icons.Default.Schedule,
                                        iconColor = AccentPurple,
                                        subtitle = "${pendingBillsForAccount.size} queued fixed bills",
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    val netDelta = (activeIncome + activeTransfersIn) - (activeExpenses + activeTransfersOut)
                                    MatrixMetricCell(
                                        title = "Net Cashflow",
                                        value = "${if (netDelta >= 0) "+" else "-"}${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", abs(netDelta))}",
                                        icon = if (netDelta >= 0) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                                        iconColor = if (netDelta >= 0) SoftGreen else SoftRed,
                                        subtitle = "Retained ledger delta",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Strategic Vault Routing Engine Card
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
                                        Icon(Icons.Default.Savings, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(17.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Surplus Routing Engine", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                    }

                                    Surface(shape = RoundedCornerShape(6.dp), color = AccentPurple.copy(alpha = 0.12f)) {
                                        Text(
                                            text = if (calculatedSweepSurplus > 0) "Surplus Available" else "Deficit Protected",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentPurple,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
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
                                        Text(
                                            text = if (calculatedSweepSurplus > 0) "Sweepable: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", calculatedSweepSurplus)}" else "Zero Leakage Protected",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                        Text("Tap to view full surplus engine math", fontSize = 10.sp, color = TextMuted)
                                    }

                                    Button(
                                        onClick = { showTransferSheet = true },
                                        enabled = calculatedSweepSurplus > 0,
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

                // Archived Accounts Section
                if (archivedAccounts.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "Archived Accounts (${archivedAccounts.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            archivedAccounts.forEach { acc ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = CardWhite,
                                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = acc.accountName,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextMuted
                                            )
                                            Text(
                                                text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", acc.currentBalance)}",
                                                fontSize = 12.sp,
                                                color = TextMuted
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                viewModel.unarchiveAccount(acc.accountName)
                                                Toast.makeText(context, "Account unarchived", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Unarchive, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Restore", fontSize = 12.sp, color = AccentPurple, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. BOTTOM GRADIENT SCRIM
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

        // 4. STANDARDIZED FLOATING BOTTOM NAVIGATION DOCK WITH FAB
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

        // Edit Account Sheet
        editingAccount?.let { acc ->
            var nameText by remember(acc) { mutableStateOf(acc.accountName) }
            var selectedRole by remember(acc) { mutableStateOf(getVaultTier(acc.accountType, acc.accountName)) }
            var balanceText by remember(acc) { mutableStateOf(String.format(Locale.US, "%.2f", acc.currentBalance)) }
            var minBalanceText by remember(acc) { mutableStateOf(acc.minBalance.toString()) }
            var isArchivedState by remember(acc) { mutableStateOf(acc.isArchived) }
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
                        Text("Edit: ${acc.accountName}", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)

                        IconButton(
                            onClick = {
                                accountToDelete = AccountEntity(
                                    accountName = acc.accountName,
                                    startingBalance = acc.startingBalance,
                                    accountType = acc.accountType,
                                    minBalance = acc.minBalance,
                                    isArchived = acc.isArchived,
                                    sortOrder = acc.sortOrder
                                )
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = SoftRed, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = nameText,
                        onValueChange = { nameText = it },
                        label = { Text("Account Name", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Strategic Vault Role", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        VaultTier.values().forEach { tier ->
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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Current Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
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
                        label = { Text("Minimum Balance Threshold / MAB (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Archive Status Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CanvasLight)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Archive Account", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            Text("Hide from active vaults and transactions", fontSize = 10.5.sp, color = TextMuted)
                        }
                        Switch(
                            checked = isArchivedState,
                            onCheckedChange = { isArchivedState = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentPurple)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.7.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Modification Impact", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
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
                            val minBal = minBalanceText.toDoubleOrNull() ?: acc.minBalance
                            if (nameText.isNotBlank()) {
                                pendingEditConfirmation = PendingEditConfirmation(
                                    originalAccount = acc,
                                    updatedName = nameText.trim().uppercase(),
                                    updatedRole = selectedRole,
                                    targetBalance = targetBal,
                                    minBalance = minBal,
                                    isArchived = isArchivedState
                                )
                            }
                        },
                        enabled = nameText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Confirmation Modal Before Applying Changes
        pendingEditConfirmation?.let { conf ->
            val isNameChanged = !conf.originalAccount.accountName.equals(conf.updatedName, ignoreCase = true)
            val isRoleChanged = !conf.originalAccount.accountType.equals(conf.updatedRole.title, ignoreCase = true)
            val isBalChanged = conf.targetBalance != conf.originalAccount.currentBalance
            val isMabChanged = conf.minBalance != conf.originalAccount.minBalance
            val isArchiveChanged = conf.isArchived != conf.originalAccount.isArchived

            AlertDialog(
                onDismissRequest = { pendingEditConfirmation = null },
                title = { Text("Confirm Account Modifications?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isNameChanged) {
                            Text("• Rename: '${conf.originalAccount.accountName}' ➔ '${conf.updatedName}'")
                        }
                        if (isRoleChanged) {
                            Text("• Strategic Role: '${conf.originalAccount.accountType}' ➔ '${conf.updatedRole.title}'")
                        }
                        if (isBalChanged) {
                            val diff = conf.targetBalance - conf.originalAccount.currentBalance
                            Text("• Balance Adjustment: ${if (diff > 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", diff)}")
                        }
                        if (isMabChanged) {
                            Text("• Min Balance (MAB): ${userProfile.currencySymbol}${conf.minBalance.toInt()}")
                        }
                        if (isArchiveChanged) {
                            Text("• Archive Status: ${if (conf.isArchived) "Archived" else "Active"}")
                        }
                        if (!isNameChanged && !isRoleChanged && !isBalChanged && !isMabChanged && !isArchiveChanged) {
                            Text("No changes detected.")
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
                                minBalance = conf.minBalance,
                                sortOrder = orig.sortOrder
                            )

                            if (conf.isArchived != orig.isArchived) {
                                if (conf.isArchived) {
                                    viewModel.archiveAccount(conf.updatedName)
                                } else {
                                    viewModel.unarchiveAccount(conf.updatedName)
                                }
                            }

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
                        Text("Confirm & Apply", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingEditConfirmation = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        // Reorder Accounts Bottom Sheet
        if (showReorderSheet) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            var reorderedList by remember(displayAccounts) { mutableStateOf(displayAccounts) }

            ModalBottomSheet(
                onDismissRequest = { showReorderSheet = false },
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
                    Text("Reorder Vault Accounts", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Text("Adjust card sequence in your strategy carousel", fontSize = 11.5.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(reorderedList) { index, account ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = CanvasLight,
                                border = BorderStroke(0.6.dp, BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. ${account.accountName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = TextDark
                                    )

                                    Row {
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val mutable = reorderedList.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index - 1]
                                                    mutable[index - 1] = temp
                                                    reorderedList = mutable
                                                }
                                            },
                                            enabled = index > 0,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = if (index > 0) AccentPurple else TextMuted.copy(alpha = 0.4f))
                                        }

                                        IconButton(
                                            onClick = {
                                                if (index < reorderedList.size - 1) {
                                                    val mutable = reorderedList.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index + 1]
                                                    mutable[index + 1] = temp
                                                    reorderedList = mutable
                                                }
                                            },
                                            enabled = index < reorderedList.size - 1,
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = if (index < reorderedList.size - 1) AccentPurple else TextMuted.copy(alpha = 0.4f))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val updatedEntities = reorderedList.mapIndexed { i, acc ->
                                AccountEntity(
                                    accountName = acc.accountName,
                                    startingBalance = acc.startingBalance,
                                    accountType = acc.accountType,
                                    minBalance = acc.minBalance,
                                    isArchived = acc.isArchived,
                                    sortOrder = i
                                )
                            }
                            viewModel.reorderAccounts(updatedEntities)
                            showReorderSheet = false
                            Toast.makeText(context, "Account sequence saved", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save New Order", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Routing & Surplus Engine Breakdown Details Sheet
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
                            Text("Surplus Engine Breakdown", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                            Text("Ring-fenced obligations & sweep mechanics", fontSize = 11.5.sp, color = TextMuted)
                        }

                        Surface(shape = RoundedCornerShape(8.dp), color = AccentPurple.copy(alpha = 0.12f)) {
                            Text("3-Vault Guard", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentPurple, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Cycle Outflow Distribution", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
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
                                    Text("Everyday Spend & Living", fontSize = 12.sp, color = TextDark)
                                }
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", activeExpenses)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentPurple))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Queued AutoPay Bills", fontSize = 12.sp, color = TextDark)
                                }
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", totalPendingBillsAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SoftTeal))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Transfers & Fortress Sweeps", fontSize = 12.sp, color = TextDark)
                                }
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", activeTransfersOut)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Surplus Calculation Engine", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.6.dp, BorderLight)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Current Balance", fontSize = 11.5.sp, color = TextMuted)
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", activeAccount?.currentBalance ?: 0.0)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                            if (mabBuffer > 0.0) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("MAB Protected Floor", fontSize = 11.5.sp, color = TextMuted)
                                    Text("-${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", mabBuffer)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SoftAmber)
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Reserved for Queued AutoPay", fontSize = 11.5.sp, color = TextMuted)
                                Text("-${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", totalPendingBillsAmount)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = SoftRed)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Remaining Month Spend Cushion ($daysRemaining d)", fontSize = 11.5.sp, color = TextMuted)
                                Text("-${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", dailyBurnRate * daysRemaining)}", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            }
                            HorizontalDivider(color = BorderLight, thickness = 0.6.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("True Sweepable Surplus", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                Text("${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", calculatedSweepSurplus)}", fontSize = 13.5.sp, fontWeight = FontWeight.Black, color = SoftGreen)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            showRoutingDetailsSheet = false
                            showTransferSheet = true
                        },
                        enabled = calculatedSweepSurplus > 0,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Sweep Surplus (${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", calculatedSweepSurplus)})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Delete Account Alert
        accountToDelete?.let { acc ->
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                title = { Text("Delete Account?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = { Text("Are you sure you want to remove '${acc.accountName}'? If it has linked transactions or AutoPay bills, deletion will be refused until reallocated.", fontSize = 13.sp) },
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
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToDelete = null }) {
                        Text("Cancel", color = TextDark)
                    }
                }
            )
        }

        // Transfer Bottom Sheet
        if (showTransferSheet) {
            var fromAccount by remember { mutableStateOf(activeAccount?.accountName ?: accountNames.firstOrNull().orEmpty()) }
            var toAccount by remember { mutableStateOf(accountNames.firstOrNull { it != fromAccount } ?: "") }
            var amountText by remember { mutableStateOf(if (calculatedSweepSurplus > 0) String.format(Locale.US, "%.0f", calculatedSweepSurplus) else "") }
            var selectedSubtype by remember { mutableStateOf(TransferSubtype.WEALTH_ALLOCATION) }
            var noteText by remember { mutableStateOf("Strategic Vault Sweep") }
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
                    Text("Strategic Vault Sweep & Transfer", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Text("Reallocate cashflow with zero-leakage classification", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Source Vault (From)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
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

                    Text("Destination Vault (To)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
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

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Transfer Classification Subtype", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            TransferSubtype.BILL_FUNDING to "Bill Funding",
                            TransferSubtype.WEALTH_ALLOCATION to "Fortress Sweep",
                            TransferSubtype.REBALANCE to "Rebalance"
                        ).forEach { (subtype, label) ->
                            val isSel = selectedSubtype == subtype
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { selectedSubtype = subtype },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) AccentPurple.copy(alpha = 0.14f) else CanvasLight,
                                border = BorderStroke(0.6.dp, if (isSel) AccentPurple else BorderLight)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.5.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) AccentPurple else TextDark,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 7.dp)
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
                        label = { Text("Purpose Note", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val amt = amountText.toDoubleOrNull() ?: 0.0
                            if (amt > 0 && fromAccount.isNotBlank() && toAccount.isNotBlank() && fromAccount != toAccount) {
                                viewModel.executeInstantTransfer(
                                    fromAccount = fromAccount,
                                    toAccount = toAccount,
                                    amount = amt,
                                    note = noteText,
                                    subtype = selectedSubtype
                                )
                                showTransferSheet = false
                                receiptPayload = SuccessReceiptPayload(
                                    subtitle = "Vault Sweep Completed",
                                    headline = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", amt)}",
                                    description = "$fromAccount ➔ $toAccount (${selectedSubtype.name})",
                                    buttonText = "Done"
                                )
                            } else {
                                Toast.makeText(context, "Select distinct accounts and an amount > 0", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Confirm Transfer", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Add Account Bottom Sheet
        if (showAddAccountSheet) {
            var name by remember { mutableStateOf("") }
            var balanceText by remember { mutableStateOf("") }
            var minBalanceText by remember { mutableStateOf("0") }
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
                    Text("Provision account name, strategic role, and starting balance", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (e.g., HDFC Salary, ICICI Bills)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { balanceText = it },
                        label = { Text("Initial Starting Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
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
                        label = { Text("Minimum Balance Threshold / MAB (${userProfile.currencySymbol})", fontSize = 12.sp) },
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
                                val minBal = minBalanceText.toDoubleOrNull() ?: 0.0
                                viewModel.addAccount(
                                    name = name.trim().uppercase(),
                                    startingBalance = bal,
                                    type = selectedTier.title,
                                    minBalance = minBal
                                )
                                showAddAccountSheet = false
                                Toast.makeText(context, "Account '${name.trim().uppercase()}' registered", Toast.LENGTH_SHORT).show()
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

        // Philosophy Guide Dialog
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(32.dp)) },
                title = { Text("3-Vault Financial Architecture", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Operating Vault: Receives income, handles daily living expenses, groceries, and UPI transfers. Kept at 1-2 months runway.", fontSize = 12.sp, color = TextDark)
                        Text("2. Commitments Vault: Ring-fences recurring EMIs, AutoPay bills, utilities, and rent so money cannot accidentally be spent.", fontSize = 12.sp, color = TextDark)
                        Text("3. Fortress Vault: High-security emergency buffer. Splits automatically into liquid savings (instant cushion) and high-yield sweep FDs (excess).", fontSize = 12.sp, color = TextDark)
                        Text("4. Cash Wallet: Physical cash on hand for micro-payments.", fontSize = 12.sp, color = TextDark)
                    }
                },
                confirmButton = {
                    Button(onClick = { showHelpDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)) {
                        Text("Got it")
                    }
                }
            )
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
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
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
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text(text = payload.buttonText, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // Standardized Floating Bottom Navigation Dock with FAB
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
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val maskedDigits = remember(account.accountName) {
        String.format(Locale.US, "%04d", abs(account.accountName.hashCode() % 9000 + 1000))
    }
    val isMabBreached = account.minBalance > 0 && account.currentBalance < account.minBalance

    Surface(
        modifier = modifier
            .shadow(if (isSelected) 4.dp else 1.5.dp, RoundedCornerShape(20.dp))
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 0.8.dp,
            color = if (isMabBreached) SoftRed else if (isSelected) tier.color else BorderLight.copy(alpha = 0.7f)
        )
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
                        imageVector = tier.icon,
                        contentDescription = null,
                        tint = tier.color,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isMabBreached) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SoftRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "! MAB",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = SoftRed,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }

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
                        onClick = onEdit,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "•••• $maskedDigits",
                    fontSize = 11.sp,
                    color = TextMuted
                )
                if (account.minBalance > 0.0) {
                    Text(
                        text = "MAB: $currencySymbol${account.minBalance.toInt()}",
                        fontSize = 10.sp,
                        color = if (isMabBreached) SoftRed else TextMuted,
                        fontWeight = if (isMabBreached) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }

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
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.width(4.dp))
                Text("$percentage%", fontSize = 10.sp, color = TextMuted)
            }
            Text(amount, fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextDark)
        }
    }
}
