package com.example.myfin.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

// Deep soft purple background for the drawer canvas
private val SoftPurpleDrawerBg = Color(0xFF231B38)

@Composable
fun PerspectiveDrawer(
    isDrawerOpen: Boolean,
    onCloseDrawer: () -> Unit,
    drawerContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit
) {
    val density = LocalDensity.current.density
    val transitionProgress by animateFloatAsState(
        targetValue = if (isDrawerOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "drawerAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftPurpleDrawerBg)
    ) {
        // Drawer Menu Layer (Isolated & active only when open)
        if (isDrawerOpen || transitionProgress > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(250.dp)
                    .graphicsLayer {
                        alpha = transitionProgress
                        translationX = (1f - transitionProgress) * -80f
                    }
            ) {
                drawerContent()
            }
        }

        // Pushed Main Content Layer
        val scale = 1f - (transitionProgress * 0.15f)
        val translationX = transitionProgress * 260f * density
        val cornerRadius = (transitionProgress * 28).dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.scaleX = scale
                    this.scaleY = scale
                    this.translationX = translationX
                    this.shadowElevation = if (isDrawerOpen) 30f else 0f
                }
                .clip(RoundedCornerShape(cornerRadius))
                .background(Color.White)
        ) {
            mainContent()

            if (isDrawerOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.18f * transitionProgress))
                        .pointerInput(Unit) {
                            detectTapGestures { onCloseDrawer() }
                        }
                )
            }
        }
    }
}
