@file:OptIn(ExperimentalFoundationApi::class)

package com.example.myfin.ui.onboarding

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.onboarding.steps.*
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MultiStepOnboardingFlow(
    viewModel: BudgetViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

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
            InitialAccountSetup("PRIMARY INCOME VAULT", "Operating", "50000"),
            InitialAccountSetup("BILLS & AUTOPAY VAULT", "Commitments", "25000"),
            InitialAccountSetup("CASH WALLET", "Cash", "5000")
        )
    }

    val initialCommitments = remember {
        mutableStateListOf(
            InitialCommitmentPreset("House Rent", "Utilities & Living Bills", "Rent", TransactionType.EXPENSE, 5, "15000", true),
            InitialCommitmentPreset("Mutual Fund SIP", "Investments & Wealth", "Mutual Funds", TransactionType.ASSET, 10, "10000", true),
            InitialCommitmentPreset("Electricity Bill", "Utilities & Living Bills", "Electricity", TransactionType.EXPENSE, 12, "2500", true),
            InitialCommitmentPreset("Wi-Fi & Broadband", "Utilities & Living Bills", "Internet", TransactionType.EXPENSE, 8, "999", true),
            InitialCommitmentPreset("OTT Subscriptions", "Leisure, Trips & Media", "Subscriptions", TransactionType.EXPENSE, 15, "649", false),
            InitialCommitmentPreset("Car / Personal EMI", "Debt & Financial Obligations", "Loan EMI", TransactionType.EXPENSE, 10, "8500", false)
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

        viewModel.updateProfileName(displayName.trim().ifEmpty { "Vault User" })
        viewModel.updateEmail(emailAddress.trim())
        viewModel.updateCurrency(selectedCountry.currencySymbol)
        viewModel.updateVaultMode(selectedStrategy)
        viewModel.updateScreenCaptureAllowed(false)
        if (formattedDob.isNotBlank()) {
            viewModel.updateDateOfBirth(formattedDob)
        }
        viewModel.setBiometricEnabled(isBiometricEnabled)

        viewModel.saveMasterPin(masterPin.ifEmpty { "1234" })

        initialAccounts.forEach { acc ->
            val bal = acc.initialBalanceText.toDoubleOrNull() ?: 0.0
            viewModel.addAccount(
                name = acc.name.trim().uppercase(),
                startingBalance = bal,
                type = acc.defaultType
            )
        }

        initialCommitments.filter { it.isSelected }.forEach { bill ->
            val amt = bill.amountText.toDoubleOrNull() ?: 0.0
            if (amt > 0.0) {
                viewModel.addFixedBill(
                    title = bill.title,
                    amount = amt,
                    category = bill.categoryName,
                    subcategory = bill.subcategoryName,
                    account = initialAccounts.firstOrNull { it.defaultType == "Commitments" }?.name ?: initialAccounts.first().name,
                    toAccount = null,
                    type = bill.type,
                    dueDay = bill.defaultDueDay
                )
            }
        }

        viewModel.completeOnboarding()
        onComplete()
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage == 3) {
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
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(76.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White, CyanPrimary, AccentPurple)
                                )
                            )
                            val c = center
                            val r = size.minDimension * 0.32f
                            repeat(8) { i ->
                                val angleRad = Math.toRadians((i * 45.0))
                                val px = c.x + (r * cos(angleRad)).toFloat()
                                val py = c.y + (r * sin(angleRad)).toFloat()
                                drawLine(
                                    color = Color.White,
                                    start = c,
                                    end = Offset(px, py),
                                    strokeWidth = 3.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = c)
                        }
                    }

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

        Column(modifier = Modifier.fillMaxSize()) {
            if (pagerState.currentPage > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pagerState.animateScrollToPage(
                                    page = pagerState.currentPage - 1,
                                    animationSpec = tween(450, easing = FastOutSlowInEasing)
                                )
                            }
                        },
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(CardWhite)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark, modifier = Modifier.size(18.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val activeStepIndex = pagerState.currentPage - 1
                            val isCurrent = activeStepIndex == index
                            val isPassed = activeStepIndex > index
                            val width by animateDpAsState(
                                targetValue = if (isCurrent) 22.dp else 7.dp,
                                animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow),
                                label = "stepPill"
                            )

                            Box(
                                modifier = Modifier
                                    .width(width)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isCurrent -> AccentPurple
                                            isPassed -> AccentPurple.copy(alpha = 0.5f)
                                            else -> BorderLight
                                        }
                                    )
                            )
                        }
                    }

                    Text(
                        text = "${pagerState.currentPage}/3",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
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
                        // GATEWAY: Carousel -> Identity -> Security -> Strategy
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
                                onDisplayNameChange = { displayName = it },
                                onEmailChange = { emailAddress = it },
                                onDobChange = { rawDobDigits = it },
                                onMasterPinChange = { masterPin = it },
                                onConfirmPinChange = { confirmPin = it },
                                onBiometricToggle = { isBiometricEnabled = it },
                                onCountrySelect = { selectedCountry = it },
                                onStrategySelect = { selectedStrategy = it },
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

                        // STEP 2: OPENING ACCOUNT BALANCES
                        1 -> {
                            OnboardingStep4Accounts(
                                accounts = initialAccounts,
                                currencySymbol = selectedCountry.currencySymbol,
                                onUpdateAccountBalance = { idx, newBal ->
                                    initialAccounts[idx] = initialAccounts[idx].copy(initialBalanceText = newBal)
                                },
                                onContinue = {
                                    coroutineScope.launch {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        pagerState.animateScrollToPage(
                                            page = 2,
                                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                                        )
                                    }
                                }
                            )
                        }

                        // STEP 3: FIXED COMMITMENTS BUFFER
                        2 -> {
                            OnboardingStep5Commitments(
                                commitments = initialCommitments,
                                currencySymbol = selectedCountry.currencySymbol,
                                onToggleCommitment = { idx ->
                                    initialCommitments[idx] = initialCommitments[idx].copy(isSelected = !initialCommitments[idx].isSelected)
                                },
                                onUpdateAmount = { idx, amt ->
                                    initialCommitments[idx] = initialCommitments[idx].copy(amountText = amt)
                                },
                                onContinue = {
                                    coroutineScope.launch {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        pagerState.animateScrollToPage(
                                            page = 3,
                                            animationSpec = tween(450, easing = FastOutSlowInEasing)
                                        )
                                    }
                                }
                            )
                        }

                        // STEP 4: DELIBERATION & SEALING
                        3 -> {
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
}
