package com.example.myfin.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.AccountEntity
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.*
import java.util.Locale

@Composable
fun VaultAccountsScreen(
    viewModel: BudgetViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.monthlyUiState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    var showAddAccountDialog by remember { mutableStateOf(false) }
    var accountToEdit by remember { mutableStateOf<AccountEntity?>(null) }
    var accountToDelete by remember { mutableStateOf<AccountEntity?>(null) }

    val bottomNavPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CardWhite)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Menu", tint = TextDark)
                }

                Text("Vault Accounts", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)

                IconButton(
                    onClick = { showAddAccountDialog = true },
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CardWhite)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Account", tint = AccentPurple)
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp + bottomNavPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Total Vault Liquidity Card
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(6.dp, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        color = CardWhite
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Total Net Liquid Vault", fontSize = 12.sp, color = TextMuted, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", uiState.metrics.totalVaultBalance)}",
                                fontWeight = FontWeight.Black,
                                fontSize = 24.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Configured across ${uiState.accounts.size} active vaults (Operating, Commitments, & Fortress reserves).",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                items(uiState.accounts, key = { it.accountName }) { acc ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(2.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = CardWhite
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.accountName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${acc.accountType} • Initial: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", acc.startingBalance)}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Current", fontSize = 10.sp, color = TextMuted)
                                    Text(
                                        text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.2f", acc.currentBalance)}",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = if (acc.currentBalance >= 0) SoftGreen else SoftRed
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                IconButton(
                                    onClick = {
                                        accountToEdit = AccountEntity(
                                            accountName = acc.accountName,
                                            startingBalance = acc.startingBalance,
                                            accountType = acc.accountType,
                                            sortOrder = acc.sortOrder
                                        )
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextMuted, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        accountToDelete = AccountEntity(
                                            accountName = acc.accountName,
                                            startingBalance = acc.startingBalance,
                                            accountType = acc.accountType,
                                            sortOrder = acc.sortOrder
                                        )
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SoftRed, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Account Dialog
        if (showAddAccountDialog) {
            var name by remember { mutableStateOf("") }
            var startingBalance by remember { mutableStateOf("") }
            var type by remember { mutableStateOf("Bank") }

            AlertDialog(
                onDismissRequest = { showAddAccountDialog = false },
                title = { Text("Add Vault Account", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Vault Name (e.g. HDFC, BOM, Cash)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = startingBalance,
                            onValueChange = { startingBalance = it },
                            label = { Text("Starting Balance") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = type,
                            onValueChange = { type = it },
                            label = { Text("Type (Bank / Cash / Card)") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val bal = startingBalance.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank()) {
                            viewModel.addAccount(name.trim(), bal, type.trim())
                            showAddAccountDialog = false
                        }
                    }) { Text("Create") }
                },
                dismissButton = {
                    TextButton(onClick = { showAddAccountDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Edit Account Dialog
        accountToEdit?.let { acc ->
            var updatedName by remember { mutableStateOf(acc.accountName) }
            var updatedBalance by remember { mutableStateOf(acc.startingBalance.toString()) }
            var updatedType by remember { mutableStateOf(acc.accountType) }

            AlertDialog(
                onDismissRequest = { accountToEdit = null },
                title = { Text("Edit Vault Account", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = updatedName,
                            onValueChange = { updatedName = it },
                            label = { Text("Vault Name") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = updatedBalance,
                            onValueChange = { updatedBalance = it },
                            label = { Text("Starting Balance") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = updatedType,
                            onValueChange = { updatedType = it },
                            label = { Text("Type") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val bal = updatedBalance.toDoubleOrNull() ?: acc.startingBalance
                        if (updatedName.isNotBlank()) {
                            viewModel.updateAccountDetails(
                                oldName = acc.accountName,
                                newName = updatedName.trim(),
                                startingBalance = bal,
                                accountType = updatedType.trim(),
                                sortOrder = acc.sortOrder
                            )
                            accountToEdit = null
                        }
                    }) { Text("Save") }
                },
                dismissButton = {
                    TextButton(onClick = { accountToEdit = null }) { Text("Cancel") }
                }
            )
        }

        // Delete Account Confirmation Dialog
        accountToDelete?.let { acc ->
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                title = { Text("Delete Vault '${acc.accountName}'?", fontWeight = FontWeight.Bold) },
                text = { Text("An account can only be removed if it has 0 linked transactions. Please reassign transactions first if any exist.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteAccount(acc) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            accountToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SoftRed)
                    ) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { accountToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }
}
