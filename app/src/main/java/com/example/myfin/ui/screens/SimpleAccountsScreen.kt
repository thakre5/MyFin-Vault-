package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.AppBottomDock
import com.example.myfin.ui.components.DockFabAction
import com.example.myfin.ui.components.NavigationTarget
import com.example.myfin.ui.components.rememberAutoScrollVisibilityConnection
import com.example.myfin.ui.theme.*
import java.util.Locale
import kotlin.math.abs

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

    var showTransferSheet by remember { mutableStateOf(false) }
    var showAddAccountSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountBalanceResult?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    val (isDockVisible, scrollConnection) = rememberAutoScrollVisibilityConnection()

    val displayAccounts = uiState.accounts
    val accountNames = remember(displayAccounts) { displayAccounts.map { it.accountName } }
    val totalBalance = remember(displayAccounts) { displayAccounts.sumOf { it.currentBalance } }

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
        Column(modifier = Modifier.fillMaxSize()) {
            // =========================================================
            // 1. PINNED TOP HEADER WITH SHELF DISSOLVE
            // =========================================================
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

                    Text(
                        text = "Accounts",
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
                                    contentDescription = "Settings",
                                    tint = AccentPurple,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }

                // Smooth Dissolve Shelf Placed Below Top Header
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

            // =========================================================
            // 2. SCROLLABLE ACCOUNTS LIST
            // =========================================================
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 125.dp)
            ) {
                // Total Balance Hero Card
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
                                            AccentPurple.copy(alpha = 0.05f)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Total Net Liquidity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", totalBalance)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showTransferSheet = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                                ) {
                                    Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Transfer", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { showAddAccountSheet = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BorderLight)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = TextDark, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Account", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Accounts Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Bank Accounts & Wallets",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextDark
                        )
                        Text(
                            text = "${displayAccounts.size} Accounts",
                            fontSize = 11.5.sp,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Flat Account List
                if (displayAccounts.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, BorderLight)
                        ) {
                            Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                Text("No accounts registered yet.", fontSize = 12.sp, color = TextMuted)
                            }
                        }
                    }
                } else {
                    items(displayAccounts) { acc ->
                        val maskedDigits = remember(acc.accountName) {
                            String.format(Locale.US, "%04d", abs(acc.accountName.hashCode() % 9000 + 1000))
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp)
                                .shadow(1.5.dp, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            color = CardWhite,
                            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(AccentPurple.copy(alpha = 0.10f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.AccountBalance,
                                            contentDescription = null,
                                            tint = AccentPurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = acc.accountName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.5.sp,
                                            color = TextDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "•••• $maskedDigits",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", acc.currentBalance)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.5.sp,
                                        color = if (acc.currentBalance >= 0) TextDark else SoftRed
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { editingAccount = acc },
                                        modifier = Modifier.size(30.dp)
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
                        }
                    }
                }
            }
        }

        // =========================================================
        // 3. BOTTOM GRADIENT SCRIM (DISSOLVES CONTENT BEFORE DOCK)
        // =========================================================
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

        // =========================================================
        // 4. STANDARDIZED FLOATING BOTTOM DOCK WITH CONTEXTUAL FAB
        // =========================================================
        AppBottomDock(
            currentSelection = NavigationTarget.VAULT_ACCOUNTS,
            onSelectTarget = { target ->
                when (target) {
                    NavigationTarget.MONTHLY_VIEW -> onNavigateToDashboard()
                    NavigationTarget.DATA_SET -> onNavigateToTaxonomy()
                    NavigationTarget.BUDGET_PLANNER -> onNavigateToPlanner()
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

        // Edit Account Bottom Sheet
        editingAccount?.let { acc ->
            var nameText by remember(acc) { mutableStateOf(acc.accountName) }
            var balanceText by remember(acc) { mutableStateOf(String.format(Locale.US, "%.2f", acc.currentBalance)) }
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
                                    sortOrder = acc.sortOrder
                                )
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Account", tint = SoftRed, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

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

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val targetBal = balanceText.toDoubleOrNull() ?: acc.currentBalance
                            if (nameText.isNotBlank()) {
                                viewModel.updateAccountDetails(
                                    oldName = acc.accountName,
                                    newName = nameText.trim().uppercase(),
                                    startingBalance = acc.startingBalance,
                                    accountType = acc.accountType,
                                    sortOrder = acc.sortOrder
                                )
                                if (targetBal != acc.currentBalance) {
                                    viewModel.adjustAccountBalance(nameText.trim().uppercase(), targetBal)
                                }
                                editingAccount = null
                                Toast.makeText(context, "Account updated", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = nameText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // Delete Account Confirmation Alert
        accountToDelete?.let { acc ->
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                title = { Text("Delete Account?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                text = { Text("Are you sure you want to remove '${acc.accountName}'?", fontSize = 13.sp) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAccount(acc) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) editingAccount = null
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
                    Text("Add Account", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Enter bank or wallet name and starting balance", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Account Name (e.g., Primary Salary, Cash)", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentPurple, unfocusedBorderColor = BorderLight)
                    )

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

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                val bal = balanceText.toDoubleOrNull() ?: 0.0
                                viewModel.addAccount(
                                    name = name.trim().uppercase(),
                                    startingBalance = bal,
                                    type = "General"
                                )
                                showAddAccountSheet = false
                                Toast.makeText(context, "Account '${name.trim().uppercase()}' created", Toast.LENGTH_SHORT).show()
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

        // Instant Transfer Bottom Sheet
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
                    Text("Transfer Funds", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("Move money between your accounts", fontSize = 11.5.sp, color = TextMuted)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("From", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
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

                    Text("To", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
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
                        label = { Text("Note (Optional)", fontSize = 12.sp) },
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
                                Toast.makeText(context, "Transferred ${userProfile.currencySymbol}$amt", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Enter a valid amount and distinct accounts", Toast.LENGTH_SHORT).show()
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
    }
}
