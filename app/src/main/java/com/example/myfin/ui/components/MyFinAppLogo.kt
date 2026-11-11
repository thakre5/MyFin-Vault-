package com.example.myfin.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.myfin.ui.theme.MyfinTheme
import com.example.myfin.ui.theme.Primary600

@Composable
fun MyFinAppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    tint: Color = Primary600,
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

            val strokePx = strokeWidth?.toPx() ?: (this.size.minDimension * 0.068f).coerceAtLeast(1.5f)
            val strokeStyle = Stroke(
                width = strokePx,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )

            // 1. Shield Outline Path
            val shieldPath = Path().apply {
                moveTo(w * 0.50f, h * 0.12f)
                lineTo(w * 0.86f, h * 0.22f)
                cubicTo(
                    w * 0.86f, h * 0.54f,
                    w * 0.72f, h * 0.78f,
                    w * 0.50f, h * 0.90f
                )
                cubicTo(
                    w * 0.28f, h * 0.78f,
                    w * 0.14f, h * 0.54f,
                    w * 0.14f, h * 0.22f
                )
                close()
            }

            drawPath(
                path = shieldPath,
                color = tint,
                style = strokeStyle
            )

            // 2. Trendline Zigzag Path
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

@Preview(name = "Outline Logo - Blue on Light", showBackground = true, backgroundColor = 0xFFF8FAFC)
@Composable
private fun PreviewMyFinAppLogoOutlineLight() {
    MyfinTheme {
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            MyFinAppLogo(size = 64.dp, tint = Primary600)
        }
    }
}

@Preview(name = "Outline Logo - White on Drawer Dark", showBackground = true, backgroundColor = 0xFF0F172A)
@Composable
private fun PreviewMyFinAppLogoOutlineDark() {
    MyfinTheme {
        Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            MyFinAppLogo(size = 64.dp, tint = Color.White)
        }
    }
}
