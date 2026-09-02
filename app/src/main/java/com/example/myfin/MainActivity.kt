package com.example.myfin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.myfin.data.AppDatabase
import com.example.myfin.data.ReminderScheduler
import com.example.myfin.data.SecurityManager
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.*
import com.example.myfin.ui.onboarding.MultiStepOnboardingFlow
import com.example.myfin.ui.screens.*
import com.example.myfin.ui.theme.MyfinTheme

class MainActivity : FragmentActivity() {

    private val securityManager by lazy { SecurityManager(applicationContext) }
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }

    private val viewModel: BudgetViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return BudgetViewModel(database.budgetDao(), securityManager) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ReminderScheduler.createNotificationChannels(applicationContext)

        // Session Auto-Lock Lifecycle Observer (60-second timeout)
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // Avoid recording background time on screen rotation or configuration changes
                    if (!isChangingConfigurations) {
                        securityManager.recordAppBackgrounded()
                    }
                }
                Lifecycle.Event.ON_START -> {
                    if (securityManager.shouldLockOnResume()) {
                        viewModel.lockApp()
                    }
                    securityManager.clearSessionLock()
                }
                else -> {}
            }
        })

        setContent {
            MyfinTheme {
                val userProfile by viewModel.userProfile.collectAsState()
                val isUnlocked by viewModel.isAppUnlocked.collectAsState()

                val isFirstLaunch = !userProfile.isOnboardingCompleted

                // Enforce FLAG_SECURE whenever app is locked (!isUnlocked)
                // Screen capture is only permitted when actively unlocked AND explicitly allowed in settings
                LaunchedEffect(userProfile.isScreenCaptureAllowed, isFirstLaunch, isUnlocked) {
                    val allowCapture = isFirstLaunch || (isUnlocked && userProfile.isScreenCaptureAllowed)
                    if (allowCapture) {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.setFlags(
                            WindowManager.LayoutParams.FLAG_SECURE,
                            WindowManager.LayoutParams.FLAG_SECURE
                        )
                    }
                }

                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { }

                // Request notification permission contextually once onboarding is completed
                LaunchedEffect(isFirstLaunch, userProfile.reminderEnabled, userProfile.isAutoPayReminderEnabled) {
                    if (!isFirstLaunch && (userProfile.reminderEnabled || userProfile.isAutoPayReminderEnabled)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED

                            if (!hasPermission) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                }

                var isDrawerOpen by rememberSaveable { mutableStateOf(false) }
                var currentTarget by rememberSaveable { mutableStateOf(NavigationTarget.MONTHLY_VIEW) }
                var settingsInitialSheet by rememberSaveable { mutableStateOf(SettingsActiveSheet.NONE) }

                when {
                    isFirstLaunch -> {
                        MultiStepOnboardingFlow(
                            viewModel = viewModel,
                            onComplete = {
                                viewModel.unlockApp()
                            }
                        )
                    }

                    !isUnlocked -> {
                        BackHandler(enabled = true) {
                            moveTaskToBack(true)
                        }

                        PinLockScreen(
                            profileName = userProfile.displayName,
                            profileImageUri = userProfile.profileImageUri,
                            recoveryDob = userProfile.dateOfBirth,
                            isBiometricEnabled = userProfile.isBiometricEnabled,
                            onUnlockSuccess = { viewModel.unlockApp() },
                            onResetPasswordOnly = { newPin ->
                                viewModel.saveMasterPin(newPin)
                            }
                        )
                    }

                    else -> {
                        BackHandler(enabled = true) {
                            if (isDrawerOpen) {
                                isDrawerOpen = false
                            } else if (currentTarget == NavigationTarget.USER_GUIDE) {
                                currentTarget = NavigationTarget.SETTINGS
                            } else if (currentTarget != NavigationTarget.MONTHLY_VIEW) {
                                settingsInitialSheet = SettingsActiveSheet.NONE
                                currentTarget = NavigationTarget.MONTHLY_VIEW
                            } else {
                                moveTaskToBack(true)
                            }
                        }

                        PerspectiveDrawer(
                            isDrawerOpen = isDrawerOpen,
                            onCloseDrawer = { isDrawerOpen = false },
                            drawerContent = {
                                DrawerMenuContent(
                                    displayName = userProfile.displayName,
                                    profileImageUri = userProfile.profileImageUri,
                                    onUpdateProfileImageUri = { newUri ->
                                        viewModel.updateProfileImageUri(newUri)
                                    },
                                    currentSelection = currentTarget,
                                    onSelectTarget = { target ->
                                        settingsInitialSheet = SettingsActiveSheet.NONE
                                        currentTarget = target
                                        isDrawerOpen = false
                                    },
                                    onEditProfile = {
                                        settingsInitialSheet = SettingsActiveSheet.PERSONAL_INFO
                                        currentTarget = NavigationTarget.SETTINGS
                                        isDrawerOpen = false
                                    },
                                    onLockApp = {
                                        viewModel.lockApp()
                                        isDrawerOpen = false
                                    }
                                )
                            },
                            mainContent = {
                                when (currentTarget) {
                                    NavigationTarget.MONTHLY_VIEW -> {
                                        MonthlyScreen(
                                            viewModel = viewModel,
                                            onOpenDrawer = { isDrawerOpen = true },
                                            onNavigateToPlanner = { currentTarget = NavigationTarget.BUDGET_PLANNER },
                                            onNavigateToTaxonomy = { currentTarget = NavigationTarget.DATA_SET },
                                            onNavigateToVaults = { currentTarget = NavigationTarget.VAULT_ACCOUNTS },
                                            onNavigateToAnalytics = { currentTarget = NavigationTarget.REPORTS_ANALYTICS }
                                        )
                                    }
                                    NavigationTarget.BUDGET_PLANNER -> {
                                        BudgetPlannerScreen(
                                            viewModel = viewModel,
                                            onOpenDrawer = { isDrawerOpen = true },
                                            onNavigateToTaxonomy = { currentTarget = NavigationTarget.DATA_SET },
                                            onNavigateToMonthly = { currentTarget = NavigationTarget.MONTHLY_VIEW },
                                            onNavigateToYearly = { currentTarget = NavigationTarget.YEARLY_VIEW },
                                            onNavigateToVaults = { currentTarget = NavigationTarget.VAULT_ACCOUNTS },
                                            onNavigateToAnalytics = { currentTarget = NavigationTarget.REPORTS_ANALYTICS }
                                        )
                                    }
                                    NavigationTarget.DATA_SET -> {
                                        MasterDataSetScreen(
                                            viewModel = viewModel,
                                            onOpenDrawer = { isDrawerOpen = true },
                                            onNavigateToPlanner = { currentTarget = NavigationTarget.BUDGET_PLANNER },
                                            onNavigateToVaults = { currentTarget = NavigationTarget.VAULT_ACCOUNTS },
                                            onNavigateToMonthly = { currentTarget = NavigationTarget.MONTHLY_VIEW },
                                            onNavigateToAnalytics = { currentTarget = NavigationTarget.REPORTS_ANALYTICS }
                                        )
                                    }
                                    NavigationTarget.YEARLY_VIEW -> {
                                        YearlyScreen(
                                            viewModel = viewModel,
                                            onOpenDrawer = { isDrawerOpen = true },
                                            onNavigateToMonth = { _, _ ->
                                                currentTarget = NavigationTarget.MONTHLY_VIEW
                                            },
                                            onNavigateToDashboard = { currentTarget = NavigationTarget.MONTHLY_VIEW },
                                            onNavigateToPlanner = { currentTarget = NavigationTarget.BUDGET_PLANNER },
                                            onNavigateToTaxonomy = { currentTarget = NavigationTarget.DATA_SET },
                                            onNavigateToVaults = { currentTarget = NavigationTarget.VAULT_ACCOUNTS },
                                            onNavigateToAnalytics = { currentTarget = NavigationTarget.REPORTS_ANALYTICS }
                                        )
                                    }
                                    NavigationTarget.VAULT_ACCOUNTS -> {
                                        if (userProfile.vaultMode.equals("SIMPLE", ignoreCase = true)) {
                                            SimpleAccountsScreen(
                                                viewModel = viewModel,
                                                onOpenDrawer = { isDrawerOpen = true },
                                                onNavigateToDashboard = { currentTarget = NavigationTarget.MONTHLY_VIEW },
                                                onNavigateToPlanner = { currentTarget = NavigationTarget.BUDGET_PLANNER },
                                                onNavigateToTaxonomy = { currentTarget = NavigationTarget.DATA_SET },
                                                onNavigateToVaultAnalytics = { currentTarget = NavigationTarget.REPORTS_ANALYTICS },
                                                onNavigateToVaultSettings = { currentTarget = NavigationTarget.SETTINGS }
                                            )
                                        } else {
                                            VaultStrategyScreen(
                                                viewModel = viewModel,
                                                onOpenDrawer = { isDrawerOpen = true },
                                                onNavigateToDashboard = { currentTarget = NavigationTarget.MONTHLY_VIEW },
                                                onNavigateToPlanner = { currentTarget = NavigationTarget.BUDGET_PLANNER },
                                                onNavigateToTaxonomy = { currentTarget = NavigationTarget.DATA_SET },
                                                onNavigateToVaultAnalytics = { currentTarget = NavigationTarget.REPORTS_ANALYTICS },
                                                onNavigateToVaultSettings = { currentTarget = NavigationTarget.SETTINGS }
                                            )
                                        }
                                    }
                                    NavigationTarget.REPORTS_ANALYTICS -> {
                                        ReportsAnalyticsScreen(
                                            viewModel = viewModel,
                                            onOpenDrawer = { isDrawerOpen = true },
                                            onNavigateToDashboard = { currentTarget = NavigationTarget.MONTHLY_VIEW },
                                            onNavigateToTaxonomy = { currentTarget = NavigationTarget.DATA_SET },
                                            onNavigateToPlanner = { currentTarget = NavigationTarget.BUDGET_PLANNER },
                                            onNavigateToVaults = { currentTarget = NavigationTarget.VAULT_ACCOUNTS },
                                            onNavigateToSettings = { currentTarget = NavigationTarget.SETTINGS }
                                        )
                                    }
                                    NavigationTarget.SETTINGS -> {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            initialActiveSheet = settingsInitialSheet,
                                            onOpenDrawer = { isDrawerOpen = true },
                                            onNavigateToGuide = { currentTarget = NavigationTarget.USER_GUIDE },
                                            onNavigateToVaults = { currentTarget = NavigationTarget.VAULT_ACCOUNTS }
                                        )
                                    }
                                    NavigationTarget.USER_GUIDE -> {
                                        UserGuideScreen(
                                            onBack = { currentTarget = NavigationTarget.SETTINGS }
                                        )
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
