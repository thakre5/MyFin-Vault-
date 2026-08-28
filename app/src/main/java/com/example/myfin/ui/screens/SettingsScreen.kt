package com.example.myfin.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.myfin.data.ExcelExportManager
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.AppBrandingFooter
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class SettingsActiveSheet {
    NONE,
    PERSONAL_INFO,
    VAULT_STRATEGY,
    FORTRESS_THRESHOLD,
    BIOMETRIC_CONFIRM,
    CHANGE_PIN,
    DAILY_REMINDER,
    COUNTRY_CURRENCY_PICKER,
    RESET_CONFIRM,
    // Navigation Aliases
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

private val BrandGreen = Color(0xFF5BB336)
private val BrandCharcoal = Color(0xFF1C1D21)
private val CoralAccent = Color(0xFFFF6B6B)

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
    var expandedSection by rememberSaveable { mutableStateOf(SettingsAccordionSection.NONE) }
    var avatarRefreshKey by remember { mutableStateOf(0L) }

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

    // Copy selected image permanently to internal storage and trigger instant UI refresh via avatarRefreshKey
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { sourceUri ->
            try {
                val inputStream = context.contentResolver.openInputStream(sourceUri)
                val file = File(context.filesDir, "profile_avatar.jpg")
                val outputStream = FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                viewModel.updateProfileImageUri(file.absolutePath)
                avatarRefreshKey = System.currentTimeMillis()
                Toast.makeText(context, "Profile picture updated", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                viewModel.updateProfileImageUri(sourceUri.toString())
                avatarRefreshKey = System.currentTimeMillis()
            }
        }
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
        Column(modifier = Modifier.fillMaxSize()) {
            // ==========================================
            // 1. PINNED PROFILE HEADER SECTION (STATIC)
            // ==========================================
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
                    // Purple Gradient Top Horizon Banner
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
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Back / Menu",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
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

                        // 50:50 Overlapping Avatar on the Horizon
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
                                val profileBitmap = remember(profileUri, avatarRefreshKey) {
                                    if (!profileUri.isNullOrBlank()) {
                                        try {
                                            val file = File(profileUri)
                                            if (file.exists()) {
                                                BitmapFactory.decodeFile(file.absolutePath)
                                            } else {
                                                val uri = Uri.parse(profileUri)
                                                val inputStream = context.contentResolver.openInputStream(uri)
                                                BitmapFactory.decodeStream(inputStream)
                                            }
                                        } catch (e: Exception) {
                                            null
                                        }
                                    } else null
                                }

                                Box(contentAlignment = Alignment.Center) {
                                    if (profileBitmap != null) {
                                        Image(
                                            bitmap = profileBitmap.asImageBitmap(),
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
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

                    // User Identity Block (Scaped strictly to text bounds without grey indicator box)
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

                // Smooth Dissolve Fade Overlay at Bottom of Pinned Header
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

            // ==========================================
            // 2. SCROLLABLE ACCORDION CARDS CONTAINER
            // ==========================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(top = 6.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Profile & Regional Configuration
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

                // Card 2: Strategy & Vault Architecture
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
                        title = "Fortress Safety Net Target",
                        value = "${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", userProfile.fortressThreshold)}",
                        onClick = { activeSheet = SettingsActiveSheet.FORTRESS_THRESHOLD }
                    )
                    SettingsChildNavRow(
                        title = "Connected Vault Accounts",
                        value = "Manage Vaults",
                        onClick = onNavigateToVaults
                    )
                }

                // Card 3: Security & Privacy
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
                        onToggle = {
                            viewModel.updateScreenCaptureAllowed(!userProfile.isScreenCaptureAllowed)
                            Toast.makeText(context, if (userProfile.isScreenCaptureAllowed) "Screen privacy enabled" else "Screen capture allowed", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Card 4: Reminders & Alerts
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

                // Card 5: Data Backup & Recovery
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

                // Card 6: Financial Statements & Reports
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

                // Card 7: User Guide & Documentation
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

                // Card 8: Danger Zone (Reset Entire Vault)
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

                // Bottom Hero Lock Button
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

                // Shared Minimalist Branding Footer
                AppBrandingFooter(
                    modifier = Modifier.fillMaxWidth(),
                    version = "v1.0.0",
                    showIcon = true
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ==========================================
    // DEDICATED BOTTOM SHEETS & MODALS
    // ==========================================

    // 1. Edit Personal Info Sheet
    if (activeSheet == SettingsActiveSheet.PERSONAL_INFO) {
        var nameInput by remember(userProfile.displayName) { mutableStateOf(userProfile.displayName.ifBlank { "Alex Doe" }) }
        var emailInput by remember(userProfile.email) { mutableStateOf(userProfile.email.ifBlank { "alex.doe@example.com" }) }
        var dobInput by remember(userProfile.dateOfBirth) { mutableStateOf(userProfile.dateOfBirth.ifBlank { "1995-01-01" }) }
        var incomeInput by remember(userProfile.baseMonthlyIncome) {
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
                    label = { Text("Date of Birth (YYYY-MM-DD)") },
                    supportingText = { Text("Your DOB serves as the immutable security key for PIN resets", fontSize = 10.5.sp) },
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
                        val updated = userProfile.copy(
                            id = 1,
                            displayName = nameInput.trim(),
                            email = emailInput.trim(),
                            dateOfBirth = dobInput.trim(),
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

    // 2. Strategy Mode Guidance Sheet
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
                    color = BrandCharcoal,
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

    // 3. Fortress Emergency Threshold Sheet
    if (activeSheet == SettingsActiveSheet.FORTRESS_THRESHOLD) {
        var thresholdInput by remember(userProfile.fortressThreshold) {
            mutableStateOf(String.format(Locale.US, "%.0f", userProfile.fortressThreshold))
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
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Fortress Safety Net Target", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Target liquid buffer used for Auto-Sweep FD and runway pacing", fontSize = 12.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = thresholdInput,
                    onValueChange = { thresholdInput = it },
                    label = { Text("Emergency Target Amount (${userProfile.currencySymbol})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Quick Multiplier Presets:", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val base = userProfile.baseMonthlyIncome.takeIf { it > 0 } ?: 25000.0
                    listOf(3 to "3 Months", 6 to "6 Months", 12 to "12 Months").forEach { (multiplier, label) ->
                        OutlinedButton(
                            onClick = {
                                thresholdInput = String.format(Locale.US, "%.0f", base * multiplier)
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Text(label, fontSize = 11.sp, color = AccentPurple)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        val parsed = thresholdInput.toDoubleOrNull() ?: userProfile.fortressThreshold
                        viewModel.updateFortressThreshold(parsed)
                        activeSheet = SettingsActiveSheet.NONE
                        Toast.makeText(context, "Fortress target set to ${userProfile.currencySymbol}${String.format(Locale.US, "%,.0f", parsed)}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Save Fortress Target", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 4. Biometric Confirmation Sheet
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

    // 5. Change Master PIN Sheet
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
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text("Modify Master PIN", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextDark)
                Text("Verify your security DOB to set a new 4-digit passcode", fontSize = 12.sp, color = TextMuted)

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = verifyDob,
                    onValueChange = { verifyDob = it; errorMessage = null },
                    label = { Text("Security Key (DOB: YYYY-MM-DD)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) { newPin = it; errorMessage = null } },
                    label = { Text("New 4-Digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) { confirmPin = it; errorMessage = null } },
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
                        val cleanVerify = verifyDob.trim().replace("-", "").replace("/", "")
                        val cleanSaved = userProfile.dateOfBirth.trim().replace("-", "").replace("/", "")
                        if (cleanVerify != cleanSaved) {
                            errorMessage = "DOB verification failed. Please enter your correct birth date."
                        } else if (newPin.length != 4) {
                            errorMessage = "New PIN must be exactly 4 digits."
                        } else if (newPin != confirmPin) {
                            errorMessage = "PIN confirmation does not match."
                        } else {
                            viewModel.setMasterPin(newPin, userProfile.dateOfBirth)
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

    // 6. Daily Review Reminder Time Sheet
    if (activeSheet == SettingsActiveSheet.DAILY_REMINDER || activeSheet == SettingsActiveSheet.NOTIFICATIONS) {
        var hourInput by remember(userProfile.reminderHour) { mutableIntStateOf(userProfile.reminderHour) }
        var minInput by remember(userProfile.reminderMinute) { mutableIntStateOf(userProfile.reminderMinute) }

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
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                ) {
                    Text("Save Reminder Time", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }

    // 7. Country & Primary Currency Selector Sheet
    if (activeSheet == SettingsActiveSheet.COUNTRY_CURRENCY_PICKER || activeSheet == SettingsActiveSheet.CURRENCY) {
        val countryCurrencies = listOf(
            Triple("India", "₹", "INR - Indian Rupee"),
            Triple("United States", "$", "USD - US Dollar"),
            Triple("United Kingdom", "£", "GBP - British Pound"),
            Triple("European Union", "€", "EUR - Euro"),
            Triple("United Arab Emirates", "AED ", "AED - UAE Dirham"),
            Triple("Japan", "¥", "JPY - Japanese Yen"),
            Triple("Canada", "C$", "CAD - Canadian Dollar"),
            Triple("Australia", "A$", "AUD - Australian Dollar"),
            Triple("Singapore", "S$", "SGD - Singapore Dollar"),
            Triple("Switzerland", "CHF ", "CHF - Swiss Franc")
        )

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
                    countryCurrencies.forEach { (country, symbol, desc) ->
                        val isSel = userProfile.currencySymbol == symbol
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.updateCurrencySymbol(symbol)
                                    activeSheet = SettingsActiveSheet.NONE
                                    Toast.makeText(context, "Country set to $country ($symbol)", Toast.LENGTH_SHORT).show()
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
                                Column {
                                    Text(country, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                    Text(desc, fontSize = 11.5.sp, color = TextMuted)
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

    // 8. Danger Zone Confirmation Dialog
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

// ==========================================
// ACCORDION CARD & ROW COMPONENTS
// ==========================================

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

// ==========================================
// VECTOR ILLUSTRATION CANVASES
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
