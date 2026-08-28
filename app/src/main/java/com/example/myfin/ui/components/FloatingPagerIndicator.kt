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
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Floating segmented indicator pill aligned with the bottom dock's active tab.
 */
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

    val animAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (isVisible) 220 else 180),
        label = "indicatorAlpha"
    )

    val animTranslationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 28f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "indicatorSlide"
    )

    if (animAlpha > 0.01f) {
        Surface(
            modifier = modifier
                .graphicsLayer {
                    alpha = animAlpha
                    translationY = animTranslationY
                }
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = Color.Black.copy(alpha = 0.12f),
                    spotColor = Color.Black.copy(alpha = 0.16f)
                )
                .clip(RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = containerColor,
            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Animated Label Crossfade
                AnimatedContent(
                    targetState = pagerState.currentPage,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(180)) + slideInVertically(animationSpec = tween(180)) { height -> height / 2 })
                            .togetherWith(fadeOut(animationSpec = tween(140)) + slideOutVertically(animationSpec = tween(140)) { height -> -height / 2 })
                    },
                    label = "tabTitleAnimation"
                ) { targetPage ->
                    val title = pageTitles.getOrNull(targetPage).orEmpty()
                    Text(
                        text = title,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                        letterSpacing = 0.2.sp
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Interactive Segmented Indicator Dashes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) { pageIndex ->
                        val isSelected = pagerState.currentPage == pageIndex
                        val dashWidth by animateDpAsState(
                            targetValue = if (isSelected) 18.dp else 7.dp,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 500f),
                            label = "dashWidth"
                        )

                        Box(
                            modifier = Modifier
                                .width(dashWidth)
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) activeColor else inactiveColor)
                                .clickable(
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
                        )
                    }
                }
            }
        }
    }
}
