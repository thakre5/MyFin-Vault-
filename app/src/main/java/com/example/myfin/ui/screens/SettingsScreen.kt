package com.example.myfin.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.ExcelExportManager
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.SettingsActiveSheet
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BudgetViewModel,
    initialActiveSheet: SettingsActiveSheet = SettingsActiveSheet.NONE,
    onOpenDrawer: () -> Unit,
    onNavigateToGuide: () -> Unit,
    onNavigateToVaults: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val userProfile by viewModel.userProfile.collectAsState()

    val excelExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                val success = ExcelExportManager.exportToUri(context, it, userProfile.currencySymbol)
                if (success) {
                    Toast.makeText(context, "Excel Ledger Exported Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val jsonBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.backupVaultToEncryptedJson(context, it) { success, msg ->
                if (success) {
                    Toast.makeText(context, "Encrypted JSON Backup Created", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, msg.ifBlank { "Backup failed" }, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.restoreVaultFromEncryptedJson(context, it) { success, msg ->
                if (success) {
                    Toast.makeText(context, "Vault Restored Successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, msg.ifBlank { "Restore failed" }, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

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
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onOpenDrawer()
                    },
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(CardWhite)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark)
                }

                Text("Vault Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)

                Spacer(modifier = Modifier.size(38.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 40.dp + bottomNavPadding
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section: Security & Privacy
                item {
                    Text("SECURITY & WINDOW MASKING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Biometric Authentication", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                    Text("Require Fingerprint/Face to open vault", fontSize = 11.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = userProfile.isBiometricEnabled,
                                    onCheckedChange = { viewModel.updateBiometricEnabled(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple)
                                )
                            }

                            HorizontalDivider(color = BorderLight, thickness = 0.6.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Allow Screenshots & Previews", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                    Text("Disables FLAG_SECURE masking across Android", fontSize = 11.sp, color = TextMuted)
                                }
                                Switch(
                                    checked = userProfile.isScreenCaptureAllowed,
                                    onCheckedChange = { viewModel.updateScreenCaptureAllowed(it) },
                                    colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple)
                                )
                            }
                        }
                    }
                }

                // Section: Backups & Data Export
                item {
                    Text("OFFLINE EXPORTS & RECOVERY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            SettingsNavRow(
                                icon = Icons.Default.TableChart,
                                title = "Export Excel Ledger (.xlsx)",
                                subtitle = "Itemized ledger, category breakdown & bank snapshot",
                                onClick = {
                                    excelExportLauncher.launch("MyFin_Ledger_${System.currentTimeMillis()}.xlsx")
                                }
                            )

                            HorizontalDivider(color = BorderLight, thickness = 0.6.dp)

                            SettingsNavRow(
                                icon = Icons.Default.CloudUpload,
                                title = "Create Encrypted Backup (.json)",
                                subtitle = "Export AES-encrypted state of the entire database",
                                onClick = {
                                    jsonBackupLauncher.launch("MyFin_Backup_${System.currentTimeMillis()}.json")
                                }
                            )

                            HorizontalDivider(color = BorderLight, thickness = 0.6.dp)

                            SettingsNavRow(
                                icon = Icons.Default.CloudDownload,
                                title = "Restore from Backup File",
                                subtitle = "Re-seed vault database from an existing .json file",
                                onClick = {
                                    jsonRestoreLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )
                        }
                    }
                }

                // Section: App Guidance & 3-Bank Topology
                item {
                    Text("LEARNING & ARCHITECTURE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = CardWhite
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                            SettingsNavRow(
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                title = "MyFin Architecture Guide",
                                subtitle = "Read about 3-Bank routing, cashflow buffers & formulas",
                                onClick = onNavigateToGuide
                            )

                            HorizontalDivider(color = BorderLight, thickness = 0.6.dp)

                            SettingsNavRow(
                                icon = Icons.Default.AccountBalance,
                                title = "Configure Vault Accounts",
                                subtitle = "Manage Operating, Commitments & Fortress accounts",
                                onClick = onNavigateToVaults
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AccentPurple.copy(alpha = 0.12f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                Text(subtitle, fontSize = 11.sp, color = TextMuted)
            }
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}
