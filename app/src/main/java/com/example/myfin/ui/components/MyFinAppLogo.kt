package com.example.myfin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun MyFinAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    showBackgroundContainer: Boolean = true,
    containerCornerRadius: Dp = size * 0.28f,
    elevation: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (showBackgroundContainer) {
                    Modifier
                        .shadow(elevation, RoundedCornerShape(containerCornerRadius))
                        .clip(RoundedCornerShape(containerCornerRadius))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1E2034),
                                    Color(0xFF131522),
                                    Color(0xFF0C0D15)
                                )
                            )
                        )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize(if (showBackgroundContainer) 0.68f else 1.0f)
        ) {
            drawTriVaultLogo()
        }
    }
}

private fun DrawScope.drawTriVaultLogo() {
    val w = size.width
    val h = size.height

    // Brand Gradients
    val operatingBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF00D2EE), Color(0xFF2563EB)),
        startY = h * 0.2f,
        endY = h * 0.9f
    )

    val commitmentsBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFFA855F7), Color(0xFF6C5CE7)),
        startY = h * 0.05f,
        endY = h * 0.75f
    )

    val fortressBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF34D399), Color(0xFF0D9488)),
        startY = h * 0.2f,
        endY = h * 0.9f
    )

    // Shard 1: Left Pillar (Operating)
    val leftShard = Path().apply {
        moveTo(w * 0.12f, h * 0.88f)
        lineTo(w * 0.32f, h * 0.28f)
        cubicTo(w * 0.34f, h * 0.22f, w * 0.40f, h * 0.22f, w * 0.42f, h * 0.28f)
        lineTo(w * 0.48f, h * 0.46f)
        lineTo(w * 0.30f, h * 0.90f)
        cubicTo(w * 0.26f, h * 0.94f, w * 0.16f, h * 0.94f, w * 0.12f, h * 0.88f)
        close()
    }
    drawPath(leftShard, brush = operatingBrush)

    // Shard 2: Right Pillar (Fortress)
    val rightShard = Path().apply {
        moveTo(w * 0.88f, h * 0.88f)
        lineTo(w * 0.68f, h * 0.28f)
        cubicTo(w * 0.66f, h * 0.22f, w * 0.60f, h * 0.22f, w * 0.58f, h * 0.28f)
        lineTo(w * 0.52f, h * 0.46f)
        lineTo(w * 0.70f, h * 0.90f)
        cubicTo(w * 0.74f, h * 0.94f, w * 0.84f, h * 0.94f, w * 0.88f, h * 0.88f)
        close()
    }
    drawPath(rightShard, brush = fortressBrush)

    // Shard 3: Center Apex Roof (Commitments Vault)
    val centerApex = Path().apply {
        moveTo(w * 0.50f, h * 0.10f)
        lineTo(w * 0.62f, h * 0.44f)
        lineTo(w * 0.50f, h * 0.72f)
        lineTo(w * 0.38f, h * 0.44f)
        close()
    }
    drawPath(centerApex, brush = commitmentsBrush)

    // Central Core Security Node (Aperture Lock)
    val coreCenter = Offset(w * 0.50f, h * 0.44f)
    drawCircle(
        color = Color(0xFF0F111E),
        radius = w * 0.11f,
        center = coreCenter,
        style = Fill
    )
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFFFFF), Color(0xFF00D2EE)),
            center = coreCenter,
            radius = w * 0.08f
        ),
        radius = w * 0.065f,
        center = coreCenter,
        style = Fill
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.85f),
        radius = w * 0.12f,
        center = coreCenter,
        style = Stroke(width = 1.2.dp.toPx())
    )
}
