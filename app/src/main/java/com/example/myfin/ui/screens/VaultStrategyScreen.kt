package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.myfin.data.AccountBalanceResult
import com.example.myfin.data.AccountEntity
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.TransferSubtype
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.CommitmentsShortfallStatus
import com.example.myfin.ui.PaydayAllocationPlan
import com.example.myfin.ui.components.AccountTransferDialog
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

enum class VaultSubTab(val label: String, val icon: ImageVector) {
    OVERVIEW("Vaults", Icons.Default.AccountBalance),
    ROUTING("Routing", Icons.Default.AltRoute),
    FORTRESS("Fortress", Icons.Default.Shield)
}

enum class BankRole(
    val title: String,
    val roleKey: String,
    val subtitle: String,
    val badgeColor: Color
) {
    OPERATING("Operating Core", "Operating", "Salary & Daily Living Outflow", Color(0xFF00D2EE)),
    COMMITMENTS("Commitments Vault", "Commitments", "AutoPay, Subscriptions & SIPs", Color(0xFF6C5CE7)),
    FORTRESS("Fortress Sweep", "Fortress", "Liquid + Auto-Swept FDs", Color(0xFF10B981)),
    GENERAL("Liquid Reserve", "General", "General Liquid Account", Color(0xFF64748B))
}

data class CardPalette(
    val baseColor: Color,
    val discColor1: Color,
    val discColor2: Color,
    val discColor3: Color,
    val bankCode: String
)

private val CyanPrimary = Color(0xFF00D2EE)
private val PurplePrimary = Color(0xFF6C5CE7)
private val TealPrimary = Color(0xFF10B981)
private val CoralAccent = Color(0xFFFF6B6B)
private val MagentaAccent = Color(0xFFE056FD)
private val BlueAccent = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val accounts = remember(uiState.accounts) { uiState.accounts.filter { !it.isArchived } }
    val totalLiquid = remember(accounts) { accounts.sumOf { it.currentBalance } }

    val allTransactions = remember(uiState.groupedTransactions) {
        uiState.groupedTransactions.values.flatten()
    }

    val accountsList = remember(accounts) { accounts.map { it.accountName } }

    val pagerState = rememberPagerState(pageCount = { accounts.size + 1 })
    var currentSubTab by remember { mutableStateOf(VaultSubTab.OVERVIEW) }

    var selectedAccountForEdit by remember { mutableStateOf<AccountBalanceResult?>(null) }
    var showAccountAnalyticsSheet by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showReorderSheet by remember { mutableStateOf(false) }
    var showFortressCalibrationSheet by remember { mutableStateOf(false) }

    val operatingBank = remember(accounts) {
        accounts.firstOrNull { getAccountRole(it.accountName, it.accountType) == BankRole.OPERATING }
            ?: accounts.firstOrNull()
    }
    val commitmentBank = remember(accounts) {
        accounts.firstOrNull { getAccountRole(it.accountName, it.accountType) == BankRole.COMMITMENTS }
    }
    val fortressBank = remember(accounts) {
        accounts.firstOrNull { getAccountRole(it.accountName, it.accountType) == BankRole.FORTRESS }
    }

    val totalPendingCommitments = remember(uiState.fixedBills) {
        uiState.fixedBills.filter { !it.isPaid && (it.type == TransactionType.EXPENSE || it.type == TransactionType.ASSET) }.sumOf { it.amount }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CardWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenDrawer()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Menu / Back",
                        tint = Color(0xFF1E202E),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "3-Bank Vault Strategy",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E202E)
                    )
                    Text(
                        text = "Operating • Commitments • Fortress",
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showReorderSheet = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CanvasLight)
                    ) {
                        Icon(Icons.Default.SwapHoriz, contentDescription = "Rearrange Cards", tint = TextDark, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showAddAccountSheet = true
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentPurple.copy(alpha = 0.12f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Vault", tint = AccentPurple, modifier = Modifier.size(20.dp))
                    }
                }
            }

            Crossfade(
                targetState = currentSubTab,
                animationSpec = tween(220),
                label = "vaultTabs",
                modifier = Modifier.weight(1f)
            ) { tab ->
                when (tab) {
                    VaultSubTab.OVERVIEW -> {
                        VaultsOverviewContent(
                            accounts = accounts,
                            pagerState = pagerState,
                            totalLiquid = totalLiquid,
                            userProfileCurrency = userProfile.currencySymbol,
                            fortressThreshold = userProfile.fortressThreshold,
                            allTransactions = allTransactions,
                            totalPendingCommitments = totalPendingCommitments,
                            commitmentsShortfall = uiState.commitmentsShortfall,
                            paydayPlan = uiState.paydaySuggestion,
                            onApplyPaydayAllocation = { plan ->
                                viewModel.applyPaydayAllocation(
                                    plan = plan,
                                    operatingAccount = operatingBank?.accountName ?: "Primary Bank",
                                    commitmentsAccount = commitmentBank?.accountName ?: "Secondary Bank",
                                    fortressAccount = fortressBank?.accountName ?: "Tertiary Bank"
                                )
                                Toast.makeText(context, "Salary successfully distributed!", Toast.LENGTH_SHORT).show()
                            },
                            onTriggerShortfallTransfer = {
                                showTransferDialog = true
                            },
                            onSelectAccountEdit = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedAccountForEdit = it
                            },
                            onLongPressCard = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showReorderSheet = true
                            }
                        )
                    }
                    VaultSubTab.ROUTING -> {
                        RoutingAllocatorTabContent(
                            operatingBank = operatingBank,
                            commitmentBank = commitmentBank,
                            fortressBank = fortressBank,
                            currencySymbol = userProfile.currencySymbol,
                            pendingCommitments = totalPendingCommitments,
                            allTransactions = allTransactions,
                            commitmentsShortfall = uiState.commitmentsShortfall,
                            onTriggerSweep = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showTransferDialog = true
                            }
                        )
                    }
                    VaultSubTab.FORTRESS -> {
                        FortressSweepTabContent(
                            fortressBank = fortressBank,
                            currencySymbol = userProfile.currencySymbol,
                            fortressThreshold = userProfile.fortressThreshold,
                            monthlyBurnRate = uiState.metrics.actualExpenses.coerceAtLeast(uiState.metrics.plannedExpenses).coerceAtLeast(1.0),
                            onOpenCalibration = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showFortressCalibrationSheet = true
                            }
                        )
                    }
                }
            }
        }

        // Floating Navigation Dock
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .height(60.dp)
                        .weight(1f)
                        .shadow(14.dp, RoundedCornerShape(30.dp)),
                    shape = RoundedCornerShape(30.dp),
                    color = CardWhite
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        VaultSubTab.entries.forEach { subTab ->
                            val isSelected = currentSubTab == subTab
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) AccentPurple.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentSubTab = subTab
                                    }
                                    .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = subTab.icon,
                                        contentDescription = subTab.label,
                                        tint = if (isSelected) AccentPurple else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = subTab.label,
                                            color = AccentPurple,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Surface(
                    modifier = Modifier
                        .size(60.dp)
                        .shadow(14.dp, CircleShape)
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToDashboard()
                        },
                    shape = CircleShape,
                    color = Color(0xFF181A2A)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Switch to Dashboard",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal: Add Account Bottom Sheet
    if (showAddAccountSheet) {
        CreateVaultModalSheet(
            currencySymbol = userProfile.currencySymbol,
            onDismiss = { showAddAccountSheet = false },
            onCreateVault = { name, initialBal, roleKey, minBal ->
                viewModel.addAccount(name, initialBal, roleKey, minBal)
                Toast.makeText(context, "Vault created successfully", Toast.LENGTH_SHORT).show()
                showAddAccountSheet = false
            }
        )
    }

    // Modal: Balance & Settings Edit Sheet
    selectedAccountForEdit?.let { account ->
        BalanceEditModalSheet(
            account = account,
            currencySymbol = userProfile.currencySymbol,
            onDismiss = { selectedAccountForEdit = null },
            onOpenAnalytics = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showAccountAnalyticsSheet = true
            },
            onSave = { newName, newRoleKey, newBalance, newMinBalance, isArchived ->
                viewModel.updateAccountDetails(
                    oldName = account.accountName,
                    newName = newName.trim(),
                    startingBalance = newBalance,
                    accountType = newRoleKey,
                    minBalance = newMinBalance,
                    isArchived = isArchived,
                    sortOrder = account.sortOrder
                )
                Toast.makeText(context, "Vault updated", Toast.LENGTH_SHORT).show()
                selectedAccountForEdit = null
            }
        )
    }

    // Modal: Rearrange Card Sequence Sheet
    if (showReorderSheet) {
        ReorderVaultsModalSheet(
            accounts = accounts,
            onDismiss = { showReorderSheet = false },
            onSaveOrder = { reorderedList ->
                viewModel.reorderAccounts(reorderedList)
                showReorderSheet = false
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
                Toast.makeText(context, "Card sequence updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Fortress Calibration Sheet
    if (showFortressCalibrationSheet) {
        FortressCalibrationModalSheet(
            currentThreshold = userProfile.fortressThreshold,
            currencySymbol = userProfile.currencySymbol,
            fortressBank = fortressBank,
            onDismiss = { showFortressCalibrationSheet = false },
            onSaveSettings = { newThreshold, newBalance ->
                viewModel.updateFortressThreshold(newThreshold)
                if (fortressBank != null && newBalance != null) {
                    viewModel.updateAccountDetails(
                        oldName = fortressBank.accountName,
                        newName = fortressBank.accountName,
                        startingBalance = newBalance,
                        accountType = fortressBank.accountType,
                        minBalance = fortressBank.minBalance,
                        isArchived = fortressBank.isArchived,
                        sortOrder = fortressBank.sortOrder
                    )
                }
                showFortressCalibrationSheet = false
                Toast.makeText(context, "Fortress configuration updated", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Modal: Bank Analytics Sheet
    if (showAccountAnalyticsSheet && selectedAccountForEdit != null) {
        val targetAccount = selectedAccountForEdit!!
        val targetTransactions = remember(allTransactions, targetAccount) {
            allTransactions.filter { it.accountName.equals(targetAccount.accountName, ignoreCase = true) }
        }
        BankAnalyticsModalSheet(
            account = targetAccount,
            currencySymbol = userProfile.currencySymbol,
            transactions = targetTransactions,
            onDismiss = { showAccountAnalyticsSheet = false },
            onArchiveAccount = {
                viewModel.archiveAccount(targetAccount.accountName)
                Toast.makeText(context, "${targetAccount.accountName} archived", Toast.LENGTH_SHORT).show()
                showAccountAnalyticsSheet = false
                selectedAccountForEdit = null
            },
            onDeleteAccount = {
                viewModel.deleteAccount(
                    AccountEntity(
                        accountName = targetAccount.accountName,
                        startingBalance = targetAccount.startingBalance,
                        accountType = targetAccount.accountType,
                        minBalance = targetAccount.minBalance,
                        isArchived = targetAccount.isArchived,
                        sortOrder = targetAccount.sortOrder
                    )
                ) { success, message ->
                    if (success) {
                        Toast.makeText(context, "${targetAccount.accountName} removed", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, message.ifBlank { "Could not delete account" }, Toast.LENGTH_SHORT).show()
                    }
                }
                showAccountAnalyticsSheet = false
                selectedAccountForEdit = null
            }
        )
    }

    // Dialog: Inter-Vault Transfer
    if (showTransferDialog) {
        AccountTransferDialog(
            accounts = accountsList,
            onDismiss = { showTransferDialog = false },
            onTransfer = { from, to, amount, note ->
                val subtype = when {
                    to.contains("Commitment", ignoreCase = true) || to.contains("AutoPay", ignoreCase = true) -> TransferSubtype.BILL_FUNDING
                    to.contains("Fortress", ignoreCase = true) || to.contains("FD", ignoreCase = true) -> TransferSubtype.WEALTH_ALLOCATION
                    to.contains("Cash", ignoreCase = true) -> TransferSubtype.CASH_WITHDRAWAL
                    else -> TransferSubtype.REBALANCE
                }
                viewModel.executeInstantTransfer(from, to, amount, note, subtype)
            }
        )
    }
}

// TAB 1: VAULTS OVERVIEW
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VaultsOverviewContent(
    accounts: List<AccountBalanceResult>,
    pagerState: androidx.compose.foundation.pager.PagerState,
    totalLiquid: Double,
    userProfileCurrency: String,
    fortressThreshold: Double,
    allTransactions: List<TransactionEntity>,
    totalPendingCommitments: Double,
    commitmentsShortfall: CommitmentsShortfallStatus,
    paydayPlan: PaydayAllocationPlan?,
    onApplyPaydayAllocation: (PaydayAllocationPlan) -> Unit,
    onTriggerShortfallTransfer: () -> Unit,
    onSelectAccountEdit: (AccountBalanceResult) -> Unit,
    onLongPressCard: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.height(4.dp))

        // Payday Suggestion Banner
        if (paydayPlan != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = TealPrimary.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, TealPrimary.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Payday Inflow Detected", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                        }
                        Text(
                            text = "$userProfileCurrency${String.format(Locale.US, "%,.0f", paydayPlan.salaryAmount)}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = TealPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Allocate $userProfileCurrency${String.format(Locale.US, "%,.0f", paydayPlan.toCommitments)} to Commitments (${paydayPlan.pendingBillsCount} bills + MAB) and $userProfileCurrency${String.format(Locale.US, "%,.0f", paydayPlan.toFortress)} to Fortress SIP?",
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onApplyPaydayAllocation(paydayPlan) },
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp).align(Alignment.End)
                    ) {
                        Text("1-Tap Allocate Salary", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Commitments Shortfall Warning Banner
        if (commitmentsShortfall.isShortfall) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = CoralAccent.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, CoralAccent.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = CoralAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Commitments Shortfall Warning", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextDark)
                            val dueText = if (commitmentsShortfall.earliestDueDay != null) " by ${commitmentsShortfall.earliestDueDay}th" else ""
                            Text(
                                text = "Transfer $userProfileCurrency${String.format(Locale.US, "%,.0f", commitmentsShortfall.shortfallAmount)}$dueText to protect MAB & pending bills.",
                                fontSize = 10.5.sp,
                                color = TextMuted,
                                lineHeight = 13.sp
                            )
                        }
                    }
                    Button(
                        onClick = onTriggerShortfallTransfer,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 24.dp),
            pageSpacing = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
        ) { page ->
            val pageOffset = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val cardScale = lerp(0.92f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
            val cardAlpha = lerp(0.65f, 1f, 1f - pageOffset.coerceIn(0f, 1f))
            val rotation = lerp(0f, 6f, pageOffset.coerceIn(0f, 1f)) * (if (pagerState.currentPage > page) -1 else 1)

            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = cardScale
                        scaleY = cardScale
                        alpha = cardAlpha
                        rotationY = rotation
                        cameraDistance = 12 * density
                    }
            ) {
                if (page == 0) {
                    MasterReservesCard(
                        currencySymbol = userProfileCurrency,
                        totalBalance = totalLiquid,
                        accountCount = accounts.size
                    )
                } else {
                    val acc = accounts.getOrNull(page - 1)
                    if (acc != null) {
                        val role = getAccountRole(acc.accountName, acc.accountType)
                        EnhancedBankCardItem(
                            account = acc,
                            role = role,
                            currencySymbol = userProfileCurrency,
                            fortressThreshold = fortressThreshold,
                            totalPendingCommitments = totalPendingCommitments,
                            onClick = { onSelectAccountEdit(acc) },
                            onLongClick = onLongPressCard
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(accounts.size + 1) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(5.dp)
                        .width(if (isSelected) 20.dp else 5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isSelected) PurplePrimary else BorderLight)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val currentAccount = if (pagerState.currentPage > 0) accounts.getOrNull(pagerState.currentPage - 1) else null

        if (pagerState.currentPage == 0 || currentAccount == null) {
            GroupedTransactionLedger(
                transactions = allTransactions,
                currencySymbol = userProfileCurrency,
                title = "Aggregated Cashflow Ledger",
                emptyMessage = "No transactions logged across vaults"
            )
        } else {
            val bankTransactions = remember(allTransactions, currentAccount.accountName) {
                allTransactions.filter { it.accountName.equals(currentAccount.accountName, ignoreCase = true) }
            }
            GroupedTransactionLedger(
                transactions = bankTransactions,
                currencySymbol = userProfileCurrency,
                title = "${currentAccount.accountName} Ledger",
                emptyMessage = "No transactions for ${currentAccount.accountName}"
            )
        }
    }
}

// TAB 2: ROUTING & ALLOCATOR
@Composable
private fun RoutingAllocatorTabContent(
    operatingBank: AccountBalanceResult?,
    commitmentBank: AccountBalanceResult?,
    fortressBank: AccountBalanceResult?,
    currencySymbol: String,
    pendingCommitments: Double,
    allTransactions: List<TransactionEntity>,
    commitmentsShortfall: CommitmentsShortfallStatus,
    onTriggerSweep: () -> Unit
) {
    val operatingBalance = max(0.0, operatingBank?.currentBalance ?: 0.0)
    val commitmentBalance = max(0.0, commitmentBank?.currentBalance ?: 0.0)
    val fortressBalance = max(0.0, fortressBank?.currentBalance ?: 0.0)

    val rawCommitmentBalance = commitmentBank?.currentBalance ?: 0.0
    val isShortfall = commitmentsShortfall.isShortfall
    val shortfallDelta = commitmentsShortfall.shortfallAmount

    val interVaultTransfers = remember(allTransactions) {
        allTransactions.filter { it.type == TransactionType.TRANSFER }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 110.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Salary Routing Funnel",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF1E202E)
        )
        Text(
            text = "Live distribution streams across your 3-Bank strategy",
            fontSize = 11.5.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(16.dp))

        SalaryRoutingFunnelCanvas(
            operatingBalance = operatingBalance,
            commitmentBalance = commitmentBalance,
            fortressBalance = fortressBalance
        )

        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = CanvasLight
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isShortfall) CoralAccent else TealPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Bank 2 AutoPay Readiness", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                    }

                    Text(
                        text = if (isShortfall) "Shortfall" else "Fully Funded",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isShortfall) CoralAccent else TealPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Active AutoPay Commitments", fontSize = 10.5.sp, color = TextMuted)
                        Text("$currencySymbol${String.format(Locale.US, "%,.0f", pendingCommitments)}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF1E202E))
                        if (commitmentBank != null && commitmentBank.minBalance > 0) {
                            Text("Floor: $currencySymbol${String.format(Locale.US, "%,.0f", commitmentBank.minBalance)}", fontSize = 9.5.sp, color = PurplePrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(commitmentBank?.accountName ?: "Vault 2", fontSize = 10.5.sp, color = TextMuted)
                        Text("$currencySymbol${String.format(Locale.US, "%,.0f", rawCommitmentBalance)}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = if (isShortfall) CoralAccent else TealPrimary)
                        val surplus = rawCommitmentBalance - pendingCommitments - (commitmentBank?.minBalance ?: 0.0)
                        Text(
                            text = if (surplus >= 0) "Surplus: +$currencySymbol${String.format(Locale.US, "%,.0f", surplus)}" else "Deficit: -$currencySymbol${String.format(Locale.US, "%,.0f", abs(surplus))}",
                            fontSize = 9.5.sp,
                            color = if (surplus >= 0) TealPrimary else CoralAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isShortfall) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onTriggerSweep,
                        colors = ButtonDefaults.buttonColors(containerColor = CoralAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Text("Sweep $currencySymbol${String.format(Locale.US, "%,.0f", shortfallDelta)} to Prevent AutoPay Bounce", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Inter-Vault Sweep Ledger", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1E202E))
            TextButton(onClick = onTriggerSweep, contentPadding = PaddingValues(0.dp)) {
                Text("+ New Sweep", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PurplePrimary)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (interVaultTransfers.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("No inter-vault sweeps recorded yet", fontSize = 12.sp, color = TextMuted)
            }
        } else {
            interVaultTransfers.forEach { tx ->
                SquircleTransactionRow(tx = tx, currencySymbol = currencySymbol)
                HorizontalDivider(color = BorderLight.copy(alpha = 0.35f), thickness = 0.7.dp)
            }
        }
    }
}

// TAB 3: FORTRESS SWEEP CONTENT
@Composable
fun FortressSweepTabContent(
    fortressBank: AccountBalanceResult?,
    currencySymbol: String,
    fortressThreshold: Double,
    monthlyBurnRate: Double,
    onOpenCalibration: () -> Unit
) {
    val totalBalance = fortressBank?.currentBalance ?: 0.0
    val liquidSavings = min(totalBalance, fortressThreshold)
    val autoSweptFDs = max(0.0, totalBalance - fortressThreshold)
    val runwayMonths = if (monthlyBurnRate > 0) (autoSweptFDs / monthlyBurnRate) else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(bottom = 110.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Fortress Auto-Sweep Facility",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF1E202E)
        )
        Text(
            text = "Balance exceeding $currencySymbol${String.format(Locale.US, "%,.0f", fortressThreshold)} auto-sweeps into Emergency FDs",
            fontSize = 11.5.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(20.dp))

        FortressDirectCanvasVisualizer(
            runwayMonths = runwayMonths,
            liquidSavings = liquidSavings,
            autoSweptFDs = autoSweptFDs,
            fortressThreshold = fortressThreshold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = CanvasLight
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Liquid Savings", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", liquidSavings)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Color(0xFF1E202E)
                    )
                    Text("Cap: $currencySymbol${String.format(Locale.US, "%,.0f", fortressThreshold)}", fontSize = 10.sp, color = TealPrimary, fontWeight = FontWeight.Bold)
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                color = CanvasLight
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Auto-Swept FDs", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.0f", autoSweptFDs)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = PurplePrimary
                    )
                    Text("Emergency Vault", fontSize = 10.sp, color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(HeroCardGradient)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Emergency Runway", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${String.format(Locale.US, "%.1f", runwayMonths)} Months",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TealPrimary
                    )
                    Text("Secured at current monthly burn", fontSize = 10.5.sp, color = TextMuted)
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TealPrimary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (runwayMonths >= 6.0) "Shield Solid" else "Accumulating",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onOpenCalibration,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF181A2A))
        ) {
            Icon(Icons.Default.Tune, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Calibrate Fortress & Auto-Sweep Cap", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

// DIRECT CANVAS VISUALIZER
@Composable
private fun FortressDirectCanvasVisualizer(
    runwayMonths: Double,
    liquidSavings: Double,
    autoSweptFDs: Double,
    fortressThreshold: Double
) {
    val displayedKpi = String.format(Locale.US, "%.1f", runwayMonths)
    val activeMilestoneIndex = runwayMonths.toInt().coerceIn(0, 6)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(105.dp)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = displayedKpi,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1E202E),
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "RUNWAY METRIC\nACTIVE MONTHS",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    lineHeight = 11.sp
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(95.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    val liquidRatio = (liquidSavings / fortressThreshold.coerceAtLeast(1.0)).toFloat().coerceIn(0.0f, 1.0f)
                    val fdRatio = if (fortressThreshold > 0) (autoSweptFDs / (fortressThreshold * 2.5)).toFloat().coerceIn(0.0f, 1.0f) else 0f

                    val p1PeakY = h * (0.95f - 0.70f * liquidRatio)
                    val p1 = Path().apply {
                        moveTo(0f, h)
                        lineTo(w * 0.18f, h)
                        cubicTo(
                            w * 0.32f, h * (0.98f - 0.05f * liquidRatio),
                            w * 0.42f, p1PeakY + 3.dp.toPx(),
                            w * 0.54f, p1PeakY
                        )
                        cubicTo(
                            w * 0.66f, p1PeakY,
                            w * 0.78f, h * (0.95f - 0.10f * liquidRatio),
                            w * 0.95f, h
                        )
                        lineTo(w, h)
                        close()
                    }

                    drawPath(
                        path = p1,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MagentaAccent.copy(alpha = 0.55f * liquidRatio.coerceAtLeast(0.15f)),
                                MagentaAccent.copy(alpha = 0.18f * liquidRatio.coerceAtLeast(0.08f)),
                                Color.Transparent
                            ),
                            startY = p1PeakY,
                            endY = h
                        )
                    )

                    val p2PeakY = h * (0.95f - 0.85f * fdRatio)
                    val p2 = Path().apply {
                        moveTo(w * 0.05f, h)
                        cubicTo(
                            w * 0.10f, h * 0.92f,
                            w * 0.15f, h * (0.95f - 0.57f * fdRatio),
                            w * 0.24f, h * (0.95f - 0.60f * fdRatio)
                        )
                        cubicTo(
                            w * 0.34f, h * (0.95f - 0.60f * fdRatio),
                            w * 0.40f, h * (0.95f - 0.20f * fdRatio),
                            w * 0.56f, h * (0.95f - 0.30f * fdRatio)
                        )
                        cubicTo(
                            w * 0.72f, h * (0.95f - 0.40f * fdRatio),
                            w * 0.82f, p2PeakY,
                            w * 0.90f, p2PeakY + 2.dp.toPx()
                        )
                        cubicTo(
                            w * 0.96f, p2PeakY + 5.dp.toPx(),
                            w * 0.98f, h * 0.88f,
                            w, h
                        )
                        close()
                    }

                    drawPath(
                        path = p2,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BlueAccent.copy(alpha = 0.60f * fdRatio.coerceAtLeast(0.12f)),
                                CyanPrimary.copy(alpha = 0.25f * fdRatio.coerceAtLeast(0.05f)),
                                Color.Transparent
                            ),
                            startY = p2PeakY,
                            endY = h
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 105.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("01", "02", "03", "04", "05", "06").forEachIndexed { index, step ->
                val isReached = (index + 1) <= activeMilestoneIndex
                Text(
                    text = step,
                    fontSize = 10.sp,
                    fontWeight = if (isReached) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isReached) BlueAccent else TextMuted.copy(alpha = 0.60f),
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// 3D BANK CARD COMPONENT
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EnhancedBankCardItem(
    account: AccountBalanceResult,
    role: BankRole,
    currencySymbol: String,
    fortressThreshold: Double,
    totalPendingCommitments: Double,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val palette = remember(account.accountName, account.accountType) {
        getVaultCardPalette(account.accountName, account.accountType)
    }

    val isFortress = role == BankRole.FORTRESS
    val liquidPart = min(account.currentBalance, fortressThreshold)
    val fdPart = max(0.0, account.currentBalance - fortressThreshold)
    val spendableSurplus = (account.currentBalance - account.minBalance).coerceAtLeast(0.0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = palette.baseColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            OrganicCardCanvas(palette = palette)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.22f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = palette.bankCode,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = account.accountName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = role.badgeColor.copy(alpha = 0.25f)
                                ) {
                                    Text(
                                        text = role.title,
                                        color = role.badgeColor,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                if (account.minBalance > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color.White.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "MAB: $currencySymbol${String.format(Locale.US, "%,.0f", account.minBalance)}",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Icon(Icons.Default.Contactless, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                }

                if (isFortress) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Liquid Savings", color = Color.White.copy(alpha = 0.65f), fontSize = 10.5.sp)
                            Text("$currencySymbol${String.format(Locale.US, "%,.0f", liquidPart)}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Auto-Swept FDs", color = TealPrimary.copy(alpha = 0.85f), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                            Text("$currencySymbol${String.format(Locale.US, "%,.0f", fdPart)}", color = TealPrimary, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text("Available Balance", color = Color.White.copy(alpha = 0.70f), fontSize = 11.5.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.2f", account.currentBalance)}",
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        if (account.minBalance > 0) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Liquid Surplus", color = TealPrimary.copy(alpha = 0.85f), fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$currencySymbol${String.format(Locale.US, "%,.0f", spendableSurplus)}",
                                    color = TealPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// MASTER RESERVES CARD
@Composable
private fun MasterReservesCard(
    currencySymbol: String,
    totalBalance: Double,
    accountCount: Int
) {
    val palette = CardPalette(
        baseColor = Color(0xFF1B0B38),
        discColor1 = Color(0xFF5B21B6),
        discColor2 = Color(0xFF7C3AED),
        discColor3 = Color(0xFFA78BFA),
        bankCode = "TOTAL"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        color = palette.baseColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            OrganicCardCanvas(palette = palette)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.20f),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Master Capital Pool", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("$accountCount Active Liquid Vaults", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
                        }
                    }

                    Icon(Icons.Default.Contactless, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                }

                Column {
                    Text("Aggregate Net Liquidity", color = Color.White.copy(alpha = 0.70f), fontSize = 11.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", totalBalance)}",
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun OrganicCardCanvas(palette: CardPalette) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = palette.discColor1.copy(alpha = 0.45f),
            radius = w * 0.45f,
            center = Offset(w * 0.80f, h * 0.15f)
        )

        drawCircle(
            color = palette.discColor2.copy(alpha = 0.70f),
            radius = w * 0.38f,
            center = Offset(w * 0.30f, h * 0.30f)
        )

        drawCircle(
            color = palette.discColor3.copy(alpha = 0.55f),
            radius = w * 0.32f,
            center = Offset(w * 0.95f, h * 0.80f)
        )
    }
}

@Composable
private fun SalaryRoutingFunnelCanvas(
    operatingBalance: Double,
    commitmentBalance: Double,
    fortressBalance: Double
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(110.dp)
    ) {
        val w = size.width
        val h = size.height

        val safeOp = operatingBalance.coerceAtLeast(0.0)
        val safeComm = commitmentBalance.coerceAtLeast(0.0)
        val safeFort = fortressBalance.coerceAtLeast(0.0)

        val total = (safeOp + safeComm + safeFort).coerceAtLeast(1.0)
        val r1 = (safeOp / total).toFloat().coerceIn(0.15f, 0.70f)
        val r2 = (safeComm / total).toFloat().coerceIn(0.15f, 0.70f)
        val r3 = (safeFort / total).toFloat().coerceIn(0.15f, 0.70f)
        val sumR = r1 + r2 + r3

        val n1 = (r1 / sumR) * h
        val n2 = (r2 / sumR) * h

        val band1Bottom = n1.coerceIn(h * 0.20f, h * 0.45f)
        val band2Bottom = (band1Bottom + n2).coerceIn(band1Bottom + h * 0.20f, h * 0.85f)

        val p1 = Path().apply {
            moveTo(0f, 0f)
            cubicTo(w * 0.35f, 0f, w * 0.65f, 0f, w, 0f)
            lineTo(w, band1Bottom)
            cubicTo(w * 0.65f, band1Bottom, w * 0.35f, h * 0.35f, 0f, h * 0.35f)
            close()
        }
        drawPath(p1, color = CyanPrimary.copy(alpha = 0.85f))

        val p2 = Path().apply {
            moveTo(0f, h * 0.35f)
            cubicTo(w * 0.35f, h * 0.35f, w * 0.65f, band1Bottom, w, band1Bottom)
            lineTo(w, band2Bottom)
            cubicTo(w * 0.65f, band2Bottom, w * 0.35f, h * 0.65f, 0f, h * 0.65f)
            close()
        }
        drawPath(p2, color = PurplePrimary.copy(alpha = 0.85f))

        val p3 = Path().apply {
            moveTo(0f, h * 0.65f)
            cubicTo(w * 0.35f, h * 0.65f, w * 0.65f, band2Bottom, w, band2Bottom)
            lineTo(w, h)
            cubicTo(w * 0.65f, h, w * 0.35f, h, 0f, h)
            close()
        }
        drawPath(p3, color = TealPrimary.copy(alpha = 0.85f))
    }
}

// CREATE LIQUID VAULT BOTTOM SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateVaultModalSheet(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onCreateVault: (String, Double, String, Double) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var newName by remember { mutableStateOf("") }
    var newBalanceStr by remember { mutableStateOf("") }
    var minBalanceStr by remember { mutableStateOf("0") }
    var selectedRole by remember { mutableStateOf(BankRole.GENERAL) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Create Liquid Vault", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Text("Add a new banking node to your cashflow strategy", fontSize = 12.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(CanvasLight)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Vault Name (e.g. Salary Account, HDFC)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = newBalanceStr,
                onValueChange = { newBalanceStr = it },
                label = { Text("Initial Balance ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = minBalanceStr,
                onValueChange = { minBalanceStr = it },
                label = { Text("Minimum Balance Floor / MAB ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Strategy Role / Purpose", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BankRole.entries) { role ->
                    val isSel = selectedRole == role
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSel) role.badgeColor.copy(alpha = 0.16f) else CanvasLight)
                            .border(
                                width = if (isSel) 1.5.dp else 0.dp,
                                color = if (isSel) role.badgeColor else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedRole = role
                                if (role == BankRole.COMMITMENTS && minBalanceStr == "0") {
                                    minBalanceStr = "10000"
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = role.title,
                            fontSize = 12.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) role.badgeColor else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val initialBal = newBalanceStr.toDoubleOrNull() ?: 0.0
                    val minBal = minBalanceStr.toDoubleOrNull() ?: 0.0
                    if (newName.isNotBlank()) {
                        onCreateVault(newName.trim(), initialBal, selectedRole.roleKey, minBal)
                    }
                },
                enabled = newName.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Vault", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

// BALANCE & ROLE EDIT MODAL SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BalanceEditModalSheet(
    account: AccountBalanceResult,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onSave: (String, String, Double, Double, Boolean) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var editName by remember { mutableStateOf(account.accountName) }
    var selectedRole by remember { mutableStateOf(getAccountRole(account.accountName, account.accountType)) }
    var amountString by remember { mutableStateOf(account.currentBalance.toInt().toString()) }
    var minBalanceString by remember { mutableStateOf(account.minBalance.toInt().toString()) }
    var isArchivedState by remember { mutableStateOf(account.isArchived) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Edit Vault Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)

                IconButton(
                    onClick = onOpenAnalytics,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(CanvasLight)
                ) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Analytics", tint = TextDark)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = editName,
                onValueChange = { editName = it },
                label = { Text("Bank / Vault Name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = minBalanceString,
                onValueChange = { minBalanceString = it },
                label = { Text("Minimum Balance Floor / MAB ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text("Strategy Role / Type", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(BankRole.entries) { role ->
                    val isSel = selectedRole == role
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) role.badgeColor.copy(alpha = 0.15f) else CanvasLight)
                            .border(
                                width = if (isSel) 1.5.dp else 0.dp,
                                color = if (isSel) role.badgeColor else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedRole = role
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = role.title,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSel) role.badgeColor else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CanvasLight)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$currencySymbol$amountString",
                    color = TextDark,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(".", "0", "⌫")
            )

            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    when (key) {
                                        "⌫" -> if (amountString.isNotEmpty()) amountString = amountString.dropLast(1)
                                        "." -> if (!amountString.contains(".")) amountString += "."
                                        else -> {
                                            if (amountString == "0") amountString = key
                                            else if (amountString.length < 9) amountString += key
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = key, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val finalBal = amountString.toDoubleOrNull() ?: account.currentBalance
                    val finalMinBal = minBalanceString.toDoubleOrNull() ?: account.minBalance
                    onSave(editName.trim(), selectedRole.roleKey, finalBal, finalMinBal, isArchivedState)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Vault Changes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// BANK ANALYTICS & ARCHIVE MODAL SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankAnalyticsModalSheet(
    account: AccountBalanceResult,
    currencySymbol: String,
    transactions: List<TransactionEntity>,
    onDismiss: () -> Unit,
    onArchiveAccount: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(account.accountName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Text("Role: ${account.accountType} • MAB: $currencySymbol${String.format(Locale.US, "%,.0f", account.minBalance)}", fontSize = 12.sp, color = TextMuted)
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("Current Balance", fontSize = 11.sp, color = TextMuted)
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%,.2f", account.currentBalance)}",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFF1E202E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CanvasLight
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Vault Ledger Metrics", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Total Transactions Logged: ${transactions.size}", fontSize = 12.sp, color = TextMuted)
                    Text("Starting Balance: $currencySymbol${String.format(Locale.US, "%,.2f", account.startingBalance)}", fontSize = 12.sp, color = TextMuted)
                    Text("Minimum Required Floor (MAB): $currencySymbol${String.format(Locale.US, "%,.0f", account.minBalance)}", fontSize = 12.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onArchiveAccount()
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(23.dp),
                    border = BorderStroke(1.dp, BorderLight)
                ) {
                    Icon(
                        imageVector = if (account.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = "Archive",
                        tint = TextDark,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (account.isArchived) "Restore Vault" else "Archive Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = TextDark
                    )
                }

                OutlinedButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteConfirmDialog = true
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftRed),
                    shape = RoundedCornerShape(23.dp),
                    border = BorderStroke(1.dp, SoftRed.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SoftRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete Vault", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = SoftRed)
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = CardWhite,
            title = { Text("Delete ${account.accountName}?", fontWeight = FontWeight.Bold, color = SoftRed) },
            text = { Text("If this account has transactions, you should archive it instead to keep your ledger history intact.", fontSize = 13.sp, color = TextDark) },
            confirmButton = {
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) { Text("Cancel", color = TextMuted) }
            }
        )
    }
}

// REORDER VAULTS MODAL SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReorderVaultsModalSheet(
    accounts: List<AccountBalanceResult>,
    onDismiss: () -> Unit,
    onSaveOrder: (List<AccountEntity>) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var workingList by remember(accounts) {
        mutableStateOf(
            accounts.map {
                AccountEntity(
                    accountName = it.accountName,
                    startingBalance = it.startingBalance,
                    accountType = it.accountType,
                    minBalance = it.minBalance,
                    isArchived = it.isArchived,
                    sortOrder = it.sortOrder
                )
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Rearrange Card Sequence", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Text("Swipe order in your carousel", fontSize = 12.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(CanvasLight)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(workingList, key = { _, item -> item.accountName }) { index, item ->
                    val role = getAccountRole(item.accountName, item.accountType)
                    val palette = getVaultCardPalette(item.accountName, item.accountType)

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CanvasLight
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    shape = CircleShape,
                                    color = palette.baseColor,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = palette.bankCode,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = item.accountName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextDark
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = role.badgeColor.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = role.title,
                                            color = role.badgeColor,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        if (index > 0) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val mutable = workingList.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index - 1]
                                            mutable[index - 1] = temp
                                            workingList = mutable
                                        }
                                    },
                                    enabled = index > 0,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowUpward,
                                        contentDescription = "Move Up",
                                        tint = if (index > 0) TextDark else BorderLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        if (index < workingList.size - 1) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            val mutable = workingList.toMutableList()
                                            val temp = mutable[index]
                                            mutable[index] = mutable[index + 1]
                                            mutable[index + 1] = temp
                                            workingList = mutable
                                        }
                                    },
                                    enabled = index < workingList.size - 1,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ArrowDownward,
                                        contentDescription = "Move Down",
                                        tint = if (index < workingList.size - 1) TextDark else BorderLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSaveOrder(workingList)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Carousel Sequence", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// FORTRESS CALIBRATION MODAL SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FortressCalibrationModalSheet(
    currentThreshold: Double,
    currencySymbol: String,
    fortressBank: AccountBalanceResult?,
    onDismiss: () -> Unit,
    onSaveSettings: (Double, Double?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var thresholdStr by remember { mutableStateOf(currentThreshold.toInt().toString()) }
    var balanceStr by remember { mutableStateOf(fortressBank?.currentBalance?.toInt()?.toString() ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = CardWhite,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Calibrate Fortress Strategy", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Text("Auto-sweep threshold & buffer balance", fontSize = 12.sp, color = TextMuted)
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(CanvasLight)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextDark, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            OutlinedTextField(
                value = thresholdStr,
                onValueChange = { thresholdStr = it },
                label = { Text("Auto-Sweep Threshold ($currencySymbol)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            if (fortressBank != null) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = balanceStr,
                    onValueChange = { balanceStr = it },
                    label = { Text("${fortressBank.accountName} Total Liquid Balance ($currencySymbol)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val thresholdVal = thresholdStr.toDoubleOrNull() ?: currentThreshold
                    val balanceVal = balanceStr.toDoubleOrNull()
                    onSaveSettings(thresholdVal, balanceVal)
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Save", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Fortress Configuration", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// GROUPED DATE-STICKY TRANSACTION LEDGER
@Composable
private fun GroupedTransactionLedger(
    transactions: List<TransactionEntity>,
    currencySymbol: String,
    title: String,
    emptyMessage: String
) {
    val grouped = remember(transactions) {
        transactions.groupBy { tx ->
            val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
            cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        }.toList().sortedByDescending { it.first }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
    ) {
        item {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
            Spacer(modifier = Modifier.height(10.dp))
        }

        if (transactions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(36.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emptyMessage, fontSize = 12.sp, color = TextMuted)
                }
            }
        } else {
            grouped.forEach { (_, dayTxs) ->
                val dateLabel = formatDayHeader(dayTxs.first().date)
                val netDayDrift = dayTxs.sumOf {
                    if (it.type == TransactionType.INCOME) it.amount else -it.amount
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = dateLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = TextDark
                        )

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (netDayDrift >= 0) TealPrimary.copy(alpha = 0.12f) else CoralAccent.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = "${if (netDayDrift >= 0) "+" else ""}$currencySymbol${String.format(Locale.US, "%,.0f", netDayDrift)}",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (netDayDrift >= 0) TealPrimary else CoralAccent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                items(dayTxs, key = { it.id }) { tx ->
                    SquircleTransactionRow(tx = tx, currencySymbol = currencySymbol)
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.35f), thickness = 0.7.dp)
                }
            }
        }
    }
}

// SQUIRCLE TRANSACTION ROW
@Composable
private fun SquircleTransactionRow(
    tx: TransactionEntity,
    currencySymbol: String
) {
    val (icon, badgeBg, iconColor) = getTransactionVisualTheme(tx)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(badgeBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tx.title.ifBlank { tx.category },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.US).format(Date(tx.date)),
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )

                    if (tx.linkedFixedBillId != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(shape = RoundedCornerShape(4.dp), color = PurplePrimary.copy(alpha = 0.12f)) {
                            Text("AutoPay", color = PurplePrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    } else if (tx.type == TransactionType.TRANSFER) {
                        Spacer(modifier = Modifier.width(6.dp))
                        val badgeLabel = when (tx.transferSubtype) {
                            TransferSubtype.BILL_FUNDING -> "AutoPay Sweep"
                            TransferSubtype.WEALTH_ALLOCATION -> "Wealth SIP"
                            TransferSubtype.CASH_WITHDRAWAL -> "Cash Out"
                            else -> "Transfer"
                        }
                        Surface(shape = RoundedCornerShape(4.dp), color = TealPrimary.copy(alpha = 0.12f)) {
                            Text(badgeLabel, color = TealPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp))
                        }
                    }
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${if (tx.type == TransactionType.INCOME) "+" else if (tx.type == TransactionType.TRANSFER) "" else "-"}$currencySymbol${String.format(Locale.US, "%,.2f", tx.amount)}",
                fontWeight = FontWeight.Black,
                fontSize = 14.5.sp,
                color = if (tx.type == TransactionType.INCOME) TealPrimary else if (tx.type == TransactionType.TRANSFER) PurplePrimary else Color(0xFF1E202E)
            )
            Text(
                text = tx.accountName,
                fontSize = 10.5.sp,
                color = TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun formatDayHeader(dateMillis: Long): String {
    val nowCal = Calendar.getInstance()
    val txCal = Calendar.getInstance().apply { timeInMillis = dateMillis }
    return when {
        nowCal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR) -> "Today"
        nowCal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                nowCal.get(Calendar.DAY_OF_YEAR) - txCal.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"
        else -> SimpleDateFormat("dd MMMM yyyy", Locale.US).format(Date(dateMillis))
    }
}

private data class TxVisualTheme(val icon: ImageVector, val background: Color, val tint: Color)

private fun getTransactionVisualTheme(tx: TransactionEntity): TxVisualTheme {
    return when (tx.type) {
        TransactionType.INCOME -> TxVisualTheme(Icons.Default.ArrowDownward, TealPrimary.copy(alpha = 0.14f), TealPrimary)
        TransactionType.TRANSFER -> TxVisualTheme(Icons.Default.SyncAlt, PurplePrimary.copy(alpha = 0.14f), PurplePrimary)
        TransactionType.ASSET -> TxVisualTheme(Icons.AutoMirrored.Filled.TrendingUp, CyanPrimary.copy(alpha = 0.14f), CyanPrimary)
        TransactionType.EXPENSE -> {
            val cat = tx.category.lowercase()
            when {
                cat.contains("food") || cat.contains("dining") || cat.contains("restaurant") ->
                    TxVisualTheme(Icons.Default.Restaurant, Color(0xFFFF7675).copy(alpha = 0.14f), Color(0xFFFF7675))
                cat.contains("bill") || cat.contains("utility") || cat.contains("rent") ->
                    TxVisualTheme(Icons.Default.Receipt, PurplePrimary.copy(alpha = 0.14f), PurplePrimary)
                cat.contains("shopping") || cat.contains("cloth") || cat.contains("store") ->
                    TxVisualTheme(Icons.Default.ShoppingBag, Color(0xFFFD79A8).copy(alpha = 0.14f), Color(0xFFFD79A8))
                cat.contains("fuel") || cat.contains("petrol") || cat.contains("travel") || cat.contains("cab") ->
                    TxVisualTheme(Icons.Default.DirectionsCar, Color(0xFFFDCB6E).copy(alpha = 0.18f), Color(0xFFE67E22))
                cat.contains("medical") || cat.contains("health") || cat.contains("pharmacy") ->
                    TxVisualTheme(Icons.Default.LocalHospital, Color(0xFFE17055).copy(alpha = 0.14f), Color(0xFFE17055))
                cat.contains("sub") || cat.contains("stream") || cat.contains("net") ->
                    TxVisualTheme(Icons.Default.Movie, Color(0xFFA29BFE).copy(alpha = 0.16f), Color(0xFF6C5CE7))
                else ->
                    TxVisualTheme(Icons.Default.Payments, CoralAccent.copy(alpha = 0.14f), CoralAccent)
            }
        }
    }
}

private fun getAccountRole(accountName: String, accountType: String): BankRole {
    val typeClean = accountType.lowercase()
    val nameClean = accountName.lowercase()
    return when {
        typeClean.contains("operating") || typeClean.contains("salary") -> BankRole.OPERATING
        typeClean.contains("commitment") || typeClean.contains("autopay") -> BankRole.COMMITMENTS
        typeClean.contains("fortress") || typeClean.contains("emergency") || typeClean.contains("sweep") -> BankRole.FORTRESS
        typeClean.contains("general") -> BankRole.GENERAL
        nameClean.contains("salary") || nameClean.contains("bom") || nameClean.contains("primary") -> BankRole.OPERATING
        nameClean.contains("autopay") || nameClean.contains("hdfc") || nameClean.contains("sip") || nameClean.contains("bill") -> BankRole.COMMITMENTS
        nameClean.contains("fortress") || nameClean.contains("emergency") || nameClean.contains("indusind") || nameClean.contains("fd") -> BankRole.FORTRESS
        else -> BankRole.GENERAL
    }
}

private fun getVaultCardPalette(name: String, type: String): CardPalette {
    val clean = name.lowercase()
    return when {
        clean.contains("bom") || clean.contains("maharashtra") || clean.contains("salary") -> CardPalette(
            baseColor = Color(0xFF1B0B38),
            discColor1 = Color(0xFF4C1D95),
            discColor2 = Color(0xFF8B5CF6),
            discColor3 = Color(0xFFD946EF),
            bankCode = "BOM"
        )
        clean.contains("hdfc") || clean.contains("autopay") -> CardPalette(
            baseColor = Color(0xFF0A1E3F),
            discColor1 = Color(0xFF1E3A8A),
            discColor2 = Color(0xFF2563EB),
            discColor3 = Color(0xFFDC2626),
            bankCode = "HDFC"
        )
        clean.contains("fortress") || clean.contains("emergency") || clean.contains("indusind") -> CardPalette(
            baseColor = Color(0xFF062B28),
            discColor1 = Color(0xFF0F766E),
            discColor2 = Color(0xFF14B8A6),
            discColor3 = Color(0xFF2DD4BF),
            bankCode = "FORT"
        )
        clean.contains("sbi") -> CardPalette(
            baseColor = Color(0xFF082F49),
            discColor1 = Color(0xFF0369A1),
            discColor2 = Color(0xFF0EA5E9),
            discColor3 = Color(0xFF38BDF8),
            bankCode = "SBI"
        )
        clean.contains("icici") -> CardPalette(
            baseColor = Color(0xFF450A0A),
            discColor1 = Color(0xFF991B1B),
            discColor2 = Color(0xFFEA580C),
            discColor3 = Color(0xFFF97316),
            bankCode = "ICICI"
        )
        clean.contains("cash") || type.equals("Cash", ignoreCase = true) -> CardPalette(
            baseColor = Color(0xFF181A2A),
            discColor1 = Color(0xFF334155),
            discColor2 = Color(0xFF64748B),
            discColor3 = Color(0xFF94A3B8),
            bankCode = "CASH"
        )
        else -> CardPalette(
            baseColor = Color(0xFF131127),
            discColor1 = Color(0xFF312E81),
            discColor2 = Color(0xFF6366F1),
            discColor3 = Color(0xFFA5B4FC),
            bankCode = name.take(3).uppercase()
        )
    }
}
