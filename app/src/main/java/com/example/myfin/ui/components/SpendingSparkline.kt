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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.myfin.ui.theme.AccentPurple

@Composable
fun SpendingSparkline(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = AccentPurple,
    fillColor: Color = AccentPurple.copy(alpha = 0.12f)
) {
    if (points.isEmpty()) return

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        val maxVal = points.maxOrNull()?.takeIf { it > 0f } ?: 1f
        val minVal = 0f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val stepX = size.width / (points.size - 1).coerceAtLeast(1)
        val strokePath = Path()
        val fillPath = Path()

        points.forEachIndexed { i, value ->
            val x = i * stepX
            val normalizedY = 1f - ((value - minVal) / range)
            val y = normalizedY * (size.height - 8.dp.toPx()) + 4.dp.toPx()

            if (i == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, size.height)
                fillPath.lineTo(x, y)
            } else {
                val prevX = (i - 1) * stepX
                val prevNormY = 1f - ((points[i - 1] - minVal) / range)
                val prevY = prevNormY * (size.height - 8.dp.toPx()) + 4.dp.toPx()

                val cx = (prevX + x) / 2
                strokePath.cubicTo(cx, prevY, cx, y, x, y)
                fillPath.cubicTo(cx, prevY, cx, y, x, y)
            }

            if (i == points.lastIndex) {
                fillPath.lineTo(x, size.height)
                fillPath.close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = size.height
            )
        )

        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
