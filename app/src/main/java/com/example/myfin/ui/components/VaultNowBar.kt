package com.example.myfin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.theme.*
import kotlinx.coroutines.delay

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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VaultNowBar(
    alerts: List<NowBarAlert>,
    modifier: Modifier = Modifier
) {
    if (alerts.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { alerts.size })

    // Auto-cycle vertically every 4.5 seconds if multiple alerts exist and not expanded
    LaunchedEffect(pagerState, alerts.size, isExpanded) {
        if (alerts.size > 1 && !isExpanded) {
            while (true) {
                delay(4500L)
                if (!pagerState.isScrollInProgress) {
                    val next = (pagerState.currentPage + 1) % alerts.size
                    pagerState.animateScrollToPage(next, animationSpec = tween(550))
                }
            }
        }
    }

    val barHeight by animateDpAsState(
        targetValue = if (isExpanded) 124.dp else 50.dp,
        animationSpec = tween(300),
        label = "nowBarHeight"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 18.dp else 25.dp,
        animationSpec = tween(300),
        label = "nowBarCorner"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .shadow(if (isExpanded) 3.dp else 1.5.dp, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        // Vertical Pager: Swiping Up & Down transitions between cards
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            val alert = alerts.getOrNull(page) ?: return@VerticalPager

            if (!isExpanded) {
                // COLLAPSED: Single-line Samsung Now Bar Live Pill
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isExpanded = true }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Status Badge + Headline (Single line, strictly no clipping)
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(alert.iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = alert.icon,
                                contentDescription = null,
                                tint = alert.iconTint,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(9.dp))

                        Text(
                            text = alert.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right Side: Samsung Micro-Dots + Action Button + Expand Arrow
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Samsung Vertical Micro-dots indicator
                        if (alerts.size > 1) {
                            Column(
                                modifier = Modifier.padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                repeat(alerts.size) { idx ->
                                    val isCurrent = pagerState.currentPage == idx
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCurrent) 4.5.dp else 3.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) AccentPurple else BorderLight)
                                    )
                                }
                            }
                        }

                        // Compact Action Pill
                        Button(
                            onClick = alert.onAction,
                            shape = RoundedCornerShape(15.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = alert.actionColor),
                            contentPadding = PaddingValues(horizontal = 13.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = alert.actionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(3.dp))

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // EXPANDED: Detailed One UI Card (Revealed on Tap)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Row
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
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(alert.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = alert.icon,
                                    contentDescription = null,
                                    tint = alert.iconTint,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = alert.title,
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
                                        text = "${page + 1}/${alerts.size}",
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

                    // Full description text with room to breathe
                    if (alert.subtitle.isNotBlank()) {
                        Text(
                            text = alert.subtitle,
                            fontSize = 11.5.sp,
                            color = TextMuted,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp)
                        )
                    }

                    // Bottom action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (alert.onDismiss != null) {
                            TextButton(
                                onClick = {
                                    alert.onDismiss.invoke()
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
                                alert.onAction.invoke()
                                isExpanded = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = alert.actionColor),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(
                                text = alert.actionLabel,
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
