package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.myfin.ui.components.AccountTransferDialog
import com.example.myfin.ui.components.AppBottomDock
import com.example.myfin.ui.components.DockFabAction
import com.example.myfin.ui.components.NavigationTarget
import com.example.myfin.ui.components.rememberAutoScrollVisibilityConnection
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs

data class SimplePendingEditConfirmation(
    val originalAccount: AccountBalanceResult,
    val updatedName: String,
    val updatedType: String,
    val targetBalance: Double,
    val minBalance: Double,
    val isArchived: Boolean = false
)

private fun getAccountTier(accountType: String, accountName: String): VaultTier {
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
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()

    var isDiscreetMode by remember { mutableStateOf(false) }
    var showTransferSheet by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var showReorderSheet by remember { mutableStateOf(false) }

    var editingAccount by remember { mutableStateOf<AccountBalanceResult?>(null) }
    var pendingEditConfirmation by remember { mutableStateOf<SimplePendingEditConfirmation?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    val displayAccounts = remember(uiState.activeAccounts) {
        uiState.activeAccounts
    }
    val archivedAccounts = remember(uiState.archivedAccounts) {
        uiState.archivedAccounts
    }

    val accountNames = remember(displayAccounts) { displayAccounts.map { it.accountName } }
    val totalLiquidBalance = remember(displayAccounts) { displayAccounts.sumOf { it.currentBalance } }

    val monthInflow = uiState.metrics.actualIncome
    val monthOutflow = uiState.metrics.actualExpenses + uiState.metrics.actualAssets

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
            // Top Navigation Bar
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
                        imageVector = Icons.Default.Menu,
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
                        text = "Unified Accounts",
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
                                contentDescription = "Settings",
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
                contentPadding = PaddingValues(top = 8.dp, bottom = 125.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Overview Liquidity Card
                item(key = "overview_liquidity_card") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(22.dp)),
                        shape = RoundedCornerShape(22.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, AccentPurple.copy(alpha = 0.20f))
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
                            Text(
                                text = "Total Net Liquidity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isDiscreetMode) "••••••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", totalLiquidBalance)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark,
                                letterSpacing = (-0.5).sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CanvasLight,
                                    border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(SoftGreen.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.TrendingUp,
                                                contentDescription = null,
                                                tint = SoftGreen,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Month Inflow", fontSize = 10.sp, color = TextMuted)
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", monthInflow)}",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                        }
                                    }
                                }

                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CanvasLight,
                                    border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(SoftRed.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.TrendingDown,
                                                contentDescription = null,
                                                tint = SoftRed,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text("Month Outflow", fontSize = 10.sp, color = TextMuted)
                                            Text(
                                                text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", monthOutflow)}",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Accounts List Header
                item(key = "active_accounts_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Active Accounts (${displayAccounts.size})",
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
                                    Icon(
                                        Icons.Default.SwapVert,
                                        contentDescription = "Reorder Accounts",
                                        tint = AccentPurple,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Tap edit to modify",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                if (displayAccounts.isEmpty()) {
                    item(key = "empty_accounts") {
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
                    items(displayAccounts, key = { it.accountName }) { account ->
                        val tier = getAccountTier(account.accountType, account.accountName)
                        val maskedDigits = remember(account.accountName) {
                            val safeHash = abs(account.accountName.hashCode().toLong())
                            String.format(Locale.US, "%04d", safeHash % 9000 + 1000)
                        }

                        val pendingBillsForAccount = remember(uiState.fixedBills, account.accountName) {
                            uiState.fixedBills.filter {
                                !it.isPaid &&
                                it.type != TransactionType.INCOME &&
                                !(it.type == TransactionType.CORPORATE && it.category.equals("Reimbursements & Claims", ignoreCase = true)) &&
                                it.accountName.equals(account.accountName, ignoreCase = true)
                            }.sumOf { it.amount }
                        }

                        val effectiveBal = account.currentBalance - pendingBillsForAccount
                        val isMabBreached = account.minBalance > 0.0 && effectiveBal < account.minBalance

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(2.dp, RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            color = CardWhite,
                            border = BorderStroke(
                                width = if (isMabBreached) 1.dp else 0.8.dp,
                                color = if (isMabBreached) SoftRed else BorderLight.copy(alpha = 0.7f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                tier.bgTint.copy(alpha = 0.45f),
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
                                                color = SoftRed.copy(alpha = 0.15f),
                                                border = BorderStroke(0.6.dp, SoftRed.copy(alpha = 0.4f))
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

                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = tier.color.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = account.accountType.ifBlank { tier.title },
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = tier.color,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        IconButton(
                                            onClick = { editingAccount = account },
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Edit Account",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

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
                                            text = "MAB: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", account.minBalance)}",
                                            fontSize = 10.sp,
                                            color = if (isMabBreached) SoftRed else TextMuted,
                                            fontWeight = if (isMabBreached) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = if (isDiscreetMode) "••••••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", account.currentBalance)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (account.currentBalance >= 0) TextDark else SoftRed,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (pendingBillsForAccount > 0.0) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AccentPurple.copy(alpha = 0.10f)
                                            ) {
                                                Text(
                                                    text = "AutoPay: -${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", pendingBillsForAccount)}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentPurple,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = if (isDiscreetMode) "Base: ••••" else "Base: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", account.startingBalance)}",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Archived Accounts Section
                if (archivedAccounts.isNotEmpty()) {
                    item(key = "archived_accounts_header") {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Archived Accounts (${archivedAccounts.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    }

                    items(archivedAccounts, key = { it.accountName }) { acc ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = acc.accountName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = TextMuted
                                    )
                                    Text(
                                        text = if (isDiscreetMode) "••••" else "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", acc.currentBalance)}",
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

        // Floating Bottom Navigation Dock
        AppBottomDock(
            currentSelection = NavigationTarget.VAULT_ACCOUNTS,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.MONTHLY_VIEW -> onNavigateToDashboard()
                    NavigationTarget.BUDGET_PLANNER -> onNavigateToPlanner()
                    NavigationTarget.DATA_SET -> onNavigateToTaxonomy()
                    NavigationTarget.REPORTS_ANALYTICS -> onNavigateToVaultAnalytics()
                    NavigationTarget.VAULT_ACCOUNTS -> {}
                    else -> {}
                }
            },
            fabActions = fabActions,
            isVisible = isDockVisible.value,
            modifier = Modifier
                .fillMaxSize()
                .zIndex(4f)
        )

        // Edit Account Bottom Sheet
        editingAccount?.let { acc ->
            var nameText by remember(acc) { mutableStateOf(acc.accountName) }
            var typeText by remember(acc) { mutableStateOf(acc.accountType) }
            var balanceText by remember(acc) { mutableStateOf(String.format(Locale.US, "%.2f", acc.currentBalance)) }
            val formattedMab = remember(acc.minBalance) {
                if (acc.minBalance % 1.0 == 0.0) acc.minBalance.toLong().toString() else acc.minBalance.toString()
            }
            var minBalanceText by remember(acc) { mutableStateOf(formattedMab) }
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
                        .imePadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Edit Account", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)

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

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = typeText,
                        onValueChange = { typeText = it },
                        label = { Text("Account Role / Type (e.g., Operating, Commitments, Fortress, Cash)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split('.')
                            balanceText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                        },
                        label = { Text("Current Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = minBalanceText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split('.')
                            minBalanceText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                        },
                        label = { Text("Minimum Balance Threshold (MAB)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

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
                            Text("Hide from active accounts and entries", fontSize = 10.5.sp, color = TextMuted)
                        }
                        Switch(
                            checked = isArchivedState,
                            onCheckedChange = { isArchivedState = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AccentPurple)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val targetBal = balanceText.toDoubleOrNull() ?: acc.currentBalance
                            val minBal = minBalanceText.toDoubleOrNull() ?: acc.minBalance
                            if (nameText.isNotBlank()) {
                                pendingEditConfirmation = SimplePendingEditConfirmation(
                                    originalAccount = acc,
                                    updatedName = nameText.trim().uppercase(),
                                    updatedType = typeText.trim().ifBlank { "Operating" },
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

        // Confirmation Modal
        pendingEditConfirmation?.let { conf ->
            val isNameChanged = !conf.originalAccount.accountName.equals(conf.updatedName, ignoreCase = true)
            val isTypeChanged = !conf.originalAccount.accountType.equals(conf.updatedType, ignoreCase = true)
            val isBalChanged = abs(conf.targetBalance - conf.originalAccount.currentBalance) >= 0.01
            val isMabChanged = abs(conf.minBalance - conf.originalAccount.minBalance) >= 0.01
            val isArchiveChanged = conf.isArchived != conf.originalAccount.isArchived

            AlertDialog(
                onDismissRequest = { pendingEditConfirmation = null },
                title = { Text("Confirm Modifications?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (isNameChanged) {
                            Text("• Rename: '${conf.originalAccount.accountName}' ➔ '${conf.updatedName}'")
                        }
                        if (isTypeChanged) {
                            Text("• Role/Type: '${conf.originalAccount.accountType}' ➔ '${conf.updatedType}'")
                        }
                        if (isBalChanged) {
                            val diff = conf.targetBalance - conf.originalAccount.currentBalance
                            Text("• Balance Adjustment: ${if (diff > 0) "+" else ""}${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", diff)}")
                        }
                        if (isMabChanged) {
                            Text("• Minimum Balance (MAB): ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", conf.minBalance)}")
                        }
                        if (isArchiveChanged) {
                            Text("• Archive Status: ${if (conf.isArchived) "Archived" else "Active"}")
                        }
                        if (!isNameChanged && !isTypeChanged && !isBalChanged && !isMabChanged && !isArchiveChanged) {
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
                                accountType = conf.updatedType.ifBlank { "Operating" },
                                minBalance = conf.minBalance,
                                isArchived = conf.isArchived,
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

        // Reorder Accounts Sheet
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
                    Text("Reorder Accounts", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Text("Adjust display sequence across screens and quick-pick menus", fontSize = 11.5.sp, color = TextMuted)
                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = reorderedList,
                            key = { _, acc -> acc.accountName }
                        ) { index, account ->
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

        // Delete Account Alert
        accountToDelete?.let { acc ->
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                title = { Text("Delete Account?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = { Text("Are you sure you want to remove '${acc.accountName}'? Accounts with existing transactions cannot be removed without reassigning.", fontSize = 13.sp) },
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

        // Instant Transfer Bottom Sheet
        if (showTransferSheet) {
            AccountTransferDialog(
                accounts = accountNames,
                currencySymbol = userProfile.currencySymbol,
                onDismiss = { showTransferSheet = false },
                onTransfer = { from, to, amount, note, subtype, date, isRecurring, dueDay ->
                    if (isRecurring) {
                        viewModel.addFixedBill(
                            title = note.ifBlank { "Vault Transfer ($from ➔ $to)" },
                            amount = amount,
                            category = "Transfer",
                            subcategory = subtype.name,
                            account = from,
                            toAccount = to,
                            type = TransactionType.TRANSFER,
                            dueDay = dueDay,
                            isPaid = true,
                            paidDateMillis = date
                        )
                        Toast.makeText(context, "Saved as recurring sweep & transferred ${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", amount)}", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.executeInstantTransfer(
                            fromAccount = from,
                            toAccount = to,
                            amount = amount,
                            note = note,
                            subtype = subtype,
                            date = date
                        )
                        Toast.makeText(context, "Transferred ${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", amount)} to $to", Toast.LENGTH_SHORT).show()
                    }
                    showTransferSheet = false
                }
            )
        }

        // Add Account Bottom Sheet
        if (showAddAccountSheet) {
            var name by remember { mutableStateOf("") }
            var type by remember { mutableStateOf("Operating") }
            var balanceText by remember { mutableStateOf("") }
            var minBalanceText by remember { mutableStateOf("0") }
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
                        .imePadding()
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("Add Account", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Register a new balance ledger", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (e.g., HDFC Salary, Cash Wallet)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Account Role / Type (e.g., Operating, Commitments, Fortress, Cash)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = balanceText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split('.')
                            balanceText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                        },
                        label = { Text("Starting Balance (${userProfile.currencySymbol})", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = minBalanceText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            val parts = filtered.split('.')
                            minBalanceText = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                        },
                        label = { Text("Minimum Balance (MAB)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                                    type = type.trim().ifBlank { "Operating" },
                                    minBalance = minBal
                                )
                                showAddAccountSheet = false
                                Toast.makeText(context, "Account '${name.trim().uppercase()}' added", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = name.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Create Account", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
