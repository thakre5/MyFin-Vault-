package com.example.myfin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myfin.ui.theme.AccentPurple

@Composable
fun SpendingSparkline(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = AccentPurple,
    gradientStartColor: Color = AccentPurple.copy(alpha = 0.28f),
    gradientEndColor: Color = AccentPurple.copy(alpha = 0.0f)
) {
    if (points.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
    ) {
        val maxVal = points.maxOrNull()?.takeIf { it > 0f } ?: 1f
        val minVal = 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        // Draw faint dashed baseline grid
        val midY = size.height * 0.5f
        drawLine(
            color = Color(0xFFE2E8F0).copy(alpha = 0.7f),
            start = Offset(0f, midY),
            end = Offset(size.width, midY),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
        )

        // Pre-compute pixel values outside the drawing loop
        val paddingTopPx = 8.dp.toPx()
        val paddingBottomPx = 18.dp.toPx()
        val usableHeightPx = size.height - paddingBottomPx
        val strokeWidthPx = 3.dp.toPx()
        val haloRadiusPx = 8.dp.toPx()
        val nodeRadiusPx = 4.dp.toPx()
        val coreRadiusPx = 2.dp.toPx()

        // Handle single-point edge case
        if (points.size == 1) {
            val y = (1f - ((points[0] - minVal) / range)) * usableHeightPx + paddingTopPx
            val centerPoint = Offset(size.width * 0.5f, y)

            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = strokeWidthPx,
                cap = StrokeCap.Round
            )
            drawCircle(color = lineColor.copy(alpha = 0.18f), radius = haloRadiusPx, center = centerPoint)
            drawCircle(color = lineColor, radius = nodeRadiusPx, center = centerPoint)
            drawCircle(color = Color.White, radius = coreRadiusPx, center = centerPoint)
            return@Canvas
        }

        val stepX = size.width / (points.size - 1)
        val strokePath = Path()
        val fillPath = Path()

        var lastPoint = Offset(0f, size.height)

        points.forEachIndexed { i, rawValue ->
            val value = rawValue.coerceAtLeast(0f)
            val x = i * stepX
            val normalizedY = 1f - ((value - minVal) / range)
            val y = normalizedY * usableHeightPx + paddingTopPx

            if (i == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevValue = points[i - 1].coerceAtLeast(0f)
                val prevNormY = 1f - ((prevValue - minVal) / range)
                val prevY = prevNormY * usableHeightPx + paddingTopPx

                val controlPoint1 = Offset(prevX + (x - prevX) / 2f, prevY)
                val controlPoint2 = Offset(prevX + (x - prevX) / 2f, y)

                strokePath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    x, y
                )
                fillPath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    x, y
                )
            }

            if (i == points.lastIndex) {
                lastPoint = Offset(x, y)
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }
        }

        // Under-curve gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(gradientStartColor, gradientEndColor),
                startY = 0f,
                endY = size.height
            )
        )

        // Smooth Bezier line stroke
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
        )

        // Endpoint nodes
        drawCircle(color = lineColor.copy(alpha = 0.18f), radius = haloRadiusPx, center = lastPoint)
        drawCircle(color = lineColor, radius = nodeRadiusPx, center = lastPoint)
        drawCircle(color = Color.White, radius = coreRadiusPx, center = lastPoint)
    }
}
