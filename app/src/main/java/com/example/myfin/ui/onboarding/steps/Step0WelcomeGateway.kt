@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.myfin.ui.onboarding.steps

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.onboarding.CyanPrimary
import com.example.myfin.ui.onboarding.PurplePrimary
import com.example.myfin.ui.onboarding.WelcomeCarouselSlides
import com.example.myfin.ui.onboarding.components.SolnexTiltedCardsHero
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.BorderLight
import com.example.myfin.ui.theme.CanvasLight
import com.example.myfin.ui.theme.CardWhite
import com.example.myfin.ui.theme.TextDark
import com.example.myfin.ui.theme.TextMuted
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingStep0WelcomeGateway(
    currencySymbol: String,
    onGetStarted: () -> Unit,
    onRestoreVault: () -> Unit
) {
    var showRestoreConfirmationSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val virtualPageCount = 3000
    val initialPage = (virtualPageCount / 2) - ((virtualPageCount / 2) % WelcomeCarouselSlides.size)
    val carouselPagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { virtualPageCount }
    )

    LaunchedEffect(Unit) {
        while (true) {
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
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App Branding Header
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

                // Synchronized 3-Pill Indexer
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
                    onClick = { showRestoreConfirmationSheet = true },
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

        // =========================================================
        // RESTORE VAULT CONFIRMATION BOTTOM SHEET
        // =========================================================
        if (showRestoreConfirmationSheet) {
            ModalBottomSheet(
                onDismissRequest = { showRestoreConfirmationSheet = false },
                sheetState = sheetState,
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
                    // Header Icon Badge
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

                    // Notice Info Box
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
                                Icon(
                                    Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Requires the Master PIN used when creating the backup.",
                                    fontSize = 11.5.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.CheckCircleOutline,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Restores all bank accounts, taxonomies, and fixed bills.",
                                    fontSize = 11.5.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "100% offline verification with hardware keystore security.",
                                    fontSize = 11.5.sp,
                                    color = TextDark,
                                    fontWeight = FontWeight.Medium
                                )
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
                        Text(
                            text = "Choose Backup File (.json)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { showRestoreConfirmationSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Cancel",
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
