package com.example.myfin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
    val shortTitle: String,
    val fullTitle: String,
    val description: String,
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
                    pagerState.animateScrollToPage(next, animationSpec = tween(500))
                }
            }
        }
    }

    val barHeight by animateDpAsState(
        targetValue = if (isExpanded) 132.dp else 48.dp,
        animationSpec = tween(320),
        label = "nowBarHeight"
    )

    val cornerRadius by animateDpAsState(
        targetValue = if (isExpanded) 18.dp else 24.dp,
        animationSpec = tween(320),
        label = "nowBarCorner"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight)
            .shadow(if (isExpanded) 3.dp else 1.5.dp, RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = true
        ) { page ->
            val alert = alerts.getOrNull(page) ?: return@VerticalPager

            if (!isExpanded) {
                // COLLAPSED ONE UI NOW BAR (Single-line live activity capsule)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left glyph + Single-line metric
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
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(9.dp))

                        Text(
                            text = alert.shortTitle,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Right action pill + vertical micro-dots
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End
                    ) {
                        // Samsung Vertical Micro-dots indicator
                        if (alerts.size > 1) {
                            Column(
                                modifier = Modifier.padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                repeat(alerts.size) { idx ->
                                    val isCurrent = pagerState.currentPage == idx
                                    Box(
                                        modifier = Modifier
                                            .size(if (isCurrent) 4.5.dp else 3.5.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) AccentPurple else BorderLight)
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

                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else {
                // EXPANDED ONE UI CARD (Full details + swipeable up/down)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header row
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
                                text = alert.fullTitle,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (alerts.size > 1) {
                                Text(
                                    text = "${page + 1}/${alerts.size}",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextMuted,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
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

                    // Explanation body
                    Text(
                        text = alert.description,
                        fontSize = 11.sp,
                        color = TextMuted,
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Footer actions
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
                                Text("Dismiss", fontSize = 10.5.sp, color = TextMuted, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = {
                                alert.onAction.invoke()
                                isExpanded = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = alert.actionColor),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
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
