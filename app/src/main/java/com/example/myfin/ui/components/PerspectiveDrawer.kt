package com.example.myfin.ui.components

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.myfin.ui.theme.CanvasLight
import com.example.myfin.ui.theme.Neutral900
import com.example.myfin.ui.theme.Primary900

// Deep Slate-to-Royal-Navy gradient for the perspective drawer canvas
private val DrawerBackdropGradient = Brush.linearGradient(
    colors = listOf(
        Neutral900,
        Color(0xFF132347),
        Primary900
    )
)

@Composable
fun PerspectiveDrawer(
    isDrawerOpen: Boolean,
    onCloseDrawer: () -> Unit,
    drawerContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val shiftPx = with(density) { 260.dp.toPx() }
    val menuEntranceOffsetPx = with(density) { (-80).dp.toPx() }

    val transitionProgress by animateFloatAsState(
        targetValue = if (isDrawerOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "drawerAnimation"
    )

    // Ensure status bar icons turn white when the dark drawer is open
    val view = LocalView.current
    if (!view.isInEditMode && isDrawerOpen) {
        DisposableEffect(Unit) {
            val window = (view.context as? Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            val previousState = controller?.isAppearanceLightStatusBars ?: true
            controller?.isAppearanceLightStatusBars = false
            onDispose {
                controller?.isAppearanceLightStatusBars = previousState
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DrawerBackdropGradient)
    ) {
        // Drawer Menu Layer (Active and visible when open or animating)
        if (isDrawerOpen || transitionProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(260.dp)
                    .graphicsLayer {
                        alpha = transitionProgress
                        translationX = (1f - transitionProgress) * menuEntranceOffsetPx
                    }
            ) {
                drawerContent()
            }
        }

        // Perspective-Shifted Main Content Layer
        val scale = 1f - (transitionProgress * 0.15f)
        val translationX = transitionProgress * shiftPx
        val cornerRadius = (transitionProgress * 28).dp
        val cornerShape = RoundedCornerShape(cornerRadius)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.transformOrigin = TransformOrigin(0.5f, 0.5f)
                    this.scaleX = scale
                    this.scaleY = scale
                    this.translationX = translationX
                    this.shadowElevation = transitionProgress * 32f
                    this.shape = cornerShape
                    this.clip = true
                }
                .clip(cornerShape)
                .background(CanvasLight)
        ) {
            mainContent()

            // Tap-outside overlay to close drawer
            if (isDrawerOpen || transitionProgress > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.22f * transitionProgress))
                        .clickable(
                            enabled = isDrawerOpen,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onCloseDrawer()
                        }
                )
            }
        }
    }
}
