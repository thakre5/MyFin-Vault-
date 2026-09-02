package com.example.myfin.ui.onboarding

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.AccountEntity
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.components.MyFinAppLogo
import com.example.myfin.ui.onboarding.steps.*
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiStepOnboardingFlow(
    viewModel: BudgetViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })

    var showSplashReveal by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000L)
        showSplashReveal = false
    }

    var displayName by remember { mutableStateOf("") }
    var emailAddress by remember { mutableStateOf("") }
    var rawDobDigits by remember { mutableStateOf("") }

    var masterPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isBiometricEnabled by remember { mutableStateOf(false) }

    var selectedCountry by remember { mutableStateOf(SupportedCountries[0]) }
    var selectedStrategy by remember { mutableStateOf("3-VAULT") }

    val initialAccounts = remember {
        mutableStateListOf(
            InitialAccountSetup("Primary Bank", "Operating", "50000", "0"),
            InitialAccountSetup("Secondary Bank", "Commitments", "25000", "10000"),
            InitialAccountSetup("Tertiary Bank", "Fortress", "5000", "0"),
            InitialAccountSetup("Cash Wallet", "Cash", "0", "0")
        )
    }

    fun syncAccountsForStrategy(strategy: String) {
        val existingBalances = initialAccounts.associate { it.name to it.initialBalanceText }
        val existingMinBalances = initialAccounts.associate { it.name to it.minBalanceText }

        val primaryBal = existingBalances["Primary Bank"] ?: "50000"
        val secondaryBal = existingBalances["Secondary Bank"] ?: "25000"
        val tertiaryBal = existingBalances["Tertiary Bank"] ?: "5000"
        val cashBal = initialAccounts.firstOrNull { it.defaultType == "Cash" }?.initialBalanceText ?: "0"

        val primaryMin = existingMinBalances["Primary Bank"] ?: "0"
        val secondaryMin = if (strategy == "3-VAULT") (existingMinBalances["Secondary Bank"] ?: "10000") else "0"
        val tertiaryMin = existingMinBalances["Tertiary Bank"] ?: "0"

        initialAccounts.clear()
        if (strategy == "3-VAULT") {
            initialAccounts.add(InitialAccountSetup("Primary Bank", "Operating", primaryBal, primaryMin))
            initialAccounts.add(InitialAccountSetup("Secondary Bank", "Commitments", secondaryBal, secondaryMin))
            initialAccounts.add(InitialAccountSetup("Tertiary Bank", "Fortress", tertiaryBal, tertiaryMin))
            initialAccounts.add(InitialAccountSetup("Cash Wallet", "Cash", cashBal, "0"))
        } else {
            initialAccounts.add(InitialAccountSetup("Primary Bank", "Operating", primaryBal, primaryMin))
            initialAccounts.add(InitialAccountSetup("Secondary Bank", "Operating", secondaryBal, "0"))
            initialAccounts.add(InitialAccountSetup("Tertiary Bank", "Operating", tertiaryBal, "0"))
            initialAccounts.add(InitialAccountSetup("Cash Wallet", "Cash", cashBal, "0"))
        }
    }

    // 5 Mapped AutoPay Commitments
    val initialCommitments = remember {
        mutableStateListOf(
            InitialCommitmentPreset("House / PG Rent", "Utilities & Living Bills", "PG Rent", TransactionType.EXPENSE, 5, "15000", true),
            InitialCommitmentPreset("Debt / Loan EMI", "Debt & Financial Obligations", "Credit Cards & EMI", TransactionType.EXPENSE, 10, "8500", true),
            InitialCommitmentPreset("Mutual Fund SIP", "Investments & Wealth", "Mutual Funds (MF)", TransactionType.ASSET, 10, "10000", true),
            InitialCommitmentPreset("Emergency Reserve", "Liquid Reserves & Receivables", "Emergency Fund", TransactionType.ASSET, 1, "5000", true),
            InitialCommitmentPreset("Monthly Salary", "Salary & Professional Inflow", "Base Salary (Pay Slip)", TransactionType.INCOME, 1, "75000", true)
        )
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.restoreVaultFromUri(context, it) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                if (success) {
                    onComplete()
                }
            }
        }
    }

    var remainingSeconds by remember { mutableIntStateOf(10) }
    var hasSealedAndLaunched by remember { mutableStateOf(false) }

    fun finalizeAndLaunchVault() {
        if (hasSealedAndLaunched) return
        hasSealedAndLaunched = true

        val formattedDob = if (rawDobDigits.length == 8) {
            "${rawDobDigits.substring(0, 2)}/${rawDobDigits.substring(2, 4)}/${rawDobDigits.substring(4, 8)}"
        } else ""

        // 1. Atomic Profile & Security Persistence
        viewModel.finalizeOnboardingProfile(
            displayName = displayName,
            email = emailAddress,
            dob = formattedDob,
            currencySymbol = selectedCountry.currencySymbol,
            vaultMode = selectedStrategy,
            masterPin = masterPin.ifEmpty { "1234" },
            isBiometricEnabled = isBiometricEnabled
        )

        // Sync Monthly Base Salary to profile
        val salaryCommitment = initialCommitments.find { it.type == TransactionType.INCOME && it.isSelected }
        val parsedSalary = salaryCommitment?.amountText?.toDoubleOrNull() ?: 0.0
        if (parsedSalary > 0.0) {
            val currentProfile = viewModel.userProfile.value
            viewModel.saveUserProfile(currentProfile.copy(baseMonthlyIncome = parsedSalary))
        }

        // 2. Set Up Accounts with User-Defined MAB Floored Values
        val accountEntities = initialAccounts.mapIndexed { index, acc ->
            val parsedMin = acc.minBalanceText.toDoubleOrNull() ?: 0.0
            AccountEntity(
                accountName = acc.name.trim().uppercase(),
                startingBalance = acc.initialBalanceText.toDoubleOrNull() ?: 0.0,
                accountType = acc.defaultType,
                minBalance = parsedMin,
                sortOrder = index
            )
        }
        viewModel.replaceAllAccounts(accountEntities)

        // 3. Set Up Commitments
        val commitmentsAccountName = initialAccounts.firstOrNull { it.defaultType == "Commitments" }?.name?.trim()?.uppercase()
            ?: initialAccounts.first().name.trim().uppercase()
        val operatingAccountName = initialAccounts.firstOrNull { it.defaultType == "Operating" }?.name?.trim()?.uppercase()
            ?: initialAccounts.first().name.trim().uppercase()

        initialCommitments.filter { it.isSelected }.forEach { bill ->
            val amt = bill.amountText.toDoubleOrNull() ?: 0.0
            if (amt > 0.0) {
                viewModel.addFixedBill(
                    title = bill.title,
                    amount = amt,
                    category = bill.categoryName,
                    subcategory = bill.subcategoryName,
                    account = if (bill.type == TransactionType.INCOME) operatingAccountName else commitmentsAccountName,
                    toAccount = null,
                    type = bill.type,
                    dueDay = bill.defaultDueDay
                )
            }
        }

        onComplete()
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 1) {
            remainingSeconds = 10
            while (remainingSeconds > 0) {
                delay(1000L)
                remainingSeconds -= 1
            }
            finalizeAndLaunchVault()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasLight)
    ) {
        AnimatedVisibility(
            visible = showSplashReveal,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(500, easing = FastOutSlowInEasing)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(10f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF4C1D95),
                                AccentPurple,
                                PurplePrimary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    MyFinAppLogo(
                        size = 80.dp,
                        showBackgroundContainer = true,
                        elevation = 8.dp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "MyFin",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.8).sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your Wealth. Your Rules. Zero Cloud.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.2.sp
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).absoluteValue
            val clampedOffset = pageOffset.coerceIn(0f, 1f)
            val scale = 1f - (0.06f * clampedOffset)
            val alpha = 1f - (0.4f * clampedOffset)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
            ) {
                when (page) {
                    0 -> {
                        OnboardingStep0WelcomeGateway(
                            displayName = displayName,
                            emailAddress = emailAddress,
                            rawDobDigits = rawDobDigits,
                            masterPin = masterPin,
                            confirmPin = confirmPin,
                            isBiometricEnabled = isBiometricEnabled,
                            selectedCountry = selectedCountry,
                            selectedStrategy = selectedStrategy,
                            accounts = initialAccounts,
                            commitments = initialCommitments,
                            onDisplayNameChange = { displayName = it },
                            onEmailChange = { emailAddress = it },
                            onDobChange = { rawDobDigits = it },
                            onMasterPinChange = { masterPin = it },
                            onConfirmPinChange = { confirmPin = it },
                            onBiometricToggle = { isBiometricEnabled = it },
                            onCountrySelect = { selectedCountry = it },
                            onStrategySelect = { newStrategy ->
                                selectedStrategy = newStrategy
                                syncAccountsForStrategy(newStrategy)
                            },
                            onUpdateAccountBalance = { idx, newBal ->
                                initialAccounts[idx] = initialAccounts[idx].copy(initialBalanceText = newBal)
                            },
                            onUpdateAccountMinBalance = { idx, newMin ->
                                initialAccounts[idx] = initialAccounts[idx].copy(minBalanceText = newMin)
                            },
                            onRemoveAccount = { idx ->
                                initialAccounts.removeAt(idx)
                            },
                            onAddAccount = {
                                val existingNames = initialAccounts.map { it.name }
                                val nextBankName = when {
                                    "Secondary Bank" !in existingNames -> "Secondary Bank"
                                    "Tertiary Bank" !in existingNames -> "Tertiary Bank"
                                    else -> "Bank ${existingNames.count { it != "Cash Wallet" } + 1}"
                                }
                                val cashAcc = initialAccounts.firstOrNull { it.defaultType == "Cash" }
                                if (cashAcc != null) {
                                    val insertIdx = initialAccounts.indexOf(cashAcc)
                                    initialAccounts.add(insertIdx, InitialAccountSetup(nextBankName, "Operating", "10000", "0"))
                                } else {
                                    initialAccounts.add(InitialAccountSetup(nextBankName, "Operating", "10000", "0"))
                                }
                            },
                            onToggleCommitment = { idx ->
                                initialCommitments[idx] = initialCommitments[idx].copy(isSelected = !initialCommitments[idx].isSelected)
                            },
                            onUpdateCommitmentAmount = { idx, amt ->
                                initialCommitments[idx] = initialCommitments[idx].copy(amountText = amt)
                            },
                            onProceedToNextStep = {
                                coroutineScope.launch {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pagerState.animateScrollToPage(
                                        page = 1,
                                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                                    )
                                }
                            },
                            onRestoreVault = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                    }

                    1 -> {
                        OnboardingStep6VaultSealing(
                            displayName = displayName,
                            emailAddress = emailAddress,
                            profileImageUri = null,
                            country = selectedCountry,
                            strategy = selectedStrategy,
                            totalLiquidity = initialAccounts.sumOf { it.initialBalanceText.toDoubleOrNull() ?: 0.0 },
                            totalCommitments = initialCommitments.filter { it.isSelected }.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 },
                            remainingSeconds = remainingSeconds,
                            onSealImmediately = { finalizeAndLaunchVault() }
                        )
                    }
                }
            }
        }
    }
}
