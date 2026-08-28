package com.example.myfin.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .graphicsLayer {
                alpha = animAlpha
                translationY = animTranslationY
            }
    ) {
        // Fullscreen dismiss layer when contextual popup is open
        if (isFabMenuExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
                    .pointerInput(Unit) {
                        detectTapGestures { isFabMenuExpanded = false }
                    }
            )
        }

        // Contextual Actions Popup Card
        AnimatedVisibility(
            visible = isFabMenuExpanded && fabActions.isNotEmpty(),
            enter = fadeIn(tween(160)) + scaleIn(tween(180), initialScale = 0.85f),
            exit = fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.85f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 68.dp)
                .zIndex(2f)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 180.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp), ambientColor = Color.Black.copy(alpha = 0.15f))
                    .clip(RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp)
                ) {
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

        // Bottom Bar Row: Navigation Island + Floating FAB
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .zIndex(1.5f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Main Navigation Island
            Surface(
                modifier = Modifier
                    .height(58.dp)
                    .weight(1f)
                    .shadow(12.dp, RoundedCornerShape(29.dp), ambientColor = Color.Black.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(29.dp),
                color = CardWhite
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

            // Right Contextual Action FAB
            Surface(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(12.dp, CircleShape, ambientColor = AccentPurple.copy(alpha = 0.35f))
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
