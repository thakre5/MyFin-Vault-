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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
    SECURITY
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
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onMasterPinChange: (String) -> Unit,
    onConfirmPinChange: (String) -> Unit,
    onBiometricToggle: (Boolean) -> Unit,
    onCountrySelect: (CountryCurrencyMapping) -> Unit,
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

    val countrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val restoreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val biometricSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val formattedDob = remember(rawDobDigits) {
        if (rawDobDigits.length == 8) {
            "${rawDobDigits.substring(0, 2)}/${rawDobDigits.substring(2, 4)}/${rawDobDigits.substring(4, 8)}"
        } else if (rawDobDigits.isNotBlank()) {
            rawDobDigits
        } else {
            "DD/MM/YYYY"
        }
    }

    // Hardware Back Interception for In-Place Reversal
    BackHandler(enabled = currentStage != GatewayStage.CAROUSEL) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        focusManager.clearFocus()
        keyboardController?.hide()
        currentStage = when (currentStage) {
            GatewayStage.SECURITY -> GatewayStage.IDENTITY
            GatewayStage.IDENTITY -> GatewayStage.CAROUSEL
            GatewayStage.CAROUSEL -> GatewayStage.CAROUSEL
        }
    }

    // Carousel Setup
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

    // Primary Button Width Transformation
    val primaryButtonWidthFraction by animateFloatAsState(
        targetValue = if (currentStage == GatewayStage.SECURITY) 0.58f else 1.0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "primaryWidth"
    )

    // Restore Button Dynamic Sizing
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // =========================================================
            // 1. PINNED FIXED BRANDING HEADER
            // =========================================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
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

            // =========================================================
            // 2. SCROLLABLE BODY WITH DYNAMIC IME KEYBOARD PADDING
            // =========================================================
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(if (isImeVisible) 2.dp else 8.dp))

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

                // Middle Content Animated 3-Stage Transition
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
                        // STAGE 0: Welcome Carousel & Indexer
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

                        // STAGE 1: Profile Identity (4 Form Fields)
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
                                    // 1. Username
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

                                    // 2. Email Address
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

                                    // 3. 50:50 Split Row (DOB & Country Currency)
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

                        // STAGE 2: Vault Security Lock
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
                                    // Row 1: Create Master Password / PIN
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

                                    // Row 2: Confirm Master Password / PIN
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

                                    // Row 3: 50:50 Split (Pre-filled DOB Note + Biometric Switch)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Left 50%: Pre-filled DOB chip
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
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column(verticalArrangement = Arrangement.Center) {
                                                    Text(
                                                        text = formattedDob,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = TextDark
                                                    )
                                                    Text(
                                                        text = "Recovery Key Bound",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = TealPrimary
                                                    )
                                                }
                                            }
                                        }

                                        // Right 50%: Biometric pill
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp)
                                                .clip(RoundedCornerShape(26.dp))
                                                .clickable {
                                                    focusManager.clearFocus()
                                                    keyboardController?.hide()
                                                    showBiometricSheet = true
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
                                                    onCheckedChange = {
                                                        focusManager.clearFocus()
                                                        keyboardController?.hide()
                                                        showBiometricSheet = true
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
                    }
                }

                Spacer(modifier = Modifier.height(if (isImeVisible) 10.dp else 16.dp))

                // =========================================================
                // 3. SYNCHRONIZED ACTION BUTTONS
                // =========================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Primary Action Button (Morphs label and shrinks in Stage 2)
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
                                        onProceedToNextStep()
                                    }
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
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Secondary Restore Button (Collapses and disappears in Stage 2)
                    AnimatedVisibility(
                        visible = currentStage != GatewayStage.SECURITY,
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
