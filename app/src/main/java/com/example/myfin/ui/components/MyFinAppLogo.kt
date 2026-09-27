package com.example.myfin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.MyfinTheme

@Composable
fun MyFinAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tint: Color = AccentPurple,
    strokeWidth: Dp? = null,
    onClick: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            // Dynamic stroke width: ~6.5% of dimension if not explicitly provided
            val strokePx = strokeWidth?.toPx() ?: (this.size.minDimension * 0.068f).coerceAtLeast(1.5f)
            val strokeStyle = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // 1. Shield Outline Path
            val shieldPath = Path().apply {
                // Top center peak
                moveTo(w * 0.50f, h * 0.12f)
                // Top-right shoulder
                lineTo(w * 0.86f, h * 0.22f)
                // Down to bottom center tip via smooth cubic curve
                cubicTo(
                    w * 0.86f, h * 0.54f,
                    w * 0.72f, h * 0.78f,
                    w * 0.50f, h * 0.90f
                )
                // Bottom tip up to top-left shoulder
                cubicTo(
                    w * 0.28f, h * 0.78f,
                    w * 0.14f, h * 0.54f,
                    w * 0.14f, h * 0.22f
                )
                // Close back to top center peak
                close()
            }

            drawPath(
                path = shieldPath,
                color = tint,
                style = strokeStyle
            )

            // 2. Trendline Zigzag Path (Up -> Down -> Shoot Up-Right)
            val trendPath = Path().apply {
                moveTo(w * 0.26f, h * 0.58f)
                lineTo(w * 0.43f, h * 0.42f)
                lineTo(w * 0.53f, h * 0.52f)
                lineTo(w * 0.72f, h * 0.33f)
            }

            drawPath(
                path = trendPath,
                color = tint,
                style = strokeStyle
            )

            // 3. Arrowhead Wings
            val arrowHeadPath = Path().apply {
                moveTo(w * 0.58f, h * 0.32f)
                lineTo(w * 0.73f, h * 0.32f)
                lineTo(w * 0.73f, h * 0.47f)
            }

            drawPath(
                path = arrowHeadPath,
                color = tint,
                style = strokeStyle
            )
        }
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(name = "Outline Logo - Purple on Light", showBackground = true, backgroundColor = 0xFFF7F8FA)
@Composable
private fun PreviewMyFinAppLogoOutlineLight() {
    MyfinTheme {
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            MyFinAppLogo(size = 64.dp, tint = AccentPurple)
        }
    }
}

@Preview(name = "Outline Logo - White on Drawer Dark", showBackground = true, backgroundColor = 0xFF231B38)
@Composable
private fun PreviewMyFinAppLogoOutlineDark() {
    MyfinTheme {
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            MyFinAppLogo(size = 64.dp, tint = Color.White)
        }
    }
}
