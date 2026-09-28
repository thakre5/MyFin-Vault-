package com.example.myfin.ui.components

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

    // Auto-cycle deck vertically every 5 seconds when idle
    LaunchedEffect(currentIndex, alerts.size, isExpanded, isDragging) {
        if (alerts.size > 1 && !isExpanded && !isDragging) {
            delay(5000L)
            dragOffsetY.animateTo(-80f, tween(260, easing = FastOutSlowInEasing))
            currentIndex = (currentIndex + 1) % alerts.size
            dragOffsetY.snapTo(40f)
            dragOffsetY.animateTo(0f, spring(dampingRatio = 0.82f))
        }
    }

    val totalHeight by animateDpAsState(
        targetValue = when {
            isExpanded -> 138.dp
            alerts.size >= 3 -> 68.dp
            alerts.size == 2 -> 64.dp
            else -> 56.dp
        },
        animationSpec = tween(300),
        label = "deckTotalHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(totalHeight),
        contentAlignment = Alignment.TopCenter
    ) {
        // LAYER 2: Third Card Peek (Visible when 3+ alerts exist)
        if (alerts.size >= 3 && !isExpanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .height(54.dp)
                    .offset(y = 12.dp)
                    .graphicsLayer { alpha = 0.45f }
                    .zIndex(1f),
                shape = RoundedCornerShape(27.dp),
                color = CardWhite,
                border = BorderStroke(0.7.dp, BorderLight.copy(alpha = 0.5f)),
                shadowElevation = 0.5.dp
            ) {}
        }

        // LAYER 1: Second Card Peek (Visible when 2+ alerts exist)
        if (alerts.size >= 2 && !isExpanded) {
            val nextIndex = (currentIndex + 1) % alerts.size
            val nextAlert = alerts[nextIndex]

            val dragProgress = (abs(dragOffsetY.value) / 100f).coerceIn(0f, 1f)
            val peekScale = 0.94f + (0.06f * dragProgress)
            val peekOffsetY = (6.5.dp * (1f - dragProgress))

            Surface(
                modifier = Modifier
                    .fillMaxWidth(peekScale)
                    .height(54.dp)
                    .offset(y = peekOffsetY)
                    .graphicsLayer { alpha = 0.7f + (0.3f * dragProgress) }
                    .zIndex(2f),
                shape = RoundedCornerShape(27.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f)),
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(nextAlert.iconBg.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = nextAlert.icon,
                            contentDescription = null,
                            tint = nextAlert.iconTint.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
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

        // FRONT ACTIVE CARD (Top of the Stack)
        val activeAlert = alerts[currentIndex % alerts.size]

        val frontCardCorner by animateDpAsState(
            targetValue = if (isExpanded) 18.dp else 28.dp,
            animationSpec = tween(280),
            label = "frontCardCorner"
        )

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isExpanded) 136.dp else 56.dp)
                .zIndex(3f)
                .graphicsLayer {
                    translationY = if (!isExpanded) dragOffsetY.value else 0f
                    val dragProgress = (abs(dragOffsetY.value) / 180f).coerceIn(0f, 1f)
                    scaleX = 1f - (0.04f * dragProgress)
                    alpha = 1f - (0.35f * dragProgress)
                }
                .shadow(if (isExpanded) 3.5.dp else 2.dp, RoundedCornerShape(frontCardCorner))
                // Vertical Swipe Handling (Swipe Up or Down to cycle cards)
                .pointerInput(alerts.size, isExpanded) {
                    if (!isExpanded && alerts.size > 1) {
                        detectVerticalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                coroutineScope.launch {
                                    val currentVal = dragOffsetY.value
                                    if (currentVal < -35f) { // Swiped Up
                                        dragOffsetY.animateTo(-100f, tween(150, easing = FastOutSlowInEasing))
                                        currentIndex = (currentIndex + 1) % alerts.size
                                        dragOffsetY.snapTo(50f)
                                        dragOffsetY.animateTo(0f, spring(dampingRatio = 0.8f))
                                    } else if (currentVal > 35f) { // Swiped Down
                                        dragOffsetY.animateTo(100f, tween(150, easing = FastOutSlowInEasing))
                                        currentIndex = (currentIndex - 1 + alerts.size) % alerts.size
                                        dragOffsetY.snapTo(-50f)
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
                },
            shape = RoundedCornerShape(frontCardCorner),
            color = CardWhite,
            border = BorderStroke(0.85.dp, BorderLight.copy(alpha = 0.85f))
        ) {
            if (!isExpanded) {
                // COLLAPSED PILL (56dp with full vertical clearance for both lines)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Side: Tap to Expand Area
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isExpanded = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(activeAlert.iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = activeAlert.icon,
                                contentDescription = null,
                                tint = activeAlert.iconTint,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(9.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = activeAlert.title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                lineHeight = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = activeAlert.subtitle,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Normal,
                                color = TextMuted,
                                lineHeight = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Side: Micro-dots, Action Pill & Expand Chevron
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Samsung Vertical Deck Indicator Dots
                        if (alerts.size > 1) {
                            Column(
                                modifier = Modifier.padding(end = 6.dp),
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

                        Button(
                            onClick = activeAlert.onAction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = activeAlert.actionColor),
                            contentPadding = PaddingValues(horizontal = 11.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = activeAlert.actionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        IconButton(
                            onClick = { isExpanded = true },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = TextMuted.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            } else {
                // EXPANDED CARD (Tapping anywhere on header/background collapses card)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Row: Tapping collapses
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { isExpanded = false },
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

                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowUp,
                                    contentDescription = "Collapse",
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Body Text: Tapping collapses
                    if (activeAlert.subtitle.isNotBlank()) {
                        Text(
                            text = activeAlert.subtitle,
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            lineHeight = 16.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp, vertical = 2.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { isExpanded = false }
                        )
                    }

                    // Action Row
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
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
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
