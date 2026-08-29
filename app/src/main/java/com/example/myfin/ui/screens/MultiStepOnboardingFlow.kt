@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.myfin.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.data.TransactionType
import com.example.myfin.ui.BudgetViewModel
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.BorderLight
import com.example.myfin.ui.theme.CanvasLight
import com.example.myfin.ui.theme.CardWhite
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val CyanPrimary = Color(0xFF00D2EE)
private val PurplePrimary = Color(0xFF6C5CE7)
private val TealPrimary = Color(0xFF10B981)
private val CoralAccent = Color(0xFFFF6B6B)

data class CountryCurrencyMapping(
    val countryName: String,
    val flagEmoji: String,
    val currencySymbol: String,
    val currencyCode: String
)

val SupportedCountries = listOf(
    CountryCurrencyMapping("India", "🇮🇳", "₹", "INR"),
    CountryCurrencyMapping("United States", "🇺🇸", "$", "USD"),
    CountryCurrencyMapping("United Kingdom", "🇬🇧", "£", "GBP"),
    CountryCurrencyMapping("Eurozone", "🇪🇺", "€", "EUR"),
    CountryCurrencyMapping("United Arab Emirates", "🇦🇪", "د.إ", "AED"),
    CountryCurrencyMapping("Singapore", "🇸🇬", "$", "SGD"),
    CountryCurrencyMapping("Australia", "🇦🇺", "$", "AUD"),
    CountryCurrencyMapping("Canada", "🇨🇦", "$", "CAD"),
    CountryCurrencyMapping("Japan", "🇯🇵", "¥", "JPY"),
    CountryCurrencyMapping("Saudi Arabia", "🇸🇦", "﷼", "SAR")
)

data class InitialAccountSetup(
    val name: String,
    val defaultType: String,
    val initialBalanceText: String
)

data class InitialCommitmentPreset(
    val title: String,
    val categoryName: String,
    val subcategoryName: String,
    val type: TransactionType,
    val defaultDueDay: Int,
    val amountText: String,
    val isSelected: Boolean
)

@Composable
fun MultiStepOnboardingFlow(
    viewModel: BudgetViewModel,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 7 })

    var profileImageUri by remember { mutableStateOf<Uri?>(null) }
    var displayName by remember { mutableStateOf("Jordan Lee") }
    var emailAddress by remember { mutableStateOf("jordan.vault@myfin.app") }

    var masterPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var rawDobDigits by remember { mutableStateOf("") }
    var isBiometricEnabled by remember { mutableStateOf(true) }
    var pinEntryPhase by remember { mutableIntStateOf(1) }

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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            profileImageUri = uri
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
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

        viewModel.updateProfileName(displayName.trim().ifEmpty { "Jordan Lee" })
        viewModel.updateEmail(emailAddress.trim().ifEmpty { "jordan.vault@myfin.app" })
        viewModel.updateCurrency(selectedCountry.currencySymbol)
        viewModel.updateVaultMode(selectedStrategy)
        viewModel.updateScreenCaptureAllowed(false)
        profileImageUri?.let { viewModel.updateProfileImageUri(it.toString()) }
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
        if (pagerState.currentPage == 6) {
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
                    if (pagerState.currentPage < 6) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (pagerState.currentPage == 2 && pinEntryPhase == 2) {
                                        pinEntryPhase = 1
                                        confirmPin = ""
                                    } else {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(CardWhite)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextDark, modifier = Modifier.size(18.dp))
                        }
                    } else {
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(6) { index ->
                            val activeStepIndex = pagerState.currentPage - 1
                            val isCurrent = activeStepIndex == index
                            val isPassed = activeStepIndex > index
                            val width by animateDpAsState(if (isCurrent) 22.dp else 7.dp, label = "stepPill")

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
                        text = "${pagerState.currentPage}/6",
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
                when (page) {
                    0 -> {
                        OnboardingStep0WelcomeGateway(
                            currencySymbol = selectedCountry.currencySymbol,
                            onGetStarted = {
                                coroutineScope.launch {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    pagerState.animateScrollToPage(1)
                                }
                            },
                            onRestoreVault = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                restoreBackupLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                    }
                    1 -> {
                        OnboardingStep1Identity(
                            profileImageUri = profileImageUri,
                            displayName = displayName,
                            emailAddress = emailAddress,
                            onPickPhoto = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            onDisplayNameChange = { displayName = it },
                            onEmailChange = { emailAddress = it },
                            onContinue = {
                                if (displayName.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter your display name", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch { pagerState.animateScrollToPage(2) }
                                }
                            }
                        )
                    }
                    2 -> {
                        OnboardingStep2PinSecurity(
                            pinEntryPhase = pinEntryPhase,
                            masterPin = masterPin,
                            confirmPin = confirmPin,
                            rawDobDigits = rawDobDigits,
                            isBiometricEnabled = isBiometricEnabled,
                            onDigitPress = { digit ->
                                if (pinEntryPhase == 1) {
                                    if (masterPin.length < 4) masterPin += digit
                                } else {
                                    if (confirmPin.length < 4) confirmPin += digit
                                }
                            },
                            onDeleteDigit = {
                                if (pinEntryPhase == 1) {
                                    if (masterPin.isNotEmpty()) masterPin = masterPin.dropLast(1)
                                } else {
                                    if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                                }
                            },
                            onDobChange = { rawDobDigits = it.filter { ch -> ch.isDigit() }.take(8) },
                            onBiometricToggle = { isBiometricEnabled = it },
                            onProceedToConfirm = {
                                if (masterPin.length == 4) {
                                    pinEntryPhase = 2
                                } else {
                                    Toast.makeText(context, "Enter a 4-digit Master PIN", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onValidateAndNext = {
                                if (confirmPin != masterPin) {
                                    Toast.makeText(context, "PINs do not match. Please re-enter.", Toast.LENGTH_SHORT).show()
                                    confirmPin = ""
                                } else if (rawDobDigits.length < 8) {
                                    Toast.makeText(context, "Enter valid 8-digit DOB (DDMMYYYY) for recovery", Toast.LENGTH_SHORT).show()
                                } else {
                                    coroutineScope.launch { pagerState.animateScrollToPage(3) }
                                }
                            }
                        )
                    }
                    3 -> {
                        OnboardingStep3CountryStrategy(
                            selectedCountry = selectedCountry,
                            selectedStrategy = selectedStrategy,
                            onSelectCountry = { selectedCountry = it },
                            onSelectStrategy = { selectedStrategy = it },
                            onContinue = {
                                coroutineScope.launch { pagerState.animateScrollToPage(4) }
                            }
                        )
                    }
                    4 -> {
                        OnboardingStep4Accounts(
                            accounts = initialAccounts,
                            currencySymbol = selectedCountry.currencySymbol,
                            onUpdateAccountBalance = { idx, newBal ->
                                initialAccounts[idx] = initialAccounts[idx].copy(initialBalanceText = newBal)
                            },
                            onContinue = {
                                coroutineScope.launch { pagerState.animateScrollToPage(5) }
                            }
                        )
                    }
                    5 -> {
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
                                coroutineScope.launch { pagerState.animateScrollToPage(6) }
                            }
                        )
                    }
                    6 -> {
                        OnboardingStep6VaultSealing(
                            displayName = displayName,
                            emailAddress = emailAddress,
                            profileImageUri = profileImageUri,
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

// -------------------------------------------------------------
// STEP 0: 1:1 SOLNEX-STYLE WELCOME GATEWAY
// -------------------------------------------------------------
data class OnboardingCarouselSlide(
    val title: String,
    val subtitle: String
)

val WelcomeCarouselSlides = listOf(
    OnboardingCarouselSlide(
        title = "The Card That\nWorks Anywhere",
        subtitle = "Create your account to trade, store,\nand grow your digital assets securely"
    ),
    OnboardingCarouselSlide(
        title = "Zero-Knowledge\nLocal Security",
        subtitle = "Your financial records stay 100% on your device\nwith hardware-backed biometric encryption"
    ),
    OnboardingCarouselSlide(
        title = "Smart 3-Tier\nWealth Strategy",
        subtitle = "Automate your capital between daily spending,\ncommitted bills, and emergency fortress"
    )
)

@Composable
private fun OnboardingStep0WelcomeGateway(
    currencySymbol: String,
    onGetStarted: () -> Unit,
    onRestoreVault: () -> Unit
) {
    val carouselPagerState = rememberPagerState(pageCount = { WelcomeCarouselSlides.size })

    LaunchedEffect(Unit) {
        while (true) {
            delay(3600L)
            carouselPagerState.animateScrollToPage(
                page = (carouselPagerState.currentPage + 1) % WelcomeCarouselSlides.size,
                animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFDE8F4),
                        Color(0xFFFBE6F2),
                        Color(0xFFF6EEFB),
                        Color(0xFFFBFBFD),
                        CanvasLight
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.size(36.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFF472B6), Color(0xFFE11D48), Color(0xFFC026D3))
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
                                strokeWidth = 2.4.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                        drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = c)
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Solnex",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    letterSpacing = (-0.4).sp
                )
            }

            Spacer(modifier = Modifier.weight(0.4f))

            SolnexTiltedCardsHero(currencySymbol = currencySymbol)

            Spacer(modifier = Modifier.weight(0.5f))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalPager(
                    state = carouselPagerState,
                    userScrollEnabled = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                ) { page ->
                    val slide = WelcomeCarouselSlides[page]
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = slide.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            textAlign = TextAlign.Center,
                            lineHeight = 34.sp,
                            letterSpacing = (-0.6).sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = slide.subtitle,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(WelcomeCarouselSlides.size) { idx ->
                        val isSelected = carouselPagerState.currentPage == idx
                        val width by animateDpAsState(if (isSelected) 18.dp else 5.dp, label = "dotWidth")
                        Box(
                            modifier = Modifier
                                .width(width)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) TextDark else BorderLight)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onGetStarted,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                ) {
                    Text(
                        text = "Get Started",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = Color.White
                    )
                }

                OutlinedButton(
                    onClick = onRestoreVault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite)
                ) {
                    Text(
                        text = "Restore Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.5.sp,
                        color = TextDark
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1:1 TILTED CARD STACK & 3D SILVER COIN MEDALLIONS
// -------------------------------------------------------------
@Composable
private fun SolnexTiltedCardsHero(currencySymbol: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(235.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(225.dp)
                .height(140.dp)
                .graphicsLayer {
                    rotationZ = -22f
                    translationX = -32f
                    translationY = -10f
                }
                .shadow(14.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFC084FC))
                        )
                    )
                    .padding(14.dp)
            ) {
                Text("✦", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text("Balance", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                    Text("$currencySymbol 2,597.12", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Surface(
            modifier = Modifier
                .width(245.dp)
                .height(152.dp)
                .graphicsLayer {
                    rotationZ = -11f
                    translationX = 14f
                    translationY = 16f
                }
                .shadow(22.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6B052B),
                                Color(0xFF9D174D),
                                Color(0xFFC026D3),
                                Color(0xFFE11D48),
                                Color(0xFFF43F5E)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Text("✦", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)

                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text("Balance", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    Text("$currencySymbol 24,597.36", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .size(62.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-14).dp, y = (-42).dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFFCBD5E1), radius = size.minDimension * 0.44f, style = Stroke(width = 2.dp.toPx()))
            }
            Text("€", fontSize = 23.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-46).dp, y = 6.dp)
                .shadow(14.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFF64748B))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFFCBD5E1), radius = size.minDimension * 0.44f, style = Stroke(width = 2.5.dp.toPx()))
            }
            Text("₿", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF475569))
        }
    }
}

// -------------------------------------------------------------
// STEP 1: IDENTITY & PROFILE
// -------------------------------------------------------------
@Composable
private fun OnboardingStep1Identity(
    profileImageUri: Uri?,
    displayName: String,
    emailAddress: String,
    onPickPhoto: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    val context = LocalContext.current
    val avatarBitmap = rememberImageBitmapFromUri(context, profileImageUri)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        item {
            OrbitalVaultParticlesCanvas(
                modifier = Modifier
                    .size(110.dp)
                    .padding(bottom = 6.dp)
            )

            Text(
                text = "Own Your Wealth Architecture",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextDark,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "100% offline, decentralized, zero-knowledge financial operating system.",
                fontSize = 12.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(22.dp))

            Surface(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(4.dp, CircleShape)
                    .clickable(onClick = onPickPhoto),
                shape = CircleShape,
                color = CardWhite,
                border = BorderStroke(2.dp, AccentPurple.copy(alpha = 0.55f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap,
                            contentDescription = "Profile Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(PurplePrimary.copy(alpha = 0.22f), CyanPrimary.copy(alpha = 0.22f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = displayName.take(1).uppercase().ifEmpty { "J" },
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentPurple
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(28.dp),
                        shape = CircleShape,
                        color = AccentPurple,
                        shadowElevation = 3.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Upload Photo",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            OutlinedTextField(
                value = displayName,
                onValueChange = onDisplayNameChange,
                label = { Text("Display Name", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = AccentPurple) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = emailAddress,
                onValueChange = onEmailChange,
                label = { Text("Email Address (Optional)", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Outlined.Mail, contentDescription = null, tint = TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Used solely for local PDF/Excel statement headers. Never sent to cloud servers.",
                fontSize = 10.5.sp,
                color = TextMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )

            Spacer(modifier = Modifier.height(26.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Continue Setup", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 2: MASTER PIN & RECOVERY DOB
// -------------------------------------------------------------
@Composable
private fun OnboardingStep2PinSecurity(
    pinEntryPhase: Int,
    masterPin: String,
    confirmPin: String,
    rawDobDigits: String,
    isBiometricEnabled: Boolean,
    onDigitPress: (String) -> Unit,
    onDeleteDigit: () -> Unit,
    onDobChange: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onProceedToConfirm: () -> Unit,
    onValidateAndNext: () -> Unit
) {
    val currentPinString = if (pinEntryPhase == 1) masterPin else confirmPin
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SecurityRadarPulseCanvas(
            modifier = Modifier
                .size(68.dp)
                .padding(bottom = 6.dp)
        )

        Text(
            text = if (pinEntryPhase == 1) "Create Master PIN" else "Confirm PIN & Recovery",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = TextDark
        )
        Text(
            text = if (pinEntryPhase == 1) "4-digit offline lock protecting your personal vault" else "Re-enter PIN and set local recovery credentials",
            fontSize = 11.5.sp,
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { idx ->
                val isFilled = idx < currentPinString.length
                val scale by animateFloatAsState(if (isFilled) 1.25f else 1.0f, spring(dampingRatio = 0.6f), label = "dotScale")
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .scale(scale)
                        .clip(CircleShape)
                        .background(if (isFilled) AccentPurple else BorderLight)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pinEntryPhase == 2) {
            OutlinedTextField(
                value = rawDobDigits,
                onValueChange = onDobChange,
                label = { Text("Recovery Date of Birth (DD / MM / YYYY)", fontSize = 11.sp) },
                visualTransformation = OnboardingDateVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = BorderLight
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = CardWhite,
                border = BorderStroke(0.6.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enable Biometric Unlock", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                    }
                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = onBiometricToggle,
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentPurple)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "DEL")
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keys.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { key ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (key.isNotBlank()) CardWhite else Color.Transparent)
                                .clickable(enabled = key.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    if (key == "DEL") onDeleteDigit() else onDigitPress(key)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "DEL") {
                                Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = TextDark, modifier = Modifier.size(18.dp))
                            } else {
                                Text(key, fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextDark)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = { if (pinEntryPhase == 1) onProceedToConfirm() else onValidateAndNext() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
        ) {
            Text(if (pinEntryPhase == 1) "Confirm PIN" else "Save Security Lock", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

// -------------------------------------------------------------
// STEP 3: COUNTRY REGION & VAULT STRATEGY ENGINE
// -------------------------------------------------------------
@Composable
private fun OnboardingStep3CountryStrategy(
    selectedCountry: CountryCurrencyMapping,
    selectedStrategy: String,
    onSelectCountry: (CountryCurrencyMapping) -> Unit,
    onSelectStrategy: (String) -> Unit,
    onContinue: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text("Country & Currency Mapping", fontSize = 19.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Currencies and denomination formats are auto-bound by region", fontSize = 11.5.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(SupportedCountries) { item ->
                    val isSelected = selectedCountry.countryName == item.countryName
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onSelectCountry(item) },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) AccentPurple.copy(alpha = 0.12f) else CardWhite,
                        border = BorderStroke(0.8.dp, if (isSelected) AccentPurple else BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(item.flagEmoji, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(item.countryName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = TextDark)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(6.dp), color = if (isSelected) AccentPurple else BorderLight.copy(alpha = 0.5f)) {
                                Text(item.currencySymbol, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else TextDark, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Select Vault Architecture", fontSize = 17.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Choose how your capital is structured across accounts", fontSize = 11.5.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(12.dp))

            val is3Vault = selectedStrategy == "3-VAULT"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectStrategy("3-VAULT") },
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(1.2.dp, if (is3Vault) AccentPurple else BorderLight.copy(alpha = 0.6f)),
                shadowElevation = if (is3Vault) 4.dp else 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(AccentPurple.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Layers, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("3-Vault Strategy", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextDark)
                        }
                        if (is3Vault) {
                            Surface(shape = RoundedCornerShape(6.dp), color = AccentPurple) {
                                Text("Recommended", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Partitions funds into Operating (daily spend), Commitments (AutoPay & fixed bills), and Fortress (untouchable emergency reserves).",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            val isSimple = selectedStrategy == "SIMPLE"
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onSelectStrategy("SIMPLE") },
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(1.2.dp, if (isSimple) AccentPurple else BorderLight.copy(alpha = 0.6f)),
                shadowElevation = if (isSimple) 4.dp else 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(34.dp).clip(CircleShape).background(TealPrimary.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Simple Mode", fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextDark)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tracks all accounts and cards in a unified, flat liquidity balance without role restrictions or sweeps.",
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Confirm Architecture", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 4: INITIAL BANK ACCOUNTS & LIQUIDITY
// -------------------------------------------------------------
@Composable
private fun OnboardingStep4Accounts(
    accounts: List<InitialAccountSetup>,
    currencySymbol: String,
    onUpdateAccountBalance: (Int, String) -> Unit,
    onContinue: () -> Unit
) {
    val totalOpening = accounts.sumOf { it.initialBalanceText.toDoubleOrNull() ?: 0.0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text("Opening Liquidity Balances", fontSize = 19.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Enter the current balances in your initial vault accounts", fontSize = 11.5.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TOTAL OPENING NET WORTH", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("$currencySymbol${String.format(Locale.US, "%,.2f", totalOpening)}", fontSize = 24.sp, fontWeight = FontWeight.Black, color = TextDark)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            accounts.forEachIndexed { idx, acc ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CardWhite,
                    border = BorderStroke(0.7.dp, BorderLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                            Text(acc.defaultType, fontSize = 10.5.sp, color = AccentPurple)
                        }

                        OutlinedTextField(
                            value = acc.initialBalanceText,
                            onValueChange = { onUpdateAccountBalance(idx, it.filter { ch -> ch.isDigit() || ch == '.' }) },
                            label = { Text("Balance ($currencySymbol)", fontSize = 10.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(135.dp),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Lock Opening Balances", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 5: FIXED COMMITMENTS
// -------------------------------------------------------------
@Composable
private fun OnboardingStep5Commitments(
    commitments: List<InitialCommitmentPreset>,
    currencySymbol: String,
    onToggleCommitment: (Int) -> Unit,
    onUpdateAmount: (Int, String) -> Unit,
    onContinue: () -> Unit
) {
    val totalCommitted = commitments.filter { it.isSelected }.sumOf { it.amountText.toDoubleOrNull() ?: 0.0 }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text("Seed Fixed Commitments", fontSize = 19.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Pre-allocate recurring bills and SIPs bound to your master categories", fontSize = 11.5.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().shadow(2.dp, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("MONTHLY COMMITTED BUFFER", fontSize = 10.sp, fontWeight = FontWeight.Black, color = TextMuted, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$currencySymbol${String.format(Locale.US, "%,.0f", totalCommitted)}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = CoralAccent)
                    }
                    OrbitalSyncClockCanvas(modifier = Modifier.size(42.dp))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            commitments.forEachIndexed { idx, bill ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onToggleCommitment(idx) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (bill.isSelected) CardWhite else CanvasLight,
                    border = BorderStroke(0.8.dp, if (bill.isSelected) AccentPurple else BorderLight)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Checkbox(
                                checked = bill.isSelected,
                                onCheckedChange = { onToggleCommitment(idx) },
                                colors = CheckboxDefaults.colors(checkedColor = AccentPurple)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(bill.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextDark)
                                Text("${bill.categoryName} • Due ${bill.defaultDueDay}th", fontSize = 10.5.sp, color = TextMuted)
                            }
                        }

                        if (bill.isSelected) {
                            OutlinedTextField(
                                value = bill.amountText,
                                onValueChange = { onUpdateAmount(idx, it.filter { ch -> ch.isDigit() }) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(110.dp),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Complete & Seal Vault", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// STEP 6: DELIBERATION HERO & COMPACT BOTTOM TIMER
// -------------------------------------------------------------
@Composable
private fun OnboardingStep6VaultSealing(
    displayName: String,
    emailAddress: String,
    profileImageUri: Uri?,
    country: CountryCurrencyMapping,
    strategy: String,
    totalLiquidity: Double,
    totalCommitments: Double,
    remainingSeconds: Int,
    onSealImmediately: () -> Unit
) {
    val context = LocalContext.current
    val avatarBitmap = rememberImageBitmapFromUri(context, profileImageUri)
    val countdownFraction = remainingSeconds / 10f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Vault Sealed & Secured", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextDark)
            Text("Your personal financial hub is initialized", fontSize = 12.sp, color = TextMuted)

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(3.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = CardWhite,
                border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap,
                                    contentDescription = null,
                                    modifier = Modifier.size(38.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier.size(38.dp).clip(CircleShape).background(AccentPurple.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(displayName.take(1).uppercase().ifEmpty { "J" }, fontWeight = FontWeight.Black, fontSize = 16.sp, color = AccentPurple)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(displayName, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, color = TextDark)
                                Text(if (emailAddress.isNotBlank()) emailAddress else "${country.countryName} ${country.flagEmoji}", fontSize = 11.sp, color = TextMuted)
                            }
                        }

                        Surface(shape = RoundedCornerShape(8.dp), color = TealPrimary.copy(alpha = 0.14f)) {
                            Text(strategy, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TealPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = BorderLight.copy(alpha = 0.6f), thickness = 0.8.dp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Opening Liquidity", fontSize = 10.sp, color = TextMuted)
                            Text("${country.currencySymbol}${String.format(Locale.US, "%,.2f", totalLiquidity)}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = TextDark)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Monthly Commitments", fontSize = 10.sp, color = TextMuted)
                            Text("${country.currencySymbol}${String.format(Locale.US, "%,.0f", totalCommitments)}", fontWeight = FontWeight.Black, fontSize = 15.sp, color = CoralAccent)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HumanDeliberationSceneCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 18.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(AccentPurple)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Auto-sealing vault in ${remainingSeconds}s...",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { countdownFraction },
                        modifier = Modifier
                            .width(130.dp)
                            .height(2.5.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AccentPurple,
                        trackColor = BorderLight.copy(alpha = 0.5f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onSealImmediately,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
            ) {
                Text("Enter Dashboard Now", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// -------------------------------------------------------------
// MOTION GRAPHICS: DELIBERATION HERO SCENE CANVAS
// -------------------------------------------------------------
@Composable
private fun HumanDeliberationSceneCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "deliberation")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "delibPulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "delibFloat"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2, h * 0.52f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PurplePrimary.copy(alpha = 0.25f * pulse), Color.Transparent),
                center = c,
                radius = 70.dp.toPx()
            ),
            radius = 70.dp.toPx(),
            center = c
        )

        drawOval(
            color = BorderLight.copy(alpha = 0.6f),
            topLeft = Offset(w * 0.22f, h * 0.78f),
            size = Size(w * 0.56f, 16.dp.toPx())
        )

        val shieldCenter = Offset(c.x, c.y + floatOffset)
        drawCircle(color = AccentPurple.copy(alpha = 0.18f), radius = 22.dp.toPx(), center = shieldCenter)
        drawCircle(color = AccentPurple, radius = 10.dp.toPx(), center = shieldCenter)
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = shieldCenter)

        val f1Center = Offset(w * 0.24f, h * 0.50f)
        drawLine(color = CyanPrimary.copy(alpha = 0.45f * pulse), start = f1Center, end = shieldCenter, strokeWidth = 1.5.dp.toPx())
        drawCircle(color = TextDark, radius = 9.dp.toPx(), center = Offset(f1Center.x, f1Center.y - 18.dp.toPx()))
        drawRoundRect(color = CyanPrimary, topLeft = Offset(f1Center.x - 10.dp.toPx(), f1Center.y - 6.dp.toPx()), size = Size(20.dp.toPx(), 26.dp.toPx()), cornerRadius = CornerRadius(6.dp.toPx()))
        drawCircle(color = TealPrimary, radius = 4.dp.toPx(), center = Offset(f1Center.x + 8.dp.toPx(), f1Center.y + 4.dp.toPx() + floatOffset))

        val f2Center = Offset(w * 0.50f, h * 0.28f)
        drawLine(color = PurplePrimary.copy(alpha = 0.45f * pulse), start = f2Center, end = shieldCenter, strokeWidth = 1.5.dp.toPx())
        drawCircle(color = TextDark, radius = 8.dp.toPx(), center = Offset(f2Center.x, f2Center.y - 16.dp.toPx()))
        drawRoundRect(color = PurplePrimary, topLeft = Offset(f2Center.x - 9.dp.toPx(), f2Center.y - 6.dp.toPx()), size = Size(18.dp.toPx(), 22.dp.toPx()), cornerRadius = CornerRadius(5.dp.toPx()))

        val f3Center = Offset(w * 0.76f, h * 0.50f)
        drawLine(color = Color(0xFFE57A28).copy(alpha = 0.45f * pulse), start = f3Center, end = shieldCenter, strokeWidth = 1.5.dp.toPx())
        drawCircle(color = TextDark, radius = 9.dp.toPx(), center = Offset(f3Center.x, f3Center.y - 18.dp.toPx()))
        drawRoundRect(color = Color(0xFFE57A28), topLeft = Offset(f3Center.x - 10.dp.toPx(), f3Center.y - 6.dp.toPx()), size = Size(20.dp.toPx(), 26.dp.toPx()), cornerRadius = CornerRadius(6.dp.toPx()))
        drawCircle(color = Color(0xFFE57A28), radius = 4.dp.toPx(), center = Offset(f3Center.x - 8.dp.toPx(), f3Center.y + 4.dp.toPx() - floatOffset))
    }
}

// -------------------------------------------------------------
// MOTION GRAPHICS CANVASES & IMAGE UTILITIES
// -------------------------------------------------------------

@Composable
fun rememberImageBitmapFromUri(context: Context, uri: Uri?): ImageBitmap? {
    return remember(uri) {
        if (uri == null) null
        else {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Composable
private fun OrbitalVaultParticlesCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "angle"
    )

    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension * 0.38f

        drawCircle(
            brush = Brush.radialGradient(listOf(PurplePrimary.copy(alpha = 0.35f), Color.Transparent)),
            radius = r * 1.1f,
            center = c
        )
        drawCircle(color = AccentPurple, radius = r * 0.55f, center = c)

        repeat(6) { i ->
            val particleAngle = Math.toRadians((angle + (i * 60)).toDouble())
            val px = c.x + (r * cos(particleAngle)).toFloat()
            val py = c.y + (r * sin(particleAngle)).toFloat()
            drawCircle(color = if (i % 2 == 0) CyanPrimary else TealPrimary, radius = 4.dp.toPx(), center = Offset(px, py))
        }
    }
}

@Composable
private fun SecurityRadarPulseCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseRatio"
    )

    Canvas(modifier = modifier) {
        val c = center
        val maxR = size.minDimension * 0.45f
        drawCircle(color = AccentPurple.copy(alpha = 0.15f * (1f - pulseRatio)), radius = maxR * pulseRatio, center = c)
        drawCircle(color = AccentPurple.copy(alpha = 0.35f), radius = maxR * 0.6f, center = c)
        drawCircle(color = AccentPurple, radius = maxR * 0.35f, center = c)
    }
}

@Composable
private fun OrbitalSyncClockCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "clock")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "rot"
    )

    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension * 0.42f

        drawCircle(
            color = AccentPurple.copy(alpha = 0.4f),
            radius = r,
            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), rotation))
        )
        drawCircle(color = AccentPurple, radius = 4.dp.toPx(), center = c)
    }
}

// -------------------------------------------------------------
// AUTOMATIC DOB FORMATTER (DD / MM / YYYY)
// -------------------------------------------------------------
class OnboardingDateVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0..7) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 || i == 3) {
                out += " / "
            }
        }

        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 1) return offset
                if (offset <= 3) return offset + 3
                if (offset <= 8) return offset + 6
                return 14
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                if (offset <= 6) return (offset - 3).coerceAtLeast(0)
                if (offset <= 14) return (offset - 6).coerceAtLeast(0)
                return 8
            }
        }

        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}
