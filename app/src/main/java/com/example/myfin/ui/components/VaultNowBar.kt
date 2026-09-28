package com.example.myfin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

data class NowBarAlert(
    val id: String,
    val icon: ImageVector,
    val iconTint: Color,
    val iconBg: Color,
    val title: String,
    val subtitle: String = "",
    val actionLabel: String,
    val actionColor: Color,
    val onAction: () -> Unit,
    val onDismiss: (() -> Unit)? = null
)

@Composable
fun VaultNowBar(
    alerts: List<NowBarAlert>,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    var isExpanded by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val dragOffsetY = remember { Animatable(0f) }

    // Auto-cycle the deck vertically every 5 seconds if not expanded or being dragged
    LaunchedEffect(currentIndex, alerts.size, isExpanded, isDragging) {
        if (alerts.size > 1 && !isExpanded && !isDragging) {
            delay(5000L)
            dragOffsetY.animateTo(-90f, tween(260, easing = FastOutSlowInEasing))
            currentIndex = (currentIndex + 1) % alerts.size
            dragOffsetY.snapTo(50f)
            dragOffsetY.animateTo(0f, spring(dampingRatio = 0.82f))
        }
    }

    val totalHeight by animateDpAsState(
        targetValue = if (isExpanded) 138.dp else if (alerts.size > 1) 58.dp else 50.dp,
        animationSpec = tween(300),
        label = "deckTotalHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        // 1. LAYER 2 (Third Card Peek, if 3+ alerts)
        if (alerts.size >= 3 && !isExpanded) {
            val thirdIndex = (currentIndex + 2) % alerts.size
            val thirdAlert = alerts[thirdIndex]

            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(48.dp)
                    .offset(y = 10.dp)
                    .graphicsLayer { alpha = 0.45f }
                    .zIndex(1f),
                shape = RoundedCornerShape(24.dp),
                color = CardWhite,
                border = BorderStroke(0.7.dp, BorderLight.copy(alpha = 0.5f)),
                shadowElevation = 0.5.dp
            ) {}
        }

        // 2. LAYER 1 (Second Card Peek, if 2+ alerts)
        if (alerts.size >= 2 && !isExpanded) {
            val nextIndex = (currentIndex + 1) % alerts.size
            val nextAlert = alerts[nextIndex]

            // Dynamically scale/tuck the peek as user swipes
            val peekProgress = (abs(dragOffsetY.value) / 100f).coerceIn(0f, 1f)
            val peekScale = 0.94f + (0.06f * peekProgress)
            val peekOffsetY = (5.5.dp * (1f - peekProgress))

            Surface(
                modifier = Modifier
                    .fillMaxWidth(peekScale)
                    .height(48.dp)
                    .offset(y = peekOffsetY)
                    .graphicsLayer { alpha = 0.65f + (0.35f * peekProgress) }
                    .zIndex(2f),
                shape = RoundedCornerShape(24.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.65f)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(nextAlert.iconBg.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = nextAlert.icon,
                            contentDescription = null,
                            tint = nextAlert.iconTint.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = nextAlert.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // 3. FRONT ACTIVE CARD (Top of the Stack)
        val activeAlert = alerts[currentIndex % alerts.size]

        val frontCardElevation = if (isExpanded) 3.5.dp else 2.dp
        val frontCardCorner by animateDpAsState(
            targetValue = if (isExpanded) 18.dp else 25.dp,
            animationSpec = tween(280),
            label = "frontCardCorner"
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isExpanded) 136.dp else 50.dp)
                .zIndex(3f)
                .graphicsLayer {
                    translationY = if (!isExpanded) dragOffsetY.value else 0f
                    val dragProgress = (abs(dragOffsetY.value) / 200f).coerceIn(0f, 1f)
                    scaleX = 1f - (0.05f * dragProgress)
                    alpha = 1f - (0.4f * dragProgress)
                }
                .shadow(frontCardElevation, RoundedCornerShape(frontCardCorner))
                // Vertical Swipe Handling (Swipe Up/Down to flick cards)
                .pointerInput(alerts.size, isExpanded) {
                    if (!isExpanded && alerts.size > 1) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                coroutineScope.launch {
                                    val currentVal = dragOffsetY.value
                                    if (currentVal < -35f) { // Swiped Up
                                        dragOffsetY.animateTo(-120f, tween(160, easing = FastOutSlowInEasing))
                                        currentIndex = (currentIndex + 1) % alerts.size
                                        dragOffsetY.snapTo(60f)
                                        dragOffsetY.animateTo(0f, spring(dampingRatio = 0.8f))
                                    } else if (currentVal > 35f) { // Swiped Down
                                        dragOffsetY.animateTo(120f, tween(160, easing = FastOutSlowInEasing))
                                        currentIndex = (currentIndex - 1 + alerts.size) % alerts.size
                                        dragOffsetY.snapTo(-60f)
                                        dragOffsetY.animateTo(0f, spring(dampingRatio = 0.8f))
                                    } else {
                                        dragOffsetY.animateTo(0f, spring(dampingRatio = 0.7f))
                                    }
                                    isDragging = false
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    dragOffsetY.animateTo(0f)
                                    isDragging = false
                                }
                            },
                            onVerticalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    dragOffsetY.snapTo(dragOffsetY.value + dragAmount * 0.75f)
                                }
                            }
                        )
                    }
                }
                // Tap anywhere to Expand or Collapse
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    isExpanded = !isExpanded
                },
            shape = RoundedCornerShape(frontCardCorner),
            color = CardWhite,
            border = BorderStroke(0.85.dp, BorderLight.copy(alpha = 0.85f))
        ) {
            if (!isExpanded) {
                // COLLAPSED SINGLE-LINE NOW BAR CAPSULE
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(activeAlert.iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeAlert.icon,
                                contentDescription = null,
                                tint = activeAlert.iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(9.dp))

                        Text(
                            text = activeAlert.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Samsung Vertical Deck Dots Indicator
                        if (alerts.size > 1) {
                            Column(
                                modifier = Modifier.padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                repeat(alerts.size) { idx ->
                                    val isCurrent = (currentIndex % alerts.size) == idx
                                    Box(
                                        modifier = Modifier
                                            .size(width = 3.5.dp, height = if (isCurrent) 8.dp else 3.5.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) activeAlert.actionColor else BorderLight)
                                    )
                                }
                            }
                        }

                        // Action Button Pill (Clicks button directly without triggering card toggle)
                        Button(
                            onClick = activeAlert.onAction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeAlert.actionColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = activeAlert.actionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(3.dp))

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = TextMuted.copy(alpha = 0.7f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            } else {
                // EXPANDED DETAILS CARD (Tap anywhere collapses back)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(activeAlert.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = activeAlert.icon,
                                    contentDescription = null,
                                    tint = activeAlert.iconTint,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = activeAlert.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (alerts.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = CanvasLight,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = "${(currentIndex % alerts.size) + 1}/${alerts.size}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Collapse",
                                tint = TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (activeAlert.subtitle.isNotBlank()) {
                        Text(
                            text = activeAlert.subtitle,
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeAlert.onDismiss != null) {
                            TextButton(
                                onClick = {
                                    activeAlert.onDismiss.invoke()
                                    isExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(
                                    text = "Dismiss",
                                    fontSize = 11.sp,
                                    color = TextMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = {
                                activeAlert.onAction.invoke()
                                isExpanded = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeAlert.actionColor),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = activeAlert.actionLabel,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
