package com.example.myfin.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.myfin.ui.theme.TextDark

@Composable
fun PerspectiveDrawer(
    isDrawerOpen: Boolean,
    onCloseDrawer: () -> Unit,
    drawerContent: @Composable () -> Unit,
    mainContent: @Composable () -> Unit
) {
    val transitionProgress by animateFloatAsState(
        targetValue = if (isDrawerOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "drawerAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TextDark)
    ) {
        // Drawer Menu Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = transitionProgress
                    translationX = (1f - transitionProgress) * -100f
                }
        ) {
            drawerContent()
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

            // Scrim to intercept taps and close drawer
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
