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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Floating segmented indicator pill linked to a horizontal PagerState.
 * Renders an animated title and interactive dash segments above the bottom navigation dock.
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
                .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.12f))
                .clip(RoundedCornerShape(18.dp)),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
            border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
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
                        letterSpacing = 0.3.sp
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Interactive Segmented Indicator Dashes
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pagerState.pageCount) { pageIndex ->
                        val isSelected = pagerState.currentPage == pageIndex
                        val dashWidth by animateDpAsState(
                            targetValue = if (isSelected) 24.dp else 10.dp,
                            animationSpec = spring(dampingRatio = 0.78f, stiffness = 500f),
                            label = "dashWidth"
                        )

                        Box(
                            modifier = Modifier
                                .width(dashWidth)
                                .height(3.5.dp)
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

/**
 * Reusable NestedScrollConnection helper that drives the auto-hide/show behavior
 * of the floating indicator based on scroll direction deltas.
 */
@Composable
fun rememberAutoScrollVisibilityConnection(): Pair<MutableState<Boolean>, NestedScrollConnection> {
    val isVisible = remember { mutableStateOf(true) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -8f && isVisible.value) {
                    isVisible.value = false
                } else if (delta > 8f && !isVisible.value) {
                    isVisible.value = true
                }
                return Offset.Zero
            }
        }
    }

    return Pair(isVisible, connection)
}
