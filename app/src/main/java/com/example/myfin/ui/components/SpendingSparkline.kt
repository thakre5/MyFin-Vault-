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

        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        val strokePath = Path()
        val fillPath = Path()

        var lastPoint = Offset(0f, size.height)

        points.forEachIndexed { i, value ->
            val x = i * stepX
            val normalizedY = 1f - ((value - minVal) / range)
            val y = normalizedY * (size.height - 18.dp.toPx()) + 8.dp.toPx()

            if (i == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevNormY = 1f - ((points[i - 1] - minVal) / range)
                val prevY = prevNormY * (size.height - 18.dp.toPx()) + 8.dp.toPx()

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
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Halo & endpoint nodes
        drawCircle(
            color = lineColor.copy(alpha = 0.18f),
            radius = 8.dp.toPx(),
            center = lastPoint
        )
        drawCircle(
            color = lineColor,
            radius = 4.dp.toPx(),
            center = lastPoint
        )
        drawCircle(
            color = Color.White,
            radius = 2.dp.toPx(),
            center = lastPoint
        )
    }
}
