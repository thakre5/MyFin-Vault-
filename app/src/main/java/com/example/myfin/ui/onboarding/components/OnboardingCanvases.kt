package com.example.myfin.ui.onboarding.components

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfin.ui.onboarding.CyanPrimary
import com.example.myfin.ui.onboarding.PurplePrimary
import com.example.myfin.ui.onboarding.TealPrimary
import com.example.myfin.ui.theme.AccentPurple
import com.example.myfin.ui.theme.BorderLight
import com.example.myfin.ui.theme.TextDark
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun rememberImageBitmapFromUri(context: Context, uri: Uri?): ImageBitmap? {
    return remember(uri) {
        if (uri == null) null
        else {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

@Composable
fun SolnexTiltedCardsHero(
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(235.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(225.dp)
                .height(140.dp)
                .graphicsLayer {
                    rotationZ = -22f
                    translationX = -32f
                    translationY = -10f
                }
                .shadow(14.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF8B5CF6), Color(0xFFA855F7), Color(0xFFC084FC))
                        )
                    )
                    .padding(14.dp)
            ) {
                Text("✦", color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text("Balance", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                    Text("$currencySymbol 2,597.12", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Surface(
            modifier = Modifier
                .width(245.dp)
                .height(152.dp)
                .graphicsLayer {
                    rotationZ = -11f
                    translationX = 14f
                    translationY = 16f
                }
                .shadow(22.dp, RoundedCornerShape(22.dp)),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6B052B),
                                Color(0xFF9D174D),
                                Color(0xFFC026D3),
                                Color(0xFFE11D48),
                                Color(0xFFF43F5E)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Text("✦", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)

                Column(modifier = Modifier.align(Alignment.BottomStart)) {
                    Text("Balance", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                    Text("$currencySymbol 24,597.36", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }

        Box(
            modifier = Modifier
                .size(62.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-14).dp, y = (-42).dp)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0), Color(0xFF94A3B8))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFFCBD5E1), radius = size.minDimension * 0.44f, style = Stroke(width = 2.dp.toPx()))
            }
            Text("€", fontSize = 23.sp, fontWeight = FontWeight.Black, color = Color(0xFF64748B))
        }

        Box(
            modifier = Modifier
                .size(72.dp)
                .align(Alignment.CenterEnd)
                .offset(x = (-46).dp, y = 6.dp)
                .shadow(14.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9), Color(0xFF64748B))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(color = Color(0xFFCBD5E1), radius = size.minDimension * 0.44f, style = Stroke(width = 2.5.dp.toPx()))
            }
            Text("₿", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color(0xFF475569))
        }
    }
}

@Composable
fun OrbitalVaultParticlesCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbital")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "angle"
    )

    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension * 0.38f

        drawCircle(
            brush = Brush.radialGradient(listOf(PurplePrimary.copy(alpha = 0.35f), Color.Transparent)),
            radius = r * 1.1f,
            center = c
        )
        drawCircle(color = AccentPurple, radius = r * 0.55f, center = c)

        repeat(6) { i ->
            val particleAngle = Math.toRadians((angle + (i * 60)).toDouble())
            val px = c.x + (r * cos(particleAngle)).toFloat()
            val py = c.y + (r * sin(particleAngle)).toFloat()
            drawCircle(color = if (i % 2 == 0) CyanPrimary else TealPrimary, radius = 4.dp.toPx(), center = Offset(px, py))
        }
    }
}

@Composable
fun SecurityRadarPulseCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseRatio"
    )

    Canvas(modifier = modifier) {
        val c = center
        val maxR = size.minDimension * 0.45f
        drawCircle(color = AccentPurple.copy(alpha = 0.15f * (1f - pulseRatio)), radius = maxR * pulseRatio, center = c)
        drawCircle(color = AccentPurple.copy(alpha = 0.35f), radius = maxR * 0.6f, center = c)
        drawCircle(color = AccentPurple, radius = maxR * 0.35f, center = c)
    }
}

@Composable
fun OrbitalSyncClockCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "clock")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "rot"
    )

    Canvas(modifier = modifier) {
        val c = center
        val r = size.minDimension * 0.42f

        drawCircle(
            color = AccentPurple.copy(alpha = 0.4f),
            radius = r,
            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), rotation))
        )
        drawCircle(color = AccentPurple, radius = 4.dp.toPx(), center = c)
    }
}

@Composable
fun HumanDeliberationSceneCanvas(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "deliberation")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "delibPulse"
    )
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "delibFloat"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val c = Offset(w / 2, h * 0.52f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(PurplePrimary.copy(alpha = 0.25f * pulse), Color.Transparent),
                center = c,
                radius = 70.dp.toPx()
            ),
            radius = 70.dp.toPx(),
            center = c
        )

        drawOval(
            color = BorderLight.copy(alpha = 0.6f),
            topLeft = Offset(w * 0.22f, h * 0.78f),
            size = Size(w * 0.56f, 16.dp.toPx())
        )

        val shieldCenter = Offset(c.x, c.y + floatOffset)
        drawCircle(color = AccentPurple.copy(alpha = 0.18f), radius = 22.dp.toPx(), center = shieldCenter)
        drawCircle(color = AccentPurple, radius = 10.dp.toPx(), center = shieldCenter)
        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = shieldCenter)

        val f1Center = Offset(w * 0.24f, h * 0.50f)
        drawLine(color = CyanPrimary.copy(alpha = 0.45f * pulse), start = f1Center, end = shieldCenter, strokeWidth = 1.5.dp.toPx())
        drawCircle(color = TextDark, radius = 9.dp.toPx(), center = Offset(f1Center.x, f1Center.y - 18.dp.toPx()))
        drawRoundRect(color = CyanPrimary, topLeft = Offset(f1Center.x - 10.dp.toPx(), f1Center.y - 6.dp.toPx()), size = Size(20.dp.toPx(), 26.dp.toPx()), cornerRadius = CornerRadius(6.dp.toPx()))
        drawCircle(color = TealPrimary, radius = 4.dp.toPx(), center = Offset(f1Center.x + 8.dp.toPx(), f1Center.y + 4.dp.toPx() + floatOffset))

        val f2Center = Offset(w * 0.50f, h * 0.28f)
        drawLine(color = PurplePrimary.copy(alpha = 0.45f * pulse), start = f2Center, end = shieldCenter, strokeWidth = 1.5.dp.toPx())
        drawCircle(color = TextDark, radius = 8.dp.toPx(), center = Offset(f2Center.x, f2Center.y - 16.dp.toPx()))
        drawRoundRect(color = PurplePrimary, topLeft = Offset(f2Center.x - 9.dp.toPx(), f2Center.y - 6.dp.toPx()), size = Size(18.dp.toPx(), 22.dp.toPx()), cornerRadius = CornerRadius(5.dp.toPx()))

        val f3Center = Offset(w * 0.76f, h * 0.50f)
        drawLine(color = Color(0xFFE57A28).copy(alpha = 0.45f * pulse), start = f3Center, end = shieldCenter, strokeWidth = 1.5.dp.toPx())
        drawCircle(color = TextDark, radius = 9.dp.toPx(), center = Offset(f3Center.x, f3Center.y - 18.dp.toPx()))
        drawRoundRect(color = Color(0xFFE57A28), topLeft = Offset(f3Center.x - 10.dp.toPx(), f3Center.y - 6.dp.toPx()), size = Size(20.dp.toPx(), 26.dp.toPx()), cornerRadius = CornerRadius(6.dp.toPx()))
        drawCircle(color = Color(0xFFE57A28), radius = 4.dp.toPx(), center = Offset(f3Center.x - 8.dp.toPx(), f3Center.y + 4.dp.toPx() - floatOffset))
    }
}
