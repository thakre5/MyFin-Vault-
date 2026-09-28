package com.example.myfin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
    val subtitle: String,
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

    val pagerState = rememberPagerState(pageCount = { alerts.size })

    // Auto-cycle every 4.5s if not touched and multiple alerts exist
    LaunchedEffect(pagerState, alerts.size) {
        if (alerts.size > 1) {
            while (true) {
                delay(4500L)
                if (!pagerState.isScrollInProgress) {
                    val next = (pagerState.currentPage + 1) % alerts.size
                    pagerState.animateScrollToPage(next, animationSpec = tween(500))
                }
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(2.dp, RoundedCornerShape(24.dp)),
        shape = RoundedCornerShape(24.dp),
        color = CardWhite,
        border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val alert = alerts.getOrNull(page) ?: return@HorizontalPager
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Icon + Text Headline
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

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = alert.title,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = alert.subtitle,
                            fontSize = 9.sp,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Page count badge + Action button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    if (alerts.size > 1) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = CanvasLight,
                            modifier = Modifier.padding(end = 5.dp)
                        ) {
                            Text(
                                text = "${page + 1}/${alerts.size}",
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Button(
                        onClick = alert.onAction,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = alert.actionColor),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 0.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = alert.actionLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    if (alert.onDismiss != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        IconButton(
                            onClick = alert.onDismiss,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = TextMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
