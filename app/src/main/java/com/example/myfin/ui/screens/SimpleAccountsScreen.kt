package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.AccountBalanceResult
import com.example.myfin.data.AccountEntity
import com.example.myfin.data.TransactionEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.data.TransferSubtype
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.AccountTransferDialog
import com.example.myfin.ui.theme.*
import java.util.Locale

private val SimplePurple = Color(0xFF6C5CE7)
private val SimpleTeal = Color(0xFF10B981)

private enum class AccountsViewTab(val title: String) {
    ACTIVE("Active Vaults"),
    ARCHIVED("Archived Vaults")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SimpleAccountsScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToTaxonomy: () -> Unit = {},
    onNavigateToVaultAnalytics: () -> Unit = {},
    onNavigateToVaultSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var currentTab by remember { mutableStateOf(AccountsViewTab.ACTIVE) }

    val activeAccounts = remember(uiState.accounts) { uiState.accounts.filter { !it.isArchived } }
    val archivedAccounts = remember(uiState.accounts) { uiState.accounts.filter { it.isArchived } }
    val displayedAccounts = if (currentTab == AccountsViewTab.ACTIVE) activeAccounts else archivedAccounts

    val totalActiveBalance = remember(activeAccounts) { activeAccounts.sumOf { it.currentBalance } }
    val accountNames = remember(activeAccounts) { activeAccounts.map { it.accountName } }

    val allTransactions = remember(uiState.groupedTransactions) {
        uiState.groupedTransactions.values.flatten()
    }

    var selectedAccountForEdit by remember { mutableStateOf<AccountBalanceResult?>(null) }
    var showAccountAnalyticsSheet by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CardWhite)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
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
                        text = "Unified Accounts Ledger",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E202E)
                    )
                    Text(
                        text = "All Bank Accounts, Wallets & Reserves",
                        fontSize = 10.5.sp,
                        color = TextMuted
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onNavigateToVaultSettings()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CanvasLight)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = TextDark, modifier = Modifier.size(19.dp))
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
                        Icon(Icons.Default.Add, contentDescription = "Add Account", tint = AccentPurple, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // SCROLLABLE BODY
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 110.dp)
            ) {
                // MASTER CAPITAL HERO CARD
                item {
                    MasterLiquidityHeroCard(
                        currencySymbol = userProfile.currencySymbol,
                        totalBalance = totalActiveBalance,
                        activeAccountCount = activeAccounts.size,
                        onTransferClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showTransferDialog = true
                        },
                        onAddAccountClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showAddAccountSheet = true
                        }
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                }

                // TAB PILL SELECTOR (ACTIVE vs ARCHIVED)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CanvasLight)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AccountsViewTab.entries.forEach { tab ->
                            val isSel = currentTab == tab
                            val count = if (tab == AccountsViewTab.ACTIVE) activeAccounts.size else archivedAccounts.size
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) CardWhite else Color.Transparent)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        currentTab = tab
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tab.title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSel) TextDark else TextMuted
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSel) AccentPurple.copy(alpha = 0.14f) else BorderLight
                                    ) {
                                        Text(
                                            text = count.toString(),
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSel) AccentPurple else TextMuted,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ACCOUNTS LIST
                if (displayedAccounts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            shape = RoundedCornerShape(18.dp),
                            color = CanvasLight
                        ) {
                            Box(
                                modifier = Modifier.padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (currentTab == AccountsViewTab.ACTIVE) "No active bank accounts found." else "No archived accounts.",
                                    fontSize = 12.5.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                } else {
                    items(displayedAccounts, key = { it.accountName }) { acc ->
                        SimpleAccountCardItem(
                            account = acc,
                            currencySymbol = userProfile.currencySymbol,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                selectedAccountForEdit = acc
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }

        // FLOATING BOTTOM DOCK
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
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurple.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Simple Ledger View", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                Text("${activeAccounts.size} active accounts", fontSize = 10.sp, color = TextMuted)
                            }
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showTransferDialog = true
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Transfer", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
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

    // CREATE ACCOUNT MODAL SHEET
    if (showAddAccountSheet) {
        CreateVaultModalSheet(
            currencySymbol = userProfile.currencySymbol,
            onDismiss = { showAddAccountSheet = false },
            onCreateVault = { name, initialBal, roleKey, minBal ->
                viewModel.addAccount(
                    name = name.trim().uppercase(),
                    startingBalance = initialBal,
                    type = roleKey,
                    minBalance = minBal
                )
                Toast.makeText(context, "Account '${name.trim().uppercase()}' created", Toast.LENGTH_SHORT).show()
                showAddAccountSheet = false
            }
        )
    }

    // EDIT & SETTINGS MODAL SHEET
    selectedAccountForEdit?.let { account ->
        BalanceEditModalSheet(
            account = account,
            currencySymbol = userProfile.currencySymbol,
            onDismiss = { selectedAccountForEdit = null },
            onOpenAnalytics = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                showAccountAnalyticsSheet = true
            },
            onSave = { newName, newRoleKey, newBalance, newMinBalance, isArchivedState ->
                viewModel.updateAccountDetails(
                    oldName = account.accountName,
                    newName = newName.trim().uppercase(),
                    startingBalance = account.startingBalance,
                    accountType = newRoleKey,
                    minBalance = newMinBalance,
                    isArchived = isArchivedState,
                    sortOrder = account.sortOrder
                )
                if (newBalance != account.currentBalance) {
                    viewModel.adjustAccountBalance(newName.trim().uppercase(), newBalance)
                }
                Toast.makeText(context, "Vault updated", Toast.LENGTH_SHORT).show()
                selectedAccountForEdit = null
            }
        )
    }

    // BANK ANALYTICS & ARCHIVE MODAL SHEET
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
                if (targetAccount.isArchived) {
                    viewModel.unarchiveAccount(targetAccount.accountName)
                    Toast.makeText(context, "${targetAccount.accountName} restored to active", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.archiveAccount(targetAccount.accountName)
                    Toast.makeText(context, "${targetAccount.accountName} archived", Toast.LENGTH_SHORT).show()
                }
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

    // INTER-VAULT TRANSFER DIALOG
    if (showTransferDialog) {
        AccountTransferDialog(
            accounts = accountNames,
            onDismiss = { showTransferDialog = false },
            onTransfer = { from, to, amount, note ->
                viewModel.executeInstantTransfer(
                    fromAccount = from,
                    toAccount = to,
                    amount = amount,
                    note = note,
                    subtype = TransferSubtype.REBALANCE
                )
            }
        )
    }
}

@Composable
private fun MasterLiquidityHeroCard(
    currencySymbol: String,
    totalBalance: Double,
    activeAccountCount: Int,
    onTransferClick: () -> Unit,
    onAddAccountClick: () -> Unit
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
            .height(200.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        color = palette.baseColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            OrganicHeroCardCanvas(palette = palette)

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
                            Text("Total Net Liquidity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("$activeAccountCount Active Accounts Ring-Fenced", color = Color.White.copy(alpha = 0.65f), fontSize = 10.5.sp)
                        }
                    }

                    Icon(Icons.Default.Contactless, contentDescription = null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(24.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("Aggregate Spendable Balance", color = Color.White.copy(alpha = 0.70f), fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", totalBalance)}",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onTransferClick() },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.20f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.SyncAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transfer", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAddAccountClick() },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = TextDark, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Add", color = TextDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SimpleAccountCardItem(
    account: AccountBalanceResult,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val palette = remember(account.accountName, account.accountType) {
        getSimpleAccountPalette(account.accountName, account.accountType)
    }
    val spendableSurplus = (account.currentBalance - account.minBalance).coerceAtLeast(0.0)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() },
        color = palette.baseColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            OrganicHeroCardCanvas(palette = palette)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
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
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = palette.bankCode,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.5.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = account.accountName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.White.copy(alpha = 0.20f)
                                ) {
                                    Text(
                                        text = account.accountType,
                                        color = Color.White,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                if (account.minBalance > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = Color.White.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "MAB: $currencySymbol${String.format(Locale.US, "%,.0f", account.minBalance)}",
                                            color = Color.White.copy(alpha = 0.85f),
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text("Current Balance", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                        Text(
                            text = "$currencySymbol${String.format(Locale.US, "%,.2f", account.currentBalance)}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    if (account.minBalance > 0) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Spendable Surplus", color = SimpleTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = "$currencySymbol${String.format(Locale.US, "%,.0f", spendableSurplus)}",
                                color = SimpleTeal,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrganicHeroCardCanvas(palette: CardPalette) {
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
                    Text("Add a new banking node to your portfolio", fontSize = 12.sp, color = TextMuted)
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
                colors = ButtonDefaults.buttonColors(containerColor = SimplePurple)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Vault", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

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
                colors = ButtonDefaults.buttonColors(containerColor = SimplePurple)
            ) {
                Icon(Icons.Default.Check, contentDescription = "Confirm", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Vault Changes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

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

private fun getSimpleAccountPalette(name: String, type: String): CardPalette {
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
