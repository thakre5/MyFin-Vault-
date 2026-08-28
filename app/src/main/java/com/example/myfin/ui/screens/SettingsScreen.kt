package com.example.myfin.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.myfin.data.ExcelExportManager
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.AppBrandingFooter
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class SettingsActiveSheet {
    NONE,
    PERSONAL_INFO,
    STRATEGY,
    SECURITY,
    NOTIFICATIONS,
    CURRENCY,
    DATA_MANAGEMENT
}

private val BrandGreen = Color(0xFF5BB336)
private val BrandBlue = Color(0xFF1E88E5)
private val BrandCharcoal = Color(0xFF1C1D21)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BudgetViewModel,
    initialActiveSheet: SettingsActiveSheet = SettingsActiveSheet.NONE,
    onOpenDrawer: () -> Unit,
    onNavigateToGuide: () -> Unit = {},
    onNavigateToVaults: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userProfile by viewModel.userProfile.collectAsState()

    var activeSheet by rememberSaveable { mutableStateOf(initialActiveSheet) }

    val is3VaultActive = remember(userProfile.vaultMode) {
        !userProfile.vaultMode.equals("SIMPLE", ignoreCase = true)
    }

    // Export & Backup File Pickers
    val xlsxExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val ok = ExcelExportManager.exportToUri(context, it, userProfile.currencySymbol)
                Toast.makeText(context, if (ok) "Excel statement saved!" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                val ok = viewModel.exportCsvToUri(context, it)
                Toast.makeText(context, if (ok) "CSV Ledger exported!" else "Export failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val jsonBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.backupVaultToEncryptedJson(context, it) { success, _ ->
                Toast.makeText(context, if (success) "Full encrypted backup saved!" else "Backup failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.restoreVaultFromEncryptedJson(context, it) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfileImageUri(it.toString()) }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateReminderSettings(context, true, userProfile.reminderHour, userProfile.reminderMinute)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // 1. Top Header Bar (Matching Reference Image 1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
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
                        contentDescription = "Back / Menu",
                        tint = TextDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Settings",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDark,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )

                IconButton(
                    onClick = onNavigateToGuide,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(38.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "User Guide",
                        tint = TextDark,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 2. Profile & Identity Header (Matching Reference Image 1)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(86.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(82.dp)
                                .clip(CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            shape = CircleShape,
                            color = AccentPurple.copy(alpha = 0.15f),
                            border = BorderStroke(1.5.dp, CardWhite)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userProfile.displayName.take(1).uppercase().ifBlank { "S" },
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentPurple
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            shape = CircleShape,
                            color = BrandBlue,
                            border = BorderStroke(1.5.dp, CardWhite)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = "Change photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = userProfile.displayName.ifBlank { "Sushant" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )

                    Text(
                        text = userProfile.email.ifBlank { "sushant@example.com" },
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "DOB: ${userProfile.dateOfBirth.ifBlank { "2000-03-21" }} • Inflow: ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", userProfile.baseMonthlyIncome)}/mo",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { activeSheet = SettingsActiveSheet.PERSONAL_INFO },
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "Edit Profile",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Continuous White Card Container
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = CardWhite
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 20.dp)
                            .padding(bottom = 36.dp),
                        verticalArrangement = Arrangement.spacedBy(22.dp)
                    ) {
                        // Section: Strategy & Architecture
                        SettingsSectionGroup(title = "Strategy & Architecture") {
                            SettingsSwitchRow(
                                title = "3-Vault Strategy",
                                subtitle = if (is3VaultActive) "Operating, Commitments & Fortress active" else "Simple flat accounts pool active",
                                isChecked = is3VaultActive,
                                onToggle = { activeSheet = SettingsActiveSheet.STRATEGY },
                                onClick = { activeSheet = SettingsActiveSheet.STRATEGY }
                            )

                            SettingsNavigationRow(
                                title = "Fortress Safety Net Target",
                                value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", userProfile.fortressThreshold)}",
                                onClick = { activeSheet = SettingsActiveSheet.STRATEGY }
                            )

                            SettingsNavigationRow(
                                title = "Expected Monthly Salary Inflow",
                                value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", userProfile.baseMonthlyIncome)}",
                                onClick = { activeSheet = SettingsActiveSheet.PERSONAL_INFO }
                            )
                        }

                        // Section: Security & Privacy
                        SettingsSectionGroup(title = "Security & Privacy") {
                            SettingsSwitchRow(
                                title = "Biometric Authentication",
                                subtitle = "Fingerprint / Face unlock protection",
                                isChecked = userProfile.isBiometricEnabled,
                                onToggle = { activeSheet = SettingsActiveSheet.SECURITY },
                                onClick = { activeSheet = SettingsActiveSheet.SECURITY }
                            )

                            SettingsNavigationRow(
                                title = "Master PIN Passcode",
                                value = "Modify PIN",
                                onClick = { activeSheet = SettingsActiveSheet.SECURITY }
                            )

                            SettingsSwitchRow(
                                title = "Anti-Spy Screen Protection",
                                subtitle = "Blocks screenshots & app-switcher previews",
                                isChecked = !userProfile.isScreenCaptureAllowed,
                                onToggle = {
                                    viewModel.updateScreenCaptureAllowed(!userProfile.isScreenCaptureAllowed)
                                    Toast.makeText(context, if (userProfile.isScreenCaptureAllowed) "Screen privacy enabled" else "Screen capture allowed", Toast.LENGTH_SHORT).show()
                                },
                                onClick = {
                                    viewModel.updateScreenCaptureAllowed(!userProfile.isScreenCaptureAllowed)
                                }
                            )
                        }

                        // Section: Option & Notifications
                        SettingsSectionGroup(title = "Option & Notifications") {
                            val timeStr = String.format(Locale.US, "%02d:%02d", userProfile.reminderHour, userProfile.reminderMinute)
                            SettingsSwitchRow(
                                title = "Daily Expense Review Reminder",
                                subtitle = "Scheduled daily at $timeStr",
                                isChecked = userProfile.reminderEnabled,
                                onToggle = { activeSheet = SettingsActiveSheet.NOTIFICATIONS },
                                onClick = { activeSheet = SettingsActiveSheet.NOTIFICATIONS }
                            )

                            SettingsSwitchRow(
                                title = "AutoPay Commitment Alerts",
                                subtitle = "Warn 48h before fixed bill due dates",
                                isChecked = userProfile.isAutoPayReminderEnabled,
                                onToggle = {
                                    val newStatus = !userProfile.isAutoPayReminderEnabled
                                    viewModel.saveUserProfile(userProfile.copy(isAutoPayReminderEnabled = newStatus))
                                }
                            )

                            SettingsSwitchRow(
                                title = "Budget Overrun Warnings",
                                subtitle = "Alert when spend pace exceeds targets",
                                isChecked = userProfile.isOverrunWarningEnabled,
                                onToggle = {
                                    val newStatus = !userProfile.isOverrunWarningEnabled
                                    viewModel.saveUserProfile(userProfile.copy(isOverrunWarningEnabled = newStatus))
                                }
                            )
                        }

                        // Section: Currency & Preferences
                        SettingsSectionGroup(title = "Currency & Preferences") {
                            SettingsNavigationRow(
                                title = "Primary Currency Symbol",
                                value = userProfile.currencySymbol,
                                onClick = { activeSheet = SettingsActiveSheet.CURRENCY }
                            )

                            SettingsNavigationRow(
                                title = "Connected Vault Accounts",
                                value = "Manage Vaults",
                                onClick = onNavigateToVaults
                            )
                        }

                        // Section: Full Backup Snapshot
                        SettingsSectionGroup(title = "Full Data Backup & Recovery") {
                            SettingsNavigationRow(
                                title = "Create Full Vault Snapshot (.json)",
                                value = "Backup",
                                onClick = {
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    jsonBackupLauncher.launch("MyFin_Backup_$timeStamp.json")
                                }
                            )

                            SettingsNavigationRow(
                                title = "Restore from Encrypted Snapshot",
                                value = "Restore",
                                onClick = {
                                    jsonRestoreLauncher.launch(arrayOf("application/json", "text/plain"))
                                }
                            )
                        }

                        // Section: Accounting Statements & Exports
                        SettingsSectionGroup(title = "Accounting Statements & Reports") {
                            SettingsNavigationRow(
                                title = "Export Excel Statement (.xlsx)",
                                value = "Generate",
                                onClick = {
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    xlsxExportLauncher.launch("MyFin_Statement_$timeStamp.xlsx")
                                }
                            )

                            SettingsNavigationRow(
                                title = "Export Universal Ledger (.csv)",
                                value = "Export",
                                onClick = {
                                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                                    csvExportLauncher.launch("MyFin_Ledger_$timeStamp.csv")
                                }
                            )
                        }

                        // Section: Danger Zone
                        SettingsSectionGroup(title = "Danger Zone") {
                            SettingsNavigationRow(
                                title = "Reset Entire Financial Vault",
                                value = "Wipe Database",
                                isDestructive = true,
                                onClick = { activeSheet = SettingsActiveSheet.DATA_MANAGEMENT }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Reusable Shared Branding Footer Component
                        AppBrandingFooter(
                            modifier = Modifier.fillMaxWidth(),
                            version = "v1.0.0",
                            showIcon = true
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // BOTTOM SHEETS & CONFIRMATION MODALS
    // ==========================================

    // 1. Biometric Confirmation Sheet (Matching Reference Image 2)
    if (activeSheet == SettingsActiveSheet.SECURITY) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BiometricIllustrationCanvas(modifier = Modifier.fillMaxSize())
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Verify your identity",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandCharcoal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Biometric authentication encrypts your local database access using device hardware keys. Your stored data never leaves this phone.",
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val newBiometricState = !userProfile.isBiometricEnabled
                        viewModel.updateBiometricEnabled(newBiometricState)
                        activeSheet = SettingsActiveSheet.NONE
                        Toast.makeText(context, if (newBiometricState) "Biometrics Enabled" else "Biometrics Disabled", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (userProfile.isBiometricEnabled) CoralAccent else BrandGreen)
                ) {
                    Text(
                        text = if (userProfile.isBiometricEnabled) "Disable Biometrics" else "Enable Biometric Unlock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 2. Strategy Mode Guidance Sheet (Matching Reference Image 3)
    if (activeSheet == SettingsActiveSheet.STRATEGY) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NeoclassicalBankCanvas(modifier = Modifier.fillMaxSize())
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (is3VaultActive) "3-Vault Strategy Active" else "Simple Mode Active",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandCharcoal,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (is3VaultActive) {
                        "Every time you log expenses or AutoPay bills, liquidity is routed cleanly through Operating, Commitments, and Fortress reserves."
                    } else {
                        "All connected bank accounts are tracked as a flat, unsegmented liquidity balance."
                    },
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CanvasLight,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (is3VaultActive) AccentPurple else BrandCharcoal,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (is3VaultActive) Icons.Default.Layers else Icons.Default.AccountBalanceWallet,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (is3VaultActive) "3-Vault Strategy" else "Simple Mode",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Text(
                                    text = if (is3VaultActive) "Operating • Commitments • Fortress" else "Flat accounts pool",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BrandGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Button(
                    onClick = {
                        val targetMode = if (is3VaultActive) "SIMPLE" else "3_VAULT"
                        viewModel.updateVaultMode(targetMode)
                        activeSheet = SettingsActiveSheet.NONE
                        Toast.makeText(context, if (targetMode == "3_VAULT") "Switched to 3-Vault Strategy" else "Switched to Simple Mode", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreen)
                ) {
                    Text(
                        text = if (is3VaultActive) "Switch to Simple Mode" else "Enable 3-Vault Strategy",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 3. Edit Personal Info Bottom Sheet
    if (activeSheet == SettingsActiveSheet.PERSONAL_INFO) {
        var nameInput by remember(userProfile) { mutableStateOf(userProfile.displayName) }
        var emailInput by remember(userProfile) { mutableStateOf(userProfile.email) }
        var dobInput by remember(userProfile) { mutableStateOf(userProfile.dateOfBirth) }
        var incomeInput by remember(userProfile) { mutableStateOf(String.format(Locale.US, "%.0f", userProfile.baseMonthlyIncome)) }

        ModalBottomSheet(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Configure identity & base financial parameters", fontSize = 12.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = dobInput,
                    onValueChange = { dobInput = it },
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    supportingText = { Text("Used as immutable security key for PIN recovery", fontSize = 10.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = incomeInput,
                    onValueChange = { incomeInput = it },
                    label = { Text("Expected Monthly Salary / Inflow (${userProfile.currencySymbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val parsedIncome = incomeInput.toDoubleOrNull() ?: userProfile.baseMonthlyIncome
                        viewModel.saveUserProfile(
                            userProfile.copy(
                                displayName = nameInput.trim(),
                                email = emailInput.trim(),
                                dateOfBirth = dobInput.trim(),
                                baseMonthlyIncome = parsedIncome
                            )
                        )
                        activeSheet = SettingsActiveSheet.NONE
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 4. Notification Settings Bottom Sheet
    if (activeSheet == SettingsActiveSheet.NOTIFICATIONS) {
        var hourInput by remember(userProfile) { mutableIntStateOf(userProfile.reminderHour) }
        var minInput by remember(userProfile) { mutableIntStateOf(userProfile.reminderMinute) }

        ModalBottomSheet(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Daily Expense Review", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Receive a gentle offline prompt to review transactions", fontSize = 12.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CanvasLight,
                    border = BorderStroke(0.6.dp, BorderLight)
                ) {
                    Text(
                        text = "• Reminders are scheduled locally using AlarmManager.\n• No background telemetry or internet connection required.\n• Ensure notification permissions are granted on Android 13+.",
                        fontSize = 11.5.sp,
                        color = TextMuted,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hourInput.toString(),
                        onValueChange = { hourInput = (it.toIntOrNull() ?: 20).coerceIn(0, 23) },
                        label = { Text("Hour (0-23)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    OutlinedTextField(
                        value = minInput.toString(),
                        onValueChange = { minInput = (it.toIntOrNull() ?: 0).coerceIn(0, 59) },
                        label = { Text("Minute (0-59)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.updateReminderSettings(context, true, hourInput, minInput)
                        }
                        activeSheet = SettingsActiveSheet.NONE
                        Toast.makeText(context, "Reminder set for ${String.format(Locale.US, "%02d:%02d", hourInput, minInput)}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandCharcoal)
                ) {
                    Text("Save Reminder Time", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 5. Currency Selector Bottom Sheet
    if (activeSheet == SettingsActiveSheet.CURRENCY) {
        val currencies = listOf("₹" to "Indian Rupee (INR)", "$" to "US Dollar (USD)", "€" to "Euro (EUR)", "£" to "British Pound (GBP)", "¥" to "Japanese Yen (JPY)", "AED " to "UAE Dirham (AED)")

        ModalBottomSheet(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Select Primary Currency", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Changes formatting symbol across all vaults & reports", fontSize = 12.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                currencies.forEach { (symbol, name) ->
                    val isSel = userProfile.currencySymbol == symbol
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.updateCurrencySymbol(symbol)
                                activeSheet = SettingsActiveSheet.NONE
                                Toast.makeText(context, "Currency changed to $symbol", Toast.LENGTH_SHORT).show()
                            },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) AccentPurple.copy(alpha = 0.12f) else CanvasLight,
                        border = BorderStroke(0.7.dp, if (isSel) AccentPurple else BorderLight)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("$symbol — $name", fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = TextDark)
                            if (isSel) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 6. Danger Zone Reset Confirmation Dialog
    if (activeSheet == SettingsActiveSheet.DATA_MANAGEMENT) {
        AlertDialog(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            title = { Text("Reset Entire Financial Vault?", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = SoftRed) },
            text = {
                Text(
                    "This action permanently wipes all transactions, accounts, fixed bills, and custom categories from your device storage. This cannot be undone.",
                    fontSize = 13.sp,
                    color = TextDark,
                    lineHeight = 17.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetEntireVault {
                            activeSheet = SettingsActiveSheet.NONE
                            Toast.makeText(context, "Vault reset complete", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SoftRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Wipe All Data", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { activeSheet = SettingsActiveSheet.NONE }) {
                    Text("Cancel", color = TextDark)
                }
            }
        )
    }
}

// ==========================================
// CUSTOM VECTOR ILLUSTRATIONS
// ==========================================

@Composable
private fun BiometricIllustrationCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val blobPath = Path().apply {
            moveTo(w * 0.25f, h * 0.15f)
            cubicTo(w * 0.70f, h * 0.05f, w * 0.95f, h * 0.35f, w * 0.85f, h * 0.70f)
            cubicTo(w * 0.75f, h * 0.95f, w * 0.30f, h * 0.95f, w * 0.15f, h * 0.75f)
            cubicTo(w * 0.05f, h * 0.45f, w * 0.10f, h * 0.20f, w * 0.25f, h * 0.15f)
            close()
        }
        drawPath(blobPath, color = Color(0xFFF2EFE9))

        val fpCenter = Offset(w * 0.42f, h * 0.48f)
        for (i in 1..6) {
            val rX = (i * 7.5f).dp.toPx()
            val rY = (i * 10.5f).dp.toPx()
            drawArc(
                color = Color(0xFF2C2D30),
                startAngle = -160f,
                sweepAngle = 300f,
                useCenter = false,
                topLeft = Offset(fpCenter.x - rX, fpCenter.y - rY),
                size = Size(rX * 2, rY * 2),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        val lockLeft = w * 0.52f
        val lockTop = h * 0.34f
        val lockW = 32.dp.toPx()
        val lockH = 36.dp.toPx()

        drawArc(
            color = Color(0xFF52A447),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(lockLeft + (lockW * 0.2f), lockTop - (lockH * 0.35f)),
            size = Size(lockW * 0.6f, lockH * 0.7f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        drawRoundRect(
            color = Color(0xFF67B044),
            topLeft = Offset(lockLeft, lockTop),
            size = Size(lockW, lockH),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        drawCircle(
            color = Color(0xFF1E3F18),
            radius = 3.dp.toPx(),
            center = Offset(lockLeft + (lockW / 2f), lockTop + (lockH * 0.42f))
        )
    }
}

@Composable
private fun NeoclassicalBankCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cX = w / 2f

        drawRoundRect(
            color = Color(0xFFC7EBC9),
            topLeft = Offset(w * 0.15f, h * 0.50f),
            size = Size(w * 0.35f, h * 0.35f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFFA6DDA8),
            topLeft = Offset(w * 0.52f, h * 0.50f),
            size = Size(w * 0.35f, h * 0.35f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        val bankW = 120.dp.toPx()
        val bankLeft = cX - (bankW / 2f)

        val triangle = Path().apply {
            moveTo(cX, h * 0.15f)
            lineTo(bankLeft + bankW, h * 0.35f)
            lineTo(bankLeft, h * 0.35f)
            close()
        }
        drawPath(triangle, color = Color(0xFF437A47))

        drawCircle(
            color = Color(0xFFBCE7BE),
            radius = 5.dp.toPx(),
            center = Offset(cX, h * 0.27f)
        )

        drawRect(
            color = Color(0xFF386641),
            topLeft = Offset(bankLeft, h * 0.35f),
            size = Size(bankW, 7.dp.toPx())
        )

        val columnW = 10.dp.toPx()
        val columnH = 45.dp.toPx()
        val columnTop = h * 0.35f + 7.dp.toPx()
        val colGap = (bankW - (columnW * 4)) / 3f

        for (i in 0..3) {
            val colX = bankLeft + (i * (columnW + colGap))
            drawRoundRect(
                color = Color(0xFF52A447),
                topLeft = Offset(colX, columnTop),
                size = Size(columnW, columnH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        drawRoundRect(
            color = Color(0xFF1C3A1D),
            topLeft = Offset(cX - 10.dp.toPx(), columnTop + 14.dp.toPx()),
            size = Size(20.dp.toPx(), columnH - 14.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        drawRect(
            color = Color(0xFF386641),
            topLeft = Offset(bankLeft - 8.dp.toPx(), columnTop + columnH),
            size = Size(bankW + 16.dp.toPx(), 8.dp.toPx())
        )
    }
}

// ==========================================
// REUSABLE SETTINGS ROW COMPONENTS (Image 1)
// ==========================================

@Composable
private fun SettingsSectionGroup(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String? = null,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = TextMuted,
                    lineHeight = 14.sp
                )
            }
        }

        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = BrandBlue,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF8E8E93)
            )
        )
    }
}

@Composable
private fun SettingsNavigationRow(
    title: String,
    value: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDestructive) SoftRed else TextDark
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 12.5.sp,
                color = if (isDestructive) SoftRed else TextMuted
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (isDestructive) SoftRed else BorderLight,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
