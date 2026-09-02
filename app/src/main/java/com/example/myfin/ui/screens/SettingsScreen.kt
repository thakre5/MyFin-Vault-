package com.example.myfin.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import coil.compose.SubcomposeAsyncImage
import com.example.myfin.BuildConfig
import com.example.myfin.data.ExcelExportManager
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.AppBrandingFooter
import com.example.myfin.ui.onboarding.SupportedCountries
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class SettingsActiveSheet {
    NONE,
    PERSONAL_INFO,
    VAULT_STRATEGY,
    AUTO_SWEEP_THRESHOLD,
    FORTRESS_SAFETY_NET,
    BIOMETRIC_CONFIRM,
    CHANGE_PIN,
    DAILY_REMINDER,
    COUNTRY_CURRENCY_PICKER,
    RESET_CONFIRM,
    STRATEGY,
    SECURITY,
    NOTIFICATIONS,
    CURRENCY,
    DATA_MANAGEMENT
}

enum class SettingsAccordionSection {
    NONE,
    PROFILE,
    STRATEGY,
    SECURITY,
    REMINDERS,
    BACKUP,
    REPORTS
}

private val SettingsTealColor = Color(0xFF0D9488)

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
    val avgMonthlySpend by viewModel.averageMonthlySpend.collectAsState()

    var activeSheet by rememberSaveable { mutableStateOf(initialActiveSheet) }
    var expandedSection by rememberSaveable { mutableStateOf(SettingsAccordionSection.NONE) }
    var avatarRefreshKey by remember { mutableStateOf(0L) }

    // Staging variables for permission requests
    var pendingReminderHour by remember { mutableIntStateOf(userProfile.reminderHour) }
    var pendingReminderMinute by remember { mutableIntStateOf(userProfile.reminderMinute) }

    // Sync external navigation requests from DrawerMenuContent
    LaunchedEffect(initialActiveSheet) {
        if (initialActiveSheet != SettingsActiveSheet.NONE) {
            activeSheet = initialActiveSheet
        }
    }

    val is3VaultActive = remember(userProfile.vaultMode) {
        !userProfile.vaultMode.equals("SIMPLE", ignoreCase = true)
    }

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
        uri?.let { sourceUri ->
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    context.filesDir.listFiles { file ->
                        file.name.startsWith("profile_avatar_")
                    }?.forEach { it.delete() }

                    val timestamp = System.currentTimeMillis()
                    val newFile = File(context.filesDir, "profile_avatar_$timestamp.jpg")

                    val inputStream = context.contentResolver.openInputStream(sourceUri)
                    val outputStream = FileOutputStream(newFile)
                    inputStream?.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }

                    viewModel.updateProfileImageUri(newFile.absolutePath)
                    withContext(Dispatchers.Main) {
                        avatarRefreshKey = timestamp
                        Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    viewModel.updateProfileImageUri(sourceUri.toString())
                    withContext(Dispatchers.Main) {
                        avatarRefreshKey = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.updateReminderSettings(context, true, pendingReminderHour, pendingReminderMinute)
            Toast.makeText(context, "Reminder enabled for ${String.format(Locale.US, "%02d:%02d", pendingReminderHour, pendingReminderMinute)}", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission is required for daily reminders", Toast.LENGTH_LONG).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pinned Profile Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(2f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CanvasLight)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(170.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(125.dp)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            AccentPurple,
                                            AccentPurple.copy(alpha = 0.88f),
                                            Color(0xFF6C5CE7).copy(alpha = 0.24f)
                                        )
                                    )
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .statusBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onOpenDrawer,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.22f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Drawer",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Text(
                                    text = "Edit Profile",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { activeSheet = SettingsActiveSheet.PERSONAL_INFO }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 24.dp)
                                .size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                shape = CircleShape,
                                color = AccentPurple.copy(alpha = 0.15f),
                                border = BorderStroke(3.dp, CardWhite)
                            ) {
                                val profileUri = userProfile.profileImageUri
                                val imageModel = remember(profileUri, avatarRefreshKey) {
                                    if (!profileUri.isNullOrBlank()) {
                                        File(profileUri).takeIf { it.exists() } ?: profileUri
                                    } else null
                                }

                                Box(contentAlignment = Alignment.Center) {
                                    if (imageModel != null) {
                                        SubcomposeAsyncImage(
                                            model = imageModel,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            error = {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().background(AccentPurple.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = userProfile.displayName.take(1).uppercase().ifBlank { "A" },
                                                        fontSize = 30.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = AccentPurple
                                                    )
                                                }
                                            }
                                        )
                                    } else {
                                        Text(
                                            text = userProfile.displayName.take(1).uppercase().ifBlank { "A" },
                                            fontSize = 30.sp,
                                            fontWeight = FontWeight.Black,
                                            color = AccentPurple
                                        )
                                    }
                                }
                            }

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") },
                                shape = CircleShape,
                                color = AccentPurple,
                                border = BorderStroke(1.5.dp, CardWhite)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Change photo",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 4.dp, bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .wrapContentWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    activeSheet = SettingsActiveSheet.PERSONAL_INFO
                                }
                        ) {
                            Text(
                                text = userProfile.displayName.ifBlank { "Alex Doe" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = TextDark
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userProfile.email.ifBlank { "alex.doe@example.com" },
                                fontSize = 13.sp,
                                color = TextMuted
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .align(Alignment.BottomCenter)
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

            // Accordion Sections
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Profile & Regional
                ExpandableSettingsCard(
                    icon = Icons.Default.Person,
                    title = "Profile & Regional",
                    isExpanded = expandedSection == SettingsAccordionSection.PROFILE,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == SettingsAccordionSection.PROFILE) {
                            SettingsAccordionSection.NONE
                        } else {
                            SettingsAccordionSection.PROFILE
                        }
                    }
                ) {
                    SettingsChildNavRow(
                        title = "Country & Primary Currency",
                        value = "${userProfile.currencySymbol} Currency",
                        onClick = { activeSheet = SettingsActiveSheet.COUNTRY_CURRENCY_PICKER }
                    )
                    SettingsChildNavRow(
                        title = "Personal Information",
                        value = "DOB & Inflow",
                        onClick = { activeSheet = SettingsActiveSheet.PERSONAL_INFO }
                    )
                    SettingsChildNavRow(
                        title = "Expected Monthly Salary",
                        value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", userProfile.baseMonthlyIncome)}",
                        onClick = { activeSheet = SettingsActiveSheet.PERSONAL_INFO }
                    )
                }

                // Strategy & Architecture
                val autoSweepLimit = if (userProfile.fortressThreshold > 0.0) userProfile.fortressThreshold else 25000.0
                ExpandableSettingsCard(
                    icon = Icons.Default.Layers,
                    title = "Strategy & Architecture",
                    isExpanded = expandedSection == SettingsAccordionSection.STRATEGY,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == SettingsAccordionSection.STRATEGY) {
                            SettingsAccordionSection.NONE
                        } else {
                            SettingsAccordionSection.STRATEGY
                        }
                    }
                ) {
                    SettingsChildSwitchRow(
                        title = "3-Vault Strategy Mode",
                        isChecked = is3VaultActive,
                        onToggle = { activeSheet = SettingsActiveSheet.VAULT_STRATEGY },
                        onClick = { activeSheet = SettingsActiveSheet.VAULT_STRATEGY }
                    )
                    SettingsChildNavRow(
                        title = "Auto-Sweep Operating Threshold",
                        value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", autoSweepLimit)}",
                        onClick = { activeSheet = SettingsActiveSheet.AUTO_SWEEP_THRESHOLD }
                    )
                    SettingsChildNavRow(
                        title = "Fortress Safety Net Target",
                        value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", userProfile.fortressThreshold)}",
                        onClick = { activeSheet = SettingsActiveSheet.FORTRESS_SAFETY_NET }
                    )
                    SettingsChildNavRow(
                        title = "Connected Vault Accounts",
                        value = "Manage Vaults",
                        onClick = onNavigateToVaults
                    )
                }

                // Security & Privacy
                ExpandableSettingsCard(
                    icon = Icons.Default.Lock,
                    title = "Security & Privacy",
                    isExpanded = expandedSection == SettingsAccordionSection.SECURITY,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == SettingsAccordionSection.SECURITY) {
                            SettingsAccordionSection.NONE
                        } else {
                            SettingsAccordionSection.SECURITY
                        }
                    }
                ) {
                    SettingsChildSwitchRow(
                        title = "Biometric Authentication",
                        isChecked = userProfile.isBiometricEnabled,
                        onToggle = { activeSheet = SettingsActiveSheet.BIOMETRIC_CONFIRM },
                        onClick = { activeSheet = SettingsActiveSheet.BIOMETRIC_CONFIRM }
                    )
                    SettingsChildNavRow(
                        title = "Master PIN Passcode",
                        value = "Modify PIN",
                        onClick = { activeSheet = SettingsActiveSheet.CHANGE_PIN }
                    )
                    SettingsChildSwitchRow(
                        title = "Anti-Spy Screen Protection",
                        isChecked = !userProfile.isScreenCaptureAllowed,
                        onToggle = { isAntiSpyChecked ->
                            viewModel.updateScreenCaptureAllowed(!isAntiSpyChecked)
                            Toast.makeText(context, if (isAntiSpyChecked) "Anti-spy protection enabled" else "Screen capture allowed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Reminders & Alerts
                val reminderTime = String.format(Locale.US, "%02d:%02d", userProfile.reminderHour, userProfile.reminderMinute)
                ExpandableSettingsCard(
                    icon = Icons.Outlined.Notifications,
                    title = "Reminders & Alerts",
                    isExpanded = expandedSection == SettingsAccordionSection.REMINDERS,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == SettingsAccordionSection.REMINDERS) {
                            SettingsAccordionSection.NONE
                        } else {
                            SettingsAccordionSection.REMINDERS
                        }
                    }
                ) {
                    SettingsChildSwitchRow(
                        title = "Daily Review Reminder ($reminderTime)",
                        isChecked = userProfile.reminderEnabled,
                        onToggle = { activeSheet = SettingsActiveSheet.DAILY_REMINDER },
                        onClick = { activeSheet = SettingsActiveSheet.DAILY_REMINDER }
                    )
                    SettingsChildSwitchRow(
                        title = "AutoPay Bill Due Alerts (48h)",
                        isChecked = userProfile.isAutoPayReminderEnabled,
                        onToggle = {
                            viewModel.saveUserProfile(userProfile.copy(isAutoPayReminderEnabled = !userProfile.isAutoPayReminderEnabled))
                        }
                    )
                    SettingsChildSwitchRow(
                        title = "Budget Overrun Warnings",
                        isChecked = userProfile.isOverrunWarningEnabled,
                        onToggle = {
                            viewModel.saveUserProfile(userProfile.copy(isOverrunWarningEnabled = !userProfile.isOverrunWarningEnabled))
                        }
                    )
                }

                // Data Backup & Recovery
                ExpandableSettingsCard(
                    icon = Icons.Default.Backup,
                    title = "Data Backup & Recovery",
                    isExpanded = expandedSection == SettingsAccordionSection.BACKUP,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == SettingsAccordionSection.BACKUP) {
                            SettingsAccordionSection.NONE
                        } else {
                            SettingsAccordionSection.BACKUP
                        }
                    }
                ) {
                    SettingsChildNavRow(
                        title = "Create Full Vault Snapshot (.json)",
                        value = "Export Backup",
                        onClick = {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            jsonBackupLauncher.launch("MyFin_Backup_$timeStamp.json")
                        }
                    )
                    SettingsChildNavRow(
                        title = "Restore from Encrypted Snapshot",
                        value = "Import Backup",
                        onClick = {
                            jsonRestoreLauncher.launch(arrayOf("application/json", "text/plain"))
                        }
                    )
                }

                // Financial Statements & Reports
                ExpandableSettingsCard(
                    icon = Icons.Default.TableChart,
                    title = "Financial Statements & Reports",
                    isExpanded = expandedSection == SettingsAccordionSection.REPORTS,
                    onToggleExpand = {
                        expandedSection = if (expandedSection == SettingsAccordionSection.REPORTS) {
                            SettingsAccordionSection.NONE
                        } else {
                            SettingsAccordionSection.REPORTS
                        }
                    }
                ) {
                    SettingsChildNavRow(
                        title = "Export Excel Statement (.xlsx)",
                        value = "Generate",
                        onClick = {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            xlsxExportLauncher.launch("MyFin_Statement_$timeStamp.xlsx")
                        }
                    )
                    SettingsChildNavRow(
                        title = "Export Universal Ledger (.csv)",
                        value = "Export",
                        onClick = {
                            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            csvExportLauncher.launch("MyFin_Ledger_$timeStamp.csv")
                        }
                    )
                }

                // User Guide
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(3.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToGuide() },
                    shape = RoundedCornerShape(20.dp),
                    color = CardWhite
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(AccentPurple.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.HelpOutline,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "User Guide & Documentation",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Danger Zone Wipe
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { activeSheet = SettingsActiveSheet.RESET_CONFIRM },
                    shape = RoundedCornerShape(20.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, SoftRed.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SoftRed.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    tint = SoftRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "Reset Entire Financial Vault",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SoftRed
                            )
                        }
                        Text(
                            text = "Wipe",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = SoftRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(4.dp, RoundedCornerShape(26.dp))
                        .clip(RoundedCornerShape(26.dp))
                        .clickable {
                            viewModel.lockApp()
                            Toast.makeText(context, "Vault Locked", Toast.LENGTH_SHORT).show()
                        },
                    shape = RoundedCornerShape(26.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = TextDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lock Vault",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                }

                AppBrandingFooter(
                    modifier = Modifier.fillMaxWidth(),
                    version = "v${BuildConfig.VERSION_NAME}",
                    showIcon = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Sheets & Modals
    if (activeSheet == SettingsActiveSheet.PERSONAL_INFO) {
        var nameInput by remember(userProfile) { mutableStateOf(userProfile.displayName.ifBlank { "Alex Doe" }) }
        var emailInput by remember(userProfile) { mutableStateOf(userProfile.email.ifBlank { "alex.doe@example.com" }) }
        var dobInput by remember(userProfile) { mutableStateOf(userProfile.dateOfBirth.ifBlank { "1995-01-01" }) }
        var incomeInput by remember(userProfile) {
            mutableStateOf(String.format(Locale.US, "%.0f", userProfile.baseMonthlyIncome))
        }

        ModalBottomSheet(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Personal Information", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Configure identity & base cashflow parameters", fontSize = 12.sp, color = TextMuted)

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
                    label = { Text("Date of Birth (YYYY-MM-DD or DD/MM/YYYY)") },
                    supportingText = { Text("Your DOB serves as the immutable security key for PIN resets", fontSize = 10.5.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = incomeInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        val parts = filtered.split('.')
                        incomeInput = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                    },
                    label = { Text("Expected Monthly Salary / Inflow (${userProfile.currencySymbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val parsedIncome = incomeInput.toDoubleOrNull() ?: userProfile.baseMonthlyIncome
                        val cleanDob = dobInput.trim()
                        viewModel.updateDateOfBirth(cleanDob)
                        val updated = userProfile.copy(
                            id = 1,
                            displayName = nameInput.trim(),
                            email = emailInput.trim(),
                            dateOfBirth = cleanDob,
                            baseMonthlyIncome = parsedIncome
                        )
                        viewModel.saveUserProfile(updated)
                        activeSheet = SettingsActiveSheet.NONE
                        Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (activeSheet == SettingsActiveSheet.VAULT_STRATEGY || activeSheet == SettingsActiveSheet.STRATEGY) {
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
                    color = TextDark,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (is3VaultActive) {
                        "Liquidity is segregated across Operating, Commitments, and Fortress tiers to prevent accidental overspending."
                    } else {
                        "All connected bank accounts are tracked as a single, flat liquidity balance without reserve locks."
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
                                color = AccentPurple,
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
                            tint = SettingsTealColor,
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
                    colors = ButtonDefaults.buttonColors(containerColor = TextDark)
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

    // Auto-Sweep Threshold Sheet
    if (activeSheet == SettingsActiveSheet.AUTO_SWEEP_THRESHOLD) {
        var thresholdInput by remember(userProfile) {
            val currentVal = if (userProfile.fortressThreshold > 0.0) userProfile.fortressThreshold else 25000.0
            mutableStateOf(String.format(Locale.US, "%.0f", currentVal))
        }

        ModalBottomSheet(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Auto-Sweep Operating Threshold", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Sets the liquid savings cap in your Fortress account. Any balance above this limit is automatically categorized as your Emergency Fixed Deposit.", fontSize = 12.sp, color = TextMuted, lineHeight = 16.sp)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        val parts = filtered.split('.')
                        thresholdInput = if (parts.size > 1) "${parts[0]}.${parts.drop(1).joinToString("")}" else filtered
                    },
                    label = { Text("Savings Cap Amount (${userProfile.currencySymbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val parsed = thresholdInput.toDoubleOrNull() ?: 25000.0
                        viewModel.updateFortressThreshold(parsed)
                        activeSheet = SettingsActiveSheet.NONE
                        Toast.makeText(context, "Auto-sweep threshold set to ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", parsed)}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Save Operating Threshold", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // Fortress Safety Net Target Sheet
    if (activeSheet == SettingsActiveSheet.FORTRESS_SAFETY_NET) {
        var selectedMonths by remember { mutableIntStateOf(6) }

        ModalBottomSheet(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Fortress Safety Net Target", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Macro runway calculated dynamically from your actual spending average across the year", fontSize = 12.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                val computedTarget = avgMonthlySpend * selectedMonths

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = CanvasLight,
                    border = BorderStroke(0.8.dp, BorderLight)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Dynamic Emergency Fund Target", fontSize = 11.5.sp, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", computedTarget)}",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = SettingsTealColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$selectedMonths Months × ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", avgMonthlySpend)}/mo (Avg spend)",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Select Runway Months:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(3, 6, 12).forEach { months ->
                        val isSel = selectedMonths == months
                        OutlinedButton(
                            onClick = { selectedMonths = months },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (isSel) SettingsTealColor.copy(alpha = 0.12f) else Color.Transparent
                            ),
                            border = BorderStroke(1.dp, if (isSel) SettingsTealColor else BorderLight)
                        ) {
                            Text("$months Months", fontSize = 12.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, color = if (isSel) SettingsTealColor else TextDark)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.updateFortressThreshold(computedTarget)
                        activeSheet = SettingsActiveSheet.NONE
                        Toast.makeText(context, "Fortress target set to ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", computedTarget)} ($selectedMonths Months)", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Apply as Fortress Target", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (activeSheet == SettingsActiveSheet.BIOMETRIC_CONFIRM || activeSheet == SettingsActiveSheet.SECURITY) {
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
                    text = "Biometric Authentication",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
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
                        val targetState = !userProfile.isBiometricEnabled
                        if (targetState && !viewModel.securityManager.canAuthenticateWithBiometrics(context)) {
                            Toast.makeText(context, "Biometrics not available on this device", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.updateBiometricEnabled(targetState)
                            activeSheet = SettingsActiveSheet.NONE
                            Toast.makeText(context, if (targetState) "Biometrics Enabled" else "Biometrics Disabled", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (userProfile.isBiometricEnabled) SoftRed else AccentPurple)
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

    if (activeSheet == SettingsActiveSheet.CHANGE_PIN) {
        var verifyDob by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        ModalBottomSheet(
            onDismissRequest = { activeSheet = SettingsActiveSheet.NONE },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Modify Master PIN", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Verify your recovery Date of Birth to set a new 4-digit passcode", fontSize = 12.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = verifyDob,
                    onValueChange = { verifyDob = it; errorMessage = null },
                    label = { Text("Security Key (DOB: DD/MM/YYYY or YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 6) { newPin = it.filter { ch -> ch.isDigit() }; errorMessage = null } },
                    label = { Text("New Master PIN (4-6 digits)") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) { confirmPin = it.filter { ch -> ch.isDigit() }; errorMessage = null } },
                    label = { Text("Confirm New PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage.orEmpty(), color = SoftRed, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val isDobValid = viewModel.securityManager.verifyRecoveryDob(verifyDob) ||
                                (userProfile.dateOfBirth.isNotBlank() && verifyDob.replace("[^0-9]".toRegex(), "") == userProfile.dateOfBirth.replace("[^0-9]".toRegex(), ""))

                        if (!isDobValid) {
                            errorMessage = "DOB verification failed. Please enter your correct birth date."
                        } else if (newPin.length < 4) {
                            errorMessage = "New PIN must be at least 4 digits."
                        } else if (newPin != confirmPin) {
                            errorMessage = "PIN confirmation does not match."
                        } else {
                            viewModel.saveMasterPin(newPin)
                            activeSheet = SettingsActiveSheet.NONE
                            Toast.makeText(context, "Master PIN updated successfully", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Update Master PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (activeSheet == SettingsActiveSheet.DAILY_REMINDER || activeSheet == SettingsActiveSheet.NOTIFICATIONS) {
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
                    .imePadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Daily Expense Review", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Configure scheduled offline reminder time", fontSize = 12.sp, color = TextMuted)

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
                        pendingReminderHour = hourInput
                        pendingReminderMinute = minInput
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.updateReminderSettings(context, true, hourInput, minInput)
                            Toast.makeText(context, "Reminder set for ${String.format(Locale.US, "%02d:%02d", hourInput, minInput)}", Toast.LENGTH_SHORT).show()
                        }
                        activeSheet = SettingsActiveSheet.NONE
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Save Reminder Time", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (activeSheet == SettingsActiveSheet.COUNTRY_CURRENCY_PICKER || activeSheet == SettingsActiveSheet.CURRENCY) {
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
                Text("Select Country & Currency", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Updates formatting symbol across all vaults & reports", fontSize = 12.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SupportedCountries.forEach { item ->
                        val isSel = userProfile.currencySymbol == item.currencySymbol
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateCurrencySymbol(item.currencySymbol)
                                    activeSheet = SettingsActiveSheet.NONE
                                    Toast.makeText(context, "Country set to ${item.countryName} (${item.currencySymbol})", Toast.LENGTH_SHORT).show()
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(item.flagEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(item.countryName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                        Text("${item.currencySymbol} - ${item.currencyCode}", fontSize = 11.5.sp, color = TextMuted)
                                    }
                                }
                                if (isSel) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    if (activeSheet == SettingsActiveSheet.RESET_CONFIRM || activeSheet == SettingsActiveSheet.DATA_MANAGEMENT) {
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

@Composable
private fun ExpandableSettingsCard(
    icon: ImageVector,
    title: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentPurple.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = title,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                val rotationState by animateFloatAsState(
                    targetValue = if (isExpanded) 90f else 0f,
                    label = "chevronRotation"
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(rotationState)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(
                        color = BorderLight.copy(alpha = 0.4f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsChildNavRow(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BorderLight,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@Composable
private fun SettingsChildSwitchRow(
    title: String,
    isChecked: Boolean,
    onToggle: (Boolean) -> Unit,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )

        Switch(
            checked = isChecked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = AccentPurple,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF8E8E93)
            )
        )
    }
}

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
        drawPath(blobPath, color = Color(0xFFEDE9FE))

        val fpCenter = Offset(w * 0.42f, h * 0.48f)
        for (i in 1..6) {
            val rX = (i * 7.5f).dp.toPx()
            val rY = (i * 10.5f).dp.toPx()
            drawArc(
                color = AccentPurple,
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
            color = SettingsTealColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(lockLeft + (lockW * 0.2f), lockTop - (lockH * 0.35f)),
            size = Size(lockW * 0.6f, lockH * 0.7f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )

        drawRoundRect(
            color = SettingsTealColor,
            topLeft = Offset(lockLeft, lockTop),
            size = Size(lockW, lockH),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )

        drawCircle(
            color = Color.White,
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
            color = AccentPurple.copy(alpha = 0.15f),
            topLeft = Offset(w * 0.15f, h * 0.50f),
            size = Size(w * 0.35f, h * 0.35f),
            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        )
        drawRoundRect(
            color = SettingsTealColor.copy(alpha = 0.15f),
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
        drawPath(triangle, color = AccentPurple)

        drawCircle(
            color = Color.White,
            radius = 5.dp.toPx(),
            center = Offset(cX, h * 0.27f)
        )

        drawRect(
            color = AccentPurple.copy(alpha = 0.9f),
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
                color = AccentPurple,
                topLeft = Offset(colX, columnTop),
                size = Size(columnW, columnH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        drawRoundRect(
            color = TextDark,
            topLeft = Offset(cX - 10.dp.toPx(), columnTop + 14.dp.toPx()),
            size = Size(20.dp.toPx(), columnH - 14.dp.toPx()),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        drawRect(
            color = AccentPurple.copy(alpha = 0.9f),
            topLeft = Offset(bankLeft - 8.dp.toPx(), columnTop + columnH),
            size = Size(bankW + 16.dp.toPx(), 8.dp.toPx())
        )
    }
}
