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
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.onboarding.CountryCurrencyMapping
import com.example.myfin.ui.onboarding.CyanPrimary
import com.example.myfin.ui.onboarding.PurplePrimary
import com.example.myfin.ui.onboarding.SupportedCountries
import com.example.myfin.ui.onboarding.WelcomeCarouselSlides
import com.example.myfin.ui.onboarding.components.OnboardingDateVisualTransformation
import com.example.myfin.ui.onboarding.components.SolnexTiltedCardsHero
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingStep0WelcomeGateway(
    displayName: String,
    emailAddress: String,
    rawDobDigits: String,
    selectedCountry: CountryCurrencyMapping,
    onDisplayNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onDobChange: (String) -> Unit,
    onCountrySelect: (CountryCurrencyMapping) -> Unit,
    onProceedToSecurity: () -> Unit,
    onRestoreVault: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scrollState = rememberScrollState()

    var isFormMode by remember { mutableStateOf(false) }

    // Dynamic keyboard visibility detection
    val isImeVisible = WindowInsets.isImeVisible

    BackHandler(enabled = isFormMode) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        isFormMode = false
    }

    var showCountryPickerSheet by remember { mutableStateOf(false) }
    var showRestoreConfirmationSheet by remember { mutableStateOf(false) }

    val countrySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val restoreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val virtualPageCount = 3000
    val initialPage = (virtualPageCount / 2) - ((virtualPageCount / 2) % WelcomeCarouselSlides.size)
    val carouselPagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { virtualPageCount }
    )

    LaunchedEffect(isFormMode) {
        while (!isFormMode) {
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

    // Smoothly scroll down when keyboard appears to keep focused inputs visible
    LaunchedEffect(isImeVisible) {
        if (isImeVisible && isFormMode) {
            delay(100L)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    // Adaptive Hero Card Scale & Height
    val heroScale by animateFloatAsState(
        targetValue = when {
            isImeVisible && isFormMode -> 0.62f
            isFormMode -> 0.88f
            else -> 1.0f
        },
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "heroScale"
    )
    val heroHeight by animateDpAsState(
        targetValue = when {
            isImeVisible && isFormMode -> 125.dp
            isFormMode -> 195.dp
            else -> 235.dp
        },
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "heroHeight"
    )

    // Dynamic Restore Button Width & Height
    val restoreButtonWidthFraction by animateFloatAsState(
        targetValue = if (isFormMode) 0.52f else 1.0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "restoreWidth"
    )
    val restoreButtonHeight by animateDpAsState(
        targetValue = if (isFormMode) 42.dp else 52.dp,
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
                // =========================================================
                // 1. PINNED BRANDING HEADER
                // =========================================================
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

                Spacer(modifier = Modifier.height(if (isImeVisible) 4.dp else 12.dp))

                // =========================================================
                // 2. RESPONSIVE HERO CARDS (Smoothly scales when keyboard opens)
                // =========================================================
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

                // =========================================================
                // 3. MIDDLE CONTENT (Carousel OR Form)
                // =========================================================
                AnimatedContent(
                    targetState = isFormMode,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } + fadeIn(tween(420)))
                                .togetherWith(slideOutHorizontally(tween(420, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth } + fadeOut(tween(420)))
                        } else {
                            (slideInHorizontally(tween(420, easing = FastOutSlowInEasing)) { fullWidth -> -fullWidth } + fadeIn(tween(420)))
                                .togetherWith(slideOutHorizontally(tween(420, easing = FastOutSlowInEasing)) { fullWidth -> fullWidth } + fadeOut(tween(420)))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = "middleContentTransition"
                ) { formActive ->
                    if (!formActive) {
                        // STAGE 0: Carousel & Indexer
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
                    } else {
                        // STAGE 1: Title + 4 Profile Form Fields
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
                                            .clickable { showCountryPickerSheet = true },
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
                }

                Spacer(modifier = Modifier.height(if (isImeVisible) 10.dp else 16.dp))

                // =========================================================
                // 4. ACTION BUTTONS
                // =========================================================
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
                            if (!isFormMode) {
                                isFormMode = true
                            } else {
                                if (displayName.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter your username", Toast.LENGTH_SHORT).show()
                                } else if (rawDobDigits.length < 8) {
                                    Toast.makeText(context, "Enter valid 8-digit DOB (DDMMYYYY) for recovery", Toast.LENGTH_SHORT).show()
                                } else {
                                    onProceedToSecurity()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TextDark)
                    ) {
                        AnimatedContent(
                            targetState = isFormMode,
                            transitionSpec = {
                                fadeIn(tween(250)).togetherWith(fadeOut(tween(250)))
                            },
                            label = "primaryButtonText"
                        ) { formActive ->
                            Text(
                                text = if (!formActive) "Get Started" else "Register Vault",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showRestoreConfirmationSheet = true },
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
                            fontWeight = if (isFormMode) FontWeight.SemiBold else FontWeight.Bold,
                            fontSize = if (isFormMode) 13.sp else 14.5.sp,
                            color = TextDark,
                            maxLines = 1
                        )
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
    }
}
