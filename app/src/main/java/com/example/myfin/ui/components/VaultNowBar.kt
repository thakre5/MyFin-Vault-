package com.example.myfin.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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

    // Auto-scroll vertically every 4.5 seconds when not expanded and not being interacted with
    LaunchedEffect(alerts.size, isExpanded) {
        if (alerts.size > 1 && !isExpanded) {
            while (true) {
                delay(4500L)
                if (!pagerState.isScrollInProgress) {
                    val nextPage = (pagerState.currentPage + 1) % alerts.size
                    pagerState.animateScrollToPage(
                        page = nextPage,
                        animationSpec = tween(450, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
    }

    // Dynamic height: Compact 48dp when collapsed, perfectly tailored 112dp when expanded (No white space)
    val barHeight by animateDpAsState(
        targetValue = if (isExpanded) 112.dp else 48.dp,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "nowBarHeight"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 18.dp else 24.dp,
        animationSpec = tween(280),
        label = "nowBarCorner"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .shadow(if (isExpanded) 3.dp else 1.5.dp, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = CardWhite,
        border = BorderStroke(0.85.dp, BorderLight.copy(alpha = 0.8f))
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = alerts.size > 1
        ) { page ->
            val alert = alerts.getOrNull(page) ?: return@VerticalPager

            if (!isExpanded) {
                // =============================================================
                // COLLAPSED: Single-line Samsung One UI Now Bar Capsule
                // =============================================================
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left tap-to-expand zone
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
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(alert.iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = alert.icon,
                                contentDescription = null,
                                tint = alert.iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

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

                    // Right Side: Micro-dots + Action Button + Chevron
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Samsung Vertical Indicator Dots
                        if (alerts.size > 1) {
                            Column(
                                modifier = Modifier.padding(end = 7.dp),
                                verticalArrangement = Arrangement.spacedBy(2.5.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                repeat(alerts.size) { idx ->
                                    val isCurrent = pagerState.currentPage == idx
                                    Box(
                                        modifier = Modifier
                                            .size(width = 3.dp, height = if (isCurrent) 7.dp else 3.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) alert.actionColor else BorderLight)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = alert.onAction,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = alert.actionColor),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = alert.actionLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(2.dp))

                        IconButton(
                            onClick = { isExpanded = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = TextMuted.copy(alpha = 0.7f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            } else {
                // =============================================================
                // EXPANDED: Snug One UI Card (Clicking anywhere collapses)
                // =============================================================
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isExpanded = false }
                        .padding(horizontal = 13.dp, vertical = 10.dp),
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
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(alert.iconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = alert.icon,
                                    contentDescription = null,
                                    tint = alert.iconTint,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(7.dp))

                            Text(
                                text = alert.title,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (alerts.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(5.dp),
                                    color = CanvasLight,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = "${page + 1}/${alerts.size}",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.5.dp)
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

                    // Description text (fits directly between header and buttons without gap)
                    if (alert.subtitle.isNotBlank()) {
                        Text(
                            text = alert.subtitle,
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp)
                        )
                    }

                    // Action Row
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
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(
                                    text = "Dismiss",
                                    fontSize = 10.5.sp,
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
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = alert.actionLabel,
                                fontSize = 11.sp,
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
