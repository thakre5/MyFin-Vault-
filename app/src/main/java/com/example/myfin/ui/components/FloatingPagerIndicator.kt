package com.example.myfin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FloatingPagerIndicator(
    pagerState: PagerState,
    pageTitles: List<String>,
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    activeColor: Color = AccentPurple,
    inactiveColor: Color = BorderLight.copy(alpha = 0.9f),
    containerColor: Color = CardWhite
) {
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val slideOffsetPx = with(density) { 36.dp.toPx() }

    val animAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (isVisible) 220 else 180),
        label = "indicatorAlpha"
    )

    val animTranslationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else slideOffsetPx,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "indicatorSlide"
    )

    val isInteractive = isVisible && animAlpha > 0.5f

    if (animAlpha > 0.01f) {
        Surface(
            modifier = modifier
                .graphicsLayer {
                    alpha = animAlpha
                    translationY = animTranslationY
                }
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = Color.Black.copy(alpha = 0.08f),
                    spotColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    enabled = isInteractive,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LightImpact)
                        coroutineScope.launch {
                            val next = (pagerState.currentPage + 1) % pagerState.pageCount
                            pagerState.animateScrollToPage(next)
                        }
                    }
                ),
            shape = RoundedCornerShape(14.dp),
            color = containerColor,
            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(160)) + slideInVertically(animationSpec = tween(160)) { height -> height / 2 })
                            .togetherWith(fadeOut(animationSpec = tween(120)) + slideOutVertically(animationSpec = tween(120)) { height -> -height / 2 })
                    },
                    label = "tabTitleAnimation"
                ) { targetPage ->
                    val title = pageTitles.getOrNull(targetPage).orEmpty()
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                        letterSpacing = 0.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(1.5.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) { pageIndex ->
                        val isSelected = pagerState.currentPage == pageIndex
                        val dashWidth by animateDpAsState(
                            targetValue = if (isSelected) 14.dp else 5.dp,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 500f),
                            label = "dashWidth"
                        )

                        Box(
                            modifier = Modifier
                                .clickable(
                                    enabled = isInteractive,
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pageIndex)
                                        }
                                    }
                                }
                                .padding(vertical = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(dashWidth)
                                    .height(2.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) activeColor else inactiveColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
