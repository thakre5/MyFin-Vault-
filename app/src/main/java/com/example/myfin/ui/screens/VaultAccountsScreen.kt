package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.myfin.ui.components.AccountTransferDialog
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs

enum class VaultTier(val title: String, val description: String, val color: Color) {
    OPERATING("Operating Vault", "Daily living & spending cashflow", SoftRed),
    COMMITMENTS("Commitments Vault", "AutoPay, EMIs & fixed bills", AccentPurple),
    FORTRESS("Emergency Fortress", "Untouchable safety net & liquid reserves", SoftTeal)
}

data class SuccessReceiptPayload(
    val subtitle: String,
    val headline: String,
    val description: String,
    val buttonText: String = "Done"
)

private fun getVaultTierForAccount(accountName: String): VaultTier {
    return when {
        accountName.contains("BOM", ignoreCase = true) ||
        accountName.contains("AXIS", ignoreCase = true) ||
        accountName.contains("SBI", ignoreCase = true) ||
        accountName.contains("BILL", ignoreCase = true) -> VaultTier.COMMITMENTS

        accountName.contains("INDUSIND", ignoreCase = true) ||
        accountName.contains("FORTRESS", ignoreCase = true) ||
        accountName.contains("FD", ignoreCase = true) ||
        accountName.contains("RESERVE", ignoreCase = true) -> VaultTier.FORTRESS

        else -> VaultTier.OPERATING
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
    var receiptPayload by remember { mutableStateOf<SuccessReceiptPayload?>(null) }

    var showActionMenu by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountBalanceResult?>(null) }

    val accountsList = remember(uiState.accounts) { uiState.accounts }
    val accountNames = remember(accountsList) {
        if (accountsList.isEmpty()) listOf("BOM", "CASH", "HDFC", "INDUSIND")
        else accountsList.map { it.accountName }
    }

    val totalLiquidBalance = remember(accountsList) { accountsList.sumOf { it.balance } }
    val totalAutoPayLiabilities = remember(uiState.fixedBills) {
        uiState.fixedBills.filter { !it.isPaid && it.type == TransactionType.EXPENSE }.sumOf { it.amount }
    }

    Box(modifier = Modifier.fillMaxSize().background(CanvasLight)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                    text = "Vault Accounts",
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = TextDark
                )

                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                ) {
                    Text(
                        text = "${accountsList.size} Accounts",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Strategy Switcher Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(BorderLight.copy(alpha = 0.5f))
                    .padding(2.5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isThreeVaultStrategy) CardWhite else Color.Transparent)
                        .clickable {
                            if (isThreeVaultStrategy) {
                                pendingModeTarget = false
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Simple Mode",
                        fontWeight = if (!isThreeVaultStrategy) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = if (!isThreeVaultStrategy) TextDark else TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isThreeVaultStrategy) CardWhite else Color.Transparent)
                        .clickable {
                            if (!isThreeVaultStrategy) {
                                pendingModeTarget = true
                            }
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "3-Vault Strategy",
                        fontWeight = if (isThreeVaultStrategy) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.5.sp,
                        color = if (isThreeVaultStrategy) AccentPurple else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 105.dp)
            ) {
                // Top Hero Consolidated Liquidity Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(3.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = CardWhite,
                        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                    ) {
                        Column(
                            modifier = Modifier
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFFFFFFFF),
                                            Color(0xFFFCFBFE),
                                            Color(0xFFF6F4FD)
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
                                Text(
                                    text = if (isThreeVaultStrategy) "3-VAULT LIQUID FORTRESS" else "TOTAL LIQUID BALANCE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextMuted,
                                    letterSpacing = 0.6.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isThreeVaultStrategy) AccentPurple.copy(alpha = 0.12f) else SoftGreen.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (isThreeVaultStrategy) "Strategic Shield" else "Active Ledger",
                                        color = if (isThreeVaultStrategy) AccentPurple else SoftGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", totalLiquidBalance)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark,
                                letterSpacing = (-0.5).sp
                            )

                            Text(
                                text = if (totalAutoPayLiabilities > 0) {
                                    "Pending AutoPay obligations: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", totalAutoPayLiabilities)}"
                                } else {
                                    "All monthly fixed commitments covered"
                                },
                                fontSize = 11.sp,
                                color = if (totalAutoPayLiabilities > 0) SoftAmber else TextMuted
                            )

                            if (isThreeVaultStrategy) {
                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                val opTotal = accountsList.filter { getVaultTierForAccount(it.accountName) == VaultTier.OPERATING }.sumOf { it.balance }
                                val comTotal = accountsList.filter { getVaultTierForAccount(it.accountName) == VaultTier.COMMITMENTS }.sumOf { it.balance }
                                val fortTotal = accountsList.filter { getVaultTierForAccount(it.accountName) == VaultTier.FORTRESS }.sumOf { it.balance }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    VaultTierStat(
                                        tier = "Operating",
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", opTotal)}",
                                        color = SoftRed
                                    )
                                    VaultTierStat(
                                        tier = "Commitments",
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", comTotal)}",
                                        color = AccentPurple
                                    )
                                    VaultTierStat(
                                        tier = "Fortress",
                                        amount = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", fortTotal)}",
                                        color = SoftTeal
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Account Cards
                if (accountsList.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite
                        ) {
                            Box(modifier = Modifier.padding(28.dp), contentAlignment = Alignment.Center) {
                                Text("No accounts detected. Tap '+' below to initialize an account.", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else if (!isThreeVaultStrategy) {
                    // Simple Mode
                    items(accountsList, key = { it.accountName }) { acc ->
                        SwipeableAccountItem(
                            account = acc,
                            currencySymbol = userProfile.currencySymbol,
                            showRoleBadge = false,
                            onEdit = { editingAccount = acc },
                            onTransfer = { showTransferDialog = true }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                } else {
                    // 3-Vault Strategy Mode
                    VaultTier.values().forEach { tier ->
                        val tierAccounts = accountsList.filter { acc -> getVaultTierForAccount(acc.accountName) == tier }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 6.dp, start = 2.dp, end = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(tier.color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tier.title.uppercase(),
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = TextMuted,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = tier.description,
                                    fontSize = 10.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        if (tierAccounts.isEmpty()) {
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = CanvasLight,
                                    border = BorderStroke(0.6.dp, BorderLight)
                                ) {
                                    Text(
                                        text = "No accounts assigned to this tier",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            items(tierAccounts, key = { it.accountName }) { acc ->
                                SwipeableAccountItem(
                                    account = acc,
                                    currencySymbol = userProfile.currencySymbol,
                                    showRoleBadge = true,
                                    roleTitle = tier.name.lowercase().replaceFirstChar { it.uppercase() },
                                    roleColor = tier.color,
                                    onEdit = { editingAccount = acc },
                                    onTransfer = { showTransferDialog = true }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
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

        // Anchored Action Menu
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
                enter = scaleIn(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(180)
                ) + fadeIn(animationSpec = tween(180)),
                exit = scaleOut(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(150)
                ) + fadeOut(animationSpec = tween(150)),
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
                                    showTransferDialog = true
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
                            "This organizes your accounts into Operating (daily spend), Commitments (AutoPay bills), and Emergency Fortress tiers. Your balances remain completely intact."
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
                                    "Accounts structured into Operating, Commitments, and Fortress tiers."
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

        // Instant Transfer Dialog
        if (showTransferDialog) {
            AccountTransferDialog(
                accounts = accountNames,
                onDismiss = { showTransferDialog = false },
                onTransfer = { from, to, amount, note ->
                    viewModel.executeInstantTransfer(from, to, amount, note)
                    receiptPayload = SuccessReceiptPayload(
                        subtitle = "Transfer Successful",
                        headline = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", amount)}",
                        description = "$from ➔ $to ${if (note.isNotBlank()) "($note)" else ""}",
                        buttonText = "View Vaults"
                    )
                }
            )
        }

        // Bottom Sheet: Success Receipt (Matching Attached Reference)
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
                    // Circular Purple Checkmark Badge
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

        // Add Account Bottom Sheet
        if (showAddAccountSheet) {
            var name by remember { mutableStateOf("") }
            var balanceText by remember { mutableStateOf("") }
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
                    Text("Add bank account, wallet, or liquid fortress", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (e.g., HDFC, ICICI, Cash)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

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

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val bal = balanceText.toDoubleOrNull() ?: 0.0
                                if (bal > 0) {
                                    viewModel.saveTransaction(
                                        id = null,
                                        title = "Opening Balance",
                                        amount = bal,
                                        category = "General",
                                        subcategory = "Opening Balance",
                                        account = name.trim().uppercase(),
                                        type = TransactionType.INCOME,
                                        date = System.currentTimeMillis()
                                    )
                                }
                                showAddAccountSheet = false
                                Toast.makeText(context, "Account '${name.trim().uppercase()}' initialized", Toast.LENGTH_SHORT).show()
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

        // Adjust Account Balance Sheet
        editingAccount?.let { acc ->
            var newBalanceText by remember { mutableStateOf(acc.balance.toInt().toString()) }
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
                            val targetBal = newBalanceText.toDoubleOrNull() ?: acc.balance
                            val diff = targetBal - acc.balance
                            if (diff != 0.0) {
                                val txType = if (diff > 0) TransactionType.INCOME else TransactionType.EXPENSE
                                viewModel.saveTransaction(
                                    id = null,
                                    title = "Balance Adjustment",
                                    amount = abs(diff),
                                    category = "General",
                                    subcategory = "Adjustment",
                                    account = acc.accountName,
                                    type = txType,
                                    date = System.currentTimeMillis()
                                )
                            }
                            editingAccount = null
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
    }
}

@Composable
private fun VaultTierStat(tier: String, amount: String, color: Color) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(color))
            Spacer(modifier = Modifier.width(4.dp))
            Text(tier, fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(amount, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableAccountItem(
    account: AccountBalanceResult,
    currencySymbol: String,
    showRoleBadge: Boolean = false,
    roleTitle: String = "Operating",
    roleColor: Color = AccentPurple,
    onEdit: () -> Unit,
    onTransfer: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val currentOnEdit by rememberUpdatedState(onEdit)
    val currentOnTransfer by rememberUpdatedState(onTransfer)

    var lastTargetValue by remember { mutableStateOf(SwipeToDismissBoxValue.Settled) }

    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { totalDistance -> totalDistance * 0.35f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnEdit()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    currentOnTransfer()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    LaunchedEffect(dismissState.targetValue) {
        if (dismissState.targetValue != lastTargetValue && dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastTargetValue = dismissState.targetValue
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val direction = dismissState.dismissDirection
            val backgroundColor by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.StartToEnd -> AccentPurple
                    SwipeToDismissBoxValue.EndToStart -> SoftTeal
                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                },
                animationSpec = tween(200),
                label = "accountSwipeBg"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 20.dp),
                contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                if (direction == SwipeToDismissBoxValue.StartToEnd) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Edit, contentDescription = "Adjust", tint = Color.White, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Adjust", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                    }
                } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Transfer", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.SyncAlt, contentDescription = "Transfer", tint = Color.White, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(1.dp, RoundedCornerShape(16.dp))
                .clickable(onClick = onEdit),
            shape = RoundedCornerShape(16.dp),
            color = CardWhite,
            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 15.dp, vertical = 13.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    account.accountName.contains("HDFC", true) -> Color(0xFFE8F0FE)
                                    account.accountName.contains("BOM", true) -> Color(0xFFFFF3E0)
                                    account.accountName.contains("INDUSIND", true) -> Color(0xFFF3E5F5)
                                    account.accountName.contains("CASH", true) -> Color(0xFFE8F5E9)
                                    else -> AccentPurple.copy(alpha = 0.12f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                account.accountName.contains("CASH", true) -> Icons.Default.Payments
                                else -> Icons.Default.AccountBalance
                            },
                            contentDescription = null,
                            tint = when {
                                account.accountName.contains("HDFC", true) -> Color(0xFF1A73E8)
                                account.accountName.contains("BOM", true) -> Color(0xFFE65100)
                                account.accountName.contains("INDUSIND", true) -> Color(0xFF7B1FA2)
                                account.accountName.contains("CASH", true) -> Color(0xFF2E7D32)
                                else -> AccentPurple
                            },
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = account.accountName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (showRoleBadge) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = roleColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = roleTitle,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = roleColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Swipe to adjust or transfer",
                            fontSize = 10.5.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "$currencySymbol${String.format(Locale.US, "%,.2f", account.balance)}",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.5.sp,
                    color = if (account.balance >= 0) TextDark else SoftRed
                )
            }
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
