package com.example.myfin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.ui.theme.*

/**
 * Model representing an action inside the Contextual FAB popup menu.
 */
data class DockFabAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

/**
 * Model representing a destination item on the bottom navigation island.
 */
data class DockNavItem(
    val target: NavigationTarget,
    val label: String,
    val icon: ImageVector
)

val DefaultDockNavItems = listOf(
    DockNavItem(NavigationTarget.MONTHLY_VIEW, "Summary", Icons.Default.Assessment),
    DockNavItem(NavigationTarget.DATA_SET, "Taxonomy", Icons.Default.Category),
    DockNavItem(NavigationTarget.VAULT_ACCOUNTS, "Vaults", Icons.Default.AccountBalance),
    DockNavItem(NavigationTarget.REPORTS_ANALYTICS, "Analytics", Icons.Default.BarChart)
)

@Composable
fun AppBottomDock(
    currentSelection: NavigationTarget,
    onSelectTarget: (NavigationTarget) -> Unit,
    modifier: Modifier = Modifier,
    fabActions: List<DockFabAction> = emptyList(),
    onDirectFabClick: (() -> Unit)? = null,
    navItems: List<DockNavItem> = DefaultDockNavItems,
    isVisible: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    var isFabMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible, currentSelection) {
        if (!isVisible) {
            isFabMenuExpanded = false
        }
    }

    val animAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = if (isVisible) 220 else 180),
        label = "dockAlpha"
    )

    val animTranslationY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 80f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "dockSlide"
    )

    val fabRotation by animateFloatAsState(
        targetValue = if (isFabMenuExpanded) 45f else 0f,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 500f),
        label = "fabRotation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .graphicsLayer {
                alpha = animAlpha
                translationY = animTranslationY
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            // Contextual Actions Popup (Anchored strictly directly above the FAB)
            AnimatedVisibility(
                visible = isFabMenuExpanded && fabActions.isNotEmpty(),
                enter = scaleIn(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(180)
                ) + fadeIn(animationSpec = tween(180)),
                exit = scaleOut(
                    transformOrigin = TransformOrigin(1f, 1f),
                    animationSpec = tween(150)
                ) + fadeOut(animationSpec = tween(150)),
                modifier = Modifier
                    .padding(bottom = 12.dp, end = 2.dp)
                    .zIndex(2f)
            ) {
                Surface(
                    modifier = Modifier
                        .widthIn(min = 185.dp)
                        .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.20f))
                        .clip(RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    color = CardWhite,
                    border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        fabActions.forEachIndexed { index, action ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        isFabMenuExpanded = false
                                        action.onClick()
                                    }
                                    .padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = action.icon,
                                    contentDescription = action.label,
                                    tint = AccentPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = action.label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = TextDark
                                )
                            }

                            if (index < fabActions.size - 1) {
                                HorizontalDivider(
                                    color = BorderLight.copy(alpha = 0.4f),
                                    thickness = 0.7.dp,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Island + Contextual FAB Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(1.5f),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Main Navigation Capsule
                Surface(
                    modifier = Modifier
                        .height(58.dp)
                        .weight(1f)
                        .shadow(16.dp, RoundedCornerShape(29.dp), ambientColor = Color.Black.copy(alpha = 0.12f), spotColor = Color.Black.copy(alpha = 0.16f)),
                    shape = RoundedCornerShape(29.dp),
                    color = CardWhite,
                    border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            val isSelected = currentSelection == item.target
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) AccentPurple.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        if (!isSelected) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            isFabMenuExpanded = false
                                            onSelectTarget(item.target)
                                        }
                                    }
                                    .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
                                        tint = if (isSelected) AccentPurple else TextMuted,
                                        modifier = Modifier.size(19.dp)
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = item.label,
                                            color = AccentPurple,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Right Action FAB
                Surface(
                    modifier = Modifier
                        .size(58.dp)
                        .shadow(16.dp, CircleShape, ambientColor = AccentPurple.copy(alpha = 0.40f), spotColor = AccentPurple.copy(alpha = 0.45f))
                        .clip(CircleShape)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            if (fabActions.isNotEmpty()) {
                                isFabMenuExpanded = !isFabMenuExpanded
                            } else {
                                onDirectFabClick?.invoke()
                            }
                        },
                    shape = CircleShape,
                    color = AccentPurple
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Contextual Action",
                            tint = Color.White,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(fabRotation)
                        )
                    }
                }
            }
        }
    }
}
