@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package com.example.myfin.ui.onboarding.steps

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.ui.onboarding.CountryCurrencyMapping
import com.example.myfin.ui.onboarding.CyanPrimary
import com.example.myfin.ui.onboarding.PurplePrimary
import com.example.myfin.ui.onboarding.SupportedCountries
import com.example.myfin.ui.onboarding.TealPrimary
import com.example.myfin.ui.onboarding.WelcomeCarouselSlides
import com.example.myfin.ui.onboarding.components.OnboardingDateVisualTransformation
import com.example.myfin.ui.onboarding.components.SolnexTiltedCardsHero
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

enum class GatewayStage {
    CAROUSEL,
    IDENTITY,
    SECURITY,
    STRATEGY
}

@Composable
fun OnboardingStep0WelcomeGateway(
    displayName: String,
    emailAddress: String,
    rawDobDigits: String,
    masterPin: String,
    confirmPin: String,
    isBiometricEnabled: Boolean,
    selectedCountry: CountryCurrencyMapping,
    selectedStrategy: String,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onMasterPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onCountrySelect: (CountryCurrencyMapping) -> Unit,
    onStrategySelect: (String) -> Unit,
    onProceedToNextStep: () -> Unit,
    onRestoreVault: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    var currentStage by remember { mutableStateOf(GatewayStage.CAROUSEL) }
    val isImeVisible = WindowInsets.isImeVisible

    var showMasterPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var showCountryPickerSheet by remember { mutableStateOf(false) }
    var showRestoreConfirmationSheet by remember { mutableStateOf(false) }
    var showBiometricSheet by remember { mutableStateOf(false) }
    var strategyDetailTarget by remember { mutableStateOf<String?>(null) }

    val countrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val restoreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val biometricSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strategySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val formattedDob = remember(rawDobDigits) {
        if (rawDobDigits.length == 8) {
            "${rawDobDigits.substring(0, 2)}/${rawDobDigits.substring(2, 4)}/${rawDobDigits.substring(4, 8)}"
        } else if (rawDobDigits.isNotBlank()) {
            rawDobDigits
        } else {
            "DD/MM/YYYY"
        }
    }

    // Bi-Directional Back Handler
    BackHandler(enabled = currentStage != GatewayStage.CAROUSEL) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        focusManager.clearFocus()
        keyboardController?.hide()
        currentStage = when (currentStage) {
            GatewayStage.STRATEGY -> GatewayStage.SECURITY
            GatewayStage.SECURITY -> GatewayStage.IDENTITY
            GatewayStage.IDENTITY -> GatewayStage.CAROUSEL
            GatewayStage.CAROUSEL -> GatewayStage.CAROUSEL
        }
    }

    // Continuous Infinite Carousel Driver
    val virtualPageCount = 3000
    val initialPage = (virtualPageCount / 2) - ((virtualPageCount / 2) % WelcomeCarouselSlides.size)
    val carouselPagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { virtualPageCount }
    )

    LaunchedEffect(currentStage) {
        while (currentStage == GatewayStage.CAROUSEL) {
            delay(3500L)
            if (carouselPagerState.pageCount > 0) {
                val nextPage = (carouselPagerState.currentPage + 1) % virtualPageCount
                carouselPagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && currentStage != GatewayStage.CAROUSEL) {
            delay(120L)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Adaptive Hero Card Scale & Height
    val heroScale by animateFloatAsState(
        targetValue = when {
            isImeVisible && currentStage != GatewayStage.CAROUSEL -> 0.58f
            currentStage != GatewayStage.CAROUSEL -> 0.88f
            else -> 1.0f
        },
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "heroScale"
    )
    val heroHeight by animateDpAsState(
        targetValue = when {
            isImeVisible && currentStage != GatewayStage.CAROUSEL -> 115.dp
            currentStage != GatewayStage.CAROUSEL -> 195.dp
            else -> 235.dp
        },
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "heroHeight"
    )

    // Dynamic Button Morphing
    val primaryButtonWidthFraction by animateFloatAsState(
        targetValue = if (currentStage == GatewayStage.SECURITY || currentStage == GatewayStage.STRATEGY) 0.58f else 1.0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "primaryWidth"
    )
    val restoreButtonWidthFraction by animateFloatAsState(
        targetValue = if (currentStage == GatewayStage.IDENTITY) 0.52f else 1.0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "restoreWidth"
    )
    val restoreButtonHeight by animateDpAsState(
        targetValue = if (currentStage == GatewayStage.IDENTITY) 42.dp else 52.dp,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "restoreHeight"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF3E8FF),
                        Color(0xFFEDE9FE).copy(alpha = 0.65f),
                        Color(0xFFF8FAFC),
                        CanvasLight
                    )
                )
            )
    ) {
        // =========================================================================
        // LAYER 1: SCROLLABLE BODY
        // =========================================================================
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val minScreenHeight = maxHeight

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 24.dp)
                    .defaultMinSize(minHeight = minScreenHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(56.dp))

                // Hero Cards
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heroHeight),
                    contentAlignment = Alignment.Center
                ) {
                    SolnexTiltedCardsHero(
                        currencySymbol = selectedCountry.currencySymbol,
                        modifier = Modifier.graphicsLayer {
                            scaleX = heroScale
                            scaleY = heroScale
                        }
                    )
                }

                Spacer(modifier = Modifier.height(if (isImeVisible) 6.dp else 16.dp))

                // Multi-Stage Animated Content
                AnimatedContent(
                    targetState = currentStage,
                    transitionSpec = {
                        val isForward = targetState.ordinal > initialState.ordinal
                        if (isForward) {
                            (slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } + fadeIn(tween(420)))
                                .togetherWith(slideOutHorizontally(tween(420, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth } + fadeOut(tween(420)))
                        } else {
                            (slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth } + fadeIn(tween(420)))
                                .togetherWith(slideOutHorizontally(tween(420, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } + fadeOut(tween(420)))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "gatewayStageTransition"
                ) { stage ->
                    when (stage) {
                        // STAGE 0: Welcome Carousel
                        GatewayStage.CAROUSEL -> {
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
                                    val actualIndex = page % WelcomeCarouselSlides.size
                                    val slide = WelcomeCarouselSlides[actualIndex]
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

                                val activeIndex = carouselPagerState.currentPage % WelcomeCarouselSlides.size
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    repeat(WelcomeCarouselSlides.size) { idx ->
                                        val isSelected = activeIndex == idx
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
                        }

                        // STAGE 1: Profile Identity
                        GatewayStage.IDENTITY -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Create Vault Account",
                                    fontSize = if (isImeVisible) 19.sp else 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = (-0.5).sp
                                )
                                if (!isImeVisible) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Enter your offline profile credentials",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(if (isImeVisible) 8.dp else 14.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = displayName,
                                        onValueChange = onDisplayNameChange,
                                        placeholder = { Text("Username", fontSize = 13.5.sp, color = TextMuted) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Person,
                                                contentDescription = null,
                                                tint = AccentPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 14.sp, color = TextDark),
                                        shape = RoundedCornerShape(26.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = CardWhite,
                                            unfocusedContainerColor = CardWhite,
                                            focusedBorderColor = AccentPurple,
                                            unfocusedBorderColor = BorderLight.copy(alpha = 0.9f)
                                        )
                                    )

                                    OutlinedTextField(
                                        value = emailAddress,
                                        onValueChange = onEmailChange,
                                        placeholder = { Text("Email Address", fontSize = 13.5.sp, color = TextMuted) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Mail,
                                                contentDescription = null,
                                                tint = AccentPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 14.sp, color = TextDark),
                                        shape = RoundedCornerShape(26.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = CardWhite,
                                            unfocusedContainerColor = CardWhite,
                                            focusedBorderColor = AccentPurple,
                                            unfocusedBorderColor = BorderLight.copy(alpha = 0.9f)
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = rawDobDigits,
                                            onValueChange = { input ->
                                                onDobChange(input.filter { it.isDigit() }.take(8))
                                            },
                                            placeholder = { Text("DD/MM/YYYY", fontSize = 12.sp, color = TextMuted) },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    tint = AccentPurple,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            },
                                            visualTransformation = OnboardingDateVisualTransformation(),
                                            singleLine = true,
                                            textStyle = TextStyle(
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextDark,
                                                letterSpacing = 0.5.sp
                                            ),
                                            shape = RoundedCornerShape(26.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedContainerColor = CardWhite,
                                                unfocusedContainerColor = CardWhite,
                                                focusedBorderColor = AccentPurple,
                                                unfocusedBorderColor = BorderLight.copy(alpha = 0.9f)
                                            )
                                        )

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .clip(RoundedCornerShape(26.dp))
                                                .clickable {
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                    showCountryPickerSheet = true
                                                },
                                            shape = RoundedCornerShape(26.dp),
                                            color = CardWhite,
                                            border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    Text(selectedCountry.flagEmoji, fontSize = 15.sp)
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = "${selectedCountry.currencySymbol} ${selectedCountry.currencyCode}",
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextDark,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Icon(
                                                    Icons.Default.ArrowDropDown,
                                                    contentDescription = "Select Currency",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // STAGE 2: Security Lock
                        GatewayStage.SECURITY -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Vault Security Lock",
                                    fontSize = if (isImeVisible) 19.sp else 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = (-0.5).sp
                                )
                                if (!isImeVisible) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Set your offline access & recovery keys",
                                        fontSize = 12.sp,
                                        color = TextMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(if (isImeVisible) 8.dp else 14.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = masterPin,
                                        onValueChange = onMasterPinChange,
                                        placeholder = { Text("Create Master PIN / Password", fontSize = 13.sp, color = TextMuted) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Lock,
                                                contentDescription = null,
                                                tint = AccentPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            IconButton(onClick = { showMasterPassword = !showMasterPassword }) {
                                                Icon(
                                                    imageVector = if (showMasterPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (showMasterPassword) "Hide PIN" else "Show PIN",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        },
                                        visualTransformation = if (showMasterPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 14.sp, color = TextDark),
                                        shape = RoundedCornerShape(26.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = CardWhite,
                                            unfocusedContainerColor = CardWhite,
                                            focusedBorderColor = AccentPurple,
                                            unfocusedBorderColor = BorderLight.copy(alpha = 0.9f)
                                        )
                                    )

                                    val isPinMatching = confirmPin.isNotEmpty() && confirmPin == masterPin
                                    OutlinedTextField(
                                        value = confirmPin,
                                        onValueChange = onConfirmPinChange,
                                        placeholder = { Text("Confirm Master PIN / Password", fontSize = 13.sp, color = TextMuted) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Lock,
                                                contentDescription = null,
                                                tint = if (isPinMatching) TealPrimary else AccentPurple,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(end = 4.dp)
                                            ) {
                                                if (isPinMatching) {
                                                    Icon(
                                                        Icons.Default.CheckCircle,
                                                        contentDescription = "Matched",
                                                        tint = TealPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                }
                                                IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                                    Icon(
                                                        imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = if (showConfirmPassword) "Hide PIN" else "Show PIN",
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        },
                                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 14.sp, color = TextDark),
                                        shape = RoundedCornerShape(26.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(50.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = CardWhite,
                                            unfocusedContainerColor = CardWhite,
                                            focusedBorderColor = if (isPinMatching) TealPrimary else AccentPurple,
                                            unfocusedBorderColor = BorderLight.copy(alpha = 0.9f)
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp),
                                            shape = RoundedCornerShape(26.dp),
                                            color = CardWhite,
                                            border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Default.CalendarToday,
                                                    contentDescription = null,
                                                    tint = AccentPurple,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Text(
                                                        text = formattedDob,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextDark,
                                                        lineHeight = 15.sp,
                                                        maxLines = 1
                                                    )
                                                    Text(
                                                        text = "Recovery Key Bound",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TealPrimary,
                                                        lineHeight = 11.sp,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .clip(RoundedCornerShape(26.dp))
                                                .clickable {
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                    if (!isBiometricEnabled) {
                                                        showBiometricSheet = true
                                                    } else {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        onBiometricToggle(false)
                                                    }
                                                },
                                            shape = RoundedCornerShape(26.dp),
                                            color = CardWhite,
                                            border = BorderStroke(1.dp, if (isBiometricEnabled) AccentPurple.copy(alpha = 0.6f) else BorderLight.copy(alpha = 0.9f))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        Icons.Default.Fingerprint,
                                                        contentDescription = null,
                                                        tint = if (isBiometricEnabled) AccentPurple else TextMuted,
                                                        modifier = Modifier.size(17.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = "Biometric",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextDark
                                                    )
                                                }
                                                Switch(
                                                    checked = isBiometricEnabled,
                                                    onCheckedChange = { checked ->
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                        if (checked) {
                                                            showBiometricSheet = true
                                                        } else {
                                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                            onBiometricToggle(false)
                                                        }
                                                    },
                                                    modifier = Modifier.scale(0.7f),
                                                    colors = SwitchDefaults.colors(
                                                        checkedThumbColor = Color.White,
                                                        checkedTrackColor = AccentPurple,
                                                        uncheckedTrackColor = CanvasLight
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // STAGE 3: Bank Strategy Selection (2 Capsule Cards)
                        GatewayStage.STRATEGY -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Choose Vault Strategy",
                                    fontSize = if (isImeVisible) 19.sp else 22.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark,
                                    textAlign = TextAlign.Center,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "Configure how your money flows across accounts",
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Strategy Card 1: 3-Tier Strategy
                                    val is3Tier = selectedStrategy == "3-VAULT"
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                            .clip(RoundedCornerShape(26.dp))
                                            .clickable {
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                strategyDetailTarget = "3-VAULT"
                                            },
                                        shape = RoundedCornerShape(26.dp),
                                        color = if (is3Tier) AccentPurple.copy(alpha = 0.08f) else CardWhite,
                                        border = BorderStroke(
                                            width = if (is3Tier) 1.5.dp else 1.dp,
                                            color = if (is3Tier) AccentPurple else BorderLight.copy(alpha = 0.9f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(36.dp),
                                                    shape = CircleShape,
                                                    color = if (is3Tier) AccentPurple.copy(alpha = 0.15f) else CanvasLight
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Outlined.AccountBalance,
                                                            contentDescription = null,
                                                            tint = AccentPurple,
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Smart 3-Tier Wealth Strategy",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextDark
                                                    )
                                                    Text(
                                                        text = "Operating • Commitments • Fortress",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            RadioButton(
                                                selected = is3Tier,
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    strategyDetailTarget = "3-VAULT"
                                                },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = AccentPurple,
                                                    unselectedColor = BorderLight
                                                )
                                            )
                                        }
                                    }

                                    // Strategy Card 2: Simple Vault
                                    val isSimple = selectedStrategy == "SIMPLE"
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(64.dp)
                                            .clip(RoundedCornerShape(26.dp))
                                            .clickable {
                                                focusManager.clearFocus()
                                                keyboardController?.hide()
                                                strategyDetailTarget = "SIMPLE"
                                            },
                                        shape = RoundedCornerShape(26.dp),
                                        color = if (isSimple) AccentPurple.copy(alpha = 0.08f) else CardWhite,
                                        border = BorderStroke(
                                            width = if (isSimple) 1.5.dp else 1.dp,
                                            color = if (isSimple) AccentPurple else BorderLight.copy(alpha = 0.9f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(36.dp),
                                                    shape = CircleShape,
                                                    color = if (isSimple) AccentPurple.copy(alpha = 0.15f) else CanvasLight
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(
                                                            Icons.Outlined.AccountBalanceWallet,
                                                            contentDescription = null,
                                                            tint = AccentPurple,
                                                            modifier = Modifier.size(19.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column {
                                                    Text(
                                                        text = "Simple Unified Vault",
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextDark
                                                    )
                                                    Text(
                                                        text = "Single ledger for all cash flow",
                                                        fontSize = 11.sp,
                                                        color = TextMuted
                                                    )
                                                }
                                            }

                                            RadioButton(
                                                selected = isSimple,
                                                onClick = {
                                                    focusManager.clearFocus()
                                                    strategyDetailTarget = "SIMPLE"
                                                },
                                                colors = RadioButtonDefaults.colors(
                                                    selectedColor = AccentPurple,
                                                    unselectedColor = BorderLight
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(if (isImeVisible) 10.dp else 16.dp))

                // Bottom Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            when (currentStage) {
                                GatewayStage.CAROUSEL -> {
                                    currentStage = GatewayStage.IDENTITY
                                }
                                GatewayStage.IDENTITY -> {
                                    if (displayName.trim().isEmpty()) {
                                        Toast.makeText(context, "Please enter your username", Toast.LENGTH_SHORT).show()
                                    } else if (rawDobDigits.length < 8) {
                                        Toast.makeText(context, "Enter valid 8-digit DOB (DDMMYYYY) for recovery", Toast.LENGTH_SHORT).show()
                                    } else {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        currentStage = GatewayStage.SECURITY
                                    }
                                }
                                GatewayStage.SECURITY -> {
                                    if (masterPin.trim().length < 4) {
                                        Toast.makeText(context, "Password/PIN must be at least 4 characters", Toast.LENGTH_SHORT).show()
                                    } else if (confirmPin != masterPin) {
                                        Toast.makeText(context, "Passwords do not match. Please re-enter.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        focusManager.clearFocus()
                                        keyboardController?.hide()
                                        currentStage = GatewayStage.STRATEGY
                                    }
                                }
                                GatewayStage.STRATEGY -> {
                                    focusManager.clearFocus()
                                    keyboardController?.hide()
                                    onProceedToNextStep()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(primaryButtonWidthFraction)
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        AnimatedContent(
                            targetState = currentStage,
                            transitionSpec = {
                                fadeIn(tween(250)).togetherWith(fadeOut(tween(250)))
                            },
                            label = "primaryButtonText"
                        ) { stage ->
                            Text(
                                text = when (stage) {
                                    GatewayStage.CAROUSEL -> "Get Started"
                                    GatewayStage.IDENTITY -> "Register Vault"
                                    GatewayStage.SECURITY -> "Lock Vault"
                                    GatewayStage.STRATEGY -> "Set Strategy"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = currentStage == GatewayStage.CAROUSEL || currentStage == GatewayStage.IDENTITY,
                        enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                        exit = fadeOut(tween(300)) + shrinkVertically(tween(300))
                    ) {
                        OutlinedButton(
                            onClick = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                                showRestoreConfirmationSheet = true
                            },
                            modifier = Modifier
                                .fillMaxWidth(restoreButtonWidthFraction)
                                .height(restoreButtonHeight),
                            shape = RoundedCornerShape(26.dp),
                            border = BorderStroke(1.dp, BorderLight.copy(alpha = 0.9f)),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardWhite),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            Text(
                                text = "Restore Vault",
                                fontWeight = if (currentStage == GatewayStage.IDENTITY) FontWeight.SemiBold else FontWeight.Bold,
                                fontSize = if (currentStage == GatewayStage.IDENTITY) 13.sp else 14.5.sp,
                                color = TextDark,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // LAYER 2: FLOATING PINNED HEADER
        // =========================================================================
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .zIndex(10f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF3E8FF).copy(alpha = 0.95f),
                            Color(0xFFF3E8FF).copy(alpha = 0.60f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
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
                                colors = listOf(CyanPrimary, AccentPurple, PurplePrimary)
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
                    text = "MyFin",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    color = TextDark,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // =========================================================
        // STRATEGY DETAIL & CONFIRMATION BOTTOM SHEET
        // =========================================================
        strategyDetailTarget?.let { target ->
            ModalBottomSheet(
                onDismissRequest = { strategyDetailTarget = null },
                sheetState = strategySheetState,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                containerColor = CardWhite,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = AccentPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (target == "3-VAULT") Icons.Outlined.AccountBalance else Icons.Outlined.AccountBalanceWallet,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = if (target == "3-VAULT") "Smart 3-Tier Wealth Strategy" else "Simple Unified Vault",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (target == "3-VAULT")
                            "Automates cash flow between daily spending, bills, and emergency reserves with zero leakage."
                        else
                            "Classic single-account ledger for all income and daily expense tracking.",
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (target == "3-VAULT") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CanvasLight,
                            border = BorderStroke(0.8.dp, BorderLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Primary Operating Vault", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Text("Daily liquid spending, UPI, groceries, and leisure.", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Shield, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Bills & Autopay Commitments", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Text("Ring-fenced funds for rent, EMIs, utilities & SIPs.", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Emergency Fortress", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Text("High-security untouchable reserve cushion.", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = CanvasLight,
                            border = BorderStroke(0.8.dp, BorderLight)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Unified Cash Flow", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Text("All incoming income and expenses live in a single ledger.", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Zero Transfer Management", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextDark)
                                        Text("No need to allocate money across separate sub-vaults.", fontSize = 11.sp, color = TextMuted)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onStrategySelect(target)
                            strategyDetailTarget = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text(
                            text = if (target == "3-VAULT") "Apply 3-Tier Strategy" else "Apply Simple Strategy",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { strategyDetailTarget = null },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    }
                }
            }
        }

        // =========================================================
        // COUNTRY / CURRENCY SELECTOR SHEET
        // =========================================================
        if (showCountryPickerSheet) {
            ModalBottomSheet(
                onDismissRequest = { showCountryPickerSheet = false },
                sheetState = countrySheetState,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                containerColor = CardWhite,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp, top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(38.dp),
                            shape = CircleShape,
                            color = AccentPurple.copy(alpha = 0.12f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Country & Currency", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextDark)
                            Text("Select region for local denomination formats", fontSize = 11.5.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(SupportedCountries) { item ->
                            val isSelected = selectedCountry.countryName == item.countryName
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        onCountrySelect(item)
                                        showCountryPickerSheet = false
                                    },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) AccentPurple.copy(alpha = 0.10f) else CanvasLight,
                                border = BorderStroke(0.8.dp, if (isSelected) AccentPurple else BorderLight)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(item.flagEmoji, fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            item.countryName,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = TextDark
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) AccentPurple else CardWhite,
                                        border = BorderStroke(0.6.dp, BorderLight)
                                    ) {
                                        Text(
                                            text = "${item.currencySymbol}  (${item.currencyCode})",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) Color.White else TextDark,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
        // RESTORE VAULT CONFIRMATION BOTTOM SHEET
        // =========================================================
        if (showRestoreConfirmationSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRestoreConfirmationSheet = false },
                sheetState = restoreSheetState,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                containerColor = CardWhite,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = AccentPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Restore from Local Backup",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Import your offline encrypted snapshot to restore your accounts, budget plans, and historical records.",
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.8.dp, BorderLight)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LockReset, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Requires the Master PIN used when creating the backup.", fontSize = 11.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Restores all bank accounts, taxonomies, and fixed bills.", fontSize = 11.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("100% offline verification with hardware keystore security.", fontSize = 11.5.sp, color = TextDark, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            showRestoreConfirmationSheet = false
                            onRestoreVault()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text("Choose Backup File (.json)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showRestoreConfirmationSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
                    }
                }
            }
        }

        // =========================================================
        // BIOMETRIC CONFIRMATION BOTTOM SHEET
        // =========================================================
        if (showBiometricSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBiometricSheet = false },
                sheetState = biometricSheetState,
                shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                containerColor = CardWhite,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp, top = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(60.dp),
                        shape = CircleShape,
                        color = AccentPurple.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Biometric Authentication",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextDark,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Unlock your offline personal ledger instantly using your device's biometric sensor (Fingerprint / Face ID).",
                        fontSize = 12.5.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = CanvasLight,
                        border = BorderStroke(0.8.dp, BorderLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Protected by Android Hardware Keystore. Biometrics never leave your physical device.",
                                fontSize = 11.5.sp,
                                color = TextDark,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            onBiometricToggle(true)
                            showBiometricSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        Text(
                            text = "Enable Biometric Unlock",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = {
                            onBiometricToggle(false)
                            showBiometricSheet = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Keep PIN Only",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}
