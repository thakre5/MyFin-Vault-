package com.example.myfin.ui.onboarding.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
