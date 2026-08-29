@file:OptIn(ExperimentalFoundationApi::class)

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.example.myfin.ui.onboarding.WelcomeCarouselSlides
import com.example.myfin.ui.onboarding.components.SolnexTiltedCardsHero
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
