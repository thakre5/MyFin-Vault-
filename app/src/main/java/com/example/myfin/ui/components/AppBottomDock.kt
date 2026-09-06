package com.example.myfin.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.myfin.ui.theme.*

data class DockFabAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit
)

data class DockNavItem(
    val target: NavigationTarget,
    val label: String,
    val icon: ImageVector
)

val DefaultDockNavItems = listOf(
    DockNavItem(NavigationTarget.MONTHLY_VIEW, "Summary", Icons.Default.Assessment),
    DockNavItem(NavigationTarget.BUDGET_PLANNER, "Planner", Icons.Default.PieChart),
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

    // Intercept hardware back button to dismiss expanded FAB actions menu
    BackHandler(enabled = isFabMenuExpanded) {
        isFabMenuExpanded = false
    }

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

    Box(modifier = modifier) {
        // 1. Scrim Backdrop
        AnimatedVisibility(
            visible = isFabMenuExpanded && fabActions.isNotEmpty(),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(140)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isFabMenuExpanded = false
                    }
            )
        }

        // 2. Floating Contextual Actions Popup
        AnimatedVisibility(
            visible = isFabMenuExpanded && fabActions.isNotEmpty(),
            enter = scaleIn(
                transformOrigin = TransformOrigin(0.88f, 1f),
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 450f)
            ) + fadeIn(animationSpec = tween(150)),
            exit = scaleOut(
                transformOrigin = TransformOrigin(0.88f, 1f),
                animationSpec = tween(120)
            ) + fadeOut(animationSpec = tween(120)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(bottom = 80.dp, end = 16.dp)
                .zIndex(3f)
        ) {
            Surface(
                modifier = Modifier
                    .width(190.dp)
                    .shadow(
                        elevation = 18.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = Color.Black.copy(alpha = 0.22f),
                        spotColor = Color.Black.copy(alpha = 0.28f)
                    )
                    .clip(RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                color = CardWhite,
                border = BorderStroke(0.8.dp, BorderLight.copy(alpha = 0.8f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .verticalScroll(rememberScrollState())
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label,
                                tint = AccentPurple,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = action.label,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextDark
                            )
                        }

                        if (index < fabActions.size - 1) {
                            HorizontalDivider(
                                color = BorderLight.copy(alpha = 0.5f),
                                thickness = 0.7.dp,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Synchronized Bottom Gradient Scrim (Hides & Shows with Dock)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    alpha = animAlpha
                    translationY = animTranslationY
                }
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            CanvasLight.copy(alpha = 0.85f),
                            CanvasLight
                        )
                    )
                )
                .zIndex(1.5f)
        )

        // 4. Main Bottom Dock Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .graphicsLayer {
                    alpha = animAlpha
                    translationY = animTranslationY
                }
                .zIndex(2f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier
                    .height(58.dp)
                    .weight(1f)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(29.dp),
                        ambientColor = Color.Black.copy(alpha = 0.12f),
                        spotColor = Color.Black.copy(alpha = 0.18f)
                    ),
                shape = RoundedCornerShape(29.dp),
                color = CardWhite,
                border = BorderStroke(0.6.dp, BorderLight.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    navItems.forEach { item ->
                        val isSelected = currentSelection == item.target

                        Box(
                            modifier = Modifier
                                .weight(if (isSelected) 1.85f else 1.0f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(25.dp))
                                .background(if (isSelected) AccentPurple.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isFabMenuExpanded = false
                                    if (!isSelected) {
                                        onSelectTarget(item.target)
                                    }
                                }
                                .padding(horizontal = 6.dp),
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
                                    modifier = Modifier.size(18.dp)
                                )
                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = item.label,
                                        color = AccentPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        letterSpacing = (-0.2).sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Clip
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Surface(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = CircleShape,
                        ambientColor = AccentPurple.copy(alpha = 0.40f),
                        spotColor = AccentPurple.copy(alpha = 0.45f)
                    )
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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

@Composable
fun rememberAutoScrollVisibilityConnection(): Pair<MutableState<Boolean>, NestedScrollConnection> {
    val isVisible = remember { mutableStateOf(true) }

    val connection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -14f && isVisible.value) {
                    isVisible.value = false
                } else if (delta > 14f && !isVisible.value) {
                    isVisible.value = true
                }
                return Offset.Zero
            }
        }
    }

    return Pair(isVisible, connection)
}
